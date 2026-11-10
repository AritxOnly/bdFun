package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.CommentItem
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 评论区「展开回复」页（BiliPai 式从视频底部整块拉起的矩形页）。
 *
 * - 顶部严丝合缝贴着播放器当前的下沿：播放器被评论顶上去多少，这一页就跟着上移多少；
 * - 不透明的 App 背景色、无圆角，位于窗口最高层（Dialog）；
 * - 里面复刻主评论区：层主评论一张白卡，下面「共 N 条评论 / 按热度·时间」，再往下是所有回复；
 * - 层主评论跟着列表一起滚；
 * - 顶部、底部各有一条和视频页一样的背景色渐变；
 * - 弹出/收起用减速补间，到位即停，没有任何回弹；
 * - 页内列表保留系统 overscroll（拖动到边缘的弹性效果）。
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun CommentThreadSheet(
    visible: Boolean,
    comment: CommentItem,
    replies: List<CommentItem>,
    loading: Boolean,
    end: Boolean,
    commentOid: String,
    commentType: Int,
    bvid: String,
    videoTitle: String,
    sortHot: Boolean,
    onToggleSort: () -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    /** 从消息页进来时要直接滚到的回复 rpid。 */
    focusRpid: Long = 0L,
    /** 面板顶边至少停在这里（px）。动态详情页用它保证不盖住返回键。 */
    minTopPx: Float = 0f,
    /** 视频页播放器当前底边；直接传值可避免总线状态晚一帧导致的上下不贴合。 */
    playerBottomDp: androidx.compose.ui.unit.Dp? = null,
    paneWidth: androidx.compose.ui.unit.Dp? = null,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navBottom = rememberNavigationBarBottom()
    val imeVisible = WindowInsets.isImeVisible

    var mounted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { mounted = true }
    // 这一页打开期间告诉播放器：即使视频是暂停的，也要把画面往上收。
    DisposableEffect(visible) {
        if (visible) VideoPageBus.commentThreadVisible = true
        onDispose { VideoPageBus.commentThreadVisible = false }
    }
    // 不用弹簧：普通减速补间，拉到位置就停，不做回弹。
    val progress = rememberSmoothFloat(
        target = if (visible && mounted) 1f else 0f,
        durationMs = 220f,
        easing = EaseOutQuint,
    )
    if (!visible && progress <= 0.005f) return

    // 和主评论区一样：往上滑先收播放器，收满之后列表才开始滚；往下滑到顶再放回播放器。
    val playerCollapseConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y >= 0f) return androidx.compose.ui.geometry.Offset.Zero
                val sink = VideoPageBus.playerCollapseScroll
                    ?: return androidx.compose.ui.geometry.Offset.Zero
                return androidx.compose.ui.geometry.Offset(0f, sink(available.y))
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y <= 0f) return androidx.compose.ui.geometry.Offset.Zero
                val sink = VideoPageBus.playerCollapseScroll
                    ?: return androidx.compose.ui.geometry.Offset.Zero
                return androidx.compose.ui.geometry.Offset(0f, sink(available.y))
            }
        }
    }

    val listState = rememberLazyListState()
    var replyTarget by remember { mutableStateOf<CommentItem?>(null) }
    var draft by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var extra by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var flashRpid by remember { mutableStateOf(0L) }
    val likedIds = remember { mutableStateMapOf<Long, Boolean>() }
    val dislikedIds = remember { mutableStateMapOf<Long, Boolean>() }
    var deletedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var menuReply by remember { mutableStateOf<CommentItem?>(null) }
    var menuVisible by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    val myMid = AppGraph.session.value?.mid ?: 0L
    val focusRequester = remember { FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    fun exitReply() {
        replyTarget = null
        draft = ""
        focusManager.clearFocus()
        keyboard?.hide()
    }
    val allReplies = (extra + replies)
        .distinctBy { it.rpid }
        .filterNot { it.rpid in deletedIds }

    // 返回键：先退出回复框；没有回复框时才关闭整个展开页。
    androidx.activity.compose.BackHandler {
        if (replyTarget != null) {
            exitReply()
        } else {
            onDismiss()
        }
    }

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

    // 消息页定位到楼中楼：回复列表里找到这条就直接滚过去。
    var consumedFocusRpid by remember { mutableStateOf(0L) }
    LaunchedEffect(focusRpid, allReplies.size, loading, end) {
        if (focusRpid <= 0L || consumedFocusRpid == focusRpid) return@LaunchedEffect
        val index = allReplies.indexOfFirst { it.rpid == focusRpid }
        if (index >= 0) {
            consumedFocusRpid = focusRpid
            // 前面有「原评论」和「共 N 条评论」两个 item。
            listState.animateScrollToItem(index + 2)
            flashRpid = focusRpid
            delay(700)
            if (flashRpid == focusRpid) flashRpid = 0L
        } else if (!end && !loading) {
            onLoadMore()
        }
    }

    LaunchedEffect(replyTarget) {
        if (replyTarget != null) {
            delay(60)
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
                AppGraph.api.addCommentByOid(
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
                        mid = me?.mid ?: 0L,
                        root = comment.rpid,
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
                AppGraph.api.likeCommentByOid(commentOid, commentType, item.rpid, next)
            }.isSuccess
            if (!ok) {
                likedIds[item.rpid] = !next
                CommentLikeMemory.put(item.rpid, !next, oldCount)
                context.showToast("点赞失败")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        val background = MaterialTheme.colorScheme.background
        val density = LocalDensity.current
        // 播放器块当前的下沿（px）→ 这一页的顶边。播放器被顶上去多少，这里就跟着上移多少。
        // 动态详情没有播放器，用 minTopPx 把面板顶边压在返回键下面，不让它顶到屏幕最上面。
        val panelTopPx = maxOf(
            // 和播放器底边重叠 2dp，抵消窗口位置更新比播放器慢一帧的问题，
            // 避免往上顶时在接缝处露出主评论区。
            (playerBottomDp?.let { with(density) { it.toPx() } }
                ?: VideoPageBus.playerBottomPx).coerceAtLeast(0f) -
                with(density) { 2.dp.toPx() },
            minTopPx,
        )
        val screenHeightPx = with(density) {
            androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
        }
        androidx.compose.runtime.SideEffect {
            dialogWindow?.setDimAmount(0f)
            // 只保留内容自己的「非线性上移」，关掉弹窗窗口自带的缩放/淡入动画。
            dialogWindow?.setWindowAnimations(0)
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                dialogWindow?.isNavigationBarContrastEnforced = false
            }
            @Suppress("DEPRECATION")
            dialogWindow?.navigationBarColor = android.graphics.Color.TRANSPARENT
            // 关键：弹窗窗口只从播放器下沿开始。窗口不覆盖播放器，点视频才能正常呼出控件；
            // 同时只留返回键能关闭这一页，点别处不再触发 onDismiss。
            dialogWindow?.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            )
        }
        // 窗口位置在组合提交阶段直接设置，和播放器同一帧更新；
        // 只在数值真的变化时才重写窗口参数，避免列表滚动时每帧都改窗口。
        val panelTopInt = panelTopPx.toInt()
        val paneWidthPx = paneWidth?.let { with(density) { it.roundToPx() } }
            ?: android.view.ViewGroup.LayoutParams.MATCH_PARENT
        val paneEndPx = if (paneWidth != null) WindowInsets.safeDrawing.getRight(
            density, androidx.compose.ui.unit.LayoutDirection.Ltr,
        ) else 0
        val panelHeightPx = (screenHeightPx - panelTopPx).toInt().coerceAtLeast(1)
        val lastPanelGeometry = remember { intArrayOf(Int.MIN_VALUE, 0, 0, 0) }
        androidx.compose.runtime.SideEffect {
            val geometry = intArrayOf(panelTopInt, paneWidthPx, panelHeightPx, paneEndPx)
            if (lastPanelGeometry.contentEquals(geometry)) return@SideEffect
            geometry.copyInto(lastPanelGeometry)
            val window = dialogWindow ?: return@SideEffect
            val lp = window.attributes ?: return@SideEffect
            lp.gravity = android.view.Gravity.TOP or android.view.Gravity.RIGHT
            lp.width = paneWidthPx
            lp.height = panelHeightPx
            lp.x = paneEndPx
            lp.y = panelTopInt
            window.attributes = lp
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize()
                    .graphicsLayer {
                        // 从下往上拉起，到位即停，没有弹簧回弹。
                        val t = progress.coerceIn(0f, 1f)
                        translationY = (1f - t) * size.height
                    }
                    .background(background),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    val headerHeight = 46.dp
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(playerCollapseConnection),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = headerHeight + 8.dp,
                            bottom = if (replyTarget != null) 78.dp else 16.dp,
                        ),
                    ) {
                        item(key = "thread-op") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.surface),
                            ) {
                                CommentRow(
                                    item = comment.copy(
                                        replyCount = 0,
                                        previewReplies = emptyList(),
                                        liked = CommentLikeMemory.isLiked(comment),
                                        like = CommentLikeMemory.likeCount(comment),
                                    ),
                                    disliked = dislikedIds[comment.rpid] == true,
                                    onLike = { toggleLike(comment) },
                                    onDislike = {
                                        dislikedIds[comment.rpid] =
                                            dislikedIds[comment.rpid] != true
                                    },
                                    onReply = { replyTarget = comment },
                                    onLongPress = { bounds ->
                                        menuAnchor = bounds
                                        menuReply = comment
                                        menuVisible = true
                                    },
                                    onMore = { bounds ->
                                        menuAnchor = bounds
                                        menuReply = comment
                                        menuVisible = true
                                    },
                                    onOpenReplies = {},
                                    onReplyToReply = { replyTarget = it },
                                    onLongPressReply = { reply, bounds ->
                                        menuAnchor = bounds
                                        menuReply = reply
                                        menuVisible = true
                                    },
                                    onPreviewLike = { toggleLike(it) },
                                )
                            }
                        }

                        item(key = "thread-head") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "共 ${comment.replyCount + extra.size} 条评论",
                                    color = colors.onBackgroundVariant,
                                    fontSize = 11.sp,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = if (sortHot) "按热度" else "按时间",
                                    color = colors.onBackgroundVariant,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clip(Capsule())
                                        .clickable(
                                            interactionSource = null,
                                            indication = null,
                                            onClick = onToggleSort,
                                        )
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }

                        itemsIndexed(allReplies, key = { _, r -> r.rpid }) { index, reply ->
                            val isFirst = index == 0
                            val isLast = index == allReplies.lastIndex && end
                            val shape = RoundedCornerShape(
                                topStart = if (isFirst) 16.dp else 0.dp,
                                topEnd = if (isFirst) 16.dp else 0.dp,
                                bottomStart = if (isLast) 16.dp else 0.dp,
                                bottomEnd = if (isLast) 16.dp else 0.dp,
                            )
                            val rowBackground by androidx.compose.animation.animateColorAsState(
                                targetValue = if (reply.rpid == flashRpid) {
                                    colors.onBackground.copy(alpha = 0.10f)
                                } else {
                                    colors.surface
                                },
                                animationSpec = androidx.compose.animation.core.tween(120),
                                label = "thread-reply-flash",
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .background(rowBackground),
                            ) {
                                CommentRow(
                                    item = reply.copy(
                                        liked = likedIds[reply.rpid]
                                            ?: CommentLikeMemory.isLiked(reply),
                                        like = CommentLikeMemory.likeCount(reply),
                                    ),
                                    disliked = dislikedIds[reply.rpid] == true,
                                    onLike = { toggleLike(reply) },
                                    onDislike = {
                                        dislikedIds[reply.rpid] =
                                            dislikedIds[reply.rpid] != true
                                    },
                                    onReply = { replyTarget = reply },
                                    onLongPress = { bounds ->
                                        menuAnchor = bounds
                                        menuReply = reply
                                        menuVisible = true
                                    },
                                    onMore = { bounds ->
                                        menuAnchor = bounds
                                        menuReply = reply
                                        menuVisible = true
                                    },
                                    onOpenReplies = {},
                                    onReplyToReply = { replyTarget = it },
                                    onLongPressReply = { reply, bounds ->
                                        menuAnchor = bounds
                                        menuReply = reply
                                        menuVisible = true
                                    },
                                    onPreviewLike = { toggleLike(it) },
                                )
                            }
                        }

                        if (loading) {
                            item(key = "thread-loading") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TeakLoading(size = 34.dp)
                                }
                            }
                        }
                    }

                    // 顶部渐变从「评论详情」标题的垂直中线开始，往下渐隐到透明；
                    // 标题固定在最上面，画在渐变之上，不会被内容盖住。
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(headerHeight)
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to background,
                                        0.5f to background,
                                        1f to background.copy(alpha = 0f),
                                    ),
                                ),
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(headerHeight)
                            .padding(start = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = "评论详情",
                            color = colors.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(54.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(background.copy(alpha = 0f), background),
                                ),
                            ),
                    )
                    // 回复框打开时，点空白区域只退出回复框，不关闭展开页。
                    if (replyTarget != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    onClick = { exitReply() },
                                ),
                        )
                    }
                }
            }

        // 面板内回复输入框：点某条回复后出现。
        // 它是浮在内容上的一层，不参与 Column 排版，所以底部的渐变不会被一起顶上去。
        val target = replyTarget
        if (target != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = if (imeVisible) 8.dp else 8.dp + navBottom,
                    )
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                        TeakGlassSurface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
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
                                        cursorBrush = SolidColor(BrandPink),
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

    // 展开页里的长按菜单：与设置页二级菜单 / 评论页完全同款。
    val menuTarget = menuReply
    fun dismissMenu(after: (() -> Unit)? = null) {
        menuVisible = false
        scope.launch {
            delay(360)
            menuReply = null
            after?.invoke()
        }
    }
    TeakGlassMenu(
        visible = menuVisible && menuTarget != null,
        onDismiss = { dismissMenu() },
        anchorInWindow = menuAnchor,
        minWidth = 168.dp,
        maxWidth = 216.dp,
        maxHeight = 420.dp,
    ) {
        if (menuTarget != null) {
            top.yukonga.miuix.kmp.glass.GlassPopupItem(
                text = "回复",
                contentColor = colors.onBackground,
                onClick = { dismissMenu { replyTarget = menuTarget } },
            )
            top.yukonga.miuix.kmp.glass.GlassPopupItem(
                text = "复制",
                contentColor = colors.onBackground,
                onClick = {
                    context.copyToClipboard("评论", menuTarget.message)
                    context.showToast("已复制")
                    dismissMenu()
                },
            )
            top.yukonga.miuix.kmp.glass.GlassPopupItem(
                text = "自由复制",
                contentColor = colors.onBackground,
                onClick = {
                    context.copyToClipboard("评论", "${menuTarget.uname}：${menuTarget.message}")
                    context.showToast("已复制")
                    dismissMenu()
                },
            )
            top.yukonga.miuix.kmp.glass.GlassPopupItem(
                text = "分享",
                contentColor = colors.onBackground,
                onClick = {
                    runCatching {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                            .apply {
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "${menuTarget.uname}：${menuTarget.message}\n" +
                                        "https://www.bilibili.com/video/$bvid",
                                )
                            }
                        context.startActivity(
                            android.content.Intent.createChooser(intent, "分享评论"),
                        )
                    }
                    dismissMenu()
                },
            )
            top.yukonga.miuix.kmp.glass.GlassPopupItem(
                text = "举报",
                contentColor = Color(0xFFE5484D),
                onClick = {
                    menuVisible = false
                    scope.launch {
                        val ok = runCatching {
                            AppGraph.api.reportCommentByOid(commentOid, commentType, menuTarget.rpid)
                        }.isSuccess
                        context.showToast(if (ok) "已举报" else "举报失败")
                        delay(360)
                        menuReply = null
                    }
                },
            )
            top.yukonga.miuix.kmp.glass.GlassPopupItem(
                text = "加入黑名单",
                contentColor = Color(0xFFE5484D),
                onClick = {
                    menuVisible = false
                    scope.launch {
                        val ok = if (menuTarget.mid <= 0) {
                            false
                        } else {
                            runCatching { AppGraph.api.blockUser(menuTarget.mid) }.isSuccess
                        }
                        context.showToast(if (ok) "已加入黑名单" else "操作失败")
                        delay(360)
                        menuReply = null
                    }
                },
            )
            if (myMid > 0 && menuTarget.mid == myMid) {
                top.yukonga.miuix.kmp.glass.GlassPopupItem(
                    text = "删除",
                    contentColor = Color(0xFFE5484D),
                    onClick = {
                        menuVisible = false
                        scope.launch {
                            val ok = runCatching {
                                AppGraph.api.deleteCommentByOid(
                                    commentOid,
                                    commentType,
                                    menuTarget.rpid,
                                )
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
                    },
                )
            }
        }
    }
}
