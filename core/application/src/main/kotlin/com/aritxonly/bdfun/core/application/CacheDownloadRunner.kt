package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.cache.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class CacheDownloadRunner(private val storage: VideoCacheStorage, private val transfer: VideoCacheTransfer,
    private val now: () -> Long) {
    suspend fun run(request: CacheDownloadRequest, current: () -> Boolean, cleanupAllowed: () -> Boolean,
        publish: suspend (CachedVideo) -> Unit, persist: suspend () -> Unit) {
        checkCurrent(current)
        var entry = request.initial.copy(transfer = request.initial.transfer.copy(status = CacheStatus.Downloading))
        publish(entry)
        val destination = request.destination
        var progress: CacheDownloadProgress? = null
        try {
            val videoLength = transfer.contentLength(request.videoUrl)
            checkCurrent(current)
            val audioLength = transfer.contentLength(request.audioUrl)
            val total = (videoLength.coerceAtLeast(0L) + audioLength.coerceAtLeast(0L)).takeIf { it > 0L } ?: 0L
            val activeProgress = CacheDownloadProgress(entry, total, now)
            progress = activeProgress
            downloadTrack(request.videoUrl, destination.videoPart, false, activeProgress, current, publish, persist)
            checkCurrent(current)
            storage.commit(destination.videoPart, destination.videoFile)
            downloadTrack(request.audioUrl, destination.audioPart, true, activeProgress, current, publish, persist)
            checkCurrent(current)
            storage.commit(destination.audioPart, destination.audioFile)
            entry = activeProgress.current
            val coverPath = transfer.cover(entry.cover, destination.coverFile)
            checkCurrent(current)
            val bytes = storage.size(destination.videoFile) + storage.size(destination.audioFile)
            entry = entry.copy(transfer = CacheTransferState(1f, bytes, bytes, CacheStatus.Completed),
                files = entry.files.copy(videoPath = destination.videoFile, audioPath = destination.audioFile, coverPath = coverPath))
            publish(entry); persist()
        } catch (cancel: CancellationException) {
            if (cleanupAllowed()) storage.removePartial(destination)
            throw cancel
        } catch (failure: Exception) {
            if (cleanupAllowed()) storage.removePartial(destination)
            if (!current()) return
            entry = progress?.current ?: entry
            entry = entry.copy(transfer = entry.transfer.copy(status = CacheStatus.Failed),
                error = failure.message ?: "下载失败")
            publish(entry); persist()
        }
    }
    private suspend fun downloadTrack(url: String, target: String, audio: Boolean, progress: CacheDownloadProgress,
        current: () -> Boolean, publish: suspend (CachedVideo) -> Unit, persist: suspend () -> Unit) {
        checkCurrent(current)
        when (val result = transfer.download(url, target) { done ->
            checkCurrent(current)
            progress.update(done, audio)?.let { publish(it); if (progress.persist) persist() }
        }) {
            is Outcome.Success -> Unit
            is Outcome.Failure -> throw CacheTransferException(result.error)
        }
    }
    private suspend fun checkCurrent(current: () -> Boolean) {
        currentCoroutineContext().ensureActive()
        if (!current()) throw CancellationException("已删除")
    }
}
internal data class CacheDownloadRequest(val initial: CachedVideo, val videoUrl: String,
    val audioUrl: String, val destination: CacheDestination)
private class CacheTransferException(error: AppError) : Exception(when (error) {
    is AppError.Network -> error.message
    is AppError.Unknown -> error.message
    is AppError.Parsing -> error.message
    is AppError.RiskControl -> error.message
    AppError.LoginExpired -> "请先登录"
})
