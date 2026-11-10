package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Delete
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.MessageItem
import com.aritxonly.bdfun.data.MessageSession
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TeakTopGlassButton
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import kotlinx.coroutines.async
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
    RegisterScrollTop(listState, "message")
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabProgress = remember { mutableFloatStateOf(0f) }
    val sessionsByTab = remember { mutableStateMapOf<Int, List<MessageSession>>() }
    val loadingByTab = remember { mutableStateMapOf<Int, Boolean>() }
    val errorByTab = remember { mutableStateMapOf<Int, String?>() }
    val sessionTsByTab = remember { mutableStateMapOf<Int, Long>() }
    val hasMoreByTab = remember { mutableStateMapOf<Int, Boolean>() }
    val loadingMoreByTab = remember { mutableStateMapOf<Int, Boolean>() }
    var consumedReadAll by remember { mutableIntStateOf(0) }

    LaunchedEffect(MessagePageBus.readAllRequest) {
        val tick = MessagePageBus.readAllRequest
        if (tick == 0 || tick == consumedReadAll) return@LaunchedEffect
        consumedReadAll = tick
        if (!loggedIn) {
            context.showToast("登录后可操作")
            return@LaunchedEffect
        }
        val snapshot = sessionsByTab.values.flatten()
        scope.launch {
            runCatching { api.clearAllUnread(snapshot) }
            sessionsByTab.keys.toList().forEach { key ->
                sessionsByTab[key] = sessionsByTab[key].orEmpty().map { it.copy(unread = 0) }
            }
            context.showToast("已全部标为已读")
        }
    }

    BackHandler(onBack = onBack)
    TrackTeakPageScroll(listState)

    suspend fun loadTab(tab: Int, more: Boolean = false) {
        if (!loggedIn) return
        if (more) {
            if (loadingMoreByTab[tab] == true || hasMoreByTab[tab] == false) return
            loadingMoreByTab[tab] = true
        } else {
            loadingByTab[tab] = true
            errorByTab[tab] = null
        }
        val lastTs = if (more) sessionTsByTab[tab] ?: 0L else 0L
        val loadedPair = runCatching {
            api.messageSessions(
                followingOnly = tab == 1,
                sessionTs = lastTs,
                pageSize = 25,
                onEnriched = { enriched ->
                    // 首屏已经显示；昵称/头像补全后在后台静默替换。
                    scope.launch {
                        if (!more) {
                            val current = sessionsByTab[tab]
                            if (current != null &&
                                current.firstOrNull()?.id == enriched.firstOrNull()?.id
                            ) {
                                sessionsByTab[tab] = enriched
                            }
                        }
                    }
                },
            )
        }.onFailure { errorByTab[tab] = it.message ?: "消息加载失败" }.getOrNull()
        var loaded = loadedPair?.first.orEmpty()
        var hasMore = loadedPair?.second == true
        // 会话接口偶发空/失败时，退回系统通知列表，至少保证消息页有内容。
        if (!more && loaded.isEmpty()) {
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
            hasMore = false
        }
        sessionsByTab[tab] = if (more) {
            (sessionsByTab[tab].orEmpty() + loaded).distinctBy { it.id }
        } else {
            loaded
        }
        sessionTsByTab[tab] = loaded.lastOrNull()?.sessionTs ?: lastTs
        hasMoreByTab[tab] = hasMore
        if (more) loadingMoreByTab[tab] = false else loadingByTab[tab] = false
    }

    LaunchedEffect(loggedIn) {
        if (!loggedIn) {
            sessionsByTab.clear()
            loadingByTab.clear()
            return@LaunchedEffect
        }
        // 只先加载当前页；另一页等用户切过去再取，减少首次进入的请求量。
        loadTab(tabIndex)
    }

    LaunchedEffect(tabIndex) {
        if (loggedIn && sessionsByTab[tabIndex] == null && loadingByTab[tabIndex] != true) {
            loadTab(tabIndex)
        }
    }

    LaunchedEffect(tabIndex, sessionsByTab[tabIndex]?.size, hasMoreByTab[tabIndex]) {
        val key = tabIndex
        androidx.compose.runtime.snapshotFlow {
            val info = listState.layoutInfo
            info.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.collect { last ->
            val size = sessionsByTab[key]?.size ?: 0
            if (size > 0 && last >= size - 2 && hasMoreByTab[key] == true) {
                loadTab(key, more = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
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
                    ) { ResourceGlyph(res = TeakIcon.Like, tint = MessagePink, size = ShortcutGlyphSize) }
                    MessageShortcut(
                        label = "新增粉丝",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenFans,
                    ) { PersonGlyph(MessageBlue) }
                }
            }

            item(key = "tabs") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    TeakPagerTabs(
                        options = listOf("全部", "我关注的人"),
                        selected = tabIndex,
                        onSelect = { tabIndex = it },
                        itemWidth = 96.dp,
                        backdrop = null,
                        plain = true,
                        position = tabProgress,
                    )
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
                    SwipeCardPager(
                        selected = tabIndex,
                        pageCount = 2,
                        onSelected = { tabIndex = it },
                        progressState = tabProgress,
                    ) { page ->
                        SessionCard(
                            sessions = sessionsByTab[page].orEmpty(),
                            loading = loadingByTab[page] == true,
                            error = errorByTab[page],
                            hasMore = hasMoreByTab[page] == true,
                            loadingMore = loadingMoreByTab[page] == true,
                            onOpen = { session ->
                                if (!session.isSystem && session.talkerId > 0L) {
                                    com.aritxonly.bdfun.TeakNavigator.openPrivateChat(context, session)
                                } else {
                                    context.showToast("系统消息请在消息中心查看")
                                }
                            },
                            onLoadMore = {
                                scope.launch { loadTab(page, more = true) }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 右上角操作放在 Activity 的 overlay 里：真玻璃必须在 backdrop 采集层之外，否则会 SIGSEGV。 */
@Composable
fun MessageTopActions() {
    TeakTopGlassButton(onClick = { MessagePageBus.requestReadAll() }) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = MiuixIcons.Delete, contentDescription = "全部已读",
            tint = teakGlassGlyphColor(),
            modifier = Modifier.size(21.dp),
        )
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
            fontWeight = if (selected) FontWeight.Normal else FontWeight.Normal,
        )
    }
}

@Composable
private fun SessionCard(
    sessions: List<MessageSession>,
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    loadingMore: Boolean,
    onOpen: (MessageSession) -> Unit,
    onLoadMore: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val view = androidx.compose.ui.platform.LocalView.current
    // 底部「加载更多」只允许触发一次；加载结束后再重新武装。
    // 原来 onGloballyPositioned 每次布局都会调用，一次滑到底会连发好几页请求。
    var loadMoreRequested by remember { mutableStateOf(false) }
    LaunchedEffect(loadingMore, hasMore) {
        if (!loadingMore) loadMoreRequested = false
    }
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
        } else if (sessions.isEmpty() && loading) {
            // 首屏请求还没回来时给一个明确占位，不再是一块空白卡片。
            Text(
                text = "加载中…",
                modifier = Modifier.fillMaxWidth().padding(vertical = 46.dp),
                color = colors.onBackgroundVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        sessions.forEachIndexed { index, session ->
            SessionRow(session = session, onClick = { onOpen(session) })
        }
        if (hasMore) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val y = coords.positionInWindow().y
                        if (!loadMoreRequested &&
                            !loadingMore &&
                            y in 0f..(view.height * 0.92f)
                        ) {
                            loadMoreRequested = true
                            onLoadMore()
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (loadingMore) "加载中…" else "上滑加载更多",
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                )
            }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.name.ifBlank { "消息" },
                    color = colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatSessionTime(session.time),
                    color = colors.onBackgroundVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = session.preview.ifBlank { "暂无新消息" },
                color = colors.onBackgroundVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (session.unread > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MessageRed),
            )
        }
    }
}

