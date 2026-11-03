package com.aritxonly.bdfun.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.telephony.TelephonyManager
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.data.FavoriteFolder
import com.aritxonly.bdfun.data.DanmakuItem
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.bdfun.data.SubtitleCue
import com.aritxonly.bdfun.data.SubtitleTrack
import com.aritxonly.bdfun.data.VideoPage
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** 全屏二级面板。所有面板统一从右侧滑出，背景是从左到右加深的黑色渐变。 */
internal enum class FullscreenPanel {
    None,
    Favorite,
    More,
    Danmaku,
    Episodes,
    Speed,
    Quality,
    Subtitles,
    Cache,
}

internal val FullscreenSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

internal data class FullscreenPlayerInfo(
    val title: String,
    val author: String,
    val authorFace: String,
    val onlineText: String,
    val onlineCount: Long,
    val followed: Boolean = false,
    val liked: Boolean,
    val likeCount: Long,
    val replyCount: Long,
    val favorited: Boolean,
    val favoriteCount: Long,
    val shareCount: Long,
    val qualityLabel: String,
    val cover: String,
    val pages: List<VideoPage>,
    val currentCid: Long,
    val folders: List<FavoriteFolder>,
    val folderIds: Set<Long>,
    val loggedIn: Boolean,
    val subtitles: List<SubtitleTrack>,
    val selectedSubtitle: SubtitleTrack?,
    val subtitleCues: List<SubtitleCue>,
    val danmakuItems: List<DanmakuItem> = emptyList(),
)

internal data class FullscreenPlaybackState(
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val bufferedMs: Long = 0L,
    val speed: Float,
    val qualities: List<Quality>,
    val qualityId: Int,
    val autoQuality: Boolean = false,
    val danmakuEnabled: Boolean,
    val autoPlayNext: Boolean,
    val loopMode: Int,
    val mirror: Boolean,
    val resizeMode: Int,
    val colorAssist: Int,
    val audioOnly: Boolean,
    val sleepAtMs: Long,
    val backgroundPlay: Boolean,
    val seekLook: Float = 0f,
    val scrubbing: Boolean = false,
)

internal class FullscreenActions(
    val onBack: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onEpisodes: () -> Unit,
    val onLike: () -> Unit,
    val onComment: () -> Unit,
    val onShare: () -> Unit,
    val onOpenSpace: () -> Unit,
    val onToggleFolder: (Long) -> Unit,
    val onRequestFavorites: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onQualitySelected: (Int) -> Unit,
    val onOpenCachePanel: () -> Unit,
    val onCacheSelected: (Quality) -> Unit,
    val onSpeedSelected: (Float) -> Unit,
    val onPageSelected: (VideoPage) -> Unit,
    val onSubtitleSelected: (SubtitleTrack?) -> Unit,
    val onSendDanmaku: () -> Unit,
    val onToggleDanmaku: () -> Unit,
    val onAddWatchLater: () -> Unit,
    val onPictureInPicture: () -> Unit,
    val onCast: () -> Unit,
    val onAudioOnlyChanged: (Boolean) -> Unit,
    val onAutoPlayNextChanged: (Boolean) -> Unit,
    val onLoopModeChanged: (Int) -> Unit,
    val onMirrorChanged: (Boolean) -> Unit,
    val onResizeModeChanged: (Int) -> Unit,
    val onColorAssistChanged: (Int) -> Unit,
    val onSleepAtChanged: (Long) -> Unit,
    val onBackgroundPlayChanged: (Boolean) -> Unit,
    val onSeekRelative: (Long) -> Unit,
)

