package com.example.myapplication

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat

/**
 * Root of the app: permissions, calendar launcher, text-size scaling and the
 * screen switch (no navigation library, no transition animations).
 */
@Composable
fun AppRoot(vm: AppViewModel) {
    val context = LocalContext.current
    val camera = remember { CameraController() }

    // --- camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (granted) {
            DebugLog.ok("PERMISSION", "Camera permission granted")
        } else {
            DebugLog.warn("PERMISSION", "Camera permission denied – shutter will use a mock image")
        }
    }
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // --- system calendar (ADR 2)
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.onCalendarResult(result.resultCode)
    }
    val pendingIntent = vm.pendingCalendarIntent
    LaunchedEffect(pendingIntent) {
        if (pendingIntent != null) {
            vm.onCalendarIntentConsumed()
            try {
                calendarLauncher.launch(pendingIntent)
            } catch (e: ActivityNotFoundException) {
                vm.onCalendarLaunchFailed(e)
            }
        }
    }
    val openCalendarAt: (EventEntity) -> Unit = { event ->
        try {
            context.startActivity(CalendarHelper.buildViewIntent(event))
            DebugLog.ok("CALENDAR", "Opened calendar at ${event.displayDateTime}")
        } catch (e: ActivityNotFoundException) {
            vm.onCalendarLaunchFailed(e)
        }
    }

    // --- flash
    LaunchedEffect(vm.flashEnabled) {
        camera.imageCapture.flashMode =
            if (vm.flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    BackHandler(enabled = vm.screen != Screen.CAMERA) { vm.goBack() }

    // --- text size setting (50 / 100 / 150 %) applied to every text in the app
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(baseDensity.density, baseDensity.fontScale * vm.settings.textScale)

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = AppColors.Green,
                background = AppColors.NavyDark,
                surface = AppColors.Navy,
            ),
        ) {
            when (vm.screen) {
                Screen.CAMERA, Screen.PROCESSING, Screen.ERROR -> CameraScreen(
                    vm = vm,
                    controller = camera,
                    hasPermission = hasCameraPermission,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onShutter = {
                        Haptics.vibrate(context, vm.settings.vibrationEnabled, Haptics.Kind.TAP)
                        takePhoto(
                            context = context,
                            controller = camera,
                            onCaptured = vm::onPhotoCaptured,
                            onFailed = vm::onCaptureFailed,
                        )
                    },
                )
                Screen.WRAP_UP -> WrapUpScreen(vm)
                Screen.SUCCESS -> SuccessScreen(vm, onOpenCalendar = { vm.lastSavedEvent?.let(openCalendarAt) })
                Screen.HISTORY -> HistoryScreen(vm, onViewInCalendar = openCalendarAt)
                Screen.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}
