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
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    val isLoadingFlow = viewModel.isLoading
    val errorMessageFlow = viewModel.errorMessage
    val favoriteChannelsFlow = viewModel.favoriteChannels
    val searchQueryFlow = viewModel.searchQuery
    val selectedCategoryFlow = viewModel.selectedCategory
    val selectedListNameFlow = viewModel.selectedListName

    init {
        if (viewModel.allChannels.value.isEmpty()) {
            viewModel.refreshChannels(m3uLists)
        }
    }

    fun saveLists(newList: List<M3UList>) {
        m3uLists = newList
        val arr = JSONArray().apply {
            newList.forEach {
                put(JSONObject().apply {
                    put("id", it.id ?: "")
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

    fun playChannel(channel: Channel?) {
        selectedChannel = channel
    }

    fun playChannelFullScreen(channel: Channel?) {
        if (channel != null && !channel.url.isNullOrEmpty()) {
            navigator.navigate(XuperNavKey.Player(channel.url))
        }
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
            M3UList(id = UUID.randomUUID().toString(), name = "AK47 Sports", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_ak47sports.m3u")
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
        topLevelRoutes = setOf(XuperNavKey.Tv, XuperNavKey.Favorites, XuperNavKey.Arena, XuperNavKey.Lists, XuperNavKey.Config)
    )
    val navigator = remember { Navigator(navState) }
    return remember(viewModel, navigator) {
        XuperAppState(context, viewModel, navigator)
    }
}

@Composable
fun XuperApp() {
    val context = LocalContext.current
    val appState = rememberXuperAppState()
    val arenaViewModel: ArenaViewModel = viewModel()
    val layoutConfig = com.example.xuper.ui.components.rememberAdaptiveLayoutConfig()

    val currentChannels by appState.currentChannelsFlow.collectAsState()
    val isLoading by appState.isLoadingFlow.collectAsState()
    val errorMessage by appState.errorMessageFlow.collectAsState()
    val favoriteChannels by appState.favoriteChannelsFlow.collectAsState()

    val searchQuery by appState.searchQueryFlow.collectAsState()
    val selectedCategory by appState.selectedCategoryFlow.collectAsState()
    val selectedListName by appState.selectedListNameFlow.collectAsState()

    var isSingleColumn by remember { mutableStateOf(true) }

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
                    onChannelSelected = { channel -> appState.playChannel(channel) },
                    onFullScreen = { appState.playChannelFullScreen(appState.selectedChannel) },
                    filterFocusRequester = appState.filterFocusRequester,
                    onRefreshM3U = { appState.triggerRefresh() },
                    isSingleColumn = isSingleColumn,
                    onToggleSingleColumn = { isSingleColumn = !isSingleColumn }
                )
            }
        }
        entry<XuperNavKey.Favorites> { _ ->
            FavoritesScreen(
                channels = favoriteChannels,
                onToggleFavorite = { appState.toggleFavorite(it) },
                onChannelSelected = { channel -> appState.playChannelFullScreen(channel) }
            )
        }
        entry<XuperNavKey.Arena> { _ ->
            ArenaScreen(viewModel = arenaViewModel)
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

    if (layoutConfig.isCompact) {
        // Modo Móvil: Navegación en la barra inferior (NavigationBar)
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navItems = listOf(
                        Triple(XuperNavKey.Tv, Icons.Default.Tv, stringResourceAI("tv")),
                        Triple(XuperNavKey.Favorites, Icons.Default.Favorite, stringResourceAI("favorites")),
                        Triple(XuperNavKey.Arena, Icons.Default.SportsSoccer, "Arena"),
                        Triple(XuperNavKey.Lists, Icons.AutoMirrored.Filled.List, stringResourceAI("lists")),
                        Triple(XuperNavKey.Config, Icons.Default.Settings, stringResourceAI("settings"))
                    )

                    navItems.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = appState.navigator.state.topLevelRoute == route,
                            onClick = { appState.navigator.navigate(route) },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavDisplay(
                    entries = appState.navigator.state.toEntries(entryProvider as (NavKey) -> NavEntry<NavKey>),
                    onBack = { appState.navigator.goBack() }
                )
            }
        }
    } else {
        // Modo Android TV / Tablet / Landscape: Panel Lateral Completo a la Izquierda
        Row(modifier = Modifier.fillMaxSize()) {
            TvSidePanel(
                currentRoute = appState.navigator.state.topLevelRoute,
                onNavigate = { appState.navigator.navigate(it) },
                onRefresh = { appState.triggerRefresh() },
                isSingleColumn = isSingleColumn,
                onToggleView = { isSingleColumn = !isSingleColumn },
                searchQuery = searchQuery.orEmpty(),
                onSearchChange = { appState.setSearchQuery(it) }
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavDisplay(
                    entries = appState.navigator.state.toEntries(entryProvider as (NavKey) -> NavEntry<NavKey>),
                    onBack = { appState.navigator.goBack() }
                )
            }
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
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isSearchExpanded || searchQuery.isNotEmpty()) 220.dp else 190.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Título App
            Text(
                "XUPER TV",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
            )

            // 1. Elementos de Navegación (Arriba)
            val navItems = listOf(
                Triple(XuperNavKey.Tv, Icons.Default.Tv, stringResourceAI("tv")),
                Triple(XuperNavKey.Favorites, Icons.Default.Favorite, stringResourceAI("favorites")),
                Triple(XuperNavKey.Arena, Icons.Default.SportsSoccer, "Arena"),
                Triple(XuperNavKey.Lists, Icons.AutoMirrored.Filled.List, stringResourceAI("lists")),
                Triple(XuperNavKey.Config, Icons.Default.Settings, stringResourceAI("settings"))
            )

            navItems.forEach { (route, icon, label) ->
                val isSelected = currentRoute == route
                var isFocused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "navFocus")

                Surface(
                    onClick = { onNavigate(route) },
                    shape = MaterialTheme.shapes.small,
                    color = if (isFocused) com.example.xuper.ui.theme.FocusBackground else if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    contentColor = if (isFocused) Color.Black else if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .onFocusChanged { isFocused = it.isFocused }
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp))

            // 2. Acciones: Refrescar + Cambio de Vista
            var isRefreshFocused by remember { mutableStateOf(false) }
            val refreshScale by animateFloatAsState(if (isRefreshFocused) 1.05f else 1f, label = "refreshFocus")
            Surface(
                onClick = onRefresh,
                shape = MaterialTheme.shapes.small,
                color = if (isRefreshFocused) com.example.xuper.ui.theme.FocusBackground else Color.Transparent,
                contentColor = if (isRefreshFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .onFocusChanged { isRefreshFocused = it.isFocused }
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar", modifier = Modifier.size(22.dp))
                    Text(stringResourceAI("refresh"), style = MaterialTheme.typography.bodyMedium)
                }
            }

            var isViewFocused by remember { mutableStateOf(false) }
            val viewScale by animateFloatAsState(if (isViewFocused) 1.05f else 1f, label = "viewFocus")
            Surface(
                onClick = onToggleView,
                shape = MaterialTheme.shapes.small,
                color = if (isViewFocused) com.example.xuper.ui.theme.FocusBackground else Color.Transparent,
                contentColor = if (isViewFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .onFocusChanged { isViewFocused = it.isFocused }
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isSingleColumn) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "Cambiar vista",
                        modifier = Modifier.size(22.dp)
                    )
                    Text(if (isSingleColumn) "Cuadrícula" else "Lista", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // 3. SPACER QUE EMPUJA EL BUSCADOR AL PIE DEL PANEL IZQUIERDO
            Spacer(modifier = Modifier.weight(1f))

            // 4. Lupa / Buscador abajo del todo
            if (isSearchExpanded || searchQuery.isNotEmpty()) {
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
                        .onFocusChanged { isSearchFieldFocused = it.isFocused }
                        .border(
                            width = if (isSearchFieldFocused) 2.dp else 0.dp,
                            color = if (isSearchFieldFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                )
            } else {
                var isSearchBtnFocused by remember { mutableStateOf(false) }
                val searchBtnScale by animateFloatAsState(if (isSearchBtnFocused) 1.05f else 1f, label = "searchFocus")

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
                        .onFocusChanged { isSearchBtnFocused = it.isFocused }
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(22.dp))
                        Text(stringResourceAI("search"), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun SideNavigationRail(
    currentRoute: NavKey,
    onNavigate: (NavKey) -> Unit,
    onRefresh: () -> Unit,
    isSingleColumn: Boolean,
    onToggleView: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        val navItems = listOf(
            Triple(XuperNavKey.Tv, Icons.Default.Tv, stringResourceAI("tv")),
            Triple(XuperNavKey.Favorites, Icons.Default.Favorite, stringResourceAI("favorites")),
            Triple(XuperNavKey.Arena, Icons.Default.SportsSoccer, "Arena"),
            Triple(XuperNavKey.Lists, Icons.AutoMirrored.Filled.List, stringResourceAI("lists")),
            Triple(XuperNavKey.Config, Icons.Default.Settings, stringResourceAI("settings"))
        )

        navItems.forEach { (route, icon, label) ->
            var isFocused by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, label = "railFocus")
            NavigationRailItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .scale(scale)
                    .border(
                        width = if (isFocused) 2.dp else 0.dp,
                        color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp))

        var isRefreshFocused by remember { mutableStateOf(false) }
        val refreshScale by animateFloatAsState(if (isRefreshFocused) 1.15f else 1f, label = "refreshFocus")
        NavigationRailItem(
            selected = false,
            onClick = onRefresh,
            icon = { Icon(Icons.Default.Refresh, contentDescription = "Refrescar") },
            label = { Text(stringResourceAI("refresh")) },
            modifier = Modifier
                .onFocusChanged { isRefreshFocused = it.isFocused }
                .scale(refreshScale)
                .border(
                    width = if (isRefreshFocused) 2.dp else 0.dp,
                    color = if (isRefreshFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
        )

        var isViewFocused by remember { mutableStateOf(false) }
        val viewScale by animateFloatAsState(if (isViewFocused) 1.15f else 1f, label = "viewFocus")
        NavigationRailItem(
            selected = false,
            onClick = onToggleView,
            icon = {
                Icon(
                    imageVector = if (isSingleColumn) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "Cambiar vista"
                )
            },
            label = { Text(if (isSingleColumn) "Cuadrícula" else "Lista") },
            modifier = Modifier
                .onFocusChanged { isViewFocused = it.isFocused }
                .scale(viewScale)
                .border(
                    width = if (isViewFocused) 2.dp else 0.dp,
                    color = if (isViewFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .padding(bottom = 12.dp, start = 4.dp, end = 4.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isSearchExpanded || searchQuery.isNotEmpty()) {
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
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda", modifier = Modifier.size(16.dp))
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .width(160.dp)
                        .onFocusChanged { isSearchFieldFocused = it.isFocused }
                        .border(
                            width = if (isSearchFieldFocused) 2.dp else 0.dp,
                            color = if (isSearchFieldFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                )
            } else {
                var isSearchIconFocused by remember { mutableStateOf(false) }
                val searchIconScale by animateFloatAsState(if (isSearchIconFocused) 1.15f else 1f, label = "searchFocus")
                NavigationRailItem(
                    selected = false,
                    onClick = { isSearchExpanded = true },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    label = { Text(stringResourceAI("search")) },
                    modifier = Modifier
                        .onFocusChanged { isSearchIconFocused = it.isFocused }
                        .scale(searchIconScale)
                        .border(
                            width = if (isSearchIconFocused) 2.dp else 0.dp,
                            color = if (isSearchIconFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
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
fun FullScreenPlayer(url: String, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        UniversalPlayer(url = url)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
        }
    }
}
