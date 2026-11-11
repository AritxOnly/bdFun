package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TeakGlassBackButton

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import com.aritxonly.bdfun.ui.resolveVideoPaneLayout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.ui.CommentReplyBar
import com.aritxonly.bdfun.ui.CommentScreen
import com.aritxonly.bdfun.ui.DanmakuInputBar
import com.aritxonly.bdfun.ui.DanmakuSettingsPanel
import com.aritxonly.bdfun.ui.EaseOutQuint
import com.aritxonly.bdfun.ui.rememberJellyProgress
import com.aritxonly.bdfun.ui.rememberSmoothFloat
import com.aritxonly.bdfun.ui.rememberSpringFloat
import com.aritxonly.bdfun.ui.DanmakuSettings
import com.aritxonly.bdfun.ui.PlayerUiState
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.bdfun.ui.TeakGlassChip
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import com.aritxonly.bdfun.ui.UiSwapDurationMs
import com.aritxonly.bdfun.ui.VideoPageBus
import com.aritxonly.bdfun.ui.VideoOrientationLock
import com.aritxonly.bdfun.ui.VideoScreen
import com.aritxonly.bdfun.ui.blockTouches
import com.aritxonly.bdfun.ui.rememberManualProgress
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch

/** 视频详情：独立 Activity；评论页留在本 Activity 内，避免播放器被销毁。 */
class VideoActivity : ComponentActivity() {

    // ---- 自己监听传感器决定横竖屏 ----
    private var splitSystemBars = false
    private var pipEntered = false
    private var pipMode by mutableStateOf(false)
    private var openingAnotherPage = false

