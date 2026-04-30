package com.example.xuper.data

import com.example.xuper.model.ArenaEvent
import com.example.xuper.model.ArenaStream
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object ArenaParser {
    private val client = OkHttpClient()
    val sources = listOf(
        "http://www.arena4viewer.in/misguia2.php",
        "http://www.arena4viewer.pl/misguia2.php",
        "https://www.arena4viewer.co.in/misguia2.php",
        "https://www.arena4viewer.cool/misguia2.php",
    )

    suspend fun fetchArenaData(sourceUrl: String? = null): Pair<List<ArenaEvent>, Map<String, String>> = withContext(Dispatchers.IO) {
        var html = ""
        val urlsToTry = if (sourceUrl != null) listOf(sourceUrl) else sources
        for (url in urlsToTry) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    html = response.body?.string() ?: ""
                    if (html.isNotEmpty()) break
                }
            } catch (e: Exception) {
                continue
            }
        }

        if (html.isEmpty()) return@withContext Pair(emptyList(), emptyMap())

        val doc = Jsoup.parse(html)
        
        // 1. Parse Streams (Hidden div)
        val streamsMap = mutableMapOf<String, String>()
        val streamsDiv = doc.select("div.streams").firstOrNull()
        streamsDiv?.let {
            val content = it.text()
            // Typical format: NAME|URL;NAME|URL
            content.split(";").forEach { entry ->
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    streamsMap[parts[0].trim()] = parts[1].trim()
                }
            }
        }

        // 2. Parse Agenda (Table)
        val events = mutableListOf<ArenaEvent>()
        val table = doc.select("table").firstOrNull()
        table?.select("tr")?.forEach { row ->
            val cols = row.select("td")
            if (cols.size >= 5) {
                events.add(ArenaEvent(
                    time = cols[0].text().trim(),
                    sport = cols[1].text().trim(),
                    title = cols[2].text().trim(),
                    competition = cols[3].text().trim(),
                    channels = cols[4].text().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                ))
            }
        }

        Pair(events, streamsMap)
    }
}
