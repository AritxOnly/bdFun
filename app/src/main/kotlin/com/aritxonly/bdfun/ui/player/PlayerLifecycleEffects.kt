package com.aritxonly.bdfun.ui

import android.app.Activity
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackPhase
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
internal fun BindPlayerLifecycle(playback: PlaybackController, ui: VideoPlayerUiStateHolder) {
    val settings = LocalVideoPageState.current.settings
    val VideoPageBus = LocalVideoPageState.current
    val activity = LocalContext.current as? Activity
    var resumeOnReturn by ui.resumeOnReturn
    DisposableEffect(playback) {
        val pauseHook: (Boolean) -> Unit = { allowResume ->
            // 相关推荐/个人主页跳转前会同步 pauseAll；记住原本正在播，
            // 回到旧页面（ON_RESUME）时自动续播，而不是停在暂停态。
            resumeOnReturn = allowResume && playback.refresh().playWhenReady &&
                playback.refresh().phase != PlaybackPhase.Idle
            playback.pause()
        }
        VideoPageBus.registerPause(activity, pauseHook)
        onDispose {
            VideoPageBus.unregisterPause(pauseHook)
        }
    }
    // 关闭「后台播放」后，App 退到后台立即暂停
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    if (!settings.backgroundPlay) {
                        playback.pause()
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (resumeOnReturn) {
                        resumeOnReturn = false
                        playback.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
