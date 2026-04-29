package com.example.xuper.ui.screens

import androidx.compose.foundation.layout.*
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
    val context = LocalContext.current
    val settings = remember { XuperConfigHelper.obtenerAjustes(context) }
    
    var apiUrl by remember { mutableStateOf(settings[0]) }
    var user by remember { mutableStateOf(settings[1]) }
    var pass by remember { mutableStateOf(settings[2]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Configuración Xuper TV", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = apiUrl,
            onValueChange = { apiUrl = it },
            label = { Text("URL de la API") },
            modifier = Modifier.fillMaxWidth(0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                XuperConfigHelper.guardarAjustes(context, apiUrl, user, pass)
            },
            modifier = Modifier.fillMaxWidth(0.4f)
        ) {
            Text("Guardar Configuración")
        }
    }
}
