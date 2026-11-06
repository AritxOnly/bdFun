package com.aritxonly.teak.common.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.colorControls
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 收藏页顶栏省略号的玻璃菜单宿主。
 *
 * 运动、轮廓与来源接管来自 BiliPai 的 GPL-3.0 实现（见 THIRD_PARTY_NOTICES.md），
 * 玻璃颜色和模糊参数使用 bdFun 当前主题，不照搬 BiliPai 的配色。
 */
@Stable
class TeakMorphMenuState {
    var open by mutableStateOf(false)
    var anchor by mutableStateOf(Rect.Zero)
    var sourceLayer by mutableStateOf<GraphicsLayer?>(null)
}

private const val MorphLogTag = "TeakLibraryMorph"

private fun logEvent(message: String) {
    Log.d(MorphLogTag, message)
}

@Composable
fun TeakMorphMenuAnchor(
    state: TeakMorphMenuState,
    content: @Composable (onClick: () -> Unit) -> Unit,
) {
    val layer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    SideEffect {
        state.sourceLayer = layer
    }
    Box(
        modifier = Modifier
            .onGloballyPositioned { state.anchor = it.boundsInWindow() }
            .drawWithContent {
                layer.record { this@drawWithContent.drawContent() }
                if (!state.open) drawLayer(layer)
            },
    ) {
        content {
            logEvent("trigger click anchor=${state.anchor} layerReleased=${layer.isReleased}")
            state.open = true
        }
    }
}

private class MenuMorph {
    var opening by mutableStateOf(true)
        private set
    var seconds by mutableFloatStateOf(0f)
        private set
    var session by mutableIntStateOf(0)
        private set
    var closeStart by mutableStateOf<TeakMorphFrame?>(null)
    var previous by mutableStateOf<TeakMorphFrame?>(null)
    var sampleSeconds by mutableFloatStateOf(1f / 120f)
    var interrupted by mutableStateOf(false)
    var closeStartSeconds by mutableFloatStateOf(TeakMorphMotion.OpenSeconds)
    var card by mutableStateOf(Rect.Zero)
    var anchor by mutableStateOf(Rect.Zero)

    fun beginClose(frame: TeakMorphFrame, previousFrame: TeakMorphFrame) {
        if (!opening) return
        closeStart = frame
        previous = previousFrame
        sampleSeconds = seconds.coerceIn(0.0001f, 1f / 120f)
        interrupted = seconds < TeakMorphMotion.OpenSeconds
        closeStartSeconds = seconds
        opening = false
        seconds = 0f
        session++
    }

    fun advanceTo(value: Float, expectedSession: Int = session): Boolean {
        if (expectedSession != session) return false
        seconds = value.coerceAtLeast(0f)
        return true
    }

    val closeDuration: Float get() = TeakMorphMotion.ReturnSeconds

    fun frame(radius: Float, density: Float): TeakMorphFrame =
        frameAt(seconds, radius, density)

    fun frameAt(time: Float, radius: Float, density: Float): TeakMorphFrame {
        if (card.isEmpty) return TeakMorphFrame(card, 0f, 1f, 0f, 0f)
        val close = closeStart?.takeUnless { it.rect.isEmpty }
        val base = when {
            opening -> TeakMorphMotion.opening(
                time,
                card,
                anchor,
                radius,
                sourceControl = true,
                density = density,
            )

            interrupted && close != null -> {
                val duration = TeakMorphMotion.ReturnSeconds
                val target = TeakMorphMotion.closing(
                    duration,
                    closeStartSeconds,
                    card,
                    anchor,
                    radius,
                    sourceControl = true,
                    density = density,
                    startFrame = close,
                    returnToControl = true,
                )
                TeakMorphMotion.retarget(
                    time,
                    duration,
                    close,
                    previous ?: close,
                    sampleSeconds,
                    target,
                )
            }

            else -> TeakMorphMotion.closing(
                time,
                closeStartSeconds,
                card,
                anchor,
                radius,
                sourceControl = true,
                density = density,
                startFrame = close,
                returnToControl = true,
            )
        }
        return base.copy(radius = base.radius.coerceIn(0f, max(0f, base.rect.minDimension / 2f)))
    }
}

