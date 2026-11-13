package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.TeakGlassPanel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.application.*

@Composable
internal fun EmojiGlassPanel(layout: EmojiGridLayout, spring: Float, onPick: (String) -> Unit) {
    val panelHeight = layout.panelHeight
    TeakGlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .graphicsLayer {
                alpha = spring.coerceIn(0f, 1f)
                scaleX = 0.82f + 0.18f * spring
                scaleY = 0.82f + 0.18f * spring
                translationY = (1f - spring) * 10f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            }
            .dropShadow(
                shape = RoundedCornerShape(24.dp),
                shadow = androidx.compose.ui.graphics.shadow.Shadow(
                    radius = 8.dp,
                    color = Color.Black,
                    alpha = 0.10f,
                ),
            ),
        shape = RoundedCornerShape(24.dp),
        // 和设置二级菜单/投币面板同一套底灰
        tintAlpha = 0.3f,
    ) {
        EmojiGridContent(layout, onPick)
    }
}

@Composable
private fun EmojiGridContent(layout: EmojiGridLayout, onPick: (String) -> Unit) {
    val hPad = layout.hPad
    val vPad = layout.vPad
    val gap = layout.gap
    val rows = layout.rows
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = hPad, vertical = vPad),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        rows.forEach { row ->
            EmojiGridRow(row, layout, onPick)
        }
    }
}

@Composable
private fun EmojiGridRow(row: List<EmojiCell>, layout: EmojiGridLayout, onPick: (String) -> Unit) {
    val cell = layout.cell
    val gap = layout.gap
    val columns = layout.columns
    Row(
        modifier = Modifier.fillMaxWidth().height(cell),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.forEach { item ->
            EmojiGridCell(item, cell, onPick)
        }
        repeat(columns - row.size) {
            Spacer(Modifier.size(cell))
        }
    }
}

@Composable
private fun EmojiGridCell(item: EmojiCell, cell: androidx.compose.ui.unit.Dp, onPick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .size(cell)
            .clickable(interactionSource = null, indication = null) {
                onPick(item.pick)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (item.url == null) {
            Text(text = item.pick, fontSize = 22.sp)
        } else {
            AsyncImage(
                model = item.url,
                contentDescription = item.pick,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(cell * 0.84f),
            )
        }
    }
}