@Composable
fun MessageDetailScreen(api: BiliApi, kind: MessageFeedKind) {
    val colors = MiuixTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    RegisterScrollTop(listState, "message_detail")
    TrackTeakPageScroll(listState)
    val myName = AppGraph.session.value?.name.orEmpty()
    var tab by remember(kind) { mutableIntStateOf(0) }
    val tabProgress = remember(kind) { mutableFloatStateOf(0f) }
    val itemsByTab = remember(kind) { mutableStateMapOf<Int, List<MessageItem>>() }
    val loadingByTab = remember(kind) { mutableStateMapOf<Int, Boolean>() }
    val errorByTab = remember(kind) { mutableStateMapOf<Int, String?>() }
    val followedOverride = remember(kind) { mutableStateMapOf<Long, Boolean>() }
    val followBusy = remember(kind) { mutableStateMapOf<Long, Boolean>() }
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
    val pageCount = if (tabs.isEmpty()) 1 else tabs.size

    suspend fun loadPage(page: Int) {
        loadingByTab[page] = true
        errorByTab[page] = null
        val result = if (kind == MessageFeedKind.Fans) {
            runCatching { api.newFollowers() }
        } else {
            val type = when (kind) {
                MessageFeedKind.Reply -> if (page == 2) 1 else 0
                MessageFeedKind.Like -> 2
                MessageFeedKind.Fans -> 3
            }
            runCatching { api.messages(type) }
        }
        val loaded = result
            .onFailure { errorByTab[page] = it.message ?: "消息加载失败" }
            .getOrDefault(emptyList())
        itemsByTab[page] = if (kind != MessageFeedKind.Fans && page == 1) {
            // 接口不一定返回关注关系。整页都没有关注标记时退回全部，
            // 否则用户看到的就是一个永远空白的「我关注的人」。
            if (loaded.any { it.followed }) loaded.filter { it.followed } else loaded
        } else {
            loaded
        }
        loadingByTab[page] = false
    }

    LaunchedEffect(kind) {
        // 「我关注的人」和「全部」是同一个接口，直接复用第一页做过滤，不再重复请求。
        val first = async { loadPage(0) }
        if (kind == MessageFeedKind.Reply) {
            launch { loadPage(2) }
        }
        first.await()
        if (pageCount > 1) {
            val loaded = itemsByTab[0].orEmpty()
            itemsByTab[1] = if (loaded.any { it.followed }) {
                loaded.filter { it.followed }
            } else {
                loaded
            }
        }
    }

    fun openMessageItem(item: MessageItem) {
        com.aritxonly.bdfun.data.LogCollector.record(
            "MSGOPEN",
            "id=${item.id} business=${item.businessId} aid=${item.aid} bvid=${item.bvid} " +
                "root=${item.rootRpid} target=${item.targetRpid} source=${item.sourceRpid} " +
                "kind=$kind",
        )
        // 视频稿件的 aid 都在百亿以内；动态/opus 的 subject_id 是 19 位的超大数。
        // 只要 aid 看起来是视频，就别被 business_id 的稀奇值挡住。
        val looksLikeVideo = item.aid in 1L..9_999_999_999L
        if (item.businessId != 0 && item.businessId != 1 && !looksLikeVideo) {
            // 动态评论等类型暂时没有对应的站内详情跳转，避免用动态 id 当视频打开。
            context.showToast("这条消息来自动态评论，暂不支持直接定位")
            return
        }
        if (item.aid <= 0L && item.bvid.isBlank()) {
            context.showToast("这条消息没有可打开的视频")
            return
        }
        val focus = if (kind == MessageFeedKind.Like) {
            item.targetRpid.takeIf { it > 0L } ?: item.sourceRpid
        } else {
            // 回复与@：优先用 native_uri 里的 comment_secondary_id（就是新回复本身）。
            item.targetRpid.takeIf { it > 0L }
                ?: item.sourceRpid.takeIf { it > 0L }
                ?: item.rootRpid
        }
        val root = item.rootRpid.takeIf { it > 0L }
            ?: item.targetRpid.takeIf { it > 0L }
            ?: focus
        com.aritxonly.bdfun.TeakNavigator.openVideoAtComment(
            context = context,
            aid = item.aid,
            bvid = item.bvid,
            title = item.title,
            rootRpid = root,
            targetRpid = focus,
            seekMs = item.seekMs,
        )
    }

    fun toggleFollow(item: MessageItem) {
        val mid = item.userMid
        if (mid <= 0L || followBusy[mid] == true) return
        val target = !(followedOverride[mid] ?: item.followed)
        followedOverride[mid] = target
        followBusy[mid] = true
        scope.launch {
            val result = runCatching { api.followUser(mid, target) }
            followBusy[mid] = false
            if (result.isSuccess) {
                context.showToast(if (target) "已回关" else "已取消关注")
            } else {
                followedOverride[mid] = !target
                context.showToast(result.exceptionOrNull()?.message ?: "操作失败")
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    TeakPagerTabs(
                        options = tabs,
                        selected = tab,
                        onSelect = { tab = it },
                        itemWidth = if (tabs.size > 2) 76.dp else 96.dp,
                        backdrop = null,
                        plain = true,
                        position = tabProgress,
                    )
                }
            }
        }
        item(key = "list") {
            SwipeCardPager(
                selected = tab,
                pageCount = pageCount,
                onSelected = { tab = it },
                progressState = tabProgress,
            ) { page ->
                val pageItems = itemsByTab[page].orEmpty()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 新增粉丝的圆角卡片整体往下 5dp。
                        .padding(top = if (kind == MessageFeedKind.Fans) 5.dp else 0.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface),
                ) {
                    when {
                        loadingByTab[page] == true -> Text(
                            text = "加载中…",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 46.dp),
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                        pageItems.isEmpty() -> Text(
                            text = if (kind == MessageFeedKind.Fans) {
                                "暂无新增粉丝"
                            } else {
                                errorByTab[page] ?: "暂无消息"
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 46.dp),
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                        else -> pageItems.forEach { item ->
                            when (kind) {
                                MessageFeedKind.Like -> LikeRow(
                                    item = item,
                                    onClick = { openMessageItem(item) },
                                )
                                MessageFeedKind.Fans -> FanRow(
                                    item = item,
                                    followed = followedOverride[item.userMid] ?: item.followed,
                                    busy = followBusy[item.userMid] == true,
                                    onFollow = { toggleFollow(item) },
                                )
                                MessageFeedKind.Reply -> ReplyRow(
                                    item = item,
                                    myName = myName,
                                    onClick = { openMessageItem(item) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------ 消息卡片：回复 / 喜欢 / 粉丝 */

@Composable
private fun ReplyRow(
    item: MessageItem,
    myName: String,
    onClick: () -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // 头像缩小 20%，把宽度让给回复正文。
        MessageAvatar(item.avatar, size = 35.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.onBackground)) {
                            append(item.name.ifBlank { "用户" })
                        }
                        append(" ")
                        withStyle(SpanStyle(color = colors.onBackgroundVariant)) {
                            append(item.action)
                        }
                    },
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
            }
            if (item.content.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                EmoteText(
                    text = item.content,
                    color = colors.onBackground,
                    fontSize = 14.85.sp,
                    lineHeight = 23.sp,
                    maxLines = Int.MAX_VALUE,
                )
            }
            if (item.quote.isNotBlank()) {
                Spacer(Modifier.height(7.dp))
                QuoteBlock(
                    name = myName.ifBlank { "我" },
                    text = item.quote,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMessageDate(item.time),
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(12.dp))
                SmallMessageAction(TeakIcon.CommentBubble, "回复")
                Spacer(Modifier.width(12.dp))
                SmallMessageAction(TeakIcon.LikeOutline, "点赞")
                Spacer(Modifier.weight(1f))
                MessageMoreDots { context.showToast("暂未开放") }
            }
        }
        if (item.videoSubject || item.subjectText.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            MessageSubject(item)
        }
    }
}

@Composable
private fun LikeRow(item: MessageItem, onClick: () -> Unit = {}) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val summary = when {
        item.likeNames.size >= 2 ->
            "${item.likeNames[0]}、${item.likeNames[1]} 等总计${item.likeCount}人${item.action}"
        item.likeNames.size == 1 -> "${item.likeNames[0]} ${item.action}"
        else -> "有人${item.action}"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMessageDate(item.time),
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.weight(1f))
                MessageMoreDots { context.showToast("暂未开放") }
            }
        }
        val likedText = item.subjectText
            .ifBlank { item.source }
            .ifBlank { item.content }
        if (item.videoSubject && item.subjectCover.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            MessageSubject(item)
        } else if (likedText.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            // 与「回复与@」右侧同一套灰色小字：显示被点赞的评论原文。
            Text(
                text = likedText,
                modifier = Modifier.width(84.dp),
                color = colors.onBackgroundVariant.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }
    }
}

@Composable
private fun FanRow(
    item: MessageItem,
    followed: Boolean,
    busy: Boolean,
    onFollow: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessageAvatar(item.avatar)
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name.ifBlank { "用户" },
                color = colors.onBackground,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "关注了你 · ${formatMessageDate(item.time)}",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        FollowBackCapsule(
            followed = followed,
            busy = busy,
            onClick = onFollow,
        )
    }
}

