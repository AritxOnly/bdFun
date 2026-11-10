package com.aritxonly.bdfun.ui

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.data.VideoPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 进程级“正在后台播放”的桥。
 *
 * 两条路径：
 * 1. 视频页还活着：只转发播放器动作（普通切换页面、系统 Home）。
 * 2. 视频页正常关闭（保留一镜到底退出动画）：关闭前把 ExoPlayer 交给这里，
 *    由前台服务保活，唱片直接控制这个播放器。
 *
 * 用 token 区分不同播放器：旧 Activity 退场晚于新 Activity 创建时，不能把新状态清掉。
 */
object BackgroundPlayback {

    interface Actions {
        fun next()
        fun previous()
        fun togglePlayPause()
        fun boostStart()
        fun boostEnd()
        fun seekTo(ms: Long)
        fun exit()
    }

    var active by mutableStateOf(false)
        private set

    var video by mutableStateOf<VideoItem?>(null)
    var title by mutableStateOf("")
    var author by mutableStateOf("")
    var cover by mutableStateOf("")
    var playing by mutableStateOf(false)
    var detached by mutableStateOf(false)
        private set

    @Volatile
    var positionMs: Long = 0L

    @Volatile
    var durationMs: Long = 0L

    private var detail: VideoDetail? = null
    private var currentCid: Long = 0L
    private var qualityId: Int = 0

    private var token: Any? = null
    private var actions: Actions? = null
    private var appContext: Context? = null

    private var handOffRequested = false
    private var detachedPlayer: ExoPlayer? = null
    private var detachedScope: CoroutineScope? = null

