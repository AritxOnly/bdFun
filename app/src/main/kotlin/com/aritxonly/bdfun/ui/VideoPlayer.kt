package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.TeakGlassPanel

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.util.Rational
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.R
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.DanmakuItem
import com.aritxonly.bdfun.data.PlayInfo
import com.aritxonly.bdfun.data.PlaybackProgressStore
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/** 控件无操作后自动渐隐的时长。 */
private const val HIDE_DELAY_MS = 4000L

private val TopGradientFullscreen = 120.dp
private val TopGradientInline = 64.dp
private val BottomGradientFullscreen = 160.dp
private val BottomGradientInline = 92.dp

/**
 * 按硬解模式构建 ExoPlayer：
 * 0 自动 / 1 硬件解码（失败回退软解）/ 2 软件解码 / 3 硬件解码（禁止回退）。
 */
private fun buildPlayer(context: Context, decoderMode: Int): ExoPlayer {
    val renderers = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(decoderMode != 3)
        // 长按倍速时让 AudioTrack 直接改播放参数，避免 Sonic 音频缓冲导致
        // “震动已经触发、过一两秒才听到加速”的延迟。
        .setEnableAudioTrackPlaybackParams(true)
    if (decoderMode in 1..3) {
        renderers.setMediaCodecSelector(TeakCodecSelector(decoderMode))
    }
    val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(15_000, 120_000, 1_000, 2_000)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()
    return ExoPlayer.Builder(context, renderers)
        .setLoadControl(loadControl)
        // 让 ExoPlayer 自己申请音频焦点：相关推荐新开播放器时，旧播放器会因失焦自动暂停，
        // 转场慢的机型也不会两路声音同时出。
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        .build()
}

private class TeakCodecSelector(private val mode: Int) : MediaCodecSelector {
    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): List<MediaCodecInfo> {
        val all = MediaCodecUtil.getDecoderInfos(
            mimeType,
            requiresSecureDecoder,
            requiresTunnelingDecoder,
        )
        return when (mode) {
            // 硬件解码：硬解排在前面，硬解初始化失败时由 ExoPlayer 回退到列表里的软解。
            1 -> all.filter { !it.softwareOnly } + all.filter { it.softwareOnly }
            2 -> all.filter { it.softwareOnly }
            // 硬件解码（禁止回退）：列表里只留硬解，buildPlayer 同时关掉 fallback。
            3 -> all.filter { !it.softwareOnly }
            else -> all
        }
    }
}

