package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.centeredListWidth
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.teak.common.chrome.teakProgressiveBarBlur
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.navigation.TeakFloatingTabBar
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.teak.common.navigation.TeakFloatingTabLayout
import com.aritxonly.teak.common.navigation.TeakMenuAnchor
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 搜索详情页的筛选条件。 */
private data class SearchFilterState(
    /** 0 默认 / 1 新发布 / 2 播放多 / 3 弹幕多 */
    val order: Int = 0,
    /** 0 不限 / 1 最近一天 / 2 最近一周 / 3 最近半年 */
    val published: Int = 0,
    /** 0 不限 / 1 0-10 / 2 10-30 / 3 30-60 / 4 60-120 / 5 120+ */
    val duration: Int = 0,
    /** 0 不限 / 1 8K / 2 4K / 3 1080P（服务端不保证支持，仅作为偏好记录） */
    val quality: Int = 0,
) {
    val orderParam: String
        get() = when (order) {
            1 -> "pubdate"
            2 -> "click"
            3 -> "dm"
            else -> "totalrank"
        }

    val durationParam: Int
        get() = duration

    val beginSeconds: Long?
        get() {
            val days = when (published) {
                1 -> 1L
                2 -> 7L
                3 -> 183L
                else -> return null
            }
            return System.currentTimeMillis() / 1000L - days * 24L * 3600L
        }
}

/**
 * 搜索页：搜索框保持原位不动，这张白纸**以搜索框为原点**从它的大小无缝长到整屏。
 *
 * - 未提交时：搜索记录（与热搜同款排名样式，双排）+ 热搜榜；
 * - 提交后：顶部「视频 / 用户」Tab + 右侧漏斗筛选，下面是结果列表。
 *
 * [reveal] 由 App 驱动（0 → 1），既用于展开，也用于内容错峰淡入。
 */
