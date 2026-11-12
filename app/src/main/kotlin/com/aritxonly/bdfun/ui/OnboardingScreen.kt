package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.R
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.chrome.TeakTopOverlay
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsDropdownRow
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.effect.bg.BgEffectBackground
import top.yukonga.miuix.kmp.effect.bg.DeviceType
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 开始页 / 首次引导。
 *
 * 版式、动效、材质都按归元（Guiyuan）的开屏与引导页移植：
 * 欢迎页（旋转呼吸的图标 + 大字 + 玻璃圆形箭头）→ 重要提醒 → 基础设置 → 完成。
 * 归元里的 Xposed / Root / 协议页在 bdFun 没有对应内容，按现有设置和文案替换。
 */
private const val STEP_WELCOME = 0
private const val STEP_NOTICE = 1
private const val STEP_BASIC = 2
private const val STEP_DONE = 3

@Composable
fun OnboardingScreen(
    settings: SettingsStore,
    onAccept: () -> Unit,
    onExit: () -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(STEP_WELCOME) }
    var backwards by remember { mutableStateOf(false) }
    var accepted by remember { mutableStateOf(false) }
    var termsViewed by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    fun go(next: Int) {
        backwards = next < step
        step = next
    }

    BackHandler {
        when {
            showTerms -> showTerms = false
            step > STEP_WELCOME -> go(step - 1)
            else -> onExit()
        }
    }

    // 整页套一层透明的 Miuix Scaffold：玻璃下拉菜单和底部弹层都需要它提供弹出宿主。
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 整页左右平移，和归元 NavDisplay 的引导步骤切换一致（只位移、不缩放）。
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (backwards) -1 else 1
                    (slideInHorizontally(tween(durationMillis = 360, easing = FastOutSlowInEasing)) { it * dir } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally(tween(durationMillis = 360, easing = FastOutSlowInEasing)) { -it * dir } + fadeOut(tween(160)))
                },
                label = "onboarding-step",
            ) { current ->
                when (current) {
                    STEP_WELCOME -> WelcomePage(onNext = { go(STEP_NOTICE) })
                    STEP_NOTICE -> NoticePage(
                        accepted = accepted,
                        termsViewed = termsViewed,
                        onAcceptedChange = { accepted = it },
                        onOpenTerms = {
                            termsViewed = true
                            showTerms = true
                        },
                        onBack = { go(STEP_WELCOME) },
                        onNext = { go(STEP_BASIC) },
                    )
                    STEP_BASIC -> BasicPage(
                        settings = settings,
                        onBack = { go(STEP_NOTICE) },
                        onStart = { go(STEP_DONE) },
                    )
                    else -> CompletePage(
                        onStart = {
                            settings.onboardingAcked = true
                            onAccept()
                        },
                    )
                }
            }

            if (showTerms) {
                TermsSheet(show = true, onDismiss = { showTerms = false })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 背景
// ─────────────────────────────────────────────────────────────────────────────

/** 归元同款动态渐变背景。同时把渐变层录进 backdrop，供上面的玻璃小件采样。 */
@Composable
private fun OnboardingBackground(
    playing: Boolean,
    backdrop: LayerBackdrop,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    val surface = MiuixTheme.colorScheme.surface
    val shader = remember { isRuntimeShaderSupported() }
    BgEffectBackground(
        dynamicBackground = playing,
        deviceType = if (LocalConfiguration.current.screenWidthDp >= 600) {
            DeviceType.PAD
        } else {
            DeviceType.PHONE
        },
        isDarkTheme = surface.luminance() < 0.5f,
        surface = surface,
        modifier = modifier.fillMaxSize(),
        bgModifier = if (shader) Modifier.layerBackdrop(backdrop) else Modifier,
        isFullSize = true,
        content = content,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 欢迎页 / 完成页
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    val surface = MiuixTheme.colorScheme.surface
    val accent = MiuixTheme.colorScheme.primary
    val fallback = Brush.linearGradient(
        listOf(
            lerp(surface, accent, 0.25f),
            lerp(surface, accent, 0.06f),
            lerp(surface, accent, 0.19f),
        ),
    )
    val backdrop = rememberLayerBackdrop()
    val wave = remember { Animatable(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val diameter = with(density) { 100.dp.toPx() }
    val radius = diameter / 2f
    val dark = surface.luminance() < 0.5f
    val glint = if (dark) lerp(accent, Color.Black, 0.35f) else lerp(accent, Color.White, 0.70f)
    var titleDistance by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize().background(fallback)) {
        OnboardingBackground(playing = true, backdrop = backdrop)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BdFunMark(
                        markSize = 100.dp,
                        wave = wave,
                        wavePhase = wavePhase,
                        onWaveRelease = { wavePhase = it },
                        interactive = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "bdFun",
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val measured = coords.positionInParent().y +
                                    coords.size.height / 2f - radius
                                if (titleDistance != measured) titleDistance = measured
                            }
                            .graphicsLayer {
                                val lift = waveLift(wave.value, titleDistance, diameter, wavePhase)
                                compositingStrategy = if (wave.isRunning) {
                                    CompositingStrategy.Offscreen
                                } else {
                                    CompositingStrategy.Auto
                                }
                                scaleX = 1f + lift * 0.055f
                                scaleY = 1f + lift * 0.035f
                                translationY = -lift * 7.dp.toPx()
                            }
                            .waveGlint(wave, titleDistance, diameter, wavePhase, glint, accent),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                }
            }
            GuideArrowButton(backdrop = backdrop, onClick = onNext)
            Spacer(Modifier.weight(0.28f))
        }
    }
}

