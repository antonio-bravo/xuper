package com.example.xuper

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object M3UParser {
    private val client = OkHttpClient()

    suspend fun fetchAndParse(url: String, listName: String): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val content = response.body?.string() ?: ""
            parse(content, listName)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun parse(content: String, listName: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentName = ""
        var currentLogo = ""
        var currentCategory = "Otros"

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            if (trimmedLine.startsWith("#EXTINF:")) {
                // Extract name - handles cases where comma might be inside quotes or missing
                currentName = trimmedLine.substringAfterLast(",").trim()
                if (currentName.isEmpty() || currentName.startsWith("#")) {
                    val nameRegex = """tvg-name="([^"]+)"""".toRegex()
                    currentName = nameRegex.find(trimmedLine)?.groupValues?.get(1) ?: "Canal sin nombre"
                }
                
                // Extract logo: tvg-logo="url"
                val logoRegex = """tvg-logo="([^"]+)"""".toRegex()
                currentLogo = logoRegex.find(trimmedLine)?.groupValues?.get(1) ?: ""
                
                // Extract category: group-title="category"
                val groupRegex = """group-title="([^"]+)"""".toRegex()
                val tvgGroupRegex = """tvg-group="([^"]+)"""".toRegex()
                val rawCategory = groupRegex.find(trimmedLine)?.groupValues?.get(1) 
                    ?: tvgGroupRegex.find(trimmedLine)?.groupValues?.get(1) 
                    ?: "Otros"
                
                // Split categories if they contain semicolons and take the first one
                currentCategory = rawCategory.split(";").firstOrNull()?.trim() ?: "Otros"
            } else if (trimmedLine.startsWith("#EXTGRP:")) {
                currentCategory = trimmedLine.substringAfter(":").split(";").firstOrNull()?.trim() ?: "Otros"
            } else if (!trimmedLine.startsWith("#")) {
                val url = trimmedLine
                if (url.isNotEmpty()) {
                    channels.add(Channel(
                        name = if (currentName.isEmpty()) "Canal" else currentName,
                        url = url, 
                        logo = currentLogo.ifEmpty { null },
                        category = currentCategory,
                        sourceListName = listName
                    ))
                    currentName = ""
                    currentLogo = ""
                    currentCategory = "Otros"
                }
            }
        }
        return channels
    }
}
