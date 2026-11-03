package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.MessageItem
import com.aritxonly.bdfun.data.MessageSession
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TeakTopGlassButton
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val MessageGreen = Color(0xFF3CCF8E)
private val MessagePink = Color(0xFFF45B86)
private val MessageBlue = Color(0xFF3CA9E8)
private val MessageRed = Color(0xFFF04B4B)

enum class MessageFeedKind { Reply, Like, Fans }

@Composable
fun MessageScreen(
    api: BiliApi,
    loggedIn: Boolean,
    onNeedLogin: () -> Unit,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenReplies: () -> Unit = {},
    onOpenLikes: () -> Unit = {},
    onOpenFans: () -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var followingOnly by remember { mutableStateOf(false) }
    var sessions by remember { mutableStateOf<List<MessageSession>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var consumedReadAll by remember { mutableIntStateOf(0) }

    LaunchedEffect(MessagePageBus.readAllRequest) {
        val tick = MessagePageBus.readAllRequest
        if (tick == 0 || tick == consumedReadAll) return@LaunchedEffect
        consumedReadAll = tick
        if (!loggedIn) {
            context.showToast("登录后可操作")
            return@LaunchedEffect
        }
        val snapshot = sessions
        scope.launch {
            runCatching { api.clearAllUnread(snapshot) }
            sessions = sessions.map { it.copy(unread = 0) }
            context.showToast("已全部标为已读")
        }
    }

    BackHandler(onBack = onBack)
    TrackTeakPageScroll(listState)

    LaunchedEffect(loggedIn, followingOnly) {
        if (!loggedIn) {
            sessions = emptyList()
            return@LaunchedEffect
        }
        loading = true
        error = null
        var loaded = runCatching { api.messageSessions(followingOnly) }
            .onFailure { error = it.message ?: "消息加载失败" }
            .getOrDefault(emptyList())
        // 会话接口偶发空/失败时，退回系统通知列表，至少保证消息页有内容。
        if (loaded.isEmpty()) {
            loaded = runCatching { api.messages(3) }
                .getOrDefault(emptyList())
                .map { item ->
                    MessageSession(
                        id = item.id,
                        talkerId = item.userMid,
                        sessionType = 1,
                        name = item.name.ifBlank { item.title },
                        avatar = item.avatar,
                        preview = item.content.ifBlank { item.title },
                        time = item.time,
                        unread = 0,
                        isSystem = true,
                    )
                }
        }
        sessions = loaded
        loading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 40.dp,
            ),
        ) {
            item(key = "header") {
                TeakLargeTitle(title = "消息")
            }

            item(key = "shortcuts") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MessageShortcut(
                        label = "回复与@",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenReplies,
                    ) { ChatGlyph(MessageGreen) }
                    MessageShortcut(
                        label = "收到喜欢",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLikes,
                    ) { ThumbGlyph(MessagePink) }
                    MessageShortcut(
                        label = "新增粉丝",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenFans,
                    ) { PersonGlyph(MessageBlue) }
                }
            }

            item(key = "tabs") {
                Row(
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MessageTab("全部", !followingOnly) { followingOnly = false }
                    MessageTab("我关注的人", followingOnly) { followingOnly = true }
                }
            }

            if (!loggedIn) {
                item(key = "login") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("登录后可查看消息", color = colors.onBackgroundVariant, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.primary)
                                .clickable(onClick = onNeedLogin)
                                .padding(horizontal = 26.dp, vertical = 11.dp),
                        ) {
                            Text("扫码登录", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                item(key = "sessions") {
                    SessionCard(
                        sessions = sessions,
                        loading = loading,
                        error = error,
                        onOpen = {
                            context.showToast("私信功能暂未开放")
                        },
                    )
                }
            }
        }
    }
}

/** 右上角操作放在 Activity 的 overlay 里：真玻璃必须在 backdrop 采集层之外，否则会 SIGSEGV。 */
@Composable
fun MessageTopActions() {
    TeakTopGlassButton(onClick = { MessagePageBus.requestReadAll() }) {
        MarkReadGlyph()
    }
    TeakTopGlassButton(onClick = { /* 省略号暂时不做功能 */ }) {
        ThreeDotsGlyph()
    }
}

object MessagePageBus {
    var readAllRequest by mutableIntStateOf(0)
        private set

    fun requestReadAll() {
        readAllRequest++
    }
}

@Composable
private fun MessageShortcut(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(58.dp),
            contentAlignment = Alignment.Center,
        ) {
            glyph()
        }
        Spacer(Modifier.height(7.dp))
        Text(text = label, color = colors.onBackground, fontSize = 13.sp)
    }
}

