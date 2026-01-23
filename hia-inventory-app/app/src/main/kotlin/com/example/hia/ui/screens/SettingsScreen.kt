package com.example.hia.ui.screens

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Link
import androidx.navigation.NavHostController
import com.example.hia.FtpConfig
import com.example.hia.FtpPreferences
import androidx.compose.ui.platform.LocalContext
import com.example.hia.SystemInfo
import com.example.hia.SystemInfoProvider
import com.example.hia.UpdateResult
import com.example.hia.UpdateManager
import kotlinx.coroutines.Dispatchers
import com.example.hia.ui.components.TopNavBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import kotlin.math.sqrt
import java.util.Calendar

@Composable
fun SettingsScreen(navController: NavHostController) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopNavBar(navController) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧：FTP 配置
            FtpConfigCard(modifier = Modifier.weight(0.35f), snackbarHostState = snackbarHostState)

            // 右侧：系统/APP 信息
            InfoPanel(modifier = Modifier.weight(0.65f), snackbarHostState = snackbarHostState)
        }
    }
}

@Composable
private fun FtpConfigCard(modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedConfig by remember { FtpPreferences.getConfig(context) }.collectAsState(initial = FtpConfig())

    // Local editable state initialized from savedConfig
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    // Initialize local state from savedConfig when it loads/changes
    LaunchedEffect(savedConfig) {
        server = savedConfig.server
        port = savedConfig.port.takeIf { it > 0 }?.toString() ?: ""
        user = savedConfig.user
        password = savedConfig.password
    }

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("FTP配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = server, onValueChange = { server = it.trim() }, label = { Text("服务器") })
            OutlinedTextField(value = port, onValueChange = { port = it.trim() }, label = { Text("端口") })
            OutlinedTextField(value = user, onValueChange = { user = it.trim() }, label = { Text("用户名") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation())

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val portInt = port.toIntOrNull() ?: 21
                    val valid = validateFtp(server, portInt, user)
                    scope.launch {
                        if (!valid) {
                            snackbarHostState.showSnackbar(
                                message = "配置不合法，请检查服务器/端口/用户名",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            try {
                                FtpPreferences.saveConfig(context, FtpConfig(server, portInt, user, password))
                                saved = true
                                snackbarHostState.showSnackbar(
                                    message = "配置已保存",
                                    duration = SnackbarDuration.Short
                                )
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    message = "保存失败: ${e.message}",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Filled.Save, contentDescription = "保存配置")
                    Spacer(Modifier.width(8.dp))
                    Text("保存配置")
                }

                Button(onClick = {
                    val portInt = port.toIntOrNull() ?: 21
                    val valid = validateFtp(server, portInt, user)
                    scope.launch {
                        if (!valid) {
                            snackbarHostState.showSnackbar(
                                message = "配置不合法，无法测试连接",
                                duration = SnackbarDuration.Short
                            )
                            return@launch
                        }
                        val ok = testFtpConnection(server, portInt, user, password)
                        snackbarHostState.showSnackbar(
                            message = if (ok) "连接成功" else "连接失败",
                            duration = SnackbarDuration.Short
                        )
                    }
                }) {
                    Icon(imageVector = Icons.Filled.Link, contentDescription = "测试连接")
                    Spacer(Modifier.width(8.dp))
                    Text("测试连接")
                }
            }
            if (saved) {
                Text("已保存：$server:$port / $user", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoPanel(modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var sys by remember { mutableStateOf<SystemInfo?>(null) }
            var loading by remember { mutableStateOf(true) }
            var media by remember { mutableStateOf<MediaInfo?>(null) }
            var mediaLoading by remember { mutableStateOf(true) }
            var checkingUpdate by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                sys = withContext(Dispatchers.IO) {
                    SystemInfoProvider.get(context)
                }
                loading = false

                media = withContext(Dispatchers.IO) {
                    loadMediaInfo(context)
                }
                mediaLoading = false
            }

            Text("系统信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (loading) {
                Text("加载中...")
            } else {
                val systemInfo = sys ?: SystemInfo("未知", "未知", "未知", "未知")
                Text("CPU：${systemInfo.cpu}")
                Text("RAM：${systemInfo.ram}")
                Text("磁盘：${systemInfo.disk}")
                // 移除“操作系统”显示
            }

            Spacer(Modifier.height(8.dp))
            Text("影音参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (mediaLoading) {
                Text("加载中...")
            } else {
                val mi = media ?: MediaInfo("未知", "未知", "未知")
                Text("摄像头：${mi.cameraSummary}")
                Text("屏幕尺寸：${mi.screenSize}")
                Text("分辨率：${mi.resolution}")
            }

            Spacer(Modifier.height(8.dp))
            Text("APP信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("APP名称：手持盘点助手 Handheld Inventory Assistant (HIA)")
            // Text("版本：1.0.1")
            //get from build.gradle.kts , NOT hardcode here
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            // Text("版本：${packageInfo.versionName} (${packageInfo.versionCode})")

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("版本：${packageInfo.versionName}")
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (checkingUpdate) return@TextButton
                        checkingUpdate = true
                        scope.launch {
                            try {
                                val result = UpdateManager.checkForUpdates(context)
                                val msg = when (result) {
                                    is UpdateResult.UpToDate -> "当前已是最新版本"
                                    is UpdateResult.Updated -> "已下载并触发安装：${result.newVersion}"
                                    is UpdateResult.Error -> "更新失败：${result.reason}"
                                }
                                checkingUpdate = false
                                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                            } catch (e: Exception) {
                                checkingUpdate = false
                                snackbarHostState.showSnackbar("检查更新失败", duration = SnackbarDuration.Short)
                            }
                        }
                    },
                    enabled = !checkingUpdate
                ) {
                    Text(if (checkingUpdate) "检查中..." else "检查更新")
                }
            }

            val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
            Text("博库信息技术(武汉)有限公司©$currentYear")
        }
    }
}

private fun validateFtp(server: String, port: Int?, user: String): Boolean {
    if (server.isBlank()) return false
    val p = port ?: return false
    if (p !in 1..65535) return false
    if (user.isBlank()) return false
    return true
}

private suspend fun testFtpConnection(server: String, port: Int, user: String, password: String): Boolean {
    return withContext(Dispatchers.IO) {
        val client = FTPClient()
        try {
            client.connect(server, port)
            val reply = client.replyCode
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect()
                return@withContext false
            }
            val logged = client.login(user, password)
            try {
                client.logout()
            } catch (_: Exception) { }
            client.disconnect()
            logged
        } catch (_: Exception) {
            try { client.disconnect() } catch (_: Exception) {}
            false
        }
    }
}

