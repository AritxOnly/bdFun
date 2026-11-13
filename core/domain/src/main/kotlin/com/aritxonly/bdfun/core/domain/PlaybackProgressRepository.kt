package com.aritxonly.bdfun.core.domain

interface PlaybackProgressRepository {
    fun resumeFor(bvid: String): Long
    fun save(bvid: String, positionMs: Long, durationMs: Long)
    fun clear(bvid: String)
}

enum class ProgressSaveDecision { Skip, Clear, Write }

/** 兼容旧 CSV 记录，保留严格大于结尾保护区和 500→400 的淘汰边界。 */
fun resumePlaybackProgress(raw: String?): Long {
    val parts = raw?.split(",") ?: return 0L
    val position = parts.getOrNull(0)?.toLongOrNull() ?: return 0L
    val duration = parts.getOrNull(1)?.toLongOrNull() ?: 0L
    return if (playbackProgressDecision(position, duration) == ProgressSaveDecision.Write) position else 0L
}
fun playbackProgressDecision(positionMs: Long, durationMs: Long): ProgressSaveDecision = when {
    positionMs < 3000L -> ProgressSaveDecision.Skip
    durationMs > 0 && positionMs > durationMs - 5000L -> ProgressSaveDecision.Clear
    else -> ProgressSaveDecision.Write
}
fun playbackProgressEvictions(entries: Map<String, String?>): List<String> {
    if (entries.size <= 500) return emptyList()
    return entries.entries.sortedBy { it.value?.split(",")?.getOrNull(2)?.toLongOrNull() ?: 0L }
        .take((entries.size - 400).coerceAtLeast(0)).map { it.key }
}
