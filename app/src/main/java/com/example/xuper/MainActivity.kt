package com.example.xuper

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xuper.ui.AppLanguage
import com.example.xuper.ui.LanguageManager
import com.example.xuper.ui.stringResourceAI
import com.example.xuper.model.Channel
import com.example.xuper.model.M3UList
import com.example.xuper.ui.components.ErrorState
import com.example.xuper.ui.components.SidebarItem
import com.example.xuper.ui.components.UniversalPlayer
import com.example.xuper.ui.screens.*
import com.example.xuper.ui.viewmodel.MainViewModel
import com.example.xuper.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageManager.init(this)
        setContent {
            XuperTheme {
                XuperApp()
            }
        }
    }
}

@Composable
fun XuperTheme(content: @Composable () -> Unit) {
    val xuperColors = darkColorScheme(
        primary = Color(0xFF00A8FF),
        secondary = Color(0xFF191919),
        surface = Color(0xFF121212),
        background = Color(0xFF0A0A0A),
        onPrimary = Color.White,
        onSurface = Color.White,
        onBackground = Color.White,
    )
    MaterialTheme(
        colorScheme = xuperColors,
        typography = Typography(),
        content = content
    )
}

enum class Screen {
    TV, LISTS, FAVORITES, ARENA, XUPER_CONFIG
}

