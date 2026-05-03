package com.example.mahalleustasi.presentation.screens.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mahalleustasi.core.util.Resource
import com.example.mahalleustasi.domain.model.AiAnalysisResult
import com.example.mahalleustasi.presentation.components.LoadingScreen

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
    onAnalysisSuccess: (AiAnalysisResult) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisState by viewModel.analysisState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    // State handling
    when (analysisState) {
        is Resource.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LoadingScreen(modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Yapay Zeka Arızayı İnceliyor...")
            }
        }
        is Resource.Success -> {
            val result = (analysisState as Resource.Success).data
            LaunchedEffect(result) {
                onAnalysisSuccess(result)
                viewModel.resetState()
            }
        }
        is Resource.Error -> {
            val error = (analysisState as Resource.Error).message
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Hata: $error", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.resetState() }) {
                    Text("Tekrar Dene")
                }
            }
        }
        is Resource.Idle -> {
            if (hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (exc: Exception) {
                                    exc.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay Instructions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Arızalı eşyanın fotoğrafını çekin",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Capture Button
                    IconButton(
                        onClick = {
                            takePhoto(
                                imageCapture = imageCapture,
                                context = context,
                                onPhotoCaptured = { bitmap ->
                                    viewModel.analyzeImage(bitmap)
                                },
                                onError = { /* Hata yönetimi */ }
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Fotoğraf Çek",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Kamera izni gerekiyor.")
                }
            }
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onPhotoCaptured: (Bitmap) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                val bitmap = imageProxyToBitmap(image)
                onPhotoCaptured(bitmap)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                onError(exception)
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    
    val matrix = Matrix()
    matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
