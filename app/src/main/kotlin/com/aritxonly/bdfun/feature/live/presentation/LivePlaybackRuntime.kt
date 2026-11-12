package com.aritxonly.bdfun.feature.live.presentation

import android.content.Context
import android.view.View
import com.aritxonly.bdfun.core.domain.LivePlayback

internal interface LiveVideoOutput {
    fun createView(context: Context): View
    fun bindView(view: View)
    fun releaseView(view: View)
}
internal interface LivePlaybackRuntime : LivePlayback, LiveVideoOutput
