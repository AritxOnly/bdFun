package com.aritxonly.bdfun.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.LiveChatKind
import com.aritxonly.bdfun.data.LiveChatMessage
import com.aritxonly.bdfun.data.LiveRoomInfo
import com.aritxonly.teak.common.navigation.TeakMorphMenuAnchor
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.More

/**
 * 直播详情页（移植自 BiliPai 的直播间布局，第一阶段全部使用 mock 数据）。
 *
 * 布局来源：BiliPai `feature/live/LivePlayerScreen.kt` 的竖屏形态：
 * 播放器在顶部，下面是互动区（弹幕 + 发送条），顶栏浮在播放器上。
 *
 * 和 BiliPai 不同的地方：
 * - 返回键 / 省略号：纯图标，没有底板（和 BiliPai 的紧凑模式一样）
 * - 省略号菜单动效沿用 bdFun 的 `TeakMorphMenuAnchor`
 * - 本页完全不使用玻璃材质，输入条用纯色胶囊
 */
@Composable
fun LiveRoomScreen(
    room: LiveRoomInfo,
    messages: List<LiveChatMessage>,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    player: androidx.media3.exoplayer.ExoPlayer? = null,
    statusText: String? = null,
    danmakuStatus: String? = null,
    qualities: List<Pair<Int, String>> = emptyList(),
    currentQuality: Int = 0,
    roomEmotes: Map<String, String> = emptyMap(),
    onSelectQuality: (Int) -> Unit = {},
    isPlaying: Boolean = true,
    onTogglePlay: () -> Unit = {},
    danmakuEnabled: Boolean = true,
    onToggleDanmaku: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    onReload: () -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val landscape = LocalConfiguration.current.screenWidthDp >
        LocalConfiguration.current.screenHeightDp
    val menuState = remember { TeakMorphMenuState() }
    var following by remember(room.roomId) { mutableStateOf(room.following) }
    var settingsOpen by remember { mutableStateOf(false) }
    var danmakuSettingsOpen by remember { mutableStateOf(false) }

    // 全屏横屏时和视频播放器一样隐藏状态栏/导航栏；回竖屏恢复
    LaunchedEffect(landscape) {
        val window = context.findLiveActivity()?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (landscape) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (landscape) {
            LandscapeLiveLayout(
                room = room,
                messages = messages,
                following = following,
                menuState = menuState,
                player = player,
                statusText = statusText,
                qualities = qualities,
                currentQuality = currentQuality,
                roomEmotes = roomEmotes,
                onSelectQuality = onSelectQuality,
                isPlaying = isPlaying,
                onTogglePlay = onTogglePlay,
                danmakuEnabled = danmakuEnabled,
                onToggleDanmaku = onToggleDanmaku,
                onToggleFullscreen = onToggleFullscreen,
                onReload = onReload,
                onSend = onSend,
                onBack = onBack,
                onFollow = { following = !following },
                settingsOpen = settingsOpen || danmakuSettingsOpen,
                onOpenSettings = { settingsOpen = true },
                onOpenDanmakuSettings = { danmakuSettingsOpen = true },
            )
        } else {
            PortraitLiveLayout(
                room = room,
                messages = messages,
                following = following,
                menuState = menuState,
                player = player,
                statusText = statusText,
                qualities = qualities,
                currentQuality = currentQuality,
                roomEmotes = roomEmotes,
                onSelectQuality = onSelectQuality,
                isPlaying = isPlaying,
                onTogglePlay = onTogglePlay,
                danmakuEnabled = danmakuEnabled,
                onToggleDanmaku = onToggleDanmaku,
                onToggleFullscreen = onToggleFullscreen,
                onReload = onReload,
                onSend = onSend,
                onBack = onBack,
                onFollow = { following = !following },
                settingsOpen = settingsOpen || danmakuSettingsOpen,
                onOpenSettings = { settingsOpen = true },
                onOpenDanmakuSettings = { danmakuSettingsOpen = true },
            )
        }

        if (settingsOpen) {
            LiveSettingsOverlay(
                landscape = landscape,
                qualities = qualities,
                currentQuality = currentQuality,
                onSelectQuality = { qn ->
                    settingsOpen = false
                    onSelectQuality(qn)
                },
                onDismiss = { settingsOpen = false },
            )
        }

        if (danmakuSettingsOpen) {
            LiveDanmakuSettingsOverlay(
                landscape = landscape,
                onDismiss = { danmakuSettingsOpen = false },
            )
        }

        TeakMorphMenuHost(menuState) { dismiss ->
            LiveMenuRow("复制直播间链接") {
                val url = "https://live.bilibili.com/${room.roomId}"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("直播间链接", url))
                dismiss { context.showToast("已复制直播间链接") }
            }
            LiveMenuRow("在浏览器中打开") {
                dismiss {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://live.bilibili.com/${room.roomId}"),
                            ),
                        )
                    }.onFailure { context.showToast("无法打开链接") }
                }
            }
            LiveMenuRow("举报直播间") {
                dismiss { context.showToast("举报功能稍后接入") }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 竖屏：上播放器，下互动区                                             */
/* ------------------------------------------------------------------ */

@Composable
private fun PortraitLiveLayout(
    room: LiveRoomInfo,
    messages: List<LiveChatMessage>,
    following: Boolean,
    menuState: TeakMorphMenuState,
    player: androidx.media3.exoplayer.ExoPlayer?,
    statusText: String? = null,
    qualities: List<Pair<Int, String>>,
    currentQuality: Int,
    roomEmotes: Map<String, String>,
    onSelectQuality: (Int) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    danmakuEnabled: Boolean,
    onToggleDanmaku: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onReload: () -> Unit,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    settingsOpen: Boolean,
    onOpenSettings: () -> Unit,
    onOpenDanmakuSettings: () -> Unit,
) {
    val density = LocalDensity.current
    val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val bottomInset = if (imeBottom > navBottom) imeBottom else navBottom
    // 互动区要给悬浮输入条让出高度：44dp 输入框 + 上下 8dp 内边距 + 底部系统留白
    val inputAreaHeight = 44.dp + 16.dp + bottomInset
    var inputFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(Modifier.fillMaxSize()) {
        // 背景：直播间封面放大模糊，避免播放器下方纯黑一块
        BackdropGlow(room, Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {
            // ---- 顶栏单独占一条（避开状态栏/前摄），播放器从它下面开始 ----
            // BiliPai 的 LiveRoomLayout 就是 header 放在 0,0，播放器 y = header 高度。
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0C0F)),
            ) {
                LiveTopBar(
                    room = room,
                    following = following,
                    menuState = menuState,
                    onBack = onBack,
                    onFollow = onFollow,
                )
            }

            // ---- 播放器区（16:9）----
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
            ) {
                if (player != null) {
                    LivePlayerSurface(player, Modifier.fillMaxSize())
                } else {
                    LiveVideoPlaceholder(room, statusText, Modifier.fillMaxSize())
                }
                LiveDanmakuOverlay(
                    messages = messages,
                    playing = isPlaying,
                    enabled = danmakuEnabled,
                    fullscreen = false,
                    roomEmotes = roomEmotes,
                    modifier = Modifier.fillMaxSize(),
                )
                LivePlayerChrome(
                    landscape = false,
                    playing = isPlaying,
                    danmakuEnabled = danmakuEnabled,
                    qualityLabel = qualities.firstOrNull { it.first == currentQuality }?.second ?: "清晰度",
                    title = room.title,
                    paused = settingsOpen,
                    onPlayPause = onTogglePlay,
                    onReload = onReload,
                    onToggleDanmaku = onToggleDanmaku,
                    onOpenSettings = onOpenSettings,
                    onOpenDanmakuSettings = onOpenDanmakuSettings,
                    onToggleFullscreen = onToggleFullscreen,
                    onBack = onBack,
                )
            }

            // ---- 互动区：只放弹幕，底部给悬浮输入条让位 ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = inputAreaHeight),
            ) {
                LiveChatList(
                    messages = messages,
                    enabled = danmakuEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
                // 播放器下沿到互动区的过渡：参考视频页的渐变遮罩，直播只用深色
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.86f),
                                0.55f to Color.Black.copy(alpha = 0.38f),
                                1f to Color.Transparent,
                            ),
                        ),
                )
            }
        }

        // 输入框聚焦时铺一层透明点击层：点画面或空白处收起键盘
        if (inputFocused) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    },
            )
        }

        // 输入条：单独悬浮在页面底部，背后是页面背景，没有黑底
        LiveInputBar(
            onSend = onSend,
            onFocusChanged = { inputFocused = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

/* ------------------------------------------------------------------ */
/* 横屏：播放器铺满，弹幕浮在左下                                       */
/* ------------------------------------------------------------------ */

@Composable
private fun LandscapeLiveLayout(
    room: LiveRoomInfo,
    messages: List<LiveChatMessage>,
    following: Boolean,
    menuState: TeakMorphMenuState,
    player: androidx.media3.exoplayer.ExoPlayer?,
    statusText: String? = null,
    qualities: List<Pair<Int, String>>,
    currentQuality: Int,
    roomEmotes: Map<String, String>,
    onSelectQuality: (Int) -> Unit,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    danmakuEnabled: Boolean,
    onToggleDanmaku: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onReload: () -> Unit,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    onFollow: () -> Unit,
    settingsOpen: Boolean,
    onOpenSettings: () -> Unit,
    onOpenDanmakuSettings: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (player != null) {
            LivePlayerSurface(player, Modifier.fillMaxSize())
        } else {
            LiveVideoPlaceholder(room, statusText, Modifier.fillMaxSize())
        }
        LiveDanmakuOverlay(
            messages = messages,
            playing = isPlaying,
            enabled = danmakuEnabled,
            fullscreen = true,
            roomEmotes = roomEmotes,
            modifier = Modifier.fillMaxSize(),
        )
        LivePlayerChrome(
            landscape = true,
            playing = isPlaying,
            danmakuEnabled = danmakuEnabled,
            qualityLabel = qualities.firstOrNull { it.first == currentQuality }?.second ?: "清晰度",
            title = room.title,
            paused = settingsOpen,
            onPlayPause = onTogglePlay,
            onReload = onReload,
            onToggleDanmaku = onToggleDanmaku,
            onOpenSettings = onOpenSettings,
            onOpenDanmakuSettings = onOpenDanmakuSettings,
            onToggleFullscreen = onToggleFullscreen,
            onBack = onToggleFullscreen,
        )
    }
}

/* ------------------------------------------------------------------ */
/* 顶栏：返回键 + 主播信息 + 省略号                                     */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveTopBar(
    room: LiveRoomInfo,
    following: Boolean,
    menuState: TeakMorphMenuState,
    onBack: () -> Unit,
    onFollow: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(50.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 返回键：纯图标，不加玻璃底（压在画面上，白箭头 + 点击热区）
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.ChevronBackward,
                contentDescription = "返回",
                tint = Color.White,
                // 和收藏页等页面一致：38dp 热区 + 22dp 图标
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveAnchorChip(room)
        }

        Spacer(Modifier.width(8.dp))
        FollowButton(following = following, onClick = onFollow, compact = true)
        Spacer(Modifier.width(4.dp))

        // 省略号：同样不加玻璃底，只保留 bdFun 的形变菜单动效
        TeakMorphMenuAnchor(menuState) { onClick ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.More,
                    contentDescription = "更多",
                    tint = Color.White,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

/** 主播头像 + 昵称 + 副标题，完全照 BiliPai 的裸排版：没有底板、没有玻璃。 */
@Composable
private fun LiveAnchorChip(room: LiveRoomInfo) {
    val subtitle = buildString {
        if (room.areaText.isNotBlank()) append(room.areaText)
        if (room.onlineText.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(room.onlineText)
            append("人气")
        }
    }
    Row(
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveAvatar(room, size = 30)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = room.uname.ifBlank { room.title },
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* 真实直播画面：Media3 PlayerView（控制器关闭，界面全部由我们自己画）    */
/* ------------------------------------------------------------------ */

@Composable
private fun LivePlayerSurface(
    player: androidx.media3.exoplayer.ExoPlayer,
    modifier: Modifier = Modifier,
) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier.background(Color.Black),
        factory = { ctx ->
            // 和视频页用的是同一个播放器布局（teak_player_view：TextureView + 无控制器）
            (android.view.LayoutInflater.from(ctx)
                .inflate(com.aritxonly.bdfun.R.layout.teak_player_view, null)
                as androidx.media3.ui.PlayerView).apply {
                useController = false
                setShutterBackgroundColor(android.graphics.Color.BLACK)
                this.player = player
            }
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
    )
}

/* ------------------------------------------------------------------ */
/* 播放器占位：接真实流之前，先用封面 + 渐变撑住画面                      */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveVideoPlaceholder(
    room: LiveRoomInfo,
    statusText: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(Color.Black)) {
        BackdropGlow(room, Modifier.fillMaxSize())
        // 画面上不再显示「直播中」角标，只在左下角保留分区文字
        if (room.areaText.isNotBlank()) {
            Text(
                text = room.areaText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.2.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 12.dp),
            )
        }
        Text(
            text = statusText ?: "正在连接直播…",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
        )
    }
}

/** 直播间背景：和 BiliPai 的 LiveRoomBackground 完全一致——铺满裁切 + 60% 透明度，不叠渐变。 */
@Composable
private fun BackdropGlow(room: LiveRoomInfo, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (room.cover.isNotBlank()) {
            AsyncImage(
                model = room.cover,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.6f },
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* 房间标题 + 关注                                                      */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveRoomTitleRow(
    room: LiveRoomInfo,
    following: Boolean,
    onFollow: () -> Unit,
    compact: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = if (compact) 10.dp else 14.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = room.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.uname,
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.2.sp,
                    maxLines = 1,
                )
                if (room.onlineText.isNotBlank()) {
                    Text(
                        text = "  ·  ${room.onlineText}人气",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 13.2.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        FollowButton(following = following, onClick = onFollow)
    }
}

@Composable
private fun FollowButton(
    following: Boolean,
    onClick: () -> Unit,
    compact: Boolean = false,
) {
    val bg = if (following) Color.White.copy(alpha = 0.14f) else BrandPink
    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(
                horizontal = if (compact) 11.dp else 16.dp,
                vertical = if (compact) 3.dp else 7.dp,
            ),
    ) {
        Text(
            text = if (following) "已关注" else "关注",
            color = if (following) Color.White.copy(alpha = 0.8f) else Color.White,
            fontSize = if (compact) 12.sp else 14.4.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/* ------------------------------------------------------------------ */
/* 弹幕列表                                                            */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveChatList(
    messages: List<LiveChatMessage>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!enabled) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "弹幕已关闭",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 13.sp,
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 2.dp,
            bottom = 6.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            LiveChatRow(message)
        }
    }
}

@Composable
private fun LiveChatRow(message: LiveChatMessage) {
    when (message.kind) {
        LiveChatKind.SYSTEM -> Text(
            text = message.text,
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 14.4.sp,
        )

        LiveChatKind.GIFT -> Text(
            text = buildString {
                append(message.uname)
                append("：")
                append(message.text)
            },
            color = Color(0xFFFFD36B),
            fontSize = 14.4.sp,
        )

        LiveChatKind.SUPER_CHAT -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x33FF6699))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = message.amountText.orEmpty(),
                color = BrandPink,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = message.uname,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.2.sp,
                )
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 14.4.sp,
                )
            }
        }

        LiveChatKind.DANMAKU -> {
            val prefix = buildString {
                if (!message.medal.isNullOrBlank()) {
                    append("[")
                    append(message.medal)
                    append("] ")
                }
                if (message.uname.isNotBlank()) {
                    append(message.uname)
                    append("：")
                }
            }
            when {
                // 整条消息就是一张大表情
                message.emoteUrl != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(prefix, color = Color.White.copy(alpha = 0.92f), fontSize = 14.4.sp)
                    AsyncImage(
                        model = message.emoteUrl,
                        contentDescription = message.text,
                        modifier = Modifier.height(30.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    )
                }
                message.emotes.isEmpty() -> Text(
                    text = prefix + message.text,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.4.sp,
                )
                else -> ChatWithEmotes(prefix, message)
            }
        }
    }
}

