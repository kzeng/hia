package com.example.hia.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.hia.ui.components.TopNavBar
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors
import com.example.hia.FtpPreferences
import com.example.hia.FtpConfig
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(navController: NavHostController) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopNavBar(navController) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { pad ->
        PhotoLibraryScreen(modifier = Modifier.fillMaxSize().padding(pad), snackbarHostState = snackbarHostState)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoLibraryScreen(modifier: Modifier = Modifier, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val readImagesPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, readImagesPermission) == PackageManager.PERMISSION_GRANTED) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(readImagesPermission)
    }

    var folders by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var images by remember(selectedFolder) { mutableStateOf<List<PhotoItem>>(emptyList()) }
            var page by remember { mutableStateOf(0) }
            val pageSize = 12  // 3x4 = 12

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            folders = withContext(Dispatchers.IO) { loadDcimDateFolders(context) }
            selectedFolder = folders.firstOrNull()
        }
    }

    // Launcher for MediaStore delete consent (Android 10+)
    val deleteConsentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        scope.launch {
            val ok = result.resultCode == Activity.RESULT_OK
            snackbarHostState.showSnackbar(if (ok) "删除完成" else "已取消或失败", duration = SnackbarDuration.Short)
            folders = withContext(Dispatchers.IO) { loadDcimDateFolders(context) }
            if (selectedFolder !in folders) selectedFolder = folders.firstOrNull()
        }
    }

    LaunchedEffect(selectedFolder) {
        page = 0
        selectedFolder?.let { folder ->
            images = withContext(Dispatchers.IO) { loadImagesForFolder(context, folder) }
        }
    }

    val pageCount = if (images.isEmpty()) 1 else (images.size + pageSize - 1) / pageSize
    val pageItems = images.drop(page * pageSize).take(pageSize)

    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableStateOf(0) }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var uploadStatus by remember { mutableStateOf("") }

    Row(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 左侧文件夹列表
        FoldersPanel(
            modifier = Modifier.weight(0.22f).fillMaxHeight(),
            folders = folders,
            selected = selectedFolder,
            onSelect = { selectedFolder = it },
            onDelete = { name ->
                scope.launch {
                    val ok = withContext(Dispatchers.IO) { deleteFolder(context, name) }
                    if (!ok && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Try consent-based delete for media not owned by this app
                        val uris = withContext(Dispatchers.IO) { collectMediaUrisInFolder(context, name) }
                        if (uris.isNotEmpty()) {
                            val intentSender = MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
                            deleteConsentLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            return@launch
                        }
                    }
                    val message = if (ok) "删除成功" else "删除失败: 请检查应用是否有文件删除权限，或文件夹是否为空"
                    snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                    folders = withContext(Dispatchers.IO) { loadDcimDateFolders(context) }
                    if (selectedFolder !in folders) selectedFolder = folders.firstOrNull()
                }
            },
            onUpload = { name ->
                if (!hasPermission) {
                    scope.launch { snackbarHostState.showSnackbar("缺少读取权限，无法上传", duration = SnackbarDuration.Short) }
                    return@FoldersPanel
                }
                scope.launch {
                    isUploading = true; uploadProgress = 0f; uploadStatus = "准备中…"
                    // 1. 清理 DCIM/pic
                    val cleared = withContext(Dispatchers.IO) { clearPicDir(context) }
                    if (!cleared) {
                        isUploading = false
                        snackbarHostState.showSnackbar("无法清理pic目录", duration = SnackbarDuration.Short)
                        return@launch
                    }
                    // 2. 重命名并拷贝
                    val source = withContext(Dispatchers.IO) { loadImagesForFolder(context, name) }
                    val total = source.size.coerceAtLeast(1)
                    var done = 0
                    val copyOk = withContext(Dispatchers.IO) {
                        source.all { item ->
                            val res = copyAndRenameToPic(context, item)
                            done++
                            uploadProgress = done.toFloat() / total
                            uploadStatus = "拷贝 ${done}/${total}"
                            res
                        }
                    }
                    if (!copyOk) {
                        isUploading = false
                        snackbarHostState.showSnackbar("拷贝失败", duration = SnackbarDuration.Short)
                        return@launch
                    }
                    // 3. 上传到 FTP (增量)
                    uploadStatus = "连接FTP…"
                    val cfg = FtpPreferences.getConfig(context).first()
                    if (cfg.server.isBlank()) {
                        isUploading = false
                        snackbarHostState.showSnackbar("FTP配置为空", duration = SnackbarDuration.Short)
                        return@launch
                    }
                    val ok = withContext(Dispatchers.IO) {
                        uploadPicDirectoryToFtp(context, cfg) { cur, tot, msg ->
                            uploadProgress = if (tot > 0) cur.toFloat() / tot else 0f
                            uploadStatus = msg
                        }
                    }
                    isUploading = false
                    snackbarHostState.showSnackbar(if (ok) "上传完成" else "上传失败", duration = SnackbarDuration.Short)
                }
            }
        )

        // 右侧图片网格 + 分页
        Column(modifier = Modifier.weight(0.78f)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp).fillMaxHeight()) {
                    if (!hasPermission) {
                        Text("需要存储读取权限才能浏览图片")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            itemsIndexed(pageItems) { idx, item ->
                                ImageTile(
                                    item = item,
                                    onClick = { viewerIndex = page * pageSize + idx; viewerOpen = true }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        // 分页导航栏
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${selectedFolder ?: ""} - 共 ${images.size} 张${if (pageCount > 1) " / 第 ${page + 1} / ${pageCount}" else ""}")
                            if (pageCount > 1) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(enabled = page > 0, onClick = { page-- }) { Text("上一页") }
                                    OutlinedButton(enabled = page < pageCount - 1, onClick = { page++ }) { Text("下一页") }
                                }
                            } else {
                                // 只有一页，不显示分页按钮，但保留空间
                                Spacer(Modifier.width(1.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (viewerOpen) {
        FullscreenViewer(
            items = images,
            startIndex = viewerIndex,
            onDismiss = { viewerOpen = false },
        )
    }

    if (isUploading) {
        Dialog(onDismissRequest = { /* uploading; keep open */ }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 4.dp) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("正在上传", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(progress = uploadProgress)
                    Text(uploadStatus)
                    Text("请勿关闭应用…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

data class PhotoItem(val uri: Uri, val displayName: String, val dateTaken: Long?, val relativePath: String?)

@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            // 在实际应用中，这里应该请求权限
            // 为了简化，我们假设权限已授予
            cameraPermissionGranted = true
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (cameraPermissionGranted) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                CameraPreviewView()
                
                // 圆形拍摄按钮，悬浮在预览窗口底部
                FloatingActionButton(
                    onClick = {
                        // TODO: 实现拍摄功能
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "拍摄",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        } else {
            Text("需要摄像头权限", color = Color.White)
        }
    }
}

@Composable
fun CameraPreviewView() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
private fun FullscreenViewer(items: List<PhotoItem>, startIndex: Int, onDismiss: () -> Unit) {
    var index by remember { mutableStateOf(startIndex) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                ZoomableImage(photo = items.getOrNull(index))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { if (index > 0) index-- }) { Text("〈", color = Color.White, fontSize = 28.sp) }
                    IconButton(onClick = { if (index < items.lastIndex) index++ }) { Text("〉", color = Color.White, fontSize = 28.sp) }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) { Text("关闭", color = Color.White) }

                val meta = items.getOrNull(index)
                if (meta != null) {
                    val info = parseInfoFromFilename(meta.displayName, meta.dateTaken)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color(0x66000000))
                            .padding(12.dp)
                    ) {
                        Text(info.title, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(info.subtitle, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(photo: PhotoItem?) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(photo) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo.uri,
                contentDescription = photo.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        }
    }
}

private data class ParsedInfo(val title: String, val subtitle: String)

private fun parseInfoFromFilename(name: String, dateTaken: Long?): ParsedInfo {
    val base = name.substringBeforeLast('.')
    val parts = base.split('-')
    val code = parts.getOrNull(0) ?: ""
    val tsRaw = parts.getOrNull(1)
    val floor = code.take(2)
    val area = code.drop(2).take(2)
    val shelf = code.drop(4).take(2)
    val face = code.drop(6).take(2)
    val column = code.drop(8).take(2)
    val point = code.drop(10).take(1)
    val ts = tsRaw?.toLongOrNull()
    val tsMs = when {
        ts == null -> dateTaken ?: 0L
        ts >= 1_000_000_000_000L -> ts // millis
        ts >= 1_000_000_000L -> ts * 1000 // seconds
        else -> dateTaken ?: 0L
    }
    val timeStr = if (tsMs > 0) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(tsMs)) else ""
    val title = name
    val subtitle = "时间: ${timeStr}  楼层:${floor} 区域:${area} 架号:${shelf} 正反面:${face} 列:${column} 点位:${point}"
    return ParsedInfo(title, subtitle)
}

private fun loadDcimDateFolders(context: android.content.Context): List<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(MediaStore.MediaColumns.RELATIVE_PATH)
        val set = linkedSetOf<String>()
        resolver.query(uri, projection, null, null, null)?.use { c ->
            val idx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            while (c.moveToNext()) {
                val path = c.getString(idx) ?: continue
                if (path.startsWith("${Environment.DIRECTORY_DCIM}/")) {
                    val rest = path.removePrefix("${Environment.DIRECTORY_DCIM}/")
                    val first = rest.substringBefore('/')
                    if (first.length == 8 && first.all { it.isDigit() }) set.add(first)
                }
            }
        }
        set.toList().sortedDescending()
    } else {
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        dcim.listFiles()?.filter { it.isDirectory && it.name.length == 8 && it.name.all { ch -> ch.isDigit() } }
            ?.map { it.name }?.sortedDescending() ?: emptyList()
    }
}

private fun loadImagesForFolder(context: android.content.Context, folder: String): List<PhotoItem> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_TAKEN
        )
        val sel = "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val args = arrayOf("${Environment.DIRECTORY_DCIM}/$folder/")
        val list = mutableListOf<PhotoItem>()
        resolver.query(uri, projection, sel, args, MediaStore.Images.Media.DATE_TAKEN + " DESC")?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val pathIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val dateIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                val name = c.getString(nameIdx) ?: ("img_" + id)
                val rel = c.getString(pathIdx)
                val date = c.getLong(dateIdx)
                val contentUri = ContentUris.withAppendedId(uri, id)
                list.add(PhotoItem(contentUri, name, date, rel))
            }
        }
        list
    } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder)
        dir.listFiles { f -> f.isFile && (f.name.endsWith(".png", true) || f.name.endsWith(".jpg", true) || f.name.endsWith(".jpeg", true) || f.name.endsWith(".bmp", true)) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { PhotoItem(Uri.fromFile(it), it.name, it.lastModified(), dir.absolutePath) } ?: emptyList()
    }
}

