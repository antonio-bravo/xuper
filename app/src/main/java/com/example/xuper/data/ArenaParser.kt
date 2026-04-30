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
        
        // Simular el User-Agent y headers que usa la app oficial para que el servidor inyecte los streams
        val headers = mapOf(
            "User-Agent" to "Arena4Viewer/4.0",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Connection" to "keep-alive"
        )

        for (url in urlsToTry) {
            try {
                val requestBuilder = Request.Builder().url(url)
                headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }
                
                val response = client.newCall(requestBuilder.build()).execute()
                if (response.isSuccessful) {
                    html = response.body?.string() ?: ""
                    // Verificamos si realmente contiene la agenda o al menos la estructura básica
                    if (html.contains("<table") || html.contains("streams")) break
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
        
        if (streamsDiv != null) {
            val content = streamsDiv.text()
            // El formato suele ser AV1|hash|AV2|hash|... o separado por punto y coma
            // Intentamos ambos delimitadores comunes
            val parts = if (content.contains(";")) {
                content.split(";")
            } else {
                content.split("|").chunked(2).map { it.joinToString("|") }
            }

            parts.forEach { entry ->
                val subParts = entry.split("|")
                if (subParts.size >= 2) {
                    val name = subParts[0].trim()
                    val url = subParts[1].trim()
                    if (name.isNotEmpty() && url.isNotEmpty()) {
                        streamsMap[name] = url
                    }
                }
            }
        }

        // Si el div oculto falló, intentamos buscar en scripts o comentarios (fallback común)
        if (streamsMap.isEmpty()) {
            val scriptContent = doc.select("script").text()
            val regex = Regex("(AV\\d+)\\|([a-fA-F0-9]{40})")
            regex.findAll(html + scriptContent).forEach { match ->
                streamsMap[match.groupValues[1]] = match.groupValues[2]
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