@Composable
internal fun FullscreenPlayerChrome(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    panel: FullscreenPanel,
    onPanelChanged: (FullscreenPanel) -> Unit,
    modifier: Modifier = Modifier,
    scrubOnly: Boolean = false,
    onScrubStart: () -> Unit = {},
    onScrubEnd: () -> Unit = {},
    onScrubLook: (Float) -> Unit = {},
    onScrubPreview: (Long) -> Unit = {},
    onPanelHidden: () -> Unit = {},
) {
    if (playback.audioOnly) {
        FullscreenAudioMode(
            info = info,
            playback = playback,
            onBack = { actions.onAudioOnlyChanged(false) },
            onPlayPause = actions.onPlayPause,
            onSeek = actions.onSeek,
            onSeekRelative = actions.onSeekRelative,
            modifier = modifier,
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val edge = 42.dp
        var lastPanel by remember { mutableStateOf(panel) }
        var panelVisible by remember { mutableStateOf(panel != FullscreenPanel.None) }
        LaunchedEffect(panel) {
            if (panel != FullscreenPanel.None) {
                lastPanel = panel
                panelVisible = true
            } else {
                if (panelVisible) {
                    delay(264L)
                    panelVisible = false
                    onPanelHidden()
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (!panelVisible && !scrubOnly) 1f else 0f
                },
        ) {
        // 顶部 / 底部压暗，保证白字在亮画面上仍可读（参考图一有同款渐层）
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(190.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.62f),
                        0.5f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Transparent,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(235.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.82f),
                    ),
                ),
        )
        // 最顶：时间居中，右侧网络 + 电量
        if (AppGraph.settings.showFullscreenTime) {
            FullscreenStatusLine(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = edge, vertical = 0.dp),
            )
        }

        // 顶部操作行：返回 / 标题 / 点赞 / 评论 / 收藏 / 分享 / 更多
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, end = 24.dp, top = 12.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        actions.onBack()
                    },
                contentAlignment = Alignment.Center,
            ) {
                BackChevron()
            }
            Spacer(Modifier.width(3.dp))
            Text(
                text = info.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (AppGraph.settings.showFullscreenActions) {
                Spacer(Modifier.width(12.dp))
                BadgeStatAction(
                    icon = TeakIcon.PlayerLike,
                    count = info.likeCount,
                    active = info.liked,
                    yOffset = (-10).dp,
                    xOffset = (-7).dp,
                    countXOffset = 8.dp,
                    countYOffset = 11.dp,
                    onClick = actions.onLike,
                )
                Spacer(Modifier.width(16.dp))
                BadgeStatAction(
                    icon = TeakIcon.PlayerComment,
                    count = info.replyCount,
                    xOffset = (-4).dp,
                    countXOffset = 6.dp,
                    countYOffset = 8.dp,
                    onClick = actions.onComment,
                )
                Spacer(Modifier.width(16.dp))
                BadgeStatAction(
                    icon = TeakIcon.PlayerStar,
                    count = info.favoriteCount,
                    active = info.favorited,
                    xOffset = 0.dp,
                    countXOffset = 5.dp,
                    countYOffset = 8.dp,
                    onClick = {
                        onPanelChanged(FullscreenPanel.Favorite)
                        actions.onRequestFavorites()
                    },
                )
                Spacer(Modifier.width(16.dp))
                IconOnlyAction(
                    icon = TeakIcon.PlayerShare,
                    xOffset = 1.dp,
                    onClick = actions.onShare,
                )
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(width = 30.dp, height = 40.dp)
                        .offset(y = (-7).dp)
                        .clickable(interactionSource = null, indication = null) {
                            onPanelChanged(FullscreenPanel.More)
                        },
                    contentAlignment = Alignment.BottomStart,
                ) {
                    MoreGlyph()
                }
            }
        }

        // 观看人数 + UP 头像 / 昵称
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 50.dp, top = 55.dp),
        ) {
            if (AppGraph.settings.showViewerCount) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset(x = 29.dp),
                ) {
                    ViewerGlyph()
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = viewerText(info),
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(26.dp)
                    .clip(Capsule())
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(interactionSource = null, indication = null) {
                        actions.onOpenSpace()
                    },
            ) {
                AsyncImage(
                    model = rememberImageRequest(info.authorFace),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (info.followed) "已关注" else "+关注",
                    color = Color.White,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(10.dp))
            }
        }

        // 进度：时长在上，进度条紧贴在下
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = edge, end = edge, bottom = 52.dp)
                .height(58.dp),
        ) {
            Text(
                text = "${formatPlayTime(playback.positionMs)}/${formatPlayTime(playback.durationMs)}",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 11.5.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 10.dp)
                    .padding(bottom = 36.dp),
            )
            if (playback.sleepAtMs > System.currentTimeMillis()) {
                val left = (playback.sleepAtMs - System.currentTimeMillis()) / 1000L
                Text(
                    text = "定时关闭 ${left / 60}:${"%02d".format(left % 60)}",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            FullscreenSeekBar(
                positionMs = playback.positionMs,
                durationMs = playback.durationMs,
                bufferedMs = playback.bufferedMs,
                onInteract = {},
                onSeek = actions.onSeek,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 10.dp),
                externalPressed = playback.scrubbing,
                externalLook = playback.seekLook,
                onScrubStart = onScrubStart,
                onScrubEnd = onScrubEnd,
                onLookChanged = onScrubLook,
                onScrubPreview = onScrubPreview,
            )
        }

        // 底部控制行
        val hasNext = hasNextPage(info)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 42.dp, end = 28.dp, bottom = 6.dp)
                .height(42.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(onClick = actions.onPlayPause, size = 40.dp) {
                if (playback.playing) {
                    PauseGlyph()
                } else {
                    PlayGlyph()
                }
            }
            Spacer(Modifier.width(12.dp))
            PlayerIconButton(
                onClick = actions.onNext,
                size = 32.dp,
            ) {
                NextGlyph(enabled = hasNext)
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        actions.onToggleDanmaku()
                    },
                contentAlignment = Alignment.Center,
            ) {
                DanmakuToggleGlyph(enabled = playback.danmakuEnabled)
            }
            Spacer(Modifier.width(8.dp))
            PlayerIconButton(
                onClick = { onPanelChanged(FullscreenPanel.Danmaku) },
                size = 32.dp,
            ) {
                SlidersGlyph()
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(Capsule())
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable(interactionSource = null, indication = null) {
                        actions.onSendDanmaku()
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "发个友善的弹幕见证当下",
                    color = Color.Black.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(14.dp))
            PlayerTextAction(
                text = if (info.selectedSubtitle != null) "字幕·开" else "字幕",
                active = info.selectedSubtitle != null,
                onClick = { onPanelChanged(FullscreenPanel.Subtitles) },
            )
            Spacer(Modifier.width(18.dp))
            PlayerTextAction(
                text = "选集",
                muted = info.pages.size <= 1,
                onClick = actions.onEpisodes,
            )
            Spacer(Modifier.width(18.dp))
            PlayerTextAction(
                text = "倍速",
                onClick = { onPanelChanged(FullscreenPanel.Speed) },
            )
            Spacer(Modifier.width(16.dp))
            QualityAction(
                label = info.qualityLabel,
                onClick = { onPanelChanged(FullscreenPanel.Quality) },
            )
            Spacer(Modifier.width(12.dp))
        }
        }

        // 二级面板：统一右侧渐变遮罩
        if (panelVisible && lastPanel != FullscreenPanel.None) {
            FullscreenPanelHost(
                panel = lastPanel,
                info = info,
                playback = playback,
                actions = actions,
                onClose = { onPanelChanged(FullscreenPanel.None) },
                visible = panel != FullscreenPanel.None,
            )
        }
    }
}

