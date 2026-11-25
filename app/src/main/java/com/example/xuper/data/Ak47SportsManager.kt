package com.example.xuper.data

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class Ak47Stream(
    val name: String,
    val channelName: String,
    val url: String,
    val logo: String = "",
    val group: String = "",
    val licenseKey: String? = null
)

data class Ak47Event(
    val id: String,
    val title: String,
    val channelName: String = "",
    val group: String,
    val logo: String = "",
    val isSpanishEvent: Boolean = false,
    val isMatchEvent: Boolean = false,
    val streams: List<Ak47Stream> = emptyList()
) {
    val broadcastingChannelsText: String
        get() = if (channelName.isNotEmpty()) channelName else streams.map { it.channelName }.filter { it.isNotEmpty() }.distinct().joinToString(" / ")
}

class Ak47SportsManager(private val context: Context) {

    companion object {
        private const val TAG = "Ak47SportsManager"
        private const val FIREBASE_KEY = "AIzaSyCOhPo6X5o8517oC_tFtH_L8ka3ElacTu0"
        private const val FIREBASE_CONFIG_URL = "https://firebaseremoteconfig.googleapis.com/v1/projects/ak47-sports/namespaces/firebase:fetch?key=$FIREBASE_KEY"
        private const val FALLBACK_SERVER = "https://hellobabu.xyz/"

        private val KEY_BYTES = "l9K5bT5xC1wP7pK1".toByteArray(Charsets.UTF_8)
        private val IV_BYTES = "k5K4nN7oU8hL6l19".toByteArray(Charsets.UTF_8)

        private val SPANISH_TEAMS_PATTERNS = listOf(
            "\\breal\\s+madrid\\b", "\\br\\.?\\s*madrid\\b", "\\batl[eé]tico(?:\\s+de)?\\s+madrid\\b", "\\batleti\\b",
            "\\bbar[cç]a\\b", "\\bfc\\s+barcelona\\b", "\\bbarcelona\\b", "\\bsevil+a(?:\\s+fc)?\\b",
            "\\b(?:real\\s+)?betis(?:\\s+balompi[eé])?\\b", "\\breal\\s+sociedad\\b", "\\bvillar+eal(?:\\s+cf)?\\b",
            "\\bvalencia(?:\\s+cf|\\s+basket)?\\b", "\\bathletic(?:\\s+club|\\s+bilbao)?\\b", "\\bgirona(?:\\s+fc|\\s+basket)?\\b",
            "\\bcelta(?:\\s+de\\s+vigo|\\s+vigo)?\\b", "\\b(?:ca\\s+)?osasuna\\b", "\\brayo(?:\\s+val+ecano)?\\b",
            "\\b(?:rcd\\s+)?mallorca\\b", "\\b(?:deportivo\\s+)?alav[eé]s\\b", "\\bgetafe(?:\\s+cf)?\\b",
            "\\b(?:rcd\\s+)?espanyol\\b", "\\b(?:ud\\s+)?las\\s+palmas\\b", "\\b(?:cd\\s+)?legan[eé]s\\b",
            "\\b(?:real\\s+)?val+adolid\\b", "\\blevante(?:\\s+ud)?\\b", "\\b(?:real\\s+)?zaragoza\\b",
            "\\bracing(?:\\s+de)?\\s+santander\\b", "\\b(?:racing(?:\\s+de)?\\s+)?ferrol\\b",
            "\\b(?:real\\s+)?sporting(?:\\s+de)?\\s+gij[oó]n\\b", "\\breal\\s+sporting\\b", "\\b(?:real\\s+)?oviedo\\b",
            "\\belche(?:\\s+cf)?\\b", "\\b(?:sd\\s+)?eibar\\b", "\\b(?:ud\\s+)?almer[ií]a\\b", "\\bgranada(?:\\s+cf|\\s+baloncesto)?\\b",
            "\\bc[aá]diz(?:\\s+cf)?\\b", "\\burgos(?:\\s+cf)?\\b", "\\balbacete(?:\\s+balompi[eé])?\\b", "\\bcastell[oó]n\\b",
            "\\bm[aá]laga(?:\\s+cf)?\\b", "(?<!central\\s)\\bc[oó]rdoba(?:\\s+cf)?\\b", "\\bdeportivo(?:\\s+de)?\\s+la\\s+coru[ñn]a\\b",
            "\\bdepor\\b", "\\b(?:fc\\s+)?cartagena\\b", "\\b(?:cd\\s+)?tenerife\\b", "\\b(?:cd\\s+)?eldense\\b",
            "\\b(?:cd\\s+)?mirand[eé]s\\b", "\\b(?:sd\\s+)?huesca\\b", "\\bponferradina\\b", "\\bcultural\\s+leonesa\\b",
            "\\bn[aà]stic(?:\\s+de\\s+tarragona)?\\b", "\\bgimn[aá]stic\\b", "\\brecreativo(?:\\s+de\\s+huelva)?\\b",
            "\\breal\\s+murcia\\b", "\\bucam\\s+murcia\\b", "\\bunionistas\\b", "\\bzamora\\s+cf\\b", "\\bbarakaldo\\b",
            "\\bsestao\\s+river\\b", "\\balcoyano\\b", "\\bh[eé]rcules(?:\\s+cf)?\\b", "\\bsabadell\\b", "\\bad\\s+ceuta\\b",
            "\\bbaskonia\\b", "\\bunicaja(?:\\s+m[aá]laga)?\\b", "\\bjoventut(?:\\s+badalona)?\\b", "\\bla\\s+laguna\\s+tenerife\\b",
            "\\blenovo\\s+tenerife\\b", "\\bgran\\s+canaria(?:\\s+cb)?\\b", "\\b(?:baxi\\s+)?manresa\\b", "\\bbilbao\\s+basket\\b",
            "\\bbreog[aá]n\\b", "\\bmorabanc\\s+andorra\\b", "\\bfor[cç]a\\s+lleida\\b", "\\bhiopos\\s+lleida\\b",
            "\\bleyma\\s+coru[ñn]a\\b", "\\bestudiantes(?:\\s+madrid|\\s+baloncesto)?\\b", "\\bpalencia\\s+baloncesto\\b",
            "\\bbidasoa(?:\\s+ir[uú]n)?\\b", "\\bademar\\s+le[oó]n\\b", "\\bpalma\\s+futsal\\b", "\\belpozo\\s+murcia\\b",
            "\\bmovistar\\s+inter\\b", "\\binter\\s+movistar\\b", "\\bja[eé]n\\s+para[ií]so\\b", "\\bjimbee\\s+cartagena\\b",
            "\\bespa[ñn]a\\b", "\\bspain\\b", "\\bselecci[oó]n\\s+espa[ñn]ola\\b"
        )

        private val RE_SPANISH_TEAMS = Regex(SPANISH_TEAMS_PATTERNS.joinToString("|") { "(?:$it)" }, RegexOption.IGNORE_CASE)
        private val RE_EVENT_SEPARATORS = Regex("\\b(?:vs\\.?|v|against|-|—|:)\\b|[-—:]", RegexOption.IGNORE_CASE)

        fun isSpanishTeamEvent(title: String): Boolean {
            if (title.isEmpty()) return false
            return RE_SPANISH_TEAMS.containsMatchIn(title)
        }

        fun isGenericOptionLabel(name: String): Boolean {
            if (name.isEmpty()) return true
            val clean = name.trim().lowercase()
            return clean.matches(Regex("""^(opci[oó]n|option|server|servidor|link|enlace|stream|hd|sd|\d+)(\s*\d+)?$"""))
        }

        fun normalizeCategoryName(rawCategory: String): String {
            val clean = rawCategory.trim().lowercase()
            return when {
                clean.contains("football") || clean.contains("fútbol") || clean.contains("soccer") -> "⚽ Fútbol"
                clean.contains("boxing") || clean.contains("boxeo") -> "🥊 Boxeo"
                clean.contains("basket") || clean.contains("baloncesto") || clean.contains("nba") -> "🏀 Baloncesto"
                clean.contains("motor") || clean.contains("f1") || clean.contains("motogp") -> "🏎️ Motor"
                clean.contains("ufc") || clean.contains("mma") || clean.contains("combat") || clean.contains("lucha") -> "🥊 Combate"
                clean.contains("tennis") || clean.contains("tenis") -> "🎾 Tenis"
                else -> rawCategory.trim()
            }
        }
    }

