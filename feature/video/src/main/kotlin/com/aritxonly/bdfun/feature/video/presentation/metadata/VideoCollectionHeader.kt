package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Playlist
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoCollectionHeader(
    title: String, count: Int, currentIndex: Int, expanded: Boolean,
    onExpand: () -> Unit, onScroll: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null) { onExpand() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Playlist,
                contentDescription = null,
                tint = colors.onBackground,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "合集 · $title",
                color = colors.onBackground,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            // 一键回到顶部，紧贴在合集名称右边。
            CollectionTopButton { onScroll(0) }
            // 一键到底：排在「一键到顶」右边。
            CollectionBottomButton { onScroll(2) }
            // 定位到当前播放的这一集：展开合集并滚到当前集。
            CollectionCurrentButton { onScroll(1) }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${currentIndex + 1}/${count}",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.width(52.dp),
            )
            Spacer(Modifier.width(2.dp))
            ExpandChevron(expanded = expanded)
        }
}

@Composable
private fun CollectionTopButton(onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        // 只回到合集内部的顶部，不把整个视频页滚回最上面。
                        onClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = 1.6.dp.toPx()
                    val cx = size.width / 2f
                    val topY = size.height * 0.16f
                    val bottomY = size.height * 0.86f
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, bottomY),
                        end = androidx.compose.ui.geometry.Offset(cx, topY),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, topY),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * 0.20f,
                            size.height * 0.44f,
                        ),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, topY),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * 0.80f,
                            size.height * 0.44f,
                        ),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
}

@Composable
private fun CollectionBottomButton(onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        onClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = 1.6.dp.toPx()
                    val cx = size.width / 2f
                    val topY = size.height * 0.14f
                    val bottomY = size.height * 0.84f
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, topY),
                        end = androidx.compose.ui.geometry.Offset(cx, bottomY),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, bottomY),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * 0.20f,
                            size.height * 0.56f,
                        ),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    drawLine(
                        color = colors.onBackgroundVariant,
                        start = androidx.compose.ui.geometry.Offset(cx, bottomY),
                        end = androidx.compose.ui.geometry.Offset(
                            size.width * 0.80f,
                            size.height * 0.56f,
                        ),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
}

@Composable
private fun CollectionCurrentButton(onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        onClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
                    val stroke = 1.5.dp.toPx()
                    val c = center
                    drawCircle(
                        color = colors.onBackgroundVariant,
                        radius = size.minDimension * 0.30f,
                        center = c,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                    )
                    drawCircle(
                        color = colors.onBackgroundVariant,
                        radius = size.minDimension * 0.10f,
                        center = c,
                    )
                    val arm = size.minDimension * 0.5f
                    val gap = size.minDimension * 0.38f
                    listOf(
                        androidx.compose.ui.geometry.Offset(c.x, c.y - arm) to
                            androidx.compose.ui.geometry.Offset(c.x, c.y - gap),
                        androidx.compose.ui.geometry.Offset(c.x, c.y + gap) to
                            androidx.compose.ui.geometry.Offset(c.x, c.y + arm),
                        androidx.compose.ui.geometry.Offset(c.x - arm, c.y) to
                            androidx.compose.ui.geometry.Offset(c.x - gap, c.y),
                        androidx.compose.ui.geometry.Offset(c.x + gap, c.y) to
                            androidx.compose.ui.geometry.Offset(c.x + arm, c.y),
                    ).forEach { (start, end) ->
                        drawLine(
                            color = colors.onBackgroundVariant,
                            start = start,
                            end = end,
                            strokeWidth = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                    }
                }
            }
}
