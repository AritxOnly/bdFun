package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.DanmakuItem
import kotlin.math.abs
import kotlin.math.roundToInt

private val DmText = Color.White
private val DmSecondary = Color(0xFFB9BDC6)
private val DmPink = BrandPink

/**
 * 弹幕设置内容：全屏右侧面板和半屏浮层共用。
 * 只负责内容，不画面板外壳 / 标题，避免两套播放器各写一份设置。
 */
@Composable
fun DanmakuSettingsContent(
    fullscreen: Boolean,
    onInteract: () -> Unit,
    danmakuItems: List<DanmakuItem> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var showKeywordInput by remember { mutableStateOf(false) }
    var showDmList by remember { mutableStateOf(false) }
    var keyword by remember { mutableStateOf("") }
    val fontScale = if (fullscreen) {
        DanmakuSettings.scaleFullscreen
    } else {
        DanmakuSettings.scaleInline
    }

    Column(modifier = modifier.fillMaxWidth()) {
        DmGroup(title = "弹幕设置") {
            DmSliderRow(
                label = "显示区域",
                valueText = "${(DanmakuSettings.area * 100).roundToInt()}%",
                value = DanmakuSettings.area,
                range = 0.15f..1f,
                onChangeStart = onInteract,
            ) { DanmakuSettings.area = it }

            DmSliderRow(
                label = "不透明度",
                valueText = "${(DanmakuSettings.opacity * 100).roundToInt()}%",
                value = DanmakuSettings.opacity,
                range = 0.2f..1f,
                onChangeStart = onInteract,
            ) { DanmakuSettings.opacity = it }

            DmSliderRow(
                label = "字体大小",
                valueText = "${(fontScale * 100).roundToInt()}%",
                value = fontScale,
                range = 0.6f..2f,
                onChangeStart = onInteract,
            ) {
                if (fullscreen) {
                    DanmakuSettings.scaleFullscreen = it
                } else {
                    DanmakuSettings.scaleInline = it
                }
            }

            DmSliderRow(
                label = "弹幕速度",
                valueText = speedWord(DanmakuSettings.speed),
                value = DanmakuSettings.speed,
                range = 0.5f..2f,
                onChangeStart = onInteract,
            ) { DanmakuSettings.speed = it }

            DmSliderRow(
                label = "描边大小",
                valueText = String.format(java.util.Locale.US, "%.1f", DanmakuSettings.stroke),
                value = DanmakuSettings.stroke,
                range = 0f..4f,
                onChangeStart = onInteract,
            ) { DanmakuSettings.stroke = it }

            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(8.dp))
        DmGroup(title = "画面防挡") {
            DmTypeFilters()
            Spacer(Modifier.height(6.dp))
            DmSwitchRow(
                title = "重复弹幕隐藏",
                description = "大量内容相同的弹幕会被隐藏",
                checked = DanmakuSettings.dedupe,
                onInteract = onInteract,
            ) { DanmakuSettings.dedupe = it }

            DmSwitchRow(
                title = "底部字幕区域防挡",
                description = "底部弹幕会被隐藏",
                checked = DanmakuSettings.subtitleGuard,
                onInteract = onInteract,
            ) { DanmakuSettings.subtitleGuard = it }
        }

        Spacer(Modifier.height(8.dp))
        DmGroup(title = "更多工具") {
            DmSwitchRow(
                title = "弹幕观看屏蔽词",
                description = "仅对我不展示特定弹幕",
                checked = DanmakuSettings.keywordFilter,
                onInteract = onInteract,
            ) { DanmakuSettings.keywordFilter = it }

            DmActionRow(
                title = "添加屏蔽词",
                onClick = {
                    onInteract()
                    showKeywordInput = !showKeywordInput
                },
                trailing = if (DanmakuSettings.keywords.isEmpty()) "" else "${DanmakuSettings.keywords.size} 个",
            )
            if (showKeywordInput) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        singleLine = true,
                        textStyle = TextStyle(color = DmText, fontSize = 12.sp),
                        cursorBrush = SolidColor(DmPink),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        decorationBox = { inner ->
                            Box {
                                if (keyword.isEmpty()) {
                                    Text(
                                        "输入要屏蔽的词",
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontSize = 12.sp,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "添加",
                        color = DmPink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                        ) {
                            val value = keyword.trim()
                            if (value.isNotBlank()) {
                                DanmakuSettings.keywords = DanmakuSettings.keywords + value
                                keyword = ""
                            }
                        },
                    )
                }
                if (DanmakuSettings.keywords.isNotEmpty()) {
                    Text(
                        text = DanmakuSettings.keywords.joinToString(" · "),
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable(interactionSource = null, indication = null) {
                                DanmakuSettings.keywords = emptySet()
                            },
                    )
                }
            }

            DmActionRow(
                title = "当前弹幕列表",
                trailing = "${danmakuItems.size}",
                onClick = {
                    onInteract()
                    showDmList = !showDmList
                },
            )
            if (showDmList) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    danmakuItems.take(60).forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                text = formatPlayTime(item.timeMs),
                                color = Color.White.copy(alpha = 0.35f),
                                fontSize = 10.sp,
                                modifier = Modifier.width(48.dp),
                            )
                            Text(
                                text = item.text,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clickable(interactionSource = null, indication = null) {
                        onInteract()
                        DanmakuSettings.resetDefaults()
                        keyword = ""
                        showKeywordInput = false
                        showDmList = false
                    }
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ResetGlyph()
                Spacer(Modifier.width(5.dp))
                Text(text = "重置弹幕设置", color = DmSecondary, fontSize = 11.5.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DmGroup(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            content = content,
        )
    }
}

private fun speedWord(speed: Float): String = when {
    speed < 0.75f -> "极慢"
    speed < 0.95f -> "慢"
    speed <= 1.05f -> "正常"
    speed < 1.5f -> "快"
    else -> "极快"
}

@Composable
private fun DmSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChangeStart: () -> Unit,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, color = DmText, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(text = valueText, color = DmSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        DmSlider(
            value = value,
            range = range,
            onChangeStart = onChangeStart,
            onValueChange = onValueChange,
        )
    }
}

@Composable
private fun DmSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChangeStart: () -> Unit,
    onValueChange: (Float) -> Unit,
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val ratio = ((value - range.start) / span).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val thumbRadius = with(density) { 8.dp.toPx() }
    val trackHeight = with(density) { 5.dp.toPx() }
    val tickRadius = with(density) { 1.6.dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .let { base ->
                base.then(
                    Modifier.androidPointerSlider(
                        onChangeStart = onChangeStart,
                        onValueChange = {
                            onValueChange(range.start + it * span)
                        },
                    ),
                )
            },
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cy = height / 2f
            val left = thumbRadius
            val usable = (width - thumbRadius * 2f).coerceAtLeast(1f)
            val fillRight = left + usable * ratio
            // 底轨
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(left, cy - trackHeight / 2f),
                size = Size(usable, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
            // 已选
            drawRoundRect(
                color = DmPink,
                topLeft = Offset(left, cy - trackHeight / 2f),
                size = Size(usable * ratio, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
            // 刻度点
            listOf(0.25f, 0.5f, 0.75f).forEach { mark ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = tickRadius,
                    center = Offset(left + usable * mark, cy),
                )
            }
            // 白色圆钮
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(fillRight, cy),
            )
        }
    }
}

