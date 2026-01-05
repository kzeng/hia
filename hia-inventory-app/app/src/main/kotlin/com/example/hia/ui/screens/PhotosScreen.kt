package com.example.hia.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(navController: NavHostController) {
    val folders = remember { mutableStateListOf("20260105", "20251231", "20250101") }
    var selectedFolder by remember { mutableStateOf(folders.first()) }

    // 模拟图片文件名
    val images = remember(selectedFolder) {
        List(32) { idx -> "01020301041-${System.currentTimeMillis() - idx * 1000}.png" }
    }
    var page by remember { mutableStateOf(0) }
    val pageSize = 16
    val pageCount = (images.size + pageSize - 1) / pageSize
    val pageItems = images.drop(page * pageSize).take(pageSize)

    Scaffold(topBar = { TopAppBar(title = { Text("照片管理") }) }) { pad ->
        Row(
            modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左栏 25% 宽度：文件夹列表
            FoldersPanel(
                modifier = Modifier.weight(0.25f).fillMaxHeight(),
                folders = folders,
                selected = selectedFolder,
                onSelect = { selectedFolder = it },
                onDelete = { folders.remove(it); if (folders.isNotEmpty()) selectedFolder = folders.first() },
                onUpload = { /* TODO: 上传文件夹 */ }
            )

            // 右栏：图片 GRID + 分页
            Column(modifier = Modifier.weight(0.75f)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                            items(pageItems) { name ->
                                ImageTile(name = name)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("第 ${page + 1} / $pageCount 页", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(enabled = page > 0, onClick = { page-- }) { Text("上一页") }
                                Button(enabled = page < pageCount - 1, onClick = { page++ }) { Text("下一页") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersPanel(
    modifier: Modifier,
    folders: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUpload: (String) -> Unit
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("图片目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(folders.size) { idx ->
                    val name = folders[idx]
                    FolderRow(name = name, selected = name == selected, onClick = { onSelect(name) }, onDelete = onDelete, onUpload = onUpload)
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (String) -> Unit,
    onUpload: (String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { menuOpen = true }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Folder, contentDescription = null)
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Box {
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("删除文件夹") }, onClick = { menuOpen = false; onDelete(name) })
                    DropdownMenuItem(text = { Text("上传文件夹") }, onClick = { menuOpen = false; onUpload(name) })
                }
            }
        }
    }
}

@Composable
private fun ImageTile(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("预览", color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}