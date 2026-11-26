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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

import androidx.core.net.toUri
import com.example.xuper.util.PlayerUtils

fun openCastStream(context: android.content.Context, streamUrl: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(streamUrl.toUri(), "application/x-mpegurl")
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
    channelTitle: String? = null,
    onPreviousChannel: (() -> Unit)? = null,
    onNextChannel: (() -> Unit)? = null,
    onRefreshStreamUrl: (suspend () -> String?)? = null,
    autoRefreshBeforeExpiryMs: Long? = null,
    onFullScreen: (() -> Unit)? = null,
    registerBackHandler: Boolean = false,
    onCloseFullScreen: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val aceStreamManager = remember { com.example.xuper.data.AceStreamManager(context) }
    var isControlsVisible by remember { mutableStateOf(true) }

    // Auto-ocultar la barra superior tras 3.5s para dejar la imagen limpia
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            kotlinx.coroutines.delay(3500)
            isControlsVisible = false
        }
    }

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

            if (channelTitle != null || onPreviousChannel != null || onNextChannel != null) {
                AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 54.dp, start = 12.dp, end = 12.dp)
                ) {
                    ZappingControlsBar(
                        channelTitle = channelTitle,
                        onPreviousChannel = onPreviousChannel,
                        onNextChannel = onNextChannel
                    )
                }
            }
        }
    } else if (url.startsWith("http") && (url.contains(".html") || url.contains("php") || (!url.contains("m3u8") && !url.contains("mp4") && !url.contains("mkv") && !url.contains("ts")))) {
        WebPlayer(url = url, modifier = modifier)
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            VideoPlayer(
                url = url,
                modifier = Modifier.fillMaxSize(),
                onRefreshStreamUrl = onRefreshStreamUrl,
                autoRefreshBeforeExpiryMs = autoRefreshBeforeExpiryMs,
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

            if (channelTitle != null || onPreviousChannel != null || onNextChannel != null) {
                AnimatedVisibility(
                    visible = isControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 54.dp, start = 12.dp, end = 12.dp)
                ) {
                    ZappingControlsBar(
                        channelTitle = channelTitle,
                        onPreviousChannel = onPreviousChannel,
                        onNextChannel = onNextChannel
                    )
                }
            }
        }
    }
}

