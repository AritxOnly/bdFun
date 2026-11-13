package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val controller = remember(dependencies, scope) { dependencies.createEmojiPanel(scope) }
    val emotes by controller.state.collectAsState()
    androidx.compose.runtime.DisposableEffect(controller) { onDispose { controller.close() } }
    LaunchedEffect(controller) { controller.load() }
    val ui = remember { EmojiPanelPresentationStateHolder() }
    LaunchedEffect(Unit) { ui.shown = true }
    val spring = rememberSpringFloat(if (ui.shown) 1f else 0f, stiffness = 300f, damping = 22f)
    val packages = remember(emotes) { emotes.groupBy { it.packageId } }
    val selected = packages[ui.selectedPackage] ?: packages.values.firstOrNull().orEmpty()
    EmojiPanelLayout(selected, spring, onPick, modifier) {
        if (packages.size > 1) EmojiPackageTabs(packages, selected.firstOrNull()?.packageId, ui)
    }
}

@Composable
private fun EmojiPanelLayout(emotes: List<EmoteItem>, spring: Float, onPick: (String) -> Unit, modifier: Modifier,
    tabs: @Composable () -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val hPad = 10.dp
        val vPad = 8.dp
        val gap = 6.dp
        val target = if (emotes.firstOrNull()?.type == 4) 90.dp else if (emotes.firstOrNull()?.size == 2) 54.dp else 34.dp
        val columns = (((maxWidth - hPad * 2) + gap) / (target + gap))
            .toInt()
            .coerceIn(2, 10)
        val cell = ((maxWidth - hPad * 2) - gap * (columns - 1)) / columns
        // 切换表情包不改变面板高度；行高和图片大小不随剩余列宽放大。
        val panelHeight = 220.dp
        val rowHeight = if (emotes.firstOrNull()?.size == 2) 48.dp else 40.dp
        val imageSize = if (emotes.firstOrNull()?.size == 2) 36.dp else 28.dp
        val cells = remember(emotes) { emojiCells(emotes) }
        val layout = EmojiGridLayout(hPad, vPad, gap, columns, cell, panelHeight, cells, rowHeight, imageSize)

        EmojiGlassPanel(layout, spring, onPick, tabs)
    }
}

internal class EmojiPanelPresentationStateHolder { var shown by mutableStateOf(false); var selectedPackage by mutableStateOf(0) }
internal data class EmojiGridLayout(val hPad: androidx.compose.ui.unit.Dp, val vPad: androidx.compose.ui.unit.Dp,
    val gap: androidx.compose.ui.unit.Dp, val columns: Int, val cell: androidx.compose.ui.unit.Dp,
    val panelHeight: androidx.compose.ui.unit.Dp, val cells: List<EmojiCell>,
    val rowHeight: androidx.compose.ui.unit.Dp, val imageSize: androidx.compose.ui.unit.Dp)

@Composable
private fun EmojiPackageTabs(packages: Map<Int, List<EmoteItem>>, selected: Int?, ui: EmojiPanelPresentationStateHolder) {
    androidx.compose.foundation.lazy.LazyRow(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        items(packages.keys.toList().size) { index ->
            val id = packages.keys.elementAt(index)
            val icon = packages.getValue(id).first().packageIcon
            androidx.compose.foundation.layout.Box(
                Modifier.size(40.dp).clickable { ui.selectedPackage = id },
                contentAlignment = Alignment.Center,
            ) {
                if (icon.isBlank()) androidx.compose.material3.Text("${index + 1}", fontSize = 12.sp,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                else coil.compose.AsyncImage(model = icon, imageLoader = emoteImageLoader(), contentDescription = "表情包 ${index + 1}",
                    modifier = Modifier.size(20.dp),
                    alpha = if (id == selected) 1f else 0.5f,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit)
            }
        }
    }
}