    /**
     * 播放器释放单独放到后台线程：Media3 的 release() 会等待内部播放线程收尾，
     * 在部分机型/网络状态下会阻塞好几秒。放在主线程就会出现“上滑后整页顿住”。
     */
    private val teardownExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "bdFun-player-teardown").apply { isDaemon = true }
    }

    internal fun attach(token: Any, actions: Actions) {
        this.token = token
        this.actions = actions
        active = true
        detached = false
        handOffRequested = false
    }

    internal fun detach(token: Any) {
        if (this.token !== token) return
        this.token = null
        if (detached || handOffRequested) {
            // 播放器已经交给后台，或者正在交接：保留唱片和元数据。
            actions = null
            return
        }
        clearState()
    }

    internal fun updateInfo(video: VideoItem?, title: String, author: String, cover: String) {
        this.video = video
        this.title = title
        this.author = author
        this.cover = cover
    }

    internal fun updateSession(
        video: VideoItem?,
        detail: VideoDetail?,
        currentCid: Long,
        qualityId: Int,
    ) {
        if (video != null) this.video = video
        this.detail = detail
        this.currentCid = currentCid
        this.qualityId = qualityId
    }

    internal fun updatePlaying(value: Boolean) {
        playing = value
    }

    /** 视频页按返回时调用：先标记“要交棒”，随后正常 finish 让系统播一镜到底退出动画。 */
    fun requestHandOff() {
        handOffRequested = true
    }

    fun cancelHandOff() {
        handOffRequested = false
    }

    internal fun shouldHandOff(): Boolean = handOffRequested

    /** 把仍在使用中的 ExoPlayer 交给后台。必须在 exo.release() 之前调用。 */
    internal fun adopt(exo: ExoPlayer, context: Context) {
        if (detached) return
        detached = true
        handOffRequested = false
        actions = null
        appContext = context.applicationContext
        detachedPlayer = exo
        exo.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        })
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        detachedScope = scope
        scope.launch {
            while (isActive) {
                positionMs = exo.currentPosition.coerceAtLeast(0L)
                durationMs = exo.duration.coerceAtLeast(0L)
                playing = exo.isPlaying
                delay(500L)
            }
        }
        startKeepAlive(context)
    }

    internal fun startKeepAlive(context: Context) {
        appContext = context.applicationContext
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BackgroundPlaybackService::class.java)
                    .setAction(BackgroundPlaybackService.ACTION_START),
            )
        }
    }

    fun stopKeepAlive() {
        if (detached) return
        val ctx = appContext ?: return
        runCatching { ctx.stopService(Intent(ctx, BackgroundPlaybackService::class.java)) }
    }

    fun next() {
        if (detached) {
            skipDetached(1)
        } else {
            actions?.next()
        }
    }

    fun previous() {
        if (detached) {
            skipDetached(-1)
        } else {
            actions?.previous()
        }
    }

    fun togglePlayPause() {
        val exo = detachedPlayer
        if (detached && exo != null) {
            if (exo.isPlaying) exo.pause() else exo.play()
            playing = exo.playWhenReady
        } else {
            actions?.togglePlayPause()
        }
    }

    fun boostStart() {
        if (detached) {
            detachedPlayer?.let { exo ->
                if (!exo.isPlaying) return
                exo.setPlaybackSpeed(AppGraph.settings.boostDefaultSpeed)
                playing = true
            }
        } else {
            actions?.boostStart()
        }
    }

    fun boostEnd() {
        if (detached) {
            detachedPlayer?.setPlaybackSpeed(AppGraph.settings.defaultPlaybackSpeed)
        } else {
            actions?.boostEnd()
        }
    }

    /** 唱片上旋转手指调进度。 */
    fun seekTo(ms: Long) {
        val max = durationMs.coerceAtLeast(0L)
        val target = if (max > 0L) ms.coerceIn(0L, max) else ms.coerceAtLeast(0L)
        if (detached) {
            detachedPlayer?.seekTo(target)
        } else {
            actions?.seekTo(target)
        }
        positionMs = target
    }

    /** 上滑退出：停掉后台播放器和常驻通知。 */
    fun exit() {
        com.aritxonly.bdfun.data.FreezeWatchdog.log(
            "BGP",
            "exit() detached=$detached actions=${actions != null}",
        )
        if (detached) {
            stopDetached()
        } else {
            actions?.exit()
            clearState()
        }
        com.aritxonly.bdfun.data.FreezeWatchdog.log("BGP", "exit() done")
    }

    /** 打开新视频或单击唱片前调用：释放后台播放器，避免两路声音。 */
    fun stopDetached() {
        if (!detached) return
        val t0 = SystemClock.elapsedRealtime()
        val exo = detachedPlayer
        val scope = detachedScope
        val ctx = appContext
        // 先只做不阻塞的事：把界面状态清掉，重活全部交给后台线程。
        detached = false
        detachedPlayer = null
        detachedScope = null
        handOffRequested = false
        clearState()
        com.aritxonly.bdfun.data.FreezeWatchdog.log(
            "BGP",
            "stopDetached: state cleared in ${SystemClock.elapsedRealtime() - t0}ms",
        )
        // 暂停放在主线程且只调用 pause()：它是往播放线程投消息，不会阻塞，
        // 能保证声音立刻停；其余 stop / release / 停服务 / 取消轮询全部放后台。
        runCatching { exo?.pause() }
        com.aritxonly.bdfun.data.FreezeWatchdog.log(
            "BGP",
            "stopDetached: pause returned in ${SystemClock.elapsedRealtime() - t0}ms",
        )
        teardownExecutor.execute {
            val t1 = SystemClock.elapsedRealtime()
            scope?.cancel()
            runCatching { exo?.stop() }
            com.aritxonly.bdfun.data.FreezeWatchdog.log(
                "BGP",
                "teardown: stop done in ${SystemClock.elapsedRealtime() - t1}ms",
            )
            runCatching { exo?.release() }
            com.aritxonly.bdfun.data.FreezeWatchdog.log(
                "BGP",
                "teardown: release done in ${SystemClock.elapsedRealtime() - t1}ms",
            )
            if (ctx != null) {
                runCatching { ctx.stopService(Intent(ctx, BackgroundPlaybackService::class.java)) }
            }
            com.aritxonly.bdfun.data.FreezeWatchdog.log(
                "BGP",
                "teardown: all done in ${SystemClock.elapsedRealtime() - t1}ms",
            )
        }
    }

    private fun clearState() {
        token = null
        actions = null
        active = false
        detached = false
        handOffRequested = false
        video = null
        playing = false
        title = ""
        author = ""
        cover = ""
        detail = null
        currentCid = 0L
        qualityId = 0
        positionMs = 0L
        durationMs = 0L
    }

    private fun skipDetached(delta: Int) {
        val exo = detachedPlayer ?: return
        val d = detail ?: return
        val scope = detachedScope ?: return
        scope.launch {
            val pages = d.backgroundPages()
            val index = pages.indexOfFirst { it.cid == currentCid }
            if (index < 0) return@launch
            val target = pages.getOrNull(index + delta) ?: return@launch
            val episode = d.episodes.firstOrNull { it.cid == target.cid }
            var useDetail = d
            if (episode != null && episode.bvid.isNotBlank() && episode.bvid != d.bvid) {
                useDetail = runCatching { AppGraph.api.view(episode.bvid) }.getOrNull()
                    ?: return@launch
                detail = useDetail
                video = useDetail.toVideoItem()
            }
            val info = runCatching {
                AppGraph.api.playInfo(
                    useDetail,
                    qualityId,
                    target.cid,
                    target.duration,
                    codecPreference = AppGraph.settings.codecPreference,
                    cdnService = AppGraph.settings.cdnService,
                )
            }.getOrNull() ?: return@launch
            val audioUrl = info.audioUrl
                ?: info.fallbackUrl
                ?: info.qualities.firstOrNull()?.videoUrl
                ?: return@launch
            val http = DefaultHttpDataSource.Factory()
                .setUserAgent(BiliApi.UA)
                .setDefaultRequestProperties(
                    mapOf("Referer" to "https://www.bilibili.com/video/${useDetail.bvid}/"),
                )
            val dataSource = DefaultDataSource.Factory(appContext ?: return@launch, http)
            val source = ProgressiveMediaSource.Factory(dataSource)
                .createMediaSource(MediaItem.fromUri(audioUrl))
            exo.setMediaSource(source)
            exo.prepare()
            exo.play()
            currentCid = target.cid
            positionMs = 0L
            durationMs = target.duration * 1000L
            title = useDetail.title
            author = useDetail.ownerName
            cover = useDetail.cover
            video = useDetail.toVideoItem(currentCid)
            playing = true
        }
    }

    private fun VideoDetail.backgroundPages(): List<VideoPage> {
        val episodePages = episodes.mapIndexed { index, episode ->
            VideoPage(
                cid = episode.cid,
                page = index + 1,
                part = episode.title,
                duration = episode.duration,
            )
        }
        if (pages.size > 1) return (pages + episodePages).distinctBy { it.cid }
        if (episodePages.size > 1) return episodePages
        return pages
    }

    private fun VideoDetail.toVideoItem(cid: Long = this.cid): VideoItem = VideoItem(
        bvid = bvid,
        aid = aid,
        title = title,
        cover = cover,
        ownerName = ownerName,
        ownerFace = ownerFace,
        ownerMid = ownerMid,
        play = play,
        danmaku = danmaku,
        duration = duration,
        pubdate = pubdate,
        cid = cid,
        startPositionMs = positionMs,
    )
}
