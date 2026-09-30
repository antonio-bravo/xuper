package com.example.xuper.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.xuper.data.AceStreamManager
import com.example.xuper.ui.components.LoadingIndicator
import com.example.xuper.ui.components.rememberAdaptiveLayoutConfig
import kotlinx.coroutines.launch

@Composable
fun AceStreamConfigScreen() {
    val context = LocalContext.current
    val layoutConfig = rememberAdaptiveLayoutConfig()
    val aceStreamManager = remember { AceStreamManager(context) }
    val scope = rememberCoroutineScope()

    var host by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("6878") }
    var isChecking by remember { mutableStateOf(false) }
    var checkResult by remember { mutableStateOf<Boolean?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var foundHosts by remember { mutableStateOf<List<String>>(emptyList()) }
    var useCustom by remember { mutableStateOf(aceStreamManager.getConfig().isCustom) }

    // Cargar configuración actual
    LaunchedEffect(Unit) {
        val config = aceStreamManager.getConfig()
        if (config.isCustom) {
            host = config.host
            port = config.port.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(layoutConfig.contentPadding)
    ) {
        // Header
        Text(
            "Configuración Acestream",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Configura tu servidor Acestream Engine",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Tipo de conexión
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Tipo de Conexión",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(12.dp))

                // Opción: Local
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !useCustom,
                        onClick = { useCustom = false }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Local (127.0.0.1:6878)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Engine instalado en este dispositivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Opción: Docker/Red
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = useCustom,
                        onClick = { useCustom = true }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Docker / Red Local", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Engine en servidor de tu red",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Configuración custom (solo si está seleccionada)
        if (useCustom) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Servidor Custom",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(16.dp))

                    // Campo: Host
                    var isHostFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it; checkResult = null },
                        label = { Text("Host / IP") },
                        placeholder = { Text("192.168.1.100") },
                        leadingIcon = { Icon(Icons.Default.Computer, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isHostFocused = it.isFocused }
                            .border(
                                width = if (isHostFocused) 3.dp else 0.dp,
                                color = if (isHostFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Campo: Puerto
                    var isPortFocused by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = port,
                        onValueChange = { if (it.all { c -> c.isDigit() }) { port = it; checkResult = null } },
                        label = { Text("Puerto") },
                        placeholder = { Text("6878") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isPortFocused = it.isFocused }
                            .border(
                                width = if (isPortFocused) 3.dp else 0.dp,
                                color = if (isPortFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = MaterialTheme.shapes.small
                            ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Botón: Probar Conexión
                    var isTestFocused by remember { mutableStateOf(false) }
                    val testScale by animateFloatAsState(if (isTestFocused) 1.05f else 1f, label = "testScale")

                    Button(
                        onClick = {
                            scope.launch {
                                isChecking = true
                                checkResult = null
                                val config = AceStreamManager.AceStreamConfig(
                                    host = host,
                                    port = port.toIntOrNull() ?: 6878,
                                    isCustom = true
                                )
                                checkResult = aceStreamManager.isEngineAvailable(config)
                                isChecking = false
                            }
                        },
                        enabled = !isChecking && host.isNotEmpty() && port.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isTestFocused = it.isFocused }
                            .scale(testScale)
                            .border(
                                width = if (isTestFocused) 3.dp else 0.dp,
                                color = if (isTestFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTestFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.primary,
                            contentColor = if (isTestFocused) Color.Black else Color.White
                        )
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isChecking) "Probando..." else "Probar Conexión")
                    }

                    // Resultado de la prueba
                    checkResult?.let { success ->
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (success) com.example.xuper.ui.theme.Success.copy(alpha = 0.1f)
                                    else com.example.xuper.ui.theme.Error.copy(alpha = 0.1f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (success) com.example.xuper.ui.theme.Success else com.example.xuper.ui.theme.Error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (success) "✓ Conexión exitosa" else "✗ No se pudo conectar",
                                color = if (success) com.example.xuper.ui.theme.Success else com.example.xuper.ui.theme.Error
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botón: Escanear Red
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Escanear Red",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Busca automáticamente servidores Acestream en tu red local",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    var isScanFocused by remember { mutableStateOf(false) }
                    val scanScale by animateFloatAsState(if (isScanFocused) 1.05f else 1f, label = "scanScale")

                    Button(
                        onClick = {
                            scope.launch {
                                isScanning = true
                                foundHosts = emptyList()
                                val baseIp = host.substringBeforeLast(".")
                                foundHosts = aceStreamManager.scanLocalNetwork(baseIp)
                                isScanning = false
                            }
                        },
                        enabled = !isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isScanFocused = it.isFocused }
                            .scale(scanScale)
                            .border(
                                width = if (isScanFocused) 3.dp else 0.dp,
                                color = if (isScanFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = ButtonDefaults.shape
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanFocused) com.example.xuper.ui.theme.FocusBackground else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (isScanFocused) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isScanning) "Escaneando..." else "Escanear Red")
                    }

                    // Resultados del escaneo
                    if (foundHosts.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Encontrados (${foundHosts.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        foundHosts.forEach { hostPort ->
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    val parts = hostPort.split(":")
                                    host = parts[0]
                                    port = parts.getOrNull(1) ?: "6878"
                                }
                            ) {
                                Text("→ $hostPort")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Botón: Guardar
        var isSaveFocused by remember { mutableStateOf(false) }
        val saveScale by animateFloatAsState(if (isSaveFocused) 1.05f else 1f, label = "saveScale")

        Button(
            onClick = {
                if (useCustom) {
                    aceStreamManager.saveCustomConfig(host, port.toIntOrNull() ?: 6878)
                } else {
                    aceStreamManager.useLocalConfig()
                }
                // TODO: Navegar atrás o mostrar confirmación
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isSaveFocused = it.isFocused }
                .scale(saveScale)
                .border(
                    width = if (isSaveFocused) 3.dp else 0.dp,
                    color = if (isSaveFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                    shape = ButtonDefaults.shape
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSaveFocused) com.example.xuper.ui.theme.SelectionIndicator else MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Guardar Configuración")
        }

        Spacer(Modifier.height(16.dp))

        // Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Docker Command:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "docker run -d --name acestream -p 6878:6878 magnetikonline/acestream-server",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