    private fun syncVideoSystemBars(split: Boolean = splitSystemBars) {
        splitSystemBars = split
        val dark = when (AppGraph.settings.darkMode) {
            1 -> false
            2 -> true
            else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = split && !dark
    }

    private var orientationListener: android.view.OrientationEventListener? = null
    private var pendingLandscape: Boolean? = null
    private var pendingSince = 0L

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * 系统是否锁定了方向（下拉快捷开关里的「方向锁定」/ 设置里的「自动旋转屏幕」关闭）。
     * 锁定后即使 App 内的「自动旋转」是开的也不能转。
     */
    private fun systemRotationLocked(): Boolean = runCatching {
        Settings.System.getInt(
            contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            1,
        ) == 0
    }.getOrDefault(false)

    private fun autoRotateAllowed(): Boolean =
        AppGraph.settings.autoRotate && !systemRotationLocked()

    /** 上一次真正生效的设备姿态（0 竖屏 / 1 横屏）：只有姿态真的翻转才驱动转屏。 */
    private var lastQuadrant: Int = -1

    private fun startOrientationTracking() {
        if (orientationListener != null) return
        lastQuadrant = if (
            resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        ) 1 else 0
        orientationListener = object : android.view.OrientationEventListener(
            this,
            android.hardware.SensorManager.SENSOR_DELAY_UI,
        ) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                // 每次方向变化都重新读：用户可能中途打开 / 关闭了方向锁定
                if (!PlayerUiState.fullscreen || !autoRotateAllowed() || VideoOrientationLock.locked) return
                // 判定：横屏要转到离水平 12° 以内（比上一版 18° 更严格）
                val landscape = orientation in 78..102 || orientation in 258..282
                val portrait =
                    orientation in 0..24 || orientation in 156..204 || orientation in 336..359
                val quadrant = when {
                    landscape -> 1
                    portrait -> 0
                    else -> return
                }
                // 姿态没变就不动：这样「竖屏时点全屏按钮」不会被传感器立刻扳回竖屏
                if (quadrant == lastQuadrant) {
                    pendingLandscape = null
                    return
                }
                val target = quadrant == 1
                if (pendingLandscape != target) {
                    pendingLandscape = target
                    pendingSince = System.currentTimeMillis()
                    return
                }
                // 姿态稳定保持 220ms 就转屏：再长会让「手机转过去 App 半天不跟」
                if (System.currentTimeMillis() - pendingSince < 220) return
                lastQuadrant = quadrant
                val wanted = if (target) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                if (requestedOrientation != wanted) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d(
                            "ROTDBG",
                            "apply requestedOrientation=$wanted (angle=$orientation)",
                        )
                    }
                    requestedOrientation = wanted
                }
            }
        }.also { it.enable() }
        if (BuildConfig.DEBUG) {
            android.util.Log.d("ROTDBG", "orientation listener enabled")
        }
    }

    private fun stopOrientationTracking() {
        orientationListener?.disable()
        orientationListener = null
        pendingLandscape = null
    }

    override fun onResume() {
        super.onResume()
        openingAnotherPage = false
        if (!isInPictureInPictureMode) pipEntered = false
        // Split panes use the page theme under system bars; stacked/fullscreen video uses light icons.
        runCatching {
            syncVideoSystemBars()
        }
        if (!PlayerUiState.fullscreen) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        if (autoRotateAllowed()) {
            startOrientationTracking()
        } else {
            stopOrientationTracking()

        }
    }

    override fun onPause() {
        super.onPause()
        stopOrientationTracking()
    }

    /** 退出视频：若设置选择「画中画」，先进入系统画中画；否则正常关闭并交给唱片。 */
    private fun exitVideoPage() {
        val s = AppGraph.settings
        if (s.stopOnVideoPageExit) {
            com.aritxonly.bdfun.ui.BackgroundPlayback.cancelHandOff()
            VideoPageBus.pauseFor(this)
            finish()
            return
        }
        if (s.showDiscAfterVideo && s.exitVideoControl == 0) {
            val ok = runCatching {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }.getOrDefault(false)
            if (ok) return
        }
        finish()
    }

    // Home/background is distinct from navigating out of the video page.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AppGraph.settings.stopOnVideoPageExit && !isFinishing && !openingAnotherPage &&
            !isInPictureInPictureMode && com.aritxonly.bdfun.ui.BackgroundPlayback.playing) {
            val entered = runCatching { enterPictureInPictureMode(android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9)).build()) }.getOrDefault(false)
            if (entered) pipEntered = true else VideoPageBus.pauseFor(this)
        }
    }

    override fun startActivityForResult(intent: android.content.Intent, requestCode: Int, options: Bundle?) {
        openingAnotherPage = true
        try { super.startActivityForResult(intent, requestCode, options) }
        catch (error: Exception) { openingAnotherPage = false; throw error }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) pipEntered = true
    }

    override fun onStop() {
        // Dismissing PiP stops the Activity without guaranteeing immediate destruction.
        if (AppGraph.settings.stopOnVideoPageExit && (pipEntered || isFinishing) && !isChangingConfigurations) {
            com.aritxonly.bdfun.ui.BackgroundPlayback.cancelHandOff()
            VideoPageBus.pauseFor(this)
        }
        super.onStop()
    }

    override fun onDestroy() {
        // 只有真正退出这个视频页才清相关推荐；从相关推荐进另一个视频时
        // 这个 Activity 只是 stop、不会 finish，所以返回时不会重载。
        if (isFinishing) {
            com.aritxonly.bdfun.ui.clearRelatedMemory(
                com.aritxonly.bdfun.ui.VideoPageBus.currentBvid,
            )
        }
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // 从评论/空间等页面返回或转场结束时，补一次图标明暗同步。
            runCatching {
                syncVideoSystemBars()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!PlayerUiState.fullscreen) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        if (BuildConfig.DEBUG) {
            android.util.Log.d(
                "ROTDBG",
                "onConfigChanged orientation=${newConfig.orientation} rotation=${display?.rotation} " +
                    "size=${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels} " +
                    "reqOri=$requestedOrientation",
            )
        }
    }

    /**
     * 视频页的状态栏样式：
     * - 沉浸：状态栏透明，视频才真正铺到状态栏下面
     * - 非沉浸：状态栏保持黑色（与旧版一致）
     */
    private fun applyStatusBarStyle(immersive: Boolean) {
        enableEdgeToEdge(
            // Content paints each pane to the window edges; system bars must not add a solid strip.
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
            ) { resources ->
                when (AppGraph.settings.darkMode) {
                    1 -> false
                    2 -> true
                    else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            },
        )
        window.isNavigationBarContrastEnforced = false
    }

    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.ensure(applicationContext)
        // 任何方式新开视频页，都先停掉后台播放器，避免两路声音。
        com.aritxonly.bdfun.ui.BackgroundPlayback.stopDetached()
        applyStatusBarStyle(AppGraph.settings.videoImmersiveStatusBar)
        if (android.os.Build.VERSION.SDK_INT >= 33 && !AppGraph.settings.notifAsked) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                AppGraph.settings.notifAsked = true
                runCatching {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1001,
                    )
                }
            }
        }
        val api = AppGraph.api
        val settings = AppGraph.settings
        // 调试/消息跳转用：adb 带 bvid、消息页只给 64 位 aid 时都从这里异步补详情。
        val debugBvid = intent?.getStringExtra("bvid")
        val debugAid = intent?.getLongExtra("aid", 0L) ?: 0L
        if (AppGraph.pendingVideo.value == null && (debugBvid != null || debugAid > 0L)) {
            Thread {
                runCatching {
                    val detail = kotlinx.coroutines.runBlocking {
                        if (debugBvid != null) {
                            AppGraph.api.view(debugBvid)
                        } else {
                            // 64 位新 av 号：只能问服务器要 BV，本地公式算不出来。
                            AppGraph.api.viewByAid(debugAid)
                        }
                    }
                    AppGraph.pendingVideo.value = com.aritxonly.bdfun.data.VideoItem(
                        bvid = detail.bvid,
                        aid = detail.aid,
                        title = detail.title,
                        cover = detail.cover,
                        ownerName = detail.ownerName,
                        ownerFace = detail.ownerFace,
                        ownerMid = detail.ownerMid,
                        play = detail.play,
                        danmaku = detail.danmaku,
                        duration = detail.duration,
                        pubdate = detail.pubdate,
                        cid = detail.cid,
                    )
                }
            }.start()
        }
        setContent {
            // 每个 Activity 固定自己打开的那个视频，后续跳转只影响新 Activity
            var video by remember { mutableStateOf(AppGraph.pendingVideo.value) }
            if (video == null) {
                // 通过 BV 链接打开时，详情是异步拉的：等它回来，不要立刻 finish
                LaunchedEffect(Unit) {
                    var waited = 0
                    while (video == null && waited < 8000) {
                        kotlinx.coroutines.delay(100)
                        video = AppGraph.pendingVideo.value
                        waited += 100
                    }
                    if (video == null) finish()
                }
                return@setContent
            }
            val loadedVideo = video ?: return@setContent
            LaunchedEffect(loadedVideo.bvid) {
                if (!com.aritxonly.bdfun.data.VideoPlayQueue.contains(loadedVideo.bvid)) {
                    com.aritxonly.bdfun.data.VideoPlayQueue.clear()
                }
            }
            var consumedAutoNext by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableIntStateOf(com.aritxonly.bdfun.ui.VideoPageBus.autoNextRequest)
            }
            LaunchedEffect(com.aritxonly.bdfun.ui.VideoPageBus.autoNextRequest) {
                val tick = com.aritxonly.bdfun.ui.VideoPageBus.autoNextRequest
                if (tick == consumedAutoNext) return@LaunchedEffect
                consumedAutoNext = tick
                val next = com.aritxonly.bdfun.data.VideoPlayQueue.next() ?: return@LaunchedEffect
                com.aritxonly.bdfun.TeakNavigator.openVideo(this@VideoActivity, next)
                finish()
            }
            val pageFullscreen = PlayerUiState.fullscreen || pipMode
            val pageOverlayAlpha = rememberManualProgress(if (pageFullscreen) 0f else 1f, 200f)
            var comments by remember { mutableStateOf<VideoDetail?>(null) }
            // 从消息页进来时，消费一次「定位评论」请求，并直接打开评论页。
            val commentRequest = remember { VideoPageBus.consumeCommentRequest() }
            val pager = androidx.compose.foundation.pager.rememberPagerState(
                initialPage = if (commentRequest.first > 0L) 1 else 0,
                pageCount = { 2 },
            )
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
            // 状态栏高度：隐藏状态栏时不变，首帧 insets 未下发时按系统资源兜底
            val statusBarDp = com.aritxonly.bdfun.ui.rememberStatusBarTop()
            val playerHeightDp = (screenWidthDp * 9f / 16f).dp
            val immersiveStatus = settings.videoImmersiveStatusBar
            // 半屏 tab 栏的顶部：沉浸时视频顶到 0，tab 紧跟播放器下沿
            val playerTopInset = if (immersiveStatus) 0.dp else statusBarDp
            LaunchedEffect(immersiveStatus) {
                val w = window ?: return@LaunchedEffect
                // 设置改变时同步状态栏底色：沉浸=透明（视频透出），非沉浸=黑
                runCatching { applyStatusBarStyle(immersiveStatus) }
                runCatching {
                    syncVideoSystemBars()
                }
            }
            var danmakuInputOpen by remember { mutableStateOf(false) }
            BackHandler(enabled = comments != null) { comments = null }
            BackHandler(enabled = danmakuInputOpen) { danmakuInputOpen = false }
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val paneLayout = resolveVideoPaneLayout(maxWidth.value, maxHeight.value)
            val splitPanes = paneLayout.isSplit && !pageFullscreen
            androidx.compose.runtime.SideEffect { syncVideoSystemBars(splitPanes) }
            TeakActivityScaffold(
                settings = settings,
                onBack = { exitVideoPage() },
                backRevealed = false,
                // 播放器页自己就是视频，不需要再叠后台唱片。
                showBackgroundDisc = false,
                // 返回键改由 overlay 自己画：半屏时和播放器控件一起显隐（平滑淡入淡出）
                showBackButton = false,
                showTopBar = false,
                showScrollTop = !pipMode,
                overlay = overlay@{
                    if (pipMode) return@overlay
                    val fs = pageFullscreen
                    LaunchedEffect(fs) {
                        if (fs) {
                            VideoPageBus.dismissReply++
                            danmakuInputOpen = false
                        }
                    }
                    val overlayAlpha = pageOverlayAlpha
                    if (overlayAlpha > 0.01f) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides
                                com.aritxonly.teak.common.chrome.LocalTeakPageScrollState.current.materialProgress,
                        ) {
                            com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial(
                                enabled = splitPanes,
                                modifier = Modifier
                                    .align(if (splitPanes) Alignment.TopEnd else Alignment.TopStart)
                                    .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
                                    .graphicsLayer {
                                        alpha = overlayAlpha
                                        translationY = if (splitPanes) 0f else VideoPageBus.playerBottomPx
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(top = if (splitPanes) statusBarDp else 0.dp)
                                        .height(52.dp)
                                        .padding(horizontal = if (splitPanes) 8.dp else 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TeakPagerTabs(
                                        options = listOf("简介", "评论"), selected = pager.currentPage,
                                        onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                                        itemWidth = if (splitPanes) 48.dp else 60.dp,
                                    )
                                    VideoDanmakuActions(compact = splitPanes) { danmakuInputOpen = true }
                                }
                            }
                        }

                        // 评论回复栏属于详情栏；只有独占全屏才隐藏。
                        if (!fs && pager.currentPage == 1) {
                            // 编辑中：铺一层全屏点击层，点空白处直接退出（同时杜绝点击穿透到播放器）
                            if (VideoPageBus.replyEditing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val down = event.changes.firstOrNull { it.pressed }
                                                    if (down != null) {
                                                        VideoPageBus.dismissReply++
                                                    }
                                                    event.changes.forEach { it.consume() }
                                                }
                                            }
                                        },
                                )
                            }
                            val replyBarAlpha = rememberManualProgress(
                                if (com.aritxonly.bdfun.ui.VideoPageBus.commentMenuOpen) 0f else 1f,
                                180f,
                            )
                            if (replyBarAlpha > 0.01f) {
                                CommentReplyBar(
                                    modifier = Modifier
                                        .align(if (splitPanes) Alignment.BottomEnd else Alignment.BottomCenter)
                                        .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier)
                                        .graphicsLayer { alpha = replyBarAlpha }
                                        .blockTouches(replyBarAlpha < 0.99f),
                                )
                            }
                        }

                        // 发弹幕输入
                        if (danmakuInputOpen && !fs) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val down = event.changes.firstOrNull { it.pressed }
                                                if (down != null) VideoPageBus.danmakuInputClose++
                                                event.changes.forEach { it.consume() }
                                            }
                                        }
                                    },
                            )
                            DanmakuInputBar(
                                onDismiss = { danmakuInputOpen = false },
                                modifier = Modifier
                                    .align(if (splitPanes) Alignment.BottomStart else Alignment.BottomCenter)
                                    .then(if (splitPanes) Modifier.width(paneLayout.playerWidthDp.dp) else Modifier),
                            )
                        }
                    }

                    // 弹幕设置面板：统一玻璃 + 弹簧弹出；动画过程中面板自身由模糊变清晰
                    val panelOpen = PlayerUiState.danmakuPanelOpen
                    val spring = rememberSpringFloat(
                        target = if (panelOpen) 1f else 0f,
                        stiffness = 300f,
                        damping = 22f,
                    )
                    val shown = spring.coerceIn(0f, 1f)
                    if (panelOpen || shown > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.changes.any { it.pressed }) {
                                                PlayerUiState.danmakuPanelOpen = false
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                },
                        )
                        val selfBlur = if (shown < 0.99f) {
                            Modifier.blur((10f * (1f - shown)).dp)
                        } else {
                            Modifier
                        }
                        DanmakuSettingsPanel(
                            onInteract = { PlayerUiState.danmakuInteract++ },
                            backdrop = VideoPageBus.panelBackdrop,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = statusBarDp + 54.dp, end = if (splitPanes) paneLayout.detailsWidthDp.dp + 12.dp else 12.dp)
                                .graphicsLayer {
                                    alpha = shown
                                    scaleX = 0.82f + 0.18f * spring
                                    scaleY = 0.82f + 0.18f * spring
                                    translationY = (1f - shown) * -14f
                                    transformOrigin =
                                        androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                                }
                                .then(selfBlur),
                        )
                    }
                },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    VideoScreen(
                        api = api,
                        video = loadedVideo,
                        pictureInPicture = pipMode,
                        paneLayout = paneLayout,
                        defaultQuality = settings.qualityPreference,
                        onOpenComments = {
                            if (paneLayout.isSplit) scope.launch { pager.animateScrollToPage(1) }
                            else comments = it
                        },
                        onBack = { exitVideoPage() },
                        pager = pager,
                        autoRotate = settings.autoRotate,
                        immersiveStatusBar = settings.videoImmersiveStatusBar,
                        // 从 Activity 根部读到的状态栏高度，避免页面内部读成 0。
                        statusBarTop = statusBarDp,
                        openedFromVideoPage =
                            intent?.getBooleanExtra("fromVideoPage", false) == true,
                        initialCommentRoot = commentRequest.first,
                        initialCommentTarget = commentRequest.second,
                        initialSeekMs = commentRequest.third,
                    )
                    // 视频底边的白色渐变放进页面录制层，Tab 玻璃采样时会把它和下方内容
                    // 当成同一张底片一起模糊，避免 Tab 内外一亮一白。
                    val fadeColor = MaterialTheme.colorScheme.background
                    if (!splitPanes) Box(
                        modifier = Modifier
                            .align(if (splitPanes) Alignment.TopEnd else Alignment.TopStart)
                            .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
                            .height(54.dp)
                            .graphicsLayer {
                                alpha = if (pipMode) 0f else pageOverlayAlpha
                                // 渐变紧贴播放器真实底边。
                                translationY = if (splitPanes) playerTopInset.toPx() else VideoPageBus.playerBottomPx
                            }
                            .background(
                                Brush.verticalGradient(
                                    0f to fadeColor,
                                    0.5f to fadeColor.copy(alpha = 0.94f),
                                    0.8f to fadeColor.copy(alpha = 0.48f),
                                    1f to fadeColor.copy(alpha = 0f),
                                ),
                            ),
                    )
                    // 页面底部反向渐变：顶部透明，越往下越接近页面底色。
                    Box(
                        modifier = Modifier
                            .align(if (splitPanes) Alignment.BottomEnd else Alignment.BottomCenter)
                            .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
                            .height(54.dp)
                            .graphicsLayer { alpha = if (pipMode) 0f else pageOverlayAlpha }
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0f),
                                        MaterialTheme.colorScheme.background,
                                    ),
                                ),
                            ),
                    )
                    val c = if (pipMode) null else comments
                    if (c != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                        ) {
                            CommentScreen(
                                api = api,
                                aid = c.aid,
                                upMid = c.ownerMid,
                                title = c.title,
                                onBack = { comments = null },
                                bvid = c.bvid,
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun RowScope.VideoDanmakuActions(compact: Boolean = false, onOpenInput: () -> Unit) {
    Spacer(Modifier.weight(1f))
    // 点我发弹幕（与「简介/评论」同一套玻璃）
    TeakGlassChip(
        label = "点我发弹幕",
        horizontalPadding = if (compact) 10.dp else 14.dp,
        active = false,
        onClick = onOpenInput,
    )
    Spacer(Modifier.width(8.dp))
    // 「弹」→ 开关弹幕显示：开启时粉色玻璃 + 白字，关闭时灰字白玻璃
    TeakGlassChip(
        label = "弹",
        active = DanmakuSettings.enabled,
        horizontalPadding = if (compact) 10.dp else 15.dp,
        onClick = {
            DanmakuSettings.enabled = !DanmakuSettings.enabled
            VideoPageBus.danmakuOn = DanmakuSettings.enabled
        },
    )
}