@Composable
private fun CompletePage(onStart: () -> Unit) {
    val surface = MiuixTheme.colorScheme.surface
    val accent = MiuixTheme.colorScheme.primary
    val fallback = Brush.linearGradient(
        listOf(
            lerp(surface, accent, 0.25f),
            lerp(surface, accent, 0.06f),
            lerp(surface, accent, 0.19f),
        ),
    )
    val backdrop = rememberLayerBackdrop()
    val wave = remember { Animatable(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val diameter = with(density) { 100.dp.toPx() }
    val radius = diameter / 2f
    val dark = surface.luminance() < 0.5f
    val glint = if (dark) lerp(accent, Color.Black, 0.35f) else lerp(accent, Color.White, 0.70f)
    var titleDistance by remember { mutableFloatStateOf(0f) }
    var doneDistance by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize().background(fallback)) {
        OnboardingBackground(playing = true, backdrop = backdrop)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BdFunMark(
                        markSize = 100.dp,
                        wave = wave,
                        wavePhase = wavePhase,
                        onWaveRelease = { wavePhase = it },
                        interactive = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "bdFun",
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val measured = coords.positionInParent().y +
                                    coords.size.height / 2f - radius
                                if (titleDistance != measured) titleDistance = measured
                            }
                            .graphicsLayer {
                                val lift = waveLift(wave.value, titleDistance, diameter, wavePhase)
                                compositingStrategy = if (wave.isRunning) {
                                    CompositingStrategy.Offscreen
                                } else {
                                    CompositingStrategy.Auto
                                }
                                scaleX = 1f + lift * 0.055f
                                scaleY = 1f + lift * 0.035f
                                translationY = -lift * 7.dp.toPx()
                            }
                            .waveGlint(wave, titleDistance, diameter, wavePhase, glint, accent),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(36.dp))
                    Text(
                        text = "设置完成",
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                val measured = coords.positionInParent().y +
                                    coords.size.height / 2f - radius
                                if (doneDistance != measured) doneDistance = measured
                            }
                            .graphicsLayer {
                                val lift = waveLift(wave.value, doneDistance, diameter, wavePhase)
                                compositingStrategy = if (wave.isRunning) {
                                    CompositingStrategy.Offscreen
                                } else {
                                    CompositingStrategy.Auto
                                }
                                scaleX = 1f + lift * 0.045f
                                scaleY = 1f + lift * 0.025f
                                translationY = -lift * 5.dp.toPx()
                            }
                            .waveGlint(wave, doneDistance, diameter, wavePhase, glint, accent),
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            GuidePrimaryPill(
                text = "开始使用",
                modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth(),
                onClick = onStart,
            )
            Spacer(Modifier.height(44.dp))
        }
    }
}

