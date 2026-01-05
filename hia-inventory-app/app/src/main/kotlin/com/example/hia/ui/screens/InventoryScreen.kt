package com.example.hia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun InventoryScreen(navController: NavHostController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "图书盘点") }) }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧：位置记录
            LocationPanel(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
            )

            // 右侧：摄像预览 + 拍摄按钮
            CameraPanel(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun LocationPanel(modifier: Modifier = Modifier) {
    var floor by remember { mutableStateOf(1) }      // 01-99
    var area by remember { mutableStateOf(1) }       // 01-99
    var shelf by remember { mutableStateOf(1) }      // 01-99
    var face by remember { mutableStateOf(1) }       // 01-99 (正反面号)
    var column by remember { mutableStateOf(1) }     // 01-99 (列号)
    var point by remember { mutableStateOf(1) }      // 1-9  (点位号)

    val codePart = remember(floor, area, shelf, face, column, point) {
        "%02d%02d%02d%02d%02d%d".format(floor, area, shelf, face, column, point)
    }
    val timestamp = remember { System.currentTimeMillis().toString() }
    val filename = remember(codePart, timestamp) { "$codePart-$timestamp.png" }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("拍照地点信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberStepper(label = "楼层", value = floor, onValueChange = { floor = it }, range = 1..99)
                NumberStepper(label = "区域", value = area, onValueChange = { area = it }, range = 1..99)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberStepper(label = "架号", value = shelf, onValueChange = { shelf = it }, range = 1..99)
                NumberStepper(label = "正反面号", value = face, onValueChange = { face = it }, range = 1..99)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberStepper(label = "列号", value = column, onValueChange = { column = it }, range = 1..99)
                NumberStepper(label = "点位号", value = point, onValueChange = { point = it }, range = 1..9)
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp)) {
                    Text("文件命名预览", style = MaterialTheme.typography.labelLarge)
                    Text(filename, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CameraPanel(modifier: Modifier = Modifier) {
    var lastCaptured by remember { mutableStateOf<String?>(null) }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("摄像预览（后置）", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(onClick = {
                // 生成示例文件名（符合规则）
                val now = System.currentTimeMillis()
                lastCaptured = "01020301041-$now.png"
            }) {
                Text("拍摄")
            }

            if (lastCaptured != null) {
                Text("最近拍摄：${lastCaptured}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (value > range.first) onValueChange(value - 1) }) {
                    Icon(painterResource(android.R.drawable.ic_media_previous), contentDescription = "-1")
                }
                Text(text = "%02d".format(value), fontSize = 22.sp, modifier = Modifier.width(56.dp),
                    color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (value < range.last) onValueChange(value + 1) }) {
                    Icon(painterResource(android.R.drawable.ic_media_next), contentDescription = "+1")
                }
            }
        }
    }
}