@Composable
private fun FollowBackCapsule(followed: Boolean, busy: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val shape = com.kyant.shapes.Capsule()
    Box(
        modifier = Modifier
            .width(74.dp)
            .height(30.dp)
            .clip(shape)
            .background(if (followed) colors.surfaceContainerHigh else BrandPink)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                busy -> "处理中"
                followed -> "已关注"
                else -> "+ 回关"
            },
            color = if (followed) colors.onBackgroundVariant else Color.White,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

/** 消息右侧的「被回复 / 被赞」对象：视频放封面，评论放原文。 */
@Composable
private fun MessageSubject(item: MessageItem) {
    val colors = MiuixTheme.colorScheme
    // 右侧对象统一用正方形区域；缩到 60dp 给左边正文留空间。
    val size = 60.dp
    if (item.videoSubject) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            if (item.subjectCover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(item.subjectCover),
                    contentDescription = if (item.title.isNotBlank()) item.title else "视频封面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp),
                    color = colors.onBackgroundVariant,
                    fontSize = 9.sp,
                    lineHeight = 12.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.size(size),
        ) {
            Text(
                text = item.subjectText,
                modifier = Modifier.fillMaxSize(),
                color = colors.onBackgroundVariant.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }
    }
}

/** 楼中楼里的浅色引用：左边一条竖线 + 「我：原文」。 */
@Composable
private fun QuoteBlock(name: String, text: String) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(1.dp))
                .background(colors.onBackgroundVariant.copy(alpha = 0.22f)),
        )
        Spacer(Modifier.width(9.dp))
        EmoteText(
            text = text,
            prefix = name,
            color = colors.onBackgroundVariant.copy(alpha = 0.82f),
            prefixColor = colors.onBackgroundVariant.copy(alpha = 0.9f),
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            maxLines = Int.MAX_VALUE,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SmallMessageAction(icon: Int, label: String) {
    val colors = MiuixTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        ResourceGlyph(res = icon, tint = colors.onBackgroundVariant, size = 14.dp)
        Spacer(Modifier.width(3.dp))
        Text(text = label, color = colors.onBackgroundVariant, fontSize = 11.sp)
    }
}

