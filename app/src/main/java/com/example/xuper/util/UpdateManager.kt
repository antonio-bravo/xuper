package com.example.xuper.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotesUrl: String,
    val fileName: String = ""
)

object UpdateManager {
    private const val GITHUB_API_URL = "https://api.github.com/repos/antonio-bravo/xuper/releases/latest"

    @Volatile
    private var pendingInstallFile: File? = null

    @Volatile
    private var activeDownloadReceiver: BroadcastReceiver? = null

    suspend fun checkForUpdates(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Xuper-Android-App")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            if (connection.responseCode == 200) {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/antonio-bravo/xuper")

                val isDebugBuild = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                val assets = json.optJSONArray("assets")
                var matchedDownloadUrl = ""
                var matchedFileName = ""

                if (assets != null) {
                    if (isDebugBuild) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.contains("_debug.apk", ignoreCase = true)) {
                                matchedDownloadUrl = asset.optString("browser_download_url", "")
                                matchedFileName = assetName
                                break
                            }
                        }
                    } else {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true) && !assetName.contains("_debug.apk", ignoreCase = true)) {
                                matchedDownloadUrl = asset.optString("browser_download_url", "")
                                matchedFileName = assetName
                                break
                            }
                        }
                    }

                    if (matchedDownloadUrl.isEmpty()) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true)) {
                                matchedDownloadUrl = asset.optString("browser_download_url", "")
                                matchedFileName = assetName
                                break
                            }
                        }
                    }
                }

                val currentVersion = getCurrentVersionName(context)

                if (tagName.isNotEmpty() && matchedDownloadUrl.isNotEmpty() && isNewerVersion(tagName, currentVersion)) {
                    return@withContext AppUpdateInfo(
                        version = tagName,
                        downloadUrl = matchedDownloadUrl,
                        releaseNotesUrl = htmlUrl,
                        fileName = matchedFileName.ifEmpty { "xuper-v$tagName.apk" }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val cleanLatest = latest.removePrefix("v").removePrefix("V").trim()
        val cleanCurrent = current.removePrefix("v").removePrefix("V").trim()
        if (cleanLatest.isEmpty() || cleanLatest.equals(cleanCurrent, ignoreCase = true)) return false

        val latestParts = cleanLatest.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toLongOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toLongOrNull() }

        if (latestParts.isNotEmpty() && currentParts.isNotEmpty()) {
            val length = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until length) {
                val l = latestParts.getOrElse(i) { 0L }
                val c = currentParts.getOrElse(i) { 0L }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        }

        return false
    }

    fun downloadAndInstallApk(context: Context, updateInfo: AppUpdateInfo) {
        val downloadUrl = updateInfo.downloadUrl
        val fileName = updateInfo.fileName.ifEmpty { "xuper-v${updateInfo.version}.apk" }

        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists()) {
                downloadDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".apk", ignoreCase = true)) {
                        file.delete()
                    }
                }
            }

            val request = DownloadManager.Request(downloadUrl.toUri()).apply {
                setTitle("Actualización Xuper (v${updateInfo.version})")
                setDescription("Descargando $fileName...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)
            Toast.makeText(context, "Iniciando descarga de v${updateInfo.version}...", Toast.LENGTH_LONG).show()

            activeDownloadReceiver?.let { prev ->
                try {
                    context.applicationContext.unregisterReceiver(prev)
                } catch (_: Exception) {}
                activeDownloadReceiver = null
            }

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                    if (id == downloadId) {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        manager.query(query)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                if (statusIdx != -1 && cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                                    val downloadedFile = File(
                                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                                        fileName
                                    )
                                    if (downloadedFile.exists()) {
                                        promptInstallApk(context, downloadedFile)
                                    } else {
                                        openInBrowser(context, downloadUrl)
                                    }
                                } else {
                                    Toast.makeText(context, "Error al descargar la actualización", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                openInBrowser(context, downloadUrl)
                            }
                        } ?: openInBrowser(context, downloadUrl)

                        try {
                            context.applicationContext.unregisterReceiver(this)
                        } catch (_: Exception) {}
                        if (activeDownloadReceiver === this) {
                            activeDownloadReceiver = null
                        }
                    }
                }
            }
            activeDownloadReceiver = onComplete

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
                context.applicationContext.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
            openInBrowser(context, downloadUrl)
        }
    }

    fun checkPendingInstall(context: Context) {
        val file = pendingInstallFile
        if (file != null && file.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()) {
                promptInstallApk(context, file)
            }
        }
    }

    fun promptInstallApk(context: Context, file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    pendingInstallFile = file
                    Toast.makeText(context, "Por favor, autoriza la instalación de fuentes desconocidas para Xuper", Toast.LENGTH_LONG).show()
                    val permissionIntent = Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        "package:${context.packageName}".toUri()
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(permissionIntent)
                    return
                }
            }

            pendingInstallFile = null
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            openInBrowser(context, file.absolutePath)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
        }
    }
}
