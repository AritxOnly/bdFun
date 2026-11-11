package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import com.aritxonly.teak.common.chrome.centeredListWidth
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.BrandPinkDeep
import com.aritxonly.bdfun.data.DynamicItem
import com.aritxonly.bdfun.data.VideoItem
import com.kyant.shapes.Capsule
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import com.aritxonly.bdfun.data.DynamicFeedContract
import com.aritxonly.bdfun.data.DynamicLiveRecommendation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.VolumeOff
import top.yukonga.miuix.kmp.icon.extended.VolumeUp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private val DynamicPullThreshold = 80.dp
private val DynamicPullMax = 240.dp

/** 固定顶栏高度（不含状态栏）：动态分类玻璃分段栏。 */
val DynamicTopBarHeight: Dp = 56.dp

@Composable
fun DynamicScreen(
    state: DynamicFeedState,
    loggedIn: Boolean,
    onNeedLogin: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenDynamic: (DynamicItem) -> Unit,
    refreshTick: Int = 0,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = with(density) { DynamicPullThreshold.toPx() }
    val maxPullPx = with(density) { DynamicPullMax.toPx() }

    var pull by remember(state) { mutableFloatStateOf(0f) }
    // 同步给 App / 指示器：位移 + 进度
    LaunchedEffect(pull) {
        state.pullOffsetDp = with(density) { pull.toDp().value }
        state.pullFraction = (pull / thresholdPx).coerceIn(0f, 1f)
    }
    // 表情数据和评论区共用同一份进程缓存，动态正文里的 [doge] 才会变成图片
    LaunchedEffect(Unit) { EmoteCache.ensure(AppGraph.api) }

    suspend fun animatePullTo(
        to: Float,
        durationMs: Float = 220f,
        easing: androidx.compose.animation.core.Easing =
            androidx.compose.animation.core.FastOutSlowInEasing,
    ) {
        val from = pull
        if (from == to) return
        if (durationMs <= 0f) {
            pull = to
            return
        }
        val start = System.nanoTime()
        while (true) {
            val p = ((System.nanoTime() - start) / 1_000_000f / durationMs)
                .coerceIn(0f, 1f)
            pull = from + (to - from) * easing.transform(p)
            if (p >= 1f) break
            delay(8)
        }
        pull = to
    }

    var handledTick by remember { mutableIntStateOf(refreshTick) }
    LaunchedEffect(state, loggedIn, refreshTick) {
        if (refreshTick == handledTick) {
            state.ensureLoaded(loggedIn)
            return@LaunchedEffect
        }
        handledTick = refreshTick
        if (state.refreshing || state.loading) return@LaunchedEffect
        // 和首页再次点击底栏完全同一套：先回顶（不在顶部时和下拉动画并行），
        // 再把下拉指示器拉到阈值，最后刷新。
        val atTop = state.listState.firstVisibleItemIndex == 0 &&
            state.listState.firstVisibleItemScrollOffset == 0
        if (atTop) {
            animatePullTo(thresholdPx, 200f)
        } else {
            kotlinx.coroutines.coroutineScope {
                launch { state.listState.animateScrollToItem(0) }
                launch { animatePullTo(thresholdPx, 260f) }
            }
        }
        state.pullFraction = 1f
        state.refresh(loggedIn)
    }

    LaunchedEffect(state.refreshing) {
        // 和首页一致：刷新结束后先让小电视收掉，再 260ms 平滑回落
        if (!state.refreshing && pull > 0f) {
            state.pullFraction = 0f
            delay(160)
            animatePullTo(0f, 260f)
        }
    }

    val statusTop = rememberStatusBarTop()

    val connection = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                if (available.y < 0f && pull > 0f) {
                    val next = (pull + available.y).coerceAtLeast(0f)
                    val consumed = next - pull
                    pull = next
                    return Offset(0f, consumed)
                }
                if (available.y > 0f && !state.listState.canScrollBackward) {
                    val next = (pull + available.y * 0.6f).coerceAtMost(maxPullPx)
                    val consumed = next - pull
                    pull = next
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity,
            ): androidx.compose.ui.unit.Velocity {
                val shouldRefresh = pull >= thresholdPx && !state.refreshing
                if (shouldRefresh) {
                    animatePullTo(thresholdPx, 140f)
                    state.pullFraction = 1f
                    state.refresh(AppGraph.session.value != null || loggedIn)
                    // 回落交给 LaunchedEffect(state.refreshing)，这里不要再动，
                    // 否则会和首页那套「160ms 后 260ms 缓动」打架，看起来就是“唰”一下弹回。
                } else {
                    animatePullTo(0f, 220f)
                    state.pullFraction = 0f
                }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    LaunchedEffect(state.type, state.filterMid) { state.listState.scrollToItem(0) }
    LaunchedEffect(state, state.type, state.filterMid) {
        snapshotFlow {
            state.listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } to state.listState.layoutInfo.totalItemsCount
        }.collect { (last, count) ->
            if (count > 0 && last != null && last >= count - 2 && state.error == null) {
                state.loadMore(loggedIn)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!loggedIn) {
            EmptyHint(
                title = "动态",
                subtitle = "登录后可查看关注 UP 主的动态",
                action = "扫码登录",
                onAction = onNeedLogin,
            )
            return@Box
        }

        val visible = state.items

        // 只在滚动停下来后判断：离屏幕中心最近的那条如果是视频，就静音自动播放。
        var autoPlayId by remember { mutableStateOf<String?>(null) }
        val dynamicAutoPlay = AppGraph.settings.dynamicAutoPlay
        val autoPlayMuted = AppGraph.settings.dynamicAutoPlayMuted
        LaunchedEffect(state.listState, visible, dynamicAutoPlay) {
            if (!dynamicAutoPlay) {
                autoPlayId = null
                return@LaunchedEffect
            }
            androidx.compose.runtime.snapshotFlow {
                val info = state.listState.layoutInfo
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val nearest = info.visibleItemsInfo.minByOrNull {
                    kotlin.math.abs(it.offset.y + it.size.height / 2f - center)
                }?.key
                state.listState.isScrollInProgress to nearest
            }.collect { (scrolling, index) ->
                if (scrolling) return@collect
                // Stable item keys identify the nearest visible card independently of its lane.
                autoPlayId = visible.firstOrNull { it.id == index }
                    ?.takeIf { it.video != null || it.orig?.video != null }
                    ?.id
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 736.dp
        val sidebarWidth = dynamicSidebarWidth(maxWidth.value).dp
        Row(Modifier.fillMaxSize()) {
        if (wide) DynamicAuthorSidebar(state, Modifier.width(sidebarWidth)
            .padding(top = statusTop + DynamicTopBarHeight, bottom = 100.dp))
        LazyVerticalStaggeredGrid(
            columns = if (wide) BoundedVideoStaggeredGridCells(DynamicCardMinWidth.dp, DynamicCardMaxWidth.dp)
                else androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(1),
            state = state.listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)

                .offset { IntOffset(0, pull.roundToInt()) }
                .nestedScroll(connection),
            contentPadding = PaddingValues(
                start = 8.dp,
                // 固定顶栏显示分类分段栏；头像卡片已经放进列表里，会跟着一起滚走
                top = statusTop + DynamicTopBarHeight + 8.dp,
                end = 8.dp,
                bottom = 100.dp,
            ),
            verticalItemSpacing = 10.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            if (!wide) item(key = "avatar-card", span = StaggeredGridItemSpan.FullLine) {
                DynamicAvatarCard(
                    state = state,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            items(visible, key = { it.id }, contentType = { if (it.video != null) "video" else "dynamic" }) { item ->
                val likedNow = state.liked[item.id] ?: item.liked
                val countNow = state.likeCounts[item.id] ?: item.like
                DynamicCard(
                    item = item.copy(liked = likedNow, like = countNow),
                    liked = likedNow,
                    onOpen = {
                        state.markSeen(item.mid)
                        onOpenDynamic(item)
                    },
                    onOpenVideo = { onOpenVideo(it) },
                    onLike = {
                        val next = !likedNow
                        val newCount = (countNow + if (next) 1 else -1).coerceAtLeast(0)
                        state.liked[item.id] = next
                        state.likeCounts[item.id] = newCount
                        scope.launch {
                            val ok = AppGraph.api.likeDynamic(item.id, next)
                            if (ok) {
                                context.showToast(if (next) "点赞成功" else "已取消赞")
                            } else {
                                state.liked[item.id] = !next
                                state.likeCounts[item.id] = countNow
                                context.showToast("点赞失败")
                            }
                        }
                    },
                    onComment = { onOpenDynamic(item) },
                    onRepost = { onOpenDynamic(item) },
                    onOpenForwarded = { onOpenDynamic(it) },
                    autoPlay = dynamicAutoPlay && autoPlayId == item.id,
                    muted = autoPlayMuted,
                    onToggleMute = {
                        AppGraph.settings.dynamicAutoPlayMuted = !autoPlayMuted
                    },
                )
            }
            if (visible.isEmpty() && !state.refreshing && !state.loading && state.error == null) {
                item(key = "empty", span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        if (state.type == "up" && state.filterMid == null) "选择一位 UP 主查看动态" else "暂无动态",
                        color = colors.onBackgroundVariant,
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                    )
                }
            }
            if (state.hasMore && visible.isNotEmpty() && !state.activeLoading && !state.refreshing && state.error == null) {
                item(key = "more", span = StaggeredGridItemSpan.FullLine) {
                    Text("加载更多", color = colors.primary, modifier = Modifier
                        .fillMaxWidth().clickable { scope.launch { state.loadMore(loggedIn) } }.padding(16.dp))
                }
            }
            state.error?.let { message ->
                item(key = "error", span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = message,
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "点击重试",
                            color = colors.primary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(Capsule())
                                .clickable { scope.launch { state.retry(loggedIn) } }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            if (state.activeLoading) {
                item(key = "loading", span = StaggeredGridItemSpan.FullLine) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        TeakLoading(size = 34.dp)
                    }
                }
            }
        }

        }
        }

        // 首页同款下拉小电视：只在真的下拉时出现，不压头像卡片
        HomePullIndicator(
            reveal = if (state.pullOffsetDp <= 1f) {
                0f
            } else if (state.refreshing) {
                1f
            } else {
                ((state.pullOffsetDp - 58f) / 26f).coerceIn(0f, 1f)
            },
            animated = state.refreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusTop + DynamicTopBarHeight + 4.dp),
        )

        if (state.refreshing && state.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TeakLoading(size = 44.dp)
            }
        }
    }
}

