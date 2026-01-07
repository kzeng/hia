package com.example.hia.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.net.Uri
import java.io.IOException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.hia.ui.components.TopNavBar
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun InventoryScreen(navController: NavHostController) {
    var floor by remember { mutableStateOf(1) }
    var area by remember { mutableStateOf(1) }
    var shelf by remember { mutableStateOf(1) }
    var face by remember { mutableStateOf(1) }
    var column by remember { mutableStateOf(1) }
    var point by remember { mutableStateOf(1) }
    var layer by remember { mutableStateOf(1) }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopNavBar(navController) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LocationPanel(
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight(),
                floor = floor,
                area = area,
                shelf = shelf,
                face = face,
                column = column,
                point = point,
                layer = layer,
                onFloorChange = { floor = it },
                onAreaChange = { area = it },
                onShelfChange = { shelf = it },
                onFaceChange = { face = it },
                onColumnChange = { column = it },
                onPointChange = { point = it },
                onLayerChange = { layer = it }
            )

            CameraPanel(
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight(),
                floor = floor,
                area = area,
                shelf = shelf,
                face = face,
                column = column,
                point = point,
                layer = layer,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
private fun LocationPanel(
    modifier: Modifier = Modifier,
    floor: Int,
    area: Int,
    shelf: Int,
    face: Int,
    column: Int,
    point: Int,
    layer: Int,
    onFloorChange: (Int) -> Unit,
    onAreaChange: (Int) -> Unit,
    onShelfChange: (Int) -> Unit,
    onFaceChange: (Int) -> Unit,
    onColumnChange: (Int) -> Unit,
    onPointChange: (Int) -> Unit,
    onLayerChange: (Int) -> Unit
) {
    val codePart = "%02d%02d%02d%02d%02d%d%02d".format(floor, area, shelf, face, column, point, layer)
    val filenamePreview = remember(codePart) {
        "$codePart-<timestamp>.png"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "拍照地点",
                    modifier = Modifier.size(24.dp)
                )
                Text("拍照地点信息", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                NumberStepper("楼层", floor, onFloorChange, 1..99)
                NumberStepper("区域", area, onAreaChange, 1..99)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                NumberStepper("架号", shelf, onShelfChange, 1..99)
                NumberStepper("正反面号", face, onFaceChange, 1..99)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                NumberStepper("列号", column, onColumnChange, 1..99)
                NumberStepper("点位号", point, onPointChange, 1..9, padTwoDigits = false)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                NumberStepper("架层", layer, onLayerChange, 1..99)
                Spacer(modifier = Modifier.weight(1f))
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(11.dp)) {
                    Text("文件命名预览", style = MaterialTheme.typography.labelLarge)
                    Text(filenamePreview, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CameraPanel(
    modifier: Modifier = Modifier,
    floor: Int,
    area: Int,
    shelf: Int,
    face: Int,
    column: Int,
    point: Int,
    layer: Int,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lastCaptured by remember { mutableStateOf<String?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val coroutineScope = rememberCoroutineScope()

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted = granted }

    var writeGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT < 29)
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> writeGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraGranted) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT < 29 && !writeGranted) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                    },
                    update = { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val selector = CameraSelector.DEFAULT_BACK_CAMERA
                            val capture = ImageCapture.Builder()
                                .setTargetRotation(previewView.display.rotation)
                                .build()
                            imageCapture = capture
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )

                // 标准的圆形拍照按钮
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (!cameraGranted || !writeGranted) return@IconButton
                            val codePart = "%02d%02d%02d%02d%02d%d%02d".format(floor, area, shelf, face, column, point, layer)
                            val ts = (System.currentTimeMillis() / 1000).toString() // 10位秒时间戳
                            val filename = "$codePart-$ts.png"
                            val capture = imageCapture ?: return@IconButton
                            capture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bmp = imageProxyToBitmap(image)
                                        image.close()
                                        if (bmp != null) {
                                            val ok = saveBitmapToDcimDateFolder(context, bmp, filename)
                                            lastCaptured = if (ok) filename else lastCaptured
                                            if (ok) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "照片已保存",
                                                        withDismissAction = true
                                                    )
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "保存失败",
                                                        withDismissAction = true
                                                    )
                                                }
                                            }
                                            if (!ok) Log.e("Inventory", "Failed to save $filename")
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("Inventory", "Capture error", exception)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "拍摄失败",
                                                withDismissAction = true
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.primary,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "拍摄",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            if (lastCaptured != null) {
                Text("最近拍摄：$lastCaptured", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    padTwoDigits: Boolean = true
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (value > range.first) onValueChange(value - 1) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        painterResource(android.R.drawable.ic_media_previous), 
                        contentDescription = "-1",
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = if (padTwoDigits) "%02d".format(value) else value.toString(),
                    fontSize = 21.sp,
                    modifier = Modifier.width(54.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { if (value < range.last) onValueChange(value + 1) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        painterResource(android.R.drawable.ic_media_next), 
                        contentDescription = "+1",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        when (image.format) {
            ImageFormat.JPEG -> {
                val buffer = image.planes[0].buffer
                buffer.rewind()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            ImageFormat.YUV_420_888 -> {
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
                val jpegBytes = out.toByteArray()
                BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            }
            else -> {
                // Fallback: try treating as NV21
                val yBuffer = image.planes[0].buffer
                val uBuffer = image.planes[1].buffer
                val vBuffer = image.planes[2].buffer

                val ySize = yBuffer.remaining()
                val uSize = uBuffer.remaining()
                val vSize = vBuffer.remaining()

                val nv21 = ByteArray(ySize + uSize + vSize)
                yBuffer.get(nv21, 0, ySize)
                vBuffer.get(nv21, ySize, vSize)
                uBuffer.get(nv21, ySize + vSize, uSize)

                val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
                val out = ByteArrayOutputStream()
                yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
                val jpegBytes = out.toByteArray()
                BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun saveBitmapToDcimDateFolder(context: Context, bitmap: Bitmap, filename: String): Boolean {
    val dateFolder = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val resolver = context.contentResolver

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val relativePath = Environment.DIRECTORY_DCIM + "/" + dateFolder + "/"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val tryInsertAndWrite: (Uri) -> Boolean = { baseUri: Uri ->
            try {
                resolver.insert(baseUri, values)?.let { uri ->
                    resolver.openOutputStream(uri)?.use { os: OutputStream ->
                        val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                        if (!ok) throw IOException("PNG compress failed")
                    } ?: throw IOException("openOutputStream returned null")
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                        .also { resolver.update(uri, it, null, null) }
                    Log.i("Inventory", "Saved $filename to $uri (relativePath=$relativePath)")
                    true
                } ?: false
            } catch (e: Exception) {
                Log.e("Inventory", "Save PNG failed via $baseUri", e)
                false
            }
        }
        val primaryUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        if (tryInsertAndWrite(primaryUri)) return true
        val externalUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return tryInsertAndWrite(externalUri)
    } else {
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val targetDir = File(dcimDir, dateFolder)
        if (!targetDir.exists()) targetDir.mkdirs()
        val outFile = File(targetDir, filename)
        FileOutputStream(outFile).use { fos ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) return false
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(outFile.absolutePath),
            arrayOf("image/png"),
            null
        )
        return true
    }
}
