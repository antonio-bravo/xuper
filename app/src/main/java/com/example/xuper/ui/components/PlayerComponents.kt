package com.example.xuper.ui.components

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.xuper.data.AceStreamManager
import com.example.xuper.ui.stringResourceAI
import com.example.xuper.ui.theme.ScrimBackground
import com.example.xuper.util.PlayerUtils

/**
 * Diálogo unificado para elegir reproductor interno (incluyendo proxies AceStream),
 * reproductor externo o gestionar favoritos.
 */
@Composable
fun ChannelPlaybackDialog(
    title: String,
    url: String,
    subtitle: String? = null,
    peersCount: Int? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onPlayInternal: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isAcestream = remember(url) { PlayerUtils.isAceStreamUrl(url) }
    val aceManager = remember { AceStreamManager(context) }
    val proxies = remember { aceManager.getProxies() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(stringResourceAI("select_player"), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))

                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (peersCount != null) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "$peersCount peers activos",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isAcestream) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Canal Acestream P2P",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isAcestream) {
                    Text(
                        "Reproducción Interna (Proxy AceStream):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    proxies.forEach { proxy ->
                        var isProxyFocused by remember { mutableStateOf(false) }
                        val proxyScale by animateFloatAsState(if (isProxyFocused) 1.03f else 1f, label = "proxyScale")

                        Button(
                            onClick = {
                                aceManager.setSelectedProxy(proxy)
                                val formattedUrl = aceManager.getFormattedUrlForProxy(url, proxy)
                                onPlayInternal(formattedUrl)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isProxyFocused = it.isFocused }
                                .scale(proxyScale)
                                .border(
                                    width = if (isProxyFocused) 3.dp else 0.dp,
                                    color = if (isProxyFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = ButtonDefaults.shape
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isProxyFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primary,
                                contentColor = if (isProxyFocused) Color.Black else Color.White
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
                                        Text(proxy.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${proxy.host}:${proxy.port}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isProxyFocused) Color.DarkGray else Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                } else {
                    var isIntFocused by remember { mutableStateOf(false) }
                    val intScale by animateFloatAsState(if (isIntFocused) 1.05f else 1f, label = "intScale")
                    Button(
                        onClick = {
                            onPlayInternal(url)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isIntFocused = it.isFocused }
                            .scale(intScale)
                            .border(
                                width = if (isIntFocused) 3.dp else 0.dp,
                                color = if (isIntFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIntFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primary,
                            contentColor = if (isIntFocused) Color.Black else Color.White
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResourceAI("internal_player"), style = MaterialTheme.typography.titleMedium)
                    }
                }

                var isExtFocused by remember { mutableStateOf(false) }
                val extScale by animateFloatAsState(if (isExtFocused) 1.05f else 1f, label = "extScale")
                Button(
                    onClick = {
                        val rawUrl = url.trim()
                        val aceId = PlayerUtils.getAceId(rawUrl)

                        if (aceId.isNotEmpty()) {
                            val currentProxy = aceManager.getSelectedProxy()
                            PlayerUtils.openInAceStreamApp(context, currentProxy.getStreamUrl(aceId))
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(rawUrl.toUri(), "video/*")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Intent.createChooser(intent, "Open with...").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(this)
                                }
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isExtFocused = it.isFocused }
                        .scale(extScale)
                        .border(
                            width = if (isExtFocused) 3.dp else 0.dp,
                            color = if (isExtFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = ButtonDefaults.shape
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExtFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isExtFocused) Color.Black else Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(stringResourceAI("external_player"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Abrir en app externa (AceStream)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isExtFocused) Color.DarkGray else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                if (isFavorite != null && onToggleFavorite != null) {
                    Spacer(Modifier.height(4.dp))
                    var isFavFocused by remember { mutableStateOf(false) }
                    val favScale by animateFloatAsState(if (isFavFocused) 1.05f else 1f, label = "favScale")
                    OutlinedButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFavFocused = it.isFocused }
                            .scale(favScale)
                            .border(
                                width = if (isFavFocused) 3.dp else 0.dp,
                                color = if (isFavFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isFavFocused) com.example.xuper.ui.theme.FocusBackground else Color.Transparent,
                            contentColor = if (isFavFocused) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isFavorite) Color.Red else if (isFavFocused) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isFavorite) stringResourceAI("remove_favorite") else stringResourceAI("add_favorite"))
                    }
                }
            }
        },
        dismissButton = {
            var isCancelFocused by remember { mutableStateOf(false) }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.onFocusChanged { isCancelFocused = it.isFocused }
            ) {
                Text(
                    stringResourceAI("cancel"),
                    color = if (isCancelFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * Player responsive que se adapta al tamaño de pantalla
 */
@Composable
fun ResponsivePlayer(
    url: String,
    onFullScreen: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val layoutConfig = rememberAdaptiveLayoutConfig()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(layoutConfig.playerHeight)
            .clip(RoundedCornerShape(if (layoutConfig.isCompact) 0.dp else 12.dp))
            .clickable { onFullScreen() }
    ) {
        UniversalPlayer(url = url, modifier = Modifier.fillMaxSize(), onFullScreen = onFullScreen)

        // Controls overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            ScrimBackground,
                            Color.Transparent,
                            Color.Transparent,
                            ScrimBackground
                        )
                    )
                )
        ) {
            // Top controls
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                var isCloseFocused by remember { mutableStateOf(false) }
                val closeScale by animateFloatAsState(if (isCloseFocused) 1.15f else 1f, label = "closeFocus")
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .onFocusChanged { isCloseFocused = it.isFocused }
                        .scale(closeScale)
                        .background(
                            color = if (isCloseFocused) com.example.xuper.ui.theme.FocusBackground else Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50)
                        )
                        .border(
                            width = if (isCloseFocused) 2.dp else 0.dp,
                            color = if (isCloseFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = if (isCloseFocused) Color.Black else Color.White
                    )
                }
            }

            // Bottom controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                var isFullFocused by remember { mutableStateOf(false) }
                val fullScale by animateFloatAsState(if (isFullFocused) 1.15f else 1f, label = "fullFocus")
                IconButton(
                    onClick = onFullScreen,
                    modifier = Modifier
                        .onFocusChanged { isFullFocused = it.isFocused }
                        .scale(fullScale)
                        .background(
                            color = if (isFullFocused) com.example.xuper.ui.theme.FocusBackground else Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(50)
                        )
                        .border(
                            width = if (isFullFocused) 2.dp else 0.dp,
                            color = if (isFullFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Pantalla completa",
                        tint = if (isFullFocused) Color.Black else Color.White
                    )
                }
            }
        }
    }
}
