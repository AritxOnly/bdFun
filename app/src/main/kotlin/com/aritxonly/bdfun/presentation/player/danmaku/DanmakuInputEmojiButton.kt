package com.aritxonly.bdfun.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassIconButton

import com.aritxonly.bdfun.feature.video.application.*

@Composable
internal fun DanmakuInputEmojiButton(runtime: DanmakuInputRuntime) {
    val focusRequester = runtime.focusRequester
    val recipe = runtime.recipe
    val backdrop = runtime.backdrop
    TeakGlassIconButton(
        onClick = {
            runtime.ui.showEmoji = !runtime.ui.showEmoji
            runCatching { focusRequester.requestFocus() }
        },
        size = 46.dp,
        recipe = recipe,
        backdrop = backdrop,
    ) {
        Text(text = "\uD83D\uDE00", fontSize = 19.sp)
    }
}