@Composable
internal fun VideoPlayer(
    onPlayStateChanged: (Boolean) -> Unit = {},
    bvid: String,
    info: PlayInfo?,
    quality: Quality?,
    autoQuality: Boolean = false,
    cover: String,
    fullscreen: Boolean,
    title: String,
    author: String = "",
    danmaku: List<DanmakuItem> = emptyList(),
    fullscreenInfo: FullscreenPlayerInfo = FullscreenPlayerInfo(
        title = title,
        author = author,
        authorFace = "",
        onlineText = "",
        onlineCount = 0L,
        liked = false,
        likeCount = 0L,
        replyCount = 0L,
        favorited = false,
        favoriteCount = 0L,
        shareCount = 0L,
        qualityLabel = quality?.label ?: "",
        cover = cover,
        pages = emptyList(),
        currentCid = 0L,
        folders = emptyList(),
        folderIds = emptySet(),
        loggedIn = false,
        subtitles = emptyList(),
        selectedSubtitle = null,
        subtitleCues = emptyList(),
    ),
    onLikeClick: () -> Unit = {},
    onDislikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onOpenSpaceClick: () -> Unit = {},
    onToggleFolder: (Long) -> Unit = {},
    onRequestFavorites: () -> Unit = {},
    onPageSelected: (com.aritxonly.bdfun.data.VideoPage) -> Unit = {},
    onSubtitleSelected: (com.aritxonly.bdfun.data.SubtitleTrack?) -> Unit = {},
    onAddWatchLater: () -> Unit = {},
    onToggleFullscreen: () -> Unit,
    onBackClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    resumePositionMs: Long = 0L,
    onQualitySelected: (Int) -> Unit = {},
    onCacheVideo: (Quality) -> Unit = {},
    immersiveStatusBar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val exo = remember {
        buildPlayer(context, runCatching { AppGraph.settings.decoderMode }.getOrDefault(0))
    }
    var resumeOnReturn by remember { mutableStateOf(false) }
    val latestBvid by rememberUpdatedState(bvid)
    DisposableEffect(exo) {
        val pauseHook: () -> Unit = {
            // 相关推荐/个人主页跳转前会同步 pauseAll；记住原本正在播，
            // 回到旧页面（ON_RESUME）时自动续播，而不是停在暂停态。
            resumeOnReturn = exo.playWhenReady &&
                exo.playbackState != androidx.media3.common.Player.STATE_IDLE
            exo.pause()
        }
        VideoPageBus.registerPause(pauseHook)
        onDispose {
            VideoPageBus.unregisterPause(pauseHook)
        }
    }
    var playing by remember { mutableStateOf(false) }
    var loudnessEnhancer by remember { mutableStateOf<android.media.audiofx.LoudnessEnhancer?>(null) }

    // 关闭「后台播放」后，App 退到后台立即暂停
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (!AppGraph.settings.backgroundPlay) {
                        exo.playWhenReady = false
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (resumeOnReturn) {
                        resumeOnReturn = false
                        exo.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(playing) { SmartRefresh.playing = playing }
    DisposableEffect(Unit) {
        onDispose { SmartRefresh.playing = false }
    }
    LaunchedEffect(fullscreen) { SmartRefresh.fullscreen = fullscreen }
    DisposableEffect(Unit) {
        onDispose { SmartRefresh.fullscreen = false }
    }
    var buffering by remember { mutableStateOf(true) }
    var ready by remember { mutableStateOf(false) }
    var showBuffering by remember { mutableStateOf(false) }
    LaunchedEffect(buffering) {
        if (buffering) {
            showBuffering = false
            delay(if (ready) 1500L else 2000L)
            showBuffering = true
        } else {
            showBuffering = false
        }
    }
    var ended by remember { mutableStateOf(false) }
    // 部分机型/系统解码器对 DASH 高画质支持不好；播放失败时自动退回 playurl 的 durl 兜底流。
    var usingFallback by remember { mutableStateOf(false) }
    var usingOriginal by remember { mutableStateOf(false) }
    LaunchedEffect(info, quality) {
        usingFallback = false
        usingOriginal = false
        com.aritxonly.bdfun.data.LogCollector.record(
            "PLAYER",
            "quality-change bvid=$bvid q=${quality?.id} hasInfo=${info != null} " +
                "qualities=${info?.qualities?.size ?: 0}",
        )
    }
    LaunchedEffect(ready, AppGraph.settings.loudnessEq) {
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        if (ready && AppGraph.settings.loudnessEq && exo.audioSessionId > 0) {
            loudnessEnhancer = runCatching {
                android.media.audiofx.LoudnessEnhancer(exo.audioSessionId).apply {
                    setTargetGain(600)
                    enabled = true
                }
            }.getOrNull()
        }
    }
    DisposableEffect(Unit) {
        onDispose { loudnessEnhancer?.release() }
    }
    var lastHistoryReportAt by remember { mutableLongStateOf(0L) }
    var lastProgressSaveAt by remember { mutableLongStateOf(0L) }
    var historyReportJob by remember { mutableStateOf<Job?>(null) }
    val historyScope = rememberCoroutineScope()
    var positionMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var controlsVisible by remember { mutableStateOf(false) }
    var sponsorSegments by remember(bvid) { mutableStateOf<List<com.aritxonly.bdfun.data.SponsorSegment>>(emptyList()) }
    var dismissedSponsor by remember(bvid) { mutableStateOf<Set<String>>(emptySet()) }
    val sponsorEnabled = AppGraph.settings.sponsorSkipEnabled
    LaunchedEffect(bvid, sponsorEnabled) {
        if (!sponsorEnabled) {
            sponsorSegments = emptyList()
            return@LaunchedEffect
        }
        sponsorSegments = runCatching {
            AppGraph.api.sponsorSegments(bvid)
        }.getOrDefault(emptyList())
    }
    var speed by remember { mutableFloatStateOf(1f) }
    var speedMenu by remember { mutableStateOf(false) }
    val speedSpring = rememberSpringFloat(if (speedMenu) 1f else 0f, stiffness = 300f, damping = 22f)
    var qualityMenu by remember { mutableStateOf(false) }
    val qualitySpring =
        rememberSpringFloat(if (qualityMenu) 1f else 0f, stiffness = 300f, damping = 22f)
    // 全屏新 UI 的本地状态：二级面板、听视频、镜像、循环、画面尺寸、定时关闭等
    var fsPanel by remember { mutableStateOf(FullscreenPanel.None) }
    var fsInputOpen by remember { mutableStateOf(false) }
    var fsLocked by remember { mutableStateOf(false) }
    var fsLockHint by remember { mutableStateOf(false) }
    LaunchedEffect(fsLockHint) {
        if (fsLockHint) {
            kotlinx.coroutines.delay(3_000L)
            fsLockHint = false
        }
    }
    LaunchedEffect(fsLocked, fullscreen) {
        VideoOrientationLock.locked = fsLocked && fullscreen
        if (fsLocked && fullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else if (fullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
    DisposableEffect(Unit) {
        onDispose { VideoOrientationLock.locked = false }
    }
    var fsAudioOnly by remember { mutableStateOf(false) }
    var halfMoreOpen by remember { mutableStateOf(false) }
    var fsAutoPlayNext by remember { mutableStateOf(true) }
    var fsLoopMode by remember { mutableIntStateOf(0) }
    var fsMirror by remember { mutableStateOf(false) }
    var fsResizeMode by remember { mutableIntStateOf(0) }
    var fsColorAssist by remember { mutableIntStateOf(0) }
    var fsSubtitlesOn by remember { mutableStateOf(true) }
    var fsSleepAt by remember { mutableLongStateOf(0L) }
    var fsSeekLook by remember { mutableFloatStateOf(0f) }
    var fsScrubbing by remember { mutableStateOf(false) }
    var fsBarPreviewMs by remember { mutableLongStateOf(-1L) }
    var boosting by remember { mutableStateOf(false) }
    var boostSpeed by remember { mutableFloatStateOf(2.5f) }
    var videoScale by remember { mutableFloatStateOf(1f) }
    var videoRotation by remember { mutableFloatStateOf(0f) }
    var videoOffset by remember { mutableStateOf(Offset.Zero) }
    val zoomScope = rememberCoroutineScope()
    LaunchedEffect(bvid, fullscreenInfo.currentCid) {
        videoScale = 1f
        videoRotation = 0f
        videoOffset = Offset.Zero
    }
    var frameMs by remember { mutableLongStateOf(0L) }
    var firstFrame by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volumeTip by remember { mutableStateOf<Float?>(null) }
    var brightnessTip by remember { mutableStateOf<Float?>(null) }
    var dragBase by remember { mutableFloatStateOf(0.5f) }
    // 横向拖动进度（比进度条慢，一次只能拖一点）
    var seekPreviewMs by remember { mutableLongStateOf(-1L) }
    var seekBaseMs by remember { mutableLongStateOf(0L) }
    // 拖进度条时的小窗画面预览
    val btConnected = rememberBluetoothAudioConnected()
    val btLatencyMs = if (AppGraph.settings.bluetoothLatencyEnabled && btConnected) {
        AppGraph.settings.bluetoothLatencyMs
    } else {
        0
    }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var speedBackdrop by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val fsHasNext = run {
        val pages = fullscreenInfo.pages
        val index = pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
        pages.size > 1 && index >= 0 && index + 1 < pages.size
    }

    LaunchedEffect(fsResizeMode) {
        playerViewRef?.resizeMode = when (fsResizeMode) {
            1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

    // 播完：单集循环 / 列表循环 / 自动连播（有下一集才切）
    LaunchedEffect(ended, fsLoopMode, fsAutoPlayNext, fsHasNext) {
        if (!ended) return@LaunchedEffect
        when {
            fsLoopMode == 1 -> {
                exo.seekTo(0L)
                exo.play()
                ended = false
            }
            fsHasNext && (fsLoopMode == 2 || fsAutoPlayNext) -> {
                onPageSelected(
                    fullscreenInfo.pages[
                        (fullscreenInfo.pages.indexOfFirst { it.cid == fullscreenInfo.currentCid } + 1)
                            .coerceAtMost(fullscreenInfo.pages.lastIndex)
                    ],
                )
            }
        }
    }

    // 定时关闭
    LaunchedEffect(fsSleepAt) {
        if (fsSleepAt <= 0L) return@LaunchedEffect
        while (true) {
            val left = fsSleepAt - System.currentTimeMillis()
            if (left <= 0L) {
                exo.pause()
                fsSleepAt = 0L
                context.showToast("定时关闭已生效")
                break
            }
            delay(1000L)
        }
    }

    fun grabVideoFrame(): androidx.compose.ui.graphics.ImageBitmap? {
        val tv = playerViewRef?.videoSurfaceView as? android.view.TextureView ?: return null
        if (tv.width <= 0 || tv.height <= 0) return null
        val w = (tv.width / 3).coerceAtLeast(1)
        val h = (tv.height / 3).coerceAtLeast(1)
        val bmp = runCatching { tv.getBitmap(w, h) }.getOrNull() ?: return null
        return runCatching { bmp.asImageBitmap() }.getOrNull()
    }

    // 官方 videoshot 精灵图（首帧预览取不到时用它）
    var videoShot by remember { mutableStateOf<com.aritxonly.bdfun.data.VideoShot?>(null) }
    LaunchedEffect(VideoPageBus.cid) {
        val cid = VideoPageBus.cid
        if (cid > 0) {
            videoShot = runCatching { AppGraph.api.videoShot(cid) }.getOrNull()
        }
    }

    var thumbFrame by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var thumbFrameMs by remember { mutableLongStateOf(0L) }
    var thumbFrameRatio by remember { mutableFloatStateOf(0f) }
    var thumbFrameJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var thumbFrameAt by remember { mutableLongStateOf(0L) }
    val frameLoader = remember { VideoFrameLoader() }
    val frameScope = rememberCoroutineScope()
    val frameUrl = quality?.videoUrl ?: info?.fallbackUrl
    val frameHeaders = remember(bvid) {
        mapOf(
            "User-Agent" to BiliApi.UA,
            "Referer" to "https://www.bilibili.com/video/$bvid/",
        )
    }

    DisposableEffect(Unit) {
        onDispose { frameLoader.release() }
    }

    fun touch() {
        interactionTick++
    }

    var mediaSession by remember { mutableStateOf<MediaPlaybackSession?>(null) }
    val latestPages by rememberUpdatedState(fullscreenInfo.pages)
    DisposableEffect(title, author) {
        val session = MediaPlaybackSession(
            context = context,
            exo = exo,
            title = title,
            artist = author,
            artworkUrl = cover,
            onQueueSelected = { index ->
                latestPages.getOrNull(index)?.let(onPageSelected)
            },
        )
        mediaSession = session
        onDispose {
            session.release()
            if (mediaSession === session) mediaSession = null
        }
    }

    // 系统媒体控件的进度条/选集：队列和当前索引随分 P 切换更新。
    LaunchedEffect(fullscreenInfo.pages, fullscreenInfo.currentCid, mediaSession) {
        val pages = fullscreenInfo.pages
        if (pages.isEmpty()) return@LaunchedEffect
        val entries = pages.mapIndexed { index, page ->
            MediaQueueEntry(
                mediaId = page.cid.toString(),
                title = page.part.ifBlank { "第 ${index + 1} 集" },
                subtitle = formatDuration(page.duration),
            )
        }
        val index = pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
        mediaSession?.updateQueue(entries, index)
    }

    DisposableEffect(playing) {
        val act = context as? Activity
        if (playing) {
            act?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            act?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(controlsVisible) { PlayerUiState.controlsVisible = controlsVisible }
    DisposableEffect(Unit) {
        onDispose { PlayerUiState.controlsVisible = false }
    }

    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            PlayerUiState.danmakuPanelOpen = false
            speedMenu = false
            fsPanel = FullscreenPanel.None
            fsInputOpen = false
        }
    }

    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            controlsVisible = true
            PlayerUiState.controlsVisible = true
        } else {
            fsPanel = FullscreenPanel.None
            fsInputOpen = false
            fsAudioOnly = false
        }
    }

    LaunchedEffect(fsAudioOnly) {
        if (fsAudioOnly) {
            controlsVisible = true
            PlayerUiState.controlsVisible = true
        }
        // 听视频：真正关掉视频轨，只保留音频解码，不是拿一张图盖住画面
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, fsAudioOnly)
            .build()
    }

    LaunchedEffect(
        controlsVisible,
        interactionTick,
        PlayerUiState.danmakuPanelOpen,
        PlayerUiState.danmakuInteract,
        boosting,
        fsPanel,
        fsInputOpen,
        fsAudioOnly,
        fsScrubbing,
    ) {
        if (controlsVisible && !PlayerUiState.danmakuPanelOpen && !boosting &&
            fsPanel == FullscreenPanel.None && !fsInputOpen && !fsAudioOnly &&
            !fsScrubbing
        ) {
            delay(HIDE_DELAY_MS)
            controlsVisible = false
            PlayerUiState.controlsVisible = false
        }
    }

    val latestInfo by rememberUpdatedState(info)
    val latestQuality by rememberUpdatedState(quality)
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrame = true
                com.aritxonly.bdfun.data.LogCollector.record(
                    "PLAYER",
                    "first-frame bvid=$bvid q=${latestQuality?.id}",
                )
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
                if (isPlaying) ended = false
                onPlayStateChanged(isPlaying)
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_BUFFERING || state == Player.STATE_READY) {
                    com.aritxonly.bdfun.data.LogCollector.record(
                        "PLAYER",
                        "state=${if (state == Player.STATE_BUFFERING) "buffering" else "ready"} " +
                            "bvid=$bvid q=${latestQuality?.id}",
                    )
                }
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    ready = true
                    ended = false
                    durationMs = exo.duration.coerceAtLeast(0L)
                }
                if (state == Player.STATE_ENDED) {
                    ended = true
                    controlsVisible = true
                    PlayerUiState.controlsVisible = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                com.aritxonly.bdfun.data.LogCollector.record(
                    "PLAYER",
                    "error code=${error.errorCodeName} q=${latestQuality?.id} " +
                        "usingOriginal=$usingOriginal usingFallback=$usingFallback " +
                        "message=${error.message?.take(300)}",
                )
                // CDN 节点有时会拒绝带签名的 4K/音轨地址（403），先用原地址重试，再考虑降画质。
                val rawVideo = latestQuality?.rawVideoUrl.orEmpty()
                val rawAudio = latestInfo?.rawAudioUrl.orEmpty()
                if (!usingOriginal && (rawVideo.isNotBlank() || rawAudio.isNotBlank())) {
                    usingOriginal = true
                    return
                }
                // DASH 高画质在部分 HyperOS / 解码器组合上会失败；优先退回 durl 兜底流。
                if (!usingFallback && !latestInfo?.fallbackUrl.isNullOrBlank()) {
                    usingFallback = true
                    context.showToast("当前清晰度播放失败，已切换到兼容画质")
                    return
                }
                context.showToast("播放失败，请尝试切换清晰度")
            }
        }
        exo.addListener(listener)
        onDispose {
            if (AppGraph.settings.resumePlayback) {
                runCatching {
                    PlaybackProgressStore.save(latestBvid, exo.currentPosition, exo.duration)
                }
            }
            exo.removeListener(listener)
            exo.release()
        }
    }

    LaunchedEffect(info, quality, usingFallback, usingOriginal) {
        val url = when {
            usingFallback -> info?.fallbackUrl
            usingOriginal -> quality?.rawVideoUrl?.takeIf { it.isNotBlank() }
                ?: info?.rawFallbackUrl?.takeIf { it.isNotBlank() }
                ?: quality?.videoUrl
                ?: info?.fallbackUrl
            else -> quality?.videoUrl ?: info?.fallbackUrl
        } ?: run {
            com.aritxonly.bdfun.data.LogCollector.record(
                "PLAYER",
                "no-source bvid=$bvid q=${quality?.id} hasInfo=${info != null} " +
                    "qualities=${info?.qualities?.size ?: 0} " +
                    "usingOriginal=$usingOriginal usingFallback=$usingFallback",
            )
            if (info != null && info.qualities.isEmpty() && info.fallbackUrl.isNullOrBlank()) {
                context.showToast("播放地址为空，已记录日志")
            }
            return@LaunchedEffect
        }
        val audioUrl = when {
            usingFallback -> null
            usingOriginal -> info?.rawAudioUrl?.takeIf { it.isNotBlank() } ?: info?.audioUrl
            else -> info?.audioUrl
        }
        com.aritxonly.bdfun.data.LogCollector.record(
            "PLAYER",
            "source bvid=$bvid q=${quality?.id} auto=$autoQuality " +
                "usingOriginal=$usingOriginal usingFallback=$usingFallback " +
                "video=${describeMediaUrl(url)} audio=${describeMediaUrl(audioUrl)}",
        )
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(BiliApi.UA)
            .setDefaultRequestProperties(
                mapOf("Referer" to "https://www.bilibili.com/video/$bvid/"),
            )
        // DefaultDataSource 同时处理 http（走上面的 Referer/UA）和本地缓存文件（file://）。
        val dataSource = DefaultDataSource.Factory(context, httpFactory)
        val videoSource: MediaSource = ProgressiveMediaSource.Factory(dataSource)
            .createMediaSource(MediaItem.fromUri(url))
        val source = if (audioUrl.isNullOrBlank()) {
            videoSource
        } else {
            MergingMediaSource(
                videoSource,
                ProgressiveMediaSource.Factory(dataSource)
                    .createMediaSource(MediaItem.fromUri(audioUrl)),
            )
        }
        exo.setMediaSource(source)
        exo.prepare()
        if (resumePositionMs > 0L) exo.seekTo(resumePositionMs)
        // 设置里可以关掉“进入视频自动播放”，关闭时停在首帧等用户点播。
        exo.playWhenReady = AppGraph.settings.autoPlay
        exo.setPlaybackSpeed(speed)
    }

    LaunchedEffect(playing, ready) {
        while (ready) {
            positionMs = exo.currentPosition
            bufferedMs = exo.bufferedPosition.coerceAtLeast(positionMs)
            durationMs = exo.duration.coerceAtLeast(0L)
            VideoPageBus.danmakuProgressMs = positionMs
            // 上报官方历史记录：首次 3 秒后，之后每 15 秒一次；播放结束时补一次
            val now = System.currentTimeMillis()
            if (AppGraph.settings.resumePlayback && now - lastProgressSaveAt >= 10_000L) {
                lastProgressSaveAt = now
                PlaybackProgressStore.save(bvid, positionMs, durationMs)
            }
            val shouldReport = (playing || ended) && positionMs >= 3_000L &&
                (now - lastHistoryReportAt >= 15_000L || (ended && now - lastHistoryReportAt >= 3_000L))
            if (shouldReport && historyReportJob?.isActive != true) {
                lastHistoryReportAt = now
                // 历史上报走独立协程：网络慢时不能卡住进度条和媒体通知的 300ms 刷新。
                historyReportJob = historyScope.launch {
                    runCatching {
                        AppGraph.api.reportHistory(
                            aid = VideoPageBus.aid,
                            cid = VideoPageBus.cid,
                            progressSec = positionMs / 1000,
                        )
                    }
                }
            }
            mediaSession?.onPlayerState(
                playing = playing,
                buffering = buffering,
                positionMs = positionMs,
                durationMs = durationMs,
                speed = speed,
            )
            delay(if (playing) 300 else 700)
        }
    }

    val danmakuVisible = !fsAudioOnly && DanmakuSettings.enabled && danmaku.isNotEmpty()

    // 弹幕计数：按相同文本的出现次数统计，每秒挑最近 15 秒里重复最多的两条。
    // 只在列表/开关变化时重算计数，循环本身 1 秒一次，不参与每帧绘制。
    val danmakuTextCounts = remember(danmaku) {
        if (danmaku.isEmpty()) emptyMap() else danmaku.groupingBy { it.text }.eachCount()
    }
    var hotDanmaku by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    LaunchedEffect(danmaku, danmakuVisible) {
        if (!danmakuVisible) {
            hotDanmaku = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            val now = positionMs
            val from = (now - 15_000L).coerceAtLeast(0L)
            hotDanmaku = danmaku.asSequence()
                .filter { it.timeMs in from..now }
                .mapNotNull { item ->
                    val count = danmakuTextCounts[item.text] ?: 0
                    if (count >= 3) item.text to count else null
                }
                .distinctBy { it.first }
                .sortedByDescending { it.second }
                .take(2)
                .toList()
            kotlinx.coroutines.delay(1_000L)
        }
    }

    val activeSponsor = sponsorSegments.firstOrNull { segment ->
        segment.uuid !in dismissedSponsor &&
            positionMs >= segment.startMs &&
            positionMs < segment.endMs
    }
    LaunchedEffect(playing, danmakuVisible) {
        if (!playing || !danmakuVisible) return@LaunchedEffect
        var lastEmitNs = 0L
        val frameIntervalNs = 16_666_667L // 弹幕最多 60 帧，降低 120Hz 下的无效重绘
        while (true) {
            withFrameNanos { frameTimeNs ->
                if (lastEmitNs == 0L) {
                    frameMs = exo.currentPosition
                    lastEmitNs = frameTimeNs
                } else if (frameTimeNs - lastEmitNs >= frameIntervalNs) {
                    val deltaMs = (frameTimeNs - lastEmitNs) / 1_000_000.0
                    // 用界面上的目标倍速，而不是等播放器内部慢慢生效的倍速，
                    // 这样长按加速/松开恢复时弹幕会立刻跟着变，不会晚半拍。
                    val rate = if (boosting) boostSpeed else speed
                    val predicted = frameMs + (deltaMs * rate).toLong()
                    val actual = exo.currentPosition
                    val error = actual - predicted
                    frameMs = if (abs(error) > 2_500L) {
                        // 拖动进度条等大跳变：直接回同步。
                        actual
                    } else {
                        // 小偏差每次最多修正 4ms，避免画面一卡一卡。
                        predicted + error.coerceIn(-4L, 4L)
                    }
                    lastEmitNs = frameTimeNs
                }
            }
        }
    }

    DisposableEffect(fullscreen, immersiveStatusBar) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (fullscreen) {
            controller?.apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            if (fullscreen) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // HDR / 杜比视界：把屏幕亮度拉到最高，请求 HDR 颜色模式与 10bit 输出。
    // 部分机型不会因为 Surface 是 HDR 就自动提亮，这里显式做一次。
    val isHdrQuality = quality?.let { q ->
        q.id == 125 || q.id == 126 ||
            q.codec.contains("hdr", ignoreCase = true) ||
            q.codec.contains("dovi", ignoreCase = true) ||
            q.codec.contains("dvh", ignoreCase = true) ||
            q.codec.contains("dolby", ignoreCase = true)
    } == true
    DisposableEffect(isHdrQuality, fullscreen, activity) {
        val window = activity?.window
        val hdrActive = isHdrQuality
        val previousBrightness = window?.attributes?.screenBrightness ?: -1f
        val previousColorMode = window?.attributes?.colorMode ?: 0
        if (hdrActive && window != null) {
            runCatching {
                window.attributes = window.attributes.apply {
                    screenBrightness =
                        android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                    colorMode = android.content.pm.ActivityInfo.COLOR_MODE_HDR
                }
                window.setFormat(android.graphics.PixelFormat.RGBA_1010102)
            }
        }
        onDispose {
            if (hdrActive && window != null) {
                // 用户如果自己调过亮度，就别强行拽回旧值。
                if (window.attributes.screenBrightness ==
                    android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                ) {
                    window.attributes = window.attributes.apply {
                        screenBrightness = previousBrightness
                        colorMode = previousColorMode
                    }
                }
            }
        }
    }

    val gestureDensity = LocalDensity.current
    val controlTopPx = with(gestureDensity) {
        (if (fullscreen) 64.dp else 32.dp).toPx()
    }
    val controlBottomPx = with(gestureDensity) {
        (if (fullscreen) 118.dp else 42.dp).toPx()
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .then(
                if (fsPanel == FullscreenPanel.None && !fsInputOpen && !fsAudioOnly && !fsLocked) {
                    Modifier.playerGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            touch()
                        },
                        onDoubleTap = {
                            togglePlay(exo, playing)
                            touch()
                        },
                        onLongPressStart = {
                            if (playing) {
                                boostSpeed = AppGraph.settings.boostDefaultSpeed
                                // 先让播放器收到倍速，再触发界面状态，减少“先震动、后加速”的感觉。
                                exo.setPlaybackSpeed(boostSpeed)
                                boosting = true
                                // 触发加速的瞬间震一下，确认手势已生效
                                buzzDevice(context)
                                // 面板/控件开着时直接收起，把画面让出来
                                controlsVisible = false
                                PlayerUiState.controlsVisible = false
                            }
                        },
                        onLongPressEnd = {
                            exo.setPlaybackSpeed(speed)
                            boosting = false
                        },
                        onDragStart = { leftSide, horizontal, verticalAllowed ->
                            if (horizontal) {
                                fsSeekLook = 0f
                                fsScrubbing = true
                                seekBaseMs = exo.currentPosition
                                seekPreviewMs = seekBaseMs
                                controlsVisible = true
                                PlayerUiState.controlsVisible = true
                                touch()
                            } else if (verticalAllowed) {
                                dragBase = if (leftSide) {
                                    currentBrightness(activity)
                                } else {
                                    currentVolumeRatio(audio)
                                }
                                if (leftSide) {
                                    brightnessTip = dragBase
                                } else {
                                    volumeTip = dragBase
                                }
                            }
                        },
                        onDrag = { leftSide, totalDy ->
                            val next = (dragBase - totalDy / 1200f).coerceIn(0f, 1f)
                            if (leftSide) {
                                applyBrightness(activity, next)
                                brightnessTip = next
                            } else {
                                setVolumeRatio(audio, next)
                                volumeTip = next
                            }
                        },
                        onHorizontalDrag = { fraction ->
                            fsSeekLook = (fraction / 0.04f).coerceIn(-1f, 1f)
                            val duration = exo.duration.coerceAtLeast(0L)
                            if (duration > 0) {
                                val delta = fraction * duration * 0.2f
                                seekPreviewMs = (seekBaseMs + delta.toLong())
                                    .coerceIn(0L, duration)
                            }
                        },
                        onDragEnd = {
                            fsSeekLook = 0f
                            if (fsScrubbing) {
                                fsScrubbing = false
                                controlsVisible = false
                                PlayerUiState.controlsVisible = false
                            }
                            brightnessTip = null
                            volumeTip = null
                            if (seekPreviewMs >= 0L) {
                                exo.seekTo(seekPreviewMs)
                                positionMs = seekPreviewMs
                                seekPreviewMs = -1L
                            }
                        },
                        // 控件可见时，顶部标题栏和底部控制条交给控件自己处理，
                        // 中间视频区域仍然可以单击/双击/拖动。
                        ignoreDown = { position, height ->
                            controlsVisible &&
                                (position.y < controlTopPx ||
                                    position.y > height - controlBottomPx)
                        },
                        centerSwipeEnabled = AppGraph.settings.gestureCenterFullscreen,
                        onCenterSwipe = { onToggleFullscreen() },
                        boostSwipeEnabled = AppGraph.settings.gestureBoostSwipe,
                        onBoostSwipe = { fraction ->
                            val base = AppGraph.settings.boostDefaultSpeed
                            val next = (base + fraction * 6f).coerceIn(1f, 5f)
                            boostSpeed = next
                            exo.setPlaybackSpeed(next)
                        },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = (if (fsMirror) -1f else 1f) * videoScale
                    scaleY = videoScale
                    rotationZ = videoRotation
                    translationX = videoOffset.x
                    translationY = videoOffset.y
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.count { it.pressed } >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val rotation = event.calculateRotation()
                                videoScale = (videoScale * zoom).coerceIn(0.4f, 3f)
                                videoRotation += rotation
                                videoOffset += pan
                                event.changes.forEach { it.consume() }
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                    when (fsColorAssist) {
                        1 -> drawRect(Color.Gray, blendMode = BlendMode.Saturation)
                        2 -> drawRect(Color(0x3300A8FF), blendMode = BlendMode.Color)
                    }
                },
        ) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.teak_player_view, null)
                        as PlayerView).apply {
                        player = exo
                        useController = false
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        playerViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (danmakuVisible) {
                DanmakuLayer(
                    items = danmaku,
                    positionMs = { frameMs - btLatencyMs },
                    playing = playing,
                    playbackSpeed = if (boosting) boostSpeed else speed,
                    modifier = Modifier.fillMaxSize(),
                    fullscreen = fullscreen,
                )
            }

            // 进度条常显：控件整体隐藏时，在画面最底部留一条 1dp 的粉色进度。
            val progressBarMode = AppGraph.settings.progressBarMode
            val alwaysProgress = !controlsVisible && !fsAudioOnly &&
                (progressBarMode == 3 ||
                    (progressBarMode == 1 && !fullscreen) ||
                    (progressBarMode == 2 && fullscreen))
            if (alwaysProgress && durationMs > 0L) {
                val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(fraction)
                        .height(2.dp)
                        .background(BrandPink),
                )
            }

            // 弹幕计数：最近 15 秒里重复次数最多的两条，附带 ×次数。
            if (AppGraph.settings.hotDanmakuEnabled &&
                hotDanmaku.isNotEmpty() && !controlsVisible && !fsAudioOnly
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (fullscreen) 44.dp else 8.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    hotDanmaku.forEach { (text, count) ->
                        Row(
                            modifier = Modifier
                                .clip(Capsule())
                                .background(Color.Black.copy(alpha = 0.50f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = text,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "×$count",
                                color = BrandPink,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            // 空降助手：进入赞助片段时在右下角出现跳过按钮。
            if (sponsorEnabled) activeSponsor?.let { segment ->
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 16.dp,
                            bottom = if (fullscreen) 104.dp else 62.dp,
                        )
                        .clip(Capsule())
                        .background(Color.Black.copy(alpha = 0.50f))
                        .clickable(interactionSource = null, indication = null) {
                            exo.seekTo(segment.endMs)
                            positionMs = segment.endMs
                            dismissedSponsor = dismissedSponsor + segment.uuid
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "空降助手 · ${sponsorCategoryLabel(segment.category)}",
                        color = Color.White,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "跳过",
                        color = BrandPink,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // 首次进入不显示封面，直接全速起播；超过 1.5s 仍缓冲才显示小电视

        // 播放中加载：粉色矢量小电视 + 三点省略号 + 正在缓冲...
        if (showBuffering) {
            TvBuffering(
                modifier = Modifier.align(Alignment.Center),
                tvSize = 26.4.dp,
            )
        }

        // 播放结束：居中显示「播放完成」
        if (ended) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(Capsule())
                    .background(Color(0x8C000000))
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    text = "播放完成",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        if (boosting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(if (fullscreen) Modifier.statusBarsPadding() else Modifier)
                    .padding(top = 12.dp)
                    .clip(Capsule())
                    .background(Color.Black.copy(alpha = 0.50f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BoostTriangles()
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format(
                            java.util.Locale.US,
                            "%.1f× 倍速播放中",
                            boostSpeed,
                        ),
                        color = Color.White,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        brightnessTip?.let { value ->
            GestureIconTip(
                brightness = true,
                value = value,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        volumeTip?.let { value ->
            GestureIconTip(
                brightness = false,
                value = value,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (seekPreviewMs >= 0L) {
            GestureTip(
                label = "${formatPlayTime(seekPreviewMs)} / ${formatPlayTime(durationMs)}",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
            )
        }

        // 呼出/收起必须立即响应，不做任何过渡动画
        val controlsAlpha = if (controlsVisible) 1f else 0f
        // 控件隐藏时画质/倍速面板一起收回（之前画质面板会一直挂着）
        LaunchedEffect(controlsVisible) {
            if (!controlsVisible) {
                qualityMenu = false
                speedMenu = false
            }
        }

        BackHandler(enabled = fullscreen && fsLocked) {
            // 锁屏时返回键只解锁，不退出全屏。
            fsLocked = false
        }

        BackHandler(enabled = fullscreen && (fsPanel != FullscreenPanel.None || fsInputOpen)) {
            if (fsInputOpen) {
                fsInputOpen = false
            } else {
                fsPanel = FullscreenPanel.None
            }
        }

        val fsActions = FullscreenActions(
            onBack = {
                if (fsPanel != FullscreenPanel.None) {
                    fsPanel = FullscreenPanel.None
                } else {
                    onToggleFullscreen()
                }
            },
            onPlayPause = {
                touch()
                togglePlay(exo, playing)
            },
            onNext = {
                touch()
                val pages = fullscreenInfo.pages
                val index = pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
                val next = pages.getOrNull(index + 1)
                if (next == null) {
                    context.showToast(
                        if (pages.size <= 1) "下一集不存在" else "已经是最后一集",
                    )
                } else {
                    onPageSelected(next)
                }
            },
            onEpisodes = {
                touch()
                if (fullscreenInfo.pages.size <= 1) {
                    context.showToast("无合集")
                } else {
                    fsPanel = FullscreenPanel.Episodes
                }
            },
            onLike = {
                touch()
                onLikeClick()
            },
            onComment = {
                touch()
                onCommentClick()
            },
            onShare = {
                touch()
                onShareClick()
            },
            onOpenSpace = {
                touch()
                onOpenSpaceClick()
            },
            onToggleFolder = onToggleFolder,
            onRequestFavorites = onRequestFavorites,
            onSeek = { target ->
                touch()
                exo.seekTo(target)
                positionMs = target
            },
            onQualitySelected = { id ->
                touch()
                onQualitySelected(id)
            },
            onOpenCachePanel = {
                touch()
                fsPanel = FullscreenPanel.Cache
            },
            onCacheSelected = { quality ->
                touch()
                onCacheVideo(quality)
            },
            onSpeedSelected = { value ->
                touch()
                speed = value
                exo.setPlaybackSpeed(value)
            },
            onPageSelected = { page ->
                touch()
                onPageSelected(page)
            },
            onSubtitleSelected = onSubtitleSelected,
            onSendDanmaku = {
                touch()
                fsInputOpen = true
            },
            onToggleDanmaku = {
                touch()
                DanmakuSettings.enabled = !DanmakuSettings.enabled
                VideoPageBus.danmakuOn = DanmakuSettings.enabled
            },
            onAddWatchLater = {
                touch()
                onAddWatchLater()
            },
            onPictureInPicture = {
                runCatching {
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        activity?.enterPictureInPictureMode(
                            PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .build(),
                        )
                    }
                }
            },
            onCast = {
                context.showToast("当前没有可用的投屏设备")
            },
            onAudioOnlyChanged = { enabled ->
                fsAudioOnly = enabled
                if (enabled) {
                    fsPanel = FullscreenPanel.None
                    controlsVisible = true
                    PlayerUiState.controlsVisible = true
                }
            },
            onAutoPlayNextChanged = { fsAutoPlayNext = it },
            onLoopModeChanged = { fsLoopMode = it },
            onMirrorChanged = { fsMirror = it },
            onResizeModeChanged = { fsResizeMode = it },
            onColorAssistChanged = { fsColorAssist = it },
            onSleepAtChanged = { fsSleepAt = it },
            onBackgroundPlayChanged = { enabled ->
                AppGraph.settings.backgroundPlay = enabled
                context.showToast(if (enabled) "已开启后台听视频" else "已关闭后台听视频")
            },
            onSeekRelative = { delta ->
                val target = (exo.currentPosition + delta)
                    .coerceIn(0L, exo.duration.coerceAtLeast(0L))
                exo.seekTo(target)
                positionMs = target
            },
        )

        // 全屏：完整复刻官方横屏 UI（顶部状态/互动，底部控制）
        if (videoScale > 1.01f || videoScale < 0.99f || kotlin.math.abs(videoRotation) > 0.5f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (fullscreen) 94.dp else 58.dp)
                    .clip(Capsule())
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(interactionSource = null, indication = null) {
                        val fromScale = videoScale
                        val fromRotation = videoRotation
                        val fromOffset = videoOffset
                        zoomScope.launch {
                            val start = System.nanoTime()
                            while (true) {
                                val t = ((System.nanoTime() - start) / 1_000_000f / 260f)
                                    .coerceIn(0f, 1f)
                                val e = 1f - (1f - t) * (1f - t) * (1f - t)
                                videoScale = fromScale + (1f - fromScale) * e
                                videoRotation = fromRotation * (1f - e)
                                videoOffset = fromOffset * (1f - e)
                                if (t >= 1f) break
                                withFrameNanos { }
                            }
                            videoScale = 1f
                            videoRotation = 0f
                            videoOffset = Offset.Zero
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "取消缩放",
                    color = Color(0xFFFB7299),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }

        if (fullscreen) {
            FullscreenPlayerChrome(
                info = fullscreenInfo,
                playback = FullscreenPlaybackState(
                    playing = playing,
                    positionMs = if (controlsVisible) {
                        if (seekPreviewMs >= 0L) seekPreviewMs else positionMs
                    } else {
                        0L
                    },
                    durationMs = if (controlsVisible) durationMs else 0L,
                    bufferedMs = if (controlsVisible) bufferedMs else 0L,
                    speed = speed,
                    qualities = info?.qualities.orEmpty(),
                    qualityId = quality?.id ?: 0,
                    autoQuality = autoQuality,
                    danmakuEnabled = DanmakuSettings.enabled,
                    autoPlayNext = fsAutoPlayNext,
                    loopMode = fsLoopMode,
                    mirror = fsMirror,
                    resizeMode = fsResizeMode,
                    colorAssist = fsColorAssist,
                    audioOnly = fsAudioOnly,
                    sleepAtMs = fsSleepAt,
                    backgroundPlay = AppGraph.settings.backgroundPlay,
                    seekLook = if (controlsVisible) fsSeekLook else 0f,
                    scrubbing = controlsVisible && fsScrubbing,
                ),
                actions = fsActions,
                panel = fsPanel,
                onPanelChanged = { fsPanel = it },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
                    .offset(x = if (controlsVisible && !fsInputOpen) 0.dp else 10_000.dp)
                    .graphicsLayer { alpha = if (fsInputOpen) 0f else controlsAlpha },
                scrubOnly = fsScrubbing,
                onScrubStart = { fsScrubbing = true },
                onScrubEnd = {
                    fsScrubbing = false
                    fsSeekLook = 0f
                },
                onScrubLook = { fsSeekLook = it },
                onScrubPreview = { fsBarPreviewMs = it },
                onPanelHidden = {
                    controlsVisible = false
                    PlayerUiState.controlsVisible = false
                },
            )
        }

        // 全屏锁：未锁定时跟控件一起出现；锁定后点画面才短暂显示锁图标。
        if (fullscreen && fsLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(15f)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            fsLockHint = true
                        }
                    },
            )
        }
        if (fullscreen && ((fsLocked && fsLockHint) ||
                (!fsLocked && controlsVisible && fsPanel == FullscreenPanel.None && !fsInputOpen))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(50.dp)
                    .zIndex(20f)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.50f))
                    .clickable(interactionSource = null, indication = null) {
                        fsLocked = !fsLocked
                        fsLockHint = false
                        if (fsLocked) {
                            controlsVisible = false
                            PlayerUiState.controlsVisible = false
                            fsPanel = FullscreenPanel.None
                            fsInputOpen = false
                            qualityMenu = false
                            speedMenu = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                LockGlyph(locked = fsLocked)
            }
        }

        if (fullscreen && fsScrubbing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = if (fullscreen) 52.dp else 4.dp,
                        end = if (fullscreen) 42.dp else 4.dp,
                        bottom = if (fullscreen) 52.dp else 28.dp,
                    )
                    .zIndex(11f),
            ) {
                FullscreenSeekBar(
                    positionMs = when {
                        fsBarPreviewMs >= 0L -> fsBarPreviewMs
                        seekPreviewMs >= 0L -> seekPreviewMs
                        else -> positionMs
                    },
                    durationMs = durationMs,
                    bufferedMs = bufferedMs,
                    onInteract = {},
                    onSeek = {},
                    modifier = Modifier.fillMaxWidth(),
                    externalPressed = true,
                    externalLook = fsSeekLook,
                )
            }
        }

        if (false && !fullscreen && controlsAlpha > 0.005f) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
                    .graphicsLayer { alpha = controlsAlpha }
                    .then(if (!controlsVisible) Modifier.blockTouches(true) else Modifier),
            ) {
                val topGradient = if (fullscreen) TopGradientFullscreen else TopGradientInline
                val bottomGradient =
                    if (fullscreen) BottomGradientFullscreen else BottomGradientInline
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(topGradient)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.62f),
                                0.55f to Color.Black.copy(alpha = 0.28f),
                                1f to Color.Transparent,
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(bottomGradient)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.45f to Color.Black.copy(alpha = 0.38f),
                                1f to Color.Black.copy(alpha = 0.78f),
                            ),
                        ),
                )

                val edge = if (fullscreen) 44.dp else 12.dp

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 沉浸模式下状态栏是透明的，视频顶到 0，控件要让开状态栏
                        .then(
                            if (fullscreen || immersiveStatusBar) Modifier.statusBarsPadding()
                            else Modifier,
                        )
                        .padding(horizontal = edge, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (fullscreen) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(interactionSource = null, indication = null) {
                                    touch()
                                    onToggleFullscreen()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.size(22.dp)) {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(size.width * 0.62f, size.height * 0.14f)
                                    lineTo(size.width * 0.30f, size.height * 0.5f)
                                    lineTo(size.width * 0.62f, size.height * 0.86f)
                                }
                                drawPath(
                                    path = path,
                                    color = Color.White,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = size.width * 0.12f,
                                        cap = StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                    ),
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    // 弹幕设置：三条横线 + 滑块（放在后台播放左边）
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(interactionSource = null, indication = null) {
                                touch()
                                val opening = !PlayerUiState.danmakuPanelOpen
                                if (opening) {
                                    VideoPageBus.panelBackdrop = grabVideoFrame()
                                }
                                PlayerUiState.danmakuPanelOpen = opening
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        DanmakuSettingsGlyph(tint = Color.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    // 后台播放开关（耳机图标）
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(interactionSource = null, indication = null) {
                                touch()
                                AppGraph.settings.backgroundPlay =
                                    !AppGraph.settings.backgroundPlay
                                context.showToast(
                                    if (AppGraph.settings.backgroundPlay) {
                                        "已开启后台播放"
                                    } else {
                                        "已关闭后台播放"
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        BackgroundPlayGlyph(
                            tint = if (AppGraph.settings.backgroundPlay) {
                                BrandPink
                            } else {
                                Color.White
                            },
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .let { if (fullscreen) it.navigationBarsPadding() else it }
                        .padding(horizontal = edge, vertical = 6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayPauseButton(
                            playing = playing,
                            onClick = {
                                touch()
                                togglePlay(exo, playing)
                            },
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = formatPlayTime(positionMs),
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        TeakSeekBar(
                            positionMs = if (seekPreviewMs >= 0L) seekPreviewMs else positionMs,
                            durationMs = durationMs,
                            onInteract = { touch() },
                            onSeek = { target ->
                                exo.seekTo(target)
                                positionMs = target
                            },
                            previewFrame = thumbFrame,
                            previewTimeMs = thumbFrameMs,
                            previewRatio = thumbFrameRatio,
                            onPreview = preview@{ ratio, timeMs ->
                                thumbFrameRatio = ratio
                                thumbFrameMs = timeMs
                                // 正在抓帧 / 刚抓过就跳过：连续拖动时才不会把请求全取消掉
                                if (thumbFrameJob?.isActive == true) return@preview
                                val now = android.os.SystemClock.elapsedRealtime()
                                if (now - thumbFrameAt < 120L) return@preview
                                thumbFrameAt = now
                                thumbFrameJob = frameScope.launch {
                                    val shot = videoShot ?: runCatching {
                                        AppGraph.api.videoShot(VideoPageBus.cid)
                                    }.getOrNull()?.also { videoShot = it }
                                    val bitmap = if (shot != null) {
                                        VideoShotLoader.frame(shot, timeMs)
                                    } else {
                                        null
                                    } ?: frameLoader.frame(
                                        url = frameUrl,
                                        headers = frameHeaders,
                                        timeMs = timeMs,
                                    )
                                    if (bitmap != null) thumbFrame = bitmap
                                }
                            },
                            onPreviewEnd = {
                                thumbFrameJob?.cancel()
                                thumbFrame = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatPlayTime(durationMs),
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (autoQuality) "自动" else quality?.label ?: "画质",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable(interactionSource = null, indication = null) {
                                    touch()
                                    val opening = !qualityMenu
                                    if (opening) {
                                        speedMenu = false
                                        VideoPageBus.panelBackdrop = grabVideoFrame()
                                    }
                                    qualityMenu = opening
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${speed}x",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable(interactionSource = null, indication = null) {
                                    touch()
                                    val opening = !speedMenu
                                    if (opening) {
                                        qualityMenu = false
                                        speedBackdrop = grabVideoFrame()
                                    }
                                    speedMenu = opening
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(interactionSource = null, indication = null) {
                                    touch()
                                    onToggleFullscreen()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            ResourceGlyph(
                                res = TeakIcon.Fullscreen,
                                tint = Color.White,
                                size = 19.dp,
                            )
                        }
                    }

                }
                // 倍速面板：半屏时从控件行往下展开（不再往上顶到状态栏），全屏仍走原来的位置
                if (speedMenu || speedSpring > 0.01f) {
                    TeakGlassPanel(
                        modifier = Modifier
                            .align(
                                if (fullscreen) Alignment.BottomEnd else Alignment.TopEnd,
                            )
                            .then(
                                if (fullscreen) {
                                    Modifier.padding(end = edge, bottom = 56.dp)
                                } else {
                                    Modifier.padding(end = edge).offset(y = maxHeight - 64.dp)
                                },
                            )
                            .width(132.dp)
                            .graphicsLayer {
                                alpha = speedSpring.coerceIn(0f, 1f)
                                scaleX = 0.82f + 0.18f * speedSpring
                                scaleY = 0.82f + 0.18f * speedSpring
                                translationY = (1f - speedSpring) * 8f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                    1f,
                                    1f,
                                )
                            }
                            // 和弹幕设置同款：弹出过程中由模糊变清晰
                            .blur(((1f - speedSpring.coerceIn(0f, 1f)) * 10f).dp),
                        shape = RoundedCornerShape(20.dp),
                        solid = true,
                        solidColor = Color(0xFF121318),
                        backdropImage = speedBackdrop,
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                // 竖屏播放器只有半屏高，面板限高后可滑动查看全部倍速
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            SPEEDS.forEach { option ->
                                val active = option == speed
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = null,
                                            indication = null,
                                        ) {
                                            touch()
                                            speed = option
                                            exo.setPlaybackSpeed(option)
                                            speedMenu = false
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${option}x",
                                        color = if (active) BrandPink else Color.White,
                                        fontSize = 13.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontWeight = if (active) {
                                            FontWeight.Medium
                                        } else {
                                            FontWeight.Normal
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (qualityMenu || qualitySpring > 0.01f) {
                    TeakGlassPanel(
                        modifier = Modifier
                            .align(
                                if (fullscreen) Alignment.BottomEnd else Alignment.TopEnd,
                            )
                            .then(
                                if (fullscreen) {
                                    Modifier.padding(end = edge, bottom = 56.dp)
                                } else {
                                    Modifier.padding(end = edge).offset(y = maxHeight - 64.dp)
                                },
                            )
                            .width(168.dp)
                            .graphicsLayer {
                                alpha = qualitySpring.coerceIn(0f, 1f)
                                scaleX = 0.82f + 0.18f * qualitySpring
                                scaleY = 0.82f + 0.18f * qualitySpring
                                translationY = (1f - qualitySpring) * 8f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)
                            }
                            .blur(((1f - qualitySpring.coerceIn(0f, 1f)) * 10f).dp),
                        shape = RoundedCornerShape(20.dp),
                        solid = true,
                        solidColor = Color(0xFF121318),
                        backdropImage = VideoPageBus.panelBackdrop,
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = "画质",
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                            // 自动：按当前网速挑一档，播放器里也能直接切
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(interactionSource = null, indication = null) {
                                        touch()
                                        qualityMenu = false
                                        onQualitySelected(0)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "自动",
                                        color = if (autoQuality) BrandPink else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (autoQuality) {
                                            FontWeight.Medium
                                        } else {
                                            FontWeight.Normal
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (autoQuality) {
                                        Text("✓", color = BrandPink, fontSize = 14.sp)
                                    }
                                }
                            }
                            info?.qualities.orEmpty().forEach { item ->
                                val active = !autoQuality && item.id == quality?.id
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(interactionSource = null, indication = null) {
                                            touch()
                                            qualityMenu = false
                                            onQualitySelected(item.id)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.label,
                                                color = if (active) BrandPink else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = if (active) {
                                                    FontWeight.Medium
                                                } else {
                                                    FontWeight.Normal
                                                },
                                            )
                                            if (item.width > 0 && item.height > 0) {
                                                Text(
                                                    text = "${item.width}×${item.height}",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 9.5.sp,
                                                )
                                            }
                                        }
                                        if (active) {
                                            Text("✓", color = BrandPink, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // 字幕层：控件自动隐藏后仍然保留当前句
        if (!fsAudioOnly && fsSubtitlesOn && fsPanel == FullscreenPanel.None &&
            !fullscreenInfo.subtitleCues.isEmpty()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SubtitleOverlay(
                    cues = fullscreenInfo.subtitleCues,
                    positionMs = positionMs,
                    bottomPadding = if (fullscreen) 116.dp else 52.dp,
                )
            }
        }

        // 半屏播放器：参考官方横屏的紧凑条，不再使用竖屏那套完整控件
        if (!fullscreen && !fsAudioOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = if (controlsVisible) 0.dp else 10_000.dp)
                    .graphicsLayer { alpha = controlsAlpha },
            ) {
                InlineCompactChrome(
                    playing = playing,
                    positionMs = if (controlsVisible) {
                        if (seekPreviewMs >= 0L) seekPreviewMs else positionMs
                    } else {
                        0L
                    },
                    durationMs = if (controlsVisible) durationMs else 0L,
                    bufferedMs = if (controlsVisible) bufferedMs else 0L,
                    scrubbing = controlsVisible && fsScrubbing,
                    seekLook = if (controlsVisible) fsSeekLook else 0f,
                    immersiveStatusBar = immersiveStatusBar,
                    onBack = { touch(); onBackClick() },
                    onHome = { touch(); onHomeClick() },
                    onRewind6 = {
                        touch()
                        val target = (exo.currentPosition - 6_000L).coerceAtLeast(0L)
                        exo.seekTo(target)
                        positionMs = target
                    },
                    onPlayPause = {
                        touch()
                        togglePlay(exo, playing)
                    },
                    onSeek = { target ->
                        touch()
                        exo.seekTo(target)
                        positionMs = target
                    },
                    onScrubStart = { fsScrubbing = true },
                    onScrubEnd = {
                        fsScrubbing = false
                        fsSeekLook = 0f
                    },
                    onScrubLook = { fsSeekLook = it },
                    onFullscreen = {
                        touch()
                        onToggleFullscreen()
                    },
                    onListen = {
                        touch()
                        fsAudioOnly = true
                    },
                    onCast = { touch(); fsActions.onCast() },
                    onMore = { touch(); halfMoreOpen = true },
                )
            }
        }

        // 半屏听视频：不进入全屏也能用，播放器区域换成音频模式。
        if (!fullscreen && fsAudioOnly) {
            HalfScreenAudioMode(
                title = fullscreenInfo.title,
                author = fullscreenInfo.author,
                cover = fullscreenInfo.cover,
                playing = playing,
                positionMs = positionMs,
                durationMs = durationMs,
                onExit = { fsAudioOnly = false },
                onPlayPause = { touch(); togglePlay(exo, playing) },
                onSeekRelative = { delta ->
                    touch()
                    val target = (positionMs + delta).coerceIn(0L, durationMs.coerceAtLeast(0L))
                    exo.seekTo(target)
                    positionMs = target
                },
            )
        }

        // 全屏弹幕输入：点输入框后从底部弹出，和竖屏共用同一套发送器
        if (fullscreen && fsInputOpen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.78f)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            fsInputOpen = false
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                                if (event.changes.none { it.pressed }) break
                            }
                        }
                    },
            )
            FullscreenDanmakuInput(
                onDismiss = { fsInputOpen = false },
                items = danmaku,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            )
        }

        // 半屏「更多」：与全屏 MorePanel 同一套设置项，复用评论区同款底部弹簧面板。
        if (!fullscreen) {
            HalfScreenMoreSheet(
                visible = halfMoreOpen,
                onDismiss = { halfMoreOpen = false },
                speed = speed,
                autoPlayNext = fsAutoPlayNext,
                loopMode = fsLoopMode,
                mirror = fsMirror,
                backgroundPlay = AppGraph.settings.backgroundPlay,
                subtitlesOn = fsSubtitlesOn,
                sleepAtMs = fsSleepAt,
                onSpeed = {
                    speed = it
                    exo.setPlaybackSpeed(it)
                },
                onDislike = {
                    onDislikeClick()
                    context.showToast("已减少此类推荐")
                },
                onWatchLater = {
                    onAddWatchLater()
                    halfMoreOpen = false
                },
                onPictureInPicture = {
                    fsActions.onPictureInPicture()
                    halfMoreOpen = false
                },
                onCast = {
                    fsActions.onCast()
                    halfMoreOpen = false
                },
                onCache = {
                    val item = quality
                    if (item == null) {
                        context.showToast("暂无可用清晰度")
                    } else {
                        onCacheVideo(item)
                    }
                    halfMoreOpen = false
                },
                onAutoPlayNext = { fsAutoPlayNext = !fsAutoPlayNext },
                onLoopMode = { fsLoopMode = it },
                onMirror = { fsMirror = !fsMirror },
                onBackgroundPlay = {
                    AppGraph.settings.backgroundPlay = !AppGraph.settings.backgroundPlay
                },
                onSubtitlesToggle = { fsSubtitlesOn = !fsSubtitlesOn },
                onDanmakuSettings = {
                    if (!PlayerUiState.danmakuPanelOpen) {
                        VideoPageBus.panelBackdrop = grabVideoFrame()
                    }
                    PlayerUiState.danmakuPanelOpen = true
                },
                onSleepAt = { target ->
                    fsSleepAt = target
                },
            )
        }
    }
}

/** 弹幕设置图标：三条横线 + 滑块。 */
@Composable
private fun InlineCompactChrome(
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    scrubbing: Boolean,
    seekLook: Float,
    immersiveStatusBar: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRewind6: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    onScrubLook: (Float) -> Unit,
    onFullscreen: () -> Unit,
    onListen: () -> Unit,
    onCast: () -> Unit,
    onMore: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.65f),
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .then(if (immersiveStatusBar) Modifier.statusBarsPadding() else Modifier)
                .graphicsLayer { alpha = if (scrubbing) 0f else 1f }
                .padding(start = 10.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InlineTopIcon(dark = true, size = 34.dp, onClick = onBack) {
                InlineBackGlyph()
            }
            InlineTopIcon(filled = true, size = 38.dp, onClick = onHome) {
                InlineHomeGlyph(tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .then(if (immersiveStatusBar) Modifier.statusBarsPadding() else Modifier)
                .graphicsLayer { alpha = if (scrubbing) 0f else 1f }
                .padding(end = 10.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            InlineTopIcon(size = 34.dp, onClick = onListen) { InlineHeadphoneGlyph() }
            InlineTopIcon(size = 34.dp, onClick = onCast) { InlineCastGlyph() }
            InlineTopIcon(filled = true, size = 38.dp, onClick = onMore) {
                InlineMoreGlyph(tint = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 10.dp, end = 12.dp, bottom = 7.dp)
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactIconButton(onClick = onPlayPause, size = 28.dp) {
                if (playing) PauseGlyph() else PlayGlyph(size = 22.dp)
            }
            Spacer(Modifier.width(7.dp))
            FullscreenSeekBar(
                positionMs = positionMs,
                durationMs = durationMs,
                bufferedMs = bufferedMs,
                onInteract = {},
                onSeek = onSeek,
                modifier = Modifier.weight(1f),
                externalPressed = scrubbing,
                externalLook = seekLook,
                onScrubStart = onScrubStart,
                onScrubEnd = onScrubEnd,
                onLookChanged = onScrubLook,
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = "${formatPlayTime(positionMs)}/${formatPlayTime(durationMs)}",
                color = Color.White,
                fontSize = 11.5.sp,
                maxLines = 1,
            )
            Spacer(Modifier.width(7.dp))
            CompactIconButton(onClick = onFullscreen, size = 29.dp) {
                ResourceGlyph(res = TeakIcon.Fullscreen, tint = Color.White, size = 17.dp)
            }
        }
    }
}

@Composable
private fun InlineTopIcon(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 34.dp,
    filled: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(if (filled) 12.dp else 50.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun CompactIconButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun BoostTriangles() {
    val transition = rememberInfiniteTransition(label = "boost-triangles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val pulse = ((phase + index * 0.5f) % 2f).let { if (it > 1f) 2f - it else it }
            val alpha = 0.25f + 0.75f * pulse
            Canvas(modifier = Modifier.size(width = 8.dp, height = 11.dp)) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, size.height / 2f)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = alpha))
            }
        }
    }
}

@Composable
private fun PlayPauseButton(playing: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (playing) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 13.dp)
                        .clip(Capsule())
                        .background(Color.White),
                )
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 13.dp)
                        .clip(Capsule())
                        .background(Color.White),
                )
            }
        } else {
            Image(
                painter = painterResource(TeakIcon.Play),
                contentDescription = "播放",
                modifier = Modifier.height(14.dp),
            )
        }
    }
}

/** 弹幕设置面板：黑底 + 白色高光描边，由 Activity 的覆盖层绘制，点外面一定收得掉。 */
@Composable
fun DanmakuSettingsPanel(
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: androidx.compose.ui.graphics.ImageBitmap? = null,
) {
    TeakGlassPanel(
        modifier = modifier.width(286.dp),
        shape = RoundedCornerShape(24.dp),
        solid = true,
        solidColor = Color(0xFF121318),
        backdropImage = backdrop,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DanmakuSettingsContent(
                    fullscreen = false,
                    onInteract = onInteract,
                )
            }
        }
    }
}

