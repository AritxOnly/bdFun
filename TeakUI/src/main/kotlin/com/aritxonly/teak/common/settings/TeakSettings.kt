package com.aritxonly.teak.common.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.TeakContinuousCornerShape
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TeakMotion
import com.aritxonly.teak.common.chrome.TeakPageChrome
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.material.TeakGlassPanel
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val CardShape = TeakContinuousCornerShape(22.dp)

/** 退出动画结束后 Dialog 还留多久（略长于 240ms 的退场），之后必须拆掉，否则会吞点击。 */
private const val ExitHoldMs = 300L

/**
 * 设置页外壳：滚动大标题 + 返回键进度 + 统一列表间距。
 *
 * 页面内容按 [LazyListScope] 追加；卡片本身用 [TeakSettingsCard]，
 * 行用 [TeakSettingsSwitchRow] / [TeakSettingsDropdownRow] / [TeakSettingsActionRow]。
 */
@Composable
fun TeakSettingsPage(
    title: String,
    modifier: Modifier = Modifier,
    pageState: TeakPageScrollState = TeakPageChrome.state,
    subtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(
        start = TeakChromeDefaults.ContentHorizontalPadding,
        end = TeakChromeDefaults.ContentHorizontalPadding,
        top = TeakChromeDefaults.ContentTopPadding,
        bottom = TeakChromeDefaults.ContentBottomPadding,
    ),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState, pageState)
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
    ) {
        item(key = "teak_settings_header") {
            TeakLargeTitle(
                title = title,
                subtitle = subtitle,
                pageState = pageState,
            )
        }
        content()
    }
}

/** 设置分组卡片：可选的小标题 + 连续曲率圆角白 / 深色卡片。 */
@Composable
fun TeakSettingsCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = modifier.padding(top = 12.dp)) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 14.dp, bottom = 5.dp),
                color = colors.onBackgroundVariant,
                fontSize = 11.5.sp,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(colors.surface),
            content = content,
        )
    }
}

@Composable
fun TeakSettingsSwitchRow(
    title: String,
    description: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.onBackground, fontSize = 16.sp)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = colors.onBackgroundVariant,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun TeakSettingsValueRow(
    title: String,
    value: String,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.onBackground, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Text(text = value, color = colors.onBackgroundVariant, fontSize = 13.5.sp)
    }
}

