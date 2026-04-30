package com.example.xuper.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xuper.data.ArenaParser
import com.example.xuper.model.ArenaEvent
import com.example.xuper.ui.stringResourceAI
import com.example.xuper.ui.viewmodel.ArenaViewModel

import com.example.xuper.util.PlayerUtils

@Composable
fun ArenaScreen(viewModel: ArenaViewModel = viewModel()) {
    val events by viewModel.events.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Arena4Viewer",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Source Selection (Radio Buttons)
        Text(text = "Seleccionar Fuente:", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(vertical = 8.dp),
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
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null // handled by Surface
                        )
                        Spacer(Modifier.width(4.dp))
                        val label = source.replace("https://", "").replace("http://", "").substringBefore("/")
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(events) { event ->
                    ArenaEventCard(event, streams) { channelName, url ->
                        PlayerUtils.launchAceStream(context, channelName, url)
                    }
                }
            }
        }
    }
}

@Composable
fun ArenaEventCard(event: ArenaEvent, streams: Map<String, String>, onChannelClick: (String, String) -> Unit) {
    var isFocused by remember { mutableStateOf(value = false) }
    val scale by animateFloatAsState(if (isFocused) 1.02f else 1f, label = "eventScale")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = event.time, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(text = event.sport, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Text(text = event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = event.competition, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(Modifier.height(8.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(event.channels) { channelName ->
                    val url = streams[channelName]
                    if (url != null) {
                        var isChanFocused by remember { mutableStateOf(value = false) }
                        val chanScale by animateFloatAsState(if (isChanFocused) 1.1f else 1f, label = "chanScale")
                        
                        SuggestionChip(
                            onClick = { onChannelClick(channelName, url) },
                            label = { Text(channelName) },
                            modifier = Modifier
                                .onFocusChanged { isChanFocused = it.isFocused }
                                .scale(chanScale)
                                .border(
                                    width = if (isChanFocused) 2.dp else 0.dp,
                                    color = if (isChanFocused) Color.White else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                ),
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
    }
}

