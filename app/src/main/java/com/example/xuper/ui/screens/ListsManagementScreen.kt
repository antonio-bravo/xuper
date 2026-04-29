package com.example.xuper.ui.screens

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Gestión de Listas M3U", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    listToEdit = null
                    nameField = ""
                    urlField = ""
                    showDialog = M3UList(name = "", url = "")
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
        
        LazyColumn {
            items(lists) { list ->
                ListItem(
                    headlineContent = { Text(list.name) },
                    supportingContent = { Text(list.url) },
                    trailingContent = {
                        Row {
                            var isEditFocused by remember { mutableStateOf(value = false) }
                            IconButton(
                                onClick = {
                                    listToEdit = list
                                    nameField = list.name
                                    urlField = list.url
                                    showDialog = list
                                },
                                modifier = Modifier
                                    .onFocusChanged { isEditFocused = it.isFocused }
                                    .border(
                                        width = if (isEditFocused) 2.dp else 0.dp,
                                        color = if (isEditFocused) Color.Cyan else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Cyan)
                            }
                            var isDeleteFocused by remember { mutableStateOf(value = false) }
                            IconButton(
                                onClick = {
                                    onSaveLists(lists.filter { it.id != list.id })
                                },
                                modifier = Modifier
                                    .onFocusChanged { isDeleteFocused = it.isFocused }
                                    .border(
                                        width = if (isDeleteFocused) 2.dp else 0.dp,
                                        color = if (isDeleteFocused) Color.Red else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Red)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog != null) {
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text(if (listToEdit == null) "Añadir Lista M3U" else "Editar Lista M3U") },
            text = {
                Column {
                    TextField(value = nameField, onValueChange = { nameField = it }, label = { Text("Nombre") })
                    Spacer(Modifier.height(8.dp))
                    TextField(value = urlField, onValueChange = { urlField = it }, label = { Text("URL") })
                }
            },
            confirmButton = {
                Button(onClick = {
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
                }) { Text(if (listToEdit == null) "Añadir" else "Actualizar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = null }) { Text("Cancelar") }
            }
        )
    }
}
