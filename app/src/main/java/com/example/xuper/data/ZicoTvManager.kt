package com.example.xuper.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

data class ZicoTvChannel(
    val id: Int,
    val name: String,
    val group: String,
    val logoUrl: String? = null
)

data class ZicoTvTokenInfo(
    val channelId: Int,
    val channelName: String,
    val token: String,
    val streamUrl: String,
    val cdn: String,
    val ttlSeconds: Long,
    val expiresAtMillis: Long
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= (expiresAtMillis - 120_000) // Renovar 2 min antes
}

class ZicoTvManager(private val context: Context) {

    companion object {
        private const val TAG = "ZicoTvManager"
        private const val BASE_URL = "https://bp.zicotv.cc"
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.3 Safari/605.1.15"

        val CANALES_FIJOS = mapOf(
            1 to ZicoTvChannel(1, "DAZN LaLiga", "Fútbol"),
            2 to ZicoTvChannel(2, "DAZN 1 Spain", "Deportes"),
            3 to ZicoTvChannel(3, "DAZN 2 Spain", "Deportes"),
            4 to ZicoTvChannel(4, "LaLiga TV Bar", "Fútbol"),
            5 to ZicoTvChannel(5, "Movistar Liga de Campeones 3", "Fútbol"),
            6 to ZicoTvChannel(6, "Movistar Liga de Campeones", "Fútbol"),
            7 to ZicoTvChannel(7, "Movistar Liga de Campeones 4", "Fútbol"),
            8 to ZicoTvChannel(8, "Movistar Liga de Campeones 2", "Fútbol"),
            9 to ZicoTvChannel(9, "Movistar #Vamos", "Deportes"),
            10 to ZicoTvChannel(10, "ESPN Deportes", "Deportes"),
            11 to ZicoTvChannel(11, "DAZN F1 Spain", "Motor"),
            12 to ZicoTvChannel(12, "LaLiga TV Hypermotion", "Fútbol"),
            13 to ZicoTvChannel(13, "Sky Sports Main Event", "Deportes"),
            14 to ZicoTvChannel(14, "Movistar Liga de Campeones 5", "Fútbol"),
            15 to ZicoTvChannel(15, "DAZN LaLiga 2", "Fútbol"),
            16 to ZicoTvChannel(16, "Sport TV 2", "Deportes"),
            17 to ZicoTvChannel(17, "Amazon Prime Video Spain", "Deportes"),
            18 to ZicoTvChannel(18, "Movistar Deportes 3", "Deportes"),
            19 to ZicoTvChannel(19, "Movistar Deportes", "Deportes"),
            20 to ZicoTvChannel(20, "Movistar Deportes 6", "Deportes"),
            21 to ZicoTvChannel(21, "UFC", "Combate"),
            22 to ZicoTvChannel(22, "LaLiga TV 2 por Movistar Plus", "Fútbol"),
            23 to ZicoTvChannel(23, "Eurosport 1 Spain", "Deportes"),
            24 to ZicoTvChannel(24, "LaLiga TV Hypermotion 2", "Fútbol"),
            25 to ZicoTvChannel(25, "NBA 1", "Baloncesto"),
            28 to ZicoTvChannel(28, "Movistar Plus+", "General"),
            32 to ZicoTvChannel(32, "Multichampions", "Fútbol"),
            34 to ZicoTvChannel(34, "DAZN 3 Spain", "Deportes"),
            35 to ZicoTvChannel(35, "Movistar Deportes 2", "Deportes"),
            36 to ZicoTvChannel(36, "Eurosport 2 Spain", "Deportes"),
            37 to ZicoTvChannel(37, "DAZN 4 Spain", "Deportes"),
            38 to ZicoTvChannel(38, "LaLiga TV Bar Aux", "Fútbol"),
            39 to ZicoTvChannel(39, "Movistar LaLiga 4", "Fútbol"),
            41 to ZicoTvChannel(41, "Movistar LaLiga 3", "Fútbol"),
            46 to ZicoTvChannel(46, "Movistar Liga de Campeones 8", "Fútbol"),
            47 to ZicoTvChannel(47, "Movistar Liga de Campeones 7", "Fútbol"),
            48 to ZicoTvChannel(48, "Movistar Liga de Campeones 6", "Fútbol")
        )
    }

    private val cachedTokens = ConcurrentHashMap<Int, ZicoTvTokenInfo>()
    private val cachedChannels = ConcurrentHashMap<Int, ZicoTvChannel>()
    private var lastChannelsUpdateMillis: Long = 0

