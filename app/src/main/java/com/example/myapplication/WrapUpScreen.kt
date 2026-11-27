package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 03_Feedback_from_LLM – editable summary of what the (mock) LLM extracted. */
@Composable
fun WrapUpScreen(vm: AppViewModel) {
    val form = vm.form
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.NavyDark),
    ) {
        BlurredBackground(vm.capturedBitmap)

        Column(
            Modifier
                .align(Alignment.Center)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
                .background(AppColors.Navy, RoundedCornerShape(12.dp))
                .border(3.dp, AppColors.Green, RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Back arrow on the left, title centered across the whole card width
            Box(Modifier.fillMaxWidth()) {
                BarIcon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Back",
                    modifier = Modifier.align(Alignment.CenterStart),
                ) { vm.goBack() }
                Text(
                    "Wrap up",
                    style = appText(36.sp, FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 48.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            FormRow("Name", form.title) { value -> vm.updateForm { it.copy(title = value) } }
            FormRow("Date", form.date, placeholder = "dd.MM.yyyy") { value -> vm.updateForm { it.copy(date = value) } }
            FormRow("Time", form.time, placeholder = "7:00 PM") { value -> vm.updateForm { it.copy(time = value) } }
            FormRow("Location", form.location) { value -> vm.updateForm { it.copy(location = value) } }

            Spacer(Modifier.height(12.dp))
            Text(
                "Description",
                style = appText(14.sp, FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            FormField(
                value = form.description,
                onChange = { value -> vm.updateForm { it.copy(description = value) } },
                placeholder = "",
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.addToCalendar() },
                enabled = !vm.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Green, contentColor = Color.White),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .widthIn(min = 200.dp)
                    .heightIn(min = 52.dp),
            ) {
                Text("Add to calendar", style = appText(16.sp, FontWeight.Bold))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Data source: ${vm.extractorName} (prototype)",
                style = appText(11.sp, color = AppColors.TextDim),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        DebugOverlay(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun FormRow(
    label: String,
    value: String,
    placeholder: String = "",
    onChange: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = appText(14.sp, FontWeight.Bold), modifier = Modifier.weight(0.35f))
        FormField(
            value = value,
            onChange = onChange,
            placeholder = placeholder,
            singleLine = true,
            modifier = Modifier
                .weight(0.65f)
                .heightIn(min = 40.dp),
        )
    }
}

@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier,
) {
    val align = if (singleLine) TextAlign.Center else TextAlign.Start
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        textStyle = appText(14.sp).copy(textAlign = align),
        cursorBrush = SolidColor(Color.White),
        modifier = modifier
            .background(AppColors.Field, RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        decorationBox = { innerTextField ->
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = if (singleLine) Alignment.Center else Alignment.TopStart,
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = appText(14.sp, color = Color.White.copy(alpha = 0.4f)))
                }
                innerTextField()
            }
        },
    )
}
