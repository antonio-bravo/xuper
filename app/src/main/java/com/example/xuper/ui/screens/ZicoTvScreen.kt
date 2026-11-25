package com.example.xuper.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
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
import com.example.xuper.data.ZicoTvChannel
import com.example.xuper.data.ZicoTvTokenInfo
import com.example.xuper.ui.components.rememberAdaptiveLayoutConfig
import com.example.xuper.ui.stringResourceAI
import com.example.xuper.ui.viewmodel.ZicoTvViewModel
import kotlinx.coroutines.launch

@Composable
fun ZicoTvScreen(
    viewModel: ZicoTvViewModel = viewModel(),
    onPlayFullScreen: ((String) -> Unit)? = null,
    isSingleColumn: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layoutConfig = rememberAdaptiveLayoutConfig()

    val channels by viewModel.channels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lastFetchTimeStr by viewModel.lastFetchTimeString.collectAsState()

    var selectedChannelForToken by remember { mutableStateOf<ZicoTvChannel?>(null) }
    var generatedTokenInfo by remember { mutableStateOf<ZicoTvTokenInfo?>(null) }
    var isGeneratingToken by remember { mutableStateOf(false) }

    val categories = remember(channels) {
        val cats = channels.map { it.group }.distinct().sorted()
        listOf("Todos") + cats
    }

    val filteredChannels = remember(channels, selectedCategory, searchQuery) {
        channels.filter { channel ->
            val matchesCategory = (selectedCategory == "Todos" || channel.group == selectedCategory)
            val matchesSearch = (searchQuery.isEmpty() || channel.name.contains(searchQuery, ignoreCase = true))
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Cabecera ZicoTV
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
                    text = "ZicoTV 🔑",
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
                        text = "⏱️ Token 3h Auto-Renew",
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
                    onClick = { viewModel.loadChannels(forceRefresh = true) },
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
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar Canales")
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
                placeholder = { Text("Buscar canal (ej: DAZN, Movistar, F1...)", style = MaterialTheme.typography.bodySmall) },
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
        } else if (errorMessage != null && filteredChannels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            val gridColumns = if (isSingleColumn) 1 else if (layoutConfig.isCompact) 1 else layoutConfig.gridColumns

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                verticalArrangement = Arrangement.spacedBy(layoutConfig.itemSpacing),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    ZicoTvChannelCard(
                        channel = channel,
                        onClick = {
                            selectedChannelForToken = channel
                            scope.launch {
                                isGeneratingToken = true
                                try {
                                    val tokenInfo = viewModel.getStreamToken(channel.id)
                                    generatedTokenInfo = tokenInfo
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error generando token: ${e.message}", Toast.LENGTH_SHORT).show()
                                    selectedChannelForToken = null
                                } finally {
                                    isGeneratingToken = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Diálogo de Token y Reproducción
    selectedChannelForToken?.let { channel ->
        AlertDialog(
            onDismissRequest = {
                selectedChannelForToken = null
                generatedTokenInfo = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(channel.name, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Canal #${channel.id} • Categoría: ${channel.group}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    if (isGeneratingToken) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Extrayendo token HLS nativo de 3 horas...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else generatedTokenInfo?.let { info ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                val timeFormat = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
                                Text("✓ Token 3H Activo (${info.cdn})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text("Duración: ${info.ttlSeconds / 3600}h (${info.ttlSeconds}s)", style = MaterialTheme.typography.labelSmall)
                                Text("Caduca: ${timeFormat.format(java.util.Date(info.expiresAtMillis))}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    generatedTokenInfo?.let { info ->
                        // Botón: Reproductor Interno
                        var isIntFocused by remember { mutableStateOf(false) }
                        val intScale by animateFloatAsState(if (isIntFocused) 1.03f else 1f, label = "intScale")
                        Button(
                            onClick = {
                                val urlToPlay = info.streamUrl
                                selectedChannelForToken = null
                                generatedTokenInfo = null
                                onPlayFullScreen?.invoke(urlToPlay)
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
                            Text(stringResourceAI("internal_player"), style = MaterialTheme.typography.titleMedium)
                        }

                        // Botón: Reproductor Externo
                        var isExtFocused by remember { mutableStateOf(false) }
                        val extScale by animateFloatAsState(if (isExtFocused) 1.03f else 1f, label = "extScale")
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(info.streamUrl.toUri(), "video/*")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Intent.createChooser(intent, "Abrir con...").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(this)
                                    }
                                }
                                selectedChannelForToken = null
                                generatedTokenInfo = null
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
                            Text(stringResourceAI("external_player"), style = MaterialTheme.typography.titleMedium)
                        }

                        // Botón: Copiar URL
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = android.content.ClipData.newPlainText("ZicoTV Stream URL", info.streamUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "URL de emisión HLS copiada al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copiar URL HLS Directa", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedChannelForToken = null
                    generatedTokenInfo = null
                }) {
                    Text(stringResourceAI("cancel"))
                }
            }
        )
    }
}

@Composable
fun ZicoTvChannelCard(
    channel: ZicoTvChannel,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "zicoCardScale"
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
                Text(
                    text = "#${channel.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color.Black else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFocused) Color.Black else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 1200
                            )
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = channel.group,
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
                            text = "⏱️ 3h Auto-Renew",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
