package com.example.hia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.hia.TaskRecord
import com.example.hia.TaskViewModel
import com.example.hia.ui.components.TopNavBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun TaskLogScreen(navController: NavHostController, taskViewModel: TaskViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tasks by taskViewModel.taskList.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopNavBar(navController, taskViewModel) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "任务记录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (tasks.isEmpty()) {
                Text("暂无任务记录")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tasks) { task ->
                        TaskRow(
                            record = task,
                            onViewPhotosClick = {
                                navController.navigate("photos")
                            },
                            onDeleteClick = {
                                confirmDelete(taskViewModel, task.id, snackbarHostState, scope)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    record: TaskRecord,
    onViewPhotosClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("任务ID：${record.id}", fontWeight = FontWeight.Medium)
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("开始时间：" + formatTime(record.startTime))
            Text("结束时间：" + (record.endTime?.let { formatTime(it) } ?: "进行中"))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("图片数量：${record.photoCount}")
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onViewPhotosClick) {
                    Text("查看图片")
                }
            }
        }
    }
}

private fun confirmDelete(
    taskViewModel: TaskViewModel,
    taskId: Long,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {
    // 简单实现：直接删除并弹出提示
    taskViewModel.deleteTask(taskId)
    scope.launch {
        snackbarHostState.showSnackbar(
            message = "任务已删除",
            duration = SnackbarDuration.Short
        )
    }
}

private fun formatTime(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(millis))
}
