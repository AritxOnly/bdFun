package com.aritxonly.bdfun.ui

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.PlayInfo
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 动态信息流里的静音预览：详情/播放地址按 bvid 缓存，滚动来回收起时不重复请求。 */
private object InlineVideoCache {
    private const val MAX_ENTRIES = 12

    private val details = java.util.concurrent.ConcurrentHashMap<String, VideoDetail>()
    private val playInfos = java.util.concurrent.ConcurrentHashMap<String, PlayInfo>()

    fun detail(bvid: String): VideoDetail? = details[bvid]

    fun putDetail(bvid: String, detail: VideoDetail) {
        details[bvid] = detail
        trim(details)
    }

    fun playInfo(bvid: String): PlayInfo? = playInfos[bvid]

    fun putPlayInfo(bvid: String, info: PlayInfo) {
        playInfos[bvid] = info
        trim(playInfos)
    }

    private fun <T> trim(map: MutableMap<String, T>) {
        while (map.size > MAX_ENTRIES) {
            val first = map.keys.firstOrNull() ?: break
            map.remove(first)
        }
    }
}

/**
 * 动态卡片里的内嵌视频预览。
 *
 * - 只用一个 TextureView + ExoPlayer，静音循环播放；
 * - 播放地址和 cid 懒加载，失败就保持封面；
 * - 由列表把「离屏幕中心最近的视频」传进来，离开后整个 composable 被回收并 release。
 */
@Composable
fun InlineVideoPlayer(
    video: VideoItem,
    muted: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var info by remember(video.bvid) {
        mutableStateOf(InlineVideoCache.playInfo(video.bvid))
    }
    var failed by remember(video.bvid) { mutableStateOf(false) }

    LaunchedEffect(video.bvid) {
        if (info != null || failed || video.bvid.isBlank()) return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val detail = InlineVideoCache.detail(video.bvid)
                    ?: AppGraph.api.view(video.bvid).also {
                        InlineVideoCache.putDetail(video.bvid, it)
                    }
                InlineVideoCache.playInfo(video.bvid)
                    ?: AppGraph.api.playInfo(
                        detail,
                        AppGraph.settings.dynamicAutoPlayQuality,
                        codecPreference = AppGraph.settings.codecPreference,
                        cdnService = AppGraph.settings.cdnService,
                    ).also {
                        InlineVideoCache.putPlayInfo(video.bvid, it)
                    }
            }.getOrNull()
        }
        if (loaded == null) {
            failed = true
        } else {
            info = loaded
        }
    }

    val loaded = info ?: return
    if (failed) return

    val exo = remember(video.bvid) {
        ExoPlayer.Builder(context).build().apply {
            volume = if (muted) 0f else 1f
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    LaunchedEffect(muted, exo) {
        exo.volume = if (muted) 0f else 1f
    }
    val textureView = remember(video.bvid) {
        TextureView(context).apply {
            // 首帧出来之前透出封面，避免闪黑
            isOpaque = false
        }
    }
    var videoAspect by remember(video.bvid) { mutableFloatStateOf(16f / 9f) }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val ratio = videoSize.width * videoSize.pixelWidthHeightRatio /
                        videoSize.height.toFloat()
                    if (ratio.isFinite() && ratio > 0.05f) videoAspect = ratio
                }
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener) }
    }

    DisposableEffect(exo, textureView) {
        exo.setVideoTextureView(textureView)
        onDispose {
            exo.clearVideoSurface()
            exo.release()
        }
    }

    // 只加载一次播放源；切到后台时暂停，回来后继续
    val preferredQuality = AppGraph.settings.dynamicAutoPlayQuality
    LaunchedEffect(loaded, exo, preferredQuality) {
        val quality = loaded.qualities.firstOrNull { it.id == preferredQuality }
            ?: loaded.qualities.firstOrNull { it.id == 64 }
            ?: loaded.qualities.lastOrNull()
        val url = quality?.videoUrl ?: loaded.fallbackUrl ?: return@LaunchedEffect
        val dataSource = DefaultHttpDataSource.Factory()
            .setUserAgent(BiliApi.UA)
            .setDefaultRequestProperties(
                mapOf("Referer" to "https://www.bilibili.com/video/${video.bvid}/"),
            )
        val videoSource: MediaSource = ProgressiveMediaSource.Factory(dataSource)
            .createMediaSource(MediaItem.fromUri(url))
        val source = if (loaded.audioUrl.isNullOrBlank()) {
            videoSource
        } else {
            MergingMediaSource(
                videoSource,
                ProgressiveMediaSource.Factory(dataSource)
                    .createMediaSource(MediaItem.fromUri(loaded.audioUrl)),
            )
        }
        exo.setMediaSource(source)
        exo.prepare()
        exo.playWhenReady = true
    }

    DisposableEffect(lifecycleOwner, exo) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> exo.pause()
                Lifecycle.Event.ON_START -> exo.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            val containerAspect = maxWidth / maxHeight
            val videoModifier = if (videoAspect >= containerAspect) {
                Modifier.fillMaxWidth().aspectRatio(videoAspect)
            } else {
                Modifier.fillMaxHeight().aspectRatio(videoAspect)
            }
            AndroidView(
                factory = { textureView },
                modifier = videoModifier,
            )
        }
    }
}
