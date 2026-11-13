package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.application.*
import com.aritxonly.bdfun.core.model.comment.EmoteItem
import androidx.compose.runtime.collectAsState

@Composable
internal fun EmojiPanel(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val dependencies = LocalRichTextDependencies.current
    val controller = remember(dependencies) { dependencies.createEmojiPanel() }
    val emotes by controller.state.collectAsState()
    androidx.compose.runtime.DisposableEffect(controller) { onDispose { controller.close() } }
    LaunchedEffect(controller) { controller.load() }
    val ui = remember { EmojiPanelPresentationStateHolder() }
    LaunchedEffect(Unit) { ui.shown = true }
    val spring = rememberSpringFloat(if (ui.shown) 1f else 0f, stiffness = 300f, damping = 22f)
    EmojiPanelLayout(emotes, spring, onPick, modifier)
}

@Composable
private fun EmojiPanelLayout(emotes: List<EmoteItem>, spring: Float, onPick: (String) -> Unit, modifier: Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val hPad = 10.dp
        val vPad = 8.dp
        val gap = 6.dp
        val columns = (((maxWidth - hPad * 2) + gap) / (34.dp + gap))
            .toInt()
            .coerceIn(5, 10)
        val cell = ((maxWidth - hPad * 2) - gap * (columns - 1)) / columns
        // 固定 4 行高度；内容整体可滚动，不再用 LazyGrid，避免最后一行被圆角裁切。
        val panelHeight = vPad * 2 + cell * 4 + gap * 3 + 2.dp
        val cells = emojiCells(emotes)
        val rows = cells.chunked(columns)
        val layout = EmojiGridLayout(hPad, vPad, gap, columns, cell, panelHeight, rows)

        EmojiGlassPanel(layout, spring, onPick)
    }
}

internal class EmojiPanelPresentationStateHolder { var shown by mutableStateOf(false) }
internal data class EmojiGridLayout(val hPad: androidx.compose.ui.unit.Dp, val vPad: androidx.compose.ui.unit.Dp,
    val gap: androidx.compose.ui.unit.Dp, val columns: Int, val cell: androidx.compose.ui.unit.Dp,
    val panelHeight: androidx.compose.ui.unit.Dp, val rows: List<List<EmojiCell>>)
