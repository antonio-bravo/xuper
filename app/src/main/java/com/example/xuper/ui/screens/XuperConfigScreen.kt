package com.example.xuper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.xuper.XuperConfigHelper

@Composable
fun XuperConfigScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Proxies AceStream") },
                icon = { Icon(Icons.Default.Router, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Servidor API Xuper") },
                icon = { Icon(Icons.Default.Dns, contentDescription = null) }
            )
        }

        when (selectedTab) {
            0 -> AceStreamConfigScreen()
            1 -> ApiConfigSection()
        }
    }
}

@Composable
private fun ApiConfigSection() {
    val context = LocalContext.current
    val settings = remember { XuperConfigHelper.obtenerAjustes(context) }

    var apiUrl by remember { mutableStateOf(settings[0]) }
    var user by remember { mutableStateOf(settings[1]) }
    var pass by remember { mutableStateOf(settings[2]) }
    var showSavedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Configuración Servidor API Xuper", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it },
            label = { Text("URL de la API") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                XuperConfigHelper.guardarAjustes(context, apiUrl, user, pass)
                showSavedMessage = true
            },
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text("Guardar Configuración")
        }

        if (showSavedMessage) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("✓ Configuración guardada correctamente", color = com.example.xuper.ui.theme.Success)
        }
    }
}