@Composable
private fun PanelToggle(
    label: String,
    checked: Boolean,
    onInteract: () -> Unit,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                onInteract()
                onChange(!checked)
            }
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Color(0xFFCFD2DA), fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(Capsule())
                .background(
                    if (checked) BrandPink else Color.White.copy(alpha = 0.16f),
                ),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (checked) 18.dp else 2.dp, y = 2.dp)
                    .size(16.dp)
                    .clip(Capsule())
                    .background(Color.White),
            )
        }
    }
}

/** 与播放器进度条完全同款：只保留轨道 + 填充，不要任何指示器。 */
@Composable
private fun PanelSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    trackColor: Color,
    onChangeStart: () -> Unit = {},
    onValueChange: (Float) -> Unit,
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val ratio = ((value - range.start) / span).coerceIn(0f, 1f)
    var pressed by remember { mutableStateOf(false) }
    val grow = rememberManualProgress(if (pressed) 1f else 0f, 150f)
    val thickness = (5f + 3f * grow).dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(range) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onChangeStart()
                    pressed = true
                    fun update(x: Float) {
                        val r = (x / size.width).coerceIn(0f, 1f)
                        onValueChange(range.start + r * span)
                    }
                    update(down.position.x)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        update(change.position.x)
                        pressed = true
                        change.consume()
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val w = maxWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thickness)
                .clip(Capsule())
                .background(trackColor),
        )
        Box(
            modifier = Modifier
                .width(w * ratio)
                .height(thickness)
                .clip(Capsule())
                .background(BrandPink),
        )
    }
}

