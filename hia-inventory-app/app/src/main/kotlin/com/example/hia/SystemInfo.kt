package com.example.hia

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Locale

data class SystemInfo(
    val cpu: String,
    val ram: String,
    val disk: String,
    val os: String,
)

object SystemInfoProvider {
    fun get(context: Context): SystemInfo {
        val cpu = buildCpuInfo()
        val ram = buildRamInfo(context)
        val disk = buildDiskInfo()
        val os = buildOsInfo()
        return SystemInfo(cpu = cpu, ram = ram, disk = disk, os = os)
    }

    private fun buildCpuInfo(): String {
        val cores = try { Runtime.getRuntime().availableProcessors() } catch (_: Exception) { 0 }
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: Build.CPU_ABI ?: "unknown"
        val soc = if (Build.VERSION.SDK_INT >= 31) {
            listOfNotNull(Build.SOC_MANUFACTURER, Build.SOC_MODEL).joinToString(" ").ifBlank { null }
        } else null
        val hardware = Build.HARDWARE.takeIf { it.isNotBlank() }
        val model = readCpuModelFromProc()

        val parts = mutableListOf<String>()
        if (cores > 0) parts += "${cores}核"
        parts += abi
        when {
            !soc.isNullOrBlank() -> parts += soc
            !hardware.isNullOrBlank() -> parts += hardware
            !model.isNullOrBlank() -> parts += model
        }
        return parts.joinToString(", ")
    }

    private fun readCpuModelFromProc(): String? {
        return try {
            val file = File("/proc/cpuinfo")
            if (!file.exists()) return null
            BufferedReader(FileReader(file)).use { br ->
                br.lineSequence()
                    .firstOrNull { it.lowercase(Locale.US).contains("model name") || it.contains("Hardware") || it.contains("Processor") }
                    ?.substringAfter(":")
                    ?.trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildRamInfo(context: Context): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val total = mi.totalMem
            val avail = mi.availMem
            val used = (total - avail).coerceAtLeast(0)
            "已用 ${formatBytes(used)} / 共 ${formatBytes(total)}"
        } catch (_: Exception) {
            "未知"
        }
    }

    private fun buildDiskInfo(): String {
        return try {
            val dataDir: File = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val total = stat.totalBytes
            val avail = stat.availableBytes
            val used = (total - avail).coerceAtLeast(0)
            "已用 ${formatBytes(used)} / 共 ${formatBytes(total)}"
        } catch (_: Exception) {
            "未知"
        }
    }

    private fun buildOsInfo(): String {
        val release = Build.VERSION.RELEASE ?: "?"
        val sdk = Build.VERSION.SDK_INT
        val manufacturer = Build.MANUFACTURER ?: "?"
        val model = Build.MODEL ?: "?"
        return "Android $release (SDK $sdk), $manufacturer $model"
    }

    private fun formatBytes(bytes: Long): String {
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
