package com.example.hia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.hia.R
import com.example.hia.TaskViewModel

@Composable
fun TopNavBar(navController: NavHostController, taskViewModel: TaskViewModel) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route

    val currentTask by taskViewModel.currentTask.collectAsState()
    val isTaskRunning = currentTask != null
    val context = LocalContext.current
    val showEndConfirm = remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 16.dp) // 左边距和左边Card边距对齐
    ) {
        // App Logo - 直径适应顶部栏的高度
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(40.dp) // 顶部栏高度通常为64.dp，Logo直径设为40.dp适应高度
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(Modifier.width(12.dp))
        
        NavButton(
            selected = current == "inventory",
            icon = { Icon(Icons.Filled.LibraryBooks, contentDescription = "图书盘点") },
            label = "图书盘点",
            onClick = { navController.navigate("inventory") }
        )
        NavButton(
            selected = current == "photos",
            icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "照片管理") },
            label = "照片管理",
            onClick = { navController.navigate("photos") }
        )
        NavButton(
            selected = current == "settings",
            icon = { Icon(Icons.Filled.Settings, contentDescription = "系统设置") },
            label = "系统设置",
            onClick = { navController.navigate("settings") }
        )
        NavButton(
            selected = current == "taskLog",
            icon = { Icon(Icons.Filled.Timer, contentDescription = "任务记录") },
            label = "任务记录",
            onClick = { navController.navigate("taskLog") }
        )
        TaskButton(
            isRunning = isTaskRunning,
            onClick = {
                if (!isTaskRunning) {
                    taskViewModel.startNewTask()
                    Toast.makeText(context, "任务已开始", Toast.LENGTH_SHORT).show()
                } else {
                    showEndConfirm.value = true
                }
            }
        )
    }

    if (showEndConfirm.value) {
        AlertDialog(
            onDismissRequest = { showEndConfirm.value = false },
            title = { Text("结束任务") },
            text = { Text("确定结束任务吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showEndConfirm.value = false
                    taskViewModel.endCurrentTask()
                    Toast.makeText(context, "任务已结束", Toast.LENGTH_SHORT).show()
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm.value = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun NavButton(selected: Boolean, icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = colors
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun TaskButton(isRunning: Boolean, onClick: () -> Unit) {
    val container = if (isRunning) Color.Red else MaterialTheme.colorScheme.surfaceVariant
    val content = if (isRunning) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Text(if (isRunning) "任务进行中..." else "开始任务")
    }
}
