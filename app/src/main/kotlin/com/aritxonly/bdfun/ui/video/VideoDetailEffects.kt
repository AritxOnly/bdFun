package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.aritxonly.bdfun.AppGraph
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
                    AppGraph.sentHistory.record(
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
internal fun BindVideoFullscreenState(ui: VideoDetailUiStateHolder) {
    val PlayerUiState = LocalVideoPageState.current.playerUi
    var fullscreen by ui.fullscreen
    var coinSheet by ui.coinSheet
    var favSheet by ui.favSheet
    // 独占全屏状态共享给 Activity；横屏分栏保留 tab / 回复栏。
    LaunchedEffect(fullscreen) { PlayerUiState.fullscreen = fullscreen }
    // 独占全屏时收回投币 / 收藏弹层。
    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            coinSheet = false
            favSheet = false
        }
    }
    // 横屏页面布局和播放器独占全屏分别管理。
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val landscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Rotation opens the two-pane page. Fullscreen is a separate, explicit player action.
    // Rotating back to portrait still leaves fullscreen; entering it while portrait can rotate first.
    LaunchedEffect(landscape) {
        if (!landscape) fullscreen = false
    }
}

@Composable
internal fun BindBackgroundVideoMetadata(video: VideoItem, detail: VideoDetail?, currentCid: Long, activeQuality: Quality?) {
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
        com.aritxonly.bdfun.ui.BackgroundPlayback.updateInfo(
            video = item,
            title = item.title,
            author = item.ownerName,
            cover = item.cover,
        )
        com.aritxonly.bdfun.ui.BackgroundPlayback.updateSession(
            video = item,
            detail = d,
            currentCid = currentCid,
            qualityId = activeQuality?.id ?: 0,
        )
    }

}

@Composable
internal fun ReconcileVideoPager(pager: androidx.compose.foundation.pager.PagerState) {
    // 兜底：极少数情况下 pager 停在上一次拖动的中间位置，
    // 松手后强制吸附到最近的一页，避免停在 0.3 / 0.7 这种位置。
    LaunchedEffect(pager) {
        snapshotFlow {
            pager.isScrollInProgress to pager.currentPageOffsetFraction
        }.collect { (scrolling, offset) ->
            if (!scrolling && kotlin.math.abs(offset) > 0.02f) {
                val target = (pager.currentPage + offset)
                    .roundToInt()
                    .coerceIn(0, pager.pageCount - 1)
                runCatching { pager.animateScrollToPage(target) }
            }
        }
    }

}