@Composable
internal fun FullscreenDanmakuInput(
    onDismiss: () -> Unit,
    items: List<DanmakuItem> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var mode by remember { mutableIntStateOf(1) }
    var fontSize by remember { mutableIntStateOf(25) }
    var color by remember { mutableIntStateOf(0xFFFFFF) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val colors = listOf(
        0xFFFFFF, 0xFB7299, 0xFF4D4D, 0xFFD93B, 0x36C46A,
        0x2F9BFF, 0xFF4FA8, 0x9A6BFF, 0x6ED6E8, 0xFFA33B,
    )

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    fun submit() {
        val value = text.trim()
        if (value.isEmpty() || sending) return
        val sender = VideoPageBus.danmakuSender
        if (sender == null) {
            onDismiss()
            return
        }
        sending = true
        scope.launch {
            runCatching { sender(value, mode, fontSize, color) }
            sending = false
            focusManager.clearFocus()
            keyboard?.hide()
            onDismiss()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xF20A0A0C))
            .imePadding()
            .padding(horizontal = 26.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        fontSize = if (fontSize == 25) 18 else 25
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "A", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(Capsule())
                    .background(Color.White.copy(alpha = 0.94f))
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = SolidColor(Color(0xFFFB7299)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "发个友善的弹幕见证当下",
                                    color = Color.Black.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            enabled = text.isNotBlank() && !sending,
                            onClick = { submit() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "➤",
                        color = if (text.isBlank()) Color.Black.copy(alpha = 0.25f) else Color(0xFFFB7299),
                        fontSize = 20.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        ComposerSettingRow(label = "字体大小") {
            ComposerChip("默认", fontSize == 25) { fontSize = 25 }
            Spacer(Modifier.width(10.dp))
            ComposerChip("较小", fontSize == 18) { fontSize = 18 }
        }
        ComposerSettingRow(label = "弹幕位置") {
            ComposerChip("滚动", mode == 1) { mode = 1 }
            Spacer(Modifier.width(10.dp))
            ComposerChip("置顶", mode == 5) { mode = 5 }
            Spacer(Modifier.width(10.dp))
            ComposerChip("置底", mode == 4) { mode = 4 }
        }
        ComposerSettingRow(label = "弹幕颜色") {
            Row(
                modifier = Modifier.padding(start = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                colors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000000.toInt() or c))
                            .border(
                                if (color == c) 2.dp else 0.5.dp,
                                if (color == c) Color.White else Color.White.copy(alpha = 0.35f),
                                CircleShape,
                            )
                            .clickable(interactionSource = null, indication = null) { color = c },
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerSettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.width(78.dp),
        )
        content()
    }
}

@Composable
private fun ComposerChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val pink = Color(0xFFFB7299)
    Text(
        text = text,
        color = if (selected) pink else Color.White.copy(alpha = 0.8f),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(64.dp)
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
    )
}

@Composable
private fun FullscreenStatusLine(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clock by produceState(initialValue = clockText()) {
        while (true) {
            value = clockText()
            delay(15_000)
        }
    }
    val battery by produceState(initialValue = batteryPercent(context)) {
        while (true) {
            value = batteryPercent(context)
            delay(30_000)
        }
    }
    val network by produceState(initialValue = networkLabel(context)) {
        while (true) {
            value = networkLabel(context)
            delay(8_000)
        }
    }
    Box(modifier = modifier.height(19.dp)) {
        Text(
            text = clock,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ViewerGlyph(tint: Color = Color.White.copy(alpha = 0.86f)) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val stroke = 1.dp.toPx()
        drawCircle(
            color = tint,
            radius = size.width * 0.15f,
            center = Offset(size.width * 0.3f, size.height * 0.32f),
            style = Stroke(stroke),
        )
        drawCircle(
            color = tint,
            radius = size.width * 0.13f,
            center = Offset(size.width * 0.7f, size.height * 0.3f),
            style = Stroke(stroke),
        )
        drawArc(
            color = tint,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.08f, size.height * 0.5f),
            size = Size(size.width * 0.44f, size.height * 0.38f),
            style = Stroke(stroke),
        )
        drawArc(
            color = tint,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(size.width * 0.5f, size.height * 0.48f),
            size = Size(size.width * 0.4f, size.height * 0.36f),
            style = Stroke(stroke),
        )
    }
}

@Composable
private fun BadgeStatAction(
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
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = countXOffset, y = countYOffset),
            )
        }
    }
}

@Composable
private fun IconOnlyAction(
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
private fun PlayerIconButton(
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
private fun PlayerTextAction(
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
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 8.dp),
    )
}

@Composable
private fun QualityAction(label: String, onClick: () -> Unit) {
    val base = qualityBaseLabel(label)
    val badge = qualityBadgeLabel(label)
    Box(
        modifier = Modifier
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = base,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-7).dp)
                    .clip(Capsule())
                    .background(Color.Black.copy(alpha = 0.52f))
                    .border(0.7.dp, Color.White.copy(alpha = 0.55f), Capsule())
                    .padding(horizontal = 4.dp, vertical = 0.5.dp),
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun isHighBitrateLabel(label: String): Boolean =
    label.contains("+") || label.contains("高码率")

private fun qualityBadgeLabel(label: String): String? = when {
    isHighBitrateLabel(label) -> "高码率"
    label.contains("60") -> "60帧"
    else -> null
}

private fun qualityBaseLabel(label: String): String {
    val cleaned = label
        .replace(" 高码率", "")
        .replace("高码率", "")
        .removeSuffix("+")
        .let { if (it.endsWith("P60")) it.removeSuffix("60") else it }
        .trim()
    return cleaned.ifBlank { "画质" }
}

private fun qualityMenuLabel(label: String): String = when {
    label == "1080P+" || label == "1080P 高码率" -> "1080P 高码率"
    label == "1080P60" -> "1080P 60帧"
    label == "720P60" -> "720P 60帧"
    else -> label
}

/* ------------------------------------------------------------------ panels */

