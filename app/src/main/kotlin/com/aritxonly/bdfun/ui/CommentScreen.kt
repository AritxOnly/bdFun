package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.TeakGlassPanel
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
private object CommentLikeMemory {
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
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var end by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sortHot by remember { mutableStateOf(true) }
    // 回复面板（点「共 N 条回复」打开的大面板）。
    var replySheetTarget by remember { mutableStateOf<CommentItem?>(null) }
    var replySheetVisible by remember { mutableStateOf(false) }
    var replySheetItems by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var replySheetLoading by remember { mutableStateOf(false) }
    var replySheetEnd by remember { mutableStateOf(true) }
    var replySheetPage by remember { mutableIntStateOf(0) }
    val disliked = remember { mutableStateMapOf<Long, Boolean>() }
    var menuComment by remember { mutableStateOf<CommentItem?>(null) }
    var menuVisible by remember { mutableStateOf(false) }
    // 横屏（全屏）时把回复面板、长按菜单提前收回，不在横屏里出现
    val landscapeNow = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(landscapeNow) {
        if (landscapeNow) {
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
                api.repliesByOid(commentOid, commentType, target.rpid, next, upMid)
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

    fun openReplySheet(target: CommentItem) {
        replySheetTarget = target
        replySheetItems = emptyList()
        replySheetPage = 0
        replySheetEnd = false
        replySheetVisible = true
        loadReplySheetPage(target, 1)
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
                                    Modifier.clip(shape).background(colors.surface)
                                },
                            ),
                    ) {
                        CommentRow(
                            item = item.copy(
                                liked = CommentLikeMemory.isLiked(item),
                                like = CommentLikeMemory.likeCount(item),
                            ),
                            disliked = disliked[item.rpid] == true,
                            onLike = { toggleLike(item) },
                            onDislike = { disliked[item.rpid] = disliked[item.rpid] != true },
                            onReply = { startReply(item) },
                            onLongPress = {
                                menuComment = item
                                menuVisible = true
                            },
                            onMore = {
                                menuComment = item
                                menuVisible = true
                            },
                            onOpenReplies = { openReplySheet(item) },
                            onReplyToReply = { reply -> startReply(reply) },
                            onLongPressReply = { reply ->
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
    TeakActionSheet(
        visible = menuVisible && target != null,
        onDismiss = {
            menuVisible = false
            // 保留 target 让退出动画能完整播完，再清内容
            scope.launch {
                delay(360)
                if (!menuVisible) menuComment = null
            }
        },
        title = target?.uname,
    ) {
        if (target != null) {
            SheetActionRow("回复", MenuGlyphKind.Reply, colors.onBackground) {
                menuVisible = false
                scope.launch {
                    delay(360)
                    menuComment = null
                    startReply(target)
                }
            }
            SheetActionRow("复制", MenuGlyphKind.Copy, colors.onBackground) {
                menuVisible = false
                context.copyToClipboard("评论", target.message)
                context.showToast("已复制")
                scope.launch {
                    delay(360)
                    menuComment = null
                }
            }
            SheetActionRow("举报", MenuGlyphKind.Report, colors.onBackground) {
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
            SheetActionRow("加入黑名单", MenuGlyphKind.Block, colors.onBackground) {
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
                SheetActionRow("删除", MenuGlyphKind.Delete, Color(0xFFE5484D), danger = true) {
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
    if (sheetTarget != null) {
        ReplySheet(
            visible = replySheetVisible,
            comment = sheetTarget,
            replies = replySheetItems,
            loading = replySheetLoading,
            end = replySheetEnd,
            commentOid = commentOid,
            commentType = commentType,
            api = api,
            bvid = bvid,
            videoTitle = title,
            onLoadMore = { loadReplySheetPage(sheetTarget, replySheetPage + 1) },
            onDismiss = {
                // 只把 visible 置否，先保留 target，让面板把退场弹簧跑完再被回收
                replySheetVisible = false
            },
        )
    }

    CommentImageDialog()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CommentRow(
    item: CommentItem,
    disliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onReply: () -> Unit,
    onLongPress: () -> Unit,
    onMore: () -> Unit,
    onOpenReplies: () -> Unit,
    onReplyToReply: (CommentItem) -> Unit,
    onLongPressReply: (CommentItem) -> Unit,
    onPreviewLike: (CommentItem) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scale = COMMENT_FONT_SCALE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onReply,
                onLongClick = onLongPress,
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
                                if (item.liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
                            ),
                            contentDescription = "点赞",
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                if (item.liked) BrandPink else colors.onBackgroundVariant,
                            ),
                            modifier = Modifier.size(16.dp),
                        )
                        if (item.like > 0) {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = formatCount(item.like.toLong()),
                                color = if (item.liked) BrandPink else colors.onBackgroundVariant,
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
                    // 竖向三点省略号（圆点）：与长按评论打开同一个面板
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = onMore,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.size(width = 10.dp, height = 15.dp),
                        ) {
                            val radius = 1.36.dp.toPx()
                            val cx = size.width / 2f
                            listOf(0.16f, 0.5f, 0.84f).forEach { ratio ->
                                drawCircle(
                                    color = colors.onBackgroundVariant,
                                    radius = radius,
                                    center = androidx.compose.ui.geometry.Offset(
                                        x = cx,
                                        y = size.height * ratio,
                                    ),
                                )
                            }
                        }
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
                            detectTransformGestures { _, delta, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                pan = if (scale > 1f) {
                                    pan + delta
                                } else {
                                    androidx.compose.ui.geometry.Offset.Zero
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
private fun ReplyBlock(
    item: CommentItem,
    onOpen: () -> Unit,
    onReplyToReply: (CommentItem) -> Unit,
    onLongPressReply: (CommentItem) -> Unit,
    onPreviewLike: (CommentItem) -> Unit,
) {
    if (item.replyCount <= 0) return
    val colors = MiuixTheme.colorScheme
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { onReplyToReply(reply) },
                        onLongClick = { onLongPressReply(reply) },
                    )
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.Top,
            ) {
                val liked = CommentLikeMemory.isLiked(reply)
                val likeCount = CommentLikeMemory.likeCount(reply)
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

@OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
private fun ReplySheet(
    visible: Boolean,
    comment: CommentItem,
    replies: List<CommentItem>,
    loading: Boolean,
    end: Boolean,
    commentOid: String,
    commentType: Int,
    api: BiliApi,
    bvid: String,
    videoTitle: String,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Dialog 里的 navigationBarsPadding 偶发读到 0，用系统资源兜底；键盘弹出时交给 imePadding
    val navBottom = rememberNavigationBarBottom()
    val imeVisible = WindowInsets.isImeVisible
    // 与投币面板完全同一套：首次组合先以 0 为起点，否则会直接瞬移到终点（没有动画）
    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }
    // 打开后的头 260ms 不响应“点外面关闭”，避免打开面板的那次抬手把它立刻关掉
    var dismissArmed by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            dismissArmed = false
            kotlinx.coroutines.delay(260)
            dismissArmed = true
        } else {
            dismissArmed = false
        }
    }
    val p = rememberSpringFloat(
        target = if (visible && mounted) 1f else 0f,
        stiffness = 420f,
        damping = 28f,
    )
    if (!visible && p <= 0.005f) return

    val listState = rememberLazyListState()
    var replyTarget by remember { mutableStateOf<CommentItem?>(null) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var extra by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    val likedIds = remember { mutableStateMapOf<Long, Boolean>() }
    val dislikedIds = remember { mutableStateMapOf<Long, Boolean>() }
    var deletedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var menuReply by remember { mutableStateOf<CommentItem?>(null) }
    var menuVisible by remember { mutableStateOf(false) }
    val myMid = AppGraph.session.value?.mid ?: 0L
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val allReplies = (extra + replies)
        .distinctBy { it.rpid }
        .filterNot { it.rpid in deletedIds }

    LaunchedEffect(listState, allReplies.size, end, loading) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, count) ->
            if (!end && !loading && allReplies.isNotEmpty() && last >= count - 1) {
                onLoadMore()
            }
        }
    }

    // 点某条回复-> 输入框在面板内渐显
    LaunchedEffect(replyTarget) {
        if (replyTarget != null) {
            kotlinx.coroutines.delay(60)
            runCatching { focusRequester.requestFocus() }
        }
    }

    fun send() {
        val target = replyTarget ?: return
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true
        scope.launch {
            val result = runCatching {
                api.addCommentByOid(
                    oid = commentOid,
                    type = commentType,
                    message = text,
                    root = comment.rpid,
                    parent = target.rpid,
                )
            }.getOrNull()
            sending = false
            if (result != null) {
                AppGraph.sentHistory.record("comment", text, bvid, videoTitle)
                val me = AppGraph.session.value
                extra = listOf(
                    CommentItem(
                        rpid = if (result > 0) result else System.currentTimeMillis(),
                        uname = me?.name ?: "我",
                        avatar = me?.face.orEmpty(),
                        message = text,
                        like = 0,
                        ctime = System.currentTimeMillis() / 1000,
                        level = me?.level ?: 0,
                        replyCount = 0,
                        isUp = false,
                        isTop = false,
                    ),
                ) + extra
                draft = ""
                replyTarget = null
                keyboard?.hide()
                context.showToast("回复成功")
            } else {
                context.showToast("发送失败，请确认已登录")
            }
        }
    }

    fun toggleLike(item: CommentItem) {
        val current = likedIds[item.rpid] ?: CommentLikeMemory.isLiked(item)
        val next = !current
        val oldCount = CommentLikeMemory.likeCount(item)
        val newCount = (oldCount + if (next) 1 else -1).coerceAtLeast(0)
        likedIds[item.rpid] = next
        CommentLikeMemory.put(item.rpid, next, newCount)
        scope.launch {
            val ok = runCatching {
                api.likeCommentByOid(commentOid, commentType, item.rpid, next)
            }.isSuccess
            if (!ok) {
                likedIds[item.rpid] = !next
                CommentLikeMemory.put(item.rpid, !next, oldCount)
                context.showToast("点赞失败")
            } else {
                context.showToast(if (next) "点赞成功" else "已取消点赞")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            // 关闭只走自己的点击层，避免打开面板的那次抬手把它立刻关掉
            dismissOnClickOutside = false,
        ),
    ) {
        // 关掉 Dialog 自带的背景压暗
        val view = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect {
            dialogWindow?.setDimAmount(0f)
            // 让导航栏保持透明，面板铺到底时小白条仍是沉浸的
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                dialogWindow?.isNavigationBarContrastEnforced = false
            }
            @Suppress("DEPRECATION")
            dialogWindow?.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 不压暗背景，只做点击层
                    .background(Color.Transparent)
                    .clickable(interactionSource = null, indication = null) {
                        if (dismissArmed) {
                            // 回复框打开时，点空白只收起回复框，不退出整个折叠评论页
                            if (replyTarget != null) {
                                replyTarget = null
                            } else {
                                onDismiss()
                            }
                        }
                    },
            )
            TeakGlassPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // 高度收在“视频与内容交界”以下，不再顶到播放器上
                    .fillMaxHeight(0.66f)
                    .graphicsLayer {
                        val progress = p.coerceAtLeast(0f)
                        // 和投币面板一致：渐显 + 从底部整块滑上来，退出沿同一条弹簧回去
                        alpha = progress.coerceIn(0f, 1f)
                        translationY = (1f - progress) * size.height
                    }
                    .dropShadow(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        shadow = Shadow(
                            radius = 8.dp,
                            color = Color.Black,
                            alpha = 0.10f,
                        ),
                    ),
                // 贴住屏幕底：只圆上面两角，下面不会被看成“被切了一刀”
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                // 和设置二级菜单/投币面板同一套底灰
                tintAlpha = 0.3f,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // 导航栏内边距放在内容里，面板本身铺到底，导航栏保持沉浸；
                        // 键盘弹出时只留 imePadding，避免底部多出一段空白
                        .padding(bottom = if (imeVisible) 0.dp else navBottom)
                        .imePadding(),
                ) {
                    Text(
                        text = "共 ${comment.replyCount + extra.size} 条回复",
                        color = colors.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    // 原评论
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        AsyncImage(
                            model = rememberImageRequest(comment.avatar),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceContainerHigh)
                                .clickable(interactionSource = null, indication = null) {
                                    if (comment.mid > 0L) {
                                        com.aritxonly.bdfun.TeakNavigator.openSpace(
                                            context,
                                            comment.mid,
                                        )
                                    }
                                },
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = comment.uname,
                                    color = if (comment.isVip) colors.primary else colors.onBackgroundVariant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier.clickable(
                                        interactionSource = null,
                                        indication = null,
                                    ) {
                                        if (comment.mid > 0L) {
                                            com.aritxonly.bdfun.TeakNavigator.openSpace(
                                                context,
                                                comment.mid,
                                            )
                                        }
                                    },
                                )
                                if (comment.isUp) {
                                    Spacer(Modifier.width(4.dp))
                                    UpBadge()
                                }
                                Spacer(Modifier.width(4.dp))
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(
                                        id = when (comment.level.coerceIn(1, 6)) {
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
                            Spacer(Modifier.height(3.dp))
                            EmoteText(
                                text = comment.message,
                                color = colors.onBackground,
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                extraEmotes = comment.emotes,
                                fontWeight = FontWeight.Normal,
                            )
                        }
                    }
                    // 层主与第一条回复拉开距离，并加一条分割线
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(0.8.dp)
                            .background(colors.onBackground.copy(alpha = 0.10f)),
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                    ) {
                        itemsIndexed(allReplies, key = { _, r -> r.rpid }) { index, reply ->
                            val liked = likedIds[reply.rpid]
                                ?: CommentLikeMemory.isLiked(reply)
                            val likeCount = CommentLikeMemory.likeCount(reply)
                            val disliked = dislikedIds[reply.rpid] == true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        interactionSource = null,
                                        indication = null,
                                        onClick = { replyTarget = reply },
                                        onLongClick = {
                                            menuReply = reply
                                            menuVisible = true
                                        },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                            ) {
                                AsyncImage(
                                    model = rememberImageRequest(reply.avatar),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceContainerHigh)
                                        .clickable(interactionSource = null, indication = null) {
                                            if (reply.mid > 0L) {
                                                com.aritxonly.bdfun.TeakNavigator.openSpace(
                                                    context,
                                                    reply.mid,
                                                )
                                            }
                                        },
                                )
                                Spacer(Modifier.width(9.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = reply.uname,
                                            color = if (reply.isVip) colors.primary else colors.onBackgroundVariant,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.clickable(
                                                interactionSource = null,
                                                indication = null,
                                            ) {
                                                if (reply.mid > 0L) {
                                                    com.aritxonly.bdfun.TeakNavigator.openSpace(
                                                        context,
                                                        reply.mid,
                                                    )
                                                }
                                            },
                                        )
                                        if (reply.isUp) {
                                            Spacer(Modifier.width(4.dp))
                                            UpBadge()
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Image(
                                            painter = androidx.compose.ui.res.painterResource(
                                                id = when (reply.level.coerceIn(1, 6)) {
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
                                    Spacer(Modifier.height(3.dp))
                                    EmoteText(
                                        text = reply.message,
                                        color = colors.onBackground,
                                        fontSize = 14.85.sp,
                                        lineHeight = 21.8.sp,
                                        extraEmotes = reply.emotes,
                                        fontWeight = FontWeight.Normal,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    // 与评论页完全相同的操作行：时间 / 回复 / 赞 / 踩 / ⋮
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = formatDate(reply.ctime),
                                            color = colors.onBackgroundVariant,
                                            fontSize = 11.sp,
                                        )
                                        if (reply.ipLocation.isNotBlank() && AppGraph.settings.showIpLocation) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = reply.ipLocation,
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
                                                ) { replyTarget = reply }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Spacer(Modifier.width(10.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .offset(y = (-1).dp)
                                                .clickable(
                                                    interactionSource = null,
                                                    indication = null,
                                                ) { toggleLike(reply) },
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
                                            if (likeCount > 0) {
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = formatCount(likeCount.toLong()),
                                                    color = if (liked) BrandPink else colors.onBackgroundVariant,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.offset(y = 1.dp),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .offset(y = 1.5.dp)
                                                .clickable(
                                                    interactionSource = null,
                                                    indication = null,
                                                ) {
                                                    dislikedIds[reply.rpid] =
                                                        dislikedIds[reply.rpid] != true
                                                },
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
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .clickable(
                                                    interactionSource = null,
                                                    indication = null,
                                                ) {
                                                    menuReply = reply
                                                    menuVisible = true
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            androidx.compose.foundation.Canvas(
                                                modifier = Modifier.size(width = 10.dp, height = 15.dp),
                                            ) {
                                                val radius = 1.36.dp.toPx()
                                                val cx = size.width / 2f
                                                listOf(0.16f, 0.5f, 0.84f).forEach { r ->
                                                    drawCircle(
                                                        color = colors.onBackgroundVariant,
                                                        radius = radius,
                                                        center = androidx.compose.ui.geometry.Offset(
                                                            x = cx,
                                                            y = size.height * r,
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (loading) {
                            item(key = "sheet-loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 36.dp, bottom = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TeakLoading(size = 34.dp)
                                }
                            }
                        }
                    }

                    // 面板内的回复输入框：点某条回复后渐显
                    val target = replyTarget
                    if (target != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TeakGlassSurface(
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = Capsule(),
                                recipe = TeakGlassRecipes.floatingNavigation(
                                    LocalTeakMaterialSpec.current.fineTuning,
                                ),
                                backdrop = LocalTeakBackdrop.current,
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 16.dp, end = 44.dp),
                                        contentAlignment = Alignment.CenterStart,
                                    ) {
                                        BasicTextField(
                                            value = draft,
                                            onValueChange = { draft = it },
                                            singleLine = true,
                                            textStyle = ComposerTextStyle.copy(
                                                color = colors.onBackground,
                                            ),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(
                                                BrandPink,
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester),
                                            decorationBox = { inner ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (draft.isEmpty()) {
                                                        Text(
                                                            text = "回复 @${target.uname}：",
                                                            color = colors.outline,
                                                            fontSize = 13.sp,
                                                        )
                                                    }
                                                    inner()
                                                }
                                            },
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 8.dp),
                                    ) {
                                        ComposerIconButton(
                                            kind = ComposerIcon.Send,
                                            tint = if (draft.isNotBlank()) {
                                                BrandPink
                                            } else {
                                                colors.outline
                                            },
                                            onClick = { send() },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 展开页里的长按菜单：与评论页同一个面板
    val menuTarget = menuReply
    TeakActionSheet(
        visible = menuVisible && menuTarget != null,
        onDismiss = {
            menuVisible = false
            scope.launch {
                delay(360)
                if (!menuVisible) menuReply = null
            }
        },
        title = menuTarget?.uname,
    ) {
        if (menuTarget != null) {
            SheetActionRow("回复", MenuGlyphKind.Reply, colors.onBackground) {
                menuVisible = false
                scope.launch {
                    delay(360)
                    menuReply = null
                    replyTarget = menuTarget
                }
            }
            SheetActionRow("复制", MenuGlyphKind.Copy, colors.onBackground) {
                menuVisible = false
                context.copyToClipboard("评论", menuTarget.message)
                context.showToast("已复制")
                scope.launch {
                    delay(360)
                    menuReply = null
                }
            }
            SheetActionRow("举报", MenuGlyphKind.Report, colors.onBackground) {
                menuVisible = false
                scope.launch {
                    val ok = runCatching {
                        api.reportCommentByOid(commentOid, commentType, menuTarget.rpid)
                    }.isSuccess
                    context.showToast(if (ok) "已举报" else "举报失败")
                    delay(360)
                    menuReply = null
                }
            }
            SheetActionRow("加入黑名单", MenuGlyphKind.Block, colors.onBackground) {
                menuVisible = false
                scope.launch {
                    val ok = if (menuTarget.mid <= 0) {
                        false
                    } else {
                        runCatching { api.blockUser(menuTarget.mid) }.isSuccess
                    }
                    context.showToast(if (ok) "已加入黑名单" else "操作失败")
                    delay(360)
                    menuReply = null
                }
            }
            if (myMid > 0 && menuTarget.mid == myMid) {
                SheetActionRow("删除", MenuGlyphKind.Delete, Color(0xFFE5484D), danger = true) {
                    menuVisible = false
                    scope.launch {
                        val ok = runCatching {
                        api.deleteCommentByOid(commentOid, commentType, menuTarget.rpid)
                    }.isSuccess
                        if (ok) {
                            deletedIds = deletedIds + menuTarget.rpid
                            context.showToast("已删除")
                        } else {
                            context.showToast("删除失败")
                        }
                        delay(360)
                        menuReply = null
                    }
                }
            }
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