private fun clearPicDir(context: android.content.Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val where = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val args = arrayOf("${Environment.DIRECTORY_DCIM}/pic/%")
        try {
            resolver.delete(uri, where, args)
            true
        } catch (_: Exception) { false }
    } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "pic")
        try {
            if (!dir.exists()) return true
            dir.walkTopDown().sortedByDescending { it.absolutePath.count { ch -> ch == File.separatorChar } }.forEach { f ->
                if (f.isFile) f.delete() else f.delete()
            }
            true
        } catch (_: Exception) { false }
    }
}

private fun copyAndRenameToPic(context: android.content.Context, item: PhotoItem): Boolean {
    val name = item.displayName
    val base = name.substringBeforeLast('.')
    val ext = name.substringAfterLast('.', missingDelimiterValue = "png").lowercase(Locale.getDefault())
    val parts = base.split('-')
    val code = parts.getOrNull(0) ?: return false
    if (code.length < 11) return false
    val code10 = code.take(10)
    val point = code.drop(10).take(1)
    val tsRaw = parts.getOrNull(1) ?: return false
    val ts = tsRaw.toLongOrNull() ?: return false
    val tsSec = if (ts >= 1_000_000_000_000L) ts / 1000 else ts
    val relPath = "${Environment.DIRECTORY_DCIM}/pic/01${code10}01/book/${tsSec}/"
    val destName = "${point}.${ext}"

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val mime = when (ext.lowercase(Locale.getDefault())) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "bmp" -> "image/bmp"
            else -> "image/*"
        }
        try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, destName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
            }
            val destUri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), values) ?: return false
            resolver.openOutputStream(destUri)?.use { os ->
                resolver.openInputStream(item.uri)?.use { ins ->
                    ins.copyTo(os)
                } ?: return false
            } ?: return false
            true
        } catch (_: Exception) { false }
    } else {
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val targetDir = File(dcim, "pic/01${code10}01/book/${tsSec}")
        if (!targetDir.exists()) targetDir.mkdirs()
        val outFile = File(targetDir, destName)
        return try {
            val ins = context.contentResolver.openInputStream(item.uri) ?: return false
            ins.use { input ->
                outFile.outputStream().use { os ->
                    input.copyTo(os)
                }
            }
            true
        } catch (_: Exception) { false }
    }
}