@Composable
private fun FullscreenPanelHost(
    panel: FullscreenPanel,
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
    visible: Boolean = true,
) {
    var shown by remember { mutableStateOf(panel) }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { revealed = visible }
    val reveal = rememberManualProgress(if (revealed) 1f else 0f, 264f)
    LaunchedEffect(panel) {
        if (panel != FullscreenPanel.None) shown = panel
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val panelWidth = (maxWidth.value * 0.41f).dp.coerceAtLeast(360.dp)
        // 左透明、右深黑：与参考图三一致，不铺满一块纯黑
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.25f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.25f),
                        0.58f to Color.Black.copy(alpha = 0.74f),
                        0.68f to Color.Black.copy(alpha = 0.95f),
                        1f to Color.Black,
                    ),
                )
                .graphicsLayer { alpha = reveal }
                .clickable(interactionSource = null, indication = null, onClick = onClose),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(panelWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = reveal
                    translationX = (1f - reveal) * 90f
                }
                .clickable(interactionSource = null, indication = null) {}
                .padding(start = 32.dp, end = 30.dp, top = 30.dp, bottom = 18.dp),
        ) {
            when (shown) {
                FullscreenPanel.Favorite -> FavoritePanel(info, actions, onClose)
                FullscreenPanel.More -> MorePanel(info, playback, actions, onClose)
                FullscreenPanel.Danmaku -> DanmakuPanel(info, onClose)
                FullscreenPanel.Episodes -> EpisodesPanel(info, actions, onClose)
                FullscreenPanel.Speed -> SpeedPanel(playback, actions, onClose)
                FullscreenPanel.Quality -> QualityPanel(info, playback, actions, onClose)
                FullscreenPanel.Cache -> CachePanel(playback, actions, onClose)
                FullscreenPanel.Subtitles -> SubtitlesPanel(info, actions, onClose)
                FullscreenPanel.None -> Unit
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            CloseGlyph()
        }
    }
}

@Composable
private fun PanelCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            content = content,
        )
    }
}

@Composable
private fun FavoritePanel(
    info: FullscreenPlayerInfo,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader("选择收藏夹", onClose)
        Spacer(Modifier.height(14.dp))
        if (info.folders.isEmpty()) {
            Text(
                text = if (info.loggedIn) "正在读取收藏夹…" else "登录后可选择收藏夹",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            info.folders.forEach { folder ->
                val checked = info.folderIds.contains(folder.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(interactionSource = null, indication = null) {
                            actions.onToggleFolder(folder.id)
                        }
                        .padding(vertical = 13.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "${folder.count} 个内容 · ${if (checked) "已收藏" else "公开"}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = if (checked) BrandPink else Color.White.copy(alpha = 0.5f),
                                shape = CircleShape,
                            )
                            .background(if (checked) BrandPink else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (checked) {
                            CheckGlyph()
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(interactionSource = null, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Text("完成", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MorePanel(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        PanelHeader("更多", onClose)
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            QuickAction("听视频", Modifier.weight(1f), { HeadphoneGlyph() }) {
                actions.onAudioOnlyChanged(true)
                onClose()
            }
            QuickAction("稍后再看", Modifier.weight(1f), { ClockGlyph() }) {
                actions.onAddWatchLater()
                onClose()
            }
            QuickAction("小窗播放", Modifier.weight(1f), { PipGlyph() }) {
                actions.onPictureInPicture()
                onClose()
            }
            QuickAction("投屏", Modifier.weight(1f), { CastGlyph() }) {
                actions.onCast()
                onClose()
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            MoreIconSwitchRow(
                label = "自动连播",
                checked = playback.autoPlayNext,
                glyph = { AutoPlayNextGlyph() },
            ) {
                actions.onAutoPlayNextChanged(!playback.autoPlayNext)
            }
            MoreLoopRow(
                loopMode = playback.loopMode,
                onSelect = { actions.onLoopModeChanged(it) },
            )
            MoreIconSwitchRow(
                label = "镜像翻转",
                checked = playback.mirror,
                glyph = { MirrorGlyph() },
            ) {
                actions.onMirrorChanged(!playback.mirror)
            }
            MoreIconSwitchRow(
                label = "后台听视频",
                checked = playback.backgroundPlay,
                glyph = { BackgroundListenGlyph() },
            ) {
                actions.onBackgroundPlayChanged(!playback.backgroundPlay)
            }
            MoreIconChoiceRow(
                label = "画面尺寸",
                value = when (playback.resizeMode) {
                    1 -> "填充"
                    2 -> "裁剪"
                    else -> "适应"
                },
                glyph = { ResizeGlyph() },
            ) {
                actions.onResizeModeChanged((playback.resizeMode + 1) % 3)
            }
            MoreIconChoiceRow(
                label = "色觉辅助",
                value = when (playback.colorAssist) {
                    1 -> "灰度"
                    2 -> "高对比"
                    else -> "不开启"
                },
                glyph = { ColorAssistGlyph() },
            ) {
                actions.onColorAssistChanged((playback.colorAssist + 1) % 3)
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun MoreIconSwitchRow(
    label: String,
    checked: Boolean,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { glyph() }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        MoreSwitch(checked)
    }
}

@Composable
private fun MoreIconChoiceRow(
    label: String,
    value: String,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { glyph() }
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White.copy(alpha = 0.58f), fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        ChevronRightGlyph()
    }
}

@Composable
private fun MoreLoopRow(loopMode: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { LoopGlyph() }
        Spacer(Modifier.width(12.dp))
        Text("循环播放", color = Color.White, fontSize = 15.sp)
        Spacer(Modifier.weight(1f))
        listOf("单集循环", "列表循环", "不循环").forEachIndexed { index, label ->
            if (index > 0) {
                Text(
                    text = "|",
                    color = Color.White.copy(alpha = 0.18f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 5.dp),
                )
            }
            Text(
                text = label,
                color = if (loopMode == index + 1 || (index == 2 && loopMode == 0)) {
                    BrandPink
                } else {
                    Color.White.copy(alpha = 0.55f)
                },
                fontSize = 12.sp,
                modifier = Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                ) {
                    onSelect(if (index == 2) 0 else index + 1)
                },
            )
        }
    }
}

@Composable
private fun MoreSwitch(checked: Boolean) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .clip(Capsule())
            .background(if (checked) BrandPink else Color.White.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .padding(start = if (checked) 20.dp else 2.dp, top = 2.dp)
                .size(18.dp)
                .clip(Capsule())
                .background(Color.White),
        )
    }
}

@Composable
private fun QuickAction(
    label: String,
    modifier: Modifier = Modifier,
    glyph: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            glyph()
        }
        Spacer(Modifier.height(5.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
    }
}

@Composable
private fun MoreSwitchRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(21.dp)
                .clip(Capsule())
                .background(if (checked) BrandPink else Color.White.copy(alpha = 0.18f)),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (checked) 19.dp else 2.dp, y = 2.dp)
                    .size(17.dp)
                    .clip(Capsule())
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun MoreChoiceRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        ChevronRightGlyph()
    }
}

@Composable
private fun DanmakuPanel(
    info: FullscreenPlayerInfo,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            DanmakuSettingsContent(
                fullscreen = true,
                onInteract = {},
                danmakuItems = info.danmakuItems,
            )
        }
    }
}

@Composable
private fun PanelSettingSwitch(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(Capsule())
                .background(if (checked) BrandPink else Color.White.copy(alpha = 0.2f)),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (checked) 18.dp else 2.dp, y = 2.dp)
                    .size(16.dp)
                    .clip(Capsule())
                    .background(Color.White),
            )
        }
    }
}

