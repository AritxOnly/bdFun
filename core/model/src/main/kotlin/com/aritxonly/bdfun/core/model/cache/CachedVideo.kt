package com.aritxonly.bdfun.core.model.cache

enum class CacheStatus { Queued, Downloading, Completed, Failed }
data class CacheVideoInfo(val bvid: String, val aid: Long, val cid: Long, val title: String,
    val cover: String, val ownerName: String, val ownerMid: Long, val durationSec: Int)
data class CacheVideoFiles(val qualityId: Int, val qualityLabel: String,
    val videoPath: String = "", val audioPath: String = "", val coverPath: String = "")
data class CacheTransferState(val progress: Float = 0f, val bytes: Long = 0L,
    val totalBytes: Long = 0L, val status: CacheStatus = CacheStatus.Queued)
/** 一条记录仍只对应当前视频当前 P；分组不改变持久化的平铺字段。 */
data class CachedVideo(val id: String, val video: CacheVideoInfo, val files: CacheVideoFiles,
    val transfer: CacheTransferState, val createdAt: Long, val error: String? = null) {
    val bvid get() = video.bvid
    val aid get() = video.aid
    val cid get() = video.cid
    val title get() = video.title
    val cover get() = video.cover
    val ownerName get() = video.ownerName
    val ownerMid get() = video.ownerMid
    val durationSec get() = video.durationSec
    val qualityId get() = files.qualityId
    val qualityLabel get() = files.qualityLabel
    val videoPath get() = files.videoPath
    val audioPath get() = files.audioPath
    val coverPath get() = files.coverPath
    val progress get() = transfer.progress
    val bytes get() = transfer.bytes
    val totalBytes get() = transfer.totalBytes
    val status get() = transfer.status
}
data class CacheDestination(val folder: String, val videoPart: String, val audioPart: String,
    val videoFile: String, val audioFile: String, val coverFile: String)
