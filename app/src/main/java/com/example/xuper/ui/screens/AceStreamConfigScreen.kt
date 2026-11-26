package com.example.xuper.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.xuper.data.AceStreamManager
import com.example.xuper.data.AceStreamProxy
import com.example.xuper.ui.components.rememberAdaptiveLayoutConfig
import kotlinx.coroutines.launch

@Composable
fun AceStreamConfigScreen() {
    val context = LocalContext.current
    val layoutConfig = rememberAdaptiveLayoutConfig()
    val aceStreamManager = remember { AceStreamManager(context) }
    val scope = rememberCoroutineScope()

    var proxies by remember { mutableStateOf(aceStreamManager.getProxies()) }
    var activeProxy by remember { mutableStateOf(aceStreamManager.getSelectedProxy()) }

    var showEditDialog by remember { mutableStateOf<AceStreamProxy?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    var testStatusMap by remember { mutableStateOf<Map<String, Boolean?>>(emptyMap()) }
    var testingId by remember { mutableStateOf<String?>(null) }

    var subnetPrefix by remember { mutableStateOf(aceStreamManager.detectLocalSubnet()) }
    var isScanningLan by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf<List<String>>(emptyList()) }
    var hasScanned by remember { mutableStateOf(false) }

    fun refreshProxies() {
        proxies = aceStreamManager.getProxies()
        activeProxy = aceStreamManager.getSelectedProxy()
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
            "Configuración AceStream Proxies",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(4.dp))

        Text(
            "Gestiona tus instancias de AceStream Engine (Local, Directo o con VPN)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        // Lista de Proxies
        Text(
            "Proxies Disponibles:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        proxies.forEach { proxy ->
            val isSelected = proxy.id == activeProxy.id
            val isTesting = testingId == proxy.id
            val testResult = testStatusMap[proxy.id]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (proxy.isVpn) "🔒" else "🌐", style = MaterialTheme.typography.titleLarge)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        proxy.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (proxy.isVpn) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("VPN", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Text(
                                    "http://${proxy.host}:${proxy.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSelected) {
                            Text(
                                "✓ ACTIVO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Seleccionar
                        if (!isSelected) {
                            var isSelectFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = {
                                    aceStreamManager.setSelectedProxy(proxy)
                                    refreshProxies()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { isSelectFocused = it.isFocused }
                                    .border(
                                        width = if (isSelectFocused) 2.dp else 0.dp,
                                        color = if (isSelectFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                        shape = ButtonDefaults.shape
                                    )
                            ) {
                                Text("ACTIVAR", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Probar
                        var isTestFocused by remember { mutableStateOf(false) }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    testingId = proxy.id
                                    val available = aceStreamManager.isProxyAvailable(proxy)
                                    testStatusMap = testStatusMap + (proxy.id to available)
                                    testingId = null
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { isTestFocused = it.isFocused }
                                .border(
                                    width = if (isTestFocused) 2.dp else 0.dp,
                                    color = if (isTestFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = ButtonDefaults.shape
                                )
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("PROBAR", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Editar
                        var isEditFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showEditDialog = proxy },
                            modifier = Modifier
                                .onFocusChanged { isEditFocused = it.isFocused }
                                .background(
                                    color = if (isEditFocused) com.example.xuper.ui.theme.FocusBackground else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .border(
                                    width = if (isEditFocused) 2.dp else 0.dp,
                                    color = if (isEditFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = if (isEditFocused) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Eliminar (si no es el único)
                        if (proxies.size > 1) {
                            var isDeleteFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    aceStreamManager.deleteProxy(proxy.id)
                                    refreshProxies()
                                },
                                modifier = Modifier
                                    .onFocusChanged { isDeleteFocused = it.isFocused }
                                    .background(
                                        color = if (isDeleteFocused) com.example.xuper.ui.theme.FocusBackground else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .border(
                                        width = if (isDeleteFocused) 2.dp else 0.dp,
                                        color = if (isDeleteFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = if (isDeleteFocused) Color.Black else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Resultado prueba
                    testResult?.let { success ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (success) "✓ Conexión exitosa" else "✗ Sin respuesta",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (success) com.example.xuper.ui.theme.Success else com.example.xuper.ui.theme.Error
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Botón Añadir Proxy
        var isAddFocused by remember { mutableStateOf(false) }
        Button(
            onClick = {
                showEditDialog = AceStreamProxy(
                    name = "Nuevo Proxy",
                    host = "127.0.0.1",
                    port = 6878
                )
                isAddingNew = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isAddFocused = it.isFocused }
                .border(
                    width = if (isAddFocused) 2.dp else 0.dp,
                    color = if (isAddFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                    shape = ButtonDefaults.shape
                )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("AÑADIR NUEVO PROXY ACESTREAM")
        }

        Spacer(Modifier.height(16.dp))

        // Escáner de Red Local (LAN)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Escáner de Red Local (LAN)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = subnetPrefix,
                        onValueChange = { subnetPrefix = it.trim() },
                        label = { Text("Subred (ej: 192.168.1)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    var isScanFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            scope.launch {
                                isScanningLan = true
                                hasScanned = false
                                scanResults = aceStreamManager.scanLocalNetwork(subnetPrefix.ifEmpty { "192.168.1" })
                                isScanningLan = false
                                hasScanned = true
                            }
                        },
                        enabled = !isScanningLan,
                        modifier = Modifier
                            .onFocusChanged { isScanFocused = it.isFocused }
                            .border(
                                width = if (isScanFocused) 2.dp else 0.dp,
                                color = if (isScanFocused) com.example.xuper.ui.theme.FocusBorder else Color.Transparent,
                                shape = ButtonDefaults.shape
                            )
                    ) {
                        if (isScanningLan) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("Escaneando...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ESCANEAR")
                        }
                    }
                }

                if (hasScanned && scanResults.isEmpty()) {
                    Text(
                        "No se encontraron motores AceStream en $subnetPrefix.1-254",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                scanResults.forEach { hostPort ->
                    val parts = hostPort.split(":")
                    val foundHost = parts.getOrNull(0) ?: return@forEach
                    val foundPort = parts.getOrNull(1)?.toIntOrNull() ?: 6878
                    val alreadyExists = proxies.any { it.host == foundHost && it.port == foundPort }

                    Surface(
                        onClick = {
                            if (!alreadyExists) {
                                val newProxy = AceStreamProxy(
                                    name = "LAN $foundHost",
                                    host = foundHost,
                                    port = foundPort
                                )
                                aceStreamManager.addOrUpdateProxy(newProxy)
                                aceStreamManager.setSelectedProxy(newProxy)
                                refreshProxies()
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("✓ Encontrado: $hostPort", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (alreadyExists) "Ya añadido" else "+ Añadir y activar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Info Docker m3u_stream_proxy
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Configuración m3u_stream_proxy:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(6.dp))
                Text("• Sin VPN: port 6879 (docker-compose-ace-no-proxy.yml)", style = MaterialTheme.typography.bodySmall)
                Text("• Con VPN: port 6878 (docker-compose-ace-proxy.yml con Gluetun)", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    // Diálogo Editar / Añadir Proxy
    showEditDialog?.let { proxyToEdit ->
        var name by remember { mutableStateOf(proxyToEdit.name) }
        var host by remember { mutableStateOf(proxyToEdit.host) }
        var port by remember { mutableStateOf(proxyToEdit.port.toString()) }
        var isVpn by remember { mutableStateOf(proxyToEdit.isVpn) }

        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text(if (isAddingNew) "Añadir Proxy AceStream" else "Editar Proxy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Proxy") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host / IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { if (it.all { c -> c.isDigit() }) port = it },
                        label = { Text("Puerto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isVpn, onCheckedChange = { isVpn = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Utiliza VPN (Gluetun / Tunnel)")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = proxyToEdit.copy(
                        name = name.ifEmpty { "AceStream Proxy" },
                        host = host.ifEmpty { "127.0.0.1" },
                        port = port.toIntOrNull() ?: 6878,
                        isVpn = isVpn
                    )
                    aceStreamManager.addOrUpdateProxy(updated)
                    refreshProxies()
                    showEditDialog = null
                    isAddingNew = false
                }) {
                    Text("GUARDAR")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = null
                    isAddingNew = false
                }) {
                    Text("CANCELAR")
                }
            }
        )
    }
}