/** 归元底部那枚圆形玻璃箭头。 */
@Composable
private fun GuideArrowButton(
    backdrop: LayerBackdrop,
    onClick: () -> Unit,
) {
    val surface = MiuixTheme.colorScheme.surface
    val shader = remember { isRuntimeShaderSupported() }
    val scrim = lerp(
        MiuixTheme.colorScheme.onSurface,
        MiuixTheme.colorScheme.onSurfaceContainer,
        0.35f,
    ).copy(alpha = 0.34f)
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(CircleShape)
            .then(
                if (shader) {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = CircleShape,
                        blurRadius = 56f,
                        colors = BlurColors(blendColors = guideBlendColors()),
                    )
                } else {
                    Modifier
                },
            )
            .background(scrim, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.width(31.dp).height(22.dp)) {
            val middle = size.height / 2f
            val tip = size.width * 0.96f
            val tail = size.width * 0.04f
            val wing = size.height * 0.42f
            drawLine(surface, Offset(tail, middle), Offset(tip, middle), 2.6.dp.toPx(), StrokeCap.Round)
            drawLine(surface, Offset(tip - wing, middle - wing), Offset(tip, middle), 2.6.dp.toPx(), StrokeCap.Round)
            drawLine(surface, Offset(tip - wing, middle + wing), Offset(tip, middle), 2.6.dp.toPx(), StrokeCap.Round)
        }
    }
}

