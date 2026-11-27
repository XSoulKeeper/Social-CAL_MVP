package com.example.myapplication

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colors picked from UI_v1.pdf. */
object AppColors {
    val NavyDark = Color(0xFF14136B)   // History background
    val Navy = Color(0xFF26257E)       // Wrap up card
    val NavyBar = Color(0xE6292870)    // camera bottom bar
    val Field = Color(0xFF3B3A96)      // input fields
    val ListItem = Color(0xFF2E2D8A)   // history rows
    val Shutter = Color(0x663F3EA8)
    val ShutterBorder = Color(0xFF2B2A9C)
    val Green = Color(0xFF12B37C)
    val Orange = Color(0xFFFF6A00)
    val Red = Color(0xFFE53935)
    val Scrim = Color(0x99000000)
    val TextDim = Color(0xB3FFFFFF)
}

/** Italic text style used across the design. */
fun appText(
    size: TextUnit,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.White,
): TextStyle = TextStyle(color = color, fontSize = size, fontWeight = weight, fontStyle = FontStyle.Italic)

/** White icon button (Google Material icons). */
@Composable
fun BarIcon(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 30.dp,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(size),
        )
    }
}

/** Big icon + label (used on the Success screen). */
@Composable
fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(64.dp))
        Text(label, style = appText(18.sp, FontWeight.Bold))
    }
}

/** "calendar_add_on" look: calendar icon + small plus. */
@Composable
fun CalendarAddIcon(tint: Color, modifier: Modifier = Modifier.size(28.dp)) {
    Box(modifier) {
        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize())
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .fillMaxSize(0.55f)
                .align(Alignment.BottomEnd)
                .background(AppColors.ListItem, CircleShape),
        )
    }
}

/** Captured photo, blurred, as background of Wrap up / Success. */
@Composable
fun BlurredBackground(bitmap: Bitmap?) {
    if (bitmap == null) return
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .blur(16.dp),
    )
}

/** Last pipeline messages on screen (toggle: AppConfig.SHOW_DEBUG_OVERLAY). */
@Composable
fun DebugOverlay(modifier: Modifier = Modifier) {
    if (!AppConfig.SHOW_DEBUG_OVERLAY) return
    val lines by DebugLog.lines.collectAsState()
    if (lines.isEmpty()) return
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(Color(0xB3000000), RoundedCornerShape(6.dp))
            .padding(6.dp),
    ) {
        lines.takeLast(4).forEach { line ->
            Text(
                text = line,
                color = when {
                    line.startsWith(DebugLog.FAIL) -> Color(0xFFFF8A80)
                    line.startsWith(DebugLog.WARN) -> Color(0xFFFFD180)
                    line.startsWith(DebugLog.OK) -> Color(0xFFB9F6CA)
                    else -> Color(0xFFE0E0E0)
                },
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