@Composable
private fun PanelSettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row {
            Text(label, color = Color.White.copy(alpha = 0.86f), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(format(value), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(4.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .then(
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                    ) {
                        // 点击轨道直接跳到对应比例；拖动由 pointerInput 处理
                    },
                ),
        ) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .padding(vertical = 8.dp)
                    .pointerInputSlider(
                        value = value,
                        range = range,
                        onPressed = { pressed = it },
                        onChange = onChange,
                    ),
            ) {
                val ratio = ((value - range.start) / (range.endInclusive - range.start))
                    .coerceIn(0f, 1f)
                val h = if (pressed) 6.dp.toPx() else 4.dp.toPx()
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.18f),
                    topLeft = Offset(0f, (size.height - h) / 2f),
                    size = Size(size.width, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                )
                drawRoundRect(
                    color = BrandPink,
                    topLeft = Offset(0f, (size.height - h) / 2f),
                    size = Size(size.width * ratio, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2f),
                )
            }
        }
    }
}

private fun Modifier.pointerInputSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onPressed: (Boolean) -> Unit,
    onChange: (Float) -> Unit,
): Modifier = this.pointerInput(range) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var decided = false
        var slider = false
        var lastX = down.position.x
        fun update(x: Float) {
            val ratio = (x / size.width).coerceIn(0f, 1f)
            onChange(range.start + ratio * (range.endInclusive - range.start))
        }
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val dx = change.position.x - down.position.x
            val dy = change.position.y - down.position.y
            if (!decided &&
                (kotlin.math.abs(dx) > viewConfiguration.touchSlop ||
                    kotlin.math.abs(dy) > viewConfiguration.touchSlop)
            ) {
                decided = true
                slider = kotlin.math.abs(dx) > kotlin.math.abs(dy)
                if (slider) {
                    onPressed(true)
                    update(change.position.x)
                }
            }
            if (slider) {
                lastX = change.position.x
                update(lastX)
                change.consume()
            }
        }
        if (slider) onPressed(false)
    }
}

@Composable
private fun EpisodesPanel(
    info: FullscreenPlayerInfo,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {
    PanelCard(title = "选集") {
        Text(
            text = if (info.pages.size <= 1) "当前视频为单集" else "共 ${info.pages.size} 集",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            info.pages.forEachIndexed { index, page ->
                val active = page.cid == info.currentCid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
                        )
                        .clickable(interactionSource = null, indication = null) {
                            actions.onPageSelected(page)
                            onClose()
                        }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "P${index + 1}",
                        color = if (active) BrandPink else Color.White.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                        modifier = Modifier.width(34.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = page.part.ifBlank { "第 ${index + 1} 集" },
                            color = if (active) BrandPink else Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = formatDuration(page.duration),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                        )
                    }
                    if (active) {
                        Text("播放中", color = BrandPink, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedPanel(
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {
    PanelCard(title = "倍速") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            FullscreenSpeeds.forEach { speed ->
                val active = kotlin.math.abs(speed - playback.speed) < 0.001f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
                        )
                        .clickable(interactionSource = null, indication = null) {
                            actions.onSpeedSelected(speed)
                            onClose()
                        }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${trimSpeed(speed)}x",
                        color = if (active) BrandPink else Color.White,
                        fontSize = 15.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(Modifier.weight(1f))
                    if (active) CheckGlyph(BrandPink)
                }
            }
        }
    }
}

@Composable
private fun QualityPanel(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {
    PanelCard(title = "画质") {
        if (playback.qualities.isEmpty()) {
                Text("暂无可用画质", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 自动：按当前网速挑一档；选中后画质标签显示「自动」
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (playback.autoQuality) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
                        )
                        .clickable(interactionSource = null, indication = null) {
                            actions.onQualitySelected(0)
                            onClose()
                        }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "自动",
                        color = if (playback.autoQuality) BrandPink else Color.White,
                        fontSize = 15.sp,
                        fontWeight = if (playback.autoQuality) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (playback.autoQuality) CheckGlyph(BrandPink)
                }
                playback.qualities.forEach { item ->
                    val active = !playback.autoQuality && item.id == playback.qualityId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
                            )
                            .clickable(interactionSource = null, indication = null) {
                                actions.onQualitySelected(item.id)
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = qualityMenuLabel(item.label),
                                color = if (active) BrandPink else Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            if (item.width > 0 && item.height > 0) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${item.width}×${item.height}",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 10.sp,
                                )
                            }
                        }
                        if (active) CheckGlyph(BrandPink)
                    }
                }
            }
        }
}

