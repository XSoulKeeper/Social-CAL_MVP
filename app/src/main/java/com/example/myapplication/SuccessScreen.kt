package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 04_Success */
@Composable
fun SuccessScreen(vm: AppViewModel, onOpenCalendar: () -> Unit) {
    val event = vm.lastSavedEvent
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.NavyDark),
    ) {
        BlurredBackground(vm.capturedBitmap)
        Box(
            Modifier
                .fillMaxSize()
                .background(AppColors.Scrim),
        )

        BarIcon(
            Icons.Outlined.Home,
            "Home",
            modifier = Modifier
                .statusBarsPadding()
                .padding(4.dp),
        ) { vm.scanNext() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "Success",
                tint = AppColors.Green,
                modifier = Modifier.size(220.dp),
            )
            Text("Success", style = appText(44.sp, FontWeight.Bold))
            Text("Event added", style = appText(18.sp))
            if (event != null) {
                Text(
                    "See you at: \"${event.title}, ${event.displayDate}\"",
                    style = appText(16.sp),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(40.dp))
            ActionItem(Icons.Outlined.PhotoCamera, "Scan next one") { vm.scanNext() }
            Spacer(Modifier.height(24.dp))
            ActionItem(Icons.Outlined.CalendarToday, "Open calendar", onOpenCalendar)
        }

        DebugOverlay(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}