/** 三个圆点的省略号按钮：和评论区同款，圆底，不是方形文字块。 */
@Composable
private fun MessageMoreDots(onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = MiuixIcons.More,
            contentDescription = "更多",
            tint = colors.onBackgroundVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MessageAvatar(url: String, size: androidx.compose.ui.unit.Dp = 44.dp) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(colors.surfaceContainerHigh),
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

/** 快捷入口三个图标统一按这个尺寸绘制，视觉大小保持一致。 */
private val ShortcutGlyphSize = 28.dp

/** 回复与@：实心圆角对话气泡 + 左下小尾巴（参考用户提供图一）。 */
@Composable
private fun ChatGlyph(color: Color) {
    Canvas(modifier = Modifier.size(ShortcutGlyphSize)) {
        val w = size.width
        val h = size.height
        // 主体：略扁的椭圆气泡，留出底部尾巴的位置
        drawOval(
            color = color,
            topLeft = Offset(w * 0.05f, h * 0.08f),
            size = Size(w * 0.90f, h * 0.66f),
        )
        // 尾巴：从气泡左下伸出，向下再收回气泡底边
        val tail = Path().apply {
            moveTo(w * 0.30f, h * 0.66f)
            cubicTo(w * 0.30f, h * 0.80f, w * 0.27f, h * 0.88f, w * 0.21f, h * 0.93f)
            cubicTo(w * 0.31f, h * 0.94f, w * 0.43f, h * 0.85f, w * 0.48f, h * 0.70f)
            close()
        }
        drawPath(tail, color)
    }
}

@Composable
private fun PersonGlyph(color: Color) {
    Canvas(modifier = Modifier.size(ShortcutGlyphSize)) {
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

internal fun formatMessageDate(seconds: Long): String {
    if (seconds <= 0L) return ""
    return SimpleDateFormat("M月d日", Locale.CHINA).format(Date(seconds * 1000L))
}
