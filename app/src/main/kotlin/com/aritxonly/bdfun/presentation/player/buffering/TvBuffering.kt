package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TvBuffering(modifier: Modifier = Modifier, tvSize: Dp = 66.dp, showedText: Boolean = true) {
    val ui = remember { TvBufferingPresentationStateHolder() }
    BindTvBufferingDots(ui)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TvBufferingVector(tvSize, ui)
        if (showedText) TvBufferingText()
    }
}
@Composable
private fun TvBufferingText() {
    Spacer(Modifier.height(0.dp))
    Text(
        text = "正在缓冲...",
        color = Color.White,
        fontSize = 11.sp,
        style = androidx.compose.ui.text.TextStyle(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.5f),
                offset = Offset(0f, 1.5f),
                blurRadius = 3f,
            ),
        ),
        modifier = Modifier.offset(y = 0.dp),
    )
}
