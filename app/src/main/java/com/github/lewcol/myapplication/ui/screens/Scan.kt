package com.github.lewcol.myapplication.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.github.lewcol.myapplication.R
import com.github.lewcol.myapplication.utils.correctBitmapRotation
import com.github.lewcol.myapplication.utils.getCameraProvider
import com.github.lewcol.myapplication.utils.takePhoto
import com.github.lewcol.myapplication.viewmodel.OCRViewModel
import com.github.lewcol.myapplication.viewmodel.OcrState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun CameraView(
    photoFile: File,
    executor: Executor,
    onImageCaptured: (Uri) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        preview.surfaceProvider = previewView.surfaceProvider
    }

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

        IconButton(
            modifier = Modifier.padding(bottom = 20.dp),
            onClick = {
                takePhoto(
                    imageCapture = imageCapture,
                    photoFile = photoFile,
                    executor = executor,
                    onImageCaptured = onImageCaptured,
                    onError = onError
                )
            },
            content = {
                Icon(
                    painter=painterResource(id=R.drawable.rounded_add_a_photo_24),
                    contentDescription = "Take picture",
                    tint = Color.White,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(1.dp)
                        .border(1.dp, Color.White, CircleShape)
                )
            }
        )
    }
}

@Composable
fun TextRecognitionScreen(bitmap: Bitmap, navController: NavHostController, context: Context, recognizer: TextRecognizer, ocrViewModel: OCRViewModel) {
    val ocrState by ocrViewModel.ocrState.collectAsState()

    LaunchedEffect(bitmap) {
        ocrViewModel.processImage(bitmap, context, recognizer)
    }

    val resultText = when (ocrState) {
        OcrState.Idle -> "Waiting to scan."
        OcrState.Loading -> "Processing..."
        is OcrState.Success -> {
            navController.navigate("receiptdetails/${-1}")
            "Reading..."
        }
        is OcrState.Error -> {
            "OCR Error: ${(ocrState as OcrState.Error).message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Scanned Receipt",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = resultText)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Scan(context: Context, navController: NavHostController, ocrViewModel: OCRViewModel) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val capturedImageUri = remember { mutableStateOf<Uri?>(null) }
    val photoFile = remember { ocrViewModel.createTempFile(context) }

    if (cameraPermissionState.status.isGranted) {
        when (val uri = capturedImageUri.value) {
            is Uri -> {
                var bitmap = try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } catch (e: Exception) {
                    Log.e("Scan", "Error decoding bitmap from URI", e)
                    null
                }

                if (bitmap != null) {
                    bitmap = correctBitmapRotation(context, uri, bitmap)
                    TextRecognitionScreen(
                        bitmap = bitmap,
                        navController = navController,
                        context = context,
                        recognizer = recognizer,
                        ocrViewModel = ocrViewModel
                    )
                } else {
                    Text(text = "Error: Failed to load image.", color = MaterialTheme.colorScheme.error)
                }
            }

            null -> {
                CameraView(
                    photoFile = photoFile,
                    executor = Executors.newSingleThreadExecutor(),
                    onImageCaptured = {
                        capturedImageUri.value = it
                    },
                    onError = { Log.e("scan", "Error:", it) }
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val textToShow = "Please enable the camera permission to use the receipt scanner."
            Text(textToShow, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Enable Camera")
            }
        }
    }
}