private fun uploadPicDirectoryToFtp(context: android.content.Context, cfg: FtpConfig, progress: (cur: Int, total: Int, msg: String) -> Unit): Boolean {
    val client = FTPClient()
    return try {
        client.connect(cfg.server, cfg.port)
        val reply = client.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) { client.disconnect(); return false }
        if (!client.login(cfg.user, cfg.password)) { client.logout(); client.disconnect(); return false }
        client.enterLocalPassiveMode()
        client.setFileType(FTP.BINARY_FILE_TYPE)

        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val picRoot = File(dcim, "pic")
        
        // 获取所有文件和目录（包括空目录）
        val allEntries = mutableListOf<File>()
        picRoot.walkTopDown().forEach { file ->
            allEntries.add(file)
        }
        
        // 先创建所有目录结构
        val dirs = allEntries.filter { it.isDirectory }.sortedBy { it.absolutePath.length }
        for (dir in dirs) {
            val relPath = dir.relativeTo(picRoot).toString().replace('\\', '/')
            if (relPath.isBlank()) continue
            
            val dirsInPath = relPath.split('/').filter { it.isNotBlank() }
            var cwd = ""
            for (d in dirsInPath) {
                cwd = if (cwd.isBlank()) d else "$cwd/$d"
                if (!client.changeWorkingDirectory(cwd)) {
                    client.makeDirectory(cwd)
                    client.changeWorkingDirectory(cwd)
                }
            }
            // 返回到根目录
            client.changeWorkingDirectory("/")
        }
        
        // 上传所有文件
        val files = allEntries.filter { it.isFile }
        val total = files.size
        var cur = 0
        
        for (file in files) {
            val parent = file.parentFile
            val relPath = if (parent != null && parent != picRoot) {
                parent.relativeTo(picRoot).toString().replace('\\', '/')
            } else ""
            val remoteName = file.name
            
            // 切换到对应目录
            if (relPath.isNotBlank()) {
                val dirsInPath = relPath.split('/').filter { it.isNotBlank() }
                var cwd = ""
                for (d in dirsInPath) {
                    cwd = if (cwd.isBlank()) d else "$cwd/$d"
                    if (!client.changeWorkingDirectory(cwd)) {
                        client.makeDirectory(cwd)
                        client.changeWorkingDirectory(cwd)
                    }
                }
            } else {
                client.changeWorkingDirectory("/")
            }
            
            progress(cur, total, "上传 ${if (relPath.isNotBlank()) "$relPath/" else ""}$remoteName")
            
            // 上传文件（覆盖已存在的文件）
            file.inputStream().use { ins -> 
                client.storeFile(remoteName, ins) 
            }
            
            cur++
            // 返回到根目录，为下一个文件准备
            client.changeWorkingDirectory("/")
        }
        
        client.logout(); client.disconnect()
        true
    } catch (e: Exception) {
        Log.e("FTP", "Upload failed", e)
        try { client.disconnect() } catch (_: Exception) {}
        false
    }
}

