package com.aritxonly.bdfun.ui

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 进度条拖动时的画面预览：用 MediaMetadataRetriever 直接在视频流上取关键帧。
 * 同一个 URL 复用同一个 retriever，并串行化调用（它不是线程安全的）。
 */
class VideoFrameLoader {
    private val mutex = Mutex()
    private var retriever: MediaMetadataRetriever? = null
    private var currentUrl: String? = null

    suspend fun frame(
        url: String?,
        headers: Map<String, String>,
        timeMs: Long,
        width: Int = 320,
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        if (url.isNullOrBlank()) return@withContext null
        mutex.withLock {
            runCatching {
                if (currentUrl != url || retriever == null) {
                    runCatching { retriever?.release() }
                    retriever = MediaMetadataRetriever().apply {
                        setDataSource(url, headers)
                    }
                    currentUrl = url
                }
                val us = (timeMs.coerceAtLeast(0L)) * 1000L
                var bitmap = retriever?.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap == null) {
                    bitmap = retriever?.getFrameAtTime(us)
                }
                if (bitmap == null) {
                    bitmap = retriever?.getFrameAtTime(us, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                if (bitmap == null) return@withLock null
                val height = (width.toFloat() * bitmap.height / bitmap.width)
                    .toInt()
                    .coerceIn(1, width * 2)
                val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
                if (scaled !== bitmap) bitmap.recycle()
                scaled.asImageBitmap()
            }.getOrNull()
        }
    }

    fun release() {
        runCatching { retriever?.release() }
        retriever = null
        currentUrl = null
    }
}
