package com.aritxonly.bdfun.playback

import android.content.Context
import android.view.View
import com.aritxonly.bdfun.feature.live.presentation.LivePlaybackRuntime
import com.aritxonly.bdfun.infra.playback.Media3LivePlayback

internal class AndroidLivePlaybackRuntime(private val platform: Media3LivePlayback) : LivePlaybackRuntime {
    override fun prepare(url: String) = platform.prepare(url)
    override fun play() = platform.play()
    override fun pause() = platform.pause()
    override fun release() = platform.release()
    override fun createView(context: Context): View = platform.createView(context)
    override fun bindView(view: View) = platform.bindView(view)
    override fun releaseView(view: View) = platform.releaseView(view)
}