private fun deleteFolder(context: android.content.Context, folder: String): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        // Images tables
        val imageUris = listOf(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).distinct()
        // Videos tables (folder可能存在视频)
        val videoUris = listOf(
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).distinct()

        // Use LIKE to include subdirectories and both with/without trailing slash
        val where = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val base = "${Environment.DIRECTORY_DCIM}/$folder"
        val args = arrayOf("$base/%", "$base/")
        
        try {
            var totalDeleted = 0
            var hadFiles = false
            
            for (uri in imageUris) {
                // First check if folder has any files in this URI
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val hasFiles = resolver.query(uri, projection, where, args, null)?.use { cursor ->
                    cursor.moveToFirst() // Returns true if there's at least one file
                } ?: false
                
                if (hasFiles) {
                    hadFiles = true
                    // Try to delete files
                    val deletedCount = resolver.delete(uri, where, args)
                    Log.i("Photos", "Deleted $deletedCount files from folder $folder in URI $uri")
                    if (deletedCount > 0) {
                        totalDeleted += deletedCount
                    }
                }
            }
            // Also delete videos in the same folder
            for (uri in videoUris) {
                val projection = arrayOf(MediaStore.Video.Media._ID)
                val hasFiles = resolver.query(uri, projection, where, args, null)?.use { cursor ->
                    cursor.moveToFirst()
                } ?: false
                if (hasFiles) {
                    hadFiles = true
                    val deletedCount = resolver.delete(uri, where, args)
                    Log.i("Photos", "Deleted $deletedCount videos from folder $folder in URI $uri")
                    if (deletedCount > 0) totalDeleted += deletedCount
                }
            }
            
            if (!hadFiles) {
                Log.i("Photos", "Folder $folder has no files or doesn't exist")
                return true // Folder is empty or doesn't exist
            }
            
            // Check if any files remain
            var stillHasFiles = false
            for (uri in imageUris) {
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val hasFiles = resolver.query(uri, projection, where, args, null)?.use { cursor ->
                    cursor.moveToFirst()
                } ?: false
                if (hasFiles) {
                    stillHasFiles = true
                    Log.w("Photos", "Folder $folder still has files in URI $uri after deletion attempt")
                    break
                }
            }
            if (!stillHasFiles) {
                for (uri in videoUris) {
                    val projection = arrayOf(MediaStore.Video.Media._ID)
                    val hasFiles = resolver.query(uri, projection, where, args, null)?.use { cursor ->
                        cursor.moveToFirst()
                    } ?: false
                    if (hasFiles) {
                        stillHasFiles = true
                        Log.w("Photos", "Folder $folder still has videos in URI $uri after deletion attempt")
                        break
                    }
                }
            }
            
            if (stillHasFiles) {
                Log.w("Photos", "Folder $folder still has files after deletion attempt")
                return false
            }
            
            Log.i("Photos", "Successfully deleted folder $folder, total files deleted: $totalDeleted")
            true
        } catch (e: SecurityException) {
            Log.e("Photos", "Delete requires user consent", e)
            false
        } catch (e: Exception) {
            Log.e("Photos", "Failed to delete folder $folder", e)
            false
        }
    } else {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder)
        try {
            if (!dir.exists()) {
                Log.i("Photos", "Folder $folder doesn't exist")
                return true
            }
            
            val files = dir.listFiles()
            if (files.isNullOrEmpty()) {
                Log.i("Photos", "Folder $folder is empty")
                // Empty directory, just delete it
                return dir.delete()
            }
            
            var allFilesDeleted = true
            files.forEach { file ->
                if (!file.delete()) {
                    Log.w("Photos", "Failed to delete file ${file.name}")
                    allFilesDeleted = false
                }
            }
            
            if (!allFilesDeleted) {
                Log.w("Photos", "Not all files in folder $folder were deleted")
                return false
            }
            
            val dirDeleted = dir.delete()
            if (!dirDeleted) {
                Log.w("Photos", "Failed to delete directory $folder, might not be empty")
            }
            dirDeleted
        } catch (e: Exception) {
            Log.e("Photos", "Failed to delete folder $folder", e)
            false
        }
    }
}

