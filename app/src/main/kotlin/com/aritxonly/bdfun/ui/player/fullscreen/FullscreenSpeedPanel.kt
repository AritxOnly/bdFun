package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink

@Composable
internal fun SpeedPanel(
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {

    PanelCard(title = "倍速") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            FullscreenSpeeds.forEach { speed ->
                val active = kotlin.math.abs(speed - playback.speed) < 0.001f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
                        )
                        .clickable(interactionSource = null, indication = null) {
                            actions.onSpeedSelected(speed)
                            onClose()
                        }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${trimSpeed(speed)}x",
                        color = if (active) BrandPink else Color.White,
                        fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    )
                    Spacer(Modifier.weight(1f))
                    if (active) CheckGlyph(BrandPink)
                }
            }
        }
    }
}
