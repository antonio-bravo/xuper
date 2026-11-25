package com.example.xuper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.xuper.XuperConfigHelper

@Composable
fun XuperConfigScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        @Suppress("DEPRECATION")
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
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Actualización y Repo") },
                icon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }

        when (selectedTab) {
            0 -> AceStreamConfigScreen()
            1 -> ApiConfigSection()
            2 -> AboutAndUpdateSection()
        }
    }
}

@Composable
private fun ApiConfigSection() {
    val context = LocalContext.current
    val settings = remember { XuperConfigHelper.obtenerAjustes(context) }

    var apiUrl by remember { mutableStateOf(settings[0]) }
    var user by remember { mutableStateOf(settings[1]) }
    val passwordState = rememberTextFieldState(settings[2])
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

        SecureTextField(
            state = passwordState,
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                XuperConfigHelper.guardarAjustes(context, apiUrl, user, passwordState.text.toString())
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

@Composable
private fun AboutAndUpdateSection() {
    val context = LocalContext.current
    val currentVersion = remember { com.example.xuper.util.UpdateManager.getCurrentVersionName(context) }
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.example.xuper.util.AppUpdateInfo?>(null) }
    var checkMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Xuper TV", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Versión instalada: v$currentVersion", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Repositorio: https://github.com/antonio-bravo/xuper", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    isChecking = true
                    checkMessage = "Buscando actualizaciones en GitHub..."
                    val update = com.example.xuper.util.UpdateManager.checkForUpdates(context)
                    isChecking = false
                    if (update != null) {
                        updateInfo = update
                        checkMessage = null
                    } else {
                        checkMessage = "✓ Tienes la versión más reciente instalada (v$currentVersion)"
                    }
                }
            },
            enabled = !isChecking,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(if (isChecking) "Buscando..." else "Buscar Actualizaciones")
            }
        }

        checkMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        updateInfo?.let { update ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(0.85f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🚀 ¡Nueva Versión Disponible (v${update.version})!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Archivo a descargar: ${update.fileName}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { com.example.xuper.util.UpdateManager.downloadAndInstallApk(context, update) }) {
                            Text("DESCARGAR E INSTALAR")
                        }
                        OutlinedButton(onClick = { com.example.xuper.util.UpdateManager.openInBrowser(context, update.releaseNotesUrl) }) {
                            Text("VER EN GITHUB")
                        }
                    }
                }
            }
        }
    }
}