/** 文内表情：把 [表情名] 这类 token 换成图片，其余仍按文字排版。 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ChatWithEmotes(prefix: String, message: LiveChatMessage) {
    val segments = remember(message.text, message.emotes) {
        splitByEmotes(message.text, message.emotes.keys)
    }
    androidx.compose.foundation.layout.FlowRow(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(prefix, color = Color.White.copy(alpha = 0.92f), fontSize = 14.4.sp)
        segments.forEach { segment ->
            when (segment) {
                is ChatSegment.Text -> Text(
                    text = segment.value,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 14.4.sp,
                )
                is ChatSegment.Emote -> {
                    val url = message.emotes[segment.token]
                    if (url.isNullOrBlank()) {
                        Text(
                            text = segment.token,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 14.4.sp,
                        )
                    } else {
                        AsyncImage(
                            model = url,
                            contentDescription = segment.token,
                            modifier = Modifier
                                .height((message.emoteHeights[segment.token] ?: 20).dp)
                                .width(((message.emoteHeights[segment.token] ?: 20) * 1.2f).dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )
                    }
                }
            }
        }
    }
}

private sealed interface ChatSegment {
    data class Text(val value: String) : ChatSegment
    data class Emote(val token: String) : ChatSegment
}

/** 按表情 token 把文本切开；token 可能是「[笑]」也可能是不带括号的「笑」。 */
private fun splitByEmotes(text: String, tokens: Set<String>): List<ChatSegment> {
    if (tokens.isEmpty()) return listOf(ChatSegment.Text(text))
    val sorted = tokens.sortedByDescending { it.length }
    val result = mutableListOf<ChatSegment>()
    var index = 0
    val buffer = StringBuilder()
    while (index < text.length) {
        val match = sorted.firstOrNull { text.startsWith(it, index) }
        if (match != null) {
            if (buffer.isNotEmpty()) {
                result += ChatSegment.Text(buffer.toString())
                buffer.clear()
            }
            result += ChatSegment.Emote(match)
            index += match.length
        } else {
            buffer.append(text[index])
            index++
        }
    }
    if (buffer.isNotEmpty()) result += ChatSegment.Text(buffer.toString())
    return result
}

