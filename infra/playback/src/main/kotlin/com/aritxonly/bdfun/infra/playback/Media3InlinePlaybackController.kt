package com.aritxonly.bdfun.infra.playback

import android.content.Context
import android.view.TextureView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.aritxonly.bdfun.core.application.InlinePlaybackController
import com.aritxonly.bdfun.core.application.inlinePlaybackAspect
import com.aritxonly.bdfun.core.model.video.InlineVideoMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 保留预览原本的默认 ExoPlayer Builder，不套用详情的解码/缓冲/音频焦点参数。 */
class Media3InlinePlaybackController(
    context: Context,
    muted: Boolean,
    private val userAgent: String,
    private val headers: Map<String, String>,
) : InlinePlaybackController {
    private val player = ExoPlayer.Builder(context).build().apply {
        volume = if (muted) 0f else 1f
        repeatMode = Player.REPEAT_MODE_ONE
        playWhenReady = true
    }
    private val mutableAspect = MutableStateFlow(16f / 9f)
    override val aspectRatio = mutableAspect.asStateFlow()
    private var released = false
    private val listener = object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            mutableAspect.value = inlinePlaybackAspect(videoSize.width, videoSize.height,
                videoSize.pixelWidthHeightRatio, mutableAspect.value)
        }
    }
    init { player.addListener(listener) }

    fun bindSurface(view: TextureView) { if (!released) player.setVideoTextureView(view) }
    override fun setMuted(muted: Boolean) { if (!released) player.volume = if (muted) 0f else 1f }
    override fun play() { if (!released) player.play() }
    override fun pause() { if (!released) player.pause() }

    override fun prepare(media: InlineVideoMedia) {
        if (released) return
        val dataSource = DefaultHttpDataSource.Factory().setUserAgent(userAgent).setDefaultRequestProperties(headers)
        val videoSource: MediaSource = ProgressiveMediaSource.Factory(dataSource)
            .createMediaSource(MediaItem.fromUri(media.videoUrl))
        val audioUrl = media.audioUrl
        val source = if (audioUrl.isNullOrBlank()) videoSource else MergingMediaSource(videoSource,
            ProgressiveMediaSource.Factory(dataSource).createMediaSource(MediaItem.fromUri(audioUrl)))
        player.setMediaSource(source)
        player.prepare()
        player.playWhenReady = true
    }

    override fun release() {
        if (released) return
        released = true
        player.clearVideoSurface()
        player.release()
        player.removeListener(listener)
    }
}