private data class MediaInfo(
    val cameraSummary: String,
    val screenSize: String,
    val resolution: String
)

private fun loadMediaInfo(context: Context): MediaInfo {
    // 屏幕分辨率与尺寸
    val metrics = context.resources.displayMetrics
    val widthPx = metrics.widthPixels.coerceAtLeast(1)
    val heightPx = metrics.heightPixels.coerceAtLeast(1)
    val xdpi = metrics.xdpi.takeIf { it > 0 } ?: metrics.densityDpi.toFloat()
    val ydpi = metrics.ydpi.takeIf { it > 0 } ?: metrics.densityDpi.toFloat()
    val widthInches = widthPx / xdpi
    val heightInches = heightPx / ydpi
    val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)
    val screenSizeStr = if (diagonalInches.isFinite() && diagonalInches > 0) {
        String.format("%.1f 英寸", diagonalInches)
    } else {
        "未知"
    }
    val resolutionStr = "${widthPx} x ${heightPx} @ ${metrics.densityDpi}dpi"

    // 摄像头信息
    var cameraSummary = "未知"
    try {
        val cm = context.getSystemService(CameraManager::class.java)
        var backCount = 0
        var frontCount = 0
        var backMax: Size? = null
        var frontMax: Size? = null

        for (id in cm.cameraIdList) {
            val chars = cm.getCameraCharacteristics(id)
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val jpegSizes = try { map?.getOutputSizes(ImageFormat.JPEG) } catch (_: Exception) { null }
            val max = jpegSizes?.maxByOrNull { it.width.toLong() * it.height.toLong() }

            when (facing) {
                CameraCharacteristics.LENS_FACING_BACK -> {
                    backCount++
                    if (max != null && (backMax == null || max.width.toLong() * max.height > backMax!!.width.toLong() * backMax!!.height)) {
                        backMax = max
                    }
                }
                CameraCharacteristics.LENS_FACING_FRONT -> {
                    frontCount++
                    if (max != null && (frontMax == null || max.width.toLong() * max.height > frontMax!!.width.toLong() * frontMax!!.height)) {
                        frontMax = max
                    }
                }
            }
        }
        cameraSummary = buildString {
            append("后置：${backCount} 个")
            if (backMax != null) append("（最大 ${backMax!!.width}x${backMax!!.height}）")
            append("；前置：${frontCount} 个")
            if (frontMax != null) append("（最大 ${frontMax!!.width}x${frontMax!!.height}）")
        }.ifBlank { "未知" }
    } catch (_: Exception) {
        // 忽略错误，保持“未知”
    }

    return MediaInfo(
        cameraSummary = cameraSummary,
        screenSize = screenSizeStr,
        resolution = resolutionStr
    )
}