@Composable
private fun GestureIconTip(
    brightness: Boolean,
    value: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(Capsule())
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val stroke = 1.7.dp.toPx()
            if (brightness) {
                drawCircle(
                    color = Color.White,
                    radius = size.width * 0.19f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
                )
                repeat(8) { i ->
                    val angle = Math.toRadians((i * 45).toDouble())
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r1 = size.width * 0.31f
                    val r2 = size.width * 0.44f
                    drawLine(
                        color = Color.White,
                        start = Offset(
                            cx + (r1 * cos(angle)).toFloat(),
                            cy + (r1 * sin(angle)).toFloat(),
                        ),
                        end = Offset(
                            cx + (r2 * cos(angle)).toFloat(),
                            cy + (r2 * sin(angle)).toFloat(),
                        ),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            } else {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.38f)
                    lineTo(size.width * 0.32f, size.height * 0.38f)
                    lineTo(size.width * 0.55f, size.height * 0.16f)
                    lineTo(size.width * 0.55f, size.height * 0.84f)
                    lineTo(size.width * 0.32f, size.height * 0.62f)
                    lineTo(size.width * 0.12f, size.height * 0.62f)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
                drawArc(
                    color = Color.White,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.52f, size.height * 0.3f),
                    size = Size(size.width * 0.3f, size.height * 0.4f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(Capsule())
                    .background(Color(0xFFFB7299)),
            )
        }
    }
}

