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
        val aceId = when {
            urlOrId.startsWith("acestream://") -> urlOrId.substringAfter("acestream://")
            urlOrId.contains("id=") -> {
                urlOrId.substringAfter("id=").substringBefore("&")
            }
            urlOrId.length == 40 && urlOrId.all { it.isLetterOrDigit() } -> urlOrId
            else -> ""
        }.trim()

        // Clean ID (sometimes there are extra chars)
        return if (aceId.length >= 40) aceId.take(40) else aceId
    }

    fun formatAceStreamHttpUrl(urlOrId: String): String {
        val id = getAceId(urlOrId)
        return if (id.isNotEmpty()) {
            "http://127.0.0.1:6878/ace/manifest.m3u8?id=$id"
        } else {
            urlOrId
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
