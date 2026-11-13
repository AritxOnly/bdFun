package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.ui.MineTopAppBar

/** 玻璃读取主壳背景时必须位于采样树外，不能成为被自己读取的 RenderNode 子节点。 */
@Composable
internal fun BoxScope.MainMineChrome(pageFloat: Float) {
    if (pageFloat <= 1.01f) return
    val context = LocalContext.current
    Box(Modifier.align(Alignment.TopCenter).graphicsLayer {
        translationX = (2f - pageFloat) * size.width
        alpha = (pageFloat - 1f).coerceIn(0f, 1f)
    }) {
        MineTopAppBar { navigateToSettings(context) }
    }
}
