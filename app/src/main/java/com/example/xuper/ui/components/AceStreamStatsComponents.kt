package com.example.xuper.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xuper.data.AceStreamManager
import com.example.xuper.ui.theme.ScrimBackground
import com.example.xuper.ui.theme.Success
import kotlinx.coroutines.delay

/**
 * Componente que muestra estadísticas en tiempo real de Acestream
 */
@Composable
fun AceStreamStatsOverlay(
    contentId: String,
    aceStreamManager: AceStreamManager,
    modifier: Modifier = Modifier,
    autoHide: Boolean = true
) {
    var stats by remember { mutableStateOf<AceStreamManager.AceStreamStats?>(null) }
    var isVisible by remember { mutableStateOf(true) }

    // Actualizar stats cada 2 segundos
    LaunchedEffect(contentId) {
        while (true) {
            val newStats = aceStreamManager.getStreamStats(contentId)
            stats = newStats
            delay(2000) // Actualizar cada 2 segundos
        }
    }

    // Auto-ocultar después de 10 segundos si está configurado
    LaunchedEffect(autoHide) {
        if (autoHide) {
            delay(10000)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible && stats != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        stats?.let { currentStats ->
            AceStreamStatsContent(
                stats = currentStats,
                modifier = modifier
            )
        }
    }
}

/**
 * Contenido de las estadísticas
 */
@Composable
private fun AceStreamStatsContent(
    stats: AceStreamManager.AceStreamStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = ScrimBackground,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicador de estado (Live/Buffering)
        if (stats.isLive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Punto pulsante
                val alpha by animateFloatAsState(
                    targetValue = if (stats.status == "dl") 1f else 0.3f,
                    label = "liveIndicator"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha)
                        .background(Color.Red, CircleShape)
                )
                Text(
                    text = if (stats.status == "prebuf") "BUFFERING" else "EN VIVO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White
                )
            }
        } else if (stats.status == "prebuf") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Text(
                    text = "BUFFERING",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // Peers
        if (stats.peers > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${stats.peers}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }

        // Download Speed
        if (stats.downloadSpeed > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formatSpeed(stats.downloadSpeed),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // Upload Speed
        if (stats.uploadSpeed > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formatSpeed(stats.uploadSpeed),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Formato compacto de velocidad
 */
private fun formatSpeed(kbps: Float): String {
    return when {
        kbps >= 1024 -> String.format("%.1f MB/s", kbps / 1024)
        kbps >= 1 -> String.format("%.0f KB/s", kbps)
        else -> "0 KB/s"
    }
}

/**
 * Versión simple: solo indicador de peers
 */
@Composable
fun PeersIndicator(
    peers: Int,
    modifier: Modifier = Modifier
) {
    if (peers > 0) {
        Row(
            modifier = modifier
                .background(
                    color = Success.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "$peers",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Success
            )
        }
    }
}
