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
            val content = response.body?.string() ?: ""
            if (url.endsWith(".json") || content.trim().startsWith("{")) {
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
                    val hash = obj.optString("hash", "")
                    val group = obj.optString("group", "Otros")
                    val logo = obj.optString("logo", "")

                    if (hash.isNotEmpty()) {
                        channels.add(
                            Channel(
                                name = title,
                                url = "acestream://$hash",
                                logo = logo.ifEmpty { null },
                                category = group,
                                sourceListName = listName
                            )
                        )
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
                if (trimmedLine.isNotEmpty()) {
                    channels.add(
                        Channel(
                            name = currentName.ifEmpty { "Canal" },
                            url = trimmedLine,
                            logo = currentLogo.ifEmpty { null },
                            category = currentCategory,
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
