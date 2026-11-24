package com.example.xuper.ui.screens

import android.content.Intent
import androidx.core.net.toUri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
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
import com.example.xuper.data.AceStreamManager
import com.example.xuper.data.ArenaParser
import com.example.xuper.model.ArenaEvent
import com.example.xuper.ui.components.UniversalPlayer
import com.example.xuper.ui.stringResourceAI
import com.example.xuper.ui.viewmodel.ArenaViewModel
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.xuper.util.PlayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArenaScreen(
    viewModel: ArenaViewModel = viewModel(),
    onPlayFullScreen: ((String) -> Unit)? = null
) {
    val events by viewModel.events.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val context = LocalContext.current
    val aceStreamManager = remember { AceStreamManager(context) }

    // State for URL confirmation dialog
    var showUrlDialog by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf("") }
    var pendingChannelName by remember { mutableStateOf("") }

    // State for channel selection dialog
    var showChannelPicker by remember { mutableStateOf(false) }
    var selectedEventForPicker by remember { mutableStateOf<ArenaEvent?>(null) }
    var channelPeers by remember { mutableStateOf<Map<String, Int?>>(emptyMap()) }
    var isLoadingPeersMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    val firstChannelFocusRequester = remember { FocusRequester() }
    val urlInternalBtnFocusRequester = remember { FocusRequester() }

    LaunchedEffect(showChannelPicker, selectedEventForPicker, streams) {
        if (showChannelPicker && selectedEventForPicker != null) {
            val channels = selectedEventForPicker!!.channels
            val availableChannels = channels.filter { streams[it] != null }

            channelPeers = emptyMap()
            isLoadingPeersMap = availableChannels.associateWith { true }

            // Log para debug
            android.util.Log.d("ArenaScreen", "Consultando peers para ${availableChannels.size} canales")

            coroutineScope {
                availableChannels.forEach { channelName ->
                    launch(Dispatchers.IO) {
                        val hash = streams[channelName]
                        if (hash != null) {
                            android.util.Log.d("ArenaScreen", "Obteniendo stats para $channelName con hash: $hash")

                            try {
                                val stats = withTimeoutOrNull(5000) {
                                    aceStreamManager.getStreamStats(hash)
                                }

                                val peerCount = stats?.peers
                                android.util.Log.d("ArenaScreen", "Stats para $channelName: peers=$peerCount, status=${stats?.status}")

                                withContext(Dispatchers.Main) {
                                    channelPeers = channelPeers + (channelName to peerCount)
                                    isLoadingPeersMap = isLoadingPeersMap + (channelName to false)
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ArenaScreen", "Error obteniendo stats para $channelName: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    channelPeers = channelPeers + (channelName to null)
                                    isLoadingPeersMap = isLoadingPeersMap + (channelName to false)
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                channelPeers = channelPeers + (channelName to null)
                                isLoadingPeersMap = isLoadingPeersMap + (channelName to false)
                            }
                        }
                    }
                }
            }
        } else {
            channelPeers = emptyMap()
            isLoadingPeersMap = emptyMap()
        }
    }

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
                                        pendingUrl = hash
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
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

                                    if (isAvailable) {
                                        val peers = channelPeers[channelName]
                                        val isLoadingPeers = isLoadingPeersMap[channelName] ?: false

                                        Surface(
                                            color = when {
                                                isItemFocused -> Color.Black.copy(alpha = 0.15f)
                                                peers != null && peers > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                            },
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (isLoadingPeers && peers == null) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(12.dp),
                                                        strokeWidth = 1.5.dp,
                                                        color = if (isItemFocused) Color.Black else MaterialTheme.colorScheme.primary
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.People,
                                                        contentDescription = "Peers",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = when {
                                                            isItemFocused -> Color.Black
                                                            peers != null && peers > 0 -> MaterialTheme.colorScheme.primary
                                                            else -> Color.Gray
                                                        }
                                                    )
                                                }
                                                Text(
                                                    text = when {
                                                        isLoadingPeers && peers == null -> "..."
                                                        peers != null && peers > 0 -> "$peers"
                                                        peers == 0 -> "0"
                                                        else -> "N/A"
                                                    },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        isItemFocused -> Color.Black
                                                        peers != null && peers > 0 -> MaterialTheme.colorScheme.primary
                                                        else -> Color.Gray
                                                    }
                                                )
                                            }
                                        }
                                    }
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

    if (showUrlDialog) {
        val isAcestream = remember(pendingUrl) { PlayerUtils.isAceStreamUrl(pendingUrl) }
        val proxies = remember { aceStreamManager.getProxies() }
        val peers = channelPeers[pendingChannelName]

        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text(pendingChannelName, style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    Text(stringResourceAI("select_player"), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    if (peers != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "$peers peers activos",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (isAcestream) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Canal Acestream P2P",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (isAcestream) {
                        Text(
                            "Reproducción Interna (Proxy AceStream):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        proxies.forEach { proxy ->
                            var isProxyFocused by remember { mutableStateOf(false) }
                            val proxyScale by animateFloatAsState(if (isProxyFocused) 1.03f else 1f, label = "proxyScale")

                            Button(
                                onClick = {
                                    aceStreamManager.setSelectedProxy(proxy)
                                    val formattedUrl = aceStreamManager.getFormattedUrlForProxy(pendingUrl, proxy)
                                    showUrlDialog = false
                                    onPlayFullScreen?.invoke(formattedUrl)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { isProxyFocused = it.isFocused }
                                    .scale(proxyScale)
                                    .border(
                                        width = if (isProxyFocused) 3.dp else 0.dp,
                                        color = if (isProxyFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                        shape = ButtonDefaults.shape
                                    ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isProxyFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isProxyFocused) Color.Black else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(if (proxy.isVpn) "🔒" else "🌐")
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(proxy.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${proxy.host}:${proxy.port}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isProxyFocused) Color.DarkGray else Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    } else {
                        // Reproductor Interno Estándar
                        var isIntFocused by remember { mutableStateOf(false) }
                        val intScale by animateFloatAsState(if (isIntFocused) 1.05f else 1f, label = "intScale")
                        Button(
                            onClick = {
                                showUrlDialog = false
                                onPlayFullScreen?.invoke(pendingUrl)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isIntFocused = it.isFocused }
                                .scale(intScale)
                                .border(
                                    width = if (isIntFocused) 3.dp else 0.dp,
                                    color = if (isIntFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = ButtonDefaults.shape
                                ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isIntFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primary,
                                contentColor = if (isIntFocused) Color.Black else Color.White
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(stringResourceAI("internal_player"), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    // Botón: Reproductor Externo
                    var isExtFocused by remember { mutableStateOf(false) }
                    val extScale by animateFloatAsState(if (isExtFocused) 1.05f else 1f, label = "extScale")
                    Button(
                        onClick = {
                            val rawUrl = pendingUrl.trim()
                            val aceId = PlayerUtils.getAceId(rawUrl)

                            if (aceId.isNotEmpty()) {
                                val currentProxy = aceStreamManager.getSelectedProxy()
                                val streamUrl = currentProxy.getStreamUrl(aceId)
                                PlayerUtils.openInAceStreamApp(context, streamUrl)
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
                            showUrlDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isExtFocused = it.isFocused }
                            .scale(extScale)
                            .border(
                                width = if (isExtFocused) 3.dp else 0.dp,
                                color = if (isExtFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExtFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isExtFocused) Color.Black else Color.White
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(stringResourceAI("external_player"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Abrir en app externa (AceStream)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isExtFocused) Color.DarkGray else Color.White.copy(alpha = 0.7f)
                            )
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
                    Text(stringResourceAI("cancel"))
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
                            width = if (isIconRefreshFocused) 3.dp else 0.dp,
                            color = if (isIconRefreshFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = CircleShape
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isIconRefreshFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.surfaceVariant,
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
                                width = if (isFocused) 3.dp else 0.dp,
                                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) com.example.xuper.ui.theme.SelectionIndicator else if (isFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.surfaceVariant
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
    val todayDate = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val isToday = event.date == todayDate

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "arenaScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            .focusable()
            .clickable { onRowClick(event) }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused) 8.dp else 2.dp
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HORA Y BADGE HOY
                Column(modifier = Modifier.width(76.dp)) {
                    Text(
                        text = event.time,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary
                    )
                    if (isToday) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = Color.Red.copy(alpha = 0.15f),
                            contentColor = Color.Red
                        ) {
                            Text(
                                text = "🔴 HOY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    // DEPORTE - COMPETICION
                    Text(
                        text = "${event.sport} • ${event.competition}",
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
                    modifier = Modifier.size(28.dp).padding(start = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // CANALES CONCATENADOS
            val channelText = event.channels.joinToString(" / ")
            Surface(
                color = if (isFocused) Color.Black.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = channelText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
