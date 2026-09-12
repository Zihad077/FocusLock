package com.example.presentation.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LRU In-memory cache for high-performance scrolling without UI jank.
 */
private object AppIconCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of the available memory for this memory cache.

    val cache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }
}

/**
 * Renders the authentic Android application launcher icon.
 * Falls back gracefully to stylized Liquid Glass initials if package is not installed.
 */
@Composable
fun RealAppIcon(
    packageName: String,
    appName: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) {
        mutableStateOf(AppIconCache.cache.get(packageName))
    }

    LaunchedEffect(packageName) {
        if (iconBitmap == null) {
            val loaded = loadAppIconBitmap(context, packageName)
            if (loaded != null) {
                AppIconCache.cache.put(packageName, loaded)
                iconBitmap = loaded
            }
        }
    }

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF16243A).copy(alpha = 0.85f),
                        Color(0xFF0F1A2A).copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.35f),
                        Color(0xFF0077D6).copy(alpha = 0.15f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = iconBitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = "$appName icon",
                modifier = Modifier
                    .size(size - 8.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            // Elegant fallback initials
            Text(
                text = appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = (size.value * 0.42f).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            )
        }
    }
}

private suspend fun loadAppIconBitmap(context: Context, packageName: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val drawable: Drawable = pm.getApplicationIcon(packageName)
            drawableToBitmap(drawable)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }

    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96

    return try {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}
