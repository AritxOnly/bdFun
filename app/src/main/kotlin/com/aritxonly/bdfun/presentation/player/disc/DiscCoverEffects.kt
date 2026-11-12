package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
internal fun DiscCoverEffects(runtime: DiscRuntime) {
    DiscCoverTintEffect(runtime)
    DiscCoverTransitionEffect(runtime)
}

@Composable
private fun DiscCoverTintEffect(runtime: DiscRuntime) {
    val context = runtime.context
    val coverPx = runtime.coverPx
    val latestCover = runtime.session.cover
    LaunchedEffect(latestCover) {
        if (latestCover.isBlank()) return@LaunchedEffect
        val request = coil.request.ImageRequest.Builder(context)
            .data(latestCover)
            .size(coverPx)
            .allowHardware(false)
            .crossfade(false)
            .build()
        val result = runCatching {
            coil.Coil.imageLoader(context).execute(request)
        }.getOrNull()
        val success = result as? coil.request.SuccessResult ?: return@LaunchedEffect
        averageCoverColor(success.drawable)?.let { runtime.ui.coverTintColor = it }
    }
}

@Composable
private fun DiscCoverTransitionEffect(runtime: DiscRuntime) {
    val context = runtime.context
    val coverPx = runtime.coverPx
    val latestCover = runtime.session.cover
    LaunchedEffect(latestCover) {
        if (latestCover.isBlank() || latestCover == runtime.ui.displayedCover) return@LaunchedEffect
        val request = coil.request.ImageRequest.Builder(context)
            .data(latestCover)
            .size(coverPx)
            .crossfade(false)
            .build()
        val result = runCatching {
            coil.Coil.imageLoader(context).execute(request)
        }.getOrNull()
        val ok = result is coil.request.SuccessResult
        if (!ok) return@LaunchedEffect
        val start = System.nanoTime()
        while (true) {
            val t = ((System.nanoTime() - start) / 1_000_000f / 120f).coerceIn(0f, 1f)
            runtime.ui.coverBlur = t
            if (t >= 1f) break
            delay(8)
        }
        runtime.ui.displayedCover = latestCover
        val startClear = System.nanoTime()
        while (true) {
            val t = ((System.nanoTime() - startClear) / 1_000_000f / 180f).coerceIn(0f, 1f)
            runtime.ui.coverBlur = 1f - t
            if (t >= 1f) break
            delay(8)
        }
        runtime.ui.coverBlur = 0f
    }
}

internal fun averageCoverColor(drawable: android.graphics.drawable.Drawable?): Color? {
    val source = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return null
    if (source.isRecycled) return null
    return runCatching {
        // 硬件位图不能直接 getPixels，取色时先复制成可读位图。
        val readable = if (source.config == android.graphics.Bitmap.Config.HARDWARE) {
            source.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return null
        } else {
            source
        }
        val small = android.graphics.Bitmap.createScaledBitmap(readable, 8, 8, false)
        val pixels = IntArray(64)
        small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        if (small !== readable && small !== source) small.recycle()
        if (readable !== source) readable.recycle()
        var r = 0L
        var g = 0L
        var b = 0L
        pixels.forEach { p ->
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
        }
        val n = pixels.size.coerceAtLeast(1)
        // 呼吸灯需要清晰的色彩，避免封面平均后太灰或太暗。
        // 仅抬高饱和度与亮度下限，保留封面色相。
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (r / n).toInt(),
            (g / n).toInt(),
            (b / n).toInt(),
            hsv,
        )
        hsv[1] = hsv[1].coerceAtLeast(0.55f)
        hsv[2] = hsv[2].coerceAtLeast(0.80f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }.getOrNull()
}
