package com.example.hia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SettingsScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("系统设置") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "系统设置", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(20.dp))
            // TODO: settings items per proto (e.g., server URL, user, etc.)
            Text(text = "服务器地址")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "用户设置")
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "同步选项")
        }
    }
}