package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.TeakNavigator
import com.aritxonly.bdfun.MainActivity
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.DanmakuItem
import com.aritxonly.bdfun.data.FavoriteFolder
import com.aritxonly.bdfun.data.PlayInfo
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.bdfun.data.VideoCacheManager
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.data.VideoPage
import com.kyant.shapes.Capsule
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 全屏选集用：优先展示 UGC 合集，否则展示多 P 分集。 */
private fun VideoDetail.playerPages(): List<VideoPage> {
    if (episodes.size > 1) {
        return episodes.mapIndexed { index, episode ->
            VideoPage(
                cid = episode.cid,
                page = index + 1,
                part = episode.title,
                duration = episode.duration,
            )
        }
    }
    return pages
}

@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)
@Composable
fun VideoScreen(
    api: BiliApi,
    video: VideoItem,
    defaultQuality: Int = 80,
    onOpenComments: (VideoDetail) -> Unit = {},
    onBack: () -> Unit,
    autoRotate: Boolean = true,
    /** 视频画面顶到状态栏下面（设置项，默认关闭）。 */
    immersiveStatusBar: Boolean = false,
    /** 由 VideoActivity 创建并持有：tab 栏直接读它的 currentPage，天然双向同步。 */
    pager: androidx.compose.foundation.pager.PagerState =
        androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 }),
) {
    val colors = MiuixTheme.colorScheme
    val videoContext = LocalContext.current
    var detail by remember { mutableStateOf<VideoDetail?>(null) }
    var activeBvid by remember { mutableStateOf(video.bvid) }
    var playInfo by remember { mutableStateOf<PlayInfo?>(null) }
    var related by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var qualityId by remember { mutableIntStateOf(defaultQuality) }
    var autoQuality by remember { mutableStateOf(defaultQuality == 0) }
    var resumePositionMs by remember {
        val explicit = if (AppGraph.settings.resumePlayback) video.startPositionMs else 0L
        val local = if (explicit <= 0L && AppGraph.settings.resumePlayback) {
            com.aritxonly.bdfun.data.PlaybackProgressStore.resumeFor(video.bvid)
        } else {
            0L
        }
        mutableLongStateOf(if (explicit > 0L) explicit else local)
    }
    var fullscreen by remember { mutableStateOf(false) }
    var danmaku by remember { mutableStateOf<List<DanmakuItem>>(emptyList()) }
    var followed by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(true) }
    // 当前分 P / 合集的 cid（多 P 切换时只换流，不重建页面）
    var currentCid by remember { mutableLongStateOf(video.cid) }
    // 字幕：官方字幕轨 + 当前选择的字幕内容
    var subtitles by remember { mutableStateOf<List<com.aritxonly.bdfun.data.SubtitleTrack>>(emptyList()) }
    var selectedSubtitle by remember {
        mutableStateOf<com.aritxonly.bdfun.data.SubtitleTrack?>(null)
    }
    var subtitleCues by remember {
        mutableStateOf<List<com.aritxonly.bdfun.data.SubtitleCue>>(emptyList())
    }

    // 互动状态（初始值来自接口，点按后乐观更新再回写）
    var liked by remember { mutableStateOf(false) }
    var disliked by remember { mutableStateOf(false) }
    var likedCount by remember { mutableLongStateOf(0L) }
    var coined by remember { mutableStateOf(false) }
    var coinCount by remember { mutableLongStateOf(0L) }
    var favorited by remember { mutableStateOf(false) }
    var favoriteCount by remember { mutableLongStateOf(0L) }
    var shareCount by remember { mutableLongStateOf(0L) }
    var descExpanded by remember { mutableStateOf(false) }
    var descExpandable by remember { mutableStateOf(false) }
    var upFollowers by remember { mutableLongStateOf(0L) }
    var upVideos by remember { mutableLongStateOf(0L) }
    var onlineCount by remember { mutableLongStateOf(0L) }
    var onlineText by remember { mutableStateOf("") }
    // 投币 / 收藏夹 弹出面板
    var coinSheet by remember { mutableStateOf(false) }
    var favSheet by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf<List<com.aritxonly.bdfun.data.FavoriteFolder>>(emptyList()) }
    var folderIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var favLoading by remember { mutableStateOf(false) }
    var favPrefetched by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(detail?.bvid) {
        val d = detail ?: return@LaunchedEffect
        liked = d.liked
        coined = d.coined
        favorited = d.favorited
        likedCount = d.like
        coinCount = d.coin
        favoriteCount = d.favorite
        shareCount = d.share
        descExpanded = false
        descExpandable = false
    }

    fun ensureLogin(): Boolean {
        if (!api.isLoggedIn) {
            context.showToast("请先登录")
            return false
        }
        return true
    }

    /** 全屏收藏面板打开时静默补齐收藏夹（预取没赶上时）。 */
    fun ensureFavoritesLoaded() {
        if (!api.isLoggedIn) {
            context.showToast("请先登录")
            return
        }
        if (favPrefetched || favLoading) return
        val d = detail ?: return
        favLoading = true
        scope.launch {
            runCatching { api.favoriteFolders() }.getOrNull()?.let { loaded ->
                folders = loaded
                folderIds = runCatching { api.favoriteFolderIds(d.aid) }.getOrDefault(emptySet())
                favPrefetched = true
            }
            favLoading = false
        }
    }

    /** 全屏更多菜单里的「稍后再看」。 */
    fun addWatchLater() {
        val d = detail ?: return
        if (!ensureLogin()) return
        scope.launch {
            val ok = api.addWatchLater(d.aid)
            context.showToast(if (ok) "已添加到稍后再看" else "添加稍后再看失败")
        }
    }

    fun syncInteractionCache() {
        val d = detail ?: return
        VideoCache.update(d.bvid) { cached ->
            cached.copy(
                liked = liked,
                like = likedCount,
                coined = coined,
                coin = coinCount,
                favorited = favorited,
                favorite = favoriteCount,
            )
        }
    }

    fun toggleFollow() {
        val mid = detail?.ownerMid ?: video.ownerMid
        if (mid <= 0) return
        if (!api.isLoggedIn) {
            context.showToast("请先登录")
            return
        }
        val next = !followed
        followed = next
        scope.launch {
            val result = runCatching { api.followUser(mid, next) }
            if (result.isSuccess) {
                context.showToast(if (next) "关注成功" else "已取消关注")
            } else {
                followed = !next
                context.showToast(result.exceptionOrNull()?.message ?: "操作失败")
            }
        }
    }

    fun toggleLike() {
        val d = detail ?: return
        if (!ensureLogin()) return
        val next = !liked
        // 服务端点赞会自动取消点踩，本地状态要同步清掉，避免两个图标同时高亮
        val wasDisliked = disliked
        val prevCount = likedCount
        liked = next
        likedCount = (likedCount + if (next) 1 else -1).coerceAtLeast(0)
        disliked = false
        syncInteractionCache()
        scope.launch {
            val result = runCatching { api.likeVideo(d.aid, next) }
            if (result.isSuccess) {
                context.showToast(if (next) "点赞成功" else "已取消赞")
            } else {
                liked = !next
                likedCount = prevCount
                disliked = wasDisliked
                syncInteractionCache()
                context.showToast(result.exceptionOrNull()?.message ?: "点赞失败")
            }
        }
    }

    fun toggleDislike() {
        val d = detail ?: return
        if (!ensureLogin()) return
        val next = !disliked
        // 点踩会取消点赞：计数要一起减回去
        val wasLiked = liked
        val prevLikeCount = likedCount
        disliked = next
        if (next && liked) {
            liked = false
            likedCount = (likedCount - 1).coerceAtLeast(0)
        }
        syncInteractionCache()
        scope.launch {
            val result = runCatching { api.dislikeVideo(d.aid, next) }
            if (result.isSuccess) {
                context.showToast(if (next) "感谢反馈" else "已取消反馈")
            } else {
                disliked = !next
                liked = wasLiked
                likedCount = prevLikeCount
                syncInteractionCache()
                context.showToast(result.exceptionOrNull()?.message ?: "操作失败")
            }
        }
    }

    fun giveCoin() {
        if (detail == null) return
        if (!ensureLogin()) return
        coinSheet = true
    }

    fun doCoin(count: Int) {
        val d = detail ?: return
        coinSheet = false
        scope.launch {
            val ok = runCatching { api.coinVideo(d.aid, count, false) }.isSuccess
            if (ok) {
                coined = true
                coinCount += count
                syncInteractionCache()
                context.showToast("投币成功")
            } else {
                context.showToast("投币失败")
            }
        }
    }

    fun toggleFavorite() {
        val d = detail ?: return
        if (!ensureLogin()) return
        if (!favorited) {
            // 未收藏：打开面板选收藏夹
            favSheet = true
            return
        }
        // 已收藏：直接取消，不再弹面板
        scope.launch {
            val ids = if (folderIds.isNotEmpty()) {
                folderIds
            } else {
                runCatching { api.favoriteFolderIds(d.aid) }.getOrDefault(emptySet())
            }
            if (ids.isEmpty()) {
                context.showToast("取消收藏失败")
                return@launch
            }
            var result = runCatching { api.removeFavoriteAll(d.aid, ids) }
            if (result.isFailure) {
                // 有的账号/收藏夹下批量删除会返回错误，退回逐个删除
                var anyOk = false
                ids.forEach { id ->
                    if (runCatching { api.setFavorite(d.aid, id, false) }.isSuccess) anyOk = true
                }
                if (anyOk) result = Result.success(true)
            }
            if (result.isSuccess) {
                folderIds = emptySet()
                favorited = false
                favoriteCount = (favoriteCount - 1).coerceAtLeast(0)
                syncInteractionCache()
                context.showToast("已取消收藏")
            } else {
                context.showToast(
                    "取消收藏失败：${result.exceptionOrNull()?.message ?: "未知错误"}",
                )
            }
        }
    }

    /** 一键三连：点赞 + 投 2 币 + 收藏（已经做过的项跳过，不会取消）。 */
    fun tripleAction() {
        val d = detail ?: return
        if (!ensureLogin()) return
        scope.launch {
            var ok = true
            if (!liked) {
                val r = runCatching { api.likeVideo(d.aid, true) }
                if (r.isSuccess) {
                    liked = true
                    likedCount += 1
                    disliked = false
                } else {
                    ok = false
                }
            }
            if (!coined) {
                val r = runCatching { api.coinVideo(d.aid, 2, false) }
                if (r.isSuccess) {
                    coined = true
                    coinCount += 2
                } else {
                    ok = false
                }
            }
            if (!favorited) {
                val folder = folders.firstOrNull()
                    ?: runCatching { api.favoriteFolders() }.getOrNull()?.firstOrNull()
                if (folder != null) {
                    val r = runCatching { api.setFavorite(d.aid, folder.id, true) }
                    if (r.isSuccess) {
                        favorited = true
                        folderIds = folderIds + folder.id
                        favoriteCount += 1
                    } else {
                        ok = false
                    }
                } else {
                    ok = false
                }
            }
            syncInteractionCache()
            context.showToast(if (ok) "已一键三连" else "一键三连部分失败")
        }
    }

    fun toggleFolder(folderId: Long) {
        val d = detail ?: return
        val contains = folderIds.contains(folderId)
        scope.launch {
            val ok = runCatching { api.setFavorite(d.aid, folderId, !contains) }.isSuccess
            if (ok) {
                folderIds = if (contains) folderIds - folderId else folderIds + folderId
                favorited = folderIds.isNotEmpty()
                favoriteCount = (favoriteCount + if (contains) -1 else 1).coerceAtLeast(0)
                syncInteractionCache()
                context.showToast(if (contains) "已取消收藏" else "已收藏")
            } else {
                context.showToast(if (contains) "取消收藏失败" else "收藏失败")
            }
        }
    }

    fun switchQuality(id: Int) {
        if (id == 0) {
            // 自动：按当前网速从现在拿到的清晰度列表里重新挑一档，不需要重新请求播放流。
            if (autoQuality) return
            resumePositionMs = VideoPageBus.danmakuProgressMs
            autoQuality = true
            qualityId = 0
            return
        }
        autoQuality = false
        if (id == qualityId) return
        val d = detail ?: return
        resumePositionMs = VideoPageBus.danmakuProgressMs
        qualityId = id
        scope.launch {
            playInfo = runCatching {
                api.playInfo(
                    d,
                    id,
                    currentCid,
                    d.duration,
                    codecPreference = AppGraph.settings.codecPreference,
                    cdnService = AppGraph.settings.cdnService,
                )
            }.getOrNull()
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
                val loaded = runCatching { api.view(episode.bvid) }.getOrNull() ?: return@launch
                detail = loaded
                activeBvid = loaded.bvid
                currentCid = page.cid
                resumePositionMs = 0L
                VideoPageBus.danmakuProgressMs = 0L
                playInfo = runCatching {
                    api.playInfo(
                        loaded,
                        qualityId,
                        page.cid,
                        page.duration,
                        codecPreference = AppGraph.settings.codecPreference,
                        cdnService = AppGraph.settings.cdnService,
                    )
                }.getOrNull()
                danmaku = runCatching { api.danmaku(page.cid) }.getOrDefault(emptyList())
                related = runCatching { api.related(loaded.bvid) }.getOrDefault(emptyList())
            }
            return
        }
        currentCid = page.cid
        resumePositionMs = 0L
        VideoPageBus.danmakuProgressMs = 0L
        scope.launch {
            playInfo = runCatching {
                api.playInfo(
                    d,
                    qualityId,
                    page.cid,
                    page.duration,
                    codecPreference = AppGraph.settings.codecPreference,
                    cdnService = AppGraph.settings.cdnService,
                )
            }.getOrNull()
            danmaku = runCatching { api.danmaku(page.cid) }.getOrDefault(emptyList())
        }
    }

    /** 缓存当前视频当前 P 的指定清晰度（视频轨 + 音轨）。 */
    fun cacheVideo(quality: Quality) {
        val d = detail ?: return
        if (VideoCacheManager.findPlayable(activeBvid, currentCid) != null) {
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
        val queued = VideoCacheManager.enqueue(
            detail = d,
            quality = quality,
            cid = currentCid,
            audioUrl = audio,
            durationSec = seconds,
        )
        videoContext.showToast(if (queued) "已加入缓存" else "该清晰度已在缓存中")
    }

    /** 下一集：当前视频有多 P / 合集时播放下一集。 */
    fun nextEpisode() {
        val d = detail ?: return
        val pages = d.playerPages()
        if (pages.size <= 1) {
            context.showToast("下一集不存在")
            return
        }
        val index = pages.indexOfFirst { it.cid == currentCid }
        val next = pages.getOrNull(index + 1)
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
        if (detail != null) shareCount += 1
    }

    BackHandler {
        if (fullscreen) fullscreen = false else onBack()
    }

    LaunchedEffect(video.bvid) {
        // 离线缓存优先：直接从本地视频轨 + 音轨起播，不再请求播放地址。
        val offline = VideoCacheManager.findPlayable(video.bvid, video.cid)
        if (offline != null) {
            val offlinePlay = offline.toPlayInfo()
            com.aritxonly.bdfun.data.LogCollector.record(
                "VIDEO",
                "source=offline bvid=${video.bvid} qualities=${offlinePlay.qualities.size} " +
                    "audio=${offlinePlay.audioUrl != null}",
            )
            detail = offline.toDetail()
            playInfo = offlinePlay
            currentCid = offline.cid
            danmaku = emptyList()
            related = emptyList()
            loading = false
            return@LaunchedEffect
        }
        // 从「我的评论与弹幕」等入口进来时，先定位到记录的时间点
        if (AppGraph.settings.resumePlayback && video.startPositionMs > 0L) {
            resumePositionMs = video.startPositionMs
        }
        // 从相关推荐点进去再返回：直接用缓存，不再重新请求
        val cached = VideoCache.get(video.bvid)
        if (cached != null) {
            com.aritxonly.bdfun.data.LogCollector.record(
                "VIDEO",
                "source=memory-cache bvid=${video.bvid} " +
                    "qualities=${cached.playInfo?.qualities?.size} " +
                    "audio=${cached.playInfo?.audioUrl != null}",
            )
            detail = cached.detail
            playInfo = cached.playInfo
            danmaku = cached.danmaku
            related = cached.related
            currentCid = cached.detail.cid
            if (AppGraph.settings.resumePlayback && resumePositionMs <= 0L) {
                resumePositionMs = runCatching {
                    api.cloudResumePositionMs(
                        cached.detail.aid,
                        cached.detail.cid,
                        cached.detail.duration,
                    )
                }.getOrDefault(0L)
            }
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val viewed = runCatching { api.view(video.bvid) }
        val loaded = viewed.getOrNull()
        if (loaded == null) {
            error = viewed.exceptionOrNull()?.message ?: "加载失败"
            loading = false
        } else {
            detail = loaded
            currentCid = loaded.cid
            // playurl / 弹幕 / 相关推荐互不依赖，并行发起：
            // 播放地址先回来就先起播，不再等弹幕和相关推荐串行跑完。
            var playError: Throwable? = null
            val playDeferred = async {
                runCatching {
                    api.playInfo(
                        loaded,
                        defaultQuality,
                        codecPreference = AppGraph.settings.codecPreference,
                        cdnService = AppGraph.settings.cdnService,
                    )
                }.onFailure { playError = it }.getOrNull()
            }
            val danmakuDeferred = async {
                runCatching { api.danmaku(loaded.cid) }.getOrDefault(emptyList())
            }
            val relatedDeferred = async {
                runCatching { api.related(video.bvid) }.getOrDefault(emptyList())
            }
            // 本地没有进度时，再问一次 B 站云端（换设备/重装后靠这个）。
            val cloudResumeDeferred = async {
                if (AppGraph.settings.resumePlayback && resumePositionMs <= 0L) {
                    runCatching {
                        api.cloudResumePositionMs(loaded.aid, loaded.cid, loaded.duration)
                    }.getOrDefault(0L)
                } else {
                    0L
                }
            }
            val play = playDeferred.await()
            val cloudResume = cloudResumeDeferred.await()
            if (resumePositionMs <= 0L && cloudResume > 0L) {
                resumePositionMs = cloudResume
            }
            playInfo = play
            com.aritxonly.bdfun.data.LogCollector.record(
                "VIDEO",
                "source=network bvid=${video.bvid} hasInfo=${play != null} " +
                    "qualities=${play?.qualities?.size} audio=${play?.audioUrl != null} " +
                    "err=${playError?.javaClass?.simpleName}:${playError?.message?.take(160)}",
            )
            // 播放地址就绪即可撤掉全屏 loading，弹幕和相关推荐在后台继续补。
            loading = false
            danmaku = danmakuDeferred.await()
            related = relatedDeferred.await()
            VideoCache.put(
                video.bvid,
                VideoCacheEntry(loaded, playInfo, danmaku, related),
            )
        }
    }

    // UP 粉丝数 / 投稿数 / 当前关注状态
    LaunchedEffect(detail?.ownerMid) {
        val mid = detail?.ownerMid ?: 0L
        if (mid <= 0) return@LaunchedEffect
        // 缓存里的 isFollowed 可能是进视频那一刻的旧值，重新问一次官方关系接口
        val attribute = if (api.isLoggedIn) {
            runCatching { api.relation(mid) }.getOrNull()
        } else {
            null
        }
        followed = if (attribute != null) {
            attribute == 2 || attribute == 6
        } else {
            detail?.isFollowed == true
        }
        upFollowers = runCatching { api.relationStat(mid).second }.getOrDefault(0L)
        upVideos = api.upVideoCount(mid)
    }

    // 当前同时观看人数
    LaunchedEffect(detail?.aid, currentCid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || currentCid <= 0) return@LaunchedEffect
        val (count, text) = api.onlineTotal(d.aid, currentCid)
        onlineCount = count
        onlineText = text
    }

    // 官方字幕轨：切分 P 后重新拉，默认关闭
    LaunchedEffect(detail?.aid, currentCid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || currentCid <= 0) return@LaunchedEffect
        subtitles = runCatching { api.subtitles(d.aid, currentCid) }.getOrDefault(emptyList())
        selectedSubtitle = null
        subtitleCues = emptyList()
    }

    // 选中字幕轨后下载 JSON 并解析成时间轴
    LaunchedEffect(selectedSubtitle) {
        val track = selectedSubtitle ?: run {
            subtitleCues = emptyList()
            return@LaunchedEffect
        }
        subtitleCues = runCatching { api.subtitleCues(track.url) }.getOrDefault(emptyList())
    }

    // 详情一到位就预取收藏夹，点「收藏」时直接出内容，不再转圈
    LaunchedEffect(detail?.aid) {
        val d = detail ?: return@LaunchedEffect
        if (d.aid <= 0 || !api.isLoggedIn) return@LaunchedEffect
        val loaded = runCatching { api.favoriteFolders() }.getOrNull() ?: return@LaunchedEffect
        folders = loaded
        folderIds = runCatching { api.favoriteFolderIds(d.aid) }.getOrDefault(emptySet())
        favPrefetched = true
    }

    // 预取失败（或没来得及）时，打开面板只做静默补齐；面板本身不显示 loading
    LaunchedEffect(favSheet) {
        if (!favSheet || favPrefetched) return@LaunchedEffect
        val d = detail ?: return@LaunchedEffect
        favLoading = true
        runCatching { api.favoriteFolders() }.getOrNull()?.let { loaded ->
            folders = loaded
            folderIds = runCatching { api.favoriteFolderIds(d.aid) }.getOrDefault(emptySet())
            favPrefetched = true
        }
        favLoading = false
    }

    // 把本页的 aid/cid 与「发弹幕」发送器交给 Activity 覆盖层（那边在采集层之外，拿不到 api）
    DisposableEffect(currentCid, detail?.aid) {
        val d = detail
        if (d != null) {
            VideoPageBus.aid = d.aid
            VideoPageBus.cid = currentCid
            VideoPageBus.danmakuSender = { text, mode, fontSize, color ->
                val ok = runCatching {
                    api.sendDanmaku(
                        cid = currentCid,
                        bvid = d.bvid,
                        text = text,
                        progressMs = VideoPageBus.danmakuProgressMs,
                        mode = mode,
                        fontSize = fontSize,
                        color = color,
                    )
                }.isSuccess
                if (ok) {
                    AppGraph.sentHistory.record(
                        kind = "danmaku",
                        text = text,
                        bvid = d.bvid,
                        title = d.title,
                        videoTimeMs = VideoPageBus.danmakuProgressMs,
                    )
                    // 自己刚发的弹幕立即出现在弹幕层里（带圆角底）
                    danmaku = danmaku + com.aritxonly.bdfun.data.DanmakuItem(
                        timeMs = VideoPageBus.danmakuProgressMs,
                        mode = mode,
                        color = color,
                        size = fontSize / 25f,
                        text = text,
                        self = true,
                    )
                }
                ok
            }
        }
        onDispose { VideoPageBus.danmakuSender = null }
    }

    // 全屏状态共享给 Activity（用于让顶栏/tab/回复栏在横屏时隐藏）
    LaunchedEffect(fullscreen) { PlayerUiState.fullscreen = fullscreen }
    // 横屏（全屏）时把投币/收藏这些弹层提前收回，不在横屏里出现
    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            coinSheet = false
            favSheet = false
        }
    }
    // 根据屏幕方向自动进入/退出全屏
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val landscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // 横屏即全屏，竖屏即退出全屏（视频页强制跟随传感器方向）
    LaunchedEffect(landscape) {
        fullscreen = landscape
    }

    val qualities = playInfo?.qualities.orEmpty()
    val activeQuality = if (autoQuality) {
        autoQualityFor(qualities, videoContext)
    } else {
        qualities.firstOrNull { it.id == qualityId } ?: qualities.firstOrNull()
    }

    // 状态栏高度：隐藏状态栏时不变，首帧 insets 未下发时按系统资源兜底。
    // 视频容器和 tab 都用这一个值，进入页面时不会一个先跳一个后跳。
    val statusTop = rememberStatusBarTop()

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

    Box(modifier = Modifier.fillMaxSize()) {
        val listState = rememberLazyListState()
        TrackTeakPageScroll(listState)

        // 全屏时只隐藏、不卸载：否则退回竖屏评论列表会整段重载
        run {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (fullscreen) 0f else 1f }
                    .blockTouches(fullscreen),
            ) {
                // 状态栏高度由外层统一提供（见 statusTop）
                val playerHeight = maxWidth * 9f / 16f +
                    if (immersiveStatusBar) 0.dp else statusTop
                HorizontalPager(
                    state = pager,
                    // 两页都保持 compose：切到简介再回来，评论列表不重载
                    beyondViewportPageCount = 1,
                    // 只有两页：到边后再往外滑不要任何回弹动画
                    overscrollEffect = null,
                    // 允许左右滑动；tab 直接读 pager.currentPage，会跟着一起动
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    if (page == 0) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = playerHeight + 52.dp,
                                bottom = 32.dp,
                            ),
                        ) {
            item(key = "meta") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    // UP 行：头像 + 昵称 + 签名 + 粉色实心关注
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = rememberImageRequest(detail?.ownerFace ?: video.ownerFace),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                ) {
                                    // 进个人主页前先停掉视频，不要带着声音过去
                                    VideoPageBus.pauseAll()
                                    TeakNavigator.openSpace(
                                        videoContext,
                                        detail?.ownerMid ?: video.ownerMid,
                                    )
                                }
                                .clip(CircleShape)
                                .background(colors.surfaceContainerHigh),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f).offset(y = 1.dp)) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    text = detail?.ownerName ?: video.ownerName,
                                    color = colors.onBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                )
                            }
                            Text(
                                text = when {
                                    upFollowers > 0 || upVideos > 0 ->
                                        "粉丝 ${formatCount(upFollowers)} · 视频 ${formatCount(upVideos)}"
                                    else -> detail?.ownerSign.orEmpty().ifBlank {
                                        detail?.tname.orEmpty().ifEmpty { "UP 主" }
                                    }
                                },
                                color = colors.onBackgroundVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.offset(x = 1.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // 纯色 + 高光描边的关注按钮
                        Box(
                            modifier = Modifier
                                .clip(Capsule())
                                .background(BrandPink)
                                .border(1.dp, Color.White.copy(alpha = 0.45f), Capsule())
                                .clickable(interactionSource = null, indication = null) {
                                    toggleFollow()
                                }
                                .padding(horizontal = 14.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = if (followed) "已关注" else "+ 关注",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    // 标题：可长按选择 / 复制
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = detail?.title ?: video.title,
                            color = colors.onBackground,
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    // 播放 / 弹幕 / 日期（各元素间距统一 8dp）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetricGlyph(play = true, color = colors.onBackgroundVariant, size = 14.dp)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = formatCount(detail?.play ?: video.play.toLong()),
                            color = colors.onBackgroundVariant,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 14.dp)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = formatCount(detail?.danmaku ?: video.danmaku.toLong()),
                            color = colors.onBackgroundVariant,
                            fontSize = 12.sp,
                        )
                        detail?.pubdate?.takeIf { it > 0 }?.let {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatDateTime(it),
                                color = colors.onBackgroundVariant,
                                fontSize = 12.sp,
                            )
                        }
                        if (onlineCount > 0 || onlineText.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            PersonGlyph(color = colors.onBackgroundVariant, size = 12.dp)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${onlineText.ifBlank { formatCount(onlineCount) }} 人",
                                color = colors.onBackgroundVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    val descText = detail?.desc?.takeIf { it.isNotBlank() }
                    val descProgress = rememberManualProgress(
                        if (descExpanded) 1f else 0f,
                        durationMs = 420f,
                    )
                    Spacer(Modifier.height(4.dp))
                    // BV 号那一栏：最右侧的箭头负责展开 / 收起简介（简介没被折叠时不显示）
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = detail?.bvid ?: video.bvid,
                                color = colors.onBackgroundVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (descText != null && descExpandable) {
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = 24.dp)
                                    .clip(Capsule())
                                    .clickable(
                                        interactionSource = null,
                                        indication = null,
                                    ) { descExpanded = !descExpanded },
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .size(width = 12.dp, height = 8.dp)
                                        .graphicsLayer { rotationZ = 180f * descProgress },
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(w * 0.12f, h * 0.28f)
                                        lineTo(w * 0.5f, h * 0.72f)
                                        lineTo(w * 0.88f, h * 0.28f)
                                    }
                                    drawPath(
                                        path = path,
                                        color = colors.onBackgroundVariant,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 1.6.dp.toPx(),
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    if (descText != null) {
                        Spacer(Modifier.height(4.dp))
                        CollapsibleDescription(
                            text = descText,
                            progress = descProgress,
                            color = colors.onBackgroundVariant,
                            backgroundColor = colors.background,
                            onExpandableChange = { descExpandable = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 1.dp)
                                .pointerInput(descText) {
                                    detectTapGestures(
                                        onLongPress = {
                                            context.copyToClipboard("简介", descText)
                                            context.showToast("已复制简介")
                                        },
                                    )
                                },
                        )
                    }

                    // 分区标签
                    if (!detail?.tname.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Box(
                                modifier = Modifier
                                    .clip(Capsule())
                                    .background(colors.surfaceContainerHigh)
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    text = detail!!.tname,
                                    color = colors.onBackgroundVariant,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    // 五宫格：点赞 / 不喜欢 / 币 / 收藏 / 分享
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        ActionStat(
                            label = "点赞",
                            value = likedCount,
                            kind = ActionKind.ThumbUp,
                            active = liked,
                            onClick = { toggleLike() },
                        )
                        ActionStat(
                            label = "不喜欢",
                            value = 0,
                            kind = ActionKind.ThumbDown,
                            active = disliked,
                            onClick = { toggleDislike() },
                        )
                        ActionStat(
                            label = "投币",
                            value = coinCount,
                            kind = ActionKind.Coin,
                            active = coined,
                            onClick = { giveCoin() },
                            onLongPress = { tripleAction() },
                        )
                        ActionStat(
                            label = "收藏",
                            value = favoriteCount,
                            kind = ActionKind.Star,
                            active = favorited,
                            onClick = { toggleFavorite() },
                        )
                        ActionStat(
                            label = "分享",
                            value = shareCount,
                            kind = ActionKind.Share,
                            active = false,
                            onClick = { shareVideo() },
                        )
                    }
                }
            }

            if (error != null) {
                item(key = "error") {
                    Text(
                        text = error!!,
                        modifier = Modifier.padding(16.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                }
            }

            if (related.isNotEmpty()) {
                item(key = "related-card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 6.dp)
                            .clip(ContinuousCornerShape(16.dp))
                            .background(colors.surface)
                            .padding(vertical = 2.dp),
                    ) {
                        related.forEachIndexed { index, relatedItem ->
                            RelatedRow(
                                item = relatedItem,
                                onClick = {
                                    // 先同步停掉当前播放器，再启动新页面，避免转场期间双声。
                                    VideoPageBus.pauseAll()
                                    com.aritxonly.bdfun.TeakNavigator.openVideo(
                                        context,
                                        relatedItem,
                                    )
                                },
                            )
                        }
                    }
                }
            }
                        }
                    } else {
                        val d = detail
                        if (d != null) {
                            CommentScreen(
                                api = api,
                                aid = d.aid,
                                upMid = d.ownerMid,
                                title = d.title,
                                onBack = {},
                                bvid = d.bvid,
                                bare = true,
                                topInset = playerHeight + 14.dp,
                            )
                        }
                    }
                }
            }
        }

        if (loading) {
            TeakLoading(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
                    .size(28.dp),
                color = colors.primary,
            )
        }

        Box(
            modifier = if (fullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    // 非沉浸：视频让开状态栏；沉浸：视频铺到 0。
                    // 用 statusTop 而不是 windowInsetsPadding，首帧 insets 未下发时也不会先跳一帧
                    .then(if (immersiveStatusBar) Modifier else Modifier.padding(top = statusTop))
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .align(Alignment.TopCenter)
            },
        ) {
            VideoPlayer(
                bvid = activeBvid,
                info = playInfo,
                quality = activeQuality,
                autoQuality = autoQuality,
                cover = detail?.cover ?: video.cover,
                fullscreen = fullscreen,
                title = detail?.title ?: video.title,
                author = detail?.ownerName ?: video.ownerName,
                danmaku = danmaku,
                fullscreenInfo = FullscreenPlayerInfo(
                    title = detail?.title ?: video.title,
                    author = detail?.ownerName ?: video.ownerName,
                    authorFace = detail?.ownerFace ?: video.ownerFace,
                    onlineText = onlineText,
                    onlineCount = onlineCount,
                    followed = followed,
                    liked = liked,
                    likeCount = likedCount,
                    replyCount = detail?.reply ?: 0L,
                    favorited = favorited,
                    favoriteCount = favoriteCount,
                    shareCount = shareCount,
                    qualityLabel = if (autoQuality) "自动" else activeQuality?.label ?: "",
                    cover = detail?.cover ?: video.cover,
                    pages = detail?.playerPages().orEmpty(),
                    currentCid = currentCid,
                    folders = folders,
                    folderIds = folderIds,
                    loggedIn = api.isLoggedIn,
                    subtitles = subtitles,
                    selectedSubtitle = selectedSubtitle,
                    subtitleCues = subtitleCues,
                    danmakuItems = danmaku,
                ),
                onLikeClick = { toggleLike() },
                onDislikeClick = { toggleDislike() },
                onCommentClick = {
                    val d = detail
                    if (d != null) {
                        fullscreen = false
                        onOpenComments(d)
                    }
                },
                onShareClick = { shareVideo() },
                onOpenSpaceClick = { toggleFollow() },
                onToggleFolder = { toggleFolder(it) },
                onRequestFavorites = { ensureFavoritesLoaded() },
                onPageSelected = { switchPage(it) },
                onSubtitleSelected = { selectedSubtitle = it },
                onAddWatchLater = { addWatchLater() },
                onPlayStateChanged = { playing = it },
                onToggleFullscreen = { fullscreen = !fullscreen },
                onBackClick = onBack,
                onHomeClick = {
                    videoContext.startActivity(
                        android.content.Intent(videoContext, MainActivity::class.java).apply {
                            addFlags(
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP,
                            )
                        },
                    )
                },
                resumePositionMs = resumePositionMs,
                onQualitySelected = { switchQuality(it) },
                onCacheVideo = { cacheVideo(it) },
                immersiveStatusBar = immersiveStatusBar,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    TeakActionSheet(
        visible = coinSheet,
        onDismiss = { coinSheet = false },
        title = "请选择投币数量",
    ) {
        SheetActionRow("投 1 枚硬币", MenuGlyphKind.Coin, MiuixTheme.colorScheme.onBackground) {
            doCoin(1)
        }
        SheetActionRow("投 2 枚硬币", MenuGlyphKind.Coin, MiuixTheme.colorScheme.onBackground) {
            doCoin(2)
        }
    }

    TeakActionSheet(
        visible = favSheet,
        onDismiss = { favSheet = false },
        title = "请选择收藏夹",
    ) {
        if (folders.isEmpty() && favLoading) {
            // 预取还没回来：先占位，避免闪一下“没有收藏夹”；正常情况下看不到这一屏
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .height(46.dp)
                        .clip(Capsule())
                        .background(MiuixTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
                )
            }
        } else if (folders.isEmpty()) {
            Text(
                text = "没有可用的收藏夹",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp),
            )
        } else {
            folders.forEach { folder ->
                val checked = folderIds.contains(folder.id)
                SheetActionRow(
                    label = folder.title,
                    glyph = MenuGlyphKind.Star,
                    color = MiuixTheme.colorScheme.onBackground,
                    trailing = {
                        if (checked) {
                            Text(
                                text = "✓",
                                color = BrandPink,
                                fontSize = 16.sp,
                            )
                        }
                    },
                ) {
                    toggleFolder(folder.id)
                }
            }
        }
    }
}

