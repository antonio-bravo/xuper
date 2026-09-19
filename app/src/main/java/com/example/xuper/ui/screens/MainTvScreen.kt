package com.example.xuper.ui.screens

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.xuper.model.Channel
import com.example.xuper.model.M3UList
import com.example.xuper.ui.LanguageManager
import com.example.xuper.ui.stringResourceAI
import com.example.xuper.ui.components.ChannelList
import com.example.xuper.ui.components.UniversalPlayer

import com.example.xuper.util.PlayerUtils

@Composable
fun MainTvScreen(
    channels: List<Channel>,
    selectedChannel: Channel?,
    isLoading: Boolean,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedListName: String,
    onListNameChange: (String) -> Unit,
    m3uLists: List<M3UList>,
    onToggleFavorite: (Channel) -> Unit,
    onChannelSelected: (Channel?) -> Unit,
    onFullScreen: () -> Unit,
    filterFocusRequester: FocusRequester,
    onRefreshM3U: () -> Unit,
) {
    var showPlayerDialog by remember { mutableStateOf<Channel?>(null) }
    var isSingleColumn by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Column {
        if (selectedChannel != null) {
            var isPlayerFocused by remember { mutableStateOf(false) }
            val playerScale by animateFloatAsState(if (isPlayerFocused) 1.02f else 1f, label = "playerScale")
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .onFocusChanged { isPlayerFocused = it.isFocused }
                    .scale(playerScale)
                    .border(
                        width = if (isPlayerFocused) 4.dp else 0.dp,
                        color = if (isPlayerFocused) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onFullScreen() }
                    .focusable(),
            ) {
                UniversalPlayer(url = selectedChannel.url)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                ) {
                    var isFsFocused by remember { mutableStateOf(value = false) }
                    val fsScale by animateFloatAsState(if (isFsFocused) 1.2f else 1f, label = "fsScale")
                    IconButton(
                        onClick = onFullScreen,
                        modifier = Modifier
                            .onFocusChanged { isFsFocused = it.isFocused }
                            .scale(fsScale)
                            .border(
                                width = if (isFsFocused) 2.dp else 0.dp,
                                color = if (isFsFocused) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Full Screen",
                            tint = Color.White
                        )
                    }
                }
                var isCloseFocused by remember { mutableStateOf(value = false) }
                val closeScale by animateFloatAsState(if (isCloseFocused) 1.2f else 1f, label = "closeScale")
                IconButton(
                    onClick = { onChannelSelected(null) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .onFocusChanged { isCloseFocused = it.isFocused }
                        .scale(closeScale)
                        .border(
                            width = if (isCloseFocused) 2.dp else 0.dp,
                            color = if (isCloseFocused) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) CircularProgressIndicator() else Text(stringResourceAI("select_channel_msg"))
            }
        }

        // Filtro de Listas (Solo las activas/enabled)
        val listSources = remember(m3uLists) {
            listOf(LanguageManager.getString("all_lists")) + m3uLists.filter { it.enabled }.map { it.name }
        }
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var isTvRefreshFocused by remember { mutableStateOf(false) }
            val tvRefreshScale by animateFloatAsState(if (isTvRefreshFocused) 1.15f else 1f, label = "tvRefreshScale")
            IconButton(
                onClick = onRefreshM3U,
                modifier = Modifier
                    .onFocusChanged { isTvRefreshFocused = it.isFocused }
                    .scale(tvRefreshScale)
                    .border(
                        width = if (isTvRefreshFocused) 2.dp else 0.dp,
                        color = if (isTvRefreshFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isTvRefreshFocused) Color.White else Color.Transparent,
                    contentColor = if (isTvRefreshFocused) Color.Black else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refrescar Listas")
            }

            var isToggleLayoutFocused by remember { mutableStateOf(false) }
            val toggleLayoutScale by animateFloatAsState(if (isToggleLayoutFocused) 1.15f else 1f, label = "toggleLayoutScale")
            IconButton(
                onClick = { isSingleColumn = !isSingleColumn },
                modifier = Modifier
                    .onFocusChanged { isToggleLayoutFocused = it.isFocused }
                    .scale(toggleLayoutScale)
                    .border(
                        width = if (isToggleLayoutFocused) 2.dp else 0.dp,
                        color = if (isToggleLayoutFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isToggleLayoutFocused) Color.White else Color.Transparent,
                    contentColor = if (isToggleLayoutFocused) Color.Black else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isSingleColumn) Icons.Default.GridView else Icons.Default.ViewList,
                    contentDescription = "Cambiar vista"
                )
            }

            LazyRow(modifier = Modifier.weight(1f)) {
                items(listSources.size) { index ->
                    val name = listSources[index]
                    var isFocused by remember { mutableStateOf(value = false) }
                    val chipScale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "chipScale")
                    FilterChip(
                        selected = selectedListName == name,
                        onClick = { onListNameChange(name) },
                        label = { Text(name) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .then(if (index == 0) Modifier.focusRequester(filterFocusRequester) else Modifier)
                            .onFocusChanged { isFocused = it.isFocused }
                            .scale(chipScale)
                            .border(
                                width = if (isFocused) 3.dp else 0.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Filtro de Categorías
        val categories = remember(channels, selectedListName) {
            val allListsLabel = LanguageManager.getString("all_lists")
            val base = if (selectedListName == allListsLabel) channels else channels.filter { it.sourceListName == selectedListName }
            listOf(LanguageManager.getString("all_categories")) + base.asSequence().map { it.category }.distinct().sorted().toList()
        }
        LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            items(categories.size) { index ->
                val category = categories[index]
                var isFocused by remember { mutableStateOf(value = false) }
                val catScale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "catScale")
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(category) },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .onFocusChanged { isFocused = it.isFocused }
                        .scale(catScale)
                        .border(
                            width = if (isFocused) 3.dp else 0.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        val filteredChannels = remember(channels, searchQuery, selectedCategory, selectedListName) {
            val allListsLabel = LanguageManager.getString("all_lists")
            val allCategoriesLabel = LanguageManager.getString("all_categories")
            channels.filter {
                ((selectedListName == allListsLabel || it.sourceListName == selectedListName) &&
                        (selectedCategory == allCategoriesLabel || it.category == selectedCategory) &&
                        it.name.contains(searchQuery, ignoreCase = true))
            }.distinctBy { it.url + it.name + it.category }
        }

        ChannelList(
            channels = filteredChannels,
            onChannelSelected = { showPlayerDialog = it },
            onToggleFavorite = onToggleFavorite,
            isSingleColumn = isSingleColumn,
            modifier = Modifier.weight(1f)
        )

        // Buscador movido al final
        var isSearchFocused by remember { mutableStateOf(value = false) }
        val searchScale by animateFloatAsState(if (isSearchFocused) 1.02f else 1f, label = "searchScale")
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .onFocusChanged { isSearchFocused = it.isFocused }
                .scale(searchScale)
                .border(
                    width = if (isSearchFocused) 3.dp else 0.dp,
                    color = if (isSearchFocused) Color.White else Color.Transparent,
                    shape = MaterialTheme.shapes.small,
                ),
            placeholder = { Text(stringResourceAI("search_placeholder")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.secondary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )
    }

    showPlayerDialog?.let { channel ->
        AlertDialog(
            onDismissRequest = { showPlayerDialog = null },
            title = { Text(channel.name, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    Text(stringResourceAI("select_player"))
                    if (channel.category != "Otros") {
                        Text("${stringResourceAI("category")}: ${channel.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var isIntFocused by remember { mutableStateOf(value = false) }
                    val intScale by animateFloatAsState(if (isIntFocused) 1.05f else 1f, label = "intScale")
                    Button(
                        onClick = {
                            onChannelSelected(channel)
                            showPlayerDialog = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isIntFocused = it.isFocused }
                            .scale(intScale)
                            .border(
                                width = if (isIntFocused) 4.dp else 0.dp,
                                color = if (isIntFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = ButtonDefaults.shape,
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIntFocused) Color.White else MaterialTheme.colorScheme.primary,
                            contentColor = if (isIntFocused) Color.Black else Color.White,
                        ),
                    ) {
                        Text(stringResourceAI("internal_player"), style = MaterialTheme.typography.titleMedium)
                    }

                    var isExtFocused by remember { mutableStateOf(value = false) }
                    val extScale by animateFloatAsState(if (isExtFocused) 1.05f else 1f, label = "extScale")
                    Button(
                        onClick = {
                            val rawUrl = channel.url.trim()
                            val aceId = PlayerUtils.getAceId(rawUrl)

                            if (aceId.isNotEmpty()) {
                                 PlayerUtils.openInAceStreamApp(
                                     context,
                                     "http://127.0.0.1:6878/ace/getstream?id=$aceId"
                                 )
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
                            showPlayerDialog = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isExtFocused = it.isFocused }
                            .scale(extScale)
                            .border(
                                width = if (isExtFocused) 4.dp else 0.dp,
                                color = if (isExtFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = ButtonDefaults.shape,
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExtFocused) Color.White else MaterialTheme.colorScheme.primary,
                            contentColor = if (isExtFocused) Color.Black else Color.White,
                        ),
                    ) {
                        Text(stringResourceAI("external_player"), style = MaterialTheme.typography.titleMedium)
                    }

                    var isFavFocused by remember { mutableStateOf(value = false) }
                    val favScale by animateFloatAsState(if (isFavFocused) 1.05f else 1f, label = "favScale")
                    OutlinedButton(
                        onClick = {
                            onToggleFavorite(channel)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFavFocused = it.isFocused }
                            .scale(favScale)
                            .border(
                                width = if (isFavFocused) 4.dp else 0.dp,
                                color = if (isFavFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = ButtonDefaults.shape,
                            ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isFavFocused) Color.White else Color.Transparent,
                            contentColor = if (isFavFocused) Color.Black else Color.White,
                        ),
                    ) {
                        Icon(
                            if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (channel.isFavorite) Color.Red else if (isFavFocused) Color.Black else Color.Gray,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (channel.isFavorite) stringResourceAI("remove_favorite") else stringResourceAI("add_favorite"))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlayerDialog = null }) {
                    Text(stringResourceAI("cancel"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}