@Composable
private fun guideBlendColors(): List<BlendColorEntry> {
    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    return remember(dark) {
        if (dark) {
            listOf(
                BlendColorEntry(Color(0xe6a1a1a1), BlurBlendMode.ColorDodge),
                BlendColorEntry(Color(0x4de6e6e6), BlurBlendMode.LinearLight),
            )
        } else {
            listOf(
                BlendColorEntry(Color(0xcc4a4a4a), BlurBlendMode.ColorBurn),
                BlendColorEntry(Color(0xff4f4f4f), BlurBlendMode.LinearLight),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 品牌图标（bdFun 自己的启动图标）
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 归元的 Logo 是一个会缓慢转圈的圆环，所以它可以旋转。
 * bdFun 的图标是带字的方形，转起来字会倒，所以保留同一套「长按起涟漪 + 按下回弹」，
 * 把旋转换成很轻的上下呼吸。
 */
@Composable
private fun BdFunMark(
    markSize: Dp = 100.dp,
    wave: Animatable<Float, AnimationVector1D>,
    wavePhase: Float,
    onWaveRelease: (Float) -> Unit,
    interactive: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var boosted by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 600f),
        label = "mark-press",
    )
    val breathe = rememberInfiniteTransition(label = "mark-breathe")
    val breath by breathe.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "mark-breath",
    )

    LaunchedEffect(boosted) {
        if (boosted) {
            onWaveRelease(0f)
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                wave.snapTo(0f)
                wave.animateTo(1f, tween(durationMillis = 1150, easing = FastOutSlowInEasing))
            }
        }
    }

    val pressure = if (interactive) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                        boosted = false
                    }
                },
                onLongPress = { boosted = true },
            )
        }
    } else {
        Modifier
    }

    val tint = MiuixTheme.colorScheme.onSurfaceContainer
    val airflow = lerp(tint, MiuixTheme.colorScheme.primary, 0.55f)

    Box(
        modifier = Modifier
            .size(markSize)
            .then(pressure)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        contentAlignment = Alignment.Center,
    ) {
        // 涟漪和归元一样画在图标外面一圈，只在长按触发时出现。
        if (wave.isRunning) {
            Canvas(Modifier.requiredSize(markSize * 3.8f)) {
                val progress = wave.value
                val diameter = markSize.toPx()
                drawWave(airflow, progress, 0.46f, diameter, wavePhase)
                drawWave(airflow, (progress - 0.2f) / 0.8f, 0.10f, diameter, wavePhase)
            }
        }
        // 前景图层四周自带约 16% 透明留白，放大 1.42 倍后图形正好落在 100dp 里。
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "bdFun",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(markSize * 1.42f)
                .graphicsLayer {
                    val s = 1f + 0.012f * breath
                    scaleX = s
                    scaleY = s
                    translationY = (-1.5f * breath).dp.toPx()
                },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 重要提醒页
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoticePage(
    accepted: Boolean,
    termsViewed: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val scroll = rememberScrollState()

    GuidePageFrame(onBack = onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 12.dp),
        ) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(82.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = colors.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "重要提醒",
                        style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = colors.onBackground,
                    )
                }
                Spacer(Modifier.height(30.dp))
                TeakSettingsCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        GuideNote("本应用是第三方哔哩哔哩客户端，与哔哩哔哩官方无隶属、授权或合作关系。")
                        GuideNote("所有视频、评论、账号数据均来自哔哩哔哩公开接口，请遵守官方协议和当地法律。")
                        GuideNote("请勿用于商业用途、批量抓取或绕过风控；由此产生的账号风险由使用者自行承担。")
                        GuideNote("接口随时可能调整，部分功能可能失效；遇到问题可在「设置 → 关于 → 日志抓取」导出日志反馈。")
                        GuideNote("登录凭据只保存在本机，不会上传到任何第三方服务器。")
                    }
                }
                Spacer(Modifier.height(12.dp))
                TeakSettingsCard(title = "使用条款", modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = null, indication = null, onClick = onOpenTerms)
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "使用条款与免责声明",
                                style = MiuixTheme.textStyles.headline1,
                                color = colors.onBackground,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = if (termsViewed) "已查看" else "使用前请完整阅读",
                                style = MiuixTheme.textStyles.footnote1,
                                color = if (termsViewed) colors.primary else colors.onSurfaceVariantSummary,
                            )
                        }
                        Text(
                            text = "›",
                            fontSize = 22.sp,
                            color = colors.onSurfaceVariantSummary,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .toggleable(
                        value = accepted,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        role = androidx.compose.ui.semantics.Role.Checkbox,
                        onValueChange = onAcceptedChange,
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    state = androidx.compose.ui.state.ToggleableState(accepted),
                    onClick = null,
                )
                Text(
                    text = "我已阅读并理解以上提醒",
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.body2,
                    color = colors.onSurface,
                )
            }
            GuidePrimaryPill(
                text = "同意并继续",
                modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth(),
                enabled = accepted,
                onClick = onNext,
            )
            Spacer(Modifier.height(44.dp))
    }
}