@Composable
private fun CachePanel(
    playback: FullscreenPlaybackState,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader("缓存", onClose)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "选择要缓存的清晰度",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (playback.qualities.isEmpty()) {
            Text(
                text = "暂无可用清晰度",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
            )
        }
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            playback.qualities.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(interactionSource = null, indication = null) {
                            actions.onCacheSelected(item)
                            onClose()
                        }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = qualityMenuLabel(item.label),
                            color = Color.White,
                            fontSize = 15.sp,
                        )
                        if (item.width > 0 && item.height > 0) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${item.width}×${item.height}",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Text(
                        text = "缓存",
                        color = BrandPink,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CacheGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = size.width * 0.09f
        val cx = size.width * 0.5f
        drawLine(
            tint,
            Offset(cx, size.height * 0.12f),
            Offset(cx, size.height * 0.58f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            tint,
            Offset(cx, size.height * 0.58f),
            Offset(size.width * 0.32f, size.height * 0.4f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            tint,
            Offset(cx, size.height * 0.58f),
            Offset(size.width * 0.68f, size.height * 0.4f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            tint,
            Offset(size.width * 0.18f, size.height * 0.72f),
            Offset(size.width * 0.18f, size.height * 0.86f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            tint,
            Offset(size.width * 0.18f, size.height * 0.86f),
            Offset(size.width * 0.82f, size.height * 0.86f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            tint,
            Offset(size.width * 0.82f, size.height * 0.86f),
            Offset(size.width * 0.82f, size.height * 0.72f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun SubtitlesPanel(
    info: FullscreenPlayerInfo,
    actions: FullscreenActions,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader("字幕", onClose)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (info.selectedSubtitle == null) {
                        BrandPink.copy(alpha = 0.18f)
                    } else {
                        Color.Transparent
                    },
                )
                .clickable(interactionSource = null, indication = null) {
                    actions.onSubtitleSelected(null)
                    onClose()
                }
                .padding(horizontal = 12.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("关闭字幕", color = Color.White, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            if (info.selectedSubtitle == null) CheckGlyph(BrandPink)
        }
        if (info.subtitles.isEmpty()) {
            Text(
                text = "该视频暂无字幕",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 18.dp),
            )
        } else {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                info.subtitles.forEach { track ->
                    val active = info.selectedSubtitle?.id == track.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (active) BrandPink.copy(alpha = 0.18f) else Color.Transparent,
                            )
                            .clickable(interactionSource = null, indication = null) {
                                actions.onSubtitleSelected(track)
                                onClose()
                            }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.label.ifBlank { "字幕" },
                                color = if (active) BrandPink else Color.White,
                                fontSize = 15.sp,
                            )
                            if (track.isAi) {
                                Spacer(Modifier.height(2.dp))
                                Text("AI 字幕", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            }
                        }
                        if (active) CheckGlyph(BrandPink)
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------ audio mode */

@Composable
private fun FullscreenAudioMode(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 不透明底：进听视频后下方 PlayerView 的视频帧不会透出来
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08090D)))
        AsyncImage(
            model = rememberImageRequest(info.cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(46.dp)
                .graphicsLayer { alpha = 0.55f },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Black.copy(alpha = 0.9f),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 76.dp, vertical = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = rememberImageRequest(info.cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(310.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Spacer(Modifier.width(70.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "听视频",
                    color = BrandPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = info.title,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    text = info.author.ifBlank { "UP 主" },
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(34.dp))
                FullscreenSeekBar(
                    positionMs = playback.positionMs,
                    durationMs = playback.durationMs,
                    bufferedMs = playback.bufferedMs,
                    onInteract = {},
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxWidth(),
                    externalPressed = playback.scrubbing,
                    externalLook = playback.seekLook,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatPlayTime(playback.positionMs),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatPlayTime(playback.durationMs),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.13f))
                            .clickable(interactionSource = null, indication = null) {
                                onSeekRelative(-10_000L)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("-10s", color = Color.White, fontSize = 10.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(interactionSource = null, indication = null, onClick = onPlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (playback.playing) PauseGlyph(Color.Black) else PlayGlyph(Color.Black, 26.dp)
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.13f))
                            .clickable(interactionSource = null, indication = null) {
                                onSeekRelative(10_000L)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+10s", color = Color.White, fontSize = 10.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(Capsule())
                            .border(1.dp, Color.White.copy(alpha = 0.35f), Capsule())
                            .clickable(interactionSource = null, indication = null, onClick = onBack)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("退出听视频", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        // 顶部返回和状态仍保留，方便直接退出全屏
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(start = 42.dp, top = 26.dp, end = 42.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        onBack()
                    },
                contentAlignment = Alignment.Center,
            ) {
                BackChevron()
            }
            Text(
                text = info.title,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
        }
        if (AppGraph.settings.showFullscreenTime) {
            FullscreenStatusLine(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 42.dp, vertical = 3.dp),
            )
        }
    }
}

/** 全屏专用紧凑进度条：和播放器现有进度条同款配色，只把高度收窄，贴合官方横屏。 */
/** 进度条小电视：纯矢量，白色实体 + 黑色轮廓/眼睛；拖动时眼睛跟着方向偏移。 */
@Composable
private fun TvSeekThumb(
    look: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.094f
        val bodyLeft = w * 0.10f
        val bodyTop = h * 0.28f
        val bodyRight = w * 0.90f
        val bodyBottom = h * 0.88f
        val radius = w * 0.13f

        // 天线
        drawLine(
            color = Color.Black,
            start = Offset(w * 0.32f, h * 0.06f),
            end = Offset(w * 0.5f, h * 0.29f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.Black,
            start = Offset(w * 0.68f, h * 0.06f),
            end = Offset(w * 0.5f, h * 0.29f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        // 白色实体
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(radius, radius),
        )
        // 黑色轮廓
        drawRoundRect(
            color = Color.Black,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke),
        )

        // 眼睛随滑动位置连续转动
        val eyeShift = look.coerceIn(-1f, 1f) * w * 0.045f
        val eyeWidth = w * 0.091f
        val eyeHeight = h * 0.21f
        val eyeTop = bodyTop + (bodyBottom - bodyTop) * 0.48f - eyeHeight / 2f
        val eyeRadius = CornerRadius(eyeWidth / 2f, eyeWidth / 2f)
        listOf(w * 0.35f + eyeShift, w * 0.65f + eyeShift).forEach { cx ->
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(cx - eyeWidth / 2f, eyeTop),
                size = Size(eyeWidth, eyeHeight),
                cornerRadius = eyeRadius,
            )
        }
    }
}

@Composable
internal fun FullscreenSeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long = 0L,
    onInteract: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    externalPressed: Boolean = false,
    externalLook: Float = 0f,
    onScrubStart: () -> Unit = {},
    onScrubEnd: () -> Unit = {},
    onLookChanged: (Float) -> Unit = {},
    onScrubPreview: (Long) -> Unit = {},
) {
    var pressed by remember { mutableStateOf(false) }
    var dragRatio by remember { mutableStateOf(-1f) }
    var dragLook by remember { mutableFloatStateOf(0f) }
    val ratio = if (durationMs > 0) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shown = if (dragRatio >= 0f) dragRatio else ratio
    BoxWithConstraints(
        modifier = modifier
            .height(24.dp)
            .pointerInput(durationMs) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    dragLook = 0f
                    onScrubStart()
                    onLookChanged(0f)
                    var lastX = down.position.x
                    onInteract()
                    if (durationMs > 0) {
                        dragRatio = (down.position.x / size.width).coerceIn(0f, 1f)
                        onScrubPreview((dragRatio * durationMs).toLong())
                    }
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dx = change.position.x - lastX
                        if (kotlin.math.abs(dx) > 0.5f) {
                            dragLook = (dragLook + dx / (size.width * 0.08f))
                                .coerceIn(-1f, 1f)
                            onLookChanged(dragLook)
                        }
                        lastX = change.position.x
                        if (durationMs > 0) {
                            dragRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                            onScrubPreview((dragRatio * durationMs).toLong())
                        }
                        change.consume()
                    }
                    if (durationMs > 0 && dragRatio >= 0f) {
                        onSeek((dragRatio * durationMs).toLong())
                    }
                    dragRatio = -1f
                    dragLook = 0f
                    pressed = false
                    onScrubEnd()
                    onLookChanged(0f)
                    onScrubPreview(-1L)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val w = maxWidth
        val thick = pressed || externalPressed
        val thickness = if (thick) 4.dp else 2.5.dp
        val seekPink = Color(0xFFFB7299)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thickness)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.3f)),
        )
        val bufferedRatio = if (durationMs > 0) {
            (bufferedMs.toFloat() / durationMs).coerceIn(shown, 1f)
        } else {
            0f
        }
        Box(
            modifier = Modifier
                .width(w * bufferedRatio)
                .height(thickness)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.52f)),
        )
        Box(
            modifier = Modifier
                .width(w * shown)
                .height(thickness)
                .clip(Capsule())
                .background(seekPink),
        )
        val thumbSize = 17.6.dp
        val activeLook = if (kotlin.math.abs(dragLook) > 0.01f) dragLook else externalLook
        val thumbX = (w * shown - thumbSize / 2f)
            .coerceIn(0.dp, (w - thumbSize).coerceAtLeast(0.dp))
        TvSeekThumb(
            look = activeLook,
            modifier = Modifier
                .padding(start = thumbX)
                .offset(y = (-1).dp)
                .size(thumbSize),
        )
    }
}

