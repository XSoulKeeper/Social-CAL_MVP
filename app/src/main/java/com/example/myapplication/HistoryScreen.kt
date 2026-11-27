package com.example.myapplication

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 06_History – rows come from the local SQLite database – Room database, list updates live via Flow (manual entries: SeedData.kt). */
@Composable
fun HistoryScreen(vm: AppViewModel, onViewInCalendar: (EventEntity) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.NavyDark)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            BarIcon(Icons.Outlined.Home, "Home") { vm.goHome() }
            Text(
                "Event History",
                style = appText(34.sp, FontWeight.Bold),
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.height(12.dp))

        if (vm.history.isEmpty()) {
            Text(
                "No scanned events yet.",
                style = appText(16.sp, color = AppColors.TextDim),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(vm.history, key = { it.id }) { event ->
                HistoryRow(
                    event = event,
                    onView = { onViewInCalendar(event) },
                    onAdd = { vm.addToCalendarFromHistory(event) },
                )
            }
        }

        Text(
            "Local database: ${EventDatabase.DB_NAME} • ${vm.history.size} entries",
            style = appText(11.sp, color = AppColors.TextDim),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        DebugOverlay(Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun HistoryRow(event: EventEntity, onView: () -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AppColors.ListItem)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(event.thumbnailPath)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        ) {
            Text(
                event.title,
                style = appText(13.sp, FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(event.displayDateTime, style = appText(12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (event.location.isNotBlank()) {
                Text(
                    event.location,
                    style = appText(11.sp, color = AppColors.TextDim),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (event.saved) {
            RowAction(label = "View in calendar", onClick = onView) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        } else {
            RowAction(label = "Add to calendar", onClick = onAdd) {
                CalendarAddIcon(tint = Color.White)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp),
        ) {
            Icon(
                if (event.saved) Icons.Outlined.CheckCircle else Icons.Outlined.Block,
                contentDescription = null,
                tint = if (event.saved) AppColors.Green else AppColors.Red,
                modifier = Modifier.size(30.dp),
            )
            Text(
                if (event.saved) "Saved" else "Not saved",
                style = appText(10.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RowAction(label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(88.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        icon()
        Text(label, style = appText(10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Thumbnail(path: String?) {
    val bitmap = remember(path) {
        path?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }?.asImageBitmap()
    }
    val shape = RoundedCornerShape(4.dp)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(shape),
        )
    } else {
        Box(
            Modifier
                .size(48.dp)
                .clip(shape)
                .background(AppColors.Field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Photo, contentDescription = null, tint = AppColors.TextDim)
        }
    }
}
