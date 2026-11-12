package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.aritxonly.bdfun.data.DanmakuItem
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import kotlin.math.roundToInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
internal fun RegisterVideoDanmakuSender(
    detail: VideoDetail?, currentCid: Long,
    metadataController: com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder,
    detailController: com.aritxonly.bdfun.feature.video.application.VideoDetailStateHolder,
) {
    val VideoPageBus = LocalVideoPageState.current
    // 把本页的 aid/cid 与「发弹幕」发送器交给 Activity 覆盖层（那边在采集层之外，拿不到 api）
    DisposableEffect(currentCid, detail?.aid) {
        val d = detail
        if (d != null) {
            VideoPageBus.aid = d.aid
            VideoPageBus.cid = currentCid
            VideoPageBus.danmakuSender = { text, mode, fontSize, color ->
                val ok = metadataController.sendDanmaku(
                    com.aritxonly.bdfun.core.domain.DanmakuPost(
                        currentCid, d.bvid, text, VideoPageBus.danmakuProgressMs, mode, fontSize, color,
                    ),
                )
                if (ok) {
                    VideoPageBus.dependencies.sentHistory.record(
                        kind = "danmaku",
                        text = text,
                        bvid = d.bvid,
                        title = d.title,
                        videoTimeMs = VideoPageBus.danmakuProgressMs,
                    )
                    // 自己刚发的弹幕立即出现在弹幕层里（带圆角底）
                    detailController.appendDanmaku(com.aritxonly.bdfun.data.DanmakuItem(
                        timeMs = VideoPageBus.danmakuProgressMs,
                        mode = mode,
                        color = color,
                        size = fontSize / 25f,
                        text = text,
                        self = true,
                    ))
                }
                ok
            }
        }
        onDispose { VideoPageBus.danmakuSender = null }
    }

}

@Composable
internal fun BindBackgroundVideoMetadata(video: VideoItem, detail: VideoDetail?, currentCid: Long, activeQuality: Quality?) {
    val background = LocalVideoPageState.current.dependencies.background
    // 底栏唱片用：当前播放的视频、标题、封面、集数。切 P / 切合集时一起更新。
    LaunchedEffect(
        video.bvid,
        detail?.bvid,
        detail?.title,
        detail?.cover,
        currentCid,
        activeQuality?.id,
    ) {
        val d = detail
        val item = video.copy(
            bvid = d?.bvid ?: video.bvid,
            aid = d?.aid ?: video.aid,
            title = d?.title ?: video.title,
            cover = d?.cover ?: video.cover,
            ownerName = d?.ownerName ?: video.ownerName,
            ownerFace = d?.ownerFace ?: video.ownerFace,
            ownerMid = d?.ownerMid ?: video.ownerMid,
            play = d?.play ?: video.play,
            danmaku = d?.danmaku ?: video.danmaku,
            duration = d?.duration ?: video.duration,
            pubdate = d?.pubdate ?: video.pubdate,
            extras = video.extras.copy(
                cid = currentCid,
            ),
        )
        background.updateInfo(
            video = item,
            title = item.title,
            author = item.ownerName,
            cover = item.cover,
        )
        background.updateSession(
            video = item,
            detail = d,
            currentCid = currentCid,
            qualityId = activeQuality?.id ?: 0,
        )
    }

}