@Composable
private fun MessageTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BrandPink.copy(alpha = 0.13f) else Color.Transparent)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (selected) BrandPink else colors.onBackground,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun SessionCard(
    sessions: List<MessageSession>,
    loading: Boolean,
    error: String?,
    onOpen: (MessageSession) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface),
    ) {
        if (sessions.isEmpty() && !loading) {
            Text(
                text = error ?: "暂无消息",
                modifier = Modifier.fillMaxWidth().padding(vertical = 46.dp),
                color = colors.onBackgroundVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        sessions.forEachIndexed { index, session ->
            SessionRow(session = session, onClick = { onOpen(session) })
        }
    }
}

@Composable
private fun SessionRow(session: MessageSession, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceContainerHigh),
        ) {
            if (session.avatar.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(session.avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.name.ifBlank { "消息" },
                color = colors.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = session.preview.ifBlank { "暂无新消息" },
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatSessionTime(session.time),
                color = colors.onBackgroundVariant.copy(alpha = 0.7f),
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(12.dp))
            if (session.unread > 0) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MessageRed))
            }
        }
    }
}

@Composable
fun MessageDetailScreen(api: BiliApi, kind: MessageFeedKind) {
    val colors = MiuixTheme.colorScheme
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    var tab by remember(kind) { mutableIntStateOf(0) }
    var items by remember(kind) { mutableStateOf<List<MessageItem>>(emptyList()) }
    var loading by remember(kind) { mutableStateOf(false) }
    var error by remember(kind) { mutableStateOf<String?>(null) }
    val tabs = when (kind) {
        MessageFeedKind.Reply -> listOf("全部", "我关注的人", "@我")
        MessageFeedKind.Like -> listOf("全部", "我关注的人")
        MessageFeedKind.Fans -> emptyList()
    }
    val title = when (kind) {
        MessageFeedKind.Reply -> "回复与@"
        MessageFeedKind.Like -> "收到喜欢"
        MessageFeedKind.Fans -> "新增粉丝"
    }

    LaunchedEffect(kind, tab) {
        if (kind == MessageFeedKind.Fans) return@LaunchedEffect
        loading = true
        error = null
        val type = when (kind) {
            MessageFeedKind.Reply -> if (tab == 2) 1 else 0
            MessageFeedKind.Like -> 2
            MessageFeedKind.Fans -> 3
        }
        val loaded = runCatching { api.messages(type) }
            .onFailure { error = it.message ?: "消息加载失败" }
            .getOrDefault(emptyList())
        items = if (tab == 1) loaded.filter { it.followed } else loaded
        loading = false
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 40.dp,
        ),
    ) {
        item(key = "header") {
            TeakLargeTitle(title = title)
        }
        if (tabs.isNotEmpty()) {
            item(key = "tabs") {
                Row(
                    modifier = Modifier.padding(top = 16.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    tabs.forEachIndexed { index, label ->
                        MessageTab(label, tab == index) { tab = index }
                    }
                }
            }
        }
        item(key = "list") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surface),
            ) {
                when {
                    loading -> Text(
                        text = "加载中…",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 46.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    items.isEmpty() -> Text(
                        text = if (kind == MessageFeedKind.Fans) "暂无新增粉丝" else (error ?: "暂无消息"),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 46.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    else -> items.forEachIndexed { index, item ->
                        if (kind == MessageFeedKind.Like) {
                            LikeRow(item)
                        } else {
                            ReplyRow(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyRow(item: MessageItem) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        MessageAvatar(item.avatar)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name.ifBlank { "用户" },
                    color = colors.onBackground,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = item.action,
                    color = colors.onBackgroundVariant,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                )
            }
            if (item.content.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = item.content,
                    color = colors.onBackground,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMessageDate(item.time),
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(14.dp))
                Text("回复", color = colors.onBackgroundVariant, fontSize = 11.sp)
                Spacer(Modifier.width(12.dp))
                Text("点赞", color = colors.onBackgroundVariant, fontSize = 11.sp)
            }
        }
        if (item.source.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.source,
                modifier = Modifier.width(76.dp),
                color = colors.onBackgroundVariant.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
        Text(
            text = "⋮",
            modifier = Modifier
                .padding(start = 6.dp)
                .clickable(interactionSource = null, indication = null) {
                    context.showToast("暂未开放")
                }
                .padding(horizontal = 2.dp),
            color = colors.onBackgroundVariant,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun LikeRow(item: MessageItem) {
    val colors = MiuixTheme.colorScheme
    val summary = when {
        item.likeNames.size >= 2 ->
            "${item.likeNames[0]}、${item.likeNames[1]} 等总计${item.likeCount}人赞了我的评论"
        item.likeNames.size == 1 -> "${item.likeNames[0]} 赞了我的评论"
        else -> "有人赞了我的评论"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        MessageAvatar(item.avatar)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary,
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatMessageDate(item.time),
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
        if (item.source.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.source,
                modifier = Modifier.width(76.dp),
                color = colors.onBackgroundVariant.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
            )
        }
        Text(
            text = "⋮",
            modifier = Modifier.padding(start = 6.dp, top = 2.dp),
            color = colors.onBackgroundVariant,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun MessageAvatar(url: String) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceContainerHigh),
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = rememberImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/* ---------------------------------------------------------------- glyphs */

@Composable
private fun ChatGlyph(color: Color) {
    Canvas(modifier = Modifier.size(30.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.12f),
            size = Size(w * 0.84f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.2f, w * 0.2f),
        )
        val tail = Path().apply {
            moveTo(w * 0.3f, h * 0.7f)
            lineTo(w * 0.24f, h * 0.9f)
            lineTo(w * 0.46f, h * 0.73f)
            close()
        }
        drawPath(tail, color)
        listOf(0.34f, 0.5f, 0.66f).forEach { x ->
            drawCircle(Color.White.copy(alpha = 0.92f), w * 0.045f, Offset(w * x, h * 0.43f))
        }
    }
}

@Composable
private fun ThumbGlyph(color: Color) {
    Canvas(modifier = Modifier.size(30.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.46f)
            lineTo(w * 0.34f, h * 0.46f)
            lineTo(w * 0.34f, h * 0.86f)
            lineTo(w * 0.18f, h * 0.86f)
            close()
        }
        drawPath(path, color)
        val thumb = Path().apply {
            moveTo(w * 0.4f, h * 0.46f)
            lineTo(w * 0.52f, h * 0.18f)
            quadraticBezierTo(w * 0.58f, h * 0.1f, w * 0.64f, h * 0.18f)
            lineTo(w * 0.6f, h * 0.42f)
            lineTo(w * 0.84f, h * 0.42f)
            quadraticBezierTo(w * 0.9f, h * 0.42f, w * 0.88f, h * 0.52f)
            lineTo(w * 0.8f, h * 0.82f)
            quadraticBezierTo(w * 0.78f, h * 0.88f, w * 0.7f, h * 0.88f)
            lineTo(w * 0.4f, h * 0.88f)
            close()
        }
        drawPath(thumb, color)
    }
}

@Composable
private fun PersonGlyph(color: Color) {
    Canvas(modifier = Modifier.size(30.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(color, w * 0.18f, Offset(w * 0.5f, h * 0.32f))
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.2f, h * 0.55f),
            size = Size(w * 0.6f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.3f, w * 0.3f),
        )
    }
}

@Composable
private fun MarkReadGlyph() {
    val color = teakGlassGlyphColor()
    Canvas(modifier = Modifier.size(21.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.12f
        // 双勾：表示“全部已读/清理未读”
        drawLine(
            color,
            Offset(w * 0.08f, h * 0.55f),
            Offset(w * 0.34f, h * 0.8f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(w * 0.34f, h * 0.8f),
            Offset(w * 0.78f, h * 0.24f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(w * 0.5f, h * 0.62f),
            Offset(w * 0.66f, h * 0.8f),
            stroke,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(w * 0.66f, h * 0.8f),
            Offset(w * 0.94f, h * 0.48f),
            stroke,
            StrokeCap.Round,
        )
    }
}

@Composable
private fun ThreeDotsGlyph() {
    val color = teakGlassGlyphColor()
    Canvas(modifier = Modifier.size(21.dp)) {
        val r = size.width * 0.07f
        listOf(0.2f, 0.5f, 0.8f).forEach { y ->
            drawCircle(color, r, Offset(size.width * 0.5f, size.height * y))
        }
    }
}

/* ---------------------------------------------------------------- time */

private fun formatSessionTime(seconds: Long): String {
    if (seconds <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = seconds * 1000L }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val sameDay = sameYear && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(seconds * 1000L))
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (yesterday) {
        return "昨天 " + SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(seconds * 1000L))
    }
    return SimpleDateFormat(if (sameYear) "M月d日" else "yyyy年M月d日", Locale.CHINA)
        .format(Date(seconds * 1000L))
}

private fun formatMessageDate(seconds: Long): String {
    if (seconds <= 0L) return ""
    return SimpleDateFormat("M月d日", Locale.CHINA).format(Date(seconds * 1000L))
}
