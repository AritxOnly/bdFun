package com.aritxonly.teak.common.player

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip

@Composable
fun PixelIcon(
    @DrawableRes res: Int,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentDescription: String? = null,
) {

    val context = LocalContext.current
    val bitmap = remember(res) {
        BitmapFactory.decodeResource(context.resources, res).asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = tint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) },
        filterQuality = FilterQuality.High,
    )
}

@Composable
fun PauseGlyph(tint: Color = Color.White) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 6.dp, height = 22.dp).clip(Capsule()).background(tint))
        Box(Modifier.size(width = 6.dp, height = 22.dp).clip(Capsule()).background(tint))
    }
}

@Composable
fun PlayGlyph(res: Int, tint: Color = Color.White, size: androidx.compose.ui.unit.Dp = 24.dp) {

    PixelIcon(
        res = res,
        tint = tint,
        contentDescription = "播放",
        modifier = Modifier.size(size),
    )
}