@Composable
fun TeakMorphMenuHost(
    state: TeakMorphMenuState,
    content: @Composable (dismiss: (after: () -> Unit) -> Unit) -> Unit,
) {
    if (!state.open || state.anchor.isEmpty) return

    val morph = remember { MenuMorph() }
    val sourceBlurCache = remember { BlurCache() }
    val contentBlurCache = remember { BlurCache() }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    SideEffect {
        morph.anchor = state.anchor
    }

    val density = LocalDensity.current
    val densityScale = density.density
    val cornerRadiusPx = with(density) { 24.dp.toPx() }
    val dark = MiuixTheme.colorScheme.background.luminance() < 0.5f

    val requestClose: () -> Unit = {
        if (morph.opening) {
            logEvent("requestClose seconds=${morph.seconds} card=${morph.card}")
            val frame = morph.frame(cornerRadiusPx, densityScale)
            val previous = morph.frameAt(
                (morph.seconds - 1f / 120f).coerceAtLeast(0f),
                cornerRadiusPx,
                densityScale,
            )
            morph.beginClose(frame, previous)
        }
    }
    val dismissWith: (() -> Unit) -> Unit = { action ->
        if (morph.opening) {
            pendingAction = action
            requestClose()
        }
    }

    BackHandler(enabled = true) { requestClose() }

    LaunchedEffect(morph.session, morph.card) {
        if (morph.card.isEmpty) return@LaunchedEffect
        val session = morph.session
        val duration = if (morph.opening) {
            TeakMorphMotion.OpenSeconds
        } else {
            morph.closeDuration
        }
        val start = withFrameNanos { it }
        logEvent(
            "clock start session=$session opening=${morph.opening} duration=$duration card=${morph.card}",
        )
        var lastLog = -1f
        var lastEvent = -1f
        while (true) {
            val elapsed = (withFrameNanos { it } - start) / 1_000_000_000f
            if (!morph.advanceTo(elapsed.coerceAtMost(duration), session)) return@LaunchedEffect
            if (elapsed - lastLog >= 0.06f || elapsed >= duration) {
                lastLog = elapsed
                val f = morph.frame(cornerRadiusPx, densityScale)
                Log.d(
                    MorphLogTag,
                    "frame t=${"%.3f".format(elapsed)} phase=${if (morph.opening) "open" else "close"} " +
                        "rect=${f.rect} r=${f.radius} scale=${f.contentScale} " +
                        "alpha=${f.contentAlpha} srcA=${f.sourceAlpha} mat=${f.materialProgress} " +
                        "blend=${f.surfaceBlend}",
                )
            }
            if (elapsed - lastEvent >= 0.09f || elapsed >= duration) {
                lastEvent = elapsed
                val f = morph.frame(cornerRadiusPx, densityScale)
                logEvent(
                    "frame t=${"%.3f".format(elapsed)} phase=${if (morph.opening) "open" else "close"} " +
                        "rect=${f.rect} scale=${f.contentScale} alpha=${f.contentAlpha} " +
                        "srcA=${f.sourceAlpha} mat=${f.materialProgress} blend=${f.surfaceBlend}",
                )
            }
            if (elapsed >= duration) break
        }
        if (session != morph.session) return@LaunchedEffect
        logEvent("clock done session=$session opening=${morph.opening} seconds=${morph.seconds}")
        if (!morph.opening) {
            val action = pendingAction
            pendingAction = null
            state.open = false
            action?.invoke()
        }
    }

    val frame = morph.frame(cornerRadiusPx, densityScale)
    val sourceAvailable = state.sourceLayer?.isReleased == false

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInWindow() }
            .pointerInput(Unit) { detectTapGestures { requestClose() } },
    ) {
        val margin = 8.dp
        val marginPx = with(density) { margin.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val menuWidthPx = with(density) { 196.dp.toPx() }
        val anchorLocal = state.anchor.translate(-overlayOrigin)
        val x = (anchorLocal.right - menuWidthPx)
            .coerceIn(marginPx, (maxWidthPx - marginPx - menuWidthPx).coerceAtLeast(marginPx))
        val y = (anchorLocal.top - with(density) { 8.dp.toPx() })
            .coerceIn(marginPx, (maxHeightPx - marginPx).coerceAtLeast(marginPx))
        val maxMenuHeightPx = (maxHeightPx - y - marginPx).coerceAtLeast(1f)

        val ownership = popupMaterialOwnership(frame, true)
        if (!frame.rect.isEmpty) {
            val radiusDp = with(density) { frame.radius.toDp() }
            val rectShape = androidx.compose.foundation.shape.RoundedCornerShape(radiusDp)
            val panelWidth = with(density) { frame.rect.width.toDp() }
            val panelHeight = with(density) { frame.rect.height.toDp() }
            // 阴影：用和面板一致的圆角，跟着形变一起缩放。
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (frame.rect.left - overlayOrigin.x).roundToInt(),
                            (frame.rect.top - overlayOrigin.y).roundToInt(),
                        )
                    }
                    .size(panelWidth, panelHeight)
                    .graphicsLayer {
                        alpha = frame.surfaceAlpha * ownership
                        shape = rectShape
                        clip = false
                        shadowElevation = with(density) { 12.dp.toPx() }
                        ambientShadowColor = Color.Black.copy(alpha = if (dark) 0.30f else 0.16f)
                        spotShadowColor = ambientShadowColor
                    },
            )
            // 独立的玻璃连接颈：不改底层，只在按钮和菜单之间补一小段胶囊玻璃。
            // 它画在主面板下面，所以只露出中间那一小段。
            val neckSource = frame.sourceRect
            val neckBlend = frame.surfaceBlend
            if (neckSource != null && neckBlend > 1f && frame.sourceAlpha > 0.01f) {
                val dx = frame.rect.center.x - neckSource.center.x
                val dy = frame.rect.center.y - neckSource.center.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance > 2f) {
                    val thickness = neckBlend.coerceAtMost(with(density) { 24.dp.toPx() })
                    val angle = kotlin.math.atan2(dy, dx) * 180f / 3.1415927f
                    com.aritxonly.teak.common.material.TeakGlassPanel(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (
                                        (neckSource.center.x + frame.rect.center.x) / 2f -
                                            (distance + thickness) / 2f - overlayOrigin.x
                                        ).roundToInt(),
                                    (
                                        (neckSource.center.y + frame.rect.center.y) / 2f -
                                            thickness / 2f - overlayOrigin.y
                                        ).roundToInt(),
                                )
                            }
                            .size(
                                with(density) { (distance + thickness).toDp() },
                                with(density) { thickness.toDp() },
                            )
                            .graphicsLayer {
                                rotationZ = angle
                                alpha = frame.surfaceAlpha * ownership * frame.sourceAlpha
                            },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                        tintAlpha = 0.3f,
                    ) {}
                }
            }
            // 这里直接用 TeakUI 自己的玻璃面板，和收藏页旧菜单同一套配方；
            // 不再用自绘的联合轮廓，避免某些设备上 drawBackdrop 对自定义路径不生效。
            com.aritxonly.teak.common.material.TeakGlassPanel(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (frame.rect.left - overlayOrigin.x).roundToInt(),
                            (frame.rect.top - overlayOrigin.y).roundToInt(),
                        )
                    }
                    .size(panelWidth, panelHeight)
                    .graphicsLayer { alpha = frame.surfaceAlpha * ownership },
                shape = rectShape,
                tintAlpha = 0.3f,
            ) {}
        }

        // 按钮快照独立于卡片测量：即使测量晚一帧，也不会出现按钮凭空消失。
        val layer = state.sourceLayer
        if (layer != null) {
            val emptyCard = morph.card.isEmpty
            val target = frame.sourceRect ?: if (emptyCard) state.anchor else null
            val sourceAlpha = if (emptyCard) 1f else frame.sourceAlpha
            if (target != null && sourceAlpha > 0.01f && sourceAvailable) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = target.width / state.anchor.width.coerceAtLeast(1f)
                            scaleY = target.height / state.anchor.height.coerceAtLeast(1f)
                            translationX = target.left - overlayOrigin.x
                            translationY = target.top - overlayOrigin.y
                            alpha = sourceAlpha
                            renderEffect = if (frame.sourceBlur > 0.1f && !emptyCard) {
                                sourceBlurCache.resolve(frame.sourceBlur)
                            } else {
                                null
                            }
                        },
                ) {
                    if (!layer.isReleased && sourceAvailable) drawLayer(layer)
                }
            }
        }

        // 正文最后画，保证在玻璃和按钮快照之上；尺寸也在这里写回动画状态。
        Layout(
            content = {
                MenuSurface(
                    morph = morph,
                    frame = frame,
                    content = content,
                    dismissWith = dismissWith,
                    cornerRadiusPx = cornerRadiusPx,
                    densityScale = densityScale,
                    contentBlurCache = contentBlurCache,
                )
            },
        ) { measurables, constraints ->
            val placeable = measurables.first().measure(
                Constraints(
                    minWidth = menuWidthPx.roundToInt(),
                    maxWidth = menuWidthPx.roundToInt(),
                    minHeight = 0,
                    maxHeight = maxMenuHeightPx.roundToInt(),
                ),
            )
            val width = placeable.width
            val height = placeable.height
            morph.card = Rect(
                overlayOrigin.x + x,
                overlayOrigin.y + y,
                overlayOrigin.x + x + width,
                overlayOrigin.y + y + height,
            )
            logEvent("measure card=${morph.card}")
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(x.roundToInt(), y.roundToInt())
            }
        }
    }
}

