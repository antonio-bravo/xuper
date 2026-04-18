package com.example.xuper.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
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
import com.example.xuper.model.Channel
import com.example.xuper.model.M3UList
import com.example.xuper.ui.components.ChannelList
import com.example.xuper.ui.components.UniversalPlayer

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
    filterFocusRequester: FocusRequester
) {
    var showPlayerDialog by remember { mutableStateOf<Channel?>(null) }
    val context = LocalContext.current

    Column {
        if (selectedChannel != null) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .focusable()) {
                UniversalPlayer(url = selectedChannel.url)
                Row(modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)) {
                    var isFsFocused by remember { mutableStateOf(false) }
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
                var isCloseFocused by remember { mutableStateOf(false) }
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
                if (isLoading) CircularProgressIndicator() else Text("Selecciona un canal para reproducir")
            }
        }

        // Buscador
        var isSearchFocused by remember { mutableStateOf(false) }
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
                    shape = MaterialTheme.shapes.small
                ),
            placeholder = { Text("Buscar canal...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.secondary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Filtro de Listas
        val listSources = remember(m3uLists) {
            listOf("Todas las listas") + m3uLists.map { it.name }
        }
        LazyRow(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            items(listSources.size) { index ->
                val name = listSources[index]
                var isFocused by remember { mutableStateOf(false) }
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
                            color = if (isFocused) Color.White else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        labelColor = Color.Gray
                    )
                )
            }
        }

        // Filtro de Categorías
        val categories = remember(channels, selectedListName) {
            val base = if (selectedListName == "Todas las listas") channels else channels.filter { it.sourceListName == selectedListName }
            listOf("Todos") + base.map { it.category }.distinct().sorted()
        }
        LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            items(categories.size) { index ->
                val category = categories[index]
                var isFocused by remember { mutableStateOf(false) }
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
                            color = if (isFocused) Color.White else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        labelColor = Color.Gray
                    )
                )
            }
        }

        val filteredChannels = remember(channels, searchQuery, selectedCategory, selectedListName) {
            channels.filter { 
                (selectedListName == "Todas las listas" || it.sourceListName == selectedListName) &&
                (selectedCategory == "Todos" || it.category == selectedCategory) &&
                it.name.contains(searchQuery, ignoreCase = true)
            }.distinctBy { it.url }
        }

        ChannelList(
            channels = filteredChannels,
            onChannelSelected = { showPlayerDialog = it },
            onToggleFavorite = onToggleFavorite,
            modifier = Modifier.weight(1f)
        )
    }

    showPlayerDialog?.let { channel ->
        val isFavorite = channel.isFavorite
        AlertDialog(
            onDismissRequest = { showPlayerDialog = null },
            title = { Text(channel.name, style = MaterialTheme.typography.headlineSmall) },
            text = { 
                Column {
                    Text("¿Deseas reproducir este canal o gestionarlo?")
                    if (channel.category != "Otros") {
                        Text("Categoría: ${channel.category}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var isIntFocused by remember { mutableStateOf(false) }
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
                                color = if (isIntFocused) Color.White else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isIntFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    ) {
                        Text("REPRODUCTOR INTERNO", style = MaterialTheme.typography.titleMedium)
                    }
                    
                    var isExtFocused by remember { mutableStateOf(false) }
                    val extScale by animateFloatAsState(if (isExtFocused) 1.05f else 1f, label = "extScale")
                    Button(
                        onClick = {
                            val aceId = when {
                                channel.url.startsWith("acestream://") -> channel.url.substringAfter("acestream://")
                                channel.url.contains("id=") -> channel.url.substringAfter("id=").substringBefore("&")
                                else -> ""
                            }
                            val intent = if (aceId.isNotEmpty()) {
                                Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("acestream://$aceId")
                                    putExtra("id", aceId)
                                    putExtra("content_id", aceId)
                                    putExtra("name", channel.name)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            } else {
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(channel.url), "video/*")
                                }
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val chooser = Intent.createChooser(intent, "Abrir con...")
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            }
                            showPlayerDialog = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isExtFocused = it.isFocused }
                            .scale(extScale)
                            .border(
                                width = if (isExtFocused) 4.dp else 0.dp,
                                color = if (isExtFocused) Color.White else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExtFocused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    ) {
                        Text("REPRODUCTOR EXTERNO", style = MaterialTheme.typography.titleMedium)
                    }

                    var isFavFocused by remember { mutableStateOf(false) }
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
                                color = if (isFavFocused) Color.White else Color.Transparent,
                                shape = ButtonDefaults.shape
                            )
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isFavorite) Color.Red else Color.Gray
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isFavorite) "QUITAR DE FAVORITOS" else "AÑADIR A FAVORITOS")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlayerDialog = null }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            }
        )
    }
}
