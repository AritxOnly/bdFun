package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Contacts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import com.aritxonly.bdfun.data.MessageCursor
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

enum class MessageFeedKind { Reply, Like, Fans, System }

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
    var sessions by remember { mutableStateOf(emptyList<MessageSession>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var next by remember { mutableStateOf<Long?>(null) }
    var retryMore by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<MessageSession?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var unread by remember { mutableStateOf(emptyMap<String, Int>()) }
    var generation by remember { mutableIntStateOf(0) }
    suspend fun load(more: Boolean = false) {
        if (!loggedIn || loading || busy) return
        val cursor = if (more) next ?: return else 0L
        retryMore = more
        loading = true
        error = null
        val version = ++generation
        try {
            val page = api.messageSessions(false, cursor, onEnriched = { enriched ->
                scope.launch {
                    if (generation == version) {
                        val names = enriched.associateBy { it.id }
                        sessions = sessions.map { current -> names[current.id]?.let {
                            current.copy(name = it.name, avatar = it.avatar)
                        } ?: current }
                    }
                }
            })
            sessions = ((if (more) sessions else emptyList()) + page.items).distinctBy { it.id }
                .sortedByDescending { it.pinned }
            next = page.next
            runCatching { api.messageFeedUnread() }.onSuccess { unread = it }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "消息加载失败" }
        finally { loading = false }
    }
    MessageResumeEffect { load() }
    LaunchedEffect(loggedIn) { if (loggedIn) load() else { sessions = emptyList(); unread = emptyMap() } }
    var consumedReadAll by remember { mutableIntStateOf(MessagePageBus.readAllRequest) }
    LaunchedEffect(MessagePageBus.readAllRequest) {
        if (consumedReadAll == MessagePageBus.readAllRequest) return@LaunchedEffect
        consumedReadAll = MessagePageBus.readAllRequest
        if (!loggedIn || busy || loading) return@LaunchedEffect
        busy = true
        generation++
        scope.launch {
        try {
            val success = api.clearAllUnread(sessions)
            context.showToast(if (success) "已全部标为已读" else "部分消息尚未标为已读，请重试")
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { context.showToast(e.message ?: "标记已读失败") }
        finally { busy = false }
        load()
        }
    }
    var consumedMore by remember { mutableIntStateOf(MessagePageBus.moreRequest) }
    LaunchedEffect(MessagePageBus.moreRequest) {
        if (consumedMore != MessagePageBus.moreRequest) {
            consumedMore = MessagePageBus.moreRequest
            showMore = true
        }
    }
    fun mutate(action: suspend (MessageSession) -> MessageSession?) {
        val target = selected ?: return
        if (busy || loading) return
        busy = true
        generation++ // Ignore stale background name enrichment after a mutation.
        scope.launch {
            try {
                val replacement = action(target)
                sessions = sessions.mapNotNull { if (it.id == target.id) replacement else it }
                    .sortedByDescending { it.pinned }
                selected = null
                confirmDelete = false
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { context.showToast(e.message ?: "操作失败") }
            finally { busy = false }
        }
    }
    BackHandler(onBack = onBack)
    TrackTeakPageScroll(listState)
    val visibleSessions = sessions.filter { tabIndex == 0 || (it.followed && !it.isSystem) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp + rememberStatusBarTop(),
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
                        .clip(com.aritxonly.teak.common.chrome.TeakContinuousCornerShape(22.dp))
                        .background(colors.surface)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    MessageShortcut(
                        label = "回复与@",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenReplies,
                        unread = (unread["reply"] ?: 0) + (unread["at"] ?: 0),
                    ) { androidx.compose.material3.Icon(MiuixIcons.Messages, null, Modifier.size(ShortcutGlyphSize), tint = MessageGreen) }
                    MessageShortcut(
                        label = "收到喜欢",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLikes,
                        unread = unread["like"] ?: 0,
                    ) { ResourceGlyph(res = TeakIcon.Like, tint = MessagePink, size = ShortcutGlyphSize) }
                    MessageShortcut(
                        label = "新增粉丝",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenFans,
                    ) { androidx.compose.material3.Icon(MiuixIcons.Contacts, null, Modifier.size(ShortcutGlyphSize), tint = MessageBlue) }
                    MessageShortcut(label = "系统通知", modifier = Modifier.weight(1f),
                        unread = unread["sys_msg"] ?: 0,
                        onClick = { com.aritxonly.bdfun.TeakNavigator.openMessageFeed(context, "system") }) {
                        androidx.compose.material3.Icon(MiuixIcons.Messages, null, Modifier.size(ShortcutGlyphSize), tint = colors.primary)
                    }
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
                itemsIndexed(visibleSessions, key = { _, session -> session.id }) { index, session ->
                    MessageRowSurface(index == 0, index == visibleSessions.lastIndex) {
                        SessionRow(session, onClick = {
                            if (session.talkerId > 0L) com.aritxonly.bdfun.TeakNavigator.openPrivateChat(context, session)
                        }, onLongClick = { selected = session; confirmDelete = false })
                    }
                }
                item(key = "footer") {
                    MessagePageFooter(loading, error, next != null, visibleSessions.isEmpty(),
                        onLoad = { scope.launch { load(more = if (error != null) retryMore else next != null && sessions.isNotEmpty()) } })
                }
            }
        }
    }
    TeakActionSheet(showMore, onDismiss = { showMore = false }, title = "消息") {
        ActionSheetRow("刷新消息", !loading && !busy) { showMore = false; scope.launch { load() } }
        ActionSheetRow("全部标为已读", !loading && !busy) { showMore = false; MessagePageBus.requestReadAll() }
    }
    TeakActionSheet(selected != null, onDismiss = { if (!busy) selected = null }, title = selected?.name) {
        val target = selected
        if (target != null) {
            val enabled = !busy && !loading
            if (confirmDelete) {
                Text("删除会话后，可通过对方的个人空间重新发起私信。", Modifier.padding(18.dp), color = colors.onBackgroundVariant)
                ActionSheetRow("确认删除会话", enabled, MiuixIcons.Delete) { mutate { api.removeMessageSession(it); null } }
                ActionSheetRow("取消", enabled) { confirmDelete = false }
            } else {
                ActionSheetRow(if (target.pinned) "取消置顶" else "置顶会话", enabled) {
                    mutate { api.pinMessageSession(it, !it.pinned); it.copy(pinned = !it.pinned) }
                }
                if (target.sessionType == 1) ActionSheetRow(if (target.muted) "关闭免打扰" else "消息免打扰", enabled) {
                    mutate { api.muteMessageSession(it, !it.muted); it.copy(muted = !it.muted) }
                }
                ActionSheetRow("标为已读", enabled && target.unread > 0) {
                    mutate { api.markSessionsRead(listOf(it)); it.copy(unread = 0) }
                }
                ActionSheetRow("删除会话", enabled, MiuixIcons.Delete) { confirmDelete = true }
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
    TeakTopGlassButton(onClick = { MessagePageBus.requestMore() }) {
        ThreeDotsGlyph()
    }
}

object MessagePageBus {
    var readAllRequest by mutableIntStateOf(0)
        private set

    var moreRequest by mutableIntStateOf(0)
        private set
    fun requestMore() { moreRequest++ }

    fun requestReadAll() {
        readAllRequest++
    }
}

@Composable
private fun MessageShortcut(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    unread: Int = 0,
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
            UnreadBadgeHost(unread) { glyph() }
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
private fun SessionRow(session: MessageSession, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onLongClick)
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
                text = (if (session.pinned) "[置顶] " else "") + (if (session.muted) "[免打扰] " else "") + session.preview.ifBlank { "暂无新消息" },
                color = colors.onBackgroundVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (session.unread > 0) {
            Spacer(Modifier.width(8.dp))
            Text(if (session.unread > 99) "99+" else session.unread.toString(),
                color = Color.White, fontSize = 11.sp,
                modifier = Modifier.background(MessageRed, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp))
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
    val nextByTab = remember(kind) { mutableStateMapOf<Int, MessageCursor?>() }
    val retryMoreByTab = remember(kind) { mutableStateMapOf<Int, Boolean>() }
    val fanPages = remember(kind) { mutableStateMapOf<Int, Int>() }
    val followedOverride = remember(kind) { mutableStateMapOf<Long, Boolean>() }
    val followBusy = remember(kind) { mutableStateMapOf<Long, Boolean>() }
    var selected by remember(kind) { mutableStateOf<Pair<Int, MessageItem>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val tabs = when (kind) {
        MessageFeedKind.Reply -> listOf("全部", "我关注的人", "@我")
        MessageFeedKind.Like -> listOf("全部", "我关注的人")
        else -> emptyList()
    }
    val title = when (kind) {
        MessageFeedKind.Reply -> "回复与@"
        MessageFeedKind.Like -> "收到喜欢"
        MessageFeedKind.Fans -> "新增粉丝"
        MessageFeedKind.System -> "系统通知"
    }
    fun feedType(page: Int) = when (kind) {
        MessageFeedKind.Reply -> if (page == 2) 1 else 0
        MessageFeedKind.Like -> 2
        else -> 3
    }
    suspend fun loadPage(page: Int, more: Boolean = false) {
        val key = if (page == 1) 0 else page
        if (loadingByTab[key] == true || busy) return
        if (more && nextByTab[key] == null) return
        retryMoreByTab[key] = more
        loadingByTab[key] = true
        errorByTab[key] = null
        try {
            val pageResult = if (kind == MessageFeedKind.Fans) {
                val pn = if (more) (fanPages[key] ?: 1) + 1 else 1
                val items = api.newFollowers(pn)
                fanPages[key] = pn
                com.aritxonly.bdfun.data.MessageFeedPage(items, if (items.size >= 20) MessageCursor(pn.toLong()) else null)
            } else api.messagesPage(feedType(key), if (more) nextByTab[key] else null)
            itemsByTab[key] = ((if (more) itemsByTab[key].orEmpty() else emptyList()) + pageResult.items).distinctBy { it.id }
            nextByTab[key] = pageResult.next
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { errorByTab[key] = e.message ?: "消息加载失败" }
        finally { loadingByTab[key] = false }
    }
    LaunchedEffect(kind) { loadPage(0) }
    LaunchedEffect(tab) { val key = if (tab == 1) 0 else tab; if (!itemsByTab.containsKey(key)) loadPage(key) }
    MessageResumeEffect { loadPage(tab) }

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
            val id = Regex("(?:dynamic|opus)/([0-9]+)").find(item.nativeUri)?.groupValues?.get(1)
                ?: android.net.Uri.parse(item.nativeUri).getQueryParameter("dynamic_id")
            if (!id.isNullOrBlank()) scope.launch {
                try {
                    val dynamic = api.dynamicDetail(id) ?: error("动态已删除或暂不可用")
                    com.aritxonly.bdfun.TeakNavigator.openDynamic(context, dynamic)
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { context.showToast(e.message ?: "动态加载失败") }
            } else context.showToast("这条消息未提供可打开的动态链接")
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
            val result = runCatching { check(api.followUser(mid, target)) { "关注操作失败" } }
            followBusy[mid] = false
            if (result.isSuccess) {
                context.showToast(if (target) "已回关" else "已取消关注")
            } else {
                followedOverride[mid] = !target
                context.showToast(result.exceptionOrNull()?.message ?: "操作失败")
            }
        }
    }

    val key = if (tab == 1) 0 else tab
    val allItems = itemsByTab[key].orEmpty()
    val pageItems = if (tab == 1) allItems.filter { it.followed } else allItems
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp + rememberStatusBarTop(), bottom = 40.dp)) {
        item(key = "header") { TeakLargeTitle(title = title) }
        item(key = "tabs") {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (tabs.isNotEmpty()) TeakPagerTabs(options = tabs, selected = tab, onSelect = { tab = it },
                    itemWidth = if (tabs.size > 2) 76.dp else 96.dp, backdrop = null, plain = true, position = tabProgress)
                Spacer(Modifier.weight(1f))
                Text("刷新", color = colors.primary, fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = loadingByTab[key] != true && !busy) { scope.launch { loadPage(tab) } }.padding(12.dp))
            }
        }
        itemsIndexed(pageItems, key = { _, item -> "message_${item.id}" }) { index, item ->
            val menu = { selected = key to item; confirmDelete = false }
            MessageRowSurface(index == 0, index == pageItems.lastIndex) {
                when (kind) {
                    MessageFeedKind.Reply -> ReplyRow(item, myName, onClick = { openMessageItem(item) }, onMore = menu)
                    MessageFeedKind.Like -> LikeRow(item, onClick = { openMessageItem(item) }, onMore = menu)
                    MessageFeedKind.Fans -> FanRow(item, followedOverride[item.userMid] ?: item.followed,
                        followBusy[item.userMid] == true, onFollow = { toggleFollow(item) })
                    MessageFeedKind.System -> SystemNotificationRow(item, onMore = menu)
                }
            }
        }
        item(key = "footer") {
            MessagePageFooter(loadingByTab[key] == true, errorByTab[key], nextByTab[key] != null, pageItems.isEmpty(),
                onLoad = { scope.launch { loadPage(tab, more = if (errorByTab[key] != null) retryMoreByTab[key] == true else nextByTab[key] != null && itemsByTab.containsKey(key)) } })
        }
    }
    fun mutate(action: suspend (MessageItem) -> MessageItem?) {
        val (page, item) = selected ?: return
        if (busy || loadingByTab[page] == true) return
        busy = true
        scope.launch {
            try {
                val replacement = action(item)
                itemsByTab[page] = itemsByTab[page].orEmpty().mapNotNull { if (it.id == item.id) replacement else it }
                selected = null
                confirmDelete = false
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { context.showToast(e.message ?: "操作失败") }
            finally { busy = false }
        }
    }
    TeakActionSheet(selected != null, onDismiss = { if (!busy) selected = null }, title = "消息操作") {
        val target = selected
        if (target != null) {
            val enabled = !busy && loadingByTab[target.first] != true
            if (confirmDelete) {
                ActionSheetRow("确认删除这条通知", enabled, MiuixIcons.Delete) {
                    mutate { if (kind == MessageFeedKind.System) api.deleteSystemMessage(it.id)
                        else api.deleteFeedMessage(feedType(target.first), it.id); null }
                }
                ActionSheetRow("取消", enabled) { confirmDelete = false }
            } else {
                if (kind == MessageFeedKind.Like) ActionSheetRow(if (target.second.noticeState == 0) "不再通知" else "接收通知", enabled) {
                    mutate { api.setLikeNotice(it.id, it.noticeState != 0); it.copy(noticeState = if (it.noticeState == 0) 1 else 0) }
                }
                ActionSheetRow("删除通知", enabled, MiuixIcons.Delete) { confirmDelete = true }
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
    onMore: () -> Unit = {},
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onMore)
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
                SmallMessageAction(TeakIcon.CommentBubble, "回复", onClick)
                Spacer(Modifier.width(12.dp))
                SmallMessageAction(TeakIcon.LikeOutline, "查看评论", onClick)
                Spacer(Modifier.weight(1f))
                MessageMoreDots(onMore)
            }
        }
        if (item.videoSubject || item.subjectText.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            MessageSubject(item)
        }
    }
}

@Composable
private fun LikeRow(item: MessageItem, onClick: () -> Unit = {}, onMore: () -> Unit = {}) {
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
            .combinedClickable(interactionSource = null, indication = null, onClick = onClick, onLongClick = onMore)
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
                MessageMoreDots(onMore)
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
private fun SmallMessageAction(icon: Int, label: String, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
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
