package com.example.xuper.ui.screens

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.xuper.ui.components.rememberAdaptiveLayoutConfig
import com.example.xuper.ui.components.ResponsivePlayer
import com.example.xuper.ui.components.LoadingIndicator
import com.example.xuper.ui.components.EmptyState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
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
import androidx.compose.ui.text.font.FontWeight
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
    availableCategories: List<String>,
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
    isSingleColumn: Boolean,
    onToggleSingleColumn: () -> Unit = {},
    availableUpdate: com.example.xuper.util.AppUpdateInfo? = null,
    onOpenUpdateDialog: () -> Unit = {}
) {
    var showPlayerDialog by remember { mutableStateOf<Channel?>(null) }
    val context = LocalContext.current
    val layoutConfig = rememberAdaptiveLayoutConfig()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (selectedChannel != null) {
            ResponsivePlayer(
                url = selectedChannel.url,
                onFullScreen = onFullScreen,
                onClose = { onChannelSelected(null) }
            )
        } else if (isLoading && channels.isNotEmpty()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Banner de Actualización Disponible (si existe una nueva versión)
        if (availableUpdate != null) {
            var isUpdateBtnFocused by remember { mutableStateOf(false) }
            val updateScale by animateFloatAsState(if (isUpdateBtnFocused) 1.03f else 1f, label = "updateScale")
            Surface(
                onClick = onOpenUpdateDialog,
                shape = MaterialTheme.shapes.small,
                color = if (isUpdateBtnFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isUpdateBtnFocused) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .onFocusChanged { isUpdateBtnFocused = it.isFocused }
                    .scale(updateScale)
                    .border(
                        width = if (isUpdateBtnFocused) 2.dp else 0.dp,
                        color = if (isUpdateBtnFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "🚀 ¡Nueva versión disponible (v${availableUpdate.version})!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "ACTUALIZAR >",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Filtro de Listas (Solo las activas/enabled)
        val allListsLabel = stringResourceAI("all_lists")
        val listSources = remember(m3uLists, allListsLabel) {
            listOf(com.example.xuper.data.ChannelRepository.ALL_LISTS to allListsLabel) +
                m3uLists.filter { it.enabled }.map { it.name to it.name }
        }
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (layoutConfig.isCompact) {
                // En móvil incluir botones de refresco y vista en cabecera
                IconButton(onClick = onRefreshM3U, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar Listas", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onToggleSingleColumn, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isSingleColumn) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = "Cambiar vista",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            LazyRow(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(listSources.size, key = { listSources[it].first }) { index ->
                    val (listKey, listLabel) = listSources[index]
                    var isFocused by remember { mutableStateOf(value = false) }
                    val chipScale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "chipScale")
                    FilterChip(
                        selected = selectedListName == listKey,
                        onClick = { onListNameChange(listKey) },
                        label = { Text(listLabel) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .then(if (index == 0) Modifier.focusRequester(filterFocusRequester) else Modifier)
                            .onFocusChanged { isFocused = it.isFocused }
                            .scale(chipScale)
                            .border(
                                width = if (isFocused) 3.dp else 0.dp,
                                color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = com.example.xuper.ui.theme.SelectionIndicator,
                            selectedLabelColor = Color.Black,
                            containerColor = if (isFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (isFocused) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        // Filtro de Categorías
        val allCategoriesLabel = stringResourceAI("all_categories")
        val categories = remember(availableCategories, allCategoriesLabel) {
            listOf(com.example.xuper.data.ChannelRepository.ALL_CATEGORIES to allCategoriesLabel) +
                availableCategories.map { it to it }
        }
        LazyRow(modifier = Modifier.padding(horizontal = 8.dp)) {
            items(categories.size, key = { categories[it].first }) { index ->
                val (categoryKey, categoryLabel) = categories[index]
                var isFocused by remember { mutableStateOf(value = false) }
                val catScale by animateFloatAsState(if (isFocused) 1.1f else 1f, label = "catScale")
                FilterChip(
                    selected = selectedCategory == categoryKey,
                    onClick = { onCategoryChange(categoryKey) },
                    label = { Text(categoryLabel) },
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .onFocusChanged { isFocused = it.isFocused }
                        .scale(catScale)
                        .border(
                            width = if (isFocused) 3.dp else 0.dp,
                            color = if (isFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                            shape = MaterialTheme.shapes.small
                        ),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = com.example.xuper.ui.theme.SelectionIndicator,
                        selectedLabelColor = Color.Black,
                        containerColor = if (isFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (isFocused) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        ChannelList(
            channels = channels,
            onChannelSelected = { showPlayerDialog = it },
            onToggleFavorite = onToggleFavorite,
            isSingleColumn = isSingleColumn,
            isLoading = isLoading && channels.isEmpty(),
            modifier = Modifier.weight(1f)
        )

        // Buscador al pie en formato móvil compacto
        if (layoutConfig.isCompact) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text(stringResourceAI("search_placeholder")) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        }
    }

    showPlayerDialog?.let { channel ->
        com.example.xuper.ui.components.ChannelPlaybackDialog(
            title = channel.name,
            url = channel.url,
            subtitle = if (channel.category != "Otros") "${stringResourceAI("category")}: ${channel.category}" else null,
            isFavorite = channel.isFavorite,
            onToggleFavorite = { onToggleFavorite(channel) },
            onPlayInternal = { formattedUrl ->
                onChannelSelected(channel.copy(url = formattedUrl))
            },
            onDismiss = { showPlayerDialog = null }
        )
    }
}
