package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlin.math.roundToInt

/** Holds the CameraX use case that takes photos. Created once in AppRoot. */
class CameraController {
    val imageCapture: ImageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    /** false -> shutter uses MockImage (no camera / no permission). */
    var isBound by mutableStateOf(false)
}

/**
 * Screens 01_Camera, 02_Processing_by_LLM and 05_Error share this layout:
 * live camera preview, settings + flash on top, history + shutter at the bottom.
 */
@Composable
fun CameraScreen(
    vm: AppViewModel,
    controller: CameraController,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onShutter: () -> Unit,
) {
    val processing = vm.screen == Screen.PROCESSING
    val error = vm.screen == Screen.ERROR

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasPermission) {
            CameraPreview(controller, Modifier.fillMaxSize())
        } else {
            NoPermissionPlaceholder(onRequestPermission, Modifier.align(Alignment.Center))
        }

        // 02 – frozen photo + dim while "LLM" works
        if (processing) {
            vm.capturedBitmap?.let { bitmap ->
                val image = remember(bitmap) { bitmap.asImageBitmap() }
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(AppColors.Scrim),
            )
        }

        // 05 – error overlay (tap anywhere to go back to the camera)
        if (error) {
            ErrorOverlay(message = vm.errorMessage, code = vm.errorCode, onDismiss = vm::dismissError)
        }

        // top bar
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarIcon(Icons.Outlined.Settings, "Settings", enabled = !processing) { vm.openSettings() }
            // TEMPORARY debug button – hide with AppConfig.SHOW_DEBUG_ERROR_BUTTON = false
            if (AppConfig.SHOW_DEBUG_ERROR_BUTTON) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AppColors.Orange.copy(alpha = 0.85f))
                        .clickable(enabled = !processing) { vm.triggerDebugError() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("DEBUG: test error", style = appText(12.sp, FontWeight.Bold))
                }
            }
            BarIcon(
                if (vm.flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                if (vm.flashEnabled) "Flash on" else "Flash off",
                enabled = !processing,
            ) { vm.toggleFlash() }
        }

        if (processing) {
            LoadingIndicator(progress = vm.scanProgress, modifier = Modifier.align(Alignment.Center))
        }

        BottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            enabled = !processing,
            onHistory = vm::openHistory,
            onShutter = onShutter,
        )

        DebugOverlay(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 118.dp),
        )
    }
}

@Composable
private fun BottomBar(
    modifier: Modifier,
    enabled: Boolean,
    onHistory: () -> Unit,
    onShutter: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(AppColors.NavyBar, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .navigationBarsPadding()
            .height(110.dp),
    ) {
        BarIcon(
            Icons.Outlined.History,
            "Event history",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 36.dp),
            enabled = enabled,
            size = 34.dp,
            onClick = onHistory,
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(78.dp)
                .clip(CircleShape)
                .background(AppColors.Shutter)
                .border(3.dp, AppColors.ShutterBorder, CircleShape)
                .clickable(enabled = enabled, onClickLabel = "Take photo", onClick = onShutter),
        )
    }
}

@Composable
private fun LoadingIndicator(progress: Float, modifier: Modifier = Modifier) {
    // Smooth fill between step values (progress itself comes from ScanPipeline steps)
    val shownProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250),
        label = "scanProgress",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = Color.White,
            strokeWidth = 5.dp,
        )
        Spacer(Modifier.height(20.dp))
        // Simple determinate bar 0–100 %
        LinearProgressIndicator(
            progress = { shownProgress },
            modifier = Modifier
                .width(220.dp)
                .height(6.dp),
            color = Color.White,
            trackColor = AppColors.NavyDark,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(10.dp))
        Text("LOADING... ${(shownProgress * 100).roundToInt()}%", style = appText(16.sp, FontWeight.Bold))
    }
}

@Composable
private fun ErrorOverlay(message: String, code: String, onDismiss: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.Scrim)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = "Error",
                tint = AppColors.Orange,
                modifier = Modifier.size(220.dp),
            )
            Text("Error", style = appText(44.sp, FontWeight.Bold))
            Text(message, style = appText(16.sp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("Code: $code", style = appText(12.sp, color = AppColors.TextDim))
            Spacer(Modifier.height(24.dp))
            Text("Tap anywhere to try again", style = appText(14.sp, color = AppColors.TextDim))
        }
    }
}

@Composable
private fun NoPermissionPlaceholder(onRequestPermission: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            "Camera permission is needed to scan posters.",
            style = appText(16.sp),
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRequestPermission) {
            Text("Grant permission", style = appText(16.sp, FontWeight.Bold, AppColors.Green))
        }
        Text(
            "(Until then the shutter uses a mock image.)",
            style = appText(12.sp, color = AppColors.TextDim),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CameraPreview(controller: CameraController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner, controller) {
        var provider: ProcessCameraProvider? = null
        var disposed = false

        if (lifecycleOwner == null) {
            DebugLog.fail("CAMERA", "No LifecycleOwner – camera cannot be started")
        } else {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                if (disposed) return@addListener
                try {
                    val cameraProvider = future.get()
                    provider = cameraProvider
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        controller.imageCapture,
                    )
                    controller.isBound = true
                    DebugLog.ok("CAMERA", "Back camera started (preview + capture)")
                } catch (e: Exception) {
                    controller.isBound = false
                    DebugLog.fail("CAMERA", "Could not start camera: ${e.message}", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        onDispose {
            disposed = true
            provider?.unbindAll()
            controller.isBound = false
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Takes a photo (or returns a mock image when the camera is unavailable). */
fun takePhoto(
    context: Context,
    controller: CameraController,
    onCaptured: (Bitmap) -> Unit,
    onFailed: (Throwable) -> Unit,
) {
    if (!controller.isBound) {
        DebugLog.warn("CAPTURE", "Camera not available – using MOCK image")
        onCaptured(MockImage.create())
        return
    }
    DebugLog.info("CAPTURE", "Taking picture...")
    controller.imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = rotateAndScale(image.toBitmap(), image.imageInfo.rotationDegrees)
                    onCaptured(bitmap)
                } catch (e: Exception) {
                    onFailed(e)
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onFailed(exception)
            }
        },
    )
}

private fun rotateAndScale(source: Bitmap, rotationDegrees: Int): Bitmap {
    val scaled = ImageCompressor.scaleDown(source, 1600)
    if (rotationDegrees == 0) return scaled
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
}
