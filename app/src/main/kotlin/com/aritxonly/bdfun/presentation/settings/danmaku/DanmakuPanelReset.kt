package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.settings.application.*

@Composable
internal fun DanmakuPanelReset(runtime: DanmakuPanelRuntime) {
    val onInteract = runtime.onInteract
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clickable(interactionSource = null, indication = null) {
                    onInteract()
                    runtime.controller.reset()
                }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ResetGlyph()
            Spacer(Modifier.width(5.dp))
            Text(text = "重置弹幕设置", color = DmSecondary, fontSize = 11.5.sp)
        }
    }
}

@Composable
internal fun ResetGlyph() {
    Canvas(modifier = Modifier.size(13.dp)) {
        val stroke = 1.4.dp.toPx()
        drawArc(
            color = DmSecondary,
            startAngle = 40f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(stroke),
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
        )
        drawLine(
            color = DmSecondary,
            start = Offset(size.width * 0.66f, size.height * 0.04f),
            end = Offset(size.width * 0.86f, size.height * 0.24f),
            strokeWidth = stroke,
        )
        drawLine(
            color = DmSecondary,
            start = Offset(size.width * 0.86f, size.height * 0.24f),
            end = Offset(size.width * 0.62f, size.height * 0.30f),
            strokeWidth = stroke,
        )
    }
}