@Composable
fun XuperApp() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val app = context.applicationContext as XuperApplication
    
    // Detección de TV o Móvil en Horizontal
    val isTv = remember {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useSidebar = isTv || isLandscape
    
    // Overscan Safe Area Padding for TV
    val overscanPadding = if (isTv) 32.dp else 0.dp

    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(app.repository)
    )
    
    val sharedPrefs = remember { context.getSharedPreferences("xuper_prefs", Context.MODE_PRIVATE) }
    
    var currentScreen by remember { mutableStateOf(Screen.TV) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    
    val currentChannels by viewModel.filteredChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val favoriteChannels by viewModel.favoriteChannels.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedListName by viewModel.selectedListName.collectAsState()
    var isFullScreen by remember { mutableStateOf(value = false) }
    var showLanguageDialog by remember { mutableStateOf(value = false) }
    
    var m3uLists by remember {
        val saved = sharedPrefs.getString("m3u_lists", null)
        val list = mutableListOf<M3UList>()
        if (saved != null) {
            try {
                val arr = JSONArray(saved)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(M3UList(obj.getString("id"), obj.getString("name"), obj.getString("url")))
                }
            } catch (e: Exception) {
            }
        }
        
        if (list.isEmpty()) {
            list.add(M3UList(name = "Arena4Viewer", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/arena4viewer.m3u"))
            list.add(M3UList(name = "_ipfs_io", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/ipfs_io.m3u"))
            list.add(M3UList(name = "TDT Channels", url = "https://www.tdtchannels.com/lists/tv.m3u8"))
            list.add(M3UList(name = "Lista Scraper Acestream", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_scraper_acestream_api.m3u"))
            list.add(M3UList(name = "Lacasadel_TikiTaka", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lacasadel_tikitaka.m3u"))
            list.add(M3UList(name = "BatmanStream", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/batmanstream.m3u"))
            list.add(M3UList(name = "PirloTV", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/pirlotv.m3u"))
            list.add(M3UList(name = "Rojadirecta", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/rojadirecta.m3u"))
            list.add(M3UList(name = "SportP2P", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/sportp2p.m3u"))
            list.add(M3UList(name = "VipRow", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/viprow.m3u"))
            list.add(M3UList(name = "IPFS Hashes", url = "https://ipfs.io/ipns/k51qzi5uqu5di462t7j4vu4akwfhvtjhy88qbupktvoacqfqe9uforjvhyi4wr/hashes.json"))
            list.add(M3UList(name = "TvPremiumHD", url = "http://tvpremiumhd.club/tv.m3u"))

            list.add(M3UList(name = "Cine & Series (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/movies.m3u"))
            list.add(M3UList(name = "Documentales (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/documentary.m3u"))
            list.add(M3UList(name = "Deportes (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/sports.m3u"))
            list.add(M3UList(name = "Kids (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/kids.m3u"))
        }

        if (list.none { it.name == "IPFS Hashes" }) {
            list.add(M3UList(name = "IPFS Hashes", url = "https://ipfs.io/ipns/k51qzi5uqu5di462t7j4vu4akwfhvtjhy88qbupktvoacqfqe9uforjvhyi4wr/hashes.json"))
        }

        mutableStateOf(list.toList())
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val filterFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(m3uLists, refreshTrigger) {
        viewModel.refreshChannels(m3uLists)
    }

    val saveLists = { newList: List<M3UList> ->
        m3uLists = newList
        val arr = JSONArray()
        newList.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("url", it.url)
            arr.put(obj)
        }
        sharedPrefs.edit { putString("m3u_lists", arr.toString()) }
    }

    val toggleFavorite: (Channel) -> Unit = { channel ->
        viewModel.toggleFavorite(channel)
    }

    val onPlayChannel: (Channel?) -> Unit = { channel ->
        selectedChannel = channel
    }

    if (isFullScreen && selectedChannel != null) {
        FullScreenPlayer(
            url = selectedChannel!!.url,
            isTv = isTv,
            onClose = { isFullScreen = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                if (!useSidebar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == Screen.TV,
                            onClick = { currentScreen = Screen.TV },
                            icon = { Icon(Icons.Default.Tv, contentDescription = null) },
                            label = { Text(stringResourceAI("tv")) }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.FAVORITES,
                            onClick = { currentScreen = Screen.FAVORITES },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                            label = { Text(stringResourceAI("favorites")) }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.ARENA,
                            onClick = { currentScreen = Screen.ARENA },
                            icon = { Icon(Icons.Default.SportsSoccer, contentDescription = null) },
                            label = { Text("Arena") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == Screen.LISTS || currentScreen == Screen.XUPER_CONFIG,
                            onClick = { currentScreen = Screen.LISTS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text(stringResourceAI("xuper")) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(
                        start = overscanPadding,
                        top = overscanPadding / 2, // Less on top usually looks better
                        end = overscanPadding,
                        bottom = overscanPadding
                    )
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useSidebar) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(if (isTv) 120.dp else 100.dp) // Wider sidebar for TV
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(16.dp))
                            SidebarItem(selected = currentScreen == Screen.TV, onClick = { currentScreen = Screen.TV }, icon = Icons.Default.Tv, label = stringResourceAI("tv"))
                            SidebarItem(selected = currentScreen == Screen.FAVORITES, onClick = { currentScreen = Screen.FAVORITES }, icon = Icons.Default.Favorite, label = stringResourceAI("favorites"))
                            SidebarItem(selected = currentScreen == Screen.ARENA, onClick = { currentScreen = Screen.ARENA }, icon = Icons.Default.SportsSoccer, label = "Arena")
                            Spacer(Modifier.weight(1f))
                            SidebarItem(selected = currentScreen == Screen.LISTS, onClick = { currentScreen = Screen.LISTS }, icon = Icons.AutoMirrored.Filled.List, label = stringResourceAI("lists"))
                            SidebarItem(selected = currentScreen == Screen.XUPER_CONFIG, onClick = { currentScreen = Screen.XUPER_CONFIG }, icon = Icons.Default.Settings, label = stringResourceAI("xuper"))
                            SidebarItem(selected = false, onClick = { refreshTrigger++ }, icon = Icons.Default.Refresh, label = stringResourceAI("refresh"))
                            SidebarItem(selected = false, onClick = { filterFocusRequester.requestFocus() }, icon = Icons.Default.FilterList, label = stringResourceAI("filters"))
                            SidebarItem(selected = false, onClick = { showLanguageDialog = true }, icon = Icons.Default.Language, label = stringResourceAI("language"))
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                            Screen.TV -> {
                                if (errorMessage != null && currentChannels.isEmpty()) {
                                    ErrorState(errorMessage!!) { refreshTrigger++ }
                                } else {
                                    MainTvScreen(
                                        channels = currentChannels,
                                        selectedChannel = selectedChannel,
                                        isLoading = isLoading,
                                        searchQuery = searchQuery,
                                        onSearchChange = { viewModel.setSearchQuery(it) },
                                        selectedCategory = selectedCategory,
                                        onCategoryChange = { viewModel.setSelectedCategory(it) },
                                        selectedListName = selectedListName,
                                        onListNameChange = { viewModel.setSelectedListName(it) },
                                        m3uLists = m3uLists,
                                        onToggleFavorite = { toggleFavorite(it) },
                                        onChannelSelected = onPlayChannel,
                                        onFullScreen = { isFullScreen = true },
                                        filterFocusRequester = filterFocusRequester
                                    )
                                }
                            }
                            Screen.LISTS -> ListsManagementScreen(lists = m3uLists, onSaveLists = saveLists)
                            Screen.FAVORITES -> FavoritesScreen(channels = favoriteChannels, onToggleFavorite = { toggleFavorite(it) }, onChannelSelected = onPlayChannel)
                            Screen.ARENA -> ArenaScreen()
                            Screen.XUPER_CONFIG -> XuperConfigScreen()
                        }
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { lang ->
                LanguageManager.setLanguage(context, lang)
                showLanguageDialog = false
            },
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
fun FullScreenPlayer(url: String, isTv: Boolean = false, onClose: () -> Unit) {
    val overscanPadding = if (isTv) 24.dp else 0.dp
    BackHandler(onBack = onClose)
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        UniversalPlayer(url = url)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .padding(end = overscanPadding, top = overscanPadding)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
        }
    }
}