@Composable
private fun DmSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onInteract: () -> Unit,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                onInteract()
                onChange(!checked)
            }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = DmText, fontSize = 14.sp)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = description, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        DmSwitch(checked = checked)
    }
}

@Composable
private fun DmSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .background(
                if (checked) DmPink else Color.White.copy(alpha = 0.16f),
                RoundedCornerShape(11.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (checked) 20.dp else 2.dp, top = 2.dp)
                .size(18.dp)
                .background(Color.White, RoundedCornerShape(9.dp)),
        )
    }
}

@Composable
private fun DmTypeFilters() {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 6.dp
        val tile = ((maxWidth - gap * 4) / 5).coerceAtLeast(34.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            DmChip("固定", DmIconKind.Fixed, !DanmakuSettings.blockFixed, tile) {
                DanmakuSettings.blockFixed = !DanmakuSettings.blockFixed
            }
            DmChip("滚动", DmIconKind.Scroll, !DanmakuSettings.blockScroll, tile) {
                DanmakuSettings.blockScroll = !DanmakuSettings.blockScroll
            }
            DmChip("彩色", DmIconKind.Color, !DanmakuSettings.blockColor, tile) {
                DanmakuSettings.blockColor = !DanmakuSettings.blockColor
            }
            DmChip("高级", DmIconKind.Advanced, !DanmakuSettings.blockAdvanced, tile) {
                DanmakuSettings.blockAdvanced = !DanmakuSettings.blockAdvanced
            }
            DmChip("计数", DmIconKind.Count, !DanmakuSettings.blockCount, tile) {
                DanmakuSettings.blockCount = !DanmakuSettings.blockCount
            }
        }
    }
}

