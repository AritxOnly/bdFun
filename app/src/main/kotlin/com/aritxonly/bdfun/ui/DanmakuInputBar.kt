package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.bdfun.BrandPink
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 视频页「点我发弹幕」的输入条。
 * 和评论回复框用的是同一套玻璃 + 同样的高度/圆角/图标，视觉上完全统一。
 */
@Composable
fun DanmakuInputBar(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()

    // 渐显 / 渐隐：进来先置 true，关闭时先归零再回调 onDismiss
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 先画一帧（alpha=0），再开始渐显，避免首帧直接以 1 出现
        androidx.compose.runtime.withFrameNanos { }
        shown = true
    }
    val reveal = rememberSmoothFloat(if (shown) 1f else 0f, 280f, EaseOutQuint)

    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var showEmoji by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val density = LocalDensity.current
    val imeDp = with(density) {
        WindowInsets.ime.getBottom(density).toDp().value
    }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    // 跟随系统 IME inset（不做自定义动画）
    val animatedBottom = if (imeDp > navDp) imeDp else navDp
    // 关闭渐隐期间锁住位置：否则 keyboard.hide() 会让 imeDp 立刻变 0，
    // 输入条会在淡出过程中先跳到屏幕底部，看起来就是"抽搐"。
    var frozenBottom by remember { mutableStateOf<Float?>(null) }
    val bottomDp = frozenBottom ?: animatedBottom

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { focusRequester.requestFocus() }
    }

    fun dismiss() {
        if (!shown) return
        shown = false
        frozenBottom = bottomDp
        focusManager.clearFocus()
        keyboard?.hide()
        // 等渐隐动画走完再真正移除
        scope.launch {
            delay(200)
            onDismiss()
        }
    }

    // 外部（点空白处）请求关闭：只看本次打开后新增的请求，避免第二次点不开
    val closeTokenAtOpen = remember { VideoPageBus.danmakuInputClose }
    LaunchedEffect(VideoPageBus.danmakuInputClose) {
        if (VideoPageBus.danmakuInputClose == closeTokenAtOpen) return@LaunchedEffect
        dismiss()
    }

    fun send() {
        val value = text.trim()
        if (value.isEmpty() || sending) return
        val sender = VideoPageBus.danmakuSender
        if (sender == null) {
            failed = true
            return
        }
        sending = true
        failed = false
        scope.launch {
            val ok = runCatching { sender(value, 1, 25, 0xFFFFFF) }.getOrDefault(false)
            sending = false
            if (ok) {
                text = ""
                dismiss()
            } else {
                failed = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - reveal) * 22f
                val s = 0.985f + 0.015f * reveal
                scaleX = s
                scaleY = s
            }
            .padding(bottom = bottomDp.dp, start = 12.dp, end = 12.dp)
            .padding(vertical = 8.dp),
    ) {
        if (showEmoji) {
            EmojiPanel(
                api = com.aritxonly.bdfun.AppGraph.api,
                onPick = { text = text + it },
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeakGlassSurface(
                modifier = Modifier.weight(1f).height(46.dp),
                shape = Capsule(),
                recipe = recipe,
                backdrop = backdrop,
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 46.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = text,
                            onValueChange = { text = it },
                            singleLine = true,
                            textStyle = ComposerTextStyle.copy(color = colors.onBackground),
                            cursorBrush = SolidColor(BrandPink),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { send() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (text.isEmpty()) {
                                        Text(
                                            text = "发一条友善的弹幕",
                                            color = colors.outline,
                                            fontSize = 13.sp,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ComposerIconButton(
                            kind = ComposerIcon.Send,
                            tint = if (text.isNotBlank() && !sending) BrandPink else colors.outline,
                            onClick = { send() },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            TeakGlassIconButton(
                onClick = {
                    showEmoji = !showEmoji
                    runCatching { focusRequester.requestFocus() }
                },
                size = 46.dp,
                recipe = recipe,
                backdrop = backdrop,
            ) {
                Text(text = "\uD83D\uDE00", fontSize = 19.sp)
            }
        }
        if (failed) {
            Text(
                text = "发送失败，请确认已登录后重试",
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 16.dp, top = 5.dp),
            )
        }
    }
}
