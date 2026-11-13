package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoMetadataIdentifier(content: VideoMetadataContent, description: VideoDescriptionStateHolder, descText: String?, descProgress: Float) {
    val colors = MiuixTheme.colorScheme
    val detail = content.detail
    val video = content.video
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
                text = detail?.bvid ?: video.bvid,
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        if (descText != null && description.expandable) {
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 24.dp)
                    .clip(Capsule())
                    .clickable(
                        interactionSource = null,
                        indication = null,
                    ) { description.expanded = !description.expanded },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(width = 12.dp, height = 8.dp)
                        .graphicsLayer { rotationZ = 180f * descProgress },
                ) {
                    val w = size.width
                    val h = size.height
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.12f, h * 0.28f)
                        lineTo(w * 0.5f, h * 0.72f)
                        lineTo(w * 0.88f, h * 0.28f)
                    }
                    drawPath(
                        path = path,
                        color = colors.onBackgroundVariant,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.6.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun VideoMetadataDescription(descText: String?, descProgress: Float, description: VideoDescriptionStateHolder, slots: VideoMetadataSlots) {
    val colors = MiuixTheme.colorScheme
    if (descText != null) {
        Spacer(Modifier.height(4.dp))
        CollapsibleDescription(
            text = descText,
            slots = slots,
            progress = descProgress,
            color = colors.onBackgroundVariant,
            backgroundColor = LocalTeakContentColors.current.background,
            onExpandableChange = { description.expandable = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 1.dp)
                .pointerInput(descText) {
                    detectTapGestures(
                        onLongPress = {
                            slots.copyDescription(descText)
                        },
                    )
                },
        )
    }
}

@Composable
internal fun VideoMetadataCategory(content: VideoMetadataContent) {
    val colors = MiuixTheme.colorScheme
    val detail = content.detail
    if (!detail?.tname.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Row {
            Box(
                modifier = Modifier
                    .clip(Capsule())
                    .background(colors.surfaceContainerHigh)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = detail!!.tname,
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
