package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.TeakGlassPanel
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import top.yukonga.miuix.kmp.icon.extended.More

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.CommentItem
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 评论区统一字号：固定为原来的「110%」档，不再提供调节。 */
private const val COMMENT_FONT_SCALE = 1.1f

/** 进程内记住点过赞的评论，退出重进评论区仍然保持粉色和数量。 */
internal object CommentLikeMemory {
    private const val MAX_ENTRIES = 300

    val likedMap = androidx.compose.runtime.mutableStateMapOf<Long, Boolean>()
    val countMap = androidx.compose.runtime.mutableStateMapOf<Long, Int>()

    fun isLiked(item: CommentItem): Boolean = likedMap[item.rpid] ?: item.liked
    fun likeCount(item: CommentItem): Int = countMap[item.rpid] ?: item.like

    /** 记住某条评论的点赞状态，并限制总量，避免长期浏览评论时无限增长。 */
    fun put(rpid: Long, liked: Boolean, count: Int) {
        likedMap[rpid] = liked
        countMap[rpid] = count
        while (likedMap.size > MAX_ENTRIES) {
            val oldest = likedMap.keys.firstOrNull() ?: break
            likedMap.remove(oldest)
            countMap.remove(oldest)
        }
    }
}

@Composable
fun CommentScreen(
    api: BiliApi,
    aid: Long,
    upMid: Long,
    title: String,
    onBack: () -> Unit,
    bvid: String = "",
    bare: Boolean = false,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
    /** 动态评论传 type=17 + id_str；视频保持 type=1 + aid。 */
    commentOid: String = aid.toString(),
    commentType: Int = 1,
    /** 动态详情等页面把大标题 / 动态内容塞在评论列表最前面。 */
    header: (androidx.compose.foundation.lazy.LazyListScope.() -> Unit)? = null,
    bottomPadding: androidx.compose.ui.unit.Dp = 96.dp,
    /** 页面自己用 verticalScroll 时传 false；列表页传 true 以驱动返回键 / 折叠标题。 */
    trackBackProgress: Boolean = false,
    /** 动态详情等页面要求加载时不出现任何加载图标。 */
    showLoading: Boolean = true,
    /** 从消息页进来时要定位的评论：根评论 / 具体回复（0 表示不定位）。 */
    focusRootRpid: Long = 0L,
    focusTargetRpid: Long = 0L,
    /** 视频页播放器当前底边；直接传值，展开回复页才能和播放器同一帧移动。 */
    threadPlayerBottom: androidx.compose.ui.unit.Dp? = null,
    threadPaneWidth: androidx.compose.ui.unit.Dp? = null,
    playerFullscreen: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val statusTop = rememberStatusBarTop()
    // 动态详情没有播放器：展开评论的面板顶边最多停在返回键下面，不要顶到状态栏。
    val threadMinTopPx = if (threadPaneWidth != null && threadPlayerBottom != null) {
        with(density) { threadPlayerBottom.toPx() }
    } else if (bare) {
        with(density) { (statusTop + 56.dp).toPx() }
    } else {
        0f
    }

    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var end by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sortHot by remember { mutableStateOf(true) }
    var consumedFocusKey by remember { mutableStateOf("") }
    var highlightRpid by remember { mutableStateOf(0L) }
    // 回复面板（点「共 N 条回复」打开的大面板）。
    var replySheetTarget by remember { mutableStateOf<CommentItem?>(null) }
    var replySheetVisible by remember { mutableStateOf(false) }
    var replySheetItems by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var replySheetLoading by remember { mutableStateOf(false) }
    var replySheetEnd by remember { mutableStateOf(true) }
    var replySheetPage by remember { mutableIntStateOf(0) }
    var replySheetSortHot by remember { mutableStateOf(true) }
    var replySheetFocusRpid by remember { mutableStateOf(0L) }
    val disliked = remember { mutableStateMapOf<Long, Boolean>() }
    var menuComment by remember { mutableStateOf<CommentItem?>(null) }
    var menuVisible by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    // Landscape comments remain interactive; only exclusive video fullscreen hides their overlays.
    LaunchedEffect(playerFullscreen) {
        if (playerFullscreen) {
            replySheetVisible = false
            menuVisible = false
        }
    }
    // 长按菜单打开时让底栏回复输入框先渐隐，菜单退出后再渐显
    LaunchedEffect(menuVisible) {
        if (menuVisible) {
            VideoPageBus.commentMenuOpen = true
        } else {
            delay(320)
            VideoPageBus.commentMenuOpen = false
        }
    }
    DisposableEffect(Unit) {
        onDispose { VideoPageBus.commentMenuOpen = false }
    }
    val listState = rememberLazyListState()
    val myMid = AppGraph.session.value?.mid ?: 0L
    if (trackBackProgress) TrackTeakPageScroll(listState)

    if (!bare) BackHandler(onBack = onBack)

    // 进评论区就预加载表情包，评论里的 [doge] 才能直接显示成图片）
    LaunchedEffect(Unit) { EmoteCache.ensure(api) }

    suspend fun fetch(next: Int): Boolean {
        val result = runCatching {
            api.commentsByOid(
                oid = commentOid,
                type = commentType,
                page = next,
                upMid = upMid,
                sort = if (sortHot) 2 else 0,
            )
        }.getOrElse {
            error = it.message ?: "评论加载失败"
            return false
        }
        comments = if (next == 1) result.items else (comments + result.items).distinctBy { it.rpid }
        total = if (result.total > 0) result.total else comments.size
        page = next
        end = result.isEnd || result.items.isEmpty()
        return true
    }

    suspend fun reload() {
        loading = true
        error = null
        end = false
        fetch(1)
        loading = false
    }

    suspend fun loadMore() {
        if (loadingMore || loading || end) return
        loadingMore = true
        error = null
        fetch(page + 1)
        loadingMore = false
    }

    LaunchedEffect(commentOid, commentType, sortHot) {
        comments = emptyList()
        total = 0
        reload()
    }

    LaunchedEffect(listState, comments.size, end, sortHot) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, count) ->
            if (comments.isNotEmpty() && !end && !loading && !loadingMore && last >= count - 1) {
                loadMore()
            }
        }
    }

    DisposableEffect(commentOid, commentType) {
        if (commentType == 1) VideoPageBus.aid = aid
        VideoPageBus.commentSender = { text, pictures ->
            val result = runCatching {
                api.addCommentByOid(
                    oid = commentOid,
                    type = commentType,
                    message = text,
                    root = ReplyTarget.root,
                    parent = ReplyTarget.parent,
                    pictures = pictures,
                )
            }.getOrNull()
            if (result == null) {
                false
            } else {
                AppGraph.sentHistory.record("comment", text, bvid, title)
                ReplyTarget.clear()
                reload()
                val index = if (result > 0) {
                    comments.indexOfFirst { it.rpid == result }
                } else {
                    -1
                }
                // 跳到自己刚发的评论（列表第 0 项是头部的「共 N 条评论」）
                runCatching {
                    if (index >= 0) listState.animateScrollToItem(index + 1)
                    else listState.animateScrollToItem(0)
                }
                context.showToast("回复成功")
                true
            }
        }
        onDispose {
            if (VideoPageBus.commentSender != null) VideoPageBus.commentSender = null
        }
    }

    fun startReply(target: CommentItem) {
        val root = if (target.root > 0) target.root else target.rpid
        ReplyTarget.set(target.uname, root, target.rpid)
        VideoPageBus.requestReplyFocus++
    }

    fun setLiked(target: CommentItem, liked: Boolean, likeCount: Int) {
        CommentLikeMemory.put(target.rpid, liked, likeCount)
        val index = comments.indexOfFirst { it.rpid == target.rpid }
        if (index < 0) return
        comments = comments.toMutableList().also {
            it[index] = it[index].copy(liked = liked, like = likeCount)
        }
    }

    fun toggleLike(target: CommentItem) {
        val current = CommentLikeMemory.isLiked(target)
        val next = !current
        val oldCount = CommentLikeMemory.likeCount(target)
        val newCount = (oldCount + if (next) 1 else -1).coerceAtLeast(0)
        setLiked(target, next, newCount)
        scope.launch {
            val ok = runCatching {
                api.likeCommentByOid(commentOid, commentType, target.rpid, next)
            }.isSuccess
            if (!ok) {
                setLiked(target, !next, oldCount)
                context.showToast("点赞失败")
            } else {
                context.showToast(if (next) "点赞成功" else "已取消点赞")
            }
        }
    }

    /** 未展开的折叠评论点赞：同时更新父评论里的 previewReplies。 */
    fun togglePreviewLike(parent: CommentItem, reply: CommentItem) {
        val current = CommentLikeMemory.isLiked(reply)
        val next = !current
        val oldCount = CommentLikeMemory.likeCount(reply)
        val newCount = (oldCount + if (next) 1 else -1).coerceAtLeast(0)
        fun apply(liked: Boolean, count: Int) {
            CommentLikeMemory.put(reply.rpid, liked, count)
            comments = comments.map { c ->
                if (c.rpid != parent.rpid) {
                    c
                } else {
                    c.copy(
                        previewReplies = c.previewReplies.map { r ->
                            if (r.rpid == reply.rpid) r.copy(liked = liked, like = count) else r
                        },
                    )
                }
            }
        }
        apply(next, newCount)
        scope.launch {
            val ok = runCatching {
                api.likeCommentByOid(commentOid, commentType, reply.rpid, next)
            }.isSuccess
            if (!ok) {
                apply(!next, oldCount)
                context.showToast("点赞失败")
            } else {
                context.showToast(if (next) "点赞成功" else "已取消点赞")
            }
        }
    }

    fun loadReplySheetPage(target: CommentItem, next: Int) {
        if (replySheetLoading) return
        replySheetLoading = true
        scope.launch {
            val result = runCatching {
                api.repliesByOid(
                    oid = commentOid,
                    type = commentType,
                    root = target.rpid,
                    page = next,
                    upMid = upMid,
                    sortTime = !replySheetSortHot,
                )
            }.getOrNull()
            replySheetItems = if (next <= 1) {
                result?.items.orEmpty()
            } else {
                (replySheetItems + result?.items.orEmpty()).distinctBy { it.rpid }
            }
            replySheetPage = next
            replySheetEnd = result == null || result.isEnd || result.items.isEmpty()
            replySheetLoading = false
        }
    }

    fun openReplySheet(target: CommentItem, focusRpid: Long = 0L) {
        replySheetTarget = target
        replySheetItems = emptyList()
        replySheetPage = 0
        replySheetEnd = false
        replySheetSortHot = true
        replySheetFocusRpid = focusRpid
        replySheetVisible = true
        loadReplySheetPage(target, 1)
    }

    // 消息页点「回复 / 赞」进来：等评论加载好后滚到那条评论；
    // 如果它是楼中楼回复，就顺手展开回复面板。
    LaunchedEffect(comments.size, focusRootRpid, focusTargetRpid, end) {
        if (focusRootRpid <= 0L || comments.isEmpty()) return@LaunchedEffect
        val key = "$focusRootRpid:$focusTargetRpid"
        if (consumedFocusKey == key) return@LaunchedEffect
        val rootIndex = comments.indexOfFirst { it.rpid == focusRootRpid }
        if (rootIndex < 0) {
            // 根评论还没出现在当前页：继续拉，最多自动翻 5 页，避免无限请求。
            if (!end && !loadingMore && page < 5) loadMore()
            return@LaunchedEffect
        }
        consumedFocusKey = key
        highlightRpid = focusTargetRpid.takeIf { it > 0L } ?: focusRootRpid
        listState.animateScrollToItem(rootIndex + 1)
        launch {
            delay(90)
            highlightRpid = 0L
        }
        val target = focusTargetRpid
        if (target > 0L && target != focusRootRpid) {
            openReplySheet(comments[rootIndex])
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        if (!bare) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(40.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (total > 0) "评论 $total" else "评论",
                        color = colors.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = title,
                        color = colors.onBackgroundVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier,
                contentPadding = PaddingValues(
                    start = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 2.dp else 12.dp,
                    end = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 2.dp else 12.dp,
                    top = 4.dp + topInset,
                    bottom = bottomPadding,
                ),
            ) {
                header?.invoke(this)
                item(key = "comment-count") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "共 ${formatCount(total.toLong())} 条评论",
                            color = colors.onBackgroundVariant,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (sortHot) "按热度" else "按时间",
                            color = colors.onBackgroundVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .clip(Capsule())
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { sortHot = !sortHot }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                // 一整张卡片：每条评论只负责自己的圆角，中间无缝、用分割线隔开
                itemsIndexed(comments, key = { _, item -> item.rpid }) { index, item ->
                    val isFirst = index == 0
                    val isLast = index == comments.lastIndex && end
                    val shape = RoundedCornerShape(
                        topStart = if (isFirst) 16.dp else 0.dp,
                        topEnd = if (isFirst) 16.dp else 0.dp,
                        bottomStart = if (isLast) 16.dp else 0.dp,
                        bottomEnd = if (isLast) 16.dp else 0.dp,
                    )
                    val classicComment = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
                    val flashColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (item.rpid == highlightRpid) {
                            colors.onBackground.copy(alpha = 0.10f)
                        } else {
                            colors.surface
                        },
                        animationSpec = androidx.compose.animation.core.tween(90),
                        label = "comment-focus-flash",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (classicComment) {
                                    Modifier.drawBehind {
                                        val y = size.height - 1.dp.toPx() / 2f
                                        drawLine(
                                            color = colors.onBackground.copy(alpha = 0.10f),
                                            start = androidx.compose.ui.geometry.Offset(0f, y),
                                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                                            strokeWidth = 1.dp.toPx(),
                                        )
                                    }
                                } else {
                                    Modifier.clip(shape).background(flashColor)
                                },
                            ),
                    ) {
                        CommentRow(
                            // 不再在这里读全局点赞表，否则点一个赞会让整屏评论行重组。
                            // 每行自己在 CommentRow 里用 derivedStateOf 只订阅自己的值。
                            item = item,
                            disliked = disliked[item.rpid] == true,
                            onLike = { toggleLike(item) },
                            onDislike = { disliked[item.rpid] = disliked[item.rpid] != true },
                            onReply = { startReply(item) },
                            onLongPress = { bounds ->
                                menuAnchor = bounds
                                menuComment = item
                                menuVisible = true
                            },
                            onMore = { bounds ->
                                menuAnchor = bounds
                                menuComment = item
                                menuVisible = true
                            },
                            onOpenReplies = { openReplySheet(item) },
                            // 点折叠回复：不弹回复框，直接展开回复页并定位、闪灰。
                            onReplyToReply = { reply -> openReplySheet(item, reply.rpid) },
                            onLongPressReply = { reply, bounds ->
                                menuAnchor = bounds
                                menuComment = reply
                                menuVisible = true
                            },
                            onPreviewLike = { reply -> togglePreviewLike(item, reply) },
                        )
                    }
                }

                if (loadingMore && showLoading) {
                    item(key = "loading-more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            TeakLoading(size = 36.dp)
                        }
                    }
                }

                if (comments.isEmpty() && !loading && error == null) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "还没有评论",
                                color = colors.onBackgroundVariant,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                error?.let { message ->
                    item(key = "error") {
                        Text(
                            text = message,
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 4.dp),
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            if (loading && showLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().offset(y = topInset / 2),
                    contentAlignment = Alignment.Center,
                ) {
                    TeakLoading(size = 48.dp)
                }
            }
        }
    }

    val target = menuComment
    // 始终组合这个面板（和投币/收藏一致）：页面一进来 mounted 就是 true，
    // 长按打开时不会再晚一帧，弹出的弹簧和投币面板完全同一条曲线。
    TeakGlassMenu(
        visible = menuVisible && target != null,
        onDismiss = {
            menuVisible = false
            // 保留 target 让退出动画能完整播完，再清内容
            scope.launch {
                delay(360)
                if (!menuVisible) menuComment = null
            }
        },
        anchorInWindow = menuAnchor,
        minWidth = 168.dp,
        maxWidth = 216.dp,
        maxHeight = 420.dp,
    ) {
        if (target != null) {
            CommentMenuRow("回复", MenuGlyphKind.Reply, colors.onBackground) {
                menuVisible = false
                scope.launch {
                    delay(360)
                    menuComment = null
                    startReply(target)
                }
            }
            CommentMenuRow("复制", MenuGlyphKind.Copy, colors.onBackground) {
                menuVisible = false
                context.copyToClipboard("评论", target.message)
                context.showToast("已复制")
                scope.launch {
                    delay(360)
                    menuComment = null
                }
            }
            CommentMenuRow("自由复制", MenuGlyphKind.Copy, colors.onBackground) {
                menuVisible = false
                context.copyToClipboard("评论", "${target.uname}：${target.message}")
                context.showToast("已复制")
                scope.launch {
                    delay(360)
                    menuComment = null
                }
            }
            CommentMenuRow("分享", null, colors.onBackground) {
                menuVisible = false
                runCatching {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            android.content.Intent.EXTRA_TEXT,
                            "${target.uname}：${target.message}\nhttps://www.bilibili.com/video/${bvid}",
                        )
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(intent, "分享评论"),
                    )
                }
                scope.launch {
                    delay(360)
                    menuComment = null
                }
            }
            CommentMenuRow("举报", MenuGlyphKind.Report, Color(0xFFE5484D)) {
                menuVisible = false
                scope.launch {
                    val ok = runCatching {
                        api.reportCommentByOid(commentOid, commentType, target.rpid)
                    }.isSuccess
                    context.showToast(if (ok) "已举报" else "举报失败")
                    delay(360)
                    menuComment = null
                }
            }
            CommentMenuRow("加入黑名单", MenuGlyphKind.Block, Color(0xFFE5484D)) {
                menuVisible = false
                scope.launch {
                    val ok = if (target.mid <= 0) false
                    else runCatching { api.blockUser(target.mid) }.isSuccess
                    context.showToast(if (ok) "已加入黑名单" else "操作失败")
                    delay(360)
                    menuComment = null
                }
            }
            if (myMid > 0 && target.mid == myMid) {
                CommentMenuRow("删除", MenuGlyphKind.Delete, Color(0xFFE5484D), danger = true) {
                    menuVisible = false
                    scope.launch {
                        val ok = runCatching {
                            api.deleteCommentByOid(commentOid, commentType, target.rpid)
                        }.isSuccess
                        if (ok) {
                            comments = comments.filterNot { it.rpid == target.rpid }
                            total = (total - 1).coerceAtLeast(0)
                            context.showToast("已删除")
                        } else {
                            context.showToast("删除失败")
                        }
                        delay(360)
                        menuComment = null
                    }
                }
            }
        }
    }

    val sheetTarget = replySheetTarget
    if (sheetTarget != null && !playerFullscreen) {
        CommentThreadSheet(
            visible = replySheetVisible,
            comment = sheetTarget,
            replies = replySheetItems,
            loading = replySheetLoading,
            end = replySheetEnd,
            commentOid = commentOid,
            commentType = commentType,
            bvid = bvid,
            videoTitle = title,
            sortHot = replySheetSortHot,
            onToggleSort = {
                replySheetSortHot = !replySheetSortHot
                replySheetItems = emptyList()
                replySheetPage = 0
                replySheetEnd = false
                loadReplySheetPage(sheetTarget, 1)
            },
            onLoadMore = { loadReplySheetPage(sheetTarget, replySheetPage + 1) },
            onDismiss = {
                // 只把 visible 置否，先保留 target，让面板把退场弹簧跑完再被回收
                replySheetVisible = false
                replySheetFocusRpid = 0L
            },
            focusRpid = replySheetFocusRpid.takeIf { it > 0L }
                ?: if (consumedFocusKey == "$focusRootRpid:$focusTargetRpid") {
                    focusTargetRpid
                } else {
                    0L
                },
            minTopPx = threadMinTopPx,
            playerBottomDp = threadPlayerBottom,
            paneWidth = threadPaneWidth,
        )
    }

    CommentImageDialog()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommentRow(
    item: CommentItem,
    disliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onReply: () -> Unit,
    onLongPress: (androidx.compose.ui.geometry.Rect) -> Unit,
    onMore: (androidx.compose.ui.geometry.Rect) -> Unit,
    onOpenReplies: () -> Unit,
    onReplyToReply: (CommentItem) -> Unit,
    onLongPressReply: (CommentItem, androidx.compose.ui.geometry.Rect) -> Unit,
    onPreviewLike: (CommentItem) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scale = COMMENT_FONT_SCALE
    var rowBounds by remember {
        mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
    }
    // 只订阅这条评论自己的点赞状态；别的行被点赞时这里不会失效。
    val liked by remember(item.rpid, item.liked) {
        derivedStateOf { CommentLikeMemory.likedMap[item.rpid] ?: item.liked }
    }
    val like by remember(item.rpid, item.like) {
        derivedStateOf { CommentLikeMemory.countMap[item.rpid] ?: item.like }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                rowBounds = it.boundsInWindow()
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onReply,
                onLongClick = { onLongPress(rowBounds) },
            )
            .padding(
                horizontal = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 6.dp else 12.dp,
                vertical = 10.dp,
            ),
    ) {
        Row {
            AsyncImage(
                model = rememberImageRequest(item.avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh)
                    .clickable(interactionSource = null, indication = null) {
                        if (item.mid > 0L) {
                            com.aritxonly.bdfun.TeakNavigator.openSpace(context, item.mid)
                        }
                    },
            )
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.uname,
                        color = if (item.isVip) colors.primary else colors.onBackgroundVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                        ) {
                            if (item.mid > 0L) {
                                com.aritxonly.bdfun.TeakNavigator.openSpace(context, item.mid)
                            }
                        },
                    )
                    if (item.isUp) {
                        Spacer(Modifier.width(4.dp))
                        UpBadge()
                    }
                    Spacer(Modifier.width(4.dp))
                    Image(
                        painter = androidx.compose.ui.res.painterResource(
                            id = when (item.level.coerceIn(1, 6)) {
                                1 -> com.aritxonly.bdfun.R.drawable.bili_lv1
                                2 -> com.aritxonly.bdfun.R.drawable.bili_lv2
                                3 -> com.aritxonly.bdfun.R.drawable.bili_lv3
                                4 -> com.aritxonly.bdfun.R.drawable.bili_lv4
                                5 -> com.aritxonly.bdfun.R.drawable.bili_lv5
                                else -> com.aritxonly.bdfun.R.drawable.bili_lv6
                            },
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(width = 22.dp, height = 10.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                EmoteText(
                    text = item.message,
                    color = colors.onBackground,
                    fontSize = 14.85.sp,
                    lineHeight = 21.8.sp,
                    extraEmotes = item.emotes,
                    fontWeight = FontWeight.Normal,
                    // 正文里 @ 到的人，和折叠回复的人名同色，点名字进对方主页。
                    mentions = remember(item.mentions) {
                        item.mentions.associate { it.name to it.mid }
                    },
                    mentionColor = colors.primary.copy(alpha = 0.72f),
                    onMentionClick = { mid ->
                        if (mid > 0L) {
                            com.aritxonly.bdfun.TeakNavigator.openSpace(context, mid)
                        }
                    },
                )
                if (item.pictures.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    CommentPictures(item.pictures) { index ->
                        CommentImageViewer.open(item.pictures, index)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDate(item.ctime),
                        color = colors.onBackgroundVariant,
                        fontSize = 11.sp,
                    )
                    if (item.ipLocation.isNotBlank() && AppGraph.settings.showIpLocation) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.ipLocation,
                            color = colors.onBackgroundVariant,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "回复",
                        color = colors.onBackgroundVariant,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(Capsule())
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = onReply,
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    // 点赞：整体略上移
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .offset(y = (-1).dp)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = onLike,
                            ),
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(
                                if (liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
                            ),
                            contentDescription = "点赞",
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                if (liked) BrandPink else colors.onBackgroundVariant,
                            ),
                            modifier = Modifier.size(16.dp),
                        )
                        if (like > 0) {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = formatCount(like.toLong()),
                                color = if (liked) BrandPink else colors.onBackgroundVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.offset(y = 1.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    // 点踩：整体略下移
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .offset(y = 1.5.dp)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = onDislike,
                            ),
                    ) {
                        Image(
                            painter = androidx.compose.ui.res.painterResource(
                                if (disliked) TeakIcon.CommentDislikeFilled else TeakIcon.CommentDislike,
                            ),
                            contentDescription = "点踩",
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                if (disliked) BrandPink else colors.onBackgroundVariant,
                            ),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    // 省略号：直接使用 Miuix 的 More 图标，尺寸和返回键箭头一致。
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = { onMore(rowBounds) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        top.yukonga.miuix.kmp.basic.Icon(
                            imageVector = top.yukonga.miuix.kmp.icon.MiuixIcons.More,
                            contentDescription = "更多",
                            tint = colors.onBackgroundVariant,
                            modifier = Modifier.size(17.5.dp),
                        )
                    }
                }

                ReplyBlock(
                    item = item,
                    onOpen = onOpenReplies,
                    onReplyToReply = onReplyToReply,
                    onLongPressReply = onLongPressReply,
                    onPreviewLike = onPreviewLike,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentPictures(urls: List<String>, onOpen: (Int) -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        urls.take(3).forEachIndexed { index, url ->
            AsyncImage(
                model = rememberImageRequest(url),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceContainerHigh)
                    .clickable(interactionSource = null, indication = null) { onOpen(index) },
            )
        }
        if (urls.size > 3) {
            Text(
                text = "+${urls.size - 3}",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
    }
}

private object CommentImageViewer {
    var images by mutableStateOf<List<String>>(emptyList())
    var index by mutableIntStateOf(0)

    fun open(list: List<String>, at: Int) {
        images = list
        index = at.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    }

    fun close() {
        images = emptyList()
        index = 0
    }
}

@Composable
private fun CommentImageDialog() {
    val images = CommentImageViewer.images
    if (images.isEmpty()) return
    val pager = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = CommentImageViewer.index.coerceIn(0, images.lastIndex),
    ) { images.size }
    androidx.activity.compose.BackHandler { CommentImageViewer.close() }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { CommentImageViewer.close() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                var scale by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableFloatStateOf(1f)
                }
                var pan by androidx.compose.runtime.remember {
                    androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
                }
                AsyncImage(
                    model = rememberImageRequest(images[page]),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var moved = false
                                var last = down.position
                                var maxPressed = 1
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val pressedCount = event.changes.count { it.pressed }
                                    maxPressed = maxOf(maxPressed, pressedCount)
                                    val change = event.changes.firstOrNull()
                                    if (change != null) {
                                        if ((change.position - last).getDistance() > 12f) moved = true
                                        last = change.position
                                    }
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    if (pressedCount >= 2) {
                                        // 双指缩放/拖动：始终由图片处理。
                                        scale = (scale * zoomChange).coerceIn(1f, 4f)
                                        pan = if (scale > 1f) pan + panChange else
                                            androidx.compose.ui.geometry.Offset.Zero
                                        event.changes.forEach { it.consume() }
                                    } else if (scale > 1.01f) {
                                        // 放大后单指移动图片。
                                        pan += panChange
                                        event.changes.forEach { it.consume() }
                                    } else {
                                        // 原始比例：单指横向拖动不消费，交给外层 Pager 翻页。
                                        pan = androidx.compose.ui.geometry.Offset.Zero
                                    }
                                    if (event.changes.none { it.pressed }) break
                                }
                                // 没放大时点一下空白：关闭大图。
                                if (maxPressed <= 1 && !moved && scale <= 1.01f) {
                                    CommentImageViewer.close()
                                }
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        },
                )
            }
            Text(
                text = "${pager.currentPage + 1}/${images.size}",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
            )
        }
    }
}

