package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.VideoDetailOwner

import com.aritxonly.bdfun.core.model.video.VideoDetailInfo

import com.aritxonly.bdfun.core.model.video.VideoDetailCounts

import com.aritxonly.bdfun.core.model.video.VideoItemExtras

import com.aritxonly.bdfun.core.domain.VideoCacheStorage
import com.aritxonly.bdfun.core.model.cache.CachedVideo
import com.aritxonly.bdfun.core.model.video.*

internal class CachedVideoPlayback(private val storage: VideoCacheStorage) {
    fun cover(entry: CachedVideo) = if (entry.coverPath.isNotBlank() && storage.exists(entry.coverPath))
        storage.uri(entry.coverPath) else entry.cover
    fun video(entry: CachedVideo) = VideoItem(
        bvid = entry.bvid,
        aid = entry.aid,
        title = entry.title,
        cover = cover(entry),
        ownerName = entry.ownerName,
        ownerFace = "",
        ownerMid = entry.ownerMid,
        play = 0L,
        danmaku = 0L,
        duration = entry.durationSec,
        pubdate = 0L,
        extras = VideoItemExtras(
            cid = entry.cid,
        ),
    )
    fun detail(entry: CachedVideo) = VideoDetail(
        bvid = entry.bvid,
        aid = entry.aid,
        title = entry.title,
        cover = cover(entry),
        desc = "",
        owner = VideoDetailOwner(
            ownerName = entry.ownerName,
            ownerFace = "",
            ownerMid = entry.ownerMid,
            ownerSign = "",
        ),
        counts = VideoDetailCounts(
            play = 0L,
            danmaku = 0L,
            like = 0L,
            coin = 0L,
            favorite = 0L,
            share = 0L,
            reply = 0L,
        ),
        info = VideoDetailInfo(
            pubdate = 0L,
            duration = entry.durationSec,
            cid = entry.cid,
            tname = "",
            pages = listOf(VideoPage(cid = entry.cid, page = 1, part = entry.title, duration = entry.durationSec)),
            episodes = emptyList(),
        ),
    )
    fun playInfo(entry: CachedVideo): PlayInfo {
        val videoUrl = storage.uri(entry.videoPath)
        val audioUrl = if (entry.audioPath.isNotBlank() && storage.exists(entry.audioPath)) storage.uri(entry.audioPath) else null
        return PlayInfo(qualities = listOf(Quality(id = entry.qualityId, label = entry.qualityLabel,
            width = 0, height = 0, frameRate = "", codec = "", videoUrl = videoUrl)),
            audioUrl = audioUrl, fallbackUrl = null, durationMs = entry.durationSec * 1000L, acceptQuality = emptyList())
    }
}
