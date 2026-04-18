package com.example.xuper

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object M3UParser {
    private val client = OkHttpClient()

    suspend fun fetchAndParse(url: String, listName: String): List<Channel> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val content = response.body?.string() ?: ""
        parse(content, listName)
    }

    fun parse(content: String, listName: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentName = ""
        var currentLogo = ""
        var currentCategory = "Otros"

        for (line in lines) {
            if (line.startsWith("#EXTINF:")) {
                // Extract name
                currentName = line.substringAfterLast(",").trim()
                
                // Extract logo: tvg-logo="url"
                val logoRegex = """tvg-logo="([^"]+)"""".toRegex()
                currentLogo = logoRegex.find(line)?.groupValues?.get(1) ?: ""
                
                // Extract category: group-title="category"
                val groupRegex = """group-title="([^"]+)"""".toRegex()
                val tvgGroupRegex = """tvg-group="([^"]+)"""".toRegex()
                currentCategory = groupRegex.find(line)?.groupValues?.get(1) 
                    ?: tvgGroupRegex.find(line)?.groupValues?.get(1) 
                    ?: "Otros"
            } else if (line.startsWith("#EXTGRP:")) {
                currentCategory = line.substringAfter(":").trim()
            } else if (line.trim().isNotEmpty() && !line.startsWith("#")) {
                val url = line.trim()
                if (currentName.isNotEmpty()) {
                    channels.add(Channel(
                        name = currentName, 
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
