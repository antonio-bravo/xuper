package com.example.xuper

import android.app.PictureInPictureParams
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.xuper.model.Channel
import com.example.xuper.model.M3UList
import com.example.xuper.ui.AppLanguage
import com.example.xuper.ui.LanguageManager
import com.example.xuper.ui.stringResourceAI
import com.example.xuper.ui.components.ErrorState
import com.example.xuper.ui.components.UniversalPlayer
import com.example.xuper.ui.navigation.*
import com.example.xuper.ui.screens.*
import com.example.xuper.ui.theme.XuperTheme
import com.example.xuper.ui.viewmodel.MainViewModel
import com.example.xuper.ui.viewmodel.MainViewModelFactory
import com.example.xuper.ui.viewmodel.ArenaViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MainActivity : ComponentActivity() {
    internal var isPlaybackActiveForPip: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LanguageManager.init(this)
        setContent {
            XuperTheme {
                XuperApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.xuper.util.UpdateManager.checkPendingInstall(this)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPlaybackActiveForPip && !isInPictureInPictureMode) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class XuperAppState(
    private val context: Context,
    private val viewModel: MainViewModel,
    val navigator: Navigator
) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("xuper_prefs", Context.MODE_PRIVATE)

    var m3uLists by mutableStateOf(loadM3uLists(sharedPrefs))
        private set

    var showLanguageDialog by mutableStateOf(false)
    var selectedChannel by mutableStateOf<Channel?>(null)

    var refreshTrigger by mutableIntStateOf(0)
        private set

    val filterFocusRequester = FocusRequester()

    val currentChannelsFlow = viewModel.filteredChannels
    val availableCategoriesFlow = viewModel.availableCategories
    val isLoadingFlow = viewModel.isLoading
    val errorMessageFlow = viewModel.errorMessage
    val favoriteChannelsFlow = viewModel.favoriteChannels
    val searchQueryFlow = viewModel.searchQuery
    val selectedCategoryFlow = viewModel.selectedCategory
    val selectedListNameFlow = viewModel.selectedListName

    var availableUpdate by mutableStateOf<com.example.xuper.util.AppUpdateInfo?>(null)
        private set
    var showUpdateDialog by mutableStateOf(false)

    init {
        if (viewModel.allChannels.value.isEmpty()) {
            viewModel.refreshChannels(m3uLists)
        }
    }

    suspend fun checkForUpdates() {
        val update = com.example.xuper.util.UpdateManager.checkForUpdates(context)
        availableUpdate = update
    }

    fun saveLists(newList: List<M3UList>) {
        m3uLists = newList
        val arr = JSONArray().apply {
            newList.forEach {
                put(JSONObject().apply {
                    put("id", it.id)
                    put("name", it.name)
                    put("url", it.url)
                    put("enabled", it.enabled)
                })
            }
        }
        sharedPrefs.edit { putString("m3u_lists", arr.toString()) }
        viewModel.refreshChannels(m3uLists)
    }

    fun toggleFavorite(channel: Channel) {
        viewModel.toggleFavorite(channel)
    }

    var fullScreenUrl by mutableStateOf<String?>(null)
        private set

    var activeChannelList by mutableStateOf<List<Channel>>(emptyList())
        private set

    var activeZicoTokenInfo by mutableStateOf<com.example.xuper.data.ZicoTvTokenInfo?>(null)
        private set

    fun playChannel(channel: Channel?) {
        selectedChannel = channel
    }

    fun playChannelFullScreen(channel: Channel?, channelList: List<Channel> = emptyList()) {
        val target = channel ?: selectedChannel
        if (target != null && target.url.isNotEmpty()) {
            activeZicoTokenInfo = null
            selectedChannel = target
            if (channelList.isNotEmpty()) {
                activeChannelList = channelList
            }
            val aceManager = com.example.xuper.data.AceStreamManager(context)
            val formattedUrl = aceManager.getFormattedUrlForProxy(target.url, aceManager.getSelectedProxy())
            fullScreenUrl = formattedUrl
        }
    }

    fun playZicoChannelFullScreen(tokenInfo: com.example.xuper.data.ZicoTvTokenInfo) {
        activeChannelList = emptyList()
        selectedChannel = null
        activeZicoTokenInfo = tokenInfo
        fullScreenUrl = tokenInfo.streamUrl
    }

    fun playUrlFullScreen(url: String) {
        if (url.isNotEmpty()) {
            activeZicoTokenInfo = null
            activeChannelList = emptyList()
            val aceManager = com.example.xuper.data.AceStreamManager(context)
            val formattedUrl = aceManager.getFormattedUrlForProxy(url, aceManager.getSelectedProxy())
            fullScreenUrl = formattedUrl
        }
    }

    fun playAdjacentChannel(step: Int) {
        val list = activeChannelList
        val current = selectedChannel
        if (list.size <= 1 || current == null) return
        val currentIndex = list.indexOfFirst { it.url == current.url && it.name == current.name }
            .let { if (it >= 0) it else list.indexOfFirst { c -> c.name == current.name } }
        val baseIndex = if (currentIndex >= 0) currentIndex else 0
        val nextIndex = ((baseIndex + step) % list.size + list.size) % list.size
        playChannelFullScreen(list[nextIndex], list)
    }

    fun closeFullScreen() {
        fullScreenUrl = null
        selectedChannel = null
        activeChannelList = emptyList()
        activeZicoTokenInfo = null
    }

    fun triggerRefresh() {
        refreshTrigger++
        viewModel.refreshChannels(m3uLists)
    }

    fun setSearchQuery(query: String) = viewModel.setSearchQuery(query)
    fun setSelectedCategory(category: String) = viewModel.setSelectedCategory(category)
    fun setSelectedListName(listName: String) = viewModel.setSelectedListName(listName)

    companion object {
        fun loadM3uLists(sharedPrefs: SharedPreferences): List<M3UList> {
            val saved = sharedPrefs.getString("m3u_lists", null)
            if (!saved.isNullOrEmpty()) {
                try {
                    val arr = JSONArray(saved)
                    val list = mutableListOf<M3UList>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = (obj.optString("id", "") ?: "").ifEmpty { UUID.randomUUID().toString() }
                        val name = (obj.optString("name", "") ?: "").ifEmpty { "Sin nombre" }
                        val url = obj.optString("url", "") ?: ""
                        val enabled = obj.optBoolean("enabled", true)
                        if (url.isNotEmpty()) {
                            list.add(M3UList(id = id, name = name, url = url, enabled = enabled))
                        }
                    }
                    return list.toList()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return getDefaultM3uLists()
        }

        private fun getDefaultM3uLists(): List<M3UList> = listOf(
            M3UList(id = UUID.randomUUID().toString(), name = "Arena4Viewer", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/arena4viewer.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "_ipfs_io", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/ipfs_io.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "TDT Channels", url = "https://www.tdtchannels.com/lists/tv.m3u8"),
            M3UList(id = UUID.randomUUID().toString(), name = "Lista Scraper Acestream", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_scraper_acestream_api.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "Lacasadel_TikiTaka", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lacasadel_tikitaka.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "BatmanStream", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/batmanstream.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "PirloTV", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/pirlotv.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "Rojadirecta", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/rojadirecta.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "SportP2P", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/sportp2p.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "VipRow", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/viprow.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "IPFS Hashes", url = "https://ipfs.io/ipns/k51qzi5uqu5di462t7j4vu4akwfhvtjhy88qbupktvoacqfqe9uforjvhyi4wr/hashes.json"),
            M3UList(id = UUID.randomUUID().toString(), name = "TvPremiumHD", url = "http://tvpremiumhd.club/tv.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "Cine & Series (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/movies.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "Documentales (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/documentary.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "Deportes (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/sports.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "Kids (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/kids.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "RBTV Futbol", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_rbtv_futbol.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "RBTV Futbol RAW", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_rbtv_futbol_raw.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "TV 247 US", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_tv247us.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "TV Now 247", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_tvnow247.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "Streamed", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_streamed.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "golxu", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_golxu.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "AK47 Sports", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_ak47sports.m3u"),
            M3UList(id = UUID.randomUUID().toString(), name = "ZicoTV", url = "http://125.125.125.125:8899/zico/playlist.m3u")
        )
    }
}

@Composable
fun rememberXuperAppState(
    viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory((LocalContext.current.applicationContext as XuperApplication).repository)
    )
): XuperAppState {
    val context = LocalContext.current
    val navState = rememberNavigationState(
        startRoute = XuperNavKey.Tv,
        topLevelRoutes = setOf(XuperNavKey.Tv, XuperNavKey.Favorites, XuperNavKey.Arena, XuperNavKey.ZicoTv, XuperNavKey.Ak47Sports, XuperNavKey.Lists, XuperNavKey.Config)
    )
    val navigator = remember { Navigator(navState) }
    return remember(viewModel, navigator) {
        XuperAppState(context, viewModel, navigator)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XuperApp() {
    val context = LocalContext.current
    val appState = rememberXuperAppState()
    val arenaViewModel: ArenaViewModel = viewModel()
    val zicoTvViewModel: com.example.xuper.ui.viewmodel.ZicoTvViewModel = viewModel()
    val ak47SportsViewModel: com.example.xuper.ui.viewmodel.Ak47SportsViewModel = viewModel()
    val layoutConfig = com.example.xuper.ui.components.rememberAdaptiveLayoutConfig()

    val currentChannels by appState.currentChannelsFlow.collectAsState()
    val availableCategories by appState.availableCategoriesFlow.collectAsState()
    val isLoading by appState.isLoadingFlow.collectAsState()
    val errorMessage by appState.errorMessageFlow.collectAsState()
    val favoriteChannels by appState.favoriteChannelsFlow.collectAsState()

    val searchQuery by appState.searchQueryFlow.collectAsState()
    val selectedCategory by appState.selectedCategoryFlow.collectAsState()
    val selectedListName by appState.selectedListNameFlow.collectAsState()

    LaunchedEffect(Unit) {
        appState.checkForUpdates()
    }

    var isSingleColumn by remember { mutableStateOf(true) }

    @Suppress("UNCHECKED_CAST")
    val entryProvider = entryProvider {
        entry<XuperNavKey.Tv> { _ ->
            if (errorMessage != null && currentChannels.isEmpty()) {
                ErrorState(errorMessage!!) {
                    appState.triggerRefresh()
                    arenaViewModel.loadData(autoScan = true, forceRefresh = true)
                }
            } else {
                MainTvScreen(
                    channels = currentChannels,
                    availableCategories = availableCategories,
                    selectedChannel = appState.selectedChannel,
                    isLoading = isLoading,
                    searchQuery = searchQuery.orEmpty(),
                    onSearchChange = { appState.setSearchQuery(it) },
                    selectedCategory = selectedCategory.orEmpty(),
                    onCategoryChange = { appState.setSelectedCategory(it) },
                    selectedListName = selectedListName.orEmpty(),
                    onListNameChange = { appState.setSelectedListName(it) },
                    m3uLists = appState.m3uLists,
                    onToggleFavorite = { appState.toggleFavorite(it) },
                    onChannelSelected = { channel -> appState.playChannelFullScreen(channel, currentChannels) },
                    onFullScreen = { appState.playChannelFullScreen(appState.selectedChannel) },
                    filterFocusRequester = appState.filterFocusRequester,
                    onRefreshM3U = { appState.triggerRefresh() },
                    isSingleColumn = isSingleColumn,
                    onToggleSingleColumn = { isSingleColumn = !isSingleColumn },
                    availableUpdate = appState.availableUpdate,
                    onOpenUpdateDialog = { appState.showUpdateDialog = true }
                )
            }
        }
        entry<XuperNavKey.Favorites> { _ ->
            FavoritesScreen(
                channels = favoriteChannels,
                onToggleFavorite = { appState.toggleFavorite(it) },
                onChannelSelected = { channel -> appState.playChannelFullScreen(channel, favoriteChannels) },
                isSingleColumn = isSingleColumn
            )
        }
        entry<XuperNavKey.Arena> { _ ->
            ArenaScreen(
                viewModel = arenaViewModel,
                onPlayFullScreen = { url -> appState.playUrlFullScreen(url) }
            )
        }
        entry<XuperNavKey.ZicoTv> { _ ->
            LaunchedEffect(searchQuery) {
                zicoTvViewModel.setSearchQuery(searchQuery.orEmpty())
            }
            ZicoTvScreen(
                viewModel = zicoTvViewModel,
                onPlayFullScreen = { url -> appState.playUrlFullScreen(url) },
                onPlayZicoChannel = { tokenInfo -> appState.playZicoChannelFullScreen(tokenInfo) },
                isSingleColumn = isSingleColumn
            )
        }
        entry<XuperNavKey.Ak47Sports> { _ ->
            LaunchedEffect(searchQuery) {
                ak47SportsViewModel.setSearchQuery(searchQuery.orEmpty())
            }
            Ak47SportsScreen(
                viewModel = ak47SportsViewModel,
                onPlayFullScreen = { url -> appState.playUrlFullScreen(url) },
                isSingleColumn = isSingleColumn
            )
        }
        entry<XuperNavKey.Lists> { _ ->
            ListsManagementScreen(
                lists = appState.m3uLists,
                onSaveLists = { appState.saveLists(it) }
            )
        }
        entry<XuperNavKey.Config> { _ ->
            XuperConfigScreen()
        }
        entry<XuperNavKey.Player> { key ->
            FullScreenPlayer(url = key.url, onClose = { appState.navigator.goBack() })
        }
    }

    val isPlayerActive = appState.fullScreenUrl != null

    SideEffect {
        (context as? MainActivity)?.isPlaybackActiveForPip = isPlayerActive || appState.selectedChannel != null
    }

    var lastBackTime by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = !isPlayerActive && appState.navigator.state.topLevelRoute == XuperNavKey.Tv) {
        val now = System.currentTimeMillis()
        if (now - lastBackTime < 2000) {
            (context as? ComponentActivity)?.finish()
        } else {
            lastBackTime = now
            android.widget.Toast.makeText(context, "Pulsa de nuevo para salir", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(isPlayerActive) {
        if (isPlayerActive && context is ComponentActivity) {
            val window = context.window
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            onDispose {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            onDispose { }
        }
    }

    if (isPlayerActive && appState.fullScreenUrl != null) {
        val canZap = appState.activeChannelList.size > 1
        val zicoInfo = appState.activeZicoTokenInfo
        val refreshBeforeExpiryMs = zicoInfo?.let { ((it.ttlSeconds - 900L).coerceAtLeast(60L)) * 1000L }
        FullScreenPlayer(
            url = appState.fullScreenUrl!!,
            channelTitle = appState.selectedChannel?.name ?: zicoInfo?.channelName,
            onPreviousChannel = if (canZap) ({ appState.playAdjacentChannel(-1) }) else null,
            onNextChannel = if (canZap) ({ appState.playAdjacentChannel(1) }) else null,
            onRefreshStreamUrl = zicoInfo?.let { info ->
                suspend {
                    val refreshed = zicoTvViewModel.getStreamToken(info.channelId, forceRefresh = true)
                    appState.playZicoChannelFullScreen(refreshed)
                    refreshed.streamUrl
                }
            },
            autoRefreshBeforeExpiryMs = refreshBeforeExpiryMs,
            onClose = { appState.closeFullScreen() }
        )
    } else if (layoutConfig.isCompact) {
        // Modo Móvil: Barra inferior limpia de 5 ítems + menú "Más"
        var showMoreSheet by remember { mutableStateOf(false) }
        val currentRoute = appState.navigator.state.topLevelRoute
        val moreRoutes = setOf(XuperNavKey.Ak47Sports, XuperNavKey.Lists, XuperNavKey.Config)

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val primaryItems = listOf(
                        Triple(XuperNavKey.Tv, Icons.Default.Tv, stringResourceAI("tv")),
                        Triple(XuperNavKey.Favorites, Icons.Default.Favorite, stringResourceAI("favorites")),
                        Triple(XuperNavKey.Arena, Icons.Default.SportsSoccer, "Arena"),
                        Triple(XuperNavKey.ZicoTv, Icons.Default.VpnKey, "ZicoTV")
                    )

                    primaryItems.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = { appState.navigator.navigate(route) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, maxLines = 1, softWrap = false) }
                        )
                    }

                    NavigationBarItem(
                        selected = currentRoute in moreRoutes,
                        onClick = { showMoreSheet = true },
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Más") },
                        label = { Text("Más", maxLines = 1, softWrap = false) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavDisplay(
                    entries = appState.navigator.state.toEntries(@Suppress("UNCHECKED_CAST") (entryProvider as (NavKey) -> NavEntry<NavKey>)),
                    onBack = { appState.navigator.goBack() }
                )
            }
        }

        if (showMoreSheet) {
            ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val moreItems = listOf(
                        Triple(XuperNavKey.Ak47Sports, Icons.Default.SportsSoccer, "AK47Sports"),
                        Triple(XuperNavKey.Lists, Icons.AutoMirrored.Filled.List, stringResourceAI("lists")),
                        Triple(XuperNavKey.Config, Icons.Default.Settings, stringResourceAI("settings"))
                    )
                    moreItems.forEach { (route, icon, label) ->
                        val isSelected = currentRoute == route
                        Surface(
                            onClick = {
                                showMoreSheet = false
                                appState.navigator.navigate(route)
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Modo Android TV / Tablet / Landscape: hueco fijo de 72dp + panel lateral superpuesto al expandir
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 72.dp)
            ) {
                NavDisplay(
                    entries = appState.navigator.state.toEntries(@Suppress("UNCHECKED_CAST") (entryProvider as (NavKey) -> NavEntry<NavKey>)),
                    onBack = { appState.navigator.goBack() }
                )
            }
            TvSidePanel(
                currentRoute = appState.navigator.state.topLevelRoute,
                onNavigate = { appState.navigator.navigate(it) },
                onRefresh = { appState.triggerRefresh() },
                isSingleColumn = isSingleColumn,
                onToggleView = { isSingleColumn = !isSingleColumn },
                searchQuery = searchQuery.orEmpty(),
                onSearchChange = { appState.setSearchQuery(it) },
                modifier = Modifier.align(Alignment.CenterStart),
                availableUpdate = appState.availableUpdate,
                onOpenUpdateDialog = { appState.showUpdateDialog = true }
            )
        }
    }

    if (appState.showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { appState.showLanguageDialog = false },
            onLanguageSelected = { lang ->
                LanguageManager.setLanguage(context, lang)
                appState.showLanguageDialog = false
            }
        )
    }

    if (appState.showUpdateDialog && appState.availableUpdate != null) {
        UpdateConfirmationDialog(
            updateInfo = appState.availableUpdate!!,
            onDismiss = { appState.showUpdateDialog = false },
            onConfirmInstall = {
                com.example.xuper.util.UpdateManager.downloadAndInstallApk(context, appState.availableUpdate!!)
                appState.showUpdateDialog = false
            },
            onOpenBrowser = {
                com.example.xuper.util.UpdateManager.openInBrowser(context, appState.availableUpdate!!.releaseNotesUrl)
            }
        )
    }
}

@Composable
fun TvSidePanel(
    currentRoute: NavKey,
    onNavigate: (NavKey) -> Unit,
    onRefresh: () -> Unit,
    isSingleColumn: Boolean,
    onToggleView: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    availableUpdate: com.example.xuper.util.AppUpdateInfo? = null,
    onOpenUpdateDialog: () -> Unit = {}
) {
    val context = LocalContext.current
    var isPanelFocused by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    val searchFieldFocusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    val isExpanded = isPanelFocused || isSearchExpanded || searchQuery.isNotEmpty()
    val panelWidth by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else 72.dp,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "panelWidth"
    )

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            kotlinx.coroutines.delay(50)
            try {
                searchFieldFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(panelWidth)
            .onFocusChanged { isPanelFocused = it.hasFocus },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isExpanded) 0.96f else 0.85f),
        tonalElevation = 6.dp,
        shadowElevation = if (isExpanded) 12.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            // Título App (Visible al expandir)
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    "XUPER TV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                )
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 1. Elementos de Navegación (Arriba)
            val navItems = listOf(
                Triple(XuperNavKey.Tv, Icons.Default.Tv, stringResourceAI("tv")),
                Triple(XuperNavKey.Favorites, Icons.Default.Favorite, stringResourceAI("favorites")),
                Triple(XuperNavKey.Arena, Icons.Default.SportsSoccer, "Arena"),
                Triple(XuperNavKey.ZicoTv, Icons.Default.VpnKey, "ZicoTV"),
                Triple(XuperNavKey.Ak47Sports, Icons.Default.SportsSoccer, "AK47Sports"),
                Triple(XuperNavKey.Lists, Icons.AutoMirrored.Filled.List, stringResourceAI("lists")),
                Triple(XuperNavKey.Config, Icons.Default.Settings, stringResourceAI("settings"))
            )

            navItems.forEach { (route, icon, label) ->
                val isSelected = currentRoute == route
                var isFocused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "navFocus")

                Surface(
                    onClick = { onNavigate(route) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isFocused) com.example.xuper.ui.theme.FocusBackground else if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (isFocused) Color.Black else if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .onFocusChanged {
                            isFocused = it.isFocused
                            if (it.hasFocus) isPanelFocused = true
                        }
                        .scale(scale)
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isExpanded) Arrangement.spacedBy(12.dp) else Arrangement.Center
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
                        if (isExpanded) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))

            // 1.5. Botón de Actualización Disponible (si hay nueva versión)
            if (availableUpdate != null) {
                var isUpdateFocused by remember { mutableStateOf(false) }
                val updateScale by animateFloatAsState(if (isUpdateFocused) 1.1f else 1f, label = "updateFocus")

                Surface(
                    onClick = onOpenUpdateDialog,
                    shape = MaterialTheme.shapes.small,
                    color = if (isUpdateFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isUpdateFocused) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .onFocusChanged {
                            isUpdateFocused = it.isFocused
                            if (it.hasFocus) isPanelFocused = true
                        }
                        .scale(updateScale)
                        .border(
                            width = if (isUpdateFocused) 2.dp else 0.dp,
                            color = if (isUpdateFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isExpanded) Arrangement.spacedBy(12.dp) else Arrangement.Center
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = "Actualizar App", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        if (isExpanded) {
                            Text("Actualizar (v${availableUpdate.version})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }

            // 2. Acciones: Refrescar + Cambio de Vista
            var isRefreshFocused by remember { mutableStateOf(false) }
            val refreshScale by animateFloatAsState(if (isRefreshFocused) 1.1f else 1f, label = "refreshFocus")
            Surface(
                onClick = onRefresh,
                shape = MaterialTheme.shapes.small,
                color = if (isRefreshFocused) com.example.xuper.ui.theme.FocusBackground else Color.Transparent,
                contentColor = if (isRefreshFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .onFocusChanged {
                        isRefreshFocused = it.isFocused
                        if (it.hasFocus) isPanelFocused = true
                    }
                    .scale(refreshScale)
                    .border(
                        width = if (isRefreshFocused) 2.dp else 0.dp,
                        color = if (isRefreshFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isExpanded) Arrangement.spacedBy(12.dp) else Arrangement.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar", modifier = Modifier.size(22.dp))
                    if (isExpanded) {
                        Text(stringResourceAI("refresh"), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }

            var isViewFocused by remember { mutableStateOf(false) }
            val viewScale by animateFloatAsState(if (isViewFocused) 1.1f else 1f, label = "viewFocus")
            Surface(
                onClick = onToggleView,
                shape = MaterialTheme.shapes.small,
                color = if (isViewFocused) com.example.xuper.ui.theme.FocusBackground else Color.Transparent,
                contentColor = if (isViewFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .onFocusChanged {
                        isViewFocused = it.isFocused
                        if (it.hasFocus) isPanelFocused = true
                    }
                    .scale(viewScale)
                    .border(
                        width = if (isViewFocused) 2.dp else 0.dp,
                        color = if (isViewFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isExpanded) Arrangement.spacedBy(12.dp) else Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSingleColumn) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = "Cambiar vista",
                        modifier = Modifier.size(22.dp)
                    )
                    if (isExpanded) {
                        Text(if (isSingleColumn) "Cuadrícula" else "Lista", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }

            // 3. Separador flexible
            Spacer(modifier = Modifier.height(12.dp))

            // 4. Lupa / Buscador abajo del todo
            if (isExpanded && (isSearchExpanded || searchQuery.isNotEmpty())) {
                var isSearchFieldFocused by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        onSearchChange(it)
                        if (currentRoute != XuperNavKey.Tv) {
                            onNavigate(XuperNavKey.Tv)
                        }
                    },
                    placeholder = { Text(stringResourceAI("search"), style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                onSearchChange("")
                                isSearchExpanded = false
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda", modifier = Modifier.size(16.dp))
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFieldFocusRequester)
                        .onFocusChanged {
                            isSearchFieldFocused = it.isFocused
                            if (it.hasFocus) isPanelFocused = true
                        }
                        .border(
                            width = if (isSearchFieldFocused) 2.dp else 0.dp,
                            color = if (isSearchFieldFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                )
            } else {
                var isSearchBtnFocused by remember { mutableStateOf(false) }
                val searchBtnScale by animateFloatAsState(if (isSearchBtnFocused) 1.1f else 1f, label = "searchFocus")

                Surface(
                    onClick = {
                        isSearchExpanded = true
                        if (currentRoute != XuperNavKey.Tv) {
                            onNavigate(XuperNavKey.Tv)
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    color = if (isSearchBtnFocused) com.example.xuper.ui.theme.FocusBackground else Color.Transparent,
                    contentColor = if (isSearchBtnFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFieldFocusRequester)
                        .onFocusChanged {
                            isSearchBtnFocused = it.isFocused
                            if (it.hasFocus) isPanelFocused = true
                        }
                        .scale(searchBtnScale)
                        .border(
                            width = if (isSearchBtnFocused) 2.dp else 0.dp,
                            color = if (isSearchBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isExpanded) Arrangement.spacedBy(12.dp) else Arrangement.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(22.dp))
                        if (isExpanded) {
                            Text(stringResourceAI("search"), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            }

            // 5. Versión de la App al pie (justo debajo del buscador)
            val currentAppVersion = remember { com.example.xuper.util.UpdateManager.getCurrentVersionName(context) }
            Spacer(modifier = Modifier.height(6.dp))
            if (isExpanded) {
                Text(
                    text = "v$currentAppVersion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            } else {
                Text(
                    text = "v${currentAppVersion.take(4)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(onDismiss: () -> Unit, onLanguageSelected: (AppLanguage) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResourceAI("language")) },
        text = {
            Column {
                AppLanguage.entries.forEach { lang ->
                    TextButton(
                        onClick = { onLanguageSelected(lang) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(lang.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResourceAI("cancel")) }
        }
    )
}

@Composable
fun FullScreenPlayer(
    url: String,
    channelTitle: String? = null,
    onPreviousChannel: (() -> Unit)? = null,
    onNextChannel: (() -> Unit)? = null,
    onRefreshStreamUrl: (suspend () -> String?)? = null,
    autoRefreshBeforeExpiryMs: Long? = null,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_CHANNEL_UP,
                        android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            onNextChannel?.invoke()
                            onNextChannel != null
                        }
                        android.view.KeyEvent.KEYCODE_CHANNEL_DOWN,
                        android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            onPreviousChannel?.invoke()
                            onPreviousChannel != null
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        UniversalPlayer(
            url = url,
            modifier = Modifier.fillMaxSize(),
            channelTitle = channelTitle,
            onPreviousChannel = onPreviousChannel,
            onNextChannel = onNextChannel,
            onRefreshStreamUrl = onRefreshStreamUrl,
            autoRefreshBeforeExpiryMs = autoRefreshBeforeExpiryMs,
            onFullScreen = null,
            registerBackHandler = true,
            onCloseFullScreen = onClose
        )
    }
}

@Composable
fun UpdateConfirmationDialog(
    updateInfo: com.example.xuper.util.AppUpdateInfo,
    onDismiss: () -> Unit,
    onConfirmInstall: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Actualización Disponible (v${updateInfo.version})", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Se ha encontrado una nueva versión de Xuper publicada en GitHub.\n\n¿Deseas descargar e instalar la versión ${updateInfo.fileName}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirmInstall) {
                Text("DESCARGAR E INSTALAR")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenBrowser) {
                    Text("VER EN GITHUB")
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResourceAI("cancel"))
                }
            }
        }
    )
}
