package com.aritxonly.bdfun.infra.playback

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.aritxonly.bdfun.core.domain.LivePlayback

class Media3LivePlayback(context: Context, private val layoutId: Int) : LivePlayback {
    private val player = ExoPlayer.Builder(context.applicationContext).build().apply { playWhenReady = true }
    override fun prepare(url: String) { player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play() }
    override fun play() = player.play()
    override fun pause() = player.pause()
    override fun release() = player.release()
    fun createView(context: Context): View = (LayoutInflater.from(context).inflate(layoutId, null) as PlayerView).apply {
        useController = false
        setShutterBackgroundColor(android.graphics.Color.BLACK)
        this.player = this@Media3LivePlayback.player
    }
    fun bindView(view: View) { (view as PlayerView).player = player }
    fun releaseView(view: View) { (view as PlayerView).player = null }
}
