package com.example.hia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.hia.FtpConfig
import com.example.hia.FtpPreferences
import androidx.compose.ui.platform.LocalContext
import com.example.hia.SystemInfoProvider
import kotlinx.coroutines.Dispatchers
import com.example.hia.ui.components.TopNavBar
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply

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
            FtpConfigCard(modifier = Modifier.weight(0.45f), snackbarHostState = snackbarHostState)

            // 右侧：系统/APP 信息
            InfoPanel(modifier = Modifier.weight(0.55f))
        }
    }
}

@Composable
private fun FtpConfigCard(modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedConfig by remember { FtpPreferences.getConfig(context) }.collectAsState(initial = FtpConfig())

    var server by remember { mutableStateOf(savedConfig.server.ifEmpty { "192.168.10.10" }) }
    var port by remember { mutableStateOf(savedConfig.port.takeIf { it > 0 }?.toString() ?: "21") }
    var user by remember { mutableStateOf(savedConfig.user.ifEmpty { "ftpuser" }) }
    var password by remember { mutableStateOf(savedConfig.password.ifEmpty { "ftpuser" }) }
    var saved by remember { mutableStateOf(false) }

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("FTP配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = server, onValueChange = { server = it.trim() }, label = { Text("服务器") })
            OutlinedTextField(value = port, onValueChange = { port = it.trim() }, label = { Text("端口") })
            OutlinedTextField(value = user, onValueChange = { user = it.trim() }, label = { Text("用户名") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation())

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val portInt = port.toIntOrNull()
                    val valid = validateFtp(server, portInt, user)
                    scope.launch {
                        if (!valid) {
                            snackbarHostState.showSnackbar(
                                message = "配置不合法，请检查服务器/端口/用户名",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            FtpPreferences.saveConfig(context, FtpConfig(server, portInt!!, user, password))
                            saved = true
                            snackbarHostState.showSnackbar(
                                message = "配置已保存",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }) { Text("保存配置") }

                Button(onClick = {
                    val portInt = port.toIntOrNull()
                    val valid = validateFtp(server, portInt, user)
                    scope.launch {
                        if (!valid) {
                            snackbarHostState.showSnackbar(
                                message = "配置不合法，无法测试连接",
                                duration = SnackbarDuration.Short
                            )
                            return@launch
                        }
                        val ok = testFtpConnection(server, portInt!!, user, password)
                        snackbarHostState.showSnackbar(
                            message = if (ok) "连接成功" else "连接失败",
                            duration = SnackbarDuration.Short
                        )
                    }
                }) { Text("测试连接") }
            }
            if (saved) {
                Text("已保存：$server:$port / $user", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoPanel(modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.hia.R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier.height(80.dp)
            )
            val context = LocalContext.current
            val sys = remember { SystemInfoProvider.get(context) }

            Text("系统信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("CPU：${'$'}{sys.cpu}")
            Text("RAM：${'$'}{sys.ram}")
            Text("磁盘：${'$'}{sys.disk}")
            Text("操作系统：${'$'}{sys.os}")

            Spacer(Modifier.height(8.dp))
            Text("APP信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("APP名称：Handheld Inventory Assistant (HIA) 手持盘点助手")
            Text("版本：1.0")
            Text("版权：boku@2026")
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