    private var activeBaseServer: String? = null
    private val cachedEvents = ConcurrentHashMap<String, Ak47Event>()
    private var lastFetchTimeMillis: Long = 0

    private suspend fun getBaseServer(): String = withContext(Dispatchers.IO) {
        activeBaseServer?.let { return@withContext it }

        try {
            val url = URL(FIREBASE_CONFIG_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 11; Pixel 5 Build/RD2A.210905.003)")
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("appId", "1:987682933046:android:b0d2f52255416b34cb0b97")
                put("appInstanceId", "12345678901234567890123456789012")
            }.toString()

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload); it.flush() }

            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(responseText)
                val entries = jsonObj.optJSONObject("entries")
                var apiUrl = entries?.optString("api_url", "")
                if (!apiUrl.isNullOrEmpty()) {
                    if (!apiUrl.endsWith("/")) apiUrl += "/"
                    Log.d(TAG, "Firebase Remote Config devolvió el servidor activo: $apiUrl")
                    activeBaseServer = apiUrl
                    return@withContext apiUrl
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error al consultar Firebase Remote Config: ${e.message}")
        }

        activeBaseServer = FALLBACK_SERVER
        return@withContext FALLBACK_SERVER
    }

    private fun decryptAesCbc(base64Text: String): String? {
        return try {
            val cleanText = base64Text.trim()
            if (cleanText.isEmpty()) return null

            val cipherTextBytes = Base64.decode(cleanText, Base64.DEFAULT)
            val secretKeySpec = SecretKeySpec(KEY_BYTES, "AES")
            val ivSpec = IvParameterSpec(IV_BYTES)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)

            val decryptedBytes = cipher.doFinal(cipherTextBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desencriptar AES: ${e.message}")
            null
        }
    }

    private suspend fun fetchAndDecryptJson(fullUrlStr: String): JSONArray? = withContext(Dispatchers.IO) {
        val targetUrlStr = if (!fullUrlStr.contains("?p=")) "$fullUrlStr?p=1" else fullUrlStr
        try {
            val url = URL(targetUrlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "okhttp/4.9.0")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (conn.responseCode == 200) {
                val encryptedText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val decryptedJsonStr = decryptAesCbc(encryptedText)
                if (!decryptedJsonStr.isNullOrEmpty()) {
                    return@withContext JSONArray(decryptedJsonStr)
                }
            } else {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error al descargar o desencriptar $targetUrlStr: ${e.message}")
        }
        return@withContext null
    }

    suspend fun fetchEvents(forceRefresh: Boolean = false): List<Ak47Event> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedEvents.isNotEmpty() && (now - lastFetchTimeMillis < 300_000)) {
            return@withContext cachedEvents.values.sortedByDescending { it.isSpanishEvent }
        }

        val baseServer = getBaseServer()
        val endpoints = listOf("sports.txt", "categories.txt")
        val rawStreams = ConcurrentHashMap<String, MutableList<Ak47Stream>>()
        val eventMetadata = ConcurrentHashMap<String, Triple<String, String, String>>() // Key -> Triple(Group, Logo, ChannelName)
        val sportsEventsKeys = ConcurrentHashMap.newKeySet<String>()

        coroutineScope {
            endpoints.forEach { ep ->
                val catUrl = baseServer + ep
                val categoriesArr = fetchAndDecryptJson(catUrl)
                if (categoriesArr != null) {
                    val catJobs = (0 until categoriesArr.length()).map { i ->
                        async {
                            val item = categoriesArr.optJSONObject(i) ?: return@async
                            val catStr = item.optString("cat", "")
                            if (catStr.isEmpty()) return@async

                            try {
                                val catObj = JSONObject(catStr)
                                if (!catObj.optBoolean("visible", true)) return@async

                                val catName = catObj.optString("name", "AK47Sports").trim()
                                val catType = catObj.optString("type", "")
                                val catApi = catObj.optString("api", "")

                                if (catType == "custom" && catApi.isNotEmpty()) {
                                    val chanUrl = baseServer + catApi
                                    val chanList = fetchAndDecryptJson(chanUrl)
                                    if (chanList != null) {
                                        for (j in 0 until chanList.length()) {
                                            val chItem = chanList.optJSONObject(j) ?: continue
                                            val chStr = chItem.optString("channel", "")
                                            if (chStr.isEmpty()) continue

                                            val chObj = JSONObject(chStr)
                                            if (!chObj.optBoolean("visible", true)) continue

                                            val chName = chObj.optString("name", "Canal").trim()
                                            val chLogo = chObj.optString("logo", "")
                                            val chLinks = chObj.optString("links", "")

                                            if (chLinks.isNotEmpty()) {
                                                val proUrl = baseServer + chLinks
                                                val proData = fetchAndDecryptJson(proUrl)
                                                if (proData != null) {
                                                    for (k in 0 until proData.length()) {
                                                        val streamObj = proData.optJSONObject(k) ?: continue
                                                        val sName = streamObj.optString("name", "").trim()
                                                        val sLink = streamObj.optString("link", "").trim()
                                                        val sApi = streamObj.optString("api", "").trim()

                                                        if (sLink.isNotEmpty()) {
                                                            val isSpecificEvent = !isGenericOptionLabel(sName)
                                                            val eventKey = if (isSpecificEvent) sName else chName
                                                            val streamChannelName = if (isSpecificEvent) chName else catName
                                                            val optionLabel = if (isSpecificEvent) chName else (if (sName.isNotEmpty()) sName else "Opción ${k + 1}")
                                                            val streamDisplayName = "$streamChannelName ($optionLabel)"
                                                            val normalizedGroup = normalizeCategoryName(catName)
                                                            val licenseKey = if (sApi.isNotEmpty() && sApi.contains(":")) sApi else null

                                                            val stream = Ak47Stream(
                                                                name = streamDisplayName,
                                                                channelName = streamChannelName,
                                                                url = sLink,
                                                                logo = chLogo,
                                                                group = normalizedGroup,
                                                                licenseKey = licenseKey
                                                            )

                                                            if (ep == "sports.txt" || isSpecificEvent) {
                                                                sportsEventsKeys.add(eventKey)
                                                            }
                                                            eventMetadata.putIfAbsent(eventKey, Triple(normalizedGroup, chLogo, streamChannelName))
                                                            rawStreams.computeIfAbsent(eventKey) { mutableListOf() }.add(stream)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error procesando categoría AK47Sports: ${e.message}")
                            }
                        }
                    }
                    catJobs.awaitAll()
                }
            }
        }

        val eventsList = mutableListOf<Ak47Event>()
        rawStreams.forEach { (eventTitle, streamsList) ->
            val meta = eventMetadata[eventTitle] ?: Triple("AK47Sports", "", "")
            val isSpanish = isSpanishTeamEvent(eventTitle)
            val isFromSportsFile = sportsEventsKeys.contains(eventTitle)
            val isMatch = isFromSportsFile || RE_EVENT_SEPARATORS.containsMatchIn(eventTitle) || isSpanish || meta.first.contains("event", ignoreCase = true) || meta.first.contains("sports", ignoreCase = true)
            val uniqueStreams = streamsList.distinctBy { it.url }

            eventsList.add(
                Ak47Event(
                    id = eventTitle.hashCode().toString(),
                    title = eventTitle,
                    channelName = meta.third,
                    group = meta.first,
                    logo = meta.second,
                    isSpanishEvent = isSpanish,
                    isMatchEvent = isMatch,
                    streams = uniqueStreams
                )
            )
        }

        val sortedEvents = eventsList.sortedWith(
            compareByDescending<Ak47Event> { it.isSpanishEvent }
                .thenBy { it.group }
                .thenBy { it.title }
        )

        cachedEvents.clear()
        sortedEvents.forEach { cachedEvents[it.id] = it }
        lastFetchTimeMillis = now

        Log.d(TAG, "Extracción de AK47Sports completada: ${sortedEvents.size} eventos agrupados (${sortedEvents.count { it.isSpanishEvent }} de equipos españoles)")
        return@withContext sortedEvents
    }

    fun clearCache() {
        cachedEvents.clear()
        lastFetchTimeMillis = 0
    }
}