@Composable
private fun ZappingControlsBar(
    channelTitle: String?,
    onPreviousChannel: (() -> Unit)?,
    onNextChannel: (() -> Unit)?
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color.Black.copy(alpha = 0.72f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            onPreviousChannel?.let { prev ->
                var isPrevFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = prev,
                    modifier = Modifier
                        .size(32.dp)
                        .onFocusChanged { isPrevFocused = it.isFocused }
                        .border(
                            width = if (isPrevFocused) 2.dp else 0.dp,
                            color = if (isPrevFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Canal anterior", tint = Color.White)
                }
            }

            if (!channelTitle.isNullOrEmpty()) {
                Text(
                    text = channelTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 260.dp)
                )
            }

            onNextChannel?.let { next ->
                var isNextFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = next,
                    modifier = Modifier
                        .size(32.dp)
                        .onFocusChanged { isNextFocused = it.isFocused }
                        .border(
                            width = if (isNextFocused) 2.dp else 0.dp,
                            color = if (isNextFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente canal", tint = Color.White)
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
    onRefreshStreamUrl: (suspend () -> String?)? = null,
    autoRefreshBeforeExpiryMs: Long? = null,
    onFullScreen: (() -> Unit)? = null,
    registerBackHandler: Boolean = false,
    onCloseFullScreen: (() -> Unit)? = null,
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUrl by remember(url) { mutableStateOf(url) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var isControllerVisible by remember { mutableStateOf(true) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var hasRefreshedOnError by remember(currentUrl) { mutableStateOf(false) }

    // Renovación preventiva del stream (ej. ZicoTV 15 min antes de caducar el token de 3h)
    LaunchedEffect(currentUrl, onRefreshStreamUrl, autoRefreshBeforeExpiryMs) {
        if (onRefreshStreamUrl != null && autoRefreshBeforeExpiryMs != null && autoRefreshBeforeExpiryMs > 0L) {
            kotlinx.coroutines.delay(autoRefreshBeforeExpiryMs)
            try {
                val refreshed = onRefreshStreamUrl()
                if (!refreshed.isNullOrEmpty() && refreshed != currentUrl) {
                    android.util.Log.d("VideoPlayer", "Token renovado preventivamente sin cortar reproducción")
                    currentUrl = refreshed
                }
            } catch (e: Exception) {
                android.util.Log.w("VideoPlayer", "Error en renovación preventiva: ${e.message}")
            }
        }
    }

    // Garantizar que los controles nativos y el selector de audio se oculten tras 3.5s
    LaunchedEffect(isControllerVisible, showAudioDialog) {
        if (isControllerVisible && !showAudioDialog) {
            kotlinx.coroutines.delay(3500)
            playerViewRef?.hideController()
            isControllerVisible = false
            onControllerVisibilityChanged?.invoke(false)
        }
    }

    if (registerBackHandler) {
        BackHandler {
            val pv = playerViewRef
            if (isControllerVisible || (pv != null && pv.isControllerFullyVisible)) {
                pv?.hideController()
                isControllerVisible = false
                onControllerVisibilityChanged?.invoke(false)
            } else {
                onCloseFullScreen?.invoke()
            }
        }
    }

    var audioTracks by remember { mutableStateOf<List<AudioTrackOption>>(emptyList()) }
    var isBuffering by remember(currentUrl) { mutableStateOf(true) }
    var playerError by remember(currentUrl) { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    val mediaItem = remember(currentUrl) {
        buildMediaItemSafely(currentUrl)
    }

    // Reutilizar la misma instancia de ExoPlayer cuando cambie currentUrl (ej. fallback getstream)
    val exoPlayer = remember {
        com.example.xuper.util.ExoPlayerFactory.createPlayer(context)
    }

    LaunchedEffect(exoPlayer, mediaItem, retryTrigger) {
        try {
            isBuffering = true
            playerError = null
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            android.util.Log.e("VideoPlayer", "Error iniciando ExoPlayer para $currentUrl: ${e.message}", e)
            isBuffering = false
            playerError = e.localizedMessage ?: "Error al iniciar la reproducción"
        }
    }

    // Listener de estado, pistas de audio y fallback automático
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                android.util.Log.d("VideoPlayer", "State: $playbackState for $currentUrl")
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    playerError = null
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    isBuffering = false
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
                } else if (onRefreshStreamUrl != null && !hasRefreshedOnError) {
                    hasRefreshedOnError = true
                    scope.launch {
                        try {
                            val refreshed = onRefreshStreamUrl()
                            if (!refreshed.isNullOrEmpty()) {
                                android.util.Log.d("VideoPlayer", "Token renovado tras error de reproducción")
                                currentUrl = refreshed
                                return@launch
                            }
                        } catch (_: Exception) {}
                        isBuffering = false
                        playerError = error.localizedMessage ?: "No se pudo reproducir el stream"
                        onError?.invoke()
                    }
                } else {
                    isBuffering = false
                    playerError = error.localizedMessage ?: "No se pudo reproducir el stream"
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
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    keepScreenOn = true
                    this.resizeMode = resizeMode
                    controllerShowTimeoutMs = 3500
                    controllerAutoShow = true
                    controllerHideOnTouch = true
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
                playerView.resizeMode = resizeMode
                playerView.controllerShowTimeoutMs = 3500
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

        // Selector de Relación de Aspecto + Pistas de Audio
        AnimatedVisibility(
            visible = isControllerVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 64.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val aspectLabel = when (resizeMode) {
                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Rellenar"
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                    else -> "Ajustar"
                }
                var isAspectFocused by remember { mutableStateOf(false) }
                val aspectScale by animateFloatAsState(if (isAspectFocused) 1.1f else 1f, label = "aspectBtnScale")
                Surface(
                    onClick = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    color = Color.Black.copy(alpha = 0.75f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .onFocusChanged { isAspectFocused = it.isFocused }
                        .scale(aspectScale)
                        .border(
                            width = if (isAspectFocused) 2.dp else 0.dp,
                            color = if (isAspectFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Relación de aspecto", modifier = Modifier.size(16.dp))
                        Text(aspectLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (audioTracks.size > 1) {
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
            }
        }

        // Indicador de buffering centrado
        if (isBuffering && playerError == null) {
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.75f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Conectando al stream...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Tarjeta de error con Reintentar / Abrir Externo
        playerError?.let { err ->
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Error de reproducción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            playerError = null
                            retryTrigger++
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reintentar")
                        }
                        OutlinedButton(onClick = {
                            val aceId = PlayerUtils.getAceId(currentUrl)
                            if (aceId.isNotEmpty()) {
                                PlayerUtils.openInAceStreamApp(context, currentUrl)
                            } else {
                                openCastStream(context, currentUrl)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Externo")
                        }
                    }
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

        val builder = MediaItem.Builder().setUri(cleanUrl.toUri())
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
        MediaItem.Builder().setUri(cleanUrl.toUri()).build()
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