/**
 * 固定顶栏：动态分类玻璃分段栏。
 * 挂在 App 层（内容采集层之外），能复用底栏同一块 backdrop 做真模糊，
 * 背景是首页同款渐进式模糊。
 */
@Composable
fun DynamicTopBar(
    state: DynamicFeedState,
    loggedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val statusTop = rememberStatusBarTop()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DynamicTopBarHeight + statusTop),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .teakBarBlur(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(vertical = 8.dp),
        ) {
            TeakSegmentedBar(
                options = listOf("全部", "视频", "番剧"),
                selected = listOf("all", "video", "pgc").indexOf(state.type).coerceAtLeast(0),
                itemWidth = 64.dp,
                materialAlpha = com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current,
                onSelect = { index ->
                    scope.launch {
                        state.switchTo(listOf("all", "video", "pgc")[index], loggedIn)
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/**
 * UP 头像卡片：不再固定在顶栏，而是列表第一项，跟着动态一起滚走。
 * 单个圆角矩形卡片内放「全部动态 + 关注 UP 头像」的横向滚动条。
 */
@Composable
fun DynamicAvatarCard(
    state: DynamicFeedState,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authorScroll = rememberLazyListState()
    val profile = AppGraph.session.value
    LaunchedEffect(state) {
        snapshotFlow {
            val info = authorScroll.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, count) ->
            if (count > 0 && last >= count - 3 && state.authorsError == null) state.loadMoreAuthors()
        }
    }
    Column(modifier.fillMaxWidth().clip(ContinuousCornerShape(18.dp)).background(colors.surface)) {
        if (state.liveCount > 0) {
            Row(Modifier.fillMaxWidth().clickable { state.showLive = !state.showLive }
                .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("正在直播 · ${state.liveCount}", color = colors.onSurface, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                Text(if (state.showLive) "收起" else "展开", color = colors.onSurfaceVariantSummary, fontSize = 12.sp)
            }
            if (state.showLive) LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.liveUsers, key = { it.roomId }) { live ->
                    AuthorAvatar(name = live.name, face = live.face, selected = false, badge = "直播") {
                        openDynamicLive(context, live.roomId)
                    }
                }
            }
        }
        val entries: LazyListScope.() -> Unit = {
            item(key = "all-dynamics") {
                AllDynamicAvatar(selected = state.type == "all") {
                    scope.launch { state.listState.scrollToItem(0); state.clearFilter() }
                }
            }
            profile?.let { me ->
                item(key = "my-dynamics") {
                    AuthorAvatar(name = "我", face = me.face, selected = state.filterMid == me.mid) {
                        scope.launch { state.listState.scrollToItem(0); state.filterTo(me.mid) }
                    }
                }
            }
            items(state.authors, key = { "up-${it.mid}" }) { author ->
                AuthorAvatar(name = author.name, face = author.face,
                    selected = state.filterMid == author.mid, hasUpdate = author.hasUpdate) {
                    scope.launch { state.listState.scrollToItem(0); state.filterTo(author.mid) }
                }
            }
            if (state.authorsLoading) item(key = "up-loading") { TeakLoading(size = 28.dp) }
            state.authorsError?.let { message ->
                item(key = "up-error") {
                    Text(message, color = colors.primary, fontSize = 12.sp,
                        modifier = Modifier.width(100.dp).clickable {
                            scope.launch { if (state.authorsHasMore) state.loadMoreAuthors() else state.refreshAuthors() }
                        }.padding(8.dp))
                }
            }
            if (state.authorsHasMore && !state.authorsLoading && state.authorsError == null) {
                item(key = "up-more") {
                    Text("更多 UP", color = colors.primary, modifier = Modifier.clickable {
                        scope.launch { state.loadMoreAuthors() }
                    }.padding(12.dp))
                }
            }
        }
        LazyRow(state = authorScroll, verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), content = entries)
    }
}

@Composable
private fun AuthorAvatar(
    name: String,
    face: String,
    selected: Boolean,
    hasUpdate: Boolean = false,
    badge: String? = null,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val ring = rememberManualProgress(if (selected) 1f else 0f, 200f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .semantics { stateDescription = if (hasUpdate) "有更新" else badge.orEmpty() }
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp),
        ) {
            AsyncImage(
                model = rememberImageRequest(face),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh),
            )
            if (hasUpdate) {
                Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(colors.primary, CircleShape))
            }
            if (badge != null) {
                Text(badge, color = colors.onPrimary, fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).clip(Capsule())
                        .background(colors.primary).padding(horizontal = 4.dp))
            }
            // 选中圈只做透明度渐显，不改变头像尺寸和位置
            if (ring > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            2.dp,
                            colors.primary.copy(alpha = ring.coerceIn(0f, 1f)),
                            CircleShape,
                        ),
                )
            }
        }
        Text(
            text = name,
            color = colors.onBackgroundVariant,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** 头像排最左边的「全部动态」：粉色渐变圆 + 四宫格图标，尺寸/描边和 UP 头像一致。 */
@Composable
private fun AllDynamicAvatar(selected: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val ring = rememberManualProgress(if (selected) 1f else 0f, 200f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.55f),
                                BrandPink,
                                BrandPinkDeep,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val w = size.width
                    val h = size.height
                    val gap = w * 0.12f
                    val cell = (w - gap) / 2f
                    val r = CornerRadius(cell * 0.22f)
                    listOf(
                        Offset(0f, 0f),
                        Offset(cell + gap, 0f),
                        Offset(0f, cell + gap),
                        Offset(cell + gap, cell + gap),
                    ).forEach { topLeft ->
                        drawRoundRect(
                            color = Color.White,
                            topLeft = topLeft,
                            size = Size(cell, cell),
                            cornerRadius = r,
                        )
                    }
                }
            }
            if (ring > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(
                            2.dp,
                            colors.primary.copy(alpha = ring.coerceIn(0f, 1f)),
                            CircleShape,
                        ),
                )
            }
        }
        Text(
            text = "全部动态",
            color = colors.onBackgroundVariant,
            fontSize = 9.5.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** 只暴露"最后一个可见项索引"的小工具。 */
private fun snapshotFlowLastIndex(listState: androidx.compose.foundation.lazy.LazyListState) =
    androidx.compose.runtime.snapshotFlow {
        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
    }

@Composable
internal fun DynamicCard(
    item: DynamicItem,
    liked: Boolean,
    onOpen: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onRepost: () -> Unit,
    onOpenForwarded: (DynamicItem) -> Unit = {},
    autoPlay: Boolean = false,
    muted: Boolean = true,
    onToggleMute: () -> Unit = {},
    /** 详情页传 true：正文/标题不再截断，进入即完整展示。 */
    expanded: Boolean = false,
    /** 点整张卡片是否走系统放大动画；详情页自己点自己时关掉。 */
    animatedOpen: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val shape = ContinuousCornerShape(16.dp)
    // 整张动态卡片当"起飞点"：点卡片里的视频时，从整卡放大到视频页
    val oneTake = rememberOneTakeAnchor()
    val cardRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
    // 点图片只看大图，不进详情页；点卡片其他区域仍然是进详情
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    var viewerOrigin by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = modifier
            .then(oneTake.modifier)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null) {
                if (animatedOpen && item.mid > 0L) {
                    oneTake.openDynamic(context, item, cardRadiusPx) { onOpen() }
                } else {
                    onOpen()
                }
            }
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = rememberImageRequest(item.face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
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
                Text(
                    text = item.name,
                    color = if (item.authorVip) colors.primary else colors.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .offset(y = 2.dp)
                        .clickable(interactionSource = null, indication = null) {
                            if (item.mid > 0L) {
                                com.aritxonly.bdfun.TeakNavigator.openSpace(context, item.mid)
                            }
                        },
                )
                Text(
                    text = item.time,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.offset(x = 1.dp),
                )
            }
        }

        if (item.moduleTag.isNotBlank()) {
            Text(item.moduleTag, color = colors.primary, fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp))
        }

        if (item.title.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 15.5.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (item.text.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            EmoteText(
                text = item.text,
                color = colors.onBackground,
                fontSize = 15.5.sp,
                lineHeight = 23.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 6,
            )
        }

        if (item.images.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            DynamicImageGrid(
                images = item.images,
                onOpen = { url, rect ->
                    viewerUrl = url
                    viewerOrigin = rect
                },
            )
        }

        if (item.pgcCover.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(ContinuousCornerShape(12.dp))
                    .background(colors.surfaceContainerHigh)
                    .clickable(interactionSource = null, indication = null, onClick = onOpen),
            ) {
                AsyncImage(
                    model = rememberImageRequest(item.pgcCover),
                    contentDescription = item.pgcTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .width(42.dp)
                        .clip(Capsule())
                        .background(colors.primary.copy(alpha = 0.92f))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.pgcLabel.ifBlank { "剧集" },
                        color = Color.White,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
            if (item.pgcTitle.isNotBlank()) {
                Text(
                    text = item.pgcTitle,
                    color = colors.onBackground,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        item.liveRecommendation?.let { live ->
            Spacer(Modifier.height(10.dp))
            DynamicLiveCard(live)
        }
        item.video?.let { video ->
            Spacer(Modifier.height(10.dp))
            DynamicVideoCard(
                video = video,
                isCollection = item.isCollection,
                autoPlay = autoPlay,
                muted = muted,
                onOpenVideo = { v -> oneTake.open(context, v, cardRadiusPx) { onOpenVideo(v) } },
                onToggleMute = onToggleMute,
            )
        }

        item.orig?.let { orig ->
            Spacer(Modifier.height(8.dp))
            ForwardedDynamicBlock(
                item = orig,
                onOpen = { onOpenForwarded(orig) },
                onOpenVideo = { v -> oneTake.open(context, v, cardRadiusPx) { onOpenVideo(v) } },
                autoPlay = autoPlay,
                muted = muted,
                onToggleMute = onToggleMute,
            )
        }

        if (item.ipLocation.isNotBlank()) {
            Text(
                text = "IP属地：${item.ipLocation}",
                modifier = Modifier.padding(top = 8.dp),
                color = colors.onBackgroundVariant.copy(alpha = 0.72f),
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.height(6.dp))
        // 三个操作均分整行、各自居中：图标列和数量都严格对齐
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DynamicAction(
                modifier = Modifier.weight(1f),
                icon = {
                    ResourceGlyph(
                        res = TeakIcon.Share,
                        tint = colors.onBackgroundVariant,
                        size = 18.dp,
                    )
                },
                count = item.forward,
                onClick = onRepost,
            )
            DynamicAction(
                modifier = Modifier.weight(1f),
                icon = {
                    ResourceGlyph(
                        res = TeakIcon.CommentBubble,
                        tint = colors.onBackgroundVariant,
                        size = 18.dp,
                    )
                },
                count = item.comment,
                onClick = onComment,
            )
            DynamicAction(
                modifier = Modifier.weight(1f),
                icon = {
                    ResourceGlyph(
                        res = if (liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
                        tint = if (liked) BrandPink else colors.onBackgroundVariant,
                        size = 18.dp,
                    )
                },
                count = item.like,
                tint = if (liked) BrandPink else colors.onBackgroundVariant,
                onClick = onLike,
            )
        }
    }

    viewerUrl?.let { url ->
        DynamicImageViewer(
            item = item,
            url = url,
            origin = viewerOrigin,
            liked = liked,
            onDismiss = { viewerUrl = null },
            onLike = onLike,
            onComment = onComment,
            onRepost = onRepost,
        )
    }
}

/** 一张图用大图（和视频卡同宽），多张图保持三列小图。 */
@Composable
private fun DynamicImageGrid(
    images: List<String>,
    onOpen: (String, Rect) -> Unit,
) {
    if (images.isEmpty()) return
    val colors = MiuixTheme.colorScheme
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    if (images.size == 1) {
        val url = images.first()
        AsyncImage(
            model = rememberImageRequest(url),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(ContinuousCornerShape(12.dp))
                .background(colors.surfaceContainerHigh)
                .onGloballyPositioned { bounds[url] = it.boundsInRoot() }
                .clickable(interactionSource = null, indication = null) {
                    onOpen(url, bounds[url] ?: Rect.Zero)
                },
        )
        return
    }
    images.take(9).chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            row.forEach { url ->
                AsyncImage(
                    model = rememberImageRequest(url),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(ContinuousCornerShape(10.dp))
                        .background(colors.surfaceContainerHigh)
                        .onGloballyPositioned { bounds[url] = it.boundsInRoot() }
                        .clickable(interactionSource = null, indication = null) {
                            onOpen(url, bounds[url] ?: Rect.Zero)
                        },
                )
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** 主动态和转发动态共用同一套大卡视频布局。 */
@Composable
private fun DynamicVideoCard(
    video: VideoItem,
    isCollection: Boolean,
    autoPlay: Boolean,
    muted: Boolean,
    onOpenVideo: (VideoItem) -> Unit,
    onToggleMute: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(ContinuousCornerShape(12.dp))
            .background(colors.surfaceContainerHigh)
            .clickable(interactionSource = null, indication = null) { onOpenVideo(video) },
    ) {
        AsyncImage(
            model = rememberImageRequest(video.cover),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (autoPlay) {
            InlineVideoPlayer(
                video = video,
                muted = muted,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isCollection) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .width(42.dp)
                    .clip(Capsule())
                    .background(colors.primary.copy(alpha = 0.92f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "合集",
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(46.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x66000000)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDuration(video.duration),
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.width(10.dp))
            MetricGlyph(play = true, size = 13.dp)
            Spacer(Modifier.width(3.dp))
            Text(
                text = formatCount(video.play),
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.width(10.dp))
            MetricGlyph(play = false, size = 13.dp)
            Spacer(Modifier.width(3.dp))
            Text(
                text = formatCount(video.danmaku),
                color = Color.White,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clickable(interactionSource = null, indication = null) {
                        onToggleMute()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (muted) MiuixIcons.VolumeOff else MiuixIcons.VolumeUp,
                    contentDescription = if (muted) "静音" else "声音",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
    Text(
        text = video.title,
        color = colors.onBackground,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** 动态图片的大图预览：黑底、顶部关闭/更多、底部作者信息和三个操作。 */
@Composable
private fun DynamicImageViewer(
    item: DynamicItem,
    url: String,
    origin: Rect?,
    liked: Boolean,
    onDismiss: () -> Unit,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onRepost: () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scope = rememberCoroutineScope()
    val dismissAnimated: () -> Unit = {
        if (shown) {
            shown = false
            scope.launch {
                delay(200)
                onDismiss()
            }
        } else {
            onDismiss()
        }
        Unit
    }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val startScaleX = (origin?.width?.div(screenWidthPx) ?: 0.9f).coerceIn(0.2f, 1f)
    val startScaleY = (origin?.height?.div(screenHeightPx) ?: 0.9f).coerceIn(0.2f, 1f)
    val originCenter = origin?.center ?: Offset(screenWidthPx / 2f, screenHeightPx / 2f)
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 220,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "dynamic-image-zoom",
    )
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180),
        label = "dynamic-image-alpha",
    )
    val images = remember(item.images, url) {
        item.images.ifEmpty { listOf(url) }
    }
    val imagePager = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = images.indexOf(url).coerceAtLeast(0).coerceAtMost(images.lastIndex),
        pageCount = { images.size },
    )
    androidx.compose.ui.window.Dialog(
        onDismissRequest = dismissAnimated,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "✕",
                    color = Color.White,
                    fontSize = 26.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(interactionSource = null, indication = null, onClick = dismissAnimated)
                        .padding(6.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "⋮",
                    color = Color.White,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(6.dp),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = imagePager,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = startScaleX + (1f - startScaleX) * progress
                            scaleY = startScaleY + (1f - startScaleY) * progress
                            translationX = (originCenter.x - screenWidthPx / 2f) * (1f - progress)
                            translationY = (originCenter.y - screenHeightPx / 2f) * (1f - progress)
                            this.alpha = alpha
                        },
                ) { page ->
                    var scale by remember { mutableStateOf(1f) }
                    var pan by remember { mutableStateOf(Offset.Zero) }
                    AsyncImage(
                        model = rememberImageRequest(images[page]),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp)
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
                                            scale = (scale * zoomChange).coerceIn(1f, 4f)
                                            pan = if (scale > 1f) pan + panChange else Offset.Zero
                                            event.changes.forEach { it.consume() }
                                        } else if (scale > 1.01f) {
                                            pan += panChange
                                            event.changes.forEach { it.consume() }
                                        } else {
                                            pan = Offset.Zero
                                        }
                                        if (event.changes.none { it.pressed }) break
                                    }
                                    // 没放大时点一下空白：关闭。
                                    if (maxPressed <= 1 && !moved && scale <= 1.01f) {
                                        dismissAnimated()
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
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 26.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = rememberImageRequest(item.face),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(text = item.time, color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                    }
                    Text(
                        text = "⌄",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(interactionSource = null, indication = null, onClick = dismissAnimated)
                            .padding(horizontal = 8.dp),
                    )
                }
                if (item.text.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = item.text,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    DynamicAction(
                        modifier = Modifier.weight(1f),
                        icon = {
                            ResourceGlyph(TeakIcon.Share, Color.White, size = 20.dp)
                        },
                        count = item.forward,
                        tint = Color.White,
                        onClick = onRepost,
                    )
                    DynamicAction(
                        modifier = Modifier.weight(1f),
                        icon = {
                            ResourceGlyph(TeakIcon.CommentBubble, Color.White, size = 20.dp)
                        },
                        count = item.comment,
                        tint = Color.White,
                        onClick = onComment,
                    )
                    DynamicAction(
                        modifier = Modifier.weight(1f),
                        icon = {
                            ResourceGlyph(
                                if (liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
                                if (liked) BrandPink else Color.White,
                                size = 20.dp,
                            )
                        },
                        count = item.like,
                        tint = if (liked) BrandPink else Color.White,
                        onClick = onLike,
                    )
                }
            }
        }
    }
}

/** 转发动态里引用的原动态：只展示内容，不重复操作栏。 */
@Composable
private fun ForwardedDynamicBlock(
    item: DynamicItem,
    onOpen: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    autoPlay: Boolean,
    muted: Boolean,
    onToggleMute: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val forwardedOneTake = rememberOneTakeAnchor()
    val forwardedRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    var viewerOrigin by remember { mutableStateOf<Rect?>(null) }
    Column(
        modifier = Modifier
            .then(forwardedOneTake.modifier)
            .fillMaxWidth()
            .clip(ContinuousCornerShape(12.dp))
            .background(colors.surfaceContainerHigh)
            .clickable(interactionSource = null, indication = null) {
                forwardedOneTake.openDynamic(context, item, forwardedRadiusPx) { onOpen() }
            }
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = rememberImageRequest(item.face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHighest)
                    .clickable(interactionSource = null, indication = null) {
                        if (item.mid > 0L) {
                            com.aritxonly.bdfun.TeakNavigator.openSpace(context, item.mid)
                        }
                    },
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = item.name,
                color = if (item.authorVip) colors.primary else colors.onBackground,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(
                    interactionSource = null,
                    indication = null,
                ) {
                    if (item.mid > 0L) {
                        com.aritxonly.bdfun.TeakNavigator.openSpace(context, item.mid)
                    }
                },
            )
            Spacer(Modifier.weight(1f))
            Text(text = item.time, color = colors.onBackgroundVariant, fontSize = 10.sp)
        }
        if (item.title.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            EmoteText(
                text = item.text,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 4,
            )
        }
        if (item.images.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            DynamicImageGrid(
                images = item.images,
                onOpen = { url, rect ->
                    viewerUrl = url
                    viewerOrigin = rect
                },
            )
        }
        item.liveRecommendation?.let { live ->
            Spacer(Modifier.height(10.dp))
            DynamicLiveCard(live)
        }
        item.video?.let { video ->
            Spacer(Modifier.height(8.dp))
            DynamicVideoCard(
                video = video,
                isCollection = item.isCollection,
                autoPlay = autoPlay,
                muted = muted,
                onOpenVideo = onOpenVideo,
                onToggleMute = onToggleMute,
            )
        }
    }
    viewerUrl?.let { url ->
        DynamicImageViewer(
            item = item,
            url = url,
            origin = viewerOrigin,
            liked = item.liked,
            onDismiss = { viewerUrl = null },
            onLike = {},
            onComment = onOpen,
            onRepost = onOpen,
        )
    }
}

@Composable
private fun DynamicAction(
    icon: @Composable () -> Unit,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            if (count > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatCount(count.toLong()),
                    color = tint ?: colors.onBackgroundVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.offset(y = 1.dp),
                )
            }
        }
    }
}

/**
 * 评论气泡：圆角矩形 + 左下角小尾巴（原来只有方框，容易被看成"图片"图标）。
 * 描边宽度和分享/点赞图标同一视觉重量。
 */
@Composable
private fun CommentGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f
        val bodyTop = h * 0.10f
        val bodyHeight = h * 0.62f
        val bodyLeft = w * 0.09f
        val bodyWidth = w * 0.82f
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(w * 0.20f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
        // 左下角的尾巴
        val tail = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.27f, bodyTop + bodyHeight - stroke * 0.4f)
            lineTo(w * 0.24f, h * 0.90f)
            lineTo(w * 0.47f, bodyTop + bodyHeight - stroke * 0.4f)
        }
        drawPath(
            path = tail,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun EmptyHint(
    title: String,
    subtitle: String,
    action: String,
    onAction: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(text = subtitle, color = colors.onBackgroundVariant, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.primary)
                .clickable { onAction() }
                .padding(horizontal = 26.dp, vertical = 11.dp),
        ) {
            Text(text = action, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Normal)
        }
    }
}


