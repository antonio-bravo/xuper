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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
            M3UList(id = UUID.randomUUID().toString(), name = "Kids (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/kids.m3u")
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

    val currentChannels by appState.currentChannelsFlow.collectAsState()
    val isLoading by appState.isLoadingFlow.collectAsState()
    val errorMessage by appState.errorMessageFlow.collectAsState()
    val favoriteChannels by appState.favoriteChannelsFlow.collectAsState()

    val searchQuery by appState.searchQueryFlow.collectAsState()
    val selectedCategory by appState.selectedCategoryFlow.collectAsState()
    val selectedListName by appState.selectedListNameFlow.collectAsState()

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
                    onRefreshM3U = { appState.triggerRefresh() }
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

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = appState.navigator.state.topLevelRoute == XuperNavKey.Tv,
                onClick = { appState.navigator.navigate(XuperNavKey.Tv) },
                icon = { Icon(Icons.Default.Tv, contentDescription = null) },
                label = { Text(stringResourceAI("tv")) }
            )
            item(
                selected = appState.navigator.state.topLevelRoute == XuperNavKey.Favorites,
                onClick = { appState.navigator.navigate(XuperNavKey.Favorites) },
                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                label = { Text(stringResourceAI("favorites")) }
            )
            item(
                selected = appState.navigator.state.topLevelRoute == XuperNavKey.Arena,
                onClick = { appState.navigator.navigate(XuperNavKey.Arena) },
                icon = { Icon(Icons.Default.SportsSoccer, contentDescription = null) },
                label = { Text("Arena") }
            )
            item(
                selected = appState.navigator.state.topLevelRoute == XuperNavKey.Lists,
                onClick = { appState.navigator.navigate(XuperNavKey.Lists) },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                label = { Text(stringResourceAI("lists")) }
            )
            item(
                selected = appState.navigator.state.topLevelRoute == XuperNavKey.Config,
                onClick = { appState.navigator.navigate(XuperNavKey.Config) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text(stringResourceAI("settings")) }
            )
        }
    ) {
        NavDisplay(
            entries = appState.navigator.state.toEntries(entryProvider as (NavKey) -> NavEntry<NavKey>),
            onBack = { appState.navigator.goBack() }
        )
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