/* ---------------------------------------------------------------- glyphs */
@Composable
private fun PixelIcon(
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
private fun BackChevron(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.64f, size.height * 0.12f)
            lineTo(size.width * 0.28f, size.height * 0.5f)
            lineTo(size.width * 0.64f, size.height * 0.88f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = size.width * 0.11f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun MoreGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val r = size.width * 0.075f
        listOf(0.22f, 0.5f, 0.78f).forEach { y ->
            drawCircle(tint, radius = r, center = Offset(size.width * 0.5f, size.height * y))
        }
    }
}

@Composable
private fun CloseGlyph(tint: Color = Color.White.copy(alpha = 0.7f)) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val s = size.width * 0.12f
        drawLine(tint, Offset(size.width * 0.2f, size.height * 0.2f), Offset(size.width * 0.8f, size.height * 0.8f), s, StrokeCap.Round)
        drawLine(tint, Offset(size.width * 0.8f, size.height * 0.2f), Offset(size.width * 0.2f, size.height * 0.8f), s, StrokeCap.Round)
    }
}

@Composable
private fun CheckGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(15.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.52f)
            lineTo(size.width * 0.42f, size.height * 0.78f)
            lineTo(size.width * 0.86f, size.height * 0.25f)
        }
        drawPath(
            path,
            tint,
            style = Stroke(size.width * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun ChevronRightGlyph(tint: Color = Color.White.copy(alpha = 0.4f)) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.38f, size.height * 0.2f)
            lineTo(size.width * 0.68f, size.height * 0.5f)
            lineTo(size.width * 0.38f, size.height * 0.8f)
        }
        drawPath(path, tint, style = Stroke(size.width * 0.12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
internal fun PlayGlyph(tint: Color = Color.White, size: androidx.compose.ui.unit.Dp = 24.dp) {
    PixelIcon(
        res = TeakIcon.PlayerPlay,
        tint = tint,
        contentDescription = "播放",
        modifier = Modifier.size(size),
    )
}

@Composable
internal fun PauseGlyph(tint: Color = Color.White) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 6.dp, height = 22.dp).clip(Capsule()).background(tint))
        Box(Modifier.size(width = 6.dp, height = 22.dp).clip(Capsule()).background(tint))
    }
}

@Composable
private fun NextGlyph(enabled: Boolean) {
    PixelIcon(
        res = TeakIcon.PlayerNext,
        tint = Color.White,
        contentDescription = "下一集",
        modifier = Modifier
            .size(19.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.28f },
    )
}

@Composable
private fun DanmakuToggleGlyph(enabled: Boolean) {
    PixelIcon(
        res = if (enabled) TeakIcon.PlayerDanmakuOn else TeakIcon.PlayerDanmakuOff,
        contentDescription = if (enabled) "关闭弹幕" else "开启弹幕",
        modifier = Modifier.size(23.dp),
    )
}

@Composable
private fun SlidersGlyph() {
    PixelIcon(
        res = TeakIcon.PlayerDanmakuSettings,
        contentDescription = "弹幕设置",
        modifier = Modifier.size(23.dp),
    )
}

