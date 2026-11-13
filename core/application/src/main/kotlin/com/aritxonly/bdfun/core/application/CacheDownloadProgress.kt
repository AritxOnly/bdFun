package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.cache.*

/** 视频/音频共用发布与落盘时钟，原阶段上限和未知总长规则保持不变。 */
internal class CacheDownloadProgress(initial: CachedVideo, private val knownTotal: Long, private val now: () -> Long) {
    var current = initial
        private set
    private var videoDone = 0L
    private var audioDone = 0L
    private var lastPublished = 0L
    private var lastPersisted = 0L
    var persist = false
        private set
    fun update(done: Long, audio: Boolean): CachedVideo? {
        if (audio) audioDone = done else videoDone = done
        val time = now()
        if (time - lastPublished < 220L) return null
        lastPublished = time
        val bytes = videoDone + audioDone
        val total = if (knownTotal > 0L) knownTotal else bytes
        current = current.copy(transfer = current.transfer.copy(bytes = bytes, totalBytes = total,
            progress = if (total > 0L) (bytes.toFloat() / total).coerceIn(0f, if (audio) 0.99f else 0.98f) else 0f))
        persist = time - lastPersisted >= 5000L
        if (persist) lastPersisted = time
        return current
    }
}
