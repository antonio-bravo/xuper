package com.example.xuper.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xuper.model.Channel
import com.example.xuper.ui.components.ChannelList

@Composable
fun FavoritesScreen(
    channels: List<Channel>,
    onToggleFavorite: (Channel) -> Unit,
    onChannelSelected: (Channel?) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Favoritos", style = MaterialTheme.typography.headlineMedium)
        if (channels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes canales favoritos")
            }
        } else {
            ChannelList(
                channels = channels,
                onChannelSelected = onChannelSelected,
                onToggleFavorite = onToggleFavorite
            )
        }
    }
}
