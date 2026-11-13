package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun LivePlayerChrome(content: LiveChromeContent, actions: LiveChromeActions, modifier: Modifier = Modifier) {
    val ui = remember { LiveChromeStateHolder() }
    LiveChromeRender(content, actions, ui).LiveChromeBody(modifier)
}

@Composable
private fun LiveChromeRender.LiveChromeBody(modifier: Modifier) {
    LaunchedEffect(visible, paused, touchTick) {
        if (visible && !paused) {
            delay(4_000L)
            visible = false
        }
    }
    Box(modifier = liveChromeGestures(modifier.fillMaxSize())) {
        if (visible) {
            Box(modifier = Modifier.fillMaxSize()) {
                LiveChromeShade(this@LiveChromeBody)
                LiveChromePortraitHeader(this@LiveChromeBody)
                LiveChromeLandscapeHeader(this@LiveChromeBody)
                LiveChromeBottomControls(Modifier.align(Alignment.BottomCenter))
            }
        }
        gestureTip?.let { tip ->
            LiveGestureIconTip(tip.brightness, tip.value, Modifier.align(Alignment.Center))
        }
    }
}

@Composable
internal fun LiveChromeRender.liveChromeGestures(modifier: Modifier): Modifier {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val activity = remember(context) {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) break
            current = current.baseContext
        }
        current as? Activity
    }
    val audio = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    return modifier.livePlayerGestures(
                ignoreDown = { position, height ->
                    // 控件显示时，顶部和底部的按钮区域交给子控件处理，不抢点击
                    val topZone = with(density) { (if (landscape) 58.dp else 0.dp).toPx() }
                    val bottomZone = with(density) { (if (landscape) 54.dp else 48.dp).toPx() }
                    visible && (
                        (landscape && position.y < topZone) ||
                            position.y > height - bottomZone
                        )
                },
                onTap = {
                    if (!paused) {
                        visible = !visible
                        touch()
                    }
                },
                onDoubleTap = {
                    if (!paused) {
                        onPlayPause()
                        touch()
                    }
                },
                onVerticalDragStart = { leftSide ->
                    dragBase = if (leftSide) {
                        liveCurrentBrightness(activity)
                    } else {
                        audio?.let(::liveCurrentVolumeRatio) ?: 0f
                    }
                },
                onVerticalDrag = { leftSide, totalDy ->
                    val next = (dragBase - totalDy / 1200f).coerceIn(0f, 1f)
                    if (leftSide) {
                        liveApplyBrightness(activity, next)
                        gestureTip = LiveGestureTip(brightness = true, value = next)
                    } else {
                        audio?.let { liveSetVolumeRatio(it, next) }
                        gestureTip = LiveGestureTip(brightness = false, value = next)
                    }
                },
                onVerticalDragEnd = {
                    gestureTip = null
                },
                onCenterSwipe = {
                    if (!paused) {
                        onToggleFullscreen()
                        touch()
                    }
                },
            )
}