    suspend fun fetchChannels(forceRefresh: Boolean = false): List<ZicoTvChannel> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedChannels.isNotEmpty() && (now - lastChannelsUpdateMillis < 300_000)) {
            return@withContext cachedChannels.values.sortedBy { it.name }
        }

        val channels = ConcurrentHashMap<Int, ZicoTvChannel>()
        CANALES_FIJOS.forEach { (id, channel) ->
            channels[id] = channel
        }

        try {
            val url = URL("$BASE_URL/api/v1/events")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode == 200) {
                val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                val rootObj = JSONObject(responseStr)
                val eventsArr = rootObj.optJSONArray("data")
                if (eventsArr != null) {
                    for (i in 0 until eventsArr.length()) {
                        val ev = eventsArr.getJSONObject(i)
                        val sportCat = ev.optString("sport_category", "Eventos").lowercase().replaceFirstChar { it.uppercase() }
                        val title = ev.optString("title", "")
                        val sourcesArr = ev.optJSONArray("sources")
                        if (sourcesArr != null) {
                            for (j in 0 until sourcesArr.length()) {
                                val src = sourcesArr.getJSONObject(j)
                                if (src.optString("source") == "own") {
                                    val cid = src.optInt("channel_id", -1)
                                    val cname = src.optString("name", "Canal $cid")
                                    if (cid > 0) {
                                        val fullName = if (title.isNotEmpty() && title != cname) "$cname - $title" else cname
                                        if (!channels.containsKey(cid)) {
                                            channels[cid] = ZicoTvChannel(cid, fullName, sportCat)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudieron obtener eventos dinámicos de ZicoTV: ${e.message}")
        }

        cachedChannels.clear()
        cachedChannels.putAll(channels)
        lastChannelsUpdateMillis = now
        return@withContext cachedChannels.values.sortedBy { it.name }
    }

    /**
     * Obtiene el token HLS activo para un canal determinado.
     * Solicita siempre la duración de 3 horas (`{"native": true}`).
     */
    suspend fun getTokenForChannel(channelId: Int, forceRefresh: Boolean = false): ZicoTvTokenInfo = withContext(Dispatchers.IO) {
        val cached = cachedTokens[channelId]
        if (!forceRefresh && cached != null && !cached.isExpired) {
            return@withContext cached
        }

        val playerUrlStr = "$BASE_URL/api/player/$channelId"
        val playerUrl = URL(playerUrlStr)
        val playerConn = (playerUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            connectTimeout = 10000
            readTimeout = 10000
        }

        if (playerConn.responseCode != 200) {
            playerConn.disconnect()
            throw Exception("Canal $channelId no disponible en ZicoTV (HTTP ${playerConn.responseCode})")
        }

        val html = playerConn.inputStream.bufferedReader().use { it.readText() }
        playerConn.disconnect()

        val renewPattern = Pattern.compile("var\\s+ZT_RENEW\\s*=\\s*(\\{.*?\\});")
        val matcher = renewPattern.matcher(html)
        if (!matcher.find()) {
            throw Exception("No se pudo extraer la firma ZT_RENEW para el canal $channelId")
        }

        val renewJsonStr = matcher.group(1) ?: throw Exception("Firma ZT_RENEW vacía")
        val renewObj = JSONObject(renewJsonStr)
        val renewPath = renewObj.optString("url", "")
        if (renewPath.isEmpty()) {
            throw Exception("Ruta de renovación inválida en ZicoTV")
        }

        val renewUrl = URL("$BASE_URL$renewPath")
        val renewConn = (renewUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Referer", playerUrlStr)
            setRequestProperty("Origin", BASE_URL)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 10000
            readTimeout = 10000
            doOutput = true
        }

        // Enviar payload {"native": true} para solicitar token de 3 horas (10800s)
        val payload = JSONObject().apply { put("native", true) }.toString()
        OutputStreamWriter(renewConn.outputStream, "UTF-8").use { writer ->
            writer.write(payload)
            writer.flush()
        }

        if (renewConn.responseCode != 200) {
            val errText = renewConn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            renewConn.disconnect()
            throw Exception("Fallo al renovar token de ZicoTV (HTTP ${renewConn.responseCode}): $errText")
        }

        val renewResponseStr = renewConn.inputStream.bufferedReader().use { it.readText() }
        renewConn.disconnect()

        val responseObj = JSONObject(renewResponseStr)
        val sourcesArr = responseObj.optJSONArray("sources") ?: throw Exception("Sin fuentes activas para canal $channelId")

        var chosenStreamUrl = ""
        var chosenCdn = "CDN"
        for (i in 0 until sourcesArr.length()) {
            val src = sourcesArr.getJSONObject(i)
            val locked = src.optBoolean("locked", false)
            val urlStr = src.optString("url", "")
            if (!locked && urlStr.isNotEmpty()) {
                chosenStreamUrl = urlStr
                chosenCdn = src.optString("label", "CDN")
                break
            }
        }

        if (chosenStreamUrl.isEmpty()) {
            throw Exception("Todas las fuentes del canal $channelId están bloqueadas o vacías")
        }

        val ttlSeconds = responseObj.optLong("ttl", 10800L) // 3 horas por defecto
        val expiresAtMillis = System.currentTimeMillis() + (ttlSeconds * 1000L)

        // Extraer token de la URL
        val tokenRegex = Regex("[?&]token=([^&]+)")
        val tokenMatch = tokenRegex.find(chosenStreamUrl)
        val tokenStr = tokenMatch?.groupValues?.get(1) ?: ""

        val channelName = CANALES_FIJOS[channelId]?.name ?: "Canal $channelId"

        val tokenInfo = ZicoTvTokenInfo(
            channelId = channelId,
            channelName = channelName,
            token = tokenStr,
            streamUrl = chosenStreamUrl,
            cdn = chosenCdn,
            ttlSeconds = ttlSeconds,
            expiresAtMillis = expiresAtMillis
        )

        cachedTokens[channelId] = tokenInfo
        Log.d(TAG, "Token ZicoTV (3h) generado para $channelName: ttl=${ttlSeconds}s, url=$chosenStreamUrl")
        return@withContext tokenInfo
    }

    fun clearCache() {
        cachedTokens.clear()
        cachedChannels.clear()
        lastChannelsUpdateMillis = 0
    }
}
