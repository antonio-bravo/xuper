package com.example.xuper

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
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
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}

enum class Screen {
    TV, LISTS, FAVORITES
}

@Composable
fun XuperApp() {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("xuper_prefs", Context.MODE_PRIVATE) }
    
    var currentScreen by remember { mutableStateOf(Screen.TV) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var channels by remember { mutableStateOf(listOf<Channel>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }
    var isFullScreen by remember { mutableStateOf(false) }
    
    var favoriteUrls by remember { 
        mutableStateOf(sharedPrefs.getStringSet("favorites", emptySet()) ?: emptySet()) 
    }
    
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
        }
        mutableStateOf(list.toList())
    }

    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(m3uLists) {
        scope.launch {
            isLoading = true
            val allChannels = mutableListOf<Channel>()
            try {
                for (source in m3uLists) {
                    val fetched = M3UParser.fetchAndParse(source.url)
                    allChannels.addAll(fetched)
                }
                channels = allChannels
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
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

    val toggleFavorite: (String) -> Unit = { url ->
        val newFavorites = if (favoriteUrls.contains(url)) favoriteUrls - url else favoriteUrls + url
        favoriteUrls = newFavorites
        sharedPrefs.edit().putStringSet("favorites", newFavorites).apply()
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
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Spacer(Modifier.height(16.dp))
                NavigationRailItem(
                    selected = currentScreen == Screen.TV,
                    onClick = { currentScreen = Screen.TV },
                    icon = { Icon(Icons.Default.Tv, contentDescription = "TV") },
                    label = { Text("TV") }
                )
                NavigationRailItem(
                    selected = currentScreen == Screen.FAVORITES,
                    onClick = { currentScreen = Screen.FAVORITES },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favoritos") },
                    label = { Text("Favoritos") }
                )
                NavigationRailItem(
                    selected = currentScreen == Screen.LISTS,
                    onClick = { currentScreen = Screen.LISTS },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Listas") },
                    label = { Text("Listas") }
                )
                Spacer(Modifier.weight(1f))
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentScreen) {
                    Screen.TV -> MainTvScreen(
                        channels = channels,
                        selectedChannel = selectedChannel,
                        isLoading = isLoading,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        favoriteUrls = favoriteUrls,
                        onToggleFavorite = toggleFavorite,
                        onChannelSelected = onPlayChannel,
                        onFullScreen = { isFullScreen = true }
                    )
                    Screen.LISTS -> ListsManagementScreen(
                        lists = m3uLists,
                        onSaveLists = saveLists
                    )
                    Screen.FAVORITES -> FavoritesScreen(
                        channels = channels,
                        favoriteUrls = favoriteUrls,
                        onToggleFavorite = toggleFavorite,
                        onChannelSelected = onPlayChannel
                    )
                }
            }
        }
    }
}

@Composable
fun MainTvScreen(
    channels: List<Channel>,
    selectedChannel: Channel?,
    isLoading: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    favoriteUrls: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onChannelSelected: (Channel?) -> Unit,
    onFullScreen: () -> Unit
) {
    var showPlayerDialog by remember { mutableStateOf<Channel?>(null) }
    val context = LocalContext.current

    Column {
        if (selectedChannel != null) {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                UniversalPlayer(url = selectedChannel.url)
                Row(modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                    IconButton(onClick = onFullScreen) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Full Screen", tint = Color.White)
                    }
                }
                IconButton(
                    onClick = { onChannelSelected(null) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator() else Text("Selecciona un canal para reproducir")
            }
        }

        // Buscador
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            placeholder = { Text("Buscar canal...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        // Filtro de Categorías
        val categories = remember(channels) { 
            listOf("Todos") + channels.map { it.category }.distinct().sorted() 
        }
        LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }

        val filteredChannels = remember(channels, searchQuery, selectedCategory) {
            channels.filter { 
                (selectedCategory == "Todos" || it.category == selectedCategory) &&
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        ChannelList(
            channels = filteredChannels,
            favoriteUrls = favoriteUrls,
            onChannelSelected = { showPlayerDialog = it },
            onToggleFavorite = onToggleFavorite
        )
    }

    showPlayerDialog?.let { channel ->
        AlertDialog(
            onDismissRequest = { showPlayerDialog = null },
            title = { Text("Reproducir Canal") },
            text = { Text("¿Deseas usar el reproductor interno o uno externo?") },
            confirmButton = {
                Button(onClick = {
                    onChannelSelected(channel)
                    showPlayerDialog = null
                }) { Text("Interno") }
            },
            dismissButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(Uri.parse(channel.url), "video/*")
                    try {
                        context.startActivity(Intent.createChooser(intent, "Abrir con"))
                    } catch (e: Exception) {
                        // Fallback if video/* doesn't work for acestream://
                        if (channel.url.startsWith("acestream://")) {
                            val aceIntent = Intent(Intent.ACTION_VIEW, Uri.parse(channel.url))
                            context.startActivity(aceIntent)
                        }
                    }
                    showPlayerDialog = null
                }) { Text("Externo") }
            }
        )
    }
}

