package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
internal fun BindPlayerOrientation(fullscreen: Boolean, ui: VideoPlayerUiStateHolder) {
    val activity = LocalContext.current as? Activity
    val VideoOrientationLock = LocalVideoPageState.current.orientation
    var fsLockHint by ui.fsLockHint
    val fsLocked by ui.fsLocked
    LaunchedEffect(fsLockHint) {
        if (fsLockHint) {
            kotlinx.coroutines.delay(3_000L)
            fsLockHint = false
        }
    }
    LaunchedEffect(fsLocked, fullscreen) {
        VideoOrientationLock.locked = fsLocked && fullscreen
        if (fsLocked && fullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        } else if (fullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
    DisposableEffect(Unit) {
        onDispose { VideoOrientationLock.locked = false }
    }
}
