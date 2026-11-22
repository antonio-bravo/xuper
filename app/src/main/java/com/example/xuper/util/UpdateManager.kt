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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotesUrl: String
)

object UpdateManager {
    private const val GITHUB_API_URL = "https://api.github.com/repos/antonio-bravo/xuper/releases/latest"

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

                val assets = json.optJSONArray("assets")
                var debugDownloadUrl = ""

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.contains("_debug.apk", ignoreCase = true)) {
                            debugDownloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }

                    // Fallback to any .apk if _debug.apk wasn't found
                    if (debugDownloadUrl.isEmpty()) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val assetName = asset.optString("name", "")
                            if (assetName.endsWith(".apk", ignoreCase = true)) {
                                debugDownloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }
                }

                val currentVersion = getCurrentVersionName(context)

                if (tagName.isNotEmpty() && debugDownloadUrl.isNotEmpty() && isNewerVersion(tagName, currentVersion)) {
                    return@withContext AppUpdateInfo(
                        version = tagName,
                        downloadUrl = debugDownloadUrl,
                        releaseNotesUrl = htmlUrl
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

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val cleanLatest = latest.removePrefix("v").trim()
        val cleanCurrent = current.removePrefix("v").trim()
        if (cleanLatest.isEmpty() || cleanLatest == cleanCurrent) return false

        val latestParts = cleanLatest.split(".").mapNotNull { it.toLongOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toLongOrNull() }

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
        val fileName = "xuper-v${updateInfo.version}_debug.apk"

        try {
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Actualización Xuper (v${updateInfo.version})")
                setDescription("Descargando _debug.apk...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType("application/vnd.android.package-archive")
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)
            Toast.makeText(context, "Iniciando descarga de v${updateInfo.version}...", Toast.LENGTH_LONG).show()

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1
                    if (id == downloadId) {
                        val downloadedFile = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            fileName
                        )
                        if (downloadedFile.exists()) {
                            promptInstallApk(context, downloadedFile)
                        } else {
                            openInBrowser(context, downloadUrl)
                        }
                        try {
                            context.applicationContext.unregisterReceiver(this)
                        } catch (_: Exception) {}
                    }
                }
            }

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

    fun promptInstallApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            openInBrowser(context, file.absolutePath)
        }
    }

    fun openInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo abrir el navegador", Toast.LENGTH_SHORT).show()
        }
    }
}
