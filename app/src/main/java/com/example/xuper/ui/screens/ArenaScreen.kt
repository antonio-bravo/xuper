package com.example.xuper.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xuper.data.ArenaParser
import com.example.xuper.model.ArenaEvent
import com.example.xuper.ui.viewmodel.ArenaViewModel
import com.example.xuper.util.PlayerUtils

@Composable
fun ArenaScreen(viewModel: ArenaViewModel = viewModel()) {
    val events by viewModel.events.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        // Title and Source Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Arena4Viewer",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArenaParser.sources.forEach { source ->
                    val isSelected = selectedSource == source
                    var isFocused by remember { mutableStateOf(value = false) }
                    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "radioScale")

                    Surface(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedSource(source) },
                        modifier = Modifier
                            .onFocusChanged { isFocused = it.isFocused }
                            .scale(scale)
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) Color.White else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = source.replace("https://", "").replace("http://", "").substringBefore("/"),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Grid Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fecha / Hora", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold, color = Color.White)
                Text("Evento / Competición", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, color = Color.White)
                Text("Canales", modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(4.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(events) { event ->
                    ArenaEventRow(event, streams) { name, url ->
                        PlayerUtils.launchAceStream(context, name, url)
                    }
                }
            }
        }
    }
}

@Composable
fun ArenaEventRow(event: ArenaEvent, streams: Map<String, String>, onChannelClick: (String, String) -> Unit) {
    var isFocused by remember { mutableStateOf(value = false) }
    val backgroundColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "rowBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = MaterialTheme.shapes.extraSmall
            )
            .clickable { 
                // Al hacer click en la fila, intentamos abrir el primer canal si existe
                event.channels.firstOrNull()?.let { firstChannel ->
                    streams[firstChannel]?.let { url -> onChannelClick(firstChannel, url) }
                }
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FECHA / HORA
            Column(modifier = Modifier.weight(0.2f)) {
                Text(
                    text = event.time,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = event.sport,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // EVENTO / COMPETICION
            Column(modifier = Modifier.weight(0.5f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.competition,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // CANALES
            LazyRow(
                modifier = Modifier.weight(0.3f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(event.channels) { channelName ->
                    val url = streams[channelName]
                    if (url != null) {
                        var isChanFocused by remember { mutableStateOf(value = false) }
                        val chanScale by animateFloatAsState(if (isChanFocused) 1.2f else 1f, label = "chanScale")
                        
                        IconButton(
                            onClick = { onChannelClick(channelName, url) },
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .onFocusChanged { isChanFocused = it.isFocused }
                                .scale(chanScale)
                                .background(
                                    if (isChanFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = channelName,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArenaCloneScreen(viewModel: ArenaViewModel = viewModel()) {
    val events by viewModel.events.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        // Title and Source Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Arena Clone",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArenaParser.sources.forEach { source ->
                    val isSelected = selectedSource == source
                    var isFocused by remember { mutableStateOf(value = false) }
                    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "radioScale")

                    Surface(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedSource(source) },
                        modifier = Modifier
                            .onFocusChanged { isFocused = it.isFocused }
                            .scale(scale)
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) Color.White else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = source.replace("https://", "").replace("http://", "").substringBefore("/"),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Grid Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            shape = MaterialTheme.shapes.extraSmall
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fecha / Hora", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold, color = Color.White)
                Text("Evento", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, color = Color.White)
                Text("Acción", modifier = Modifier.weight(0.3f), fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Event List
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events) { event ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(event.time, modifier = Modifier.weight(0.2f), color = Color.White)
                        Text(event.title, modifier = Modifier.weight(0.5f), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = {
                            val channelName = event.channels.firstOrNull()
                            val streamUrl = channelName?.let { streams[it] }
                            if (streamUrl != null) {
                                PlayerUtils.launchAceStream(context, channelName, streamUrl)
                            } else {
                                Toast.makeText(context, "Stream no disponible", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Abrir en AceStream", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}
