package com.example.xuper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xuper.model.Channel
import com.example.xuper.ui.components.ChannelList
import com.example.xuper.ui.components.EmptyState
import com.example.xuper.ui.components.rememberAdaptiveLayoutConfig

@Composable
fun FavoritesScreen(
    channels: List<Channel>,
    onToggleFavorite: (Channel) -> Unit,
    onChannelSelected: (Channel?) -> Unit
) {
    val layoutConfig = rememberAdaptiveLayoutConfig()
    var isSingleColumn by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(layoutConfig.contentPadding)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Favoritos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (channels.isNotEmpty()) {
                Text(
                    "${channels.size} ${if (channels.size == 1) "canal" else "canales"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (channels.isEmpty()) {
            EmptyState(
                message = "No tienes canales favoritos.\nMantén presionado sobre un canal para agregarlo.",
                icon = Icons.Default.FavoriteBorder
            )
        } else {
            ChannelList(
                channels = channels,
                onChannelSelected = onChannelSelected,
                onToggleFavorite = onToggleFavorite,
                isSingleColumn = isSingleColumn,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