@Composable
private fun DmChip(
    label: String,
    kind: DmIconKind,
    active: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(size)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    if (active) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.04f),
                    RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            DmTypeGlyph(kind = kind, tint = if (active) DmText else Color.White.copy(alpha = 0.35f))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) DmText else Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
        )
    }
}

private enum class DmIconKind { Fixed, Scroll, Color, Advanced, Count }

@Composable
private fun DmTypeGlyph(kind: DmIconKind, tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        when (kind) {
            DmIconKind.Fixed -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.28f),
                    size = Size(w * 0.76f, h * 0.44f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = Stroke(stroke),
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.30f, h * 0.5f),
                    end = Offset(w * 0.70f, h * 0.5f),
                    strokeWidth = stroke,
                )
            }
            DmIconKind.Scroll -> {
                drawLine(
                    color = tint,
                    start = Offset(w * 0.12f, h * 0.38f),
                    end = Offset(w * 0.86f, h * 0.38f),
                    strokeWidth = stroke,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.86f, h * 0.38f),
                    end = Offset(w * 0.68f, h * 0.20f),
                    strokeWidth = stroke,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.14f, h * 0.66f),
                    end = Offset(w * 0.88f, h * 0.66f),
                    strokeWidth = stroke,
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.14f, h * 0.66f),
                    end = Offset(w * 0.32f, h * 0.84f),
                    strokeWidth = stroke,
                )
            }
            DmIconKind.Color -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.34f,
                    center = Offset(w * 0.46f, h * 0.52f),
                    style = Stroke(stroke),
                )
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.34f, h * 0.42f))
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.52f, h * 0.34f))
                drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.61f, h * 0.48f))
            }
            DmIconKind.Advanced -> {
                val cx = w * 0.58f
                val cy = h * 0.42f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx, cy - h * 0.18f)
                    lineTo(cx + w * 0.06f, cy - h * 0.03f)
                    lineTo(cx + w * 0.21f, cy)
                    lineTo(cx + w * 0.06f, cy + h * 0.03f)
                    lineTo(cx, cy + h * 0.18f)
                    lineTo(cx - w * 0.06f, cy + h * 0.03f)
                    lineTo(cx - w * 0.21f, cy)
                    lineTo(cx - w * 0.06f, cy - h * 0.03f)
                    close()
                }
                drawPath(path, tint)
                drawLine(
                    color = tint,
                    start = Offset(w * 0.12f, h * 0.50f),
                    end = Offset(w * 0.28f, h * 0.50f),
                    strokeWidth = stroke,
                )
            }
            DmIconKind.Count -> {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = tint.toArgb()
                        textSize = 10.sp.toPx()
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText("×99", w / 2f, h * 0.62f, paint)
                }
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.06f, h * 0.24f),
                    size = Size(w * 0.88f, h * 0.52f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = Stroke(stroke * 0.8f),
                )
            }
        }
    }
}

@Composable
private fun DmActionRow(
    title: String,
    trailing: String = "",
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = DmText, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        if (trailing.isNotBlank()) {
            Text(text = trailing, color = DmSecondary, fontSize = 11.5.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text = "›", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
    }
}

@Composable
private fun ResetGlyph() {
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

/** 连续拖动进度：只响应横向拖动；纵向移动交给外层滚动。 */
private fun Modifier.androidPointerSlider(
    onChangeStart: () -> Unit,
    onValueChange: (Float) -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var start = down.position
            var dragging = false
            var decided = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dx = change.position.x - start.x
                val dy = change.position.y - start.y
                if (!decided) {
                    // 先判断方向：纵向滑动直接放行给外层滚动，避免一碰滑条就改值。
                    if (abs(dy) > viewConfiguration.touchSlop && abs(dy) > abs(dx)) {
                        return@awaitEachGesture
                    }
                    if (abs(dx) > viewConfiguration.touchSlop) {
                        decided = true
                        dragging = true
                        onChangeStart()
                    }
                }
                if (dragging) {
                    onValueChange(
                        (change.position.x / size.width.toFloat().coerceAtLeast(1f))
                            .coerceIn(0f, 1f),
                    )
                    change.consume()
                }
            }
            // 没有拖动、只是点了一下滑轨：按点击位置取值。
            if (!decided) {
                onChangeStart()
                onValueChange(
                    (down.position.x / size.width.toFloat().coerceAtLeast(1f))
                        .coerceIn(0f, 1f),
                )
            }
        }
    },
)