@Composable
private fun ActionStat(
    label: String,
    value: Long,
    kind: ActionKind,
    active: Boolean = false,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    var isPressing by remember { mutableStateOf(false) }
    var press by remember { mutableFloatStateOf(0f) }
    // 单击投币时模糊回弹时间固定 230ms，其它操作 160ms
    val pressDecayMs = if (kind == ActionKind.Coin) 230f else 160f
    // 按住期间一直保持压满状态，松开后才回弹
    LaunchedEffect(isPressing) {
        if (isPressing) {
            press = 1f
        } else {
            // 首帧 press 本来就是 0，别把它“回弹”成 1 再落回去
            if (press <= 0.01f) return@LaunchedEffect
            val startNanos = System.nanoTime()
            while (true) {
                val t = ((System.nanoTime() - startNanos) / 1_000_000f / pressDecayMs)
                    .coerceIn(0f, 1f)
                press = 1f - t
                if (t >= 1f) break
                kotlinx.coroutines.delay(8)
            }
            press = 0f
        }
    }
    // 变粉 / 恢复都走渐变动画；按住期间保持粉色，松手后等按压回弹结束再渐隐
    val colorTarget = if (active || isPressing || press > 0.01f) BrandPink else colors.onBackgroundVariant
    val animatedTint by androidx.compose.animation.animateColorAsState(
        targetValue = colorTarget,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 240,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "action-tint",
    )

    // 长按投币：圆环随按住时间逐渐绕满，绕满后发光并触发一键三连
    var ringTick by remember { mutableIntStateOf(0) }
    var ringProgress by remember { mutableFloatStateOf(0f) }
    var ringAlpha by remember { mutableFloatStateOf(0f) }
    var ringGlow by remember { mutableStateOf(false) }
    var ringScale by remember { mutableFloatStateOf(1f) }
    var glowAlpha by remember { mutableFloatStateOf(0f) }
    var longPressFired by remember { mutableStateOf(false) }
    var glowPhase by remember { mutableFloatStateOf(0f) }
    // 先按住一小会才启动三连圆环；普通单击只打开投币面板
    LaunchedEffect(isPressing) {
        if (isPressing && onLongPress != null) {
            delay(280)
            if (isPressing) {
                longPressFired = false
                ringTick++
            }
        }
    }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    fun strongVibrate() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as? android.os.VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.cancel()
        // 一下大的长震，而不是连续两下
        vibrator.vibrate(
            android.os.VibrationEffect.createOneShot(
                420,
                255,
            ),
        )
    }
    LaunchedEffect(ringGlow) {
        if (!ringGlow) return@LaunchedEffect
        while (ringGlow) {
            glowPhase += 5f
            kotlinx.coroutines.delay(16)
        }
    }
    LaunchedEffect(ringTick) {
        if (onLongPress == null || ringTick == 0) return@LaunchedEffect
        ringGlow = false
        longPressFired = false
        ringProgress = 0f
        ringAlpha = 0f
        glowAlpha = 0f
        ringScale = 1f
        val startNanos = System.nanoTime()
        var lastHapticAt = -1000f
        while (true) {
            if (!isPressing) {
                // 没到三连就松手：直接清掉圆环和光效
                ringGlow = false
                glowAlpha = 0f
                ringProgress = 0f
                ringAlpha = 0f
                ringScale = 1f
                break
            }
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000f
            // 画圈过程中持续轻震动；三连完成后即使还按着也不再震
            if (!longPressFired && elapsedMs - lastHapticAt >= 90f) {
                lastHapticAt = elapsedMs
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            val p = (elapsedMs / 1150f).coerceIn(0f, 1f)
            ringProgress = p
            ringAlpha = 1f
            if (p >= 1f && !longPressFired) {
                longPressFired = true
                ringGlow = true
                glowAlpha = 1f
                // 完成时用系统马达做一次更强、更长的震动
                strongVibrate()
                onLongPress()
                // 结束时圆环和光效一起向外扩、渐隐；手指仍按着也不再保留
                val fromAlpha = ringAlpha
                val fadeStart = System.nanoTime()
                while (true) {
                    val t = ((System.nanoTime() - fadeStart) / 1_000_000f / 420f)
                        .coerceIn(0f, 1f)
                    ringAlpha = fromAlpha * (1f - t)
                    glowAlpha = 1f - t
                    ringScale = 1f + 0.18f * t
                    if (t >= 1f) break
                    kotlinx.coroutines.delay(8)
                }
                ringGlow = false
                ringProgress = 0f
                ringAlpha = 0f
                glowAlpha = 0f
                ringScale = 1f
                break
            }
            kotlinx.coroutines.delay(8)
        }
    }

    Column(
        modifier = Modifier
            .width(58.dp)
            // 不要 clip：外扩的圆环需要画出 36dp 图标框之外
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    // 所有按钮按住期间都保持压满状态
                    isPressing = true
                    press = 1f
                    val up = waitForUpOrCancellation()
                    isPressing = false
                    if (up != null && !longPressFired) onClick()
                }
            }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 发光层和进度圆环不跟着图标一起模糊，保持清晰
            if (ringGlow) {
                // 纯圆形渐变：粉色径向光晕 + 两层旋转七彩环，不会有方形图层边缘
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            rotationZ = glowPhase
                            alpha = glowAlpha
                        },
                ) {
                    val center = androidx.compose.ui.geometry.Offset(
                        size.width / 2f,
                        size.height / 2f,
                    )
                    val maxR = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BrandPink.copy(alpha = 0.50f),
                                BrandPink.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = maxR,
                        ),
                        radius = maxR,
                        center = center,
                    )
                    val rainbow = listOf(
                        BrandPink,
                        Color(0xFFB388FF),
                        Color(0xFF64D2FF),
                        Color(0xFFFFE066),
                        BrandPink,
                    )
                    drawCircle(
                        brush = Brush.sweepGradient(rainbow, center),
                        radius = maxR * 0.74f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 10.dp.toPx(),
                        ),
                        alpha = 0.65f,
                    )
                    drawCircle(
                        brush = Brush.sweepGradient(rainbow, center),
                        radius = maxR * 0.54f,
                        center = center,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 6.dp.toPx(),
                        ),
                        alpha = 0.40f,
                    )
                }
            }
            if (onLongPress != null && ringAlpha > 0.01f && (ringProgress > 0f || ringGlow)) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(35.dp)
                        .graphicsLayer {
                            scaleX = ringScale
                            scaleY = ringScale
                        },
                ) {
                    drawArc(
                        color = BrandPink.copy(alpha = ringAlpha),
                        startAngle = -90f,
                        sweepAngle = 360f * ringProgress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.4.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        ),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        val s = 1f - 0.09f * press
                        scaleX = s
                        scaleY = s
                    }
                    .blur((2.8f * press).dp),
                contentAlignment = Alignment.Center,
            ) {
                ResourceGlyph(
                    res = kind.res,
                    tint = animatedTint,
                    size = 27.dp,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (value > 0) formatCount(value) else label,
            color = animatedTint,
            fontSize = 11.sp,
        )
    }
}