@Composable
private fun GuideNote(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "•",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.body2.copy(lineHeight = 1.5.em),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 基础设置页
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BasicPage(
    settings: SettingsStore,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val scroll = rememberScrollState()
    val guideState = remember { TeakPageScrollState() }
    LaunchedEffect(scroll.value) {
        guideState.update(firstVisibleItemIndex = 0, offsetPx = scroll.value.toFloat())
    }

    GuidePageFrame(onBack = onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 12.dp),
        ) {
                Spacer(Modifier.height(4.dp))
                TeakLargeTitle(title = "基础设置", pageState = guideState, topSpacer = 0.dp)
                Spacer(Modifier.height(8.dp))

                TeakSettingsCard(title = "外观", modifier = Modifier.fillMaxWidth()) {
                    TeakSettingsDropdownRow(
                        title = "主题",
                        options = listOf("跟随系统", "浅色", "深色"),
                        selected = settings.darkMode.coerceIn(0, 2),
                        onSelected = { settings.darkMode = it },
                    )
                    TeakSettingsDropdownRow(
                        title = "色彩风格",
                        options = listOf("AC 粉", "Hyper 蓝"),
                        selected = settings.colorStyle.coerceIn(0, 1),
                        onSelected = {
                            settings.colorStyle = it
                            com.aritxonly.bdfun.ThemePalette.style = it
                        },
                    )
                    TeakSettingsDropdownRow(
                        title = "界面风格",
                        options = listOf("官方", "经典", "HyperOS 4"),
                        selected = settings.interfaceStyle.coerceIn(0, 2),
                        onSelected = {
                            settings.interfaceStyle = it
                            com.aritxonly.teak.common.theme.TeakUiStyle.style = it
                        },
                    )
                }
                TeakSettingsCard(title = "布局与效果", modifier = Modifier.fillMaxWidth()) {
                    TeakSettingsSwitchRow(
                        title = "抬高底栏",
                        description = "解决未开启安卓导航条时底栏位置过低",
                        checked = settings.bottomBarRaise,
                        onCheckedChange = { settings.bottomBarRaise = it },
                    )
                    TeakSettingsSwitchRow(
                        title = "回到顶部控件",
                        checked = settings.scrollTopEnabled,
                        onCheckedChange = { settings.scrollTopEnabled = it },
                    )
                    TeakSettingsSwitchRow(
                        title = "模糊效果",
                        description = "关闭后玻璃控件改为纯色，低端机更流畅",
                        checked = settings.blurEnabled,
                        onCheckedChange = { settings.blurEnabled = it },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            GuidePrimaryPill(
                text = "继续",
                modifier = Modifier.widthIn(max = 336.dp).fillMaxWidth(),
                onClick = onStart,
            )
            Spacer(Modifier.height(44.dp))
    }
}

/**
 * 引导步骤的统一外壳：整页背景 + backdrop 采集 + 与其他页面同款的玻璃返回键。
 *
 * 返回键用 [TeakTopOverlay]，和设置页、收藏页、视频页是同一个组件、同一套 40dp 尺寸、
 * 同一块玻璃 recipe 和 300ms 渐显，不再单独画一个圆。
 */
@Composable
private fun GuidePageFrame(
    onBack: (() -> Unit)?,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val backdrop = rememberLayerBackdrop {
        drawRect(colors.background)
        drawContent()
    }
    CompositionLocalProvider(LocalTeakBackdrop provides backdrop) {
        Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .statusBarsPadding()
                    .padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
            if (onBack != null) {
                TeakTopOverlay(revealed = true, onBack = onBack)
            }
        }
    }
}

/** App 里通用的主题色胶囊按钮：纯色底 + 白色高光描边，和关注、保存二维码是同一套。 */
@Composable
private fun GuidePrimaryPill(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(Capsule())
            .background(if (enabled) colors.primary else colors.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (enabled) {
                    Color.White.copy(alpha = 0.55f)
                } else {
                    Color.White.copy(alpha = 0.22f)
                },
                shape = Capsule(),
            )
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) Color.White else colors.onBackgroundVariant,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 使用条款弹层
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TermsSheet(show: Boolean, onDismiss: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    // 和归元同一个 Miuix 底部弹层：从底部弹出、顶部有拖动条和标题、可下拉/点外面收起。
    OverlayBottomSheet(
        show = show,
        modifier = Modifier.fillMaxHeight(0.85f),
        title = "使用条款",
        backgroundColor = colors.background,
        insideMargin = DpSize(0.dp, 0.dp),
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        ) {
            TERMS_SECTIONS.forEach { section ->
                Text(
                    text = section,
                    color = colors.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 涟漪 / 反光（与归元同一套几何，保证长按和文字上扫过的光一致）
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawWave(
    color: Color,
    progress: Float,
    strength: Float,
    diameter: Float,
    phase: Float,
) {
    if (progress <= 0f || progress >= 1f) return

    val radius = diameter * (0.55f + 1.15f * progress)
    val feather = diameter * (0.20f + 0.07f * progress)
    val edge = radius + feather
    val turn = phase - 0.55f * progress
    val shift = diameter * 0.025f * progress
    val front = center + Offset(cos(turn) * shift, sin(turn) * shift)
    val alpha = strength * sin((PI * progress).toFloat()).coerceAtLeast(0f)

    drawCircle(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            ((radius - feather) / edge) to Color.Transparent,
            ((radius - 0.55f * feather) / edge) to color.copy(alpha = alpha * 0.15f),
            (radius / edge) to color.copy(alpha = alpha * 0.52f),
            ((radius + 0.55f * feather) / edge) to color.copy(alpha = alpha * 0.19f),
            1f to Color.Transparent,
            center = front,
            radius = edge,
        ),
        radius = edge,
        center = front,
    )
}

private fun flowRadius(progress: Float, angle: Float, diameter: Float, phase: Float): Float {
    val radius = diameter * (0.55f + 1.15f * progress)
    val shift = diameter * 0.025f * progress
    return radius + shift * cos(angle - (phase - 0.55f * progress))
}

private fun Modifier.waveGlint(
    wave: Animatable<Float, AnimationVector1D>,
    distance: Float,
    diameter: Float,
    phase: Float,
    glint: Color,
    accent: Color,
): Modifier = drawWithContent {
    drawContent()
    if (!wave.isRunning || distance <= 0f) return@drawWithContent
    val progress = wave.value
    drawGlint(progress, distance, diameter, phase, glint, accent, 0.95f)
    drawGlint((progress - 0.2f) / 0.8f, distance, diameter, phase, glint, accent, 0.50f)
}

private fun DrawScope.drawGlint(
    progress: Float,
    distance: Float,
    diameter: Float,
    phase: Float,
    glint: Color,
    accent: Color,
    strength: Float,
) {
    if (progress <= 0f || progress >= 1f) return
    val radius = diameter * (0.55f + 1.15f * progress)
    val feather = diameter * (0.20f + 0.07f * progress)
    val edge = radius + feather
    val turn = phase - 0.55f * progress
    val shift = diameter * 0.025f * progress
    val front = Offset(
        size.width / 2f + cos(turn) * shift,
        size.height / 2f - distance + sin(turn) * shift,
    )
    val intensity = strength * sin((PI * progress).toFloat()).coerceAtLeast(0f)
    drawRect(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            ((radius - feather) / edge) to Color.Transparent,
            ((radius - 0.55f * feather) / edge) to accent.copy(alpha = intensity * 0.22f),
            (radius / edge) to glint.copy(alpha = intensity),
            ((radius + 0.55f * feather) / edge) to accent.copy(alpha = intensity * 0.32f),
            1f to Color.Transparent,
            center = front,
            radius = edge,
        ),
        blendMode = BlendMode.SrcAtop,
    )
}

private fun waveLift(progress: Float, distance: Float, diameter: Float, phase: Float): Float {
    if (distance <= 0f || diameter <= 0f) return 0f
    return maxOf(
        wavefrontLift(progress, distance, diameter, phase),
        wavefrontLift((progress - 0.2f) / 0.8f, distance, diameter, phase) * 0.6f,
    )
}

private fun wavefrontLift(progress: Float, distance: Float, diameter: Float, phase: Float): Float {
    if (progress <= 0f || progress >= 1f) return 0f
    val radius = flowRadius(progress, (PI / 2).toFloat(), diameter, phase)
    val feather = diameter * (0.20f + 0.07f * progress)
    val crossing = (1f - abs(distance - radius) / feather).coerceIn(0f, 1f)
    return crossing * sin((PI * progress).toFloat())
}
