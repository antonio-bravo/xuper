package com.example.xuper.ui.components

import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import com.example.xuper.util.PlayerUtils

fun openCastStream(context: android.content.Context, streamUrl: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(android.net.Uri.parse(streamUrl), "application/x-mpegurl")
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = android.content.Intent.createChooser(intent, "Emitir a TV / Chromecast (Google Cast & DLNA)")
    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, "No se encontró ningún receptor de emisión", android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun UniversalPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onFullScreen: (() -> Unit)? = null,
    registerBackHandler: Boolean = false,
    onCloseFullScreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val aceStreamManager = remember { com.example.xuper.data.AceStreamManager(context) }
    var isControlsVisible by remember { mutableStateOf(true) }

    // Mantener la pantalla encendida mientras el reproductor esté activo
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Detectar si es Acestream y obtener el ID
    val aceId = remember(url) { com.example.xuper.util.PlayerUtils.getAceId(url) }
    val isAceStream = aceId.isNotEmpty()

    if (isAceStream) {
        var activeProxy by remember { mutableStateOf(aceStreamManager.getSelectedProxy()) }
        var showProxyDialog by remember { mutableStateOf(false) }
        var failoverNotice by remember { mutableStateOf<String?>(null) }

        val hlsUrl = remember(url, activeProxy) {
            aceStreamManager.getFormattedUrlForProxy(url, activeProxy)
        }

        Box(modifier = modifier.fillMaxSize()) {
            VideoPlayer(
                url = hlsUrl,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    val nextProxy = aceStreamManager.getNextFailoverProxy(activeProxy)
                    if (nextProxy != null && nextProxy.id != activeProxy.id) {
                        failoverNotice = "⚡ Auto-Failover: Cambiando a ${nextProxy.name}..."
                        activeProxy = nextProxy
                        aceStreamManager.setSelectedProxy(nextProxy)
                    }
                },
                onFullScreen = onFullScreen,
                registerBackHandler = registerBackHandler,
                onCloseFullScreen = onCloseFullScreen,
                onControllerVisibilityChanged = { visible -> isControlsVisible = visible }
            )

            // Overlay superior con stats, selector de proxy, botón Fullscreen y botón Cast
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AceStreamStatsOverlay(
                        contentId = aceId,
                        aceStreamManager = aceStreamManager
                    )

                    // Botón selector de proxy durante reproducción
                    var isProxyBtnFocused by remember { mutableStateOf(false) }
                    val btnScale by animateFloatAsState(if (isProxyBtnFocused) 1.1f else 1f, label = "proxyBtnScale")

                    Surface(
                        onClick = { showProxyDialog = true },
                        shape = MaterialTheme.shapes.small,
                        color = Color.Black.copy(alpha = 0.75f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .onFocusChanged { isProxyBtnFocused = it.isFocused }
                            .scale(btnScale)
                            .border(
                                width = if (isProxyBtnFocused) 2.dp else 0.dp,
                                color = if (isProxyBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(if (activeProxy.isVpn) "🔒" else "🌐", style = MaterialTheme.typography.labelSmall)
                            Text(activeProxy.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar Proxy", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Botón Pantalla Completa (si aplica)
                    onFullScreen?.let { onFS ->
                        var isFSBtnFocused by remember { mutableStateOf(false) }
                        val fsScale by animateFloatAsState(if (isFSBtnFocused) 1.1f else 1f, label = "fsBtnScale")

                        Surface(
                            onClick = onFS,
                            shape = MaterialTheme.shapes.small,
                            color = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .onFocusChanged { isFSBtnFocused = it.isFocused }
                                .scale(fsScale)
                                .border(
                                    width = if (isFSBtnFocused) 2.dp else 0.dp,
                                    color = if (isFSBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Pantalla completa", modifier = Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }

                    // Botón Chromecast / Cast
                    var isCastBtnFocused by remember { mutableStateOf(false) }
                    val castScale by animateFloatAsState(if (isCastBtnFocused) 1.1f else 1f, label = "castBtnScale")

                    Surface(
                        onClick = { openCastStream(context, hlsUrl) },
                        shape = MaterialTheme.shapes.small,
                        color = Color.Black.copy(alpha = 0.75f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .onFocusChanged { isCastBtnFocused = it.isFocused }
                            .scale(castScale)
                            .border(
                                width = if (isCastBtnFocused) 2.dp else 0.dp,
                                color = if (isCastBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Cast, contentDescription = "Chromecast / Cast", modifier = Modifier.size(16.dp), tint = Color.White)
                            Text("CAST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón Cerrar (si aplica)
                    onCloseFullScreen?.let { onCloseFS ->
                        var isCloseBtnFocused by remember { mutableStateOf(false) }
                        val closeScale by animateFloatAsState(if (isCloseBtnFocused) 1.1f else 1f, label = "closeBtnScale")

                        Surface(
                            onClick = onCloseFS,
                            shape = MaterialTheme.shapes.small,
                            color = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .onFocusChanged { isCloseBtnFocused = it.isFocused }
                                .scale(closeScale)
                                .border(
                                    width = if (isCloseBtnFocused) 2.dp else 0.dp,
                                    color = if (isCloseBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }

            // Aviso flotante de Auto-Failover
            failoverNotice?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(4000)
                    failoverNotice = null
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (showProxyDialog) {
                ProxySelectorDialog(
                    proxies = aceStreamManager.getProxies(),
                    activeProxy = activeProxy,
                    onSelectProxy = { newProxy ->
                        activeProxy = newProxy
                        aceStreamManager.setSelectedProxy(newProxy)
                    },
                    onDismiss = { showProxyDialog = false }
                )
            }
        }
    } else if (url.startsWith("http") && (url.contains(".html") || url.contains("php") || (!url.contains("m3u8") && !url.contains("mp4") && !url.contains("mkv") && !url.contains("ts")))) {
        WebPlayer(url = url, modifier = modifier)
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            VideoPlayer(
                url = url,
                modifier = Modifier.fillMaxSize(),
                onFullScreen = onFullScreen,
                registerBackHandler = registerBackHandler,
                onCloseFullScreen = onCloseFullScreen,
                onControllerVisibilityChanged = { visible -> isControlsVisible = visible }
            )

            // Botones de control para reproductores estándar
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    onFullScreen?.let { onFS ->
                        var isFSBtnFocused by remember { mutableStateOf(false) }
                        val fsScale by animateFloatAsState(if (isFSBtnFocused) 1.1f else 1f, label = "fsBtnScale")

                        Surface(
                            onClick = onFS,
                            shape = MaterialTheme.shapes.small,
                            color = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .onFocusChanged { isFSBtnFocused = it.isFocused }
                                .scale(fsScale)
                                .border(
                                    width = if (isFSBtnFocused) 2.dp else 0.dp,
                                    color = if (isFSBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Pantalla completa", modifier = Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }

                    var isCastBtnFocused by remember { mutableStateOf(false) }
                    val castScale by animateFloatAsState(if (isCastBtnFocused) 1.1f else 1f, label = "castBtnScale")

                    Surface(
                        onClick = { openCastStream(context, url) },
                        shape = MaterialTheme.shapes.small,
                        color = Color.Black.copy(alpha = 0.75f),
                        contentColor = Color.White,
                        modifier = Modifier
                            .onFocusChanged { isCastBtnFocused = it.isFocused }
                            .scale(castScale)
                            .border(
                                width = if (isCastBtnFocused) 2.dp else 0.dp,
                                color = if (isCastBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Cast, contentDescription = "Chromecast / Cast", modifier = Modifier.size(16.dp), tint = Color.White)
                            Text("CAST", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    onCloseFullScreen?.let { onCloseFS ->
                        var isCloseBtnFocused by remember { mutableStateOf(false) }
                        val closeScale by animateFloatAsState(if (isCloseBtnFocused) 1.1f else 1f, label = "closeBtnScale")

                        Surface(
                            onClick = onCloseFS,
                            shape = MaterialTheme.shapes.small,
                            color = Color.Black.copy(alpha = 0.75f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .onFocusChanged { isCloseBtnFocused = it.isFocused }
                                .scale(closeScale)
                                .border(
                                    width = if (isCloseBtnFocused) 2.dp else 0.dp,
                                    color = if (isCloseBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", modifier = Modifier.size(16.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxySelectorDialog(
    proxies: List<com.example.xuper.data.AceStreamProxy>,
    activeProxy: com.example.xuper.data.AceStreamProxy,
    onSelectProxy: (com.example.xuper.data.AceStreamProxy) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Proxy AceStream", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                proxies.forEach { proxy ->
                    val isSelected = proxy.id == activeProxy.id
                    var isFocused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "proxyItemScale")

                    OutlinedButton(
                        onClick = {
                            onSelectProxy(proxy)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .scale(scale)
                            .border(
                                width = if (isFocused) 2.dp else if (isSelected) 1.5.dp else 0.dp,
                                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (proxy.isVpn) "🔒" else "🌐")
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(proxy.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("${proxy.host}:${proxy.port}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CERRAR") }
        }
    )
}

@Composable
fun WebPlayer(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                @Suppress("SetJavaScriptEnabled")
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                keepScreenOn = true
                
                loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { webView ->
        if (webView.url != url) {
            webView.loadUrl(url)
        }
    }
}

data class AudioTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onError: (() -> Unit)? = null,
    onFullScreen: (() -> Unit)? = null,
    registerBackHandler: Boolean = false,
    onCloseFullScreen: (() -> Unit)? = null,
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    var currentUrl by remember(url) { mutableStateOf(url) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var isControllerVisible by remember { mutableStateOf(true) }

    if (registerBackHandler) {
        BackHandler {
            val pv = playerViewRef
            if (pv != null && pv.isControllerFullyVisible) {
                pv.hideController()
            } else {
                onCloseFullScreen?.invoke()
            }
        }
    }

    var audioTracks by remember { mutableStateOf<List<AudioTrackOption>>(emptyList()) }
    var showAudioDialog by remember { mutableStateOf(false) }

    val mediaItem = remember(currentUrl) {
        buildMediaItemSafely(currentUrl)
    }

    // Usar factory optimizado en lugar de crear siempre nuevo
    val exoPlayer = remember(currentUrl) {
        try {
            com.example.xuper.util.ExoPlayerFactory.getOrCreate(context, reuseIfPossible = false).apply {
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoPlayer", "Error iniciando ExoPlayer para $currentUrl: ${e.message}", e)
            com.example.xuper.util.ExoPlayerFactory.getOrCreate(context, reuseIfPossible = false)
        }
    }

    // Listener de estado, pistas de audio y fallback automático
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                android.util.Log.d("VideoPlayer", "State: $playbackState for $currentUrl")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    playerViewRef?.postDelayed({
                        playerViewRef?.hideController()
                    }, 3000)
                }
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val list = mutableListOf<AudioTrackOption>()
                for (gIndex in 0 until tracks.groups.size) {
                    val group = tracks.groups[gIndex]
                    if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                        for (tIndex in 0 until group.length) {
                            val format = group.getTrackFormat(tIndex)
                            val lang = format.language.orEmpty().uppercase()
                            val label = when {
                                !format.label.isNullOrEmpty() -> format.label!!
                                lang.isNotEmpty() -> "Idioma: $lang"
                                else -> "Pista de Audio ${tIndex + 1}"
                            }
                            list.add(
                                AudioTrackOption(
                                    groupIndex = gIndex,
                                    trackIndex = tIndex,
                                    label = label,
                                    language = lang,
                                    isSelected = group.isTrackSelected(tIndex)
                                )
                            )
                        }
                    }
                }
                audioTracks = list
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("VideoPlayer", "PlaybackError para $currentUrl: ${error.message}", error)
                // Si falla la manifest de HLS, intentar fallback automático al endpoint directo getstream
                if (currentUrl.contains("manifest.m3u8")) {
                    val fallbackUrl = currentUrl.replace("manifest.m3u8", "getstream")
                    android.util.Log.d("VideoPlayer", "Probando fallback a: $fallbackUrl")
                    currentUrl = fallbackUrl
                } else {
                    onError?.invoke()
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            try {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            com.example.xuper.util.ExoPlayerFactory.releasePlayer()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    keepScreenOn = true
                    controllerShowTimeoutMs = 3000
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        val visible = (visibility == android.view.View.VISIBLE)
                        isControllerVisible = visible
                        onControllerVisibilityChanged?.invoke(visible)
                    })
                    if (onFullScreen != null) {
                        setFullscreenButtonClickListener { _ ->
                            onFullScreen.invoke()
                        }
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    playerViewRef = this
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerViewRef = playerView
                playerView.controllerShowTimeoutMs = 3000
                playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                    val visible = (visibility == android.view.View.VISIBLE)
                    isControllerVisible = visible
                    onControllerVisibilityChanged?.invoke(visible)
                })
                if (onFullScreen != null) {
                    playerView.setFullscreenButtonClickListener { _ ->
                        onFullScreen.invoke()
                    }
                } else {
                    playerView.setFullscreenButtonClickListener(null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Selector de Pistas de Audio (Visible cuando hay más de una pista detectada y los controles están visibles)
        AnimatedVisibility(
            visible = isControllerVisible && audioTracks.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 64.dp)
        ) {
            val selectedTrack = audioTracks.find { it.isSelected } ?: audioTracks.firstOrNull()
            var isAudioBtnFocused by remember { mutableStateOf(false) }
            val btnScale by animateFloatAsState(if (isAudioBtnFocused) 1.1f else 1f, label = "audioBtnScale")

            Surface(
                onClick = { showAudioDialog = true },
                shape = MaterialTheme.shapes.small,
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White,
                modifier = Modifier
                    .onFocusChanged { isAudioBtnFocused = it.isFocused }
                    .scale(btnScale)
                    .border(
                        width = if (isAudioBtnFocused) 2.dp else 0.dp,
                        color = if (isAudioBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = "Pista de Audio", modifier = Modifier.size(16.dp))
                    Text(
                        text = selectedTrack?.label ?: "Audio",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Cambiar Audio", modifier = Modifier.size(16.dp))
                }
            }
        }

        if (showAudioDialog) {
            AudioTrackSelectionDialog(
                tracks = audioTracks,
                onSelectTrack = { option ->
                    val currentTracks = exoPlayer.currentTracks
                    if (option.groupIndex < currentTracks.groups.size) {
                        val group = currentTracks.groups[option.groupIndex]
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(
                                androidx.media3.common.TrackSelectionOverride(
                                    group.mediaTrackGroup,
                                    option.trackIndex
                                )
                            )
                            .build()
                    }
                },
                onDismiss = { showAudioDialog = false }
            )
        }
    }
}

@Composable
fun AudioTrackSelectionDialog(
    tracks: List<AudioTrackOption>,
    onSelectTrack: (AudioTrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Pista de Audio", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tracks.forEach { option ->
                    var isFocused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "audioItemScale")

                    OutlinedButton(
                        onClick = {
                            onSelectTrack(option)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .scale(scale)
                            .border(
                                width = if (isFocused) 2.dp else if (option.isSelected) 1.5.dp else 0.dp,
                                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else if (option.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (option.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(option.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            if (option.isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Seleccionada", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CERRAR") }
        }
    )
}

@Composable
fun SidebarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "sidebarScale"
    )
    val backgroundColor by animateColorAsState(
        targetValue = when {
            selected -> com.example.xuper.ui.theme.SelectionIndicator
            isFocused -> com.example.xuper.ui.theme.FocusBackground
            else -> Color.Transparent
        },
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "sidebarBg"
    )
    val contentColor = when {
        selected -> Color.Black
        isFocused -> Color.Black
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    EnhancedErrorState(
        message = message,
        onRetry = onRetry,
        icon = Icons.Default.Error
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun buildMediaItemSafely(rawUrl: String): MediaItem {
    return try {
        val cleanUrl = if (rawUrl.contains("|")) rawUrl.substringBefore("|").trim() else rawUrl.trim()
        val headersPart = if (rawUrl.contains("|")) rawUrl.substringAfter("|").trim() else ""

        var licenseKey: String? = null
        if (headersPart.isNotEmpty()) {
            val params = headersPart.split("&", ";", "|")
            for (p in params) {
                if (p.contains("license_key=", ignoreCase = true)) {
                    licenseKey = p.substringAfter("license_key=").trim()
                }
            }
        }

        val builder = MediaItem.Builder().setUri(android.net.Uri.parse(cleanUrl))
        val lowerUrl = cleanUrl.lowercase()

        when {
            lowerUrl.contains(".m3u8") || lowerUrl.contains("manifest.m3u8") -> {
                builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            }
            lowerUrl.contains(".mpd") || lowerUrl.contains("manifest.mpd") -> {
                builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD)
            }
            lowerUrl.contains(".ts") || lowerUrl.contains("getstream") -> {
                builder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP2T)
            }
        }

        // Configuración de ClearKey DRM si existe clave kid:key
        if (!licenseKey.isNullOrEmpty() && licenseKey.contains(":")) {
            val parts = licenseKey.split(":")
            if (parts.size == 2) {
                val kidHex = parts[0].trim().replace("-", "").lowercase()
                val keyHex = parts[1].trim().replace("-", "").lowercase()
                try {
                    val kidB64 = hexToBase64Url(kidHex)
                    val keyB64 = hexToBase64Url(keyHex)
                    val drmJson = "{\"keys\":[{\"kty\":\"oct\",\"k\":\"$keyB64\",\"kid\":\"$kidB64\"}],\"type\":\"temporary\"}"
                    val clearKeyUri = "data:application/json;base64," + android.util.Base64.encodeToString(drmJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)

                    builder.setDrmConfiguration(
                        MediaItem.DrmConfiguration.Builder(androidx.media3.common.C.CLEARKEY_UUID)
                            .setLicenseUri(clearKeyUri)
                            .build()
                    )
                } catch (e: Exception) {
                    android.util.Log.w("VideoPlayer", "Error configurando ClearKey DRM: ${e.message}")
                }
            }
        }

        builder.build()
    } catch (e: Exception) {
        android.util.Log.e("VideoPlayer", "Error creando MediaItem para $rawUrl: ${e.message}")
        val cleanUrl = if (rawUrl.contains("|")) rawUrl.substringBefore("|").trim() else rawUrl.trim()
        MediaItem.Builder().setUri(android.net.Uri.parse(cleanUrl)).build()
    }
}

private fun hexToBase64Url(hex: String): String {
    val len = hex.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        i += 2
    }
    return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING or android.util.Base64.URL_SAFE)
}
