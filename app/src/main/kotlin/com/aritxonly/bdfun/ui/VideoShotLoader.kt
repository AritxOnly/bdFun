package com.aritxonly.bdfun.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.VideoShot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * 用官方 videoshot 精灵图取「某一秒」的画面：
 * 不用解码视频流，云端已经按固定间隔切好图，裁一块就是预览。
 */
object VideoShotLoader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /** 只留最近 3 张精灵图；LruCache 淘汰后交给 GC，不主动 recycle（可能仍被画面引用）。 */
    private val sheets = LruCache<String, Bitmap>(3)

    suspend fun frame(shot: VideoShot, timeMs: Long): ImageBitmap? = withContext(Dispatchers.IO) {
        if (shot.timesSec.isEmpty() || shot.images.isEmpty()) return@withContext null
        val sec = (timeMs / 1000L).toInt()
        var best = 0
        for (i in shot.timesSec.indices) {
            if (abs(shot.timesSec[i] - sec) < abs(shot.timesSec[best] - sec)) best = i
        }
        val perSheet = (shot.cols * shot.rows).coerceAtLeast(1)
        val sheetIndex = best / perSheet
        val pos = best % perSheet
        val url = shot.images.getOrNull(sheetIndex) ?: return@withContext null

        val sheet = sheets.get(url) ?: runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", BiliApi.UA)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                BitmapFactory.decodeStream(body.byteStream())
            }
        }.getOrNull()?.also { sheets.put(url, it) } ?: return@withContext null

        val frameW = shot.frameWidth.takeIf { it > 0 } ?: (sheet.width / shot.cols)
        val frameH = shot.frameHeight.takeIf { it > 0 } ?: (sheet.height / shot.rows)
        val col = pos % shot.cols
        val row = pos / shot.cols
        val x = col * frameW
        val y = row * frameH
        if (x + frameW > sheet.width || y + frameH > sheet.height) return@withContext null
        runCatching {
            Bitmap.createBitmap(sheet, x, y, frameW, frameH).asImageBitmap()
        }.getOrNull()
    }
}
