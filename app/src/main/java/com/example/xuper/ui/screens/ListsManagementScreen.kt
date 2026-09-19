package com.example.xuper.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xuper.model.M3UList

@Composable
fun ListsManagementScreen(
    lists: List<M3UList>,
    onSaveLists: (List<M3UList>) -> Unit,
) {
    var showDialog by remember { mutableStateOf<M3UList?>(null) }
    var listToEdit by remember { mutableStateOf<M3UList?>(null) }
    
    var nameField by remember { mutableStateOf("") }
    var urlField by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("Gestión de Listas M3U", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            
            var isAddFocused by remember { mutableStateOf(false) }
            val addScale by animateFloatAsState(if (isAddFocused) 1.15f else 1f, label = "addScale")
            
            Button(
                onClick = {
                    listToEdit = null
                    nameField = ""
                    urlField = ""
                    showDialog = M3UList(name = "", url = "")
                },
                modifier = Modifier
                    .onFocusChanged { isAddFocused = it.isFocused }
                    .scale(addScale)
                    .border(
                        width = if (isAddFocused) 3.dp else 0.dp,
                        color = if (isAddFocused) Color.White else Color.Transparent,
                        shape = ButtonDefaults.shape
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAddFocused) Color.White else MaterialTheme.colorScheme.primary,
                    contentColor = if (isAddFocused) Color.Black else Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Añadir Lista")
            }
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(
                items = lists,
                key = { it.id }
            ) { list ->
                M3UListItemCard(
                    list = list,
                    onToggleEnabled = { isChecked ->
                        onSaveLists(lists.map { 
                            if (it.id == list.id) it.copy(enabled = isChecked) else it
                        })
                    },
                    onEdit = {
                        listToEdit = list
                        nameField = list.name
                        urlField = list.url
                        showDialog = list
                    },
                    onDelete = {
                        onSaveLists(lists.filter { it.id != list.id })
                    }
                )
            }
        }
    }

    if (showDialog != null) {
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text(if (listToEdit == null) "Añadir Lista M3U" else "Editar Lista M3U") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    var isNameFocused by remember { mutableStateOf(false) }
                    val nameScale by animateFloatAsState(if (isNameFocused) 1.02f else 1f, label = "nameScale")
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        label = { Text("Nombre") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isNameFocused = it.isFocused }
                            .scale(nameScale)
                            .border(
                                width = if (isNameFocused) 3.dp else 0.dp,
                                color = if (isNameFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    
                    var isUrlFocused by remember { mutableStateOf(false) }
                    val urlScale by animateFloatAsState(if (isUrlFocused) 1.02f else 1f, label = "urlScale")
                    OutlinedTextField(
                        value = urlField,
                        onValueChange = { urlField = it },
                        label = { Text("URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isUrlFocused = it.isFocused }
                            .scale(urlScale)
                            .border(
                                width = if (isUrlFocused) 3.dp else 0.dp,
                                color = if (isUrlFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            },
            confirmButton = {
                var isConfirmFocused by remember { mutableStateOf(false) }
                val confirmScale by animateFloatAsState(if (isConfirmFocused) 1.08f else 1f, label = "confirmScale")
                Button(
                    onClick = {
                        if (nameField.isNotBlank() && urlField.isNotBlank()) {
                            if (listToEdit == null) {
                                onSaveLists(lists + M3UList(name = nameField, url = urlField))
                            } else {
                                onSaveLists(lists.map { 
                                    if (it.id == listToEdit!!.id) it.copy(name = nameField, url = urlField) else it 
                                })
                            }
                            showDialog = null
                        }
                    },
                    modifier = Modifier
                        .onFocusChanged { isConfirmFocused = it.isFocused }
                        .scale(confirmScale)
                        .border(
                            width = if (isConfirmFocused) 3.dp else 0.dp,
                            color = if (isConfirmFocused) Color.White else Color.Transparent,
                            shape = ButtonDefaults.shape
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConfirmFocused) Color.White else MaterialTheme.colorScheme.primary,
                        contentColor = if (isConfirmFocused) Color.Black else Color.White
                    )
                ) {
                    Text(if (listToEdit == null) "Añadir" else "Actualizar")
                }
            },
            dismissButton = {
                var isDismissFocused by remember { mutableStateOf(false) }
                val dismissScale by animateFloatAsState(if (isDismissFocused) 1.08f else 1f, label = "dismissScale")
                OutlinedButton(
                    onClick = { showDialog = null },
                    modifier = Modifier
                        .onFocusChanged { isDismissFocused = it.isFocused }
                        .scale(dismissScale)
                        .border(
                            width = if (isDismissFocused) 3.dp else 0.dp,
                            color = if (isDismissFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = ButtonDefaults.shape
                        ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDismissFocused) Color.White else Color.Transparent,
                        contentColor = if (isDismissFocused) Color.Black else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun M3UListItemCard(
    list: M3UList,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var isCardFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(if (isCardFocused) 1.02f else 1f, label = "cardScale")
    
    val containerColor by animateColorAsState(
        targetValue = when {
            isCardFocused -> MaterialTheme.colorScheme.primaryContainer
            list.enabled -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        label = "containerColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isCardFocused = it.isFocused }
            .scale(cardScale)
            .border(
                width = if (isCardFocused) 3.dp else 0.dp,
                color = if (isCardFocused) Color.White else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCardFocused) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isSwitchFocused by remember { mutableStateOf(false) }
            val switchScale by animateFloatAsState(if (isSwitchFocused) 1.2f else 1f, label = "switchScale")
            Box(
                modifier = Modifier
                    .onFocusChanged { isSwitchFocused = it.isFocused }
                    .scale(switchScale)
                    .border(
                        width = if (isSwitchFocused) 2.dp else 0.dp,
                        color = if (isSwitchFocused) Color.White else Color.Transparent,
                        shape = CircleShape
                    )
            ) {
                Switch(
                    checked = list.enabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = list.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                var isEditFocused by remember { mutableStateOf(false) }
                val editScale by animateFloatAsState(if (isEditFocused) 1.2f else 1f, label = "editScale")
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .onFocusChanged { isEditFocused = it.isFocused }
                        .scale(editScale)
                        .border(
                            width = if (isEditFocused) 3.dp else 0.dp,
                            color = if (isEditFocused) Color.White else Color.Transparent,
                            shape = CircleShape
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isEditFocused) Color.White else Color.Transparent,
                        contentColor = if (isEditFocused) Color.Black else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }

                var isDeleteFocused by remember { mutableStateOf(false) }
                val deleteScale by animateFloatAsState(if (isDeleteFocused) 1.2f else 1f, label = "deleteScale")
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .onFocusChanged { isDeleteFocused = it.isFocused }
                        .scale(deleteScale)
                        .border(
                            width = if (isDeleteFocused) 3.dp else 0.dp,
                            color = if (isDeleteFocused) Color.Red else Color.Transparent,
                            shape = CircleShape
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isDeleteFocused) Color.Red else Color.Transparent,
                        contentColor = if (isDeleteFocused) Color.White else Color.Red
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar")
                }
            }
        }
    }
}
