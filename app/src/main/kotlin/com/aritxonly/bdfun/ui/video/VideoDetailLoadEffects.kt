package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import kotlinx.coroutines.delay
import com.aritxonly.bdfun.feature.video.application.VideoCacheEntry

@Composable
internal fun LoadInitialVideo(
    video: VideoItem, defaultQuality: Int, controllers: VideoDetailControllers, ui: VideoDetailUiStateHolder,
) {
    val dependencies = LocalVideoPageState.current.dependencies
    val settings = LocalVideoPageState.current.settings
    val readCache = controllers.readCache
    val detailController = controllers.detail
    val selectionController = controllers.selection
    LaunchedEffect(video.bvid) {
        ui.metaReady.value = false
        // 离线缓存优先：直接从本地视频轨 + 音轨起播，不再请求播放地址。
        val offline = dependencies.downloads.findPlayable(video.bvid, video.cid)
        if (offline != null) {
            val offlinePlay = dependencies.downloads.playInfo(offline)
            dependencies.log(
                "VIDEO",
                "source=offline bvid=${video.bvid} qualities=${offlinePlay.qualities.size} " +
                    "audio=${offlinePlay.audioUrl != null}",
            )
            detailController.restore(dependencies.downloads.detail(offline), offlinePlay, emptyList(), emptyList())
            selectionController.restoreCid(offline.cid)
            return@LaunchedEffect
        }
        // 从「我的评论与弹幕」等入口进来时，先定位到记录的时间点
        if (settings.resumePlayback && video.startPositionMs > 0L) {
            selectionController.seek(video.startPositionMs)
        }
        // 从相关推荐点进去再返回：直接用缓存，不再重新请求
        val cached = readCache.video(video.bvid)
        if (cached != null) {
            dependencies.log(
                "VIDEO",
                "source=memory-cache bvid=${video.bvid} " +
                    "qualities=${cached.playInfo?.qualities?.size} " +
                    "audio=${cached.playInfo?.audioUrl != null}",
            )
            detailController.restore(cached.detail, cached.playInfo, cached.danmaku,
                cached.related.ifEmpty { readCache.related(video.bvid).orEmpty() })
            selectionController.restoreCid(cached.detail.cid)
            if (settings.resumePlayback && selectionController.state.value.resumePositionMs <= 0L) {
                selectionController.offerCloudResume(detailController.resumePosition(cached.detail))
            }
            return@LaunchedEffect
        }
        val loadedSuccessfully = detailController.load(
            video.bvid, defaultQuality, settings.codecPreference, settings.cdnService,
            resume = settings.resumePlayback && selectionController.state.value.resumePositionMs <= 0L,
            onDetail = { selectionController.restoreCid(it.cid) },
            onReady = { position ->
                selectionController.offerCloudResume(position)
                val ready = detailController.state.value
                dependencies.log(
                    "VIDEO",
                    "source=network bvid=${video.bvid} hasInfo=${ready.playInfo != null} " +
                        "qualities=${ready.playInfo?.qualities?.size} audio=${ready.playInfo?.audioUrl != null} " +
                        "err=${ready.playError}",
                )
            },
        )
        if (!loadedSuccessfully) return@LaunchedEffect
        val loaded = detailController.state.value
        loaded.detail?.let { d ->
            if (loaded.related.isNotEmpty()) readCache.putRelated(video.bvid, loaded.related)
            readCache.putVideo(video.bvid, VideoCacheEntry(d, loaded.playInfo, loaded.danmaku, loaded.related))
        }
    }

}

@Composable
internal fun BindVideoAuthor(
    detail: VideoDetail?, loggedIn: Boolean,
    metadataController: com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder,
    interactionController: com.aritxonly.bdfun.feature.video.application.VideoInteractionStateHolder,
    ui: VideoDetailUiStateHolder, description: VideoDescriptionStateHolder,
) {
    LaunchedEffect(detail?.bvid) {
        val d = detail ?: return@LaunchedEffect
        interactionController.bind(d)
        description.reset()
    }

    // UP 粉丝数 / 投稿数 / 当前关注状态
    LaunchedEffect(detail?.ownerMid) {
        val mid = detail?.ownerMid ?: 0L
        if (mid <= 0) return@LaunchedEffect
        val relationToken = interactionController.relationToken()
        metadataController.loadAuthor(mid, loggedIn, detail?.isFollowed == true) {
            interactionController.restoreFollowed(it, relationToken)
        }
        ui.metaReady.value = true
    }

    // 粉丝接口偶发很慢时，最多等一小会儿就让下方信息一起出现，避免页面长时间空着。
    LaunchedEffect(detail?.bvid) {
        if (detail == null) {
            ui.metaReady.value = false
            return@LaunchedEffect
        }
        delay(175)
        ui.metaReady.value = true
    }

}

@Composable
internal fun BindVideoPageMetadata(
    detail: VideoDetail?, currentCid: Long, loggedIn: Boolean,
    metadataController: com.aritxonly.bdfun.feature.video.application.VideoMetadataStateHolder,
    interactionController: com.aritxonly.bdfun.feature.video.application.VideoInteractionStateHolder,
    ui: VideoDetailUiStateHolder,
) {
    // 当前同时观看人数
    LaunchedEffect(detail?.aid, currentCid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || currentCid <= 0) return@LaunchedEffect
        metadataController.loadOnline(d.aid, currentCid)
    }

    // 官方字幕轨：切分 P 后重新拉，默认关闭。
    LaunchedEffect(detail?.aid, currentCid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid > 0 && currentCid > 0) metadataController.loadSubtitles(d.aid, currentCid)
    }

    // 详情一到位就预取收藏夹，点「收藏」时直接出内容，不再转圈
    LaunchedEffect(detail?.aid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || !loggedIn) return@LaunchedEffect
        interactionController.bind(d)
        interactionController.loadFavorites()
    }

    // 预取失败时打开面板补齐，保持现有面板与提示方式。
    LaunchedEffect(ui.favSheet.value) {
        if (ui.favSheet.value && !interactionController.state.value.favoritesLoaded) interactionController.loadFavorites()
    }

}