@Composable
private fun GestureTip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(Capsule())
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}

private fun togglePlay(exo: ExoPlayer, playing: Boolean) {
    if (playing) exo.pause() else exo.play()
}

/**
 * 单一手势识别器：
 * - 单击延迟 260ms 生效（给双击让路），双击只暂停/播放、不切控件
 * - 竖滑超过 touchSlop → 左亮度 / 右音量
 * - 长按 380ms → 只进入高倍速，之后任何移动都不再触发其它手势
 */
private data class GestureCallbacks(
    val onTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onLongPressStart: () -> Unit,
    val onLongPressEnd: () -> Unit,
    val onDragStart: (Boolean, Boolean, Boolean) -> Unit,
    val onDrag: (Boolean, Float) -> Unit,
    val onHorizontalDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit,
    val ignoreDown: (Offset, Float) -> Boolean,
    val centerSwipeEnabled: Boolean,
    val onCenterSwipe: () -> Unit,
    val boostSwipeEnabled: Boolean,
    val onBoostSwipe: (Float) -> Unit,
)

@Composable
private fun Modifier.playerGestures(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onLongPressStart: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    onDragStart: (leftSide: Boolean, horizontal: Boolean, verticalAllowed: Boolean) -> Unit,
    onDrag: (leftSide: Boolean, totalDy: Float) -> Unit,
    onHorizontalDrag: (fraction: Float) -> Unit,
    onDragEnd: () -> Unit,
    ignoreDown: (Offset, Float) -> Boolean = { _, _ -> false },
    centerSwipeEnabled: Boolean = false,
    onCenterSwipe: () -> Unit = {},
    boostSwipeEnabled: Boolean = false,
    onBoostSwipe: (Float) -> Unit = {},
): Modifier {
    val tapState = remember { longArrayOf(0L) }
    val pendingTap = remember { arrayOfNulls<Job>(1) }
    val scope = rememberCoroutineScope()
    // pointerInput(Unit) 不会因为外部重组而重启，闭包里的 controlsVisible/playing/speed
    // 会停在创建时那一帧，表现为「控件开着时中间区域怎么点都没反应」。
    // 所有回调都通过 rememberUpdatedState 取最新值。
    val callbacks by rememberUpdatedState(
        GestureCallbacks(
            onTap = onTap,
            onDoubleTap = onDoubleTap,
            onLongPressStart = onLongPressStart,
            onLongPressEnd = onLongPressEnd,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onHorizontalDrag = onHorizontalDrag,
            onDragEnd = onDragEnd,
            ignoreDown = ignoreDown,
            centerSwipeEnabled = centerSwipeEnabled,
            onCenterSwipe = onCenterSwipe,
            boostSwipeEnabled = boostSwipeEnabled,
            onBoostSwipe = onBoostSwipe,
        ),
    )
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (callbacks.ignoreDown(down.position, size.height.toFloat())) {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.none { it.pressed }) break
                }
                return@awaitEachGesture
            }
            // 系统返回手势区域：24dp 内完全放行，避免抢系统侧滑
            val systemEdge = 24.dp.toPx()
            if (down.position.x < systemEdge ||
                down.position.x > size.width - systemEdge
            ) {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.none { it.pressed }) break
                }
                return@awaitEachGesture
            }
            val side = when {
                down.position.x < size.width * 0.28f -> true
                down.position.x > size.width * 0.72f -> false
                else -> null
            }
            val smallPlayer = size.height < 320.dp.toPx()
            val verticalAllowed = AppGraph.settings.gestureBrightnessVolume && side != null &&
                down.position.y > (if (smallPlayer) 24.dp.toPx() else 80.dp.toPx()) &&
                down.position.y < size.height - (if (smallPlayer) 40.dp.toPx() else 100.dp.toPx())
            val leftSide = side ?: (down.position.x < size.width / 2f)
            val centerZone = side == null
            var dragged = false
            var horizontal = false
            var dragVertical = false
            var centerSwipe = false
            var centerSwipeDy = 0f
            var totalDy = 0f
            var totalDx = 0f
            var previous = down
            var longPressed = false
            val downTime = System.currentTimeMillis()

            val longPressJob = scope.launch {
                delay(380)
                if (!dragged) {
                    longPressed = true
                    tapState[0] = 0
                    pendingTap[0]?.cancel()
                    callbacks.onLongPressStart()
                }
            }
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) {
                        if (!dragged && !longPressed) {
                            val held = System.currentTimeMillis() - downTime
                            if (held < 300) {
                                val now = System.currentTimeMillis()
                                if (now - tapState[0] < 260) {
                                    tapState[0] = 0
                                    pendingTap[0]?.cancel()
                                    pendingTap[0] = null
                                    callbacks.onDoubleTap()
                                } else {
                                    tapState[0] = now
                                    pendingTap[0]?.cancel()
                                    pendingTap[0] = scope.launch {
                                        delay(260)
                                        if (tapState[0] != 0L) callbacks.onTap()
                                    }
                                }
                            }
                        }
                        break
                    }
                    val dx = change.position.x - previous.position.x
                    val dy = change.position.y - previous.position.y
                    totalDx += dx
                    totalDy += dy
                    previous = change
                    if (longPressed) {
                        // 长按加速中：左右滑动调倍速，纵向移动不响应。
                        if (callbacks.boostSwipeEnabled) {
                            callbacks.onBoostSwipe(totalDx / size.width)
                        }
                        change.consume()
                        continue
                    }
                    if (!dragged &&
                        (abs(totalDy) > viewConfiguration.touchSlop ||
                            abs(totalDx) > viewConfiguration.touchSlop)
                    ) {
                        dragged = true
                        horizontal = abs(totalDx) > abs(totalDy)
                        dragVertical = !horizontal && verticalAllowed
                        centerSwipe = !horizontal && centerZone && callbacks.centerSwipeEnabled
                        longPressJob.cancel()
                        if (!centerSwipe) {
                            callbacks.onDragStart(leftSide, horizontal, verticalAllowed)
                        }
                    }
                    if (dragged) {
                        if (centerSwipe) {
                            centerSwipeDy = totalDy
                            change.consume()
                        } else if (horizontal) {
                            callbacks.onHorizontalDrag(totalDx / size.width)
                        } else if (dragVertical) {
                            callbacks.onDrag(leftSide, totalDy)
                        }
                        if (!centerSwipe && (horizontal || dragVertical)) change.consume()
                    }
                }
            } finally {
                longPressJob.cancel()
            }
            if (dragged && (horizontal || dragVertical)) callbacks.onDragEnd()
            if (centerSwipe && abs(centerSwipeDy) > 70.dp.toPx()) {
                callbacks.onCenterSwipe()
            }
            if (longPressed) callbacks.onLongPressEnd()
        }
    }
}