@Composable
private fun CommentMenuRow(
    label: String,
    glyph: MenuGlyphKind?,
    tint: Color,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    // 直接用设置页二级菜单同一套 Miuix 玻璃行：同样的高度、内边距、按压反馈和文字排版。
    top.yukonga.miuix.kmp.glass.GlassPopupItem(
        text = label,
        onClick = onClick,
        contentColor = tint,
        selected = false,
    )
}

@Composable
private fun ReplyBlock(
    item: CommentItem,
    onOpen: () -> Unit,
    onReplyToReply: (CommentItem) -> Unit,
    onLongPressReply: (CommentItem, androidx.compose.ui.geometry.Rect) -> Unit,
    onPreviewLike: (CommentItem) -> Unit,
) {
    if (item.replyCount <= 0) return
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scale = COMMENT_FONT_SCALE
    val preview = item.previewReplies.take(2)

    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        // 更淡、更接近页面背景的灰
                        .background(colors.onBackground.copy(alpha = 0.045f))
                },
            )
            .padding(
                horizontal = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 2.dp else 10.dp,
                vertical = 8.dp,
            ),
    ) {
        preview.forEachIndexed { replyIndex, reply ->
            var replyBounds by remember(reply.rpid) {
                mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { replyBounds = it.boundsInWindow() }
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { onReplyToReply(reply) },
                        onLongClick = { onLongPressReply(reply, replyBounds) },
                    )
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top,
            ) {
                // 同样只订阅这条折叠回复自己的值。
                val liked by remember(reply.rpid, reply.liked) {
                    derivedStateOf { CommentLikeMemory.likedMap[reply.rpid] ?: reply.liked }
                }
                val likeCount by remember(reply.rpid, reply.like) {
                    derivedStateOf { CommentLikeMemory.countMap[reply.rpid] ?: reply.like }
                }
                EmoteText(
                    text = reply.message,
                    prefix = reply.uname,
                    color = colors.onBackground,
                    // 折叠回复：普通用户用淡一点的主题色；大会员用完整主题色。
                    prefixColor = if (reply.isVip) {
                        colors.primary
                    } else {
                        colors.primary.copy(alpha = 0.72f)
                    },
                    prefixBadge = if (reply.isUp) {
                        { UpBadge() }
                    } else {
                        null
                    },
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f),
                    extraEmotes = reply.emotes,
                    fontWeight = FontWeight.Normal,
                    mentions = remember(reply.mentions) {
                        reply.mentions.associate { it.name to it.mid }
                    },
                    mentionColor = if (reply.isVip) {
                        colors.primary
                    } else {
                        colors.primary.copy(alpha = 0.72f)
                    },
                    onMentionClick = { mid ->
                        if (mid > 0L) {
                            com.aritxonly.bdfun.TeakNavigator.openSpace(context, mid)
                        }
                    },
                    trailingWidth = if (likeCount > 0) 56.sp else 26.sp,
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                ) { onPreviewLike(reply) },
                        ) {
                            Spacer(Modifier.width(2.dp))
                            Image(
                                painter = androidx.compose.ui.res.painterResource(
                                    if (liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
                                ),
                                contentDescription = "点赞",
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                    if (liked) BrandPink else colors.onBackgroundVariant,
                                ),
                                modifier = Modifier.size(12.dp),
                            )
                            if (likeCount > 0) {
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = formatCount(likeCount.toLong()),
                                    color = if (liked) BrandPink else colors.onBackgroundVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    modifier = Modifier.offset(y = (-2).dp),
                                )
                            }
                        }
                    },
                )
            }
            // 折叠回复之间多留 2dp，避免两条挤在一起。
            if (replyIndex != preview.lastIndex) {
                Spacer(Modifier.height(2.dp))
            }
        }

        // 点这里打开大面板看全部回复
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(interactionSource = null, indication = null, onClick = onOpen)
                .padding(top = 5.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "共${item.replyCount}条回复",
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.width(2.dp))
            Text(text = "＞", color = colors.onBackgroundVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(text = text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Normal)
    }
}