@Composable
private fun NetworkGlyph(label: String, tint: Color) {
    if (label == "WiFi") {
        Canvas(modifier = Modifier.size(14.dp)) {
            val stroke = size.width * 0.1f
            drawArc(
                tint,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(size.width * 0.08f, size.height * 0.18f),
                size = Size(size.width * 0.84f, size.height * 0.84f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                tint,
                startAngle = 215f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(size.width * 0.28f, size.height * 0.38f),
                size = Size(size.width * 0.44f, size.height * 0.44f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawCircle(tint, radius = size.width * 0.08f, center = Offset(size.width * 0.5f, size.height * 0.82f))
        }
    } else {
        Canvas(modifier = Modifier.size(14.dp)) {
            val stroke = size.width * 0.1f
            listOf(0.26f, 0.5f, 0.74f).forEachIndexed { index, fraction ->
                val h = size.height * (0.24f + index * 0.2f)
                drawLine(
                    tint,
                    Offset(size.width * fraction, size.height * 0.82f),
                    Offset(size.width * fraction, size.height * 0.82f - h),
                    stroke,
                    StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun BatteryGlyph(percent: Int, tint: Color) {
    Canvas(modifier = Modifier.size(width = 14.dp, height = 8.dp)) {
        val stroke = 1.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, 0f),
            size = Size(size.width * 0.88f, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = Stroke(stroke),
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.91f, size.height * 0.3f),
            size = Size(size.width * 0.06f, size.height * 0.4f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
        )
        val inset = 1.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(inset, inset),
            size = Size(
                (size.width * 0.88f - inset * 2f) * (percent.coerceIn(0, 100) / 100f),
                size.height - inset * 2f,
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),
        )
    }
}

@Composable
private fun HeadphoneGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = size.width * 0.09f
        drawArc(
            tint,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(size.width * 0.13f, size.height * 0.13f),
            size = Size(size.width * 0.74f, size.height * 0.74f),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawRoundRect(
            tint,
            topLeft = Offset(size.width * 0.13f, size.height * 0.5f),
            size = Size(size.width * 0.17f, size.height * 0.33f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.07f),
        )
        drawRoundRect(
            tint,
            topLeft = Offset(size.width * 0.7f, size.height * 0.5f),
            size = Size(size.width * 0.17f, size.height * 0.33f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.width * 0.07f),
        )
    }
}

@Composable
private fun ClockGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(24.dp)) {
        drawCircle(tint, radius = size.width * 0.38f, style = Stroke(size.width * 0.08f))
        drawLine(tint, center, Offset(center.x, size.height * 0.28f), size.width * 0.07f, StrokeCap.Round)
        drawLine(tint, center, Offset(size.width * 0.68f, center.y), size.width * 0.07f, StrokeCap.Round)
    }
}

@Composable
private fun PipGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(24.dp)) {
        drawRoundRect(
            tint,
            topLeft = Offset(size.width * 0.08f, size.height * 0.12f),
            size = Size(size.width * 0.84f, size.height * 0.64f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            style = Stroke(size.width * 0.07f),
        )
        drawRoundRect(
            tint,
            topLeft = Offset(size.width * 0.52f, size.height * 0.48f),
            size = Size(size.width * 0.34f, size.height * 0.22f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
        )
    }
}

@Composable
private fun CastGlyph(tint: Color = Color.White) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = size.width * 0.07f
        drawArc(
            tint,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width * 0.1f, size.height * 0.1f),
            size = Size(size.width * 0.8f, size.height * 0.8f),
            style = Stroke(stroke),
        )
        drawArc(
            tint,
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(size.width * 0.28f, size.height * 0.28f),
            size = Size(size.width * 0.44f, size.height * 0.44f),
            style = Stroke(stroke),
        )
        drawCircle(tint, radius = size.width * 0.07f, center = Offset(size.width * 0.26f, size.height * 0.74f))
    }
}

/* -------------------------------------------------------------- helpers */

private fun hasNextPage(info: FullscreenPlayerInfo): Boolean {
    if (info.pages.size <= 1) return false
    val index = info.pages.indexOfFirst { it.cid == info.currentCid }
    return index >= 0 && index + 1 < info.pages.size
}

private fun viewerText(info: FullscreenPlayerInfo): String {
    val text = info.onlineText.ifBlank {
        if (info.onlineCount > 0) compactCount(info.onlineCount) else ""
    }
    return if (text.isBlank()) "" else "${text}人正在看"
}

internal fun compactCount(value: Long): String {
    if (value < 10_000) return value.toString()
    if (value >= 100_000_000) return trimCount(value / 100_000_000.0) + "亿"
    return trimCount(value / 10_000.0) + "万"
}

private fun trimCount(value: Double): String {
    val one = "%.1f".format(Locale.CHINA, value)
    return if (one.endsWith(".0")) one.dropLast(2) else one
}

private fun trimSpeed(value: Float): String {
    val one = "%.2f".format(Locale.CHINA, value)
    return one.trimEnd('0').trimEnd('.')
}

private fun clockText(): String =
    SimpleDateFormat("HH:mm", Locale.CHINA).format(Date())

private fun batteryPercent(context: Context): Int {
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val direct = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    if (direct in 0..100) return direct
    val intent = runCatching {
        context.registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
        )
    }.getOrNull()
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    return if (level >= 0 && scale > 0) level * 100 / scale else 0
}

private fun networkLabel(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return "WiFi"
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "WiFi"
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "WiFi"
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val type = runCatching { tm?.dataNetworkType ?: 0 }.getOrDefault(0)
        return when (type) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            -> "3G"
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            -> "2G"
            else -> "4G"
        }
    }
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "WiFi"
        else -> "WiFi"
    }
}

/** 字幕层：控件自动隐藏后仍然显示当前句。 */
@Composable
internal fun SubtitleOverlay(
    cues: List<SubtitleCue>,
    positionMs: Long,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    if (cues.isEmpty()) return
    val cue = remember(cues, positionMs) {
        cues.lastOrNull { positionMs >= it.fromMs && positionMs <= it.toMs }
    } ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            text = cue.text,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}