/** 长按加速的触感反馈；设备没有振动器时静默跳过。 */
private fun buzzDevice(context: Context) {
    runCatching {
        val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
            as? android.os.VibratorManager)?.defaultVibrator
        vibrator?.vibrate(
            android.os.VibrationEffect.createOneShot(
                12L,
                48,
            ),
        )
    }
}

private fun currentBrightness(activity: Activity?): Float {
    val value = activity?.window?.attributes?.screenBrightness ?: -1f
    return if (value < 0f) 0.5f else value
}

private fun applyBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    window.attributes = window.attributes.apply {
        screenBrightness = value.coerceIn(0.02f, 1f)
    }
}

private fun currentVolumeRatio(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun setVolumeRatio(audio: AudioManager, ratio: Float) {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    audio.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        (ratio * max).toInt().coerceIn(0, max),
        0,
    )
}

internal fun formatPlayTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0L)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/**
 * 进度条：小电视指示器（比上一版放大），按下/拖动恒为粗，抬手变细。
 */
@Composable
internal fun TeakSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    previewFrame: androidx.compose.ui.graphics.ImageBitmap? = null,
    previewTimeMs: Long = 0L,
    previewRatio: Float = 0f,
    onPreview: (ratio: Float, timeMs: Long) -> Unit = { _, _ -> },
    onPreviewEnd: () -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableFloatStateOf(-1f) }
    val ratio = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shownRatio = if (dragRatio >= 0f) dragRatio else ratio
    val grow = rememberManualProgress(if (pressed) 1f else 0f, 150f)
    val thickness = (5f + 3f * grow).dp
    val thumbHeight = (20f + 4f * grow).dp

    BoxWithConstraints(
        modifier = modifier
            .height(32.dp)
            .pointerInput(durationMs) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onInteract()
                    if (durationMs > 0) {
                        dragRatio = (down.position.x / size.width).coerceIn(0f, 1f)
                        onPreview(dragRatio, (dragRatio * durationMs).toLong())
                    }
                    var released = false
                    var lastInteract = System.currentTimeMillis()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            released = true
                            break
                        }
                        if (durationMs > 0) {
                            dragRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                            onPreview(dragRatio, (dragRatio * durationMs).toLong())
                        }
                        pressed = true
                        val now = System.currentTimeMillis()
                        if (now - lastInteract > 600) {
                            lastInteract = now
                            onInteract()
                        }
                        change.consume()
                    }
                    if (released && durationMs > 0 && dragRatio >= 0f) {
                        onSeek((dragRatio * durationMs).toLong())
                    }
                    onPreviewEnd()
                    dragRatio = -1f
                    pressed = false
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val w = maxWidth
        // 拖动时在进度条上方浮出画面预览小电视
        if (previewFrame != null) {
            val previewWidth = 148.dp
            val previewHeight = 84.dp
            val px = (w * previewRatio - previewWidth / 2f)
                .coerceIn(0.dp, (w - previewWidth).coerceAtLeast(0.dp))
            Box(
                modifier = Modifier
                    .offset(x = px, y = -(previewHeight + 14.dp))
                    .width(previewWidth)
                    .height(previewHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
            ) {
                Image(
                    bitmap = previewFrame,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = formatPlayTime(previewTimeMs),
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 3.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thickness)
                .clip(Capsule())
                .background(Color(0x59FFFFFF)),
        )
        Box(
            modifier = Modifier
                .width(w * shownRatio)
                .height(thickness)
                .clip(Capsule())
                .background(Color.White),
        )
        val thumbWidth = thumbHeight * 0.94f
        val rawX = w * shownRatio - thumbWidth / 2f
        val maxX = (w - thumbWidth).coerceAtLeast(0.dp)
        val x = rawX.coerceIn(0.dp, maxX)
        Image(
            painter = painterResource(TeakIcon.SeekThumb),
            contentDescription = null,
            modifier = Modifier
                .padding(start = x)
                .size(width = thumbWidth, height = thumbHeight),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun HalfScreenMoreSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    speed: Float,
    autoPlayNext: Boolean,
    loopMode: Int,
    mirror: Boolean,
    backgroundPlay: Boolean,
    subtitlesOn: Boolean,
    sleepAtMs: Long,
    onSpeed: (Float) -> Unit,
    onDislike: () -> Unit,
    onWatchLater: () -> Unit,
    onPictureInPicture: () -> Unit,
    onCast: () -> Unit,
    onCache: () -> Unit,
    onAutoPlayNext: () -> Unit,
    onLoopMode: (Int) -> Unit,
    onMirror: () -> Unit,
    onBackgroundPlay: () -> Unit,
    onSubtitlesToggle: () -> Unit,
    onDanmakuSettings: () -> Unit,
    onSleepAt: (Long) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val now = System.currentTimeMillis()
    val timerMinutes = if (sleepAtMs > now) {
        ((sleepAtMs - now + 59_999L) / 60_000L).toInt()
    } else {
        0
    }
    TeakActionSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "更多",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            HalfQuickAction("不感兴趣", colors.onBackground) {
                onDislike()
                onDismiss()
            }
            HalfQuickAction("稍后再看", colors.onBackground) {
                onWatchLater()
                onDismiss()
            }
            HalfQuickAction("缓存", colors.onBackground) {
                onCache()
                onDismiss()
            }
            HalfQuickAction("小窗播放", colors.onBackground) {
                onPictureInPicture()
                onDismiss()
            }
            HalfQuickAction("投屏", colors.onBackground) {
                onCast()
                onDismiss()
            }
        }
        Spacer(Modifier.height(6.dp))
        HalfSpeedRow(
            speed = speed,
            onSelect = onSpeed,
        )
        HalfMoreSwitchRow(
            label = "自动连播",
            checked = autoPlayNext,
            glyph = { AutoPlayNextGlyph(colors.onBackground) },
            onToggle = onAutoPlayNext,
        )
        HalfLoopRow(loopMode = loopMode, onSelect = onLoopMode)
        HalfMoreChoiceRow(
            label = "定时关闭",
            value = if (timerMinutes > 0) "${timerMinutes}分钟" else "不开启",
            glyph = { TimerGlyph(colors.onBackground) },
        ) {
            val next = when {
                timerMinutes <= 0 -> 15
                timerMinutes <= 15 -> 30
                timerMinutes <= 30 -> 60
                else -> 0
            }
            onSleepAt(if (next == 0) 0L else System.currentTimeMillis() + next * 60_000L)
        }
        HalfMoreSwitchRow(
            label = "后台听视频",
            checked = backgroundPlay,
            glyph = { BackgroundListenGlyph(colors.onBackground) },
            onToggle = onBackgroundPlay,
        )
        HalfMoreSwitchRow(
            label = "字幕",
            checked = subtitlesOn,
            glyph = { SubtitleGlyph(colors.onBackground) },
            onToggle = onSubtitlesToggle,
        )
        HalfMoreChoiceRow(
            label = "弹幕设置",
            value = "",
            glyph = { DanmakuSettingGlyph(colors.onBackground) },
        ) {
            onDanmakuSettings()
            onDismiss()
        }
        HalfMoreSwitchRow(
            label = "镜像翻转",
            checked = mirror,
            glyph = { MirrorGlyph(colors.onBackground) },
            onToggle = onMirror,
        )
        }
    }
}