@Composable
fun SearchOverlay(
    api: BiliApi,
    settings: SettingsStore,
    submitted: String?,
    /** 当前输入框里的词；未提交时用它拉搜索联想。 */
    query: String = "",
    reveal: Float,
    onPickKeyword: (String) -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenUser: (SearchUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    var hotKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var users by remember { mutableStateOf<List<SearchUser>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    // 请求真正回来之前不显示“没有找到”，避免搜索刚提交时先闪一下空结果。
    var loadedOnce by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    var filterOpen by remember { mutableStateOf(false) }
    var filters by remember { mutableStateOf(SearchFilterState()) }
    var appliedFilters by remember { mutableStateOf(SearchFilterState()) }
    // 搜索记录需要本页立即刷新：清空后当前界面马上变空，不等下次重新组合。
    var history by remember { mutableStateOf(settings.searchHistory) }
    val rememberKeyword: (String) -> Unit = { word ->
        settings.addSearchHistory(word)
        history = settings.searchHistory
    }

    LaunchedEffect(Unit) {
        if (settings.showHotSearch && hotKeywords.isEmpty()) hotKeywords = api.hotKeywords()
    }

    // 输入时防抖拉联想；提交结果页后不再拉。
    LaunchedEffect(query, submitted) {
        val word = query.trim()
        if (submitted != null || word.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(180)
        suggestions = api.searchSuggest(word)
    }

    // 关键词或筛选条件变化都走同一个请求集合，避免两段 Effect 互相覆盖结果。
    // 用户结果只在关键词变化时重新拉；改排序/时长/发布时间不需要重拉用户。
    var lastUserKeyword by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(submitted, appliedFilters) {
        if (submitted == null) {
            results = emptyList()
            users = emptyList()
            error = null
            tab = 0
            filterOpen = false
            lastUserKeyword = null
            loadedOnce = false
            return@LaunchedEffect
        }
        // 一提交就进入加载态，避免请求还没发出时先闪“没有找到相关视频”。
        loading = true
        loadedOnce = false
        error = null
        // 筛选是连续点选的，稍微等 180ms 再发请求，避免堆积。
        if (lastUserKeyword != submitted) delay(90) else delay(180)
        if (lastUserKeyword != submitted) {
            runCatching { api.searchUsers(submitted) }
                .onSuccess {
                    users = it
                    lastUserKeyword = submitted
                }
                .onFailure { /* 用户搜索失败不影响视频结果 */ }
        }
        runCatching {
            api.search(
                keyword = submitted,
                order = appliedFilters.orderParam,
                duration = appliedFilters.durationParam,
                pubtimeBegin = appliedFilters.beginSeconds,
                pubtimeEnd = if (appliedFilters.beginSeconds != null) {
                    System.currentTimeMillis() / 1000L
                } else {
                    null
                },
            )
        }
            .onSuccess { results = it }
            .onFailure { error = it.message ?: "搜索失败" }
        loading = false
        loadedOnce = true
    }

    val t = reveal.coerceIn(0f, 1f)
    val contentReveal = t

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val pillCenterY = with(density) { (statusTop + HomeTopBarHeight / 2).toPx() }
        val pillHeightPx = with(density) { 40.dp.toPx() }
        val pillWidthPx = widthPx * 0.80f

        // 动画结束后不再保留离屏图层，避免文字被当成半透明层渲染而显得变细。
        val revealLayer = if (t < 0.999f) {
            Modifier.graphicsLayer { alpha = t }
        } else {
            Modifier
        }
        val contentLayer = if (contentReveal < 0.999f) {
            Modifier.graphicsLayer {
                alpha = contentReveal
                translationY = (1f - contentReveal) * 22.dp.toPx()
            }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(revealLayer)
                .background(colors.background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    // 结果页不再给整块内容留顶栏高度，让列表能滚到顶栏下面被模糊；
                    // 搜索首页（记录/热搜）仍从顶栏下方开始。
                    .padding(top = if (submitted == null) HomeTopBarHeight else 0.dp)
                    .then(contentLayer),
            ) {
                if (submitted == null) {
                    SearchLanding(
                        history = history,
                        hot = if (settings.showHotSearch) hotKeywords else emptyList(),
                        query = query,
                        suggestions = suggestions,
                        onPickKeyword = { word ->
                            rememberKeyword(word)
                            onPickKeyword(word)
                        },
                        onClearHistory = {
                            settings.clearSearchHistory()
                            history = emptyList()
                        },
                    )
                } else {
                    SearchResults(
                        tab = tab,
                        onTabChange = { tab = it },
                        filterOpen = filterOpen,
                        onFilterOpenChange = { filterOpen = it },
                        filters = filters,
                        onFiltersChange = { filters = it },
                        onApplyFilters = { filters = it },
                        appliedFilters = appliedFilters,
                        results = results,
                        users = users,
                        loading = loading,
                        loadedOnce = loadedOnce,
                        error = error,
                        onOpenVideo = onOpenVideo,
                        onOpenUser = onOpenUser,
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------- landing */

@Composable
private fun SearchLanding(
    history: List<String>,
    hot: List<String>,
    query: String,
    suggestions: List<String>,
    onPickKeyword: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().centeredListWidth(scrollState = listState),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 32.dp),
    ) {
        // 输入框有内容时，用联想词替代记录 / 热搜。
        if (query.isNotBlank()) {
            if (suggestions.isNotEmpty()) {
                item(key = "suggest-title") {
                    SectionHeader(title = "搜索建议", action = null, onAction = null)
                }
                item(key = "suggest-list") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surface)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        suggestions.forEach { word ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPickKeyword(word) }
                                    .padding(vertical = 12.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SearchSuggestGlyph()
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = word,
                                    color = colors.onBackground,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            return@LazyColumn
        }

        if (history.isNotEmpty()) {
            item(key = "history-title") {
                SectionHeader(
                    title = "搜索记录",
                    action = "清空",
                    onAction = onClearHistory,
                )
            }
            item(key = "history-grid") {
                // 与热搜一致的圆角卡片，但去掉排名序号，改成双排词条。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    history.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { word ->
                                HistoryWord(
                                    word = word,
                                    onClick = { onPickKeyword(word) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }

        if (hot.isNotEmpty()) {
            item(key = "hot-title") {
                SectionHeader(title = "热搜", action = null, onAction = null)
            }
            item(key = "hot-list") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    hot.forEachIndexed { index, word ->
                        HotRow(
                            rank = index + 1,
                            word = word,
                            onClick = { onPickKeyword(word) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestGlyph() {
    val colors = MiuixTheme.colorScheme
    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()
        drawCircle(
            color = colors.onBackgroundVariant.copy(alpha = 0.75f),
            radius = w * 0.30f,
            center = androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.42f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke),
        )
        drawLine(
            color = colors.onBackgroundVariant.copy(alpha = 0.75f),
            start = androidx.compose.ui.geometry.Offset(w * 0.64f, h * 0.64f),
            end = androidx.compose.ui.geometry.Offset(w * 0.86f, h * 0.86f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun HistoryWord(
    word: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .clip(Capsule())
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = word,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------------------------------------- results */

@Composable
private fun SearchResults(
    tab: Int,
    onTabChange: (Int) -> Unit,
    filterOpen: Boolean,
    onFilterOpenChange: (Boolean) -> Unit,
    filters: SearchFilterState,
    onFiltersChange: (SearchFilterState) -> Unit,
    onApplyFilters: (SearchFilterState) -> Unit,
    appliedFilters: SearchFilterState,
    results: List<VideoItem>,
    users: List<SearchUser>,
    loading: Boolean,
    loadedOnce: Boolean,
    error: String?,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenUser: (SearchUser) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    LaunchedEffect(tab) {
        if (pagerState.currentPage != tab) pagerState.animateScrollToPage(tab)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != tab) onTabChange(pagerState.currentPage)
    }
    // 指示器跟随手指：左右滑时不是等松手才跳过去。
    val position = remember {
        derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }
    val videoListState = rememberLazyListState()
    val userListState = rememberLazyListState()
    TrackTeakPageScroll(if (pagerState.currentPage == 0) videoListState else userListState)
    // 给结果内容单独录一层：Tab 栏是它的兄弟节点，不在录制范围内，
    // 因此 Tab 的玻璃可以安全采样，不会形成自引用。
    val colors = MiuixTheme.colorScheme
    val tabBackdrop = rememberLayerBackdrop {
        drawRect(colors.background)
        drawContent()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(tabBackdrop),
        ) {
            CompositionLocalProvider(
                LocalViewConfiguration provides rememberStricterSwipeConfiguration(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    overscrollEffect = null,
                ) { page ->
                    val pageColors = MiuixTheme.colorScheme
                    LazyColumn(
                        state = if (page == 0) videoListState else userListState,
                        modifier = Modifier.fillMaxSize().centeredListWidth(
                            scrollState = if (page == 0) videoListState else userListState,
                        ),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            // 顶栏 + Tab 栏 + 一点间距
                            top = HomeTopBarHeight + 54.dp,
                            bottom = 32.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (loading && results.isEmpty() && users.isEmpty()) {
                            item(key = "loading") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TeakLoading()
                                }
                            }
                        }
                        error?.let { message ->
                            if (results.isEmpty() && users.isEmpty()) {
                                item(key = "error") {
                                    Text(
                                        text = message,
                                        color = pageColors.onBackgroundVariant,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 24.dp),
                                    )
                                }
                            }
                        }

                        if (page == 0) {
                            items(items = results, key = { "r" + it.bvid }) { video ->
                                val oneTake = rememberOneTakeAnchor()
                                val itemContext = LocalContext.current
                                val cardRadiusPx = with(LocalDensity.current) { 10.dp.toPx() }
                                SearchResultRow(
                                    video = video,
                                    modifier = oneTake.modifier,
                                    onClick = {
                                        oneTake.open(itemContext, video, cardRadiusPx) {
                                            onOpenVideo(video)
                                        }
                                    },
                                )
                            }
                            if (loadedOnce && !loading && results.isEmpty() && error == null) {
                                item(key = "empty-videos") {
                                    Text(
                                        text = "没有找到相关视频",
                                        color = pageColors.onBackgroundVariant,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 24.dp),
                                    )
                                }
                            }
                        } else {
                            items(items = users, key = { "u" + it.mid }) { user ->
                                SearchUserRow(user = user, onClick = { onOpenUser(user) })
                            }
                            if (loadedOnce && !loading && users.isEmpty() && error == null) {
                                item(key = "empty-users") {
                                    Text(
                                        text = "没有找到相关用户",
                                        color = pageColors.onBackgroundVariant,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 24.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tab / 筛选按钮固定不动，只跟随页面本身的滚动，不再随列表上移。
        CompositionLocalProvider(LocalTeakBackdrop provides tabBackdrop) {
            ResultsTabHeader(
                tab = tab,
                onTabChange = onTabChange,
                position = position,
                filterOpen = filterOpen,
                onFilterOpenChange = onFilterOpenChange,
                filters = filters,
                onFiltersChange = onFiltersChange,
                onApplyFilters = onApplyFilters,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = HomeTopBarHeight + 6.dp),
            )
        }
    }
}

/** 视频 / 用户 Tab：和视频页简介评论同尺寸，固定在顶栏下方。 */
@Composable
private fun ResultsTabHeader(
    tab: Int,
    onTabChange: (Int) -> Unit,
    position: androidx.compose.runtime.State<Float>,
    filterOpen: Boolean,
    onFilterOpenChange: (Boolean) -> Unit,
    filters: SearchFilterState,
    onFiltersChange: (SearchFilterState) -> Unit,
    onApplyFilters: (SearchFilterState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterAnchor by remember {
        mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
    }
    Box(
        modifier = modifier.height(42.dp),
    ) {
        TeakPagerTabs(
            options = listOf("视频", "用户"),
            selected = tab,
            onSelect = onTabChange,
            itemWidth = 64.dp,
            position = position,
            modifier = Modifier.align(Alignment.Center),
        )
        // 漏斗：真正采样 Tab 后面的内容，和设置页二级菜单一样是 Miuix 玻璃。
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                // 顶栏清空按钮的右边距也是 12dp，这里正好对齐。
                .padding(end = 12.dp)
                .onGloballyPositioned { filterAnchor = it.boundsInWindow() },
        ) {
            com.aritxonly.teak.common.material.TeakGlassIconButton(
                onClick = { onFilterOpenChange(!filterOpen) },
                size = 40.dp,
                backdrop = LocalTeakBackdrop.current,
            ) {
                FunnelGlyph()
            }
        }
        TeakGlassMenu(
            visible = filterOpen,
            onDismiss = { onFilterOpenChange(false) },
            anchorInWindow = filterAnchor,
            minWidth = 280.dp,
            maxWidth = 340.dp,
            maxHeight = 520.dp,
        ) {
            FilterMenu(
                value = filters,
                onChange = {
                    onFiltersChange(it)
                    onApplyFilters(it)
                },
            )
        }
    }
}

@Composable
private fun FunnelGlyph() {
    val colors = MiuixTheme.colorScheme
    Canvas(modifier = Modifier.size(17.dp)) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.085f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.18f)
            lineTo(w * 0.58f, h * 0.52f)
            lineTo(w * 0.58f, h * 0.86f)
            lineTo(w * 0.42f, h * 0.74f)
            lineTo(w * 0.42f, h * 0.52f)
            close()
        }
        drawPath(
            path = path,
            color = colors.onBackground,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun FilterMenu(
    value: SearchFilterState,
    onChange: (SearchFilterState) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        FilterSection("排序方式") {
            FilterChips(
                options = listOf("默认排序", "新发布", "播放多", "弹幕多"),
                selected = value.order,
                columns = 2,
                onSelect = { onChange(value.copy(order = it)) },
            )
        }
        FilterSection("发布时间") {
            FilterChips(
                options = listOf("不限", "最近一天", "最近一周", "最近半年"),
                selected = value.published,
                columns = 2,
                onSelect = { onChange(value.copy(published = it)) },
            )
        }
        FilterSection("内容时长") {
            FilterChips(
                options = listOf("不限", "0-10 分钟", "10-30 分钟", "30-60 分钟", "60-120 分钟", "120 分钟+"),
                selected = value.duration,
                columns = 2,
                onSelect = { onChange(value.copy(duration = it)) },
            )
        }
        FilterSection("音画质量", bottom = 4.dp) {
            FilterChips(
                options = listOf("不限", "8K", "4K", "1080P"),
                selected = value.quality,
                columns = 2,
                onSelect = { onChange(value.copy(quality = it)) },
            )
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    bottom: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = bottom)) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
private fun FilterChips(
    options: List<String>,
    selected: Int,
    columns: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        options.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEachIndexed { colIndex, label ->
                    val index = rowIndex * columns + colIndex
                    val active = index == selected
                    // 胶囊底：选中粉底白字，未选中淡灰底，两者都半透明。
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(Capsule())
                            .background(
                                if (active) {
                                    colors.primary.copy(alpha = 0.78f)
                                } else {
                                    colors.onBackground.copy(alpha = 0.06f)
                                },
                            )
                            .clickable(interactionSource = null, indication = null) {
                                onSelect(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (active) Color.White else colors.onBackground,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: (() -> Unit)?) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
private fun HotRow(rank: Int, word: String, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = rank.toString(),
            color = if (rank <= 3) BrandPink else colors.onBackgroundVariant,
            fontSize = 13.sp,
            fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(22.dp),
        )
        Text(
            text = word,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchUserRow(user: SearchUser, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = rememberImageRequest(user.face),
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.surfaceContainerHigh),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                color = colors.onBackground,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.sign.isNotBlank()) {
                Text(
                    text = user.sign,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${formatCount(user.fans)} 粉丝 · ${user.videos} 投稿",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
    }
}

/** 和视频详情页「相关推荐」同一套行布局：150dp 封面 + 两行标题 + 数据 + 底部 UP。 */
@Composable
private fun SearchResultRow(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val coverWidth = 150.dp
    val coverHeight = coverWidth * 9f / 16f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(coverWidth)
                .height(coverHeight)
                .clip(ContinuousCornerShape(10.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(video.cover),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (video.duration > 0) {
                Text(
                    text = formatDuration(video.duration),
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 5.dp, bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).height(coverHeight)) {
            Text(
                text = video.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.padding(start = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricGlyph(play = true, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(video.play),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.width(6.dp))
                MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(video.danmaku),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpBadge()
                Spacer(Modifier.width(5.dp))
                Text(
                    text = video.ownerName,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