/* ------------------------------------------------------------------ */
/* 输入条：和视频页同款的胶囊，但用纯色底（本页不用玻璃）                */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveInputBar(
    onSend: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // 和视频页输入条一致：底部让出「输入法高度 / 导航栏高度」里较大的那个
    val density = LocalDensity.current
    val imeDp = with(density) { WindowInsets.ime.getBottom(density).toDp().value }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    val bottomDp = if (imeDp > navDp) imeDp else navDp

    fun send() {
        val value = text.trim()
        if (value.isEmpty()) return
        onSend(value)
        text = ""
        focusManager.clearFocus()
    }

    Row(
        modifier = modifier
            .padding(bottom = bottomDp.dp, start = 12.dp, end = 12.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(Capsule())
                
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 46.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = ComposerTextStyle.copy(color = Color.White),
                    cursorBrush = SolidColor(BrandPink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "发一条友善的弹幕",
                                    color = Color.White.copy(alpha = 0.45f),
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
                    tint = if (text.isNotBlank()) BrandPink else Color.White.copy(alpha = 0.45f),
                    onClick = { send() },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "\uD83D\uDE00", fontSize = 18.sp)
        }
    }
}

/* ------------------------------------------------------------------ */
/* 小零件                                                              */
/* ------------------------------------------------------------------ */

@Composable
private fun LiveAvatar(room: LiveRoomInfo, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF2A2D33)),
        contentAlignment = Alignment.Center,
    ) {
        if (room.face.isNotBlank()) {
            AsyncImage(
                model = room.face,
                contentDescription = room.uname,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Text(
                text = room.uname.take(1),
                color = Color.White,
                fontSize = (size / 2.4f).sp,
            )
        }
    }
}

@Composable
private fun LiveMenuRow(label: String, onClick: () -> Unit) {
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = label, color = colors.onBackground, fontSize = 14.sp)
    }
}

private tailrec fun Context.findLiveActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findLiveActivity()
    else -> null
}