private enum class ActionKind(val res: Int) {
    ThumbUp(TeakIcon.Like),
    ThumbDown(TeakIcon.Dislike),
    Coin(TeakIcon.Coin),
    Star(TeakIcon.Star),
    Share(TeakIcon.Share),
}

@Composable
private fun MiniStat(text: String, playIcon: Boolean) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
            if (playIcon) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.3f, size.height * 0.12f)
                    lineTo(size.width * 0.3f, size.height * 0.88f)
                    lineTo(size.width * 0.9f, size.height * 0.5f)
                    close()
                }
                drawPath(path, colors.onBackgroundVariant)
            } else {
                drawRoundRect(
                    color = colors.onBackgroundVariant,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * 0.15f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height * 0.15f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.1f),
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(text = text, color = colors.onBackgroundVariant, fontSize = 12.sp)
    }
}

@Composable
private fun PersonGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 12.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            color = color,
            radius = w * 0.17f,
            center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.26f),
        )
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.5f),
            size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.42f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.3f, w * 0.3f),
        )
    }
}

/**
 * 可折叠简介：用 TextMeasurer 量出完整高度，再按手动进度裁剪高度，
 * 不依赖 frame clock 的 animateContentSize（本项目里它不推进）。
 */
@Composable
private fun CollapsibleDescription(
    text: String,
    progress: Float,
    color: Color,
    backgroundColor: Color,
    onExpandableChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = androidx.compose.ui.text.TextStyle(
        color = color,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        val measured = measurer.measure(
            text = androidx.compose.ui.text.AnnotatedString(text),
            style = style,
            maxLines = Int.MAX_VALUE,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
        )
        val fullPx = measured.size.height.toFloat()
        // 两行的真实高度与第二行结束位置：模糊只作用在这之后的部分
        val twoLineLayout = measurer.measure(
            text = androidx.compose.ui.text.AnnotatedString(text),
            style = style,
            maxLines = 2,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
        )
        val twoLinePx = twoLineLayout.size.height.toFloat()
        val collapsedPx = minOf(twoLinePx, fullPx)
        val tailText = if (twoLineLayout.lineCount >= 2) {
            val end = runCatching { twoLineLayout.getLineEnd(1, visibleEnd = false) }
                .getOrDefault(text.length)
                .coerceIn(0, text.length)
            text.substring(end)
        } else {
            ""
        }
        // 以「完整高度 > 两行高度」判定是否真的被折叠，比 hasVisualOverflow 稳
        LaunchedEffect(fullPx, collapsedPx) {
            if (fullPx > collapsedPx + 1f) onExpandableChange()
        }
        val p = progress.coerceIn(0f, 1f)
        // 给前两行下面再留 2dp 安全区：即使实际渲染行高和测量值有 1~2dp 偏差，
        // 模糊也进不到前两行
        val safeTopPx = collapsedPx + with(density) { 2.dp.toPx() }
        val targetPx = safeTopPx +
            (fullPx - safeTopPx).coerceAtLeast(0f) * p
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { targetPx.toDp() })
                .clipToBounds(),
        ) {
            // 先画第三行及以后（带模糊），再盖一层背景和清晰的前两行：
            // 即使模糊半径向上溢出，也会被背景层完全挡住
            if (tailText.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .offset(y = with(density) { safeTopPx.toDp() })
                        .fillMaxWidth()
                        .height(with(density) { (fullPx - safeTopPx).coerceAtLeast(0f).toDp() }),
                ) {
                    Text(
                        text = tailText,
                        style = style,
                        maxLines = Int.MAX_VALUE,
                        modifier = Modifier
                            .graphicsLayer { alpha = p }
                            .blur(((1f - p).coerceIn(0f, 1f) * 9f).dp),
                    )
                }
            }
            // 遮住前两行区域的模糊溢出
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { safeTopPx.toDp() })
                    .background(backgroundColor)
                    .zIndex(0.5f),
            )
            // 折叠态：两行 + 省略号（随进度淡出）
            Text(
                text = text,
                style = style,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { layout ->
                    if (layout.hasVisualOverflow) onExpandableChange()
                },
                modifier = Modifier
                    .graphicsLayer { alpha = 1f - p }
                    .zIndex(1f),
            )
            // 原有的前两行：展开后保持清晰，不参与模糊
            Text(
                text = text,
                style = style,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .graphicsLayer { alpha = p }
                    .zIndex(1f),
            )
        }
    }
}

