package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider

/**
 * Event Scanner – MVP / prototype.
 *
 * Flow: Camera -> Loading (mock LLM) -> Wrap up -> system Calendar -> Success
 *       + Event History (local SQLite) + Settings (text size, vibration).
 *
 * Start here when reading the code:
 *   AppConfig.kt       – switches (simulated errors, debug overlay, timeouts)
 *   MockData.kt        – default data for screen 3 (instead of the LLM)
 *   SeedData.kt        – manual entries for the history database
 *   EventExtractor.kt  – where the real LLM gets connected later
 *   ScanPipeline.kt    – the checked steps + exceptions
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        setContent {
            AppRoot(viewModel)
        }
    }
}
