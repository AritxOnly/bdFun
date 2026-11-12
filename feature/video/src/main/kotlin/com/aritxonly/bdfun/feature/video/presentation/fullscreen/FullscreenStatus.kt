package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun FullscreenStatusLine(modifier: Modifier = Modifier) {

    val clock by produceState(initialValue = clockText()) {
        while (true) {
            value = clockText()
            delay(15_000)
        }
    }
    Box(modifier = modifier.height(19.dp)) {
        Text(
            text = clock,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
fun ViewerGlyph(tint: Color = Color.White.copy(alpha = 0.86f)) {

    Canvas(modifier = Modifier.size(12.dp)) {
        val stroke = 1.dp.toPx()
        drawCircle(
            color = tint,
            radius = size.width * 0.15f,
            center = Offset(size.width * 0.3f, size.height * 0.32f),
            style = Stroke(stroke),
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.13f,
            center = Offset(size.width * 0.7f, size.height * 0.3f),
            style = Stroke(stroke),
        )
        drawArc(
            color = tint,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.08f, size.height * 0.5f),
            size = Size(size.width * 0.44f, size.height * 0.38f),
            style = Stroke(stroke),
        )
        drawArc(
            color = tint,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.5f, size.height * 0.48f),
            size = Size(size.width * 0.4f, size.height * 0.36f),
            style = Stroke(stroke),
        )
    }
}