private val HALF_SPEEDS = listOf(1f, 1.25f, 1.5f, 2f, 3f)

@Composable
private fun HalfSpeedRow(speed: Float, onSelect: (Float) -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeedGlyph(colors.onBackground)
        Spacer(Modifier.width(12.dp))
        Text("倍速", color = colors.onBackground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        HALF_SPEEDS.forEachIndexed { index, value ->
            if (index > 0) {
                Text(
                    text = "|",
                    color = colors.onBackgroundVariant.copy(alpha = 0.28f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Text(
                text = if (value % 1f == 0f) value.toInt().toString() else value.toString(),
                color = if (kotlin.math.abs(speed - value) < 0.01f) {
                    BrandPink
                } else {
                    colors.onBackgroundVariant
                },
                fontSize = 11.5.sp,
                modifier = Modifier.clickable(interactionSource = null, indication = null) {
                    onSelect(value)
                },
            )
        }
    }
}

@Composable
private fun HalfQuickAction(
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            when (label) {
                "不感兴趣" -> DislikeGlyph(tint)
                "稍后再看" -> WatchLaterGlyph(tint)
                "缓存" -> CacheGlyphSmall(tint)
                "小窗播放" -> PipGlyphSmall(tint)
                else -> CastGlyphSmall(tint)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = tint.copy(alpha = 0.8f), fontSize = 10.5.sp)
    }
}

@Composable
private fun HalfMoreSwitchRow(
    label: String,
    checked: Boolean,
    glyph: @Composable () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph()
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = colors.onBackground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(22.dp)
                .clip(Capsule())
                .background(if (checked) BrandPink else colors.onBackgroundVariant.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = if (checked) 20.dp else 2.dp, top = 2.dp)
                    .size(18.dp)
                    .clip(Capsule())
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun HalfLoopRow(loopMode: Int, onSelect: (Int) -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoopGlyph(colors.onBackground)
        Spacer(Modifier.width(12.dp))
        Text("循环播放", color = colors.onBackground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        listOf("单集循环", "列表循环", "不循环").forEachIndexed { index, label ->
            if (index > 0) {
                Text(
                    text = "|",
                    color = colors.onBackgroundVariant.copy(alpha = 0.28f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 5.dp),
                )
            }
            val active = if (index == 2) loopMode == 0 else loopMode == index + 1
            Text(
                text = label,
                color = if (active) BrandPink else colors.onBackgroundVariant,
                fontSize = 11.5.sp,
                modifier = Modifier.clickable(interactionSource = null, indication = null) {
                    onSelect(if (index == 2) 0 else index + 1)
                },
            )
        }
    }
}

@Composable
private fun HalfMoreChoiceRow(
    label: String,
    value: String,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph()
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = colors.onBackground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        if (value.isNotBlank()) {
            Text(text = value, color = colors.onBackgroundVariant, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
        }
        Text(text = "›", color = colors.onBackgroundVariant, fontSize = 16.sp)
    }
}

@Composable
private fun HalfScreenAudioMode(
    title: String,
    author: String,
    cover: String,
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    onExit: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekRelative: (Long) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08090D)),
    ) {
        AsyncImage(
            model = rememberImageRequest(cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(36.dp)
                .graphicsLayer { alpha = 0.42f },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.2f),
                        1f to Color.Black.copy(alpha = 0.86f),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 12.dp, end = 88.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = rememberImageRequest(cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("听视频", color = BrandPink, fontSize = 11.sp, fontWeight = FontWeight.Normal)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = author.ifBlank { "UP 主" },
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = "退出听视频",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 14.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = null, indication = null, onClick = onExit)
                .padding(horizontal = 7.dp, vertical = 4.dp),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlayTime(positionMs),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 10.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "-10s",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        onSeekRelative(-10_000L)
                    }
                    .padding(8.dp),
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable(interactionSource = null, indication = null, onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                if (playing) PauseGlyph() else PlayGlyph()
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "+10s",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        onSeekRelative(10_000L)
                    }
                    .padding(8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatPlayTime(durationMs),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 10.sp,
            )
        }
    }
}

