package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule

import com.aritxonly.bdfun.feature.video.application.*

@Composable
internal fun androidx.compose.foundation.layout.RowScope.DanmakuInputSurface(runtime: DanmakuInputRuntime) {
    val recipe = runtime.recipe
    val backdrop = runtime.backdrop
    TeakGlassSurface(
        modifier = Modifier.weight(1f).height(46.dp),
        shape = Capsule(),
        recipe = recipe,
        backdrop = backdrop,
        contentAlignment = Alignment.CenterStart,
    ) {
        DanmakuInputField(runtime)
    }

}
