package com.example.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.NoticeAttachment
import com.example.ui.components.ColorfulIconBadge
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class ScannedPage(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val bitmap: Bitmap
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerDialog(
    onDismiss: () -> Unit,
    onPdfCreated: (NoticeAttachment) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) permissionDeniedOnce = true
    }

    val pages = remember { mutableStateListOf<ScannedPage>() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var buildingPdf by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            pages.forEach { it.bitmap.recycle() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Document Scanner (${pages.size} pages)") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close scanner")
                        }
                    },
                    actions = {
                        if (pages.isNotEmpty()) {
                            Button(
                                onClick = {
                                    buildingPdf = true
                                    scanError = null
                                    scope.launch {
                                        val result = buildPdfFromPages(context, pages.toList())
                                        buildingPdf = false
                                        result.onSuccess { attachment ->
                                            onPdfCreated(attachment)
                                            onDismiss()
                                        }.onFailure { err ->
                                            scanError = err.localizedMessage ?: "Failed to create PDF."
                                        }
                                    }
                                },
                                enabled = !buildingPdf,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("combine_pdf_button")
                            ) {
                                if (buildingPdf) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save PDF")
                                }
                            }
                        }
                    }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                ColorfulIconBadge(
                                    icon = Icons.Rounded.CameraAlt,
                                    tint = IndigoPrimary,
                                    contentDescription = null,
                                    size = 60.dp,
                                    iconSize = 30.dp
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Camera Access for Document Scanning",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = if (permissionDeniedOnce) {
                                        "Camera permission was denied. Grant Camera access to capture multi-page circulars and combine them into PDFs, or use the file picker instead."
                                    } else {
                                        "Allow Camera access to scan printed circulars, timetables, or forms directly into a multi-page PDF attachment."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(20.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(onClick = onDismiss) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        modifier = Modifier.testTag("grant_camera_permission_button")
                                    ) {
                                        Text("Enable Camera")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCapture = capture
                                    try {
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            capture
                                        )
                                    } catch (e: Exception) {
                                        scanError = "Camera unavailable: ${e.localizedMessage}"
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Frame guide overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(28.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        )

                        FloatingActionButton(
                            onClick = {
                                val capture = imageCapture ?: return@FloatingActionButton
                                if (capturing) return@FloatingActionButton
                                capturing = true
                                scanError = null
                                val outFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            scope.launch {
                                                val bmp = withContext(Dispatchers.IO) {
                                                    decodeSampledBitmap(outFile, 1280)
                                                }
                                                capturing = false
                                                if (bmp != null) {
                                                    pages.add(ScannedPage(file = outFile, bitmap = bmp))
                                                } else {
                                                    scanError = "Failed to decode captured page."
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            capturing = false
                                            scanError = exception.localizedMessage ?: "Capture failed."
                                        }
                                    }
                                )
                            },
                            shape = CircleShape,
                            containerColor = IndigoPrimary,
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                                .size(68.dp)
                                .testTag("capture_scan_page_button")
                        ) {
                            if (capturing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(26.dp))
                            } else {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = "Capture page", modifier = Modifier.size(30.dp))
                            }
                        }
                    }

                    scanError?.let { err ->
                        Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Page queue with rotate, reorder, and delete
                    Surface(
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (pages.isEmpty()) "Tap the shutter to capture page 1" else "Captured Pages · Rotate or Reorder",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (pages.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ready for PDF", style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                                    }
                                }
                            }
                            if (pages.isNotEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.width(120.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Image(
                                                    bitmap = page.bitmap.asImageBitmap(),
                                                    contentDescription = "Scanned page ${index + 1}",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(100.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            val rotated = rotateBitmap(page.bitmap, 90f)
                                                            pages[index] = page.copy(bitmap = rotated)
                                                        },
                                                        modifier = Modifier.size(34.dp).minimumInteractiveComponentSize()
                                                    ) {
                                                        Icon(Icons.AutoMirrored.Rounded.RotateRight, contentDescription = "Rotate page", modifier = Modifier.size(18.dp))
                                                    }
                                                    if (index > 0) {
                                                        IconButton(
                                                            onClick = {
                                                                val item = pages.removeAt(index)
                                                                pages.add(index - 1, item)
                                                            },
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Move left", modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    if (index < pages.lastIndex) {
                                                        IconButton(
                                                            onClick = {
                                                                val item = pages.removeAt(index)
                                                                pages.add(index + 1, item)
                                                            },
                                                            modifier = Modifier.size(34.dp)
                                                        ) {
                                                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Move right", modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val removed = pages.removeAt(index)
                                                            removed.bitmap.recycle()
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete page", tint = AmberWarm, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodeSampledBitmap(file: File, maxDim: Int): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, opts)
    var sample = 1
    while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
        sample *= 2
    }
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
}

private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private suspend fun buildPdfFromPages(
    context: Context,
    pages: List<ScannedPage>
): Result<NoticeAttachment> = withContext(Dispatchers.IO) {
    try {
        val pdfDocument = PdfDocument()
        pages.forEachIndexed { index, page ->
            val pageInfo = PdfDocument.PageInfo.Builder(
                page.bitmap.width.coerceAtLeast(100),
                page.bitmap.height.coerceAtLeast(100),
                index + 1
            ).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            pdfPage.canvas.drawBitmap(page.bitmap, 0f, 0f, null)
            pdfDocument.finishPage(pdfPage)
        }
        val fileName = "NoticeScan_${System.currentTimeMillis()}.pdf"
        val outDir = File(context.filesDir, "scanned_pdfs").apply { mkdirs() }
        val outFile = File(outDir, fileName)
        FileOutputStream(outFile).use { stream ->
            pdfDocument.writeTo(stream)
        }
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        Result.success(
            NoticeAttachment(
                id = UUID.randomUUID().toString(),
                name = fileName,
                mimeType = "application/pdf",
                sizeBytes = outFile.length(),
                storageTarget = "LOCAL_SCANNED_PDF",
                localUri = uri.toString(),
                uploadStatus = "VERIFIED"
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }
}
