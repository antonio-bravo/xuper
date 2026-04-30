package com.example.xuper.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object PlayerUtils {
    fun launchAceStream(context: Context, name: String, urlOrId: String) {
        val aceId = when {
            urlOrId.startsWith("acestream://") -> urlOrId.substringAfter("acestream://")
            urlOrId.contains("id=") -> urlOrId.substringAfter("id=").substringBefore("&")
            urlOrId.length == 40 && urlOrId.all { it.isLetterOrDigit() } -> urlOrId
            else -> urlOrId
        }.trim()

        if (aceId.isEmpty()) {
            Toast.makeText(context, "ID de Acestream no válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Try several intent variations
        val intentsToTry = listOf(
            // 1. Official way with package and mime type
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("acestream://$aceId"), "application/x-acestream")
                setPackage("org.acestream.media")
                putExtra("name", name)
            },
            // 2. Without mime type but with package
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("acestream://$aceId")
                setPackage("org.acestream.media")
                putExtra("name", name)
            },
            // 3. Generic acestream URI
            Intent(Intent.ACTION_VIEW, Uri.parse("acestream://$aceId")),
            // 4. Using content_id extra
            Intent(Intent.ACTION_VIEW).apply {
                setPackage("org.acestream.media")
                putExtra("content_id", aceId)
                putExtra("name", name)
            }
        )

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                continue
            }
        }

        Toast.makeText(context, "No se pudo abrir Ace Stream. ¿Está instalada?", Toast.LENGTH_SHORT).show()
    }
}
