package com.example.myapplication

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** User settings: text size (50 / 100 / 150 %) and vibration. Stored in SharedPreferences. */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var textScale by mutableStateOf(prefs.getFloat(KEY_TEXT_SCALE, 1f))
        private set

    var vibrationEnabled by mutableStateOf(prefs.getBoolean(KEY_VIBRATION, true))
        private set

    fun updateTextScale(scale: Float) {
        textScale = scale
        prefs.edit().putFloat(KEY_TEXT_SCALE, scale).apply()
        DebugLog.info("SETTINGS", "Text size set to ${(scale * 100).toInt()}%")
    }

    fun updateVibration(enabled: Boolean) {
        vibrationEnabled = enabled
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        DebugLog.info("SETTINGS", "Vibration ${if (enabled) "ON" else "OFF"}")
    }

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_VIBRATION = "vibration"

        val TEXT_SCALES = listOf(0.5f, 1.0f, 1.5f)
    }
}

/** Short vibrations as feedback (no sounds in the prototype). */
object Haptics {

    enum class Kind { TAP, SUCCESS, ERROR }

    fun vibrate(context: Context, enabled: Boolean, kind: Kind) {
        if (!enabled) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = when (kind) {
                    Kind.TAP -> VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE)
                    Kind.SUCCESS -> VibrationEffect.createOneShot(120L, VibrationEffect.DEFAULT_AMPLITUDE)
                    Kind.ERROR -> VibrationEffect.createWaveform(longArrayOf(0L, 80L, 80L, 80L), -1)
                }
                vibrator.vibrate(effect)
            } else {
                vibrateLegacy(vibrator, kind)
            }
        } catch (e: SecurityException) {
            DebugLog.warn("HAPTICS", "Vibration not allowed: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy(vibrator: Vibrator, kind: Kind) {
        when (kind) {
            Kind.TAP -> vibrator.vibrate(30L)
            Kind.SUCCESS -> vibrator.vibrate(120L)
            Kind.ERROR -> vibrator.vibrate(longArrayOf(0L, 80L, 80L, 80L), -1)
        }
    }

    @Suppress("DEPRECATION")
    private fun getVibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