/** 日志里只记录域名和路径，避免把带签名的完整播放地址写进文件。 */
private fun describeMediaUrl(url: String?): String {
    if (url.isNullOrBlank()) return "none"
    return runCatching {
        val uri = android.net.Uri.parse(url)
        "${uri.host}${uri.path}"
    }.getOrDefault("invalid")
}

/** Fullscreen lock glyph: shackle closed when locked, open when unlocked. */
@Composable
private fun LockGlyph(locked: Boolean) {
    Canvas(modifier = Modifier.size(23.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // lock body
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.22f, h * 0.45f),
            size = Size(w * 0.56f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        // shackle: full arc when locked, open arc when unlocked
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = if (locked) 180f else 140f,
            useCenter = false,
            topLeft = Offset(
                w * if (locked) 0.32f else 0.30f,
                h * if (locked) 0.18f else 0.12f,
            ),
            size = Size(w * 0.36f, h * 0.46f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
    }
}

private fun sponsorCategoryLabel(category: String): String = when (category) {
    "sponsor" -> "赞助"
    "selfpromo" -> "自我推广"
    "interaction" -> "互动提醒"
    "intro" -> "开场"
    "outro" -> "结尾"
    "preview" -> "预览"
    "music_offtopic" -> "非音乐部分"
    "filler" -> "填充"
    else -> "片段"
}
