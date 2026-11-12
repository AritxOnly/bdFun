package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.kyant.shapes.Capsule

@Composable
internal fun BadgeStatAction(
    icon: Int,
    count: Long,
    active: Boolean = false,
    yOffset: androidx.compose.ui.unit.Dp = (-7).dp,
    xOffset: androidx.compose.ui.unit.Dp = 0.dp,
    countXOffset: androidx.compose.ui.unit.Dp = 3.dp,
    countYOffset: androidx.compose.ui.unit.Dp = 6.dp,
    onClick: () -> Unit,
) {

    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 40.dp)
            .offset(x = xOffset, y = yOffset)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        PixelIcon(
            res = icon,
            tint = if (active) BrandPink else Color.White,
            modifier = Modifier.size(22.dp),
        )
        if (count > 0L) {
            Text(
                text = compactCount(count),
                color = if (active) BrandPink else Color.White,
                fontSize = 9.5.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = countXOffset, y = countYOffset),
            )
        }
    }
}

@Composable
internal fun IconOnlyAction(
    icon: Int,
    xOffset: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit,
) {

    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 40.dp)
            .offset(x = xOffset, y = (-7).dp)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        PixelIcon(
            res = icon,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
internal fun PlayerIconButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun PlayerTextAction(
    text: String,
    active: Boolean = false,
    muted: Boolean = false,
    onClick: () -> Unit,
) {

    Text(
        text = text,
        color = when {
            active -> BrandPink
            muted -> Color.White.copy(alpha = 0.35f)
            else -> Color.White
        },
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        modifier = Modifier
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 8.dp),
    )
}
