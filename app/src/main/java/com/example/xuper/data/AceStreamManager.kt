package com.example.xuper.data

import android.content.Context
import android.content.SharedPreferences
import com.example.xuper.util.PlayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Gestor de conexión a Acestream Engine
 * Soporta:
 * - Engine local en Android (127.0.0.1:6878)
 * - Engine en Docker en red local (IP:PORT custom)
 */
class AceStreamManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("acestream_config", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PREF_CUSTOM_HOST = "custom_host"
        private const val PREF_CUSTOM_PORT = "custom_port"
        private const val PREF_USE_CUSTOM = "use_custom"

        const val DEFAULT_LOCAL_HOST = "127.0.0.1"
        const val DEFAULT_LOCAL_PORT = 6878
    }

    data class AceStreamConfig(
        val host: String,
        val port: Int,
        val isCustom: Boolean
    ) {
        val baseUrl: String get() = "http://$host:$port"

        fun getStreamUrl(contentId: String): String {
            return "$baseUrl/ace/getstream?id=$contentId"
        }

        fun getManifestUrl(contentId: String): String {
            return "$baseUrl/ace/manifest.m3u8?id=$contentId"
        }

        fun getStatUrl(contentId: String): String {
            return "$baseUrl/ace/stat?id=$contentId"
        }
    }

    data class AceStreamStats(
        val peers: Int = 0,
        val downloadSpeed: Float = 0f, // KB/s
        val uploadSpeed: Float = 0f,   // KB/s
        val status: String = "idle",
        val isLive: Boolean = false
    )

    /**
     * Obtener configuración actual
     */
    fun getConfig(): AceStreamConfig {
        val useCustom = prefs.getBoolean(PREF_USE_CUSTOM, false)

        return if (useCustom) {
            AceStreamConfig(
                host = prefs.getString(PREF_CUSTOM_HOST, DEFAULT_LOCAL_HOST) ?: DEFAULT_LOCAL_HOST,
                port = prefs.getInt(PREF_CUSTOM_PORT, DEFAULT_LOCAL_PORT),
                isCustom = true
            )
        } else {
            AceStreamConfig(
                host = DEFAULT_LOCAL_HOST,
                port = DEFAULT_LOCAL_PORT,
                isCustom = false
            )
        }
    }

    /**
     * Guardar configuración custom
     */
    fun saveCustomConfig(host: String, port: Int) {
        prefs.edit()
            .putString(PREF_CUSTOM_HOST, host)
            .putInt(PREF_CUSTOM_PORT, port)
            .putBoolean(PREF_USE_CUSTOM, true)
            .apply()
    }

    /**
     * Usar configuración local por defecto
     */
    fun useLocalConfig() {
        prefs.edit()
            .putBoolean(PREF_USE_CUSTOM, false)
            .apply()
    }

    /**
     * Verificar si el engine está disponible
     */
    suspend fun isEngineAvailable(config: AceStreamConfig = getConfig()): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Primero verificar conectividad básica
                if (config.isCustom) {
                    val address = InetAddress.getByName(config.host)
                    if (!address.isReachable(3000)) {
                        return@withContext false
                    }
                }

                // Verificar endpoint de Acestream
                val request = Request.Builder()
                    .url("${config.baseUrl}/webui/api/service?method=get_version")
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Obtener estadísticas del stream (probando múltiples endpoints si es necesario)
     */
    suspend fun getStreamStats(contentIdOrUrl: String, config: AceStreamConfig = getConfig()): AceStreamStats? {
        return withContext(Dispatchers.IO) {
            val cleanId = PlayerUtils.getAceId(contentIdOrUrl).ifEmpty { contentIdOrUrl.trim() }
            if (cleanId.isEmpty()) return@withContext null

            val urlsToTry = listOf(
                "${config.baseUrl}/ace/stat?id=$cleanId",
                "${config.baseUrl}/ace/stat?content_id=$cleanId",
                "${config.baseUrl}/ace/stat?infohash=$cleanId",
                "${config.baseUrl}/webui/api/service?method=get_stream_status&id=$cleanId"
            )

            var lastStats: AceStreamStats? = null

            for (url in urlsToTry) {
                try {
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrEmpty()) {
                            val stats = parseStats(body)
                            lastStats = stats
                            if (stats.peers > 0 || stats.status == "dl" || stats.status == "prebuf") {
                                return@withContext stats
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Continuar probando los siguientes endpoints
                }
            }

            lastStats
        }
    }

    /**
     * Parsear estadísticas JSON de Acestream de forma robusta
     */
    private fun parseStats(json: String): AceStreamStats {
        try {
            val root = JSONObject(json)
            val target = when {
                root.has("response") -> root.optJSONObject("response") ?: root
                root.has("result") -> root.optJSONObject("result") ?: root
                root.has("stat") -> root.optJSONObject("stat") ?: root
                else -> root
            }

            var peers = target.optInt("peers", -1)
            if (peers == -1) peers = target.optInt("peers_count", -1)
            if (peers == -1) peers = target.optInt("connected_peers", -1)
            if (peers == -1) peers = target.optInt("total_peers", -1)
            if (peers == -1) {
                peers = target.optString("peers", "").toIntOrNull() ?: -1
            }
            if (peers == -1) {
                val match = Regex("\"(?:peers|peers_count|total_peers)\"\\s*:\\s*\"?(\\d+)\"?").find(json)
                peers = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }

            val downSpeed = target.optDouble("down_speed", target.optDouble("speed_down", 0.0)).toFloat()
            val upSpeed = target.optDouble("up_speed", target.optDouble("speed_up", 0.0)).toFloat()
            val status = target.optString("status", "idle")

            return AceStreamStats(
                peers = if (peers < 0) 0 else peers,
                downloadSpeed = downSpeed / 1024f, // Convertir a KB/s
                uploadSpeed = upSpeed / 1024f,
                status = status,
                isLive = status == "dl" || status == "prebuf"
            )
        } catch (_: Exception) {
            val match = Regex("\"(?:peers|peers_count|total_peers)\"\\s*:\\s*\"?(\\d+)\"?").find(json)
            val peers = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
            return AceStreamStats(peers = peers)
        }
    }

    /**
     * Escanear red local en busca de Acestream Engine
     */
    suspend fun scanLocalNetwork(baseIp: String = "192.168.1"): List<String> {
        return withContext(Dispatchers.IO) {
            val foundHosts = mutableListOf<String>()
            val commonPorts = listOf(6878, 6877, 8765) // Puertos comunes de Acestream

            // Escanear rango común de IPs (1-254)
            for (i in 1..254) {
                val host = "$baseIp.$i"

                for (port in commonPorts) {
                    try {
                        val config = AceStreamConfig(host, port, true)
                        if (isEngineAvailable(config)) {
                            foundHosts.add("$host:$port")
                        }
                    } catch (e: Exception) {
                        // Continuar con siguiente IP
                    }
                }
            }

            foundHosts
        }
    }

    /**
     * Procesar URL de canal para usar el servidor configurado
     * Reemplaza 127.0.0.1:6878 por el servidor custom si está configurado
     */
    fun processChannelUrl(originalUrl: String): String {
        val config = getConfig()

        // Si no es custom, devolver URL original
        if (!config.isCustom) {
            return originalUrl
        }

        // Reemplazar 127.0.0.1:6878 por el servidor configurado
        var processedUrl = originalUrl
            .replace("127.0.0.1:6878", "${config.host}:${config.port}")
            .replace("localhost:6878", "${config.host}:${config.port}")

        // También reemplazar http://127.0.0.1 sin puerto explícito
        if (processedUrl.contains("127.0.0.1") || processedUrl.contains("localhost")) {
            processedUrl = processedUrl
                .replace("http://127.0.0.1", "http://${config.host}")
                .replace("http://localhost", "http://${config.host}")
        }

        return processedUrl
    }

    /**
     * Obtener URL procesada para reproducción interna (HLS)
     */
    fun getInternalPlaybackUrl(originalUrl: String): String {
        val aceId = originalUrl.let { url ->
            // Extraer ID de Acestream
            val regex = Regex("[a-fA-F0-9]{40}")
            regex.find(url)?.value ?: ""
        }

        if (aceId.isEmpty()) {
            return processChannelUrl(originalUrl)
        }

        val config = getConfig()
        return config.getManifestUrl(aceId)
    }
}
