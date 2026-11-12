package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule

@Composable
fun QualityAction(label: String, onClick: () -> Unit) {

    val base = qualityBaseLabel(label)
    val badge = qualityBadgeLabel(label)
    Box(
        modifier = Modifier
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = base,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-7).dp)
                    .clip(Capsule())
                    .background(Color.Black.copy(alpha = 0.52f))
                    .border(0.7.dp, Color.White.copy(alpha = 0.55f), Capsule())
                    .padding(horizontal = 4.dp, vertical = 0.5.dp),
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

internal fun isHighBitrateLabel(label: String): Boolean =
    label.contains("+") || label.contains("高码率")

internal fun qualityBadgeLabel(label: String): String? = when {
    isHighBitrateLabel(label) -> "高码率"
    label.contains("60") -> "60帧"
    else -> null
}

internal fun qualityBaseLabel(label: String): String {
    val cleaned = label
        .replace(" 高码率", "")
        .replace("高码率", "")
        .removeSuffix("+")
        .let { if (it.endsWith("P60")) it.removeSuffix("60") else it }
        .trim()
    return cleaned.ifBlank { "画质" }
}

fun qualityMenuLabel(label: String): String = when {
    label == "1080P+" || label == "1080P 高码率" -> "1080P 高码率"
    label == "1080P60" -> "1080P 60帧"
    label == "720P60" -> "720P 60帧"
    else -> label
}

/* ------------------------------------------------------------------ panels */