/**
 * 收集 DCIM/<folder> 下所有图片/视频的 Uri，用于在 Q+ 通过 MediaStore.createDeleteRequest 请求用户授权删除。
 */
private fun collectMediaUrisInFolder(context: android.content.Context, folder: String): List<Uri> {
    val resolver = context.contentResolver
    val base = "${Environment.DIRECTORY_DCIM}/$folder"
    val where = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
    val args = arrayOf("$base/%", "$base/")
    val result = mutableListOf<Uri>()

    // Images
    val imageUris = listOf(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    ).distinct()
    val imageProj = arrayOf(MediaStore.Images.Media._ID)
    for (table in imageUris) {
        resolver.query(table, imageProj, where, args, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                result += ContentUris.withAppendedId(table, id)
            }
        }
    }

    // Videos
    val videoUris = listOf(
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).distinct()
    val videoProj = arrayOf(MediaStore.Video.Media._ID)
    for (table in videoUris) {
        resolver.query(table, videoProj, where, args, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            while (c.moveToNext()) {
                val id = c.getLong(idIdx)
                result += ContentUris.withAppendedId(table, id)
            }
        }
    }
    return result
}

@Composable
private fun FoldersPanel(
    modifier: Modifier,
    folders: List<String>,
    selected: String?,
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
                    DropdownMenuItem(text = { Text("删除文件夹") }, onClick = { 
                        menuOpen = false
                        showDeleteConfirm = true
                    })
                    DropdownMenuItem(text = { Text("上传文件夹") }, onClick = { menuOpen = false; onUpload(name) })
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除文件夹 '$name' 吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(name)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ImageTile(item: PhotoItem, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(item.displayName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}
