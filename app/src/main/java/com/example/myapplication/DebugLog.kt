package com.example.myapplication

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Debug messages for every step of the scan process.
 *
 *  - Logcat:   filter with  tag:EventScanner
 *  - On screen: the last lines are shown by DebugOverlay (AppConfig.SHOW_DEBUG_OVERLAY)
 *
 * Line format:  [OK] [STEP] message   /  [WARN] ...  /  [FAIL] ...  /  [INFO] ...
 * A scan without problems ends with:  [OK] [PIPELINE] Scan finished without errors
 */
object DebugLog {

    const val OK = "[OK]"
    const val INFO = "[INFO]"
    const val WARN = "[WARN]"
    const val FAIL = "[FAIL]"

    private const val MAX_LINES = 50

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun ok(step: String, message: String) = write(OK, step, message, null)
    fun info(step: String, message: String) = write(INFO, step, message, null)
    fun warn(step: String, message: String, error: Throwable? = null) = write(WARN, step, message, error)
    fun fail(step: String, message: String, error: Throwable? = null) = write(FAIL, step, message, error)

    fun clear() {
        _lines.value = emptyList()
    }

    private fun write(level: String, step: String, message: String, error: Throwable?) {
        val line = "$level [$step] $message"
        try {
            when (level) {
                FAIL -> Log.e(AppConfig.LOG_TAG, line, error)
                WARN -> Log.w(AppConfig.LOG_TAG, line, error)
                else -> Log.d(AppConfig.LOG_TAG, line)
            }
        } catch (e: RuntimeException) {
            // Plain JVM unit tests: android.util.Log is not available -> print instead.
            println(line + (error?.let { "  ($it)" } ?: ""))
        }
        _lines.update { (it + line).takeLast(MAX_LINES) }
    }
}
