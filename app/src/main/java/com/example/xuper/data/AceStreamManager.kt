package com.example.xuper.data

import android.content.Context
import android.content.SharedPreferences
import com.example.xuper.util.PlayerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Representa una instancia de Proxy de AceStream (Motor Local, Proxy Directo sin VPN, Proxy con VPN, etc.)
 */
data class AceStreamProxy(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int,
    val isVpn: Boolean = false,
    val isDefault: Boolean = false
) {
    val baseUrl: String get() = "http://$host:$port"

    fun getManifestUrl(aceId: String): String = "$baseUrl/ace/manifest.m3u8?id=$aceId"
    fun getStreamUrl(aceId: String): String = "$baseUrl/ace/getstream?id=$aceId"
    fun getStatUrl(aceId: String): String = "$baseUrl/ace/stat?id=$aceId"
}

/**
 * Gestor de conexión a proxies/motores de AceStream Engine
 */
class AceStreamManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("acestream_config", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PREF_PROXIES_JSON = "proxies_list_json"
        private const val PREF_SELECTED_PROXY_ID = "selected_proxy_id"

        const val DEFAULT_LOCAL_HOST = "127.0.0.1"
        const val DEFAULT_LOCAL_PORT = 6878

        // Proxies por defecto configurados en m3u_stream_proxy
        val DEFAULT_PROXIES = listOf(
            AceStreamProxy(
                id = "no_vpn",
                name = "Proxy Sin VPN",
                host = "125.125.125.125",
                port = 6878,
                isVpn = false,
                isDefault = true
            ),
            AceStreamProxy(
                id = "vpn",
                name = "Proxy Con VPN",
                host = "125.125.125.125",
                port = 6879,
                isVpn = true,
                isDefault = false
            )
        )
    }

    data class AceStreamConfig(
        val host: String,
        val port: Int,
        val isCustom: Boolean
    ) {
        val baseUrl: String get() = "http://$host:$port"

        fun getStreamUrl(contentId: String): String = "$baseUrl/ace/getstream?id=$contentId"
        fun getManifestUrl(contentId: String): String = "$baseUrl/ace/manifest.m3u8?id=$contentId"
        fun getStatUrl(contentId: String): String = "$baseUrl/ace/stat?id=$contentId"
    }

    data class AceStreamStats(
        val peers: Int = 0,
        val downloadSpeed: Float = 0f, // KB/s
        val uploadSpeed: Float = 0f,   // KB/s
        val status: String = "idle",
        val isLive: Boolean = false
    )

    /**
     * Obtener la lista completa de proxies configurados
     */
    fun getProxies(): List<AceStreamProxy> {
        val jsonStr = prefs.getString(PREF_PROXIES_JSON, null)
        if (jsonStr.isNullOrEmpty() || jsonStr.contains("\"id\":\"local\"")) {
            saveProxies(DEFAULT_PROXIES)
            return DEFAULT_PROXIES
        }

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<AceStreamProxy>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AceStreamProxy(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Proxy AceStream"),
                        host = obj.optString("host", DEFAULT_LOCAL_HOST),
                        port = obj.optInt("port", DEFAULT_LOCAL_PORT),
                        isVpn = obj.optBoolean("isVpn", false),
                        isDefault = obj.optBoolean("isDefault", false)
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_PROXIES else list
        } catch (e: Exception) {
            DEFAULT_PROXIES
        }
    }

    /**
     * Guardar la lista de proxies
     */
    fun saveProxies(proxies: List<AceStreamProxy>) {
        val jsonArray = JSONArray()
        proxies.forEach { proxy ->
            jsonArray.put(JSONObject().apply {
                put("id", proxy.id)
                put("name", proxy.name)
                put("host", proxy.host)
                put("port", proxy.port)
                put("isVpn", proxy.isVpn)
                put("isDefault", proxy.isDefault)
            })
        }
        prefs.edit().putString(PREF_PROXIES_JSON, jsonArray.toString()).apply()
    }

    /**
     * Obtener el proxy actualmente seleccionado
     */
    fun getSelectedProxy(): AceStreamProxy {
        val proxies = getProxies()
        val selectedId = prefs.getString(PREF_SELECTED_PROXY_ID, null)
        return proxies.find { it.id == selectedId }
            ?: proxies.find { it.isDefault }
            ?: proxies.firstOrNull()
            ?: DEFAULT_PROXIES.first()
    }

    /**
     * Guardar el ID del proxy seleccionado
     */
    fun setSelectedProxy(proxy: AceStreamProxy) {
        prefs.edit().putString(PREF_SELECTED_PROXY_ID, proxy.id).apply()
    }

    /**
     * Obtener el siguiente proxy disponible para failover automático
     */
    fun getNextFailoverProxy(currentProxy: AceStreamProxy): AceStreamProxy? {
        val proxies = getProxies()
        if (proxies.size <= 1) return null
        val currentIndex = proxies.indexOfFirst { it.id == currentProxy.id }
        val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % proxies.size else 0
        return proxies[nextIndex]
    }

    /**
     * Añadir o actualizar un proxy
     */
    fun addOrUpdateProxy(proxy: AceStreamProxy) {
        val current = getProxies().toMutableList()
        val index = current.indexOfFirst { it.id == proxy.id }
        if (index >= 0) {
            current[index] = proxy
        } else {
            current.add(proxy)
        }
        saveProxies(current)
    }

    /**
     * Eliminar un proxy
     */
    fun deleteProxy(id: String) {
        val current = getProxies().filter { it.id != id }
        saveProxies(current)
    }

    /**
     * Formatear la URL de reproducción para un proxy específico.
     * Para proxies/servidores remotos (distintos de 127.0.0.1) se utiliza /ace/getstream
     * para transmisión MPEG-TS directa continua, evitando que el manifiesto HLS remoto
     * devuelva enlaces internos apuntando a 127.0.0.1.
     */
    fun getFormattedUrlForProxy(originalUrl: String, proxy: AceStreamProxy): String {
        val aceId = PlayerUtils.getAceId(originalUrl)
        if (aceId.isNotEmpty()) {
            return if (proxy.host != DEFAULT_LOCAL_HOST && proxy.host != "localhost") {
                proxy.getStreamUrl(aceId)
            } else {
                proxy.getManifestUrl(aceId)
            }
        }

        // Si no se extrae un ID de 40 caracteres, reemplazar el host/puerto
        return originalUrl
            .replace("127.0.0.1:6878", "${proxy.host}:${proxy.port}")
            .replace("localhost:6878", "${proxy.host}:${proxy.port}")
    }

    /**
     * Compatibilidad con getConfig() legacy
     */
    fun getConfig(): AceStreamConfig {
        val active = getSelectedProxy()
        return AceStreamConfig(
            host = active.host,
            port = active.port,
            isCustom = active.host != DEFAULT_LOCAL_HOST || active.port != DEFAULT_LOCAL_PORT
        )
    }

    /**
     * Guardar configuración custom legacy
     */
    fun saveCustomConfig(host: String, port: Int) {
        val customProxy = AceStreamProxy(
            id = "custom",
            name = "Proxy Personalizado",
            host = host,
            port = port,
            isVpn = false,
            isDefault = true
        )
        addOrUpdateProxy(customProxy)
        setSelectedProxy(customProxy)
    }

    /**
     * Verificar disponibilidad de un proxy
     */
    suspend fun isProxyAvailable(proxy: AceStreamProxy = getSelectedProxy()): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (proxy.host != DEFAULT_LOCAL_HOST && proxy.host != "localhost") {
                    val address = InetAddress.getByName(proxy.host)
                    if (!address.isReachable(3000)) {
                        return@withContext false
                    }
                }

                val request = Request.Builder()
                    .url("${proxy.baseUrl}/webui/api/service?method=get_version")
                    .build()

                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun isEngineAvailable(config: AceStreamConfig = getConfig()): Boolean {
        return isProxyAvailable(AceStreamProxy(id = "legacy", name = "Legacy", host = config.host, port = config.port))
    }

    /**
     * Obtener estadísticas de AceStream para un proxy
     */
    suspend fun getStreamStatsForProxy(contentIdOrUrl: String, proxy: AceStreamProxy = getSelectedProxy()): AceStreamStats? {
        return withContext(Dispatchers.IO) {
            val cleanId = PlayerUtils.getAceId(contentIdOrUrl).ifEmpty { contentIdOrUrl.trim() }
            if (cleanId.isEmpty()) return@withContext null

            val urlsToTry = listOf(
                "${proxy.baseUrl}/ace/stat?id=$cleanId",
                "${proxy.baseUrl}/ace/stat?content_id=$cleanId",
                "${proxy.baseUrl}/ace/stat?infohash=$cleanId",
                "${proxy.baseUrl}/webui/api/service?method=get_stream_status&id=$cleanId"
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
                }
            }

            lastStats
        }
    }

    suspend fun getStreamStats(contentIdOrUrl: String, config: AceStreamConfig = getConfig()): AceStreamStats? {
        val proxy = AceStreamProxy("legacy", "Legacy", config.host, config.port)
        return getStreamStatsForProxy(contentIdOrUrl, proxy)
    }

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
                downloadSpeed = downSpeed / 1024f,
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

    suspend fun scanLocalNetwork(baseIp: String = "192.168.1"): List<String> {
        return withContext(Dispatchers.IO) {
            val foundHosts = mutableListOf<String>()
            val commonPorts = listOf(6878, 6879, 6877, 8765)

            for (i in 1..254) {
                val host = "$baseIp.$i"
                for (port in commonPorts) {
                    try {
                        val proxy = AceStreamProxy("scan", "Scan", host, port)
                        if (isProxyAvailable(proxy)) {
                            foundHosts.add("$host:$port")
                        }
                    } catch (_: Exception) {
                    }
                }
            }

            foundHosts
        }
    }

    fun processChannelUrl(originalUrl: String): String {
        return getFormattedUrlForProxy(originalUrl, getSelectedProxy())
    }

    fun getInternalPlaybackUrl(originalUrl: String): String {
        return getFormattedUrlForProxy(originalUrl, getSelectedProxy())
    }
}
