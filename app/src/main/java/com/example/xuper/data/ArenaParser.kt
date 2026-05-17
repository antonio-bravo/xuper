package com.example.xuper.data

import com.example.xuper.model.ArenaEvent
import com.example.xuper.model.ArenaStream
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object ArenaParser {
    private val client = getUnsafeOkHttpClient()
    private const val API_KEY = "fc8c75bd41f06b0fa1d32c8b0b76493d"

    val sources = listOf(
        "https://arena4viewer.in/misguia2.php",
        "https://www.arena4viewer.ru/misguia2.php",
        "https://www.arena4viewer.app/misguia2.php",
        "https://www.arena4viewer.co.in/misguia2.php",
        "https://www.arena4viewer.cool/misguia2.php",
        "https://www.arena4viewer.top/misguia2.php",
        "https://www.arena4viewer.lv/misguia2.php",
    )

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            return OkHttpClient()
        }
    }

    suspend fun fetchArenaData(sourceUrl: String? = null): Pair<List<ArenaEvent>, Map<String, String>> = withContext(Dispatchers.IO) {
        var html = ""
        val urlsToTry = if (sourceUrl != null) listOf(sourceUrl) else sources
        
        val expireDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        val headers = mapOf(
            "User-Agent" to "Dalvik/2.1.0 (Linux; U; Android 14; Pixel 7 Build/UQ1A.231205.015)",
            "X-Requested-With" to "com.bone.android.a4v.oficial",
            "Content-Type" to "application/x-www-form-urlencoded",
            "Accept-Encoding" to "gzip",
            "Connection" to "Keep-Alive"
        )

        for (url in urlsToTry) {
            try {
                val formBody = FormBody.Builder()
                    .add("key", API_KEY)
                    .add("expire", expireDate)
                    .build()

                val requestBuilder = Request.Builder()
                    .url(url)
                    .post(formBody)
                
                headers.forEach { (name, value) -> requestBuilder.addHeader(name, value) }
                
                val response = client.newCall(requestBuilder.build()).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.contains("<table") || body.contains("acestream://")) {
                        html = body
                        break
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }

        if (html.isEmpty()) return@withContext Pair(emptyList(), emptyMap())

        val streamsMap = mutableMapOf<String, String>()
        
        // Regex para capturar av1#acestream://ID
        val canalPattern = Regex("""av\s*(\d{1,3})\s*#acestream://([a-fA-F0-9]{40})""", RegexOption.IGNORE_CASE)
        canalPattern.findAll(html).forEach { match ->
            val num = match.groupValues[1]
            val hash = match.groupValues[2].lowercase()
            streamsMap["AV$num"] = hash
        }

        // Parse Agenda (Table)
        val events = mutableListOf<ArenaEvent>()
        val doc = Jsoup.parse(html)
        val table = doc.select("table").firstOrNull()
        table?.select("tr")?.forEach { row ->
            val cells = row.select("td, th")
            if (cells.size >= 5) {
                // Estructura: DAY (opcional) | TIME | SPORT | COMPETITION | EVENT | LIVE
                val cleanCells = cells.map { it.text().trim() }
                val hasDate = cleanCells.size >= 6 && cleanCells[0].contains("/")
                val offset = if (hasDate) 1 else 0
                
                val timeStr = cleanCells[offset]
                val sport = cleanCells[offset + 1].uppercase()
                val competition = cleanCells[offset + 2].uppercase()
                val eventName = cleanCells[offset + 3]
                val liveList = cleanCells[offset + 4]

                if (timeStr.contains(":")) {
                    val avNums = Regex("""\d+""").findAll(liveList).map { "AV${it.value}" }.toList()
                    if (avNums.isNotEmpty()) {
                        events.add(ArenaEvent(
                            time = timeStr,
                            sport = sport,
                            title = eventName,
                            competition = competition,
                            channels = avNums
                        ))
                    }
                }
            }
        }

        Pair(events, streamsMap)
    }
}
