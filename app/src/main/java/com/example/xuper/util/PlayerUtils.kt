package com.example.xuper.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri

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
                setDataAndType("acestream://$cleanAceId".toUri(), "application/x-acestream")
                setPackage("org.acestream.media")
                putExtra("name", name)
            },
            // 2. Official URI without Mime Type
            Intent(Intent.ACTION_VIEW).apply {
                data = "acestream://$cleanAceId".toUri()
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
            Intent(Intent.ACTION_VIEW, "acestream://$cleanAceId".toUri())
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
        val input = urlOrId.trim()

        // 1. Si empieza por acestream://
        if (input.startsWith("acestream://", ignoreCase = true)) {
            val after = input.substringAfter("acestream://").substringBefore("?").substringBefore("/").trim()
            val match = Regex("[a-fA-F0-9]{40}").find(after)
            if (match != null) return match.value.lowercase()
        }

        // 2. Si contiene id= en una URL de AceStream/proxy
        if (input.contains("id=", ignoreCase = true) && (input.contains("acestream", ignoreCase = true) || input.contains("/ace/", ignoreCase = true) || input.contains("127.0.0.1:6878"))) {
            val afterId = input.substringAfter("id=").substringBefore("&").substringBefore("/").trim()
            val match = Regex("[a-fA-F0-9]{40}").find(afterId)
            if (match != null) return match.value.lowercase()
        }

        // 3. Si contiene /ace/ o .acelive
        if (input.contains("/ace/", ignoreCase = true) || input.contains(".acelive", ignoreCase = true)) {
            val match = Regex("[a-fA-F0-9]{40}").find(input)
            if (match != null) return match.value.lowercase()
        }

        // 4. Si NO es una URL HTTP/HTTPS y es un ID crudo de 40 caracteres hexadecimales
        if (!input.startsWith("http://", ignoreCase = true) && !input.startsWith("https://", ignoreCase = true)) {
            val match = Regex("^[a-fA-F0-9]{40}$").find(input)
            if (match != null) return match.value.lowercase()
        }

        // Cualquier otra URL (ej. ZicoTV, IPTV M3U8 con token=...) NO es AceStream
        return ""
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

    fun formatAceStreamHttpUrl(urlOrId: String, host: String = "127.0.0.1", port: Int = 6878): String {
        val id = getAceId(urlOrId)
        return if (id.isNotEmpty()) {
            "http://$host:$port/ace/manifest.m3u8?id=$id"
        } else {
            urlOrId
        }
    }

    fun formatAceStreamGetStreamUrl(urlOrId: String, host: String = "127.0.0.1", port: Int = 6878): String {
        val id = getAceId(urlOrId)
        return if (id.isNotEmpty()) {
            "http://$host:$port/ace/getstream?id=$id"
        } else {
            urlOrId
        }
    }

    /**
     * Verificar si una URL es de Acestream
     */
    fun isAceStreamUrl(url: String): Boolean {
        if (url.contains("zicotv", ignoreCase = true) || url.contains("bp.zicotv.cc", ignoreCase = true)) {
            return false
        }
        return url.startsWith("acestream://", ignoreCase = true) ||
               url.contains("127.0.0.1:6878") ||
               url.contains("/ace/") ||
               getAceId(url).isNotEmpty()
    }

    fun openInAceStreamApp(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(url.toUri(), "application/x-mpegurl")
                setPackage("org.acestream.media")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback sin paquete por si acaso
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(url.toUri(), "application/x-mpegurl")
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
            setDataAndType(url.toUri(), "video/*")
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
