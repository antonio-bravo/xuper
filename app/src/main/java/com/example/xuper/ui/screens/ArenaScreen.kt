package com.example.xuper.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xuper.data.ArenaParser
import com.example.xuper.model.ArenaEvent
import com.example.xuper.ui.components.UniversalPlayer
import com.example.xuper.ui.viewmodel.ArenaViewModel
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.xuper.util.PlayerUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArenaScreen(viewModel: ArenaViewModel = viewModel()) {
    val events by viewModel.events.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val context = LocalContext.current

    // State for URL confirmation dialog
    var showUrlDialog by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf("") }
    var pendingChannelName by remember { mutableStateOf("") }
    var internalPlayerUrl by remember { mutableStateOf<String?>(null) }

    // State for channel selection dialog
    var showChannelPicker by remember { mutableStateOf(false) }
    var selectedEventForPicker by remember { mutableStateOf<ArenaEvent?>(null) }

    val firstChannelFocusRequester = remember { FocusRequester() }
    val urlInternalBtnFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showChannelPicker) {
        if (showChannelPicker) {
            delay(200)
            try {
                firstChannelFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showUrlDialog) {
        if (showUrlDialog) {
            delay(200)
            try {
                urlInternalBtnFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    if (showChannelPicker && selectedEventForPicker != null) {
        val firstAvailableIndex = remember(selectedEventForPicker, streams) {
            val idx = selectedEventForPicker?.channels?.indexOfFirst { streams[it] != null } ?: -1
            if (idx >= 0) idx else 0
        }

        AlertDialog(
            onDismissRequest = { showChannelPicker = false },
            title = { 
                Column {
                    Text("Seleccionar Canal", style = MaterialTheme.typography.titleLarge)
                    Text(selectedEventForPicker!!.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                LaunchedEffect(Unit) {
                    delay(100)
                    try {
                        firstChannelFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                }
                Column {
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        itemsIndexed(selectedEventForPicker!!.channels) { index, channelName ->
                            val hash = streams[channelName]
                            val isAvailable = hash != null
                            
                            var isItemFocused by remember { mutableStateOf(false) }
                            val itemScale by animateFloatAsState(if (isItemFocused) 1.04f else 1f, label = "itemScale")
                            
                            Card(
                                onClick = {
                                    if (isAvailable) {
                                        pendingChannelName = channelName
                                        pendingUrl = hash!!
                                        showChannelPicker = false
                                        showUrlDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .then(if (index == firstAvailableIndex) Modifier.focusRequester(firstChannelFocusRequester) else Modifier)
                                    .onFocusChanged { isItemFocused = it.isFocused }
                                    .scale(itemScale)
                                    .border(
                                        width = if (isItemFocused) 3.dp else 0.dp,
                                        color = if (isItemFocused) Color.White else Color.Transparent,
                                        shape = MaterialTheme.shapes.medium
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isItemFocused -> Color.White
                                        isAvailable -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                ),
                                enabled = isAvailable
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isAvailable) Icons.Default.PlayArrow else Icons.Default.ContentCopy, 
                                        contentDescription = null, 
                                        tint = when {
                                            isItemFocused -> Color.Black
                                            isAvailable -> MaterialTheme.colorScheme.primary
                                            else -> Color.Gray
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = channelName, 
                                        fontWeight = FontWeight.Bold,
                                        color = if (isItemFocused) Color.Black else if (isAvailable) MaterialTheme.colorScheme.onSurface else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                var isCancelPickerFocused by remember { mutableStateOf(false) }
                val cancelPickerScale by animateFloatAsState(if (isCancelPickerFocused) 1.1f else 1f, label = "cancelPickerScale")
                OutlinedButton(
                    onClick = { showChannelPicker = false },
                    modifier = Modifier
                        .onFocusChanged { isCancelPickerFocused = it.isFocused }
                        .scale(cancelPickerScale)
                        .border(
                            width = if (isCancelPickerFocused) 3.dp else 0.dp,
                            color = if (isCancelPickerFocused) Color.White else Color.Transparent,
                            shape = ButtonDefaults.shape
                        ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isCancelPickerFocused) Color.White else Color.Transparent,
                        contentColor = if (isCancelPickerFocused) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (internalPlayerUrl != null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            UniversalPlayer(url = internalPlayerUrl!!)
            IconButton(
                onClick = { internalPlayerUrl = null },
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar", tint = Color.White)
            }
        }
        return
    }

    if (showUrlDialog) {
        val displayUrl = PlayerUtils.formatAceStreamHttpUrl(pendingUrl)
        
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Abrir Canal") },
            text = {
                Column {
                    Text("Canal: $pendingChannelName", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Selecciona el método de reproducción:")
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var isBtnIntFocused by remember { mutableStateOf(false) }
                        val scaleInt by animateFloatAsState(if (isBtnIntFocused) 1.05f else 1f, label = "scaleInt")
                        Button(
                            onClick = {
                                showUrlDialog = false
                                internalPlayerUrl = displayUrl
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(urlInternalBtnFocusRequester)
                                .onFocusChanged { isBtnIntFocused = it.isFocused }
                                .scale(scaleInt)
                                .border(
                                    width = if (isBtnIntFocused) 3.dp else 0.dp,
                                    color = if (isBtnIntFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = ButtonDefaults.shape
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBtnIntFocused) Color.White else MaterialTheme.colorScheme.primary,
                                contentColor = if (isBtnIntFocused) Color.Black else Color.White
                            )
                        ) {
                            Text("Interno")
                        }

                        var isBtnExtFocused by remember { mutableStateOf(false) }
                        val scaleExt by animateFloatAsState(if (isBtnExtFocused) 1.05f else 1f, label = "scaleExt")
                        Button(
                            onClick = {
                                showUrlDialog = false
                                PlayerUtils.launchAceStream(context, pendingChannelName, pendingUrl)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isBtnExtFocused = it.isFocused }
                                .scale(scaleExt)
                                .border(
                                    width = if (isBtnExtFocused) 3.dp else 0.dp,
                                    color = if (isBtnExtFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = ButtonDefaults.shape
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBtnExtFocused) Color.White else MaterialTheme.colorScheme.primary,
                                contentColor = if (isBtnExtFocused) Color.Black else Color.White
                            )
                        ) {
                            Text("Externo")
                        }
                    }
                }
            },
            dismissButton = {
                var isCancelUrlFocused by remember { mutableStateOf(false) }
                val cancelUrlScale by animateFloatAsState(if (isCancelUrlFocused) 1.1f else 1f, label = "cancelUrlScale")
                OutlinedButton(
                    onClick = { showUrlDialog = false },
                    modifier = Modifier
                        .onFocusChanged { isCancelUrlFocused = it.isFocused }
                        .scale(cancelUrlScale)
                        .border(
                            width = if (isCancelUrlFocused) 3.dp else 0.dp,
                            color = if (isCancelUrlFocused) Color.White else Color.Transparent,
                            shape = ButtonDefaults.shape
                        ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isCancelUrlFocused) Color.White else Color.Transparent,
                        contentColor = if (isCancelUrlFocused) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        val lastFetchTimeStr by viewModel.lastFetchTimeString.collectAsState()
        
        // Title and Source Selection
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
                    text = "Arena4Viewer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
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
                    onClick = { viewModel.loadData(autoScan = true, forceRefresh = true) },
                    modifier = Modifier
                        .onFocusChanged { isIconRefreshFocused = it.isFocused }
                        .scale(iconRefreshScale)
                        .border(
                            width = if (isIconRefreshFocused) 2.dp else 0.dp,
                            color = if (isIconRefreshFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isIconRefreshFocused) Color.White else Color.Transparent,
                        contentColor = if (isIconRefreshFocused) Color.Black else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Forzar Actualización")
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
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
                            .focusable()
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = source.replace("https://", "").replace("http://", "").substringBefore("/"),
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
        } else {
            val grouped = remember(events) { events.groupBy { it.date } }
            
            if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay eventos disponibles en esta fuente", color = Color.Gray)
                }
            } else {
                val listState = rememberLazyListState()
                
                // Efecto para buscar y scrollear automáticamente al día de hoy
                LaunchedEffect(grouped) {
                    val targetDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    var targetIndex = 0
                    var found = false
                    
                    for ((date, dateEvents) in grouped) {
                        if (date == targetDate) {
                            found = true
                            break
                        }
                        // Cada grupo suma 1 por el stickyHeader + el número de eventos correspondientes
                        targetIndex += 1 + dateEvents.size
                    }
                    
                    if (found) {
                        listState.scrollToItem(targetIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    grouped.forEach { (date, dateEvents) ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.95f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = date,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(dateEvents) { event ->
                            ArenaEventRow(event, 
                                onRowClick = {
                                    selectedEventForPicker = it
                                    showChannelPicker = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArenaEventRow(
    event: ArenaEvent, 
    onRowClick: (ArenaEvent) -> Unit
) {
    var isFocused by remember { mutableStateOf(value = false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onRowClick(event) }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.small
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color.White else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HORA
                Text(
                    text = event.time,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(70.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    // DEPORTE - COMPETICION
                    Text(
                        text = "${event.sport} - ${event.competition}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isFocused) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // EVENTO
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp).padding(start = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // CANALES CONCATENADOS
            val channelText = event.channels.joinToString(" / ")
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = channelText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
