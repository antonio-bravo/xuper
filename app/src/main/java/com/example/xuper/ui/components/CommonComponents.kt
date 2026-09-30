package com.example.xuper.ui.components

import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import com.example.xuper.util.PlayerUtils

@Composable
fun UniversalPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val aceStreamManager = remember { com.example.xuper.data.AceStreamManager(context) }
    val config = remember { aceStreamManager.getConfig() }

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

    if (isAceStream && (url.contains("127.0.0.1:6878") && !url.contains("manifest.m3u8")) || url.startsWith("acestream://")) {
        // Mostrar mensaje para usar reproductor externo
        Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Contenido Acestream", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Text("Usa el REPRODUCTOR EXTERNO para este canal", color = Color.Gray)
                Spacer(Modifier.height(24.dp))

                var isFocused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "btnScale")

                Button(
                    onClick = { com.example.xuper.util.PlayerUtils.launchAceStream(context, "Acestream", url) },
                    modifier = Modifier
                        .onFocusChanged { isFocused = it.isFocused }
                        .scale(scale)
                        .border(
                            width = if (isFocused) 2.dp else 0.dp,
                            color = if (isFocused) Color.White else Color.Transparent,
                            shape = ButtonDefaults.shape
                        )
                ) {
                    Text("ABRIR EN ACE STREAM")
                }
            }
        }
    } else if (isAceStream) {
        // Reproducir Acestream con HLS usando el servidor configurado
        val hlsUrl = remember(url, config) {
            com.example.xuper.util.PlayerUtils.formatAceStreamHttpUrl(url, config.host, config.port)
        }

        Box(modifier = modifier.fillMaxSize()) {
            VideoPlayer(url = hlsUrl, modifier = Modifier.fillMaxSize())

            // Overlay con stats de Acestream
            AceStreamStatsOverlay(
                contentId = aceId,
                aceStreamManager = aceStreamManager,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    } else if (url.startsWith("http") && (url.contains(".html") || url.contains("php") || (!url.contains("m3u8") && !url.contains("mp4") && !url.contains("mkv") && !url.contains("ts")))) {
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

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Usar factory optimizado en lugar de crear siempre nuevo
    val exoPlayer = remember(url) {
        com.example.xuper.util.ExoPlayerFactory.getOrCreate(context, reuseIfPossible = false).apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    // Listener de estado para debugging
    DisposableEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // Log state changes para debugging
                android.util.Log.d("VideoPlayer", "State: $playbackState")
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            // NO liberar aquí si usamos pool, solo limpiar
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                keepScreenOn = true
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
