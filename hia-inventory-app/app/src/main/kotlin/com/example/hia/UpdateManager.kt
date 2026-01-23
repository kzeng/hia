package com.example.hia

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/kzeng/hia/releases/latest"

    suspend fun checkForUpdates(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        val currentVersion = BuildConfig.VERSION_NAME

        val latestJson = fetchLatestReleaseJson() ?: return@withContext UpdateResult.Error("无法获取版本信息")
        val latestVersion = latestJson.optString("tag_name")
        if (latestVersion.isNullOrBlank()) {
            return@withContext UpdateResult.Error("版本号为空")
        }

        if (!isNewerVersion(latestVersion, currentVersion)) {
            return@withContext UpdateResult.UpToDate
        }

        val assets = latestJson.optJSONArray("assets") ?: return@withContext UpdateResult.Error("未找到可下载的安装包")
        if (assets.length() == 0) return@withContext UpdateResult.Error("未找到可下载的安装包")

        // 简单起见，取第一个 asset，实际可以按名字过滤 .apk
        val apkAsset = assets.getJSONObject(0)
        val apkUrl = apkAsset.optString("browser_download_url")
        if (apkUrl.isBlank()) return@withContext UpdateResult.Error("安装包下载地址为空")

        val apkFile = downloadApk(context, apkUrl) ?: return@withContext UpdateResult.Error("下载失败")

        val ok = installApk(context, apkFile)
        if (ok) UpdateResult.Updated(latestVersion) else UpdateResult.Error("触发安装失败")
    }

    private fun fetchLatestReleaseJson(): JSONObject? {
        return try {
            val url = URL(LATEST_RELEASE_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            conn.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                JSONObject(body)
            }
        } catch (_: Exception) {
            null
        }
    }

    // 简单语义版本比较：v1.2.3 形式，忽略前缀 v/V
    private fun isNewerVersion(latest: String, current: String): Boolean {
        fun parse(v: String) = v.trim().trimStart('v', 'V')
            .split(".")
            .mapNotNull { it.toIntOrNull() }

        val l = parse(latest)
        val c = parse(current)
        val max = maxOf(l.size, c.size)
        for (i in 0 until max) {
            val li = l.getOrNull(i) ?: 0
            val ci = c.getOrNull(i) ?: 0
            if (li > ci) return true
            if (li < ci) return false
        }
        return false
    }

    private fun downloadApk(context: Context, urlStr: String): File? {
        return try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            val apkFile = File(context.cacheDir, "update-latest.apk")
            conn.inputStream.use { input ->
                FileOutputStream(apkFile).use { out ->
                    input.copyTo(out)
                }
            }
            apkFile
        } catch (_: Exception) {
            null
        }
    }

    private fun installApk(context: Context, apkFile: File): Boolean {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

sealed class UpdateResult {
    data object UpToDate : UpdateResult()
    data class Updated(val newVersion: String) : UpdateResult()
    data class Error(val reason: String) : UpdateResult()
}
