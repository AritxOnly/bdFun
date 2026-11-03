package com.aritxonly.bdfun.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
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

/** 输入框统一字体：去掉字体上下不对称的 padding，光标才能真的在垂直中心。 */
val ComposerTextStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 16.sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

/**
 * 评论页底部回复栏：**和首页顶栏完全同一套组件**
 * （TeakGlassSurface 胶囊 + TeakGlassIconButton 圆钮），由 VideoActivity 画在采集层之外。
 *
 * 交互：点输入框进入编辑 → 框内右侧渐变出现「图片」「发送」；点空白处（Activity 的点击层）收起。
 * 键盘/表情面板切换时，整条栏用 [rememberSmoothFloat] 补间，中途打断也会从当前位置接着走。
 */
@Composable
fun CommentReplyBar(modifier: Modifier = Modifier) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val backdrop = LocalTeakBackdrop.current
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var pendingPictures by remember { mutableStateOf<String?>(null) }
    var pendingPreview by remember { mutableStateOf<android.net.Uri?>(null) }
    var failed by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeDp = with(density) { imeBottom.toDp().value }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    val raiseDp = if (com.aritxonly.bdfun.AppGraph.settings.bottomBarRaise) {
        TeakChromeDefaults.RaisedBottomBarExtra.value
    } else {
        0f
    }
    // 跟随系统 IME inset（不做自定义动画）；未开导航条时按设置额外抬高。
    val animatedBottom = if (imeDp > navDp) imeDp else navDp + raiseDp

    fun dismiss() {
        showEmoji = false
        editing = false
        focusManager.clearFocus()
        keyboard?.hide()
        VideoPageBus.replyEditing = false
        if (draft.isBlank()) {
            ReplyTarget.clear()
            pendingPictures = null
            pendingPreview = null
        }
    }

    // 点某条评论的「回复」→ 主动聚焦。
    // 注意：不能在本 effect 里把 key 改回 0，否则 effect 会被立刻取消。
    LaunchedEffect(VideoPageBus.requestReplyFocus) {
        val tick = VideoPageBus.requestReplyFocus
        if (tick == 0 || tick == VideoPageBus.consumedReplyFocus) return@LaunchedEffect
        VideoPageBus.consumedReplyFocus = tick
        delay(60)
        runCatching { focusRequester.requestFocus() }
        editing = true
        VideoPageBus.replyEditing = true
    }
    // Activity 的空白点击层要求收起
    LaunchedEffect(VideoPageBus.dismissReply) {
        val tick = VideoPageBus.dismissReply
        if (tick == 0 || tick == VideoPageBus.consumedDismissReply) return@LaunchedEffect
        VideoPageBus.consumedDismissReply = tick
        dismiss()
    }
    DisposableEffect(Unit) {
        onDispose { VideoPageBus.replyEditing = false }
    }

    val actionsAlpha = rememberSmoothFloat(if (editing) 1f else 0f, 180f)

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingPreview = uri
        uploading = true
        failed = false
        val api = com.aritxonly.bdfun.AppGraph.api
        scope.launch {
            val json = runCatching { api.uploadCommentImage(uri) }.getOrNull()
            uploading = false
            if (json == null) {
                pendingPreview = null
                failed = true
            } else {
                pendingPictures = json
            }
        }
    }

    fun send() {
        val value = draft.trim()
        if ((value.isEmpty() && pendingPictures == null) || sending || uploading) return
        val sender = VideoPageBus.commentSender
        if (sender == null) {
            failed = true
            return
        }
        sending = true
        failed = false
        scope.launch {
            val ok = runCatching { sender(value, pendingPictures) }.getOrDefault(false)
            sending = false
            if (ok) {
                draft = ""
                pendingPictures = null
                pendingPreview = null
                dismiss()
            } else {
                failed = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = animatedBottom.dp, start = 12.dp, end = 12.dp)
            .padding(vertical = 8.dp),
    ) {
        if (showEmoji) {
            EmojiPanel(
                api = com.aritxonly.bdfun.AppGraph.api,
                onPick = { draft = draft + it },
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
                            .padding(
                                start = 16.dp,
                                end = (8f + 64f * actionsAlpha +
                                    (if (pendingPreview != null) 30f else 0f)).dp,
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            textStyle = ComposerTextStyle.copy(color = colors.onBackground),
                            cursorBrush = SolidColor(BrandPink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        editing = true
                                        VideoPageBus.replyEditing = true
                                    } else if (!showEmoji) {
                                        editing = false
                                        VideoPageBus.replyEditing = false
                                    }
                                },
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (draft.isEmpty()) {
                                        Text(
                                            text = ReplyTarget.name?.let { "回复 @$it：" }
                                                ?: "发一条友善的评论",
                                            color = colors.outline,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                            .graphicsLayer { alpha = actionsAlpha }
                            .blockTouches(actionsAlpha < 0.99f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (pendingPreview != null) {
                            AsyncImage(
                                model = pendingPreview,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(colors.surfaceContainerHigh),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        ComposerIconButton(
                            kind = ComposerIcon.Image,
                            tint = colors.onBackgroundVariant,
                            onClick = { imagePicker.launch("image/*") },
                        )
                        Spacer(Modifier.width(1.dp))
                        ComposerIconButton(
                            kind = ComposerIcon.Send,
                            tint = if ((draft.isNotBlank() || pendingPictures != null) &&
                                !sending && !uploading
                            ) {
                                BrandPink
                            } else {
                                colors.outline
                            },
                            onClick = { send() },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            TeakGlassIconButton(
                onClick = {
                    showEmoji = !showEmoji
                    // 点表情时保持/唤起键盘，也不取消输入状态
                    editing = true
                    VideoPageBus.replyEditing = true
                    runCatching { focusRequester.requestFocus() }
                },
                size = 46.dp,
                recipe = recipe,
                backdrop = backdrop,
            ) {
                Text(text = "\uD83D\uDE00", fontSize = 19.sp)
            }
        }
        if (failed || uploading) {
            Text(
                text = if (uploading) "图片上传中…" else "发送失败，请确认已登录后重试",
                color = if (uploading) colors.onBackgroundVariant else MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 16.dp, top = 5.dp),
            )
        }
    }
}

enum class ComposerIcon { Send, Image }

/** 输入框内的两个小按钮：尺寸完全一致，按压时统一变粉。 */
@Composable
fun ComposerIconButton(
    kind: ComposerIcon,
    tint: Color,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val color = if (pressed) BrandPink else tint
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(Capsule())
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(
                if (kind == ComposerIcon.Send) TeakIcon.Send else TeakIcon.Image,
            ),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color),
            modifier = Modifier.size(19.dp),
        )
    }
}
