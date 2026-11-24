package com.example.xuper.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.xuper.data.Ak47Event
import com.example.xuper.data.Ak47Stream
import com.example.xuper.ui.components.rememberAdaptiveLayoutConfig
import com.example.xuper.ui.stringResourceAI
import com.example.xuper.ui.viewmodel.Ak47SportsViewModel

@Composable
fun Ak47SportsScreen(
    viewModel: Ak47SportsViewModel = viewModel(),
    onPlayFullScreen: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val layoutConfig = rememberAdaptiveLayoutConfig()

    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lastFetchTimeStr by viewModel.lastFetchTimeString.collectAsState()

    var selectedEventForPicker by remember { mutableStateOf<Ak47Event?>(null) }

    val categories = remember(events) {
        val groups = events.map { it.group }.distinct().sorted()
        listOf("Todos", "📅 Partidos y Eventos", "🇪🇸 Eventos España") + groups
    }

    val filteredEvents = remember(events, selectedCategory, searchQuery) {
        events.filter { event ->
            val matchesCategory = when (selectedCategory) {
                "Todos" -> true
                "📅 Partidos y Eventos" -> event.isMatchEvent
                "🇪🇸 Eventos España" -> event.isSpanishEvent
                else -> event.group == selectedCategory
            }
            val matchesSearch = (searchQuery.isEmpty() ||
                    event.title.contains(searchQuery, ignoreCase = true) ||
                    event.group.contains(searchQuery, ignoreCase = true) ||
                    event.broadcastingChannelsText.contains(searchQuery, ignoreCase = true))

            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Cabecera AK47Sports
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "AK47Sports ⚽",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "AES-128 Direct Streams",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                if (lastFetchTimeStr.isNotEmpty()) {
                    Text(
                        text = "Refrescado: $lastFetchTimeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                var isIconRefreshFocused by remember { mutableStateOf(false) }
                val iconRefreshScale by animateFloatAsState(if (isIconRefreshFocused) 1.15f else 1f, label = "iconRefreshScale")
                IconButton(
                    onClick = { viewModel.loadEvents(forceRefresh = true) },
                    modifier = Modifier
                        .onFocusChanged { isIconRefreshFocused = it.isFocused }
                        .scale(iconRefreshScale)
                        .border(
                            width = if (isIconRefreshFocused) 3.dp else 0.dp,
                            color = if (isIconRefreshFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = CircleShape
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isIconRefreshFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isIconRefreshFocused) Color.Black else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar Eventos")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Barra de Búsqueda y Filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            var isSearchFocused by remember { mutableStateOf(false) }
            val searchScale by animateFloatAsState(if (isSearchFocused) 1.02f else 1f, label = "searchScale")

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar evento o equipo (ej: Madrid, Barça, F1...)", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .weight(1f, fill = false)
                    .onFocusChanged { isSearchFocused = it.isFocused }
                    .scale(searchScale)
                    .border(
                        width = if (isSearchFocused) 2.dp else 0.dp,
                        color = if (isSearchFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            )

            // Categorías horizontal scroll
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    var isFocused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(if (isFocused) 1.05f else 1f, label = "catScale")

                    Surface(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedCategory(cat) },
                        modifier = Modifier
                            .onFocusChanged { isFocused = it.isFocused }
                            .scale(scale)
                            .focusable()
                            .border(
                                width = if (isFocused) 3.dp else 0.dp,
                                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) com.example.xuper.ui.theme.SelectionIndicator else if (isFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = cat,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
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
        } else if (errorMessage != null && filteredEvents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else if (filteredEvents.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay eventos disponibles en esta categoría", color = Color.Gray)
            }
        } else {
            val gridColumns = if (layoutConfig.isCompact) 1 else layoutConfig.gridColumns

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                verticalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredEvents, key = { it.id }) { event ->
                    Ak47EventCard(
                        event = event,
                        onClick = { selectedEventForPicker = event }
                    )
                }
            }
        }
    }

    // Diálogo de Selección de Opción / Servidor de Emisión
    selectedEventForPicker?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedEventForPicker = null },
            title = {
                Column {
                    Text("Seleccionar Opción de Emisión", style = MaterialTheme.typography.titleLarge)
                    Text(event.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Opciones / Servidores disponibles (${event.streams.size}):", style = MaterialTheme.typography.labelMedium)

                    event.streams.forEachIndexed { index, stream ->
                        var isStreamFocused by remember { mutableStateOf(false) }
                        val streamScale by animateFloatAsState(if (isStreamFocused) 1.03f else 1f, label = "streamScale")

                        Button(
                            onClick = {
                                val urlToPlay = if (!stream.licenseKey.isNullOrEmpty()) {
                                    "${stream.url}|license_key=${stream.licenseKey}"
                                } else {
                                    stream.url
                                }
                                selectedEventForPicker = null
                                onPlayFullScreen?.invoke(urlToPlay)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isStreamFocused = it.isFocused }
                                .scale(streamScale)
                                .border(
                                    width = if (isStreamFocused) 3.dp else 0.dp,
                                    color = if (isStreamFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = ButtonDefaults.shape
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStreamFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primary,
                                contentColor = if (isStreamFocused) Color.Black else Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = if (stream.name.isNotEmpty()) stream.name else "Opción ${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = Color.Black.copy(alpha = 0.2f),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "HD Direct",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedEventForPicker = null }) {
                    Text(stringResourceAI("cancel"))
                }
            }
        )
    }
}

@Composable
fun Ak47EventCard(
    event: Ak47Event,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "ak47CardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .focusable()
            .clickable { onClick() }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = if (isFocused) Color.Black.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (event.logo.isNotEmpty()) {
                    AsyncImage(
                        model = event.logo,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = null,
                        tint = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (event.isSpanishEvent) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = Color.Red.copy(alpha = 0.15f),
                            contentColor = Color.Red
                        ) {
                            Text(
                                text = "🇪🇸 ESPAÑA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = event.group,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "${event.streams.size} opciones",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (event.broadcastingChannelsText.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = if (isFocused) Color.Black.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "📺 Retransmiten: ${event.broadcastingChannelsText}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).padding(start = 4.dp)
            )
        }
    }
}
