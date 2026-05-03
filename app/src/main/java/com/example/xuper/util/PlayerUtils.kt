package com.example.xuper.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object PlayerUtils {
    fun launchAceStream(context: Context, name: String, urlOrId: String) {
        val cleanAceId = getAceId(urlOrId)

        if (cleanAceId.isEmpty()) {
            if (urlOrId.startsWith("http")) {
                openAsGenericVideo(context, urlOrId)
                return
            }
            Toast.makeText(context, "ID de Acestream no válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Try several intent variations to ensure AceStream opens correctly
        val intentsToTry = listOf(
            // 1. Official URI with Mime Type (Standard)
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("acestream://$cleanAceId"), "application/x-acestream")
                setPackage("org.acestream.media")
                putExtra("name", name)
            },
            // 2. Official URI without Mime Type
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("acestream://$cleanAceId")
                setPackage("org.acestream.media")
                putExtra("name", name)
                putExtra("org.acestream.media.EXTRA_CONTENT_ID", cleanAceId)
            },
            // 3. Using specific extras for content ID (Some TV versions)
            Intent(Intent.ACTION_VIEW).apply {
                setPackage("org.acestream.media")
                putExtra("content_id", cleanAceId)
                putExtra("name", name)
            },
            // 4. Using "id" extra
            Intent(Intent.ACTION_VIEW).apply {
                setPackage("org.acestream.media")
                putExtra("id", cleanAceId)
            },
            // 5. Generic system-wide URI
            Intent(Intent.ACTION_VIEW, Uri.parse("acestream://$cleanAceId"))
        )

        var started = false
        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                started = true
                break
            } catch (e: Exception) {
                continue
            }
        }

        if (!started) {
            Toast.makeText(context, "No se pudo abrir Ace Stream. ¿Está instalada?", Toast.LENGTH_SHORT).show()
        }
    }

    fun getAceId(urlOrId: String): String {
        if (urlOrId.isEmpty()) return ""
        
        // Expresión regular para encontrar hashes de 40 caracteres hexadecimales
        val regex = Regex("[a-fA-F0-9]{40}")
        
        // Buscamos todas las ocurrencias y nos quedamos con la última
        // Esto soluciona problemas de URLs anidadas tipo id=http://...id=HASH
        val allMatches = regex.findAll(urlOrId)
        val lastMatch = allMatches.lastOrNull()
        
        return lastMatch?.value?.lowercase()?.trim() ?: ""
    }

//    fun getAceId(urlOrId: String): String {
//        if (urlOrId.isEmpty()) return ""
//
//        // Limpiamos la entrada de espacios o posibles comillas
//        val input = urlOrId.trim().replace("\"", "").replace("'", "")
//
//        // 1. Intentar extraer de id= (el más común en nuestras listas)
//        if (input.contains("id=")) {
//            val afterId = input.substringAfter("id=")
//            // Tomamos los primeros 40 caracteres si parecen ser un hash
//            val idPart = afterId.substringBefore("&").substringBefore("/").substringBefore(" ").trim()
//            if (idPart.length >= 40) {
//                val candidate = idPart.take(40)
//                if (candidate.all { it.isLetterOrDigit() }) return candidate
//            }
//        }
//
//        // 2. Intentar extraer de acestream://
//        if (input.startsWith("acestream://")) {
//            val idPart = input.substringAfter("acestream://").substringBefore("/").substringBefore("?").trim()
//            if (idPart.length >= 40) {
//                val candidate = idPart.take(40)
//                if (candidate.all { it.isLetterOrDigit() }) return candidate
//            }
//        }
//
//        // 3. Búsqueda por Regex de 40 caracteres hexadecimales (el formato estándar de AceID)
//        // Buscamos cualquier cadena de exactamente 40 caracteres alfanuméricos
//        val regex = Regex("[a-fA-F0-9]{40}")
//        val match = regex.find(input)
//        if (match != null) {
//            return match.value
//        }
//
//        // 4. Si la cadena en sí tiene 40 caracteres y es alfanumérica, asumimos que es el ID
//        if (input.length == 40 && input.all { it.isLetterOrDigit() }) {
//            return input
//        }
//
//        return ""
//    }

    fun formatAceStreamHttpUrl(urlOrId: String): String {
        val id = getAceId(urlOrId)
        return if (id.isNotEmpty()) {
            "http://127.0.0.1:6878/ace/manifest.m3u8?id=$id"
        } else {
            urlOrId
        }
    }

    fun formatAceStreamGetStreamUrl(urlOrId: String): String {
        val id = getAceId(urlOrId)
        return if (id.isNotEmpty()) {
            "http://127.0.0.1:6878/ace/getstream?id=$id"
        } else {
            urlOrId
        }
    }

    fun openInAceStreamApp(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "application/x-mpegurl")
                setPackage("org.acestream.media")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback sin paquete por si acaso
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(url), "application/x-mpegurl")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Ace Stream no está instalado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAsGenericVideo(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val chooser = Intent.createChooser(intent, "Open with...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
