package com.example.xuper.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.xuper.model.Channel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    channel: Channel,
    onChannelSelected: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val isFavorite = channel.isFavorite

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "cardScale"
    )

    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isFocused) 12.dp else 2.dp,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "cardElevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                shape = MaterialTheme.shapes.medium,
            )
            .combinedClickable(
                onClick = { onChannelSelected(channel) },
                onLongClick = { onToggleFavorite(channel) },
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFocused -> com.example.xuper.ui.theme.FocusBackground
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.Black.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                if (channel.logo != null) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                    )
                } else {
                    Text(
                        text = channel.name.take(1),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200
                    )
                )
                if (channel.category.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = channel.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 2000
                        )
                    )
                }
            }
            IconButton(onClick = { onToggleFavorite(channel) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Color.Red else Color.Gray,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun ChannelList(
    channels: List<Channel>,
    onChannelSelected: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    isSingleColumn: Boolean = true,
    isLoading: Boolean = false
) {
    if (isLoading) {
        ChannelListSkeleton()
        return
    }

    val layoutConfig = rememberAdaptiveLayoutConfig()
    val gridColumns = if (isSingleColumn) 1 else layoutConfig.gridColumns

    if (gridColumns == 1) {
        LazyColumn(
            contentPadding = PaddingValues(layoutConfig.contentPadding),
            verticalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
            modifier = modifier,
        ) {
            items(
                items = channels,
                key = { it.url + it.name }
            ) { channel ->
                ChannelCard(
                    channel = channel,
                    onChannelSelected = onChannelSelected,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(layoutConfig.contentPadding),
            horizontalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
            verticalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
            modifier = modifier,
        ) {
            items(
                items = channels,
                key = { it.url + it.name }
            ) { channel ->
                ChannelCard(
                    channel = channel,
                    onChannelSelected = onChannelSelected,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}
