package com.example.hia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SettingsScreen(navController: NavHostController) {
    Scaffold(topBar = { TopAppBar(title = { Text("系统设置") }) }) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧：FTP 配置
            FtpConfigCard(modifier = Modifier.weight(0.45f))

            // 右侧：系统/APP 信息
            InfoPanel(modifier = Modifier.weight(0.55f))
        }
    }
}

@Composable
private fun FtpConfigCard(modifier: Modifier = Modifier) {
    var server by remember { mutableStateOf("192.168.10.10") }
    var port by remember { mutableStateOf("21") }
    var user by remember { mutableStateOf("ftpuser") }
    var password by remember { mutableStateOf("ftpuser") }
    var saved by remember { mutableStateOf(false) }

    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("FTP配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = server, onValueChange = { server = it }, label = { Text("服务器") })
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("端口") })
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("用户名") })
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation())
            Button(onClick = { saved = true }) { Text("保存配置") }
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
            Text("系统信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("CPU: 8 核 (示例)")
            Text("RAM: 6 GB (示例)")

            Spacer(Modifier.height(8.dp))
            Text("APP信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("APP名称：Handheld Inventory Assistant (HIA) 手持盘点助手")
            Text("版本：1.0")
            Text("版权：boku@2026")
        }
    }
}