@Composable
fun TeakSettingsActionRow(
    title: String,
    trailing: String = "",
    color: Color? = null,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = color ?: colors.onBackground, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        if (trailing.isNotBlank()) {
            Text(text = trailing, color = colors.onBackgroundVariant, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(text = "›", color = colors.onBackgroundVariant, fontSize = 21.sp)
    }
}

/**
 * 点击整行后弹出选项：弹簧 + 由模糊变清晰的玻璃二级菜单。
 *
 * 菜单宽度 196dp，右对齐到当前行，纵向盖住行本身；点外部收起，打开后的 260ms 内不响应外部点击。
 */
@Composable
fun TeakSettingsDropdownRow(
    title: String,
    description: String = "",
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    panelWidth: androidx.compose.ui.unit.Dp = 196.dp,
) {
    if (options.isEmpty()) return
    val colors = MiuixTheme.colorScheme
    val accent = accentColor ?: MaterialTheme.colorScheme.primary
    // 选项行和高度测量共用同一套 style；字重比外层行略细一点，但比默认粗
    val optionTextStyle = remember {
        TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    }
    var open by remember { mutableStateOf(false) }
    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var rowPos by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dismissArmed by remember { mutableStateOf(false) }
    var selectArmed by remember { mutableStateOf(false) }
    // Dialog 的存活时间跟退出动画对齐，不能等弹簧收敛：visibilityThreshold 1e-4 的尾巴
    // 会拖到两秒以上，这期间全屏遮罩会吞掉点击，表现就是"关掉以后几秒内点不开"。
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) {
            mounted = true
            dismissArmed = false
            // 刚弹出的 220ms 内不接受选项点击：面板盖在原行上，
            // 连点两下很容易直接选中手指底下的那一项。
            selectArmed = false
            delay(220)
            dismissArmed = true
            selectArmed = true
        } else {
            dismissArmed = false
            selectArmed = false
            delay(ExitHoldMs)
            mounted = false
        }
    }
    // 进场用弹簧（留一点回弹），退场用固定时长的 tween：
    // 弹簧在 1e-4 阈值下尾巴能拖到两秒，期间模糊/玻璃 shader 每帧重算，
    // 既卡又会在 Dialog 被拆掉时看起来"没播完就消失"。
    val motionEnabled = TeakMotion.enabled
    val fractionSpec = spring<Float>(
        dampingRatio = 0.76f,
        stiffness = 350f,
        visibilityThreshold = 1e-4f,
    )
    val exitFractionSpec = tween<Float>(
        durationMillis = 240,
        easing = androidx.compose.animation.core.FastOutSlowInEasing,
    )
    val enterAlphaSpec = tween<Float>(durationMillis = 200)
    val exitAlphaSpec = tween<Float>(durationMillis = 220)
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = open
    val transition = rememberTransition(transitionState, label = "settingsDropdown")
    val fraction by transition.animateFloat(
        transitionSpec = {
            when {
                !motionEnabled -> snap()
                targetState -> fractionSpec
                else -> exitFractionSpec
            }
        },
        label = "fraction",
    ) { state -> if (state) 1f else 0f }
    val fade by transition.animateFloat(
        transitionSpec = {
            if (!motionEnabled) snap()
            else if (targetState) enterAlphaSpec else exitAlphaSpec
        },
        label = "alpha",
    ) { state -> if (state) 1f else 0f }
    // 退出动画期间 Dialog 必须留着；mounted 由 LaunchedEffect 控制，不再等弹簧收敛
    val visible = open || mounted
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                rowSize = it.size
                rowPos = it.positionInWindow()
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = null, indication = null) { open = !open }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = colors.onBackground, fontSize = 16.sp)
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        color = colors.onBackgroundVariant,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = options.getOrElse(selected) { options.first() },
                color = colors.onBackgroundVariant,
                fontSize = 14.sp,
            )
            Spacer(Modifier.width(6.dp))
            TeakChevronUpDown(color = colors.onBackgroundVariant)
        }

        if (visible) {
            val density = LocalDensity.current
            Dialog(
                onDismissRequest = { if (dismissArmed) open = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                    dismissOnClickOutside = false,
                ),
            ) {
                val view = androidx.compose.ui.platform.LocalView.current
                val dialogWindow = (view.parent as? DialogWindowProvider)?.window
                androidx.compose.runtime.SideEffect { dialogWindow?.setDimAmount(0f) }
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .clickable(interactionSource = null, indication = null) {
                                if (dismissArmed) open = false
                            },
                    )
                    val panelWidthDp = panelWidth
                    val panelX = with(density) {
                        rowPos.x + rowSize.width - panelWidthDp.toPx() - 12.dp.toPx()
                    }
                    val screenH = with(density) {
                        LocalConfiguration.current.screenHeightDp.dp.toPx()
                    }
                    // 面板高度不再预估：让内容自己撑开，量到实际高度后再兜住屏幕边缘。
                    // 字体、字号、换行、选项数量怎么变都不会再裁掉最后一项。
                    var measuredHeightPx by remember { mutableStateOf(0) }
                    val maxContentHeight = with(density) {
                        minOf(screenH - 40.dp.toPx(), 420.dp.toPx()).toDp()
                    }
                    val fallbackHeight = with(density) {
                        (20.sp.toDp() + 24.dp) * options.size + 12.dp
                    }
                    val fallbackHeightPx = with(density) { fallbackHeight.toPx() }
                    val bottomGapPx = with(density) { 24.dp.toPx() }
                    // 阴影层在第一帧就要有尺寸，否则量到高度之前它会“突然闪出来”
                    val shadowHeightPx = measuredHeightPx.takeIf { it > 0 } ?: fallbackHeightPx.toInt()
                    val panelY = with(density) { rowPos.y + rowSize.height * 0.28f }
                        .coerceIn(
                            0f,
                            (screenH - (measuredHeightPx.takeIf { it > 0 }?.toFloat()
                                ?: fallbackHeightPx) - bottomGapPx).coerceAtLeast(0f),
                        )
                    // 阴影单独一层：透明度比面板升得更快（fade * 6），退得更快（fade²），
                    // 面板层只负责缩放和内容模糊。
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(panelX.toInt(), panelY.toInt()) }
                            .width(panelWidthDp)
                            .height(with(density) { shadowHeightPx.toDp() })
                            .graphicsLayer {
                                val t = fraction.coerceAtLeast(0f)
                                val a = fade.coerceIn(0f, 1f)
                                alpha = if (open) {
                                    (a * 6f).coerceIn(0f, 1f)
                                } else {
                                    (a * a).coerceIn(0f, 1f)
                                }
                                val s = 0.15f + 0.85f * t
                                scaleX = s
                                scaleY = s
                                transformOrigin = TransformOrigin(1f, 0f)
                            }
                            .dropShadow(
                                shape = RoundedCornerShape(18.dp),
                                shadow = Shadow(
                                    radius = 8.dp,
                                    color = Color.Black,
                                    alpha = 0.10f,
                                ),
                            )
                            .background(Color.White.copy(alpha = 0.001f), RoundedCornerShape(18.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(panelX.toInt(), panelY.toInt()) }
                            .width(panelWidthDp)
                            .onGloballyPositioned { measuredHeightPx = it.size.height }
                            .graphicsLayer {
                                val t = fraction.coerceAtLeast(0f)
                                alpha = fade.coerceIn(0f, 1f)
                                val s = 0.15f + 0.85f * t
                                scaleX = s
                                scaleY = s
                                transformOrigin = TransformOrigin(1f, 0f)
                                // 内容模糊：半径随进度收拢（除以缩放，避免被缩放“锁死”）。
                                // 放在 graphicsLayer 里算，只失效 draw，不触发重组。
                                val blurDp = ((1f - t.coerceIn(0f, 1f)) * 6f / s.coerceAtLeast(0.2f))
                                    .coerceAtMost(10f)
                                renderEffect = if (blurDp > 0.05f) {
                                    val r = blurDp.dp.toPx()
                                    BlurEffect(
                                        radiusX = r,
                                        radiusY = r,
                                        edgeTreatment = TileMode.Decal,
                                    )
                                } else {
                                    null
                                }
                            },
                    ) {
                        TeakGlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            tintAlpha = 0.3f,
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxContentHeight)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 6.dp),
                        ) {
                            options.forEachIndexed { index, option ->
                                val active = index == selected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(interactionSource = null, indication = null) {
                                            if (selectArmed) {
                                                onSelected(index)
                                                open = false
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = option,
                                        style = optionTextStyle,
                                        color = if (active) accent else colors.onBackground,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    if (active) {
                                        // 用同一套 style：勾的字体一旦比选项高，切选项时行高会变，
                                        // 面板会跟着重新量高，看起来就是文字"跳一下"
                                        Text(
                                            text = "✓",
                                            style = optionTextStyle.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = accent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun TeakChevronUpDown(color: Color) {    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .width(10.dp)
            .height(14.dp),
    ) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, h * 0.36f)
            lineTo(w * 0.5f, h * 0.14f)
            lineTo(w * 0.88f, h * 0.36f)
            moveTo(w * 0.12f, h * 0.64f)
            lineTo(w * 0.5f, h * 0.86f)
            lineTo(w * 0.88f, h * 0.64f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}
