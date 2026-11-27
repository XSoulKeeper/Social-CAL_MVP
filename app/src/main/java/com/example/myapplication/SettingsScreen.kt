package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Settings: only text size (50 / 100 / 150 %) and vibration. */
@Composable
fun SettingsScreen(vm: AppViewModel) {
    val settings = vm.settings
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.NavyDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BarIcon(Icons.AutoMirrored.Filled.ArrowBack, "Back") { vm.goBack() }
            Text(
                "Settings",
                style = appText(34.sp, FontWeight.Bold),
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("Text size", style = appText(20.sp, FontWeight.Bold))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppSettings.TEXT_SCALES.forEach { scale ->
                    ScaleChip(
                        label = "${(scale * 100).toInt()}%",
                        selected = settings.textScale == scale,
                        modifier = Modifier.weight(1f),
                    ) { settings.updateTextScale(scale) }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Preview: See you at the event!", style = appText(16.sp, color = AppColors.TextDim))

            Spacer(Modifier.height(36.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Vibration", style = appText(20.sp, FontWeight.Bold), modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.vibrationEnabled,
                    onCheckedChange = { enabled ->
                        settings.updateVibration(enabled)
                        Haptics.vibrate(context, enabled, Haptics.Kind.TAP)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AppColors.Green,
                    ),
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Prototype • extractor: ${vm.extractorName} • simulated error: ${AppConfig.SIMULATED_ERROR}",
            style = appText(11.sp, color = AppColors.TextDim),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        DebugOverlay(Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun ScaleChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) AppColors.Green else AppColors.Field)
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = appText(16.sp, FontWeight.Bold))
    }
}