@Composable
fun ListsManagementScreen(
    lists: List<M3UList>,
    onSaveLists: (List<M3UList>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newListName by remember { mutableStateOf("") }
    var newListUrl by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gestión de Listas M3U", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
        
        LazyColumn {
            items(lists) { list ->
                ListItem(
                    headlineContent = { Text(list.name) },
                    supportingContent = { Text(list.url) },
                    trailingContent = {
                        IconButton(onClick = {
                            onSaveLists(lists.filter { it.id != list.id })
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Red)
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Añadir Lista M3U") },
            text = {
                Column {
                    TextField(value = newListName, onValueChange = { newListName = it }, label = { Text("Nombre") })
                    Spacer(Modifier.height(8.dp))
                    TextField(value = newListUrl, onValueChange = { newListUrl = it }, label = { Text("URL") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newListName.isNotBlank() && newListUrl.isNotBlank()) {
                        onSaveLists(lists + M3UList(name = newListName, url = newListUrl))
                        newListName = ""
                        newListUrl = ""
                        showAddDialog = false
                    }
                }) { Text("Añadir") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun FavoritesScreen(
    channels: List<Channel>,
    favoriteUrls: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onChannelSelected: (Channel?) -> Unit
) {
    val favoriteChannels = remember(channels, favoriteUrls) {
        channels.filter { favoriteUrls.contains(it.url) }
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Favoritos", style = MaterialTheme.typography.headlineMedium)
        if (favoriteChannels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes canales favoritos")
            }
        } else {
            ChannelList(
                channels = favoriteChannels,
                favoriteUrls = favoriteUrls,
                onChannelSelected = onChannelSelected,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}

@Composable
fun UniversalPlayer(url: String, modifier: Modifier = Modifier) {
    if (url.startsWith("http") && (url.contains(".html") || url.contains("php") || !url.contains("m3u8") && !url.contains("mp4") && !url.contains("mkv") && !url.contains("ts"))) {
        WebPlayer(url = url, modifier = modifier)
    } else {
        VideoPlayer(url = url, modifier = modifier)
    }
}

@Composable
fun WebPlayer(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                
                loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
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

@Composable
fun ChannelList(
    channels: List<Channel>,
    favoriteUrls: Set<String>,
    onChannelSelected: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    LazyColumn {
        items(channels) { channel ->
            val isFavorite = favoriteUrls.contains(channel.url)
            ListItem(
                headlineContent = { Text(channel.name) },
                supportingContent = { Text(channel.category) },
                leadingContent = {
                    if (channel.logo != null) {
                        AsyncImage(
                            model = channel.logo,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Text(channel.name.take(1))
                        }
                    }
                },
                trailingContent = {
                    IconButton(onClick = { onToggleFavorite(channel.url) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) Color.Red else Color.Gray
                        )
                    }
                },
                modifier = Modifier.clickable { onChannelSelected(channel) }
            )
            HorizontalDivider()
        }
    }
}
