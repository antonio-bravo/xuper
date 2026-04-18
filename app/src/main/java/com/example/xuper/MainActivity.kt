package com.example.xuper

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.xuper.data.M3UParser
import com.example.xuper.model.Channel
import com.example.xuper.model.M3UList
import com.example.xuper.ui.components.ErrorState
import com.example.xuper.ui.components.SidebarItem
import com.example.xuper.ui.components.UniversalPlayer
import com.example.xuper.ui.screens.FavoritesScreen
import com.example.xuper.ui.screens.ListsManagementScreen
import com.example.xuper.ui.screens.MainTvScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xuper.ui.viewmodel.MainViewModel
import com.example.xuper.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        onBackground = Color.White
    )
    MaterialTheme(
        colorScheme = xuperColors,
        typography = Typography(),
        content = content
    )
}

enum class Screen {
    TV, LISTS, FAVORITES
}

@Composable
fun XuperApp() {
    val context = LocalContext.current
    val app = context.applicationContext as XuperApplication
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
    var isFullScreen by remember { mutableStateOf(false) }
    
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
            } catch (e: Exception) {}
        }
        
        if (list.isEmpty()) {
            list.add(M3UList(name = "TDT Channels", url = "https://www.tdtchannels.com/lists/tv.m3u8"))
            list.add(M3UList(name = "Lista Scraper Acestream", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lista_scraper_acestream_api.m3u"))
            list.add(M3UList(name = "Lacasadel_TikiTaka", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/lacasadel_tikitaka.m3u"))
            list.add(M3UList(name = "BatmanStream", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/batmanstream.m3u"))
            list.add(M3UList(name = "PirloTV", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/pirlotv.m3u"))
            list.add(M3UList(name = "Rojadirecta", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/rojadirecta.m3u"))
            list.add(M3UList(name = "SportP2P", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/sportp2p.m3u"))
            list.add(M3UList(name = "VipRow", url = "https://raw.githubusercontent.com/antonio-bravo/m3u/refs/heads/main/viprow.m3u"))
            
            // Fuentes de IPTV-org (Open Source)
            list.add(M3UList(name = "Cine & Series (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/movies.m3u"))
            list.add(M3UList(name = "Documentales (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/documentary.m3u"))
            list.add(M3UList(name = "Deportes (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/sports.m3u"))
            list.add(M3UList(name = "Kids (IPTV-org)", url = "https://iptv-org.github.io/iptv/categories/kids.m3u"))
        }
        mutableStateOf(list.toList())
    }

    var refreshTrigger by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
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
        sharedPrefs.edit().putString("m3u_lists", arr.toString()).apply()
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
            onClose = { isFullScreen = false }
        )
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Spacer(Modifier.height(16.dp))
                
                SidebarItem(
                    selected = currentScreen == Screen.TV,
                    onClick = { currentScreen = Screen.TV },
                    icon = Icons.Default.Tv,
                    label = "TV"
                )
                
                SidebarItem(
                    selected = currentScreen == Screen.FAVORITES,
                    onClick = { currentScreen = Screen.FAVORITES },
                    icon = Icons.Default.Favorite,
                    label = "Favoritos"
                )
                
                SidebarItem(
                    selected = currentScreen == Screen.LISTS,
                    onClick = { currentScreen = Screen.LISTS },
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "Listas"
                )
                
                SidebarItem(
                    selected = false,
                    onClick = { refreshTrigger++ },
                    icon = Icons.Default.Refresh,
                    label = "Actualizar"
                )
                
                SidebarItem(
                    selected = false,
                    onClick = { filterFocusRequester.requestFocus() },
                    icon = Icons.Default.FilterList,
                    label = "Filtros"
                )
                
                Spacer(Modifier.weight(1f))
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
                    Screen.LISTS -> ListsManagementScreen(
                        lists = m3uLists,
                        onSaveLists = saveLists
                    )
                    Screen.FAVORITES -> FavoritesScreen(
                        channels = favoriteChannels,
                        onToggleFavorite = { toggleFavorite(it) },
                        onChannelSelected = onPlayChannel
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenPlayer(url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler { onClose() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        UniversalPlayer(url = url)
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Cerrar", tint = Color.White)
        }
    }
}
