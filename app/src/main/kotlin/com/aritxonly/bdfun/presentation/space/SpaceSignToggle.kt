package com.aritxonly.bdfun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.SpaceSignToggle(ui: SpaceSignStateHolder) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(18.dp)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null) {
                ui.expanded = !ui.expanded
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                val w = size.width
                val h = size.height
                moveTo(w * 0.18f, h * 0.38f)
                lineTo(w * 0.5f, h * 0.68f)
                lineTo(w * 0.82f, h * 0.38f)
            }
            rotate(degrees = if (ui.expanded) 180f else 0f) {
                drawPath(
                    path,
                    color = colors.onBackgroundVariant,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}
