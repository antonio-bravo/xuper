package com.example.xuper.data

import com.example.xuper.model.Channel
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object M3UParser {
    private val client = OkHttpClient()

    suspend fun fetchAndParse(url: String, listName: String): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val content = response.body?.string()?.trim() ?: ""
            if (content.isEmpty()) return@withContext emptyList()

            // Detección robusta: si la URL contiene .json o el contenido parece JSON
            return@withContext if (url.contains(".json", ignoreCase = true) || content.startsWith("{") || content.startsWith("[")) {
                parseJson(content, listName)
            } else {
                parse(content, listName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseJson(content: String, listName: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val root = JSONObject(content)
            val hashesArr = root.optJSONArray("hashes")
            if (hashesArr != null) {
                for (i in 0 until hashesArr.length()) {
                    val obj = hashesArr.getJSONObject(i)
                    val title = obj.optString("title", "Canal")
                    val hash = obj.optString("hash", "").trim().lowercase()
                    val group = obj.optString("group", "Otros")
                    val logo = obj.optString("logo", "")

                    if (hash.isNotEmpty()) {
                        // Limitar longitud para evitar SQLiteBlobTooBigException
                        if (hash.length <= 100 && title.length <= 500) {
                            channels.add(
                                Channel(
                                    name = title,
                                    url = "acestream://$hash",
                                    logo = if (logo.length > 1000) null else logo.ifEmpty { null },
                                    category = group.take(100),
                                    sourceListName = listName
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return channels
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
                // Extract name
                currentName = trimmedLine.substringAfterLast(",").trim()
                if (currentName.isEmpty() || currentName.startsWith("#")) {
                    val nameRegex = """tvg-name="([^"]+)"""".toRegex()
                    currentName = nameRegex.find(trimmedLine)?.groupValues?.get(1) ?: "Canal sin nombre"
                }
                
                val logoRegex = """tvg-logo="([^"]+)"""".toRegex()
                currentLogo = logoRegex.find(trimmedLine)?.groupValues?.get(1) ?: ""
                
                val groupRegex = """group-title="([^"]+)"""".toRegex()
                val tvgGroupRegex = """tvg-group="([^"]+)"""".toRegex()
                val rawCategory = groupRegex.find(trimmedLine)?.groupValues?.get(1) 
                    ?: tvgGroupRegex.find(trimmedLine)?.groupValues?.get(1) 
                    ?: "Otros"
                
                currentCategory = rawCategory.split(";").firstOrNull()?.trim() ?: "Otros"
            } else if (trimmedLine.startsWith("#EXTGRP:")) {
                currentCategory = trimmedLine.substringAfter(":").split(";").firstOrNull()?.trim() ?: "Otros"
            } else if (!trimmedLine.startsWith("#")) {
                if (trimmedLine.isNotEmpty() && trimmedLine.length < 2000) { // Evitar URLs gigantes
                    channels.add(
                        Channel(
                            name = currentName.ifEmpty { "Canal" }.take(500),
                            url = trimmedLine,
                            logo = if (currentLogo.length > 1000) null else currentLogo.ifEmpty { null },
                            category = currentCategory.take(100),
                            sourceListName = listName,
                        ),
                    )
                    currentName = ""
                    currentLogo = ""
                    currentCategory = "Otros"
                }
            }
        }
        return channels
    }
}