private fun openDynamicLive(context: android.content.Context, roomId: Long) {
    if (roomId <= 0) return
    runCatching {
        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://live.bilibili.com/$roomId")))
    }.onFailure { context.showToast("无法打开直播间") }
}

@Composable
private fun DynamicLiveCard(live: DynamicLiveRecommendation) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().clip(ContinuousCornerShape(12.dp))
        .background(colors.surfaceContainerHigh).clickable(enabled = live.roomId > 0) {
            openDynamicLive(context, live.roomId)
        }) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(model = rememberImageRequest(live.cover), contentDescription = live.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Text(if (live.live) "直播中" else "直播结束", color = colors.onPrimary,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(Capsule())
                    .background(colors.primary).padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp)
        }
        Text(live.title, color = colors.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp))
        Text(listOf(live.areaName, live.watched).filter { it.isNotBlank() }.joinToString(" · "),
            color = colors.onSurfaceVariantSummary, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
    }
}

/** Wide screens keep author navigation separate from the independently scrolling masonry feed. */
@Composable
private fun DynamicAuthorSidebar(state: DynamicFeedState, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authorState = rememberLazyListState()
    LaunchedEffect(state, authorState) {
        snapshotFlow {
            authorState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to authorState.layoutInfo.totalItemsCount
        }.collect { (last, count) ->
            if (count > 0 && last != null && last >= count - 3 && state.authorsError == null) state.loadMoreAuthors()
        }
    }
    LazyColumn(state = authorState, modifier = modifier, contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item(key = "all") {
            DynamicAuthorRow("全部动态", "", state.filterMid == null, false) {
                scope.launch { state.clearFilter() }
            }
        }
        if (state.liveCount > 0) {
            item(key = "live-toggle") {
                Text("正在直播 · ${state.liveCount}  ${if (state.showLive) "收起" else "展开"}",
                    fontSize = 12.sp, color = colors.onBackgroundVariant,
                    modifier = Modifier.fillMaxWidth().clickable { state.showLive = !state.showLive }.padding(10.dp))
            }
            if (state.showLive) items(state.liveUsers, key = { "live-${it.roomId}" }) { live ->
                DynamicAuthorRow(live.name, live.face, false, false, live = true) { openDynamicLive(context, live.roomId) }
            }
        }
        AppGraph.session.value?.let { me ->
            item(key = "me") {
                DynamicAuthorRow("我", me.face, state.filterMid == me.mid, false) {
                    scope.launch { state.filterTo(me.mid) }
                }
            }
        }
        items(state.authors, key = { "author-${it.mid}" }) { author ->
            DynamicAuthorRow(author.name, author.face, state.filterMid == author.mid, author.hasUpdate) {
                scope.launch { state.filterTo(author.mid) }
            }
        }
        if (state.authorsLoading) item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TeakLoading(size = 28.dp) } }
        if (state.authorsError != null || state.authorsHasMore) item {
            Text(if (state.authorsError != null) "加载失败，点击重试" else "更多 UP", color = colors.primary, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().clickable { scope.launch {
                    if (state.authorsHasMore) state.loadMoreAuthors() else state.refreshAuthors()
                } }.padding(12.dp))
        }
    }
}

@Composable
private fun DynamicAuthorRow(name: String, face: String, selected: Boolean, updated: Boolean,
    live: Boolean = false, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth().clip(ContinuousCornerShape(14.dp))
        .background(if (selected) colors.surface else Color.Transparent)
        .clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(38.dp).semantics { stateDescription = if (updated) "有更新" else if (live) "直播中" else "" }) {
            if (face.isBlank()) Box(Modifier.fillMaxSize().background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                Text("全", color = colors.onPrimary, fontSize = 16.sp)
            } else AsyncImage(model = rememberImageRequest(face), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            if (updated || live) Box(Modifier.align(Alignment.TopEnd).size(8.dp).background(colors.primary, CircleShape))
        }
        Text(name, color = if (selected) colors.primary else colors.onBackground, fontSize = 12.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}