@Composable
private fun MenuSurface(
    morph: MenuMorph,
    frame: TeakMorphFrame,
    content: @Composable (dismiss: (after: () -> Unit) -> Unit) -> Unit,
    dismissWith: (() -> Unit) -> Unit,
    cornerRadiusPx: Float,
    densityScale: Float,
    contentBlurCache: BlurCache,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val card = morph.card
                if (card.isEmpty) {
                    alpha = 0f
                    return@graphicsLayer
                }
                val scale = frame.contentScale
                scaleX = scale
                scaleY = scale
                translationX = frame.rect.center.x - card.center.x
                translationY = frame.rect.center.y - card.center.y
                transformOrigin = TransformOrigin.Center
                alpha = frame.contentAlpha
                renderEffect = if (frame.contentBlur > 0.1f) {
                    contentBlurCache.resolve(frame.contentBlur)
                } else {
                    null
                }
            }
            .graphicsLayer {
                clip = true
                shape = TeakMorphFrameShape(frame, morph.card, true)
            }
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content(dismissWith)
        }
    }
}

private data class TeakMorphFrameShape(
    private val frame: TeakMorphFrame,
    private val card: Rect,
    private val content: Boolean,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (card.isEmpty) return Outline.Rectangle(Rect(Offset.Zero, size))
        val (rect, radius) = glassPopupLocalOutline(frame, card, size, content)
        return Outline.Rounded(
            RoundRect(
                rect,
                if (content) {
                    CornerRadius(radius / frame.contentStretchX, radius / frame.contentStretchY)
                } else {
                    CornerRadius(radius)
                },
            ),
        )
    }
}

private class BlurCache {
    private var radius = Float.NaN
    private var effect: BlurEffect? = null

    fun resolve(value: Float): BlurEffect {
        val cached = effect
        if (cached != null && kotlin.math.abs(radius - value) < 0.05f) return cached
        return BlurEffect(value, value, TileMode.Clamp).also {
            effect = it
            radius = value
        }
    }
}

