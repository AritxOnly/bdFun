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
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackSource
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
internal val BackgroundPlayback get() = AppGraph.backgroundPlayback

internal class BackgroundPlaybackStateHolder(
    private val session: com.aritxonly.bdfun.core.application.PlaybackSessionStateHolder,
    private val processScope: CoroutineScope,
    private val queue: com.aritxonly.bdfun.feature.video.application.BackgroundQueueLoader,
) : com.aritxonly.bdfun.core.application.BackgroundDiscPlayback {

    interface Actions {
        fun next()
        fun previous()
        fun togglePlayPause()
        fun boostStart()
        fun boostEnd()
        fun seekTo(ms: Long)
        fun exit()
    }

    override val state get() = session.state
    private var snapshot by mutableStateOf(session.state.value)
    init { processScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
        session.state.collect { snapshot = session.state.value }
    } }
    private fun sync() { snapshot = session.state.value }
    val fullscreen get() = snapshot.fullscreen
    internal fun updateFullscreen(owner: Any, value: Boolean) { session.setFullscreen(owner, value); sync() }
    val active get() = snapshot.active
    val detached get() = snapshot.detached
    override val playing get() = snapshot.playback.playing
    override val positionMs get() = snapshot.playback.positionMs
    override val durationMs get() = snapshot.playback.durationMs
    override var video: VideoItem?
        get() = snapshot.video
        set(value) { session.updateMetadata(value, title, author, cover); sync() }
    override var title: String
        get() = snapshot.title
        set(value) { session.updateMetadata(video, value, author, cover); sync() }
    override var author: String
        get() = snapshot.author
        set(value) { session.updateMetadata(video, title, value, cover); sync() }
    override var cover: String
        get() = snapshot.cover
        set(value) { session.updateMetadata(video, title, author, value); sync() }

    private var detail: VideoDetail? = null
    private var currentCid: Long = 0L
    private var qualityId: Int = 0

    private var token: Any? = null
    private var actions: Actions? = null
    private var appContext: Context? = null

    private var detachedPlayer: PlaybackController? = null
    private var detachedScope: CoroutineScope? = null

    /**
     * 播放器释放单独放到后台线程：Media3 的 release() 会等待内部播放线程收尾，
     * 在部分机型/网络状态下会阻塞好几秒。放在主线程就会出现“上滑后整页顿住”。
     */
    private val teardownExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "bdFun-player-teardown").apply { isDaemon = true }
    }

    internal fun attach(token: Any, playback: PlaybackController, actions: Actions) {
        this.token = token
        this.actions = actions
        session.attach(token, playback)
        sync()
    }

    internal fun detach(token: Any) {
        if (this.token !== token) return
        this.token = null
        if (detached || session.state.value.handOffRequested) {
            session.detach(token)
            actions = null
            return
        }
        session.detach(token)
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

    /** 视频页按返回时调用：先标记“要交棒”，随后正常 finish 让系统播一镜到底退出动画。 */
    fun requestHandOff() {
        session.requestHandOff(); sync()
    }

    fun cancelHandOff() {
        session.cancelHandOff(); sync()
    }

    internal fun shouldHandOff(): Boolean = session.state.value.handOffRequested

    /** 把仍在使用中的 ExoPlayer 交给后台。必须在 exo.release() 之前调用。 */
    internal fun adopt(exo: PlaybackController, context: Context) {
        if (detached) return
        session.adopt(exo); sync()
        session.cancelHandOff(); sync()
        actions = null
        appContext = context.applicationContext
        detachedPlayer = exo
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        detachedScope = scope
        scope.launch {
            while (isActive) {
                val playback = exo.refresh()
                if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
                    com.aritxonly.bdfun.data.LogCollector.record("BGP_STATE",
                        "id=${System.identityHashCode(exo)} position=${playback.positionMs} " +
                            "playing=${playback.playing} speed=${playback.speed}")
                }
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

    override fun next() {
        if (detached) {
            skipDetached(1)
        } else {
            actions?.next()
        }
    }

    override fun previous() {
        if (detached) {
            skipDetached(-1)
        } else {
            actions?.previous()
        }
    }

    override fun togglePlayPause() {
        val exo = detachedPlayer
        if (detached && exo != null) {
            if (exo.refresh().playing) exo.pause() else exo.play()
        } else {
            actions?.togglePlayPause()
        }
    }

    override fun boostStart() {
        if (detached) {
            detachedPlayer?.let { exo ->
                if (!exo.refresh().playing) return
                exo.setSpeed(AppGraph.settings.boostDefaultSpeed)
            }
        } else {
            actions?.boostStart()
        }
    }

    override fun boostEnd() {
        if (detached) {
            detachedPlayer?.setSpeed(AppGraph.settings.defaultPlaybackSpeed)
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
    }

    /** 上滑退出：停掉后台播放器和常驻通知。 */
    override fun exit() {
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
        detachedPlayer = null
        detachedScope = null
        session.cancelHandOff(); sync()
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
            runCatching { exo?.pause() }
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
        queue.invalidate()
        token = null
        actions = null
        session.clear()
        sync()
        detail = null
        currentCid = 0L
        qualityId = 0
    }

    private fun skipDetached(delta: Int) {
        val exo = detachedPlayer ?: return
        val d = detail ?: return
        val scope = detachedScope ?: return
        scope.launch {
            val track = queue.skip(d, currentCid, delta, qualityId,
                AppGraph.settings.codecPreference, AppGraph.settings.cdnService) ?: return@launch
            if (detachedPlayer !== exo || !detached) return@launch
            exo.prepare(PlaybackSource(
                videoUrl = track.audioUrl, userAgent = BiliApi.UA,
                headers = mapOf("Referer" to "https://www.bilibili.com/video/${track.detail.bvid}/"),
            ))
            detail = track.detail
            currentCid = track.cid
            title = track.detail.title
            author = track.detail.ownerName
            cover = track.detail.cover
            video = track.detail.toVideoItem(currentCid)
        }
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
