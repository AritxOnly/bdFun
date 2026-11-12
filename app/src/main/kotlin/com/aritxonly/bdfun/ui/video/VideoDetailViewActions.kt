package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.VideoStreamRequest
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.data.VideoPage
import kotlinx.coroutines.launch

internal data class VideoDetailControllers(
    val detail: com.aritxonly.bdfun.feature.video.application.VideoDetailStateHolder,
    val interaction: com.aritxonly.bdfun.feature.video.application.VideoInteractionStateHolder,
    val selection: com.aritxonly.bdfun.feature.video.application.VideoPlaybackSelectionStateHolder,
    val readCache: com.aritxonly.bdfun.feature.video.application.VideoReadCacheScope,
)

/** 页面动作只接线提示、系统入口和已有应用层控制器，不持有业务状态副本。 */
internal class VideoDetailViewActions(
    private val context: android.content.Context, private val scope: kotlinx.coroutines.CoroutineScope,
    private val video: VideoItem, controllers: VideoDetailControllers, private val ui: VideoDetailUiStateHolder,
    private val VideoPageBus: VideoPagePresentationState, private val loggedIn: () -> Boolean,
    private val openedFromVideoPage: Boolean, private val onBack: () -> Unit,
) {
    private val settings get() = VideoPageBus.settings
    private val readCache = controllers.readCache
    private val detailController = controllers.detail
    private val interactionController = controllers.interaction
    private val selectionController = controllers.selection
    private val videoContext get() = context
    private val detail get() = detailController.state.value.detail
    private val playInfo get() = detailController.state.value.playInfo
    private val currentCid get() = selectionController.state.value.cid
    private val qualityId get() = selectionController.state.value.quality
    private val activeBvid get() = selectionController.state.value.bvid
    private val favorited get() = interactionController.state.value.favorited
    private var coinSheet: Boolean
        get() = ui.coinSheet.value
        set(value) { ui.coinSheet.value = value }
    private var favSheet: Boolean
        get() = ui.favSheet.value
        set(value) { ui.favSheet.value = value }
    fun ensureLogin(): Boolean {
        if (!loggedIn()) {
            context.showToast("请先登录")
            return false
        }
        return true
    }

    /** 视图层只负责登录提示、面板开关与操作结果提示。 */
    fun ensureFavoritesLoaded() {
        if (!ensureLogin()) return
        scope.launch { interactionController.loadFavorites() }
    }

    fun performInteraction(action: suspend () -> String?) {
        if (!ensureLogin()) return
        scope.launch { action()?.let { context.showToast(it) } }
    }

    fun addWatchLater() = performInteraction { interactionController.watchLater() }
    fun toggleFollow() {
        val mid = detail?.ownerMid ?: video.ownerMid
        if (mid > 0) performInteraction { interactionController.toggleFollow(mid) }
    }
    fun toggleLike() = performInteraction { interactionController.toggleLike() }
    fun toggleDislike() = performInteraction { interactionController.toggleDislike() }
    fun giveCoin() {
        if (detail != null && ensureLogin()) coinSheet = true
    }
    fun doCoin(count: Int) {
        coinSheet = false
        performInteraction { interactionController.coin(count) }
    }
    fun toggleFavorite() {
        if (detail == null || !ensureLogin()) return
        if (!favorited) favSheet = true
        else performInteraction { interactionController.removeFavorites() }
    }
    fun tripleAction() = performInteraction { interactionController.tripleAction() }
    fun toggleFolder(folderId: Long) = performInteraction { interactionController.toggleFolder(folderId) }

    fun switchQuality(id: Int) {
        val d = detail ?: return
        if (!selectionController.selectQuality(id, VideoPageBus.danmakuProgressMs)) return
        if (id == 0) return
        scope.launch {
            detailController.switchStream(
                VideoStreamRequest(d, id, currentCid, d.duration,
                    settings.codecPreference, settings.cdnService),
                reloadDanmaku = false,
            )
        }
    }

    /** 切到多 P 视频的某一集：换 cid、重置进度、重新拉播放流。 */
    fun switchPage(page: com.aritxonly.bdfun.data.VideoPage) {
        val d = detail ?: return
        if (page.cid == currentCid) return
        // 合集里的跨 bvid 单集：在当前播放器内切换，不跳新 Activity
        val episode = d.episodes.firstOrNull { it.cid == page.cid }
        if (episode != null && episode.bvid.isNotBlank() && episode.bvid != d.bvid) {
            scope.launch {
                val switched = detailController.switchEpisode(
                    episode.bvid,
                    VideoStreamRequest(d, qualityId, page.cid, page.duration,
                        settings.codecPreference, settings.cdnService),
                ) { loaded ->
                    selectionController.selectPage(page.cid, loaded.bvid)
                    VideoPageBus.danmakuProgressMs = 0L
                }
                if (switched) {
                    val state = detailController.state.value
                    if (state.related.isNotEmpty()) readCache.putRelated(episode.bvid, state.related)
                }
            }
            return
        }
        selectionController.selectPage(page.cid)
        VideoPageBus.danmakuProgressMs = 0L
        scope.launch {
            detailController.switchStream(
                VideoStreamRequest(d, qualityId, page.cid, page.duration,
                    settings.codecPreference, settings.cdnService),
                reloadDanmaku = true,
            )
        }
    }

    /** 缓存当前视频当前 P 的指定清晰度（视频轨 + 音轨）。 */
    fun cacheVideo(quality: Quality) {
        val d = detail ?: return
        if (VideoPageBus.dependencies.downloads.findPlayable(activeBvid, currentCid) != null) {
            videoContext.showToast("已缓存")
            return
        }
        val audio = playInfo?.audioUrl
        if (audio.isNullOrBlank()) {
            videoContext.showToast("该清晰度暂时无法缓存")
            return
        }
        val seconds = playInfo?.durationMs?.div(1000L)?.toInt()?.takeIf { it > 0 }
            ?: d.duration
        val queued = VideoPageBus.dependencies.downloads.enqueue(
            detail = d,
            quality = quality,
            cid = currentCid,
            audioUrl = audio,
            durationSec = seconds,
        )
        videoContext.showToast(if (queued) "已加入缓存" else "该清晰度已在缓存中")
    }

    /** 下一集：先走完当前视频的分 P，再进入合集的下一集。 */
    fun nextEpisode() {
        val d = detail ?: return
        val pageIndex = d.pages.indexOfFirst { it.cid == currentCid }
        val next = if (pageIndex >= 0 && pageIndex < d.pages.lastIndex) {
            d.pages[pageIndex + 1]
        } else {
            val episodeIndex = d.episodes.indexOfFirst {
                it.cid == currentCid || it.bvid == activeBvid
            }
            if (episodeIndex >= 0 && episodeIndex < d.episodes.lastIndex) {
                val ep = d.episodes[episodeIndex + 1]
                com.aritxonly.bdfun.data.VideoPage(
                    cid = ep.cid,
                    page = episodeIndex + 2,
                    part = ep.title,
                    duration = ep.duration,
                )
            } else {
                null
            }
        }
        if (next == null) {
            context.showToast("已经是最后一集")
        } else {
            switchPage(next)
        }
    }

    fun shareVideo() {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "${detail?.title ?: video.title} https://www.bilibili.com/video/${video.bvid}",
            )
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "分享视频"),
        )
        interactionController.recordShare()
    }

    /** 退出播放器：开启后台播放时先交棒，再正常关闭页面，保留一镜到底退出动画。 */
    fun leaveVideo() {
        // 从相关推荐进来、返回是回到上一个视频页时，不交棒给后台：
        // 上一页会自己续播，交棒只会造成两路声音。
        if (settings.showDiscAfterVideo && !settings.stopOnVideoPageExit && !openedFromVideoPage) {
            VideoPageBus.dependencies.background.requestHandOff()
        }
        onBack()
    }

}
