package com.example.myapplication

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** "Przetwarzanie lokalne": scale down + JPEG compress before sending to the model. */
object ImageCompressor {

    fun compress(
        source: Bitmap,
        maxDimension: Int = AppConfig.IMAGE_MAX_DIMENSION_PX,
        quality: Int = AppConfig.IMAGE_JPEG_QUALITY,
    ): ByteArray {
        val scaled = try {
            scaleDown(source, maxDimension)
        } catch (e: Exception) {
            throw ScanException.Compression("Could not resize the photo.", e)
        }
        val out = ByteArrayOutputStream()
        val success = scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        if (!success || out.size() == 0) {
            throw ScanException.Compression("JPEG compression failed.")
        }
        return out.toByteArray()
    }

    fun scaleDown(source: Bitmap, maxDimension: Int): Bitmap {
        val largest = max(source.width, source.height)
        if (largest <= maxDimension) return source
        val ratio = maxDimension.toFloat() / largest
        val width = (source.width * ratio).roundToInt().coerceAtLeast(1)
        val height = (source.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }
}

/** Placeholder "photo" used when no camera is available (e.g. emulator without camera). */
object MockImage {
    fun create(): Bitmap {
        val bitmap = Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(38, 37, 126))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 72f
            isFakeBoldText = true
        }
        canvas.drawText("MOCK POSTER", 360f, 600f, paint)
        paint.textSize = 36f
        paint.isFakeBoldText = false
        canvas.drawText("(camera not available)", 360f, 680f, paint)
        return bitmap
    }
}

/** Real connectivity check (used once a network-based extractor is plugged in). */
object NetworkChecker {
    fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