@Composable
private fun RelatedRow(item: VideoItem, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val coverWidth = 150.dp
    val coverHeight = coverWidth * 9f / 16f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(coverWidth)
                .height(coverHeight)
                .clip(ContinuousCornerShape(10.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(item.cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // 时长直接压在封面上，不加任何背景
            if (item.duration > 0) {
                Text(
                    text = formatDuration(item.duration),
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        // 右列高度锁成封面高度：标题第一行贴封面顶部，UP 行贴封面底部
        Column(
            modifier = Modifier.weight(1f).height(coverHeight),
        ) {
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.padding(start = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricGlyph(play = true, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(item.play),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.width(6.dp))
                MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(item.danmaku),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpBadge()
                Spacer(Modifier.width(5.dp))
                Text(
                    text = item.ownerName,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}


/**
 * 「自动」清晰度：按系统给出的当前网络下行带宽选一档。
 * 不看 WiFi/移动网络类型，只按实际带宽走；带宽未知时保守选 720P。
 */
private fun autoQualityFor(
    qualities: List<Quality>,
    context: android.content.Context,
): Quality? {
    if (qualities.isEmpty()) return null
    val mbps = runCatching {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        (caps?.linkDownstreamBandwidthKbps ?: 0) / 1024.0
    }.getOrDefault(0.0)
    val target = when {
        mbps >= 40.0 -> 127
        mbps >= 24.0 -> 120
        mbps >= 10.0 -> 112
        mbps >= 6.0 -> 80
        mbps >= 3.0 -> 64
        mbps >= 1.5 -> 32
        mbps > 0.0 -> 16
        else -> 64
    }
    return autoQualityLadder
        .firstOrNull { id -> id <= target && qualities.any { it.id == id } }
        ?.let { id -> qualities.first { it.id == id } }
        ?: qualities.minByOrNull { it.id }
}

/** 自动选档只走常规 SDR 阶梯，避开杜比视界/HDR，避免自动切到设备解不了的流。 */
private val autoQualityLadder = listOf(127, 120, 116, 112, 80, 74, 64, 32, 16)

private data class VideoCacheEntry(
    val detail: VideoDetail,
    val playInfo: PlayInfo?,
    val danmaku: List<DanmakuItem>,
    val related: List<VideoItem>,
)

/** 视频详情的内存缓存：从相关推荐返回时不再重新请求。 */
private object VideoCache {
    private val map = LinkedHashMap<String, VideoCacheEntry>()

    fun get(bvid: String): VideoCacheEntry? = map[bvid]

    fun put(bvid: String, entry: VideoCacheEntry) {
        map[bvid] = entry
        while (map.size > 6) {
            val first = map.keys.firstOrNull() ?: break
            map.remove(first)
        }
    }

    /** 点赞 / 投币 / 收藏后同步缓存，退出再进来仍然显示已操作状态。 */
    fun update(bvid: String, transform: (VideoDetail) -> VideoDetail) {
        val entry = map[bvid] ?: return
        map[bvid] = entry.copy(detail = transform(entry.detail))
    }
}
