package com.aritxonly.bdfun

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.TeakPageChrome
import com.aritxonly.bdfun.ui.rememberImageRequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.bdfun.data.DynamicItem
import com.aritxonly.bdfun.data.SpaceArticle
import com.aritxonly.bdfun.data.SpaceBangumi
import com.aritxonly.bdfun.data.SpaceInfo
import com.aritxonly.bdfun.data.SpaceSeason
import com.aritxonly.bdfun.data.SpaceShopItem
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.ui.ContinuousCornerShape
import com.aritxonly.bdfun.ui.DynamicCard
import com.aritxonly.bdfun.ui.RelatedRow
import com.aritxonly.bdfun.ui.RegisterScrollTop
import com.aritxonly.bdfun.ui.ResourceGlyph
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.bdfun.ui.TeakIcon
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.teak.common.chrome.TeakTopGlassButton
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import com.aritxonly.bdfun.ui.VideoGridCard
import com.aritxonly.bdfun.ui.formatCount
import com.aritxonly.bdfun.ui.showToast
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

class SpaceActivity : ComponentActivity() {
    override fun onDestroy() {
        super.onDestroy()
        // 真正退出个人主页时释放进程级资料缓存；从主页进视频再返回不会触发。
        if (isFinishing) SpaceCache.map.clear()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.ensure(applicationContext)
        val mid = AppGraph.pendingSpaceMid.value
        if (mid == null || mid <= 0) {
            finish()
            return
        }
        setContent {
            val settings = AppGraph.settings
            var title by remember { mutableStateOf("个人主页") }
            var barBlurFraction by remember { mutableFloatStateOf(0f) }
            // 顶栏图标：头图深色时先白，玻璃一出现再渐变回正常主题色。
            var bannerIsDark by remember { mutableStateOf(true) }
            // 注意：这里在 TeakTheme 外面，不能读 MiuixTheme（会拿到默认浅色）。直接用设置判断。
            val darkTheme = when (settings.darkMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val darkGlyph = teakGlassGlyphColor()
            val pageGlyph = if (darkTheme) Color.White else darkGlyph
            val onBannerGlyph = if (bannerIsDark) Color.White else darkGlyph
            val topGlyph = androidx.compose.ui.graphics.lerp(
                onBannerGlyph,
                pageGlyph,
                barBlurFraction.coerceIn(0f, 1f),
            )
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                title = title,
                barBlurFraction = barBlurFraction,
                topBarGlyphColor = topGlyph,
                topBarActions = {
                    // 和返回键同一套玻璃显隐：滚动到一定距离后才出玻璃背景。
                    TeakTopGlassButton(onClick = {}) {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = "搜索",
                            tint = topGlyph,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    TeakTopGlassButton(onClick = {}) {
                        Icon(
                            imageVector = MiuixIcons.More,
                            contentDescription = "更多",
                            tint = topGlyph,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            ) {
                SpaceScreen(
                    mid = mid,
                    onTitleChange = { if (it.isNotBlank()) title = it },
                    onBarBlurFractionChange = { barBlurFraction = it },
                    onBannerDarkChange = { bannerIsDark = it },
                )
            }
        }
    }
}

private data class CachedSpace(
    val space: SpaceInfo,
    val dynamics: List<DynamicItem>,
    val followed: Boolean,
    val at: Long,
)

/** 个人主页资料 5 分钟进程内缓存：再次进入先出内容，再后台刷新。 */
private object SpaceCache {
    const val TTL = 5 * 60 * 1000L
    val map = mutableMapOf<Long, CachedSpace>()
}

@Composable
private fun SpaceScreen(
    mid: Long,
    onTitleChange: (String) -> Unit,
    onBarBlurFractionChange: (Float) -> Unit,
    onBannerDarkChange: (Boolean) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    androidx.compose.runtime.DisposableEffect(Unit) {
        if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
            com.aritxonly.bdfun.data.LiveCounters.spaceInstances++
        }
        onDispose {
            if (com.aritxonly.bdfun.BuildConfig.DEBUG) {
                com.aritxonly.bdfun.data.LiveCounters.spaceInstances--
            }
        }
    }
    val cached = remember(mid) { SpaceCache.map[mid] }

    var space by remember(mid) { mutableStateOf(cached?.space) }
    var dynamics by remember(mid) { mutableStateOf(cached?.dynamics.orEmpty()) }
    var loading by remember(mid) { mutableStateOf(cached == null) }
    var dynamicOffset by remember(mid) { mutableStateOf("") }
    var dynamicEnd by remember(mid) { mutableStateOf(false) }
    var dynamicLoading by remember(mid) { mutableStateOf(false) }
    // 头图预加载完成前不渲染资料区，避免先闪默认图再换成真人头图。
    var bannerReady by remember(mid) { mutableStateOf(false) }
    var bannerBitmap by remember(mid) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var followed by remember(mid) {
        mutableStateOf(cached?.followed ?: cached?.space?.isFollowed ?: false)
    }
    var followBusy by remember { mutableStateOf(false) }

    val profile = space?.profile
    val isSelf = AppGraph.session.value?.mid == mid

    // ---- 主页/投稿/追番 数据 ----
    var seasons by remember(mid) { mutableStateOf<List<SpaceSeason>>(emptyList()) }
    var allVideos by remember(mid) { mutableStateOf<List<VideoItem>>(emptyList()) }
    var videoPage by remember(mid) { mutableIntStateOf(1) }
    var videoEnd by remember(mid) { mutableStateOf(false) }
    var videoLoading by remember(mid) { mutableStateOf(false) }
    var articles by remember(mid) { mutableStateOf<List<SpaceArticle>>(emptyList()) }
    var articlePage by remember(mid) { mutableIntStateOf(1) }
    var articleEnd by remember(mid) { mutableStateOf(false) }
    var articleLoading by remember(mid) { mutableStateOf(false) }
    var collectionVideos by remember(mid) { mutableStateOf<List<VideoItem>>(emptyList()) }
    var collectionLoading by remember(mid) { mutableStateOf(false) }
    var bangumi by remember(mid) { mutableStateOf<List<SpaceBangumi>>(emptyList()) }
    var bangumiPage by remember(mid) { mutableIntStateOf(1) }
    var bangumiEnd by remember(mid) { mutableStateOf(false) }
    var bangumiLoading by remember(mid) { mutableStateOf(false) }
    var shopItems by remember(mid) { mutableStateOf<List<SpaceShopItem>>(emptyList()) }
    var shopLoading by remember(mid) { mutableStateOf(false) }
    var shopLoaded by remember(mid) { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }
    // 头图和 Tab 固定，只有下面的内容用 pager 左右滑。
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 4 })
    val pagePosition = remember(pagerState) {
        derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }
    // 0 = 视频，1 = 图文，2 = 某个合集/系列
    var submissionMode by remember { mutableIntStateOf(0) }
    var selectedCollection by remember { mutableStateOf<SpaceSeason?>(null) }

    fun loadVideos(page: Int) {
        if (videoLoading) return
        videoLoading = true
        scope.launch {
            val list = runCatching { AppGraph.api.spaceVideos(mid, page) }
                .getOrDefault(emptyList())
            allVideos = if (page <= 1) {
                list
            } else {
                (allVideos + list).distinctBy { it.bvid }
            }
            videoPage = page
            videoEnd = list.isEmpty()
            videoLoading = false
        }
    }

    fun loadArticles(page: Int) {
        if (articleLoading) return
        articleLoading = true
        scope.launch {
            val list = runCatching { AppGraph.api.spaceArticles(mid, page) }
                .getOrDefault(emptyList())
            articles = if (page <= 1) {
                list
            } else {
                (articles + list).distinctBy { it.id }
            }
            articlePage = page
            articleEnd = list.isEmpty()
            articleLoading = false
        }
    }

    fun loadShop() {
        if (shopLoading || shopLoaded) return
        shopLoading = true
        scope.launch {
            shopItems = runCatching { AppGraph.api.spaceShop(mid) }.getOrDefault(emptyList())
            shopLoaded = true
            shopLoading = false
        }
    }

    /** 个人空间「动态」往下翻：cursor 是上一次返回的 offset。 */
    fun loadMoreDynamics() {
        if (dynamicLoading || dynamicEnd) return
        dynamicLoading = true
        scope.launch {
            val page = runCatching { AppGraph.api.userDynamics(mid, dynamicOffset) }.getOrNull()
            if (page != null) {
                dynamics = if (dynamicOffset.isBlank()) {
                    page.items
                } else {
                    (dynamics + page.items).distinctBy { it.id }
                }
                dynamicOffset = page.offset
                dynamicEnd = !page.hasMore || page.offset.isBlank()
            } else {
                // 失败就停住，等用户下拉刷新时再重试，避免滚到底无限请求。
                dynamicEnd = true
            }
            dynamicLoading = false
        }
    }

    fun loadBangumi(page: Int) {        if (bangumiLoading) return
        bangumiLoading = true
        scope.launch {
            val list = runCatching { AppGraph.api.spaceBangumi(mid, page) }
                .getOrDefault(emptyList())
            bangumi = if (page <= 1) {
                list
            } else {
                (bangumi + list).distinctBy { it.seasonId }
            }
            bangumiPage = page
            bangumiEnd = list.isEmpty()
            bangumiLoading = false
        }
    }

    fun loadCollection(collection: SpaceSeason) {
        selectedCollection = collection
        submissionMode = 2
        selectedTab = 2
        scope.launch { pagerState.animateScrollToPage(2) }
        if (collectionLoading) return
        collectionLoading = true
        scope.launch {
            collectionVideos = runCatching {
                AppGraph.api.spaceCollectionArchives(mid, collection)
            }.getOrDefault(emptyList())
            collectionLoading = false
        }
    }

    LaunchedEffect(mid, isSelf) {
        if (cached == null) loading = true
        coroutineScope {
            val spaceDeferred = async { runCatching { AppGraph.api.space(mid) }.getOrNull() }
            val seasonsDeferred = async {
                runCatching { AppGraph.api.spaceSeasonsSeries(mid) }.getOrDefault(emptyList())
            }
            val dynamicsDeferred = async {
                runCatching { AppGraph.api.userDynamics(mid) }.getOrNull()
            }
            val relationDeferred = async {
                if (!isSelf && AppGraph.api.isLoggedIn) {
                    runCatching { AppGraph.api.relation(mid) }.getOrNull()
                } else {
                    null
                }
            }
            val videosDeferred = async {
                runCatching { AppGraph.api.spaceVideos(mid, 1) }.getOrDefault(emptyList())
            }
            val fetched = spaceDeferred.await()
            if (fetched != null) space = fetched
            // 先把头图加载出来让用户先看到，剩下的资料/投稿/动态继续并行等待。
            val bannerUrl = (fetched ?: space)?.profile?.banner.orEmpty()
            val bitmap = if (bannerUrl.isNotBlank()) {
                runCatching { loadSpaceBannerBitmap(context, bannerUrl) }.getOrNull()
            } else {
                null
            }
            bannerBitmap = bitmap
            if (bitmap != null && !bitmap.isRecycled) {
                onBannerDarkChange(bitmapLuminance(bitmap) < 0.5f)
            }
            bannerReady = true
            androidx.compose.runtime.withFrameNanos { }
            val fetchedSeasons = seasonsDeferred.await()
            seasons = fetchedSeasons
            val fetchedDynamics = dynamicsDeferred.await()
            if (fetchedDynamics != null) {
                dynamics = fetchedDynamics.items
                dynamicOffset = fetchedDynamics.offset
                dynamicEnd = !fetchedDynamics.hasMore || fetchedDynamics.offset.isBlank()
            }
            val attribute = relationDeferred.await()
            followed = when {
                isSelf -> false
                attribute != null -> attribute == 2 || attribute == 6
                fetched != null -> fetched.isFollowed
                cached != null -> cached.followed
                else -> false
            }
            // 空间接口偶尔不返回投稿列表，这时并行的 spaceVideos 直接顶上。
            if (fetched != null && fetched.videos.isNotEmpty()) {
                allVideos = fetched.videos
                videoPage = 1
                videoEnd = fetched.videos.size < 30
            } else {
                val firstPage = videosDeferred.await()
                allVideos = firstPage
                videoPage = 1
                videoEnd = firstPage.isEmpty()
            }
            // 头图已经在前面先加载完成；这里只负责把其余内容一次性放出来。
            loading = false
            val resolved = fetched ?: space
            if (resolved != null) {
                SpaceCache.map[mid] = CachedSpace(
                    space = resolved,
                    dynamics = fetchedDynamics?.items ?: dynamics,
                    followed = followed,
                    at = System.currentTimeMillis(),
                )
            }
        }
    }

    LaunchedEffect(profile?.name) {
        onTitleChange(profile?.name.orEmpty().ifBlank { "个人主页" })
    }

    // 切到对应 tab 时补拉数据。
    LaunchedEffect(selectedTab, submissionMode) {
        when {
            selectedTab == 2 && submissionMode == 0 && allVideos.isEmpty() -> loadVideos(1)
            selectedTab == 2 && submissionMode == 1 && articles.isEmpty() -> loadArticles(1)
            selectedTab == 3 && shopItems.isEmpty() -> loadShop()
        }
    }

    // 每个 Tab 保留自己的滚动位置；这样来回切换不会每次都回到顶部。
    val tabListStates = remember {
        List(4) { androidx.compose.foundation.lazy.LazyListState() }
    }
    val listState = tabListStates[selectedTab.coerceIn(0, 3)]
    RegisterScrollTop(listState, "space")

    // 头部高度 / 收起位移：先正常往上滚，滚到 Tab 停在顶部栏下方时不再继续收。
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var headerCollapsePx by remember { mutableFloatStateOf(0f) }
    val blurDensity = LocalDensity.current
    val bannerHeightPx = with(blurDensity) {
        (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp * 0.625f).toPx()
    }
    val statusBarPx = with(blurDensity) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }
    // Tab 最终固定在“状态栏 + 顶部按钮那一行”的位置。
    // 有且只有 Tab 固定：头像 / 等级 / 简介会完整滚出屏幕，不再留一截。
    val pinHeightPx = statusBarPx + with(blurDensity) { 56.dp.toPx() }
    // 资料列自己用 trimBottom 收掉视觉上移留下的空白，这里不再额外扣。
    val headerVisualTrimPx = 0f
    // Tab 与 UID 的视觉间距。
    val tabGapPx = with(blurDensity) { 7.dp.toPx() }

    // 头部整块都可以被移出屏幕；Tab 现在是内容的一部分，会跟着一起滚走。
    val headerMaxCollapsePx = headerHeightPx.toFloat()
    LaunchedEffect(headerMaxCollapsePx) {
        if (headerCollapsePx > headerMaxCollapsePx) {
            headerCollapsePx = headerMaxCollapsePx
        }
    }

    val barBlurProgress by remember(bannerHeightPx) {
        derivedStateOf {
            (headerCollapsePx / (bannerHeightPx * 0.5f)).coerceIn(0f, 1f)
        }
    }
    androidx.compose.runtime.SideEffect { onBarBlurFractionChange(barBlurProgress) }

    LaunchedEffect(
        headerCollapsePx,
        headerHeightPx,
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset,
    ) {
        val maxCollapse = headerMaxCollapsePx
        val headerDone = maxCollapse > 0f && headerCollapsePx >= maxCollapse - 1f
        val extra = if (headerDone) listState.firstVisibleItemScrollOffset.toFloat() else 0f
        val index = if (headerDone && listState.firstVisibleItemIndex > 0) 1 else 0
        TeakPageChrome.state.update(index, headerCollapsePx + extra)
    }

    // 1 = 新页从右边进，-1 = 从左边进。点 Tab 和左右滑都走同一套。
    var tabDirection by remember { mutableIntStateOf(1) }

    fun selectTab(index: Int) {
        if (index !in 0..3 || index == selectedTab) return
        selectedTab = index
        scope.launch { pagerState.animateScrollToPage(index) }
    }

    // 页面切换改为和首页 / 动态页一样的整页横向滑动（AnimatedContent 同时保留旧页和新页）。
    val tabEnterModifier = Modifier

    // 横划只翻一页；竖向滑动完全不拦截。
    val viewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val swipeSlop = viewConfig.touchSlop
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val swipeModifier = Modifier.pointerInput(selectedTab) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var totalX = 0f
            var totalY = 0f
            var horizontal: Boolean? = null
            var childTookIt = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.isConsumed) {
                    // 横向列表（合集、投稿筛选）已经接走了这次拖动，不当作翻页。
                    childTookIt = true
                    break
                }
                totalX += change.position.x - change.previousPosition.x
                totalY += change.position.y - change.previousPosition.y
                if (horizontal == null &&
                    (kotlin.math.abs(totalX) > swipeSlop || kotlin.math.abs(totalY) > swipeSlop)
                ) {
                    horizontal = kotlin.math.abs(totalX) > kotlin.math.abs(totalY) * 1.3f
                }
                if (horizontal == true) change.consume()
                if (!change.pressed) break
            }
            if (!childTookIt && horizontal == true &&
                kotlin.math.abs(totalX) >= swipeThreshold
            ) {
                val next = if (totalX < 0f) selectedTab + 1 else selectedTab - 1
                selectTab(next)
            }
        }
    }

    // 滚到底部时翻页。
    LaunchedEffect(listState, selectedTab, submissionMode) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
        }.collect { (last, total) ->
            if (last < 0 || total <= 0 || last < total - 3) return@collect
            when {
                selectedTab == 1 && !dynamicLoading && !dynamicEnd ->
                    loadMoreDynamics()
                selectedTab == 2 && submissionMode == 0 && !videoLoading && !videoEnd ->
                    loadVideos(videoPage + 1)
                selectedTab == 2 && submissionMode == 1 && !articleLoading && !articleEnd ->
                    loadArticles(articlePage + 1)
                selectedTab == 3 && !bangumiLoading && !bangumiEnd ->
                    loadBangumi(bangumiPage + 1)
            }
        }
    }

    val headerContent: @Composable () -> Unit = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SpaceBanner(bitmap = bannerBitmap)
                    // 白色面板从头顶边开始，不做圆角矩形；头像用负 offset 盖到头上。
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background)
                            .padding(top = 12.dp, bottom = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            // 头像放大，压在头图上；右侧加一颗主题色小圆点做点缀。
                            Box(modifier = Modifier.offset(y = (-38).dp).size(96.dp)) {
                                AsyncImage(
                                    model = rememberImageRequest(profile?.face.orEmpty()),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(5.dp, colors.background, CircleShape),
                                )
                                // 头像右侧的浅色弧线 + 小圆点组合成点缀，
                                // 箭头不画完：弧线穿过小圆中心后再多留出一小段。
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(94.dp),
                                ) {
                                    val stroke = 3.dp.toPx()
                                    val arcColor = colors.primary.copy(alpha = 0.45f)
                                    // 小圆在约 35° 处，两边各留约 10° 的空隙，
                                    // 弧线被小圆断成两段，两段都用圆角收尾。
                                    drawArc(
                                        color = arcColor,
                                        startAngle = -90f,
                                        sweepAngle = 115f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = stroke,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        ),
                                    )
                                    drawArc(
                                        color = arcColor,
                                        startAngle = 45f,
                                        sweepAngle = 15f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = stroke,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        ),
                                    )
                                }
                                // 头像旁不再显示大会员角标，大会员只在 LV 旁边显示。
                                // 点缀：浅粉色弧线，小粉色圆点正好压在这条弧线的中段。
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = 38.5.dp, y = 27.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.45f)),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .offset(y = (-4).dp),
                            ) {
                                // 粉丝 / 关注 / 获赞：和其他圆角卡片同款白色底。
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(colors.surface)
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SpaceStat("粉丝", profile?.follower ?: 0, Modifier.weight(1f))
                                    SpaceStat("关注", profile?.following ?: 0, Modifier.weight(1f))
                                    SpaceStat("获赞", profile?.likes ?: 0, Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.weight(1f))
                                    MessageCircleButton(
                                        onClick = {
                                            context.startActivity(
                                                android.content.Intent(
                                                    context,
                                                    PrivateChatActivity::class.java,
                                                )
                                                    .putExtra("talker_id", mid)
                                                    .putExtra("session_type", 1)
                                                    .putExtra("name", profile?.name.orEmpty())
                                                    .putExtra("avatar", profile?.face.orEmpty()),
                                            )
                                        },
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    FollowCapsule(
                                        isSelf = isSelf,
                                        followed = followed,
                                        busy = followBusy,
                                        modifier = Modifier.width(124.dp),
                                        onClick = {
                                            if (isSelf) {
                                                context.showToast("编辑资料暂未接入")
                                                return@FollowCapsule
                                            }
                                            if (!AppGraph.api.isLoggedIn) {
                                                context.showToast("请先登录")
                                                return@FollowCapsule
                                            }
                                            if (followBusy) return@FollowCapsule
                                            val next = !followed
                                            followed = next
                                            followBusy = true
                                            scope.launch {
                                                val result = runCatching {
                                                    AppGraph.api.followUser(mid, next)
                                                }
                                                followBusy = false
                                                if (result.isSuccess) {
                                                    SpaceCache.map[mid]?.let {
                                                        SpaceCache.map[mid] = it.copy(
                                                            followed = next,
                                                            at = System.currentTimeMillis(),
                                                        )
                                                    }
                                                    context.showToast(
                                                        if (next) "关注成功" else "已取消关注",
                                                    )
                                                } else {
                                                    followed = !next
                                                    context.showToast(
                                                        result.exceptionOrNull()?.message ?: "操作失败",
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .trimBottom(36.dp)
                                .padding(start = 16.dp, end = 16.dp, top = 0.dp),
                        ) {
                            val rawName = profile?.name.orEmpty().ifBlank { " " }
                            val displayName = remember(rawName) { breakProfileName(rawName) }
                            Text(
                                text = displayName,
                                color = colors.onBackground,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                // 右边给关注胶囊让位，避免长名字压到按钮上。
                                modifier = Modifier.fillMaxWidth().padding(end = 128.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(
                                        id = when ((profile?.level ?: 0).coerceIn(1, 6)) {
                                            1 -> R.drawable.bili_lv1
                                            2 -> R.drawable.bili_lv2
                                            3 -> R.drawable.bili_lv3
                                            4 -> R.drawable.bili_lv4
                                            5 -> R.drawable.bili_lv5
                                            else -> R.drawable.bili_lv6
                                        },
                                    ),
                                    contentDescription = "LV${profile?.level ?: 0}",
                                    modifier = Modifier.size(width = 26.dp, height = 12.dp),
                                )
                                // 大会员等级胶囊和「我的」页保持同一套样式：
                                // 有大会员用品牌粉底白字；没有大会员也显示灰色的「大会员」。
                                // 年度 / 十年 / 百年等文字来自接口 vip.label.text。
                                Spacer(Modifier.width(5.dp))
                                val isVip = profile?.vip == true
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isVip) BrandPink
                                            else colors.onBackground.copy(alpha = 0.10f),
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                ) {
                                    Text(
                                        text = if (isVip) {
                                            profile?.vipLabel.orEmpty().ifBlank { "大会员" }
                                        } else {
                                            "大会员"
                                        },
                                        color = if (isVip) Color.White else colors.onBackgroundVariant,
                                        fontSize = 8.5.sp,
                                        lineHeight = 11.sp,
                                        maxLines = 1,
                                    )
                                }
                                if (!profile?.fansMedalName.isNullOrBlank()) {
                                    Spacer(Modifier.width(5.dp))
                                    val medalRgb = profile?.fansMedalColor ?: 0
                                    val medalColor = if (medalRgb != 0) {
                                        Color(0xFF000000L or (medalRgb.toLong() and 0xFFFFFFL))
                                    } else {
                                        colors.primary
                                    }
                                    // 和视频卡片里的 UP 小胶囊同一套样式：
                                    // 半透明底色 + 同色文字，四周只留刚好贴住文字的留白。
                                    Box(
                                        modifier = Modifier
                                            .clip(com.kyant.shapes.Capsule())
                                            .background(medalColor.copy(alpha = 0.16f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            text = "${profile?.fansMedalName} Lv.${profile?.fansMedalLevel}",
                                            color = medalColor,
                                            fontSize = 9.sp,
                                            lineHeight = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }

                            val verify = listOfNotNull(
                                profile?.officialTitle?.takeIf { it.isNotBlank() },
                                profile?.officialDesc?.takeIf { it.isNotBlank() },
                            ).joinToString(" ")
                            if (verify.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "✔",
                                        color = Color(0xFF3B82F6),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = verify,
                                        color = colors.onBackgroundVariant,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            ExpandableSign(
                                text = profile?.sign.orEmpty().ifBlank { "这个人很神秘，什么都没有写" },
                            )

                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(interactionSource = null, indication = null) {
                                        runCatching {
                                            context.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(
                                                        "https://space.bilibili.com/$mid/upower/rank",
                                                    ),
                                                ),
                                            )
                                        }.onFailure { context.showToast("无法打开充电页") }
                                    }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ChargeGlyph(size = 14.dp)
                                Spacer(Modifier.width(4.dp))
                                val charging = profile?.chargingCount ?: 0L
                                Text(
                                    text = "${formatCountNoDecimal(charging)}人已为其充电",
                                    color = colors.onBackgroundVariant,
                                    fontSize = 11.5.sp,
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "UID：$mid",
                                color = colors.onBackgroundVariant,
                                fontSize = 10.5.sp,
                            )
                        }
                        // Tab 作为资料区的一部分，跟着上下滚走；不放进横向 Pager，
                        // 所以左右切页时它不会跟着横移。
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 9.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TeakPagerTabs(
                                options = listOf("主页", "动态", "投稿", "小店"),
                                selected = pagerState.currentPage,
                                onSelect = { selectTab(it) },
                                position = pagePosition,
                                backdrop = null,
                            )
                        }
                    }
                }
    }

    // pager 和 selectedTab 双向同步：左右滑或点 Tab 都会更新数据加载状态。
    LaunchedEffect(pagerState) {
        // 等滑动真正停下来再改 selectedTab，避免划过中间页时误触发那一页的加载。
        snapshotFlow { pagerState.settledPage }.collect { selectedTab = it }
    }
    // 内容列表往上滑时先收起头部，收到 Tab 固定位置后，剩下的位移才交给列表。
    val headerScrollConnection = remember(headerMaxCollapsePx) {
        val maxCollapse = headerMaxCollapsePx
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y < 0f && headerCollapsePx < maxCollapse) {
                    val consume = (-available.y).coerceAtMost(maxCollapse - headerCollapsePx)
                    headerCollapsePx += consume
                    return androidx.compose.ui.geometry.Offset(0f, -consume)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (available.y > 0f && headerCollapsePx > 0f) {
                    val consume = available.y.coerceAtMost(headerCollapsePx)
                    headerCollapsePx -= consume
                    return androidx.compose.ui.geometry.Offset(0f, consume)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    // 头图 / 上方空白区域也能上下拖动整页：先收头部，收到 Tab 固定位置后再滚当前列表。
    fun applyPageDrag(dragY: Float) {
        val maxCollapse = headerMaxCollapsePx
        if (dragY < 0f) {
            val collapse = (-dragY).coerceAtMost(
                (maxCollapse - headerCollapsePx).coerceAtLeast(0f),
            )
            if (collapse > 0f) headerCollapsePx += collapse
            val remain = -dragY - collapse
            if (remain > 0f) listState.dispatchRawDelta(remain)
        } else {
            val atListTop = listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset == 0
            if (headerCollapsePx > 0f && atListTop) {
                headerCollapsePx = (headerCollapsePx - dragY).coerceAtLeast(0f)
            } else {
                listState.dispatchRawDelta(-dragY)
            }
        }
    }

    if (loading || !bannerReady) {
        Column(modifier = Modifier.fillMaxSize()) {
            bannerBitmap?.let { SpaceBanner(bitmap = it) }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TeakLoading(size = 44.dp)
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 固定头部：只上下收，不跟着 Tab 左右移动。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .collapseHeaderContainer(
                        headerHeightPx,
                        headerCollapsePx,
                        headerVisualTrimPx.roundToInt(),
                    )
                    .pointerInput(selectedTab, headerMaxCollapsePx) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            applyPageDrag(dragAmount)
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { headerHeightPx = it.height }
                        .graphicsLayer { translationY = -headerCollapsePx },
                ) {
                    headerContent()
                }
            }

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                // 相邻页也保持组合，滑过去时内容已经就位。
                beyondViewportPageCount = 1,
                overscrollEffect = null,
            ) { page ->
                LazyColumn(
                    state = tabListStates[page.coerceIn(0, 3)],
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(headerScrollConnection),
                    // 「主页」内容本来就短，再留底部空白会被误认为还能继续滑一截。
                    contentPadding = PaddingValues(
                        top = 0.dp,
                        bottom = if (page == 0) 0.dp else 90.dp,
                    ),
                    overscrollEffect = null,
                ) {
            when (page) {
                0 -> {
                    item(key = "home-videos-title") {
                        SpaceSectionHeader(
                            title = "视频（${(space?.videoCount ?: 0).takeIf { it > 0 } ?: allVideos.size}）",
                            onMore = {
                                submissionMode = 0
                                selectedCollection = null
                                selectTab(2)
                            },
                            modifier = Modifier
                                .then(tabEnterModifier)
                                .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                        )
                    }
                    item(key = "home-videos") {
                        Column(
                            modifier = Modifier
                                .then(tabEnterModifier)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            val shown = allVideos.take(4)
                            for (row in 0..1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    for (col in 0..1) {
                                        val index = row * 2 + col
                                        val video = shown.getOrNull(index)
                                        if (video == null) {
                                            Spacer(Modifier.weight(1f))
                                        } else {
                                            VideoGridCard(
                                                video = video,
                                                onClick = {
                                                    TeakNavigator.openVideo(context, video)
                                                },
                                                modifier = Modifier.weight(1f),
                                                showOwner = false,
                                            )
                                        }
                                    }
                                }
                                if (row == 0) Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                    if (seasons.isNotEmpty()) {
                        item(key = "home-collections-title") {
                            SpaceSectionHeader(
                                title = "合集和系列",
                                onMore = {
                                    submissionMode = 3
                                    selectedCollection = null
                                    selectTab(2)
                                },
                                modifier = Modifier
                                    .then(tabEnterModifier)
                                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
                            )
                        }
                        item(key = "home-collections") {
                            Column(
                                modifier = Modifier
                                    .then(tabEnterModifier)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                val shown = seasons.take(4)
                                for (row in 0..1) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        for (col in 0..1) {
                                            val index = row * 2 + col
                                            val season = shown.getOrNull(index)
                                            if (season == null) {
                                                Spacer(Modifier.weight(1f))
                                            } else {
                                                SpaceCollectionGridCard(
                                                    season = season,
                                                    onClick = { loadCollection(season) },
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                        }
                                    }
                                    if (row == 0) Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }

                1 -> {
                    if (dynamics.isEmpty() && loading) {
                        item(key = "dynamic-loading") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                TeakLoading(size = 34.dp)
                            }
                        }
                    } else if (dynamics.isEmpty()) {
                        item(key = "dynamic-empty") { SpaceEmpty("暂无动态") }
                    } else {
                        items(dynamics, key = { "dynamic-${it.id}" }) { item ->
                            DynamicCard(
                                item = item,
                                liked = false,
                                onOpen = { TeakNavigator.openDynamic(context, item) },
                                onOpenVideo = { TeakNavigator.openVideo(context, it) },
                                onLike = {},
                                onComment = { TeakNavigator.openDynamic(context, item) },
                                onRepost = {},
                                modifier = Modifier
                                    .then(tabEnterModifier)
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                            )
                        }
                        if (dynamicLoading) {
                            item(key = "dynamic-loading-more") { SpaceLoadingRow() }
                        }
                    }
                }

                2 -> {
                    item(key = "submission-chips") {
                        LazyRow(
                            modifier = tabEnterModifier,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item {
                                SubmissionChip(
                                    text = "视频",
                                    selected = submissionMode == 0,
                                    onClick = { submissionMode = 0 },
                                )
                            }
                            item {
                                SubmissionChip(
                                    text = "图文",
                                    selected = submissionMode == 1,
                                    onClick = {
                                        submissionMode = 1
                                        selectedCollection = null
                                        if (articles.isEmpty()) loadArticles(1)
                                    },
                                )
                            }
                            items(seasons) { s ->
                                SubmissionChip(
                                    text = s.title,
                                    selected = submissionMode == 2 && selectedCollection?.id == s.id,
                                    onClick = { loadCollection(s) },
                                )
                            }
                            item {
                                SubmissionChip(
                                    text = "合集与系列",
                                    selected = submissionMode == 3,
                                    onClick = {
                                        submissionMode = 3
                                        selectedCollection = null
                                    },
                                )
                            }
                        }
                    }

                    when {
                        submissionMode == 1 -> {
                            items(articles, key = { "article-${it.id}" }) { article ->
                                SpaceArticleRow(
                                    article = article,
                                    modifier = tabEnterModifier,
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(
                                                        "https://www.bilibili.com/read/cv${article.id}",
                                                    ),
                                                ),
                                            )
                                        }.onFailure { context.showToast("无法打开图文") }
                                    },
                                )
                            }
                            if (articleLoading) {
                                item(key = "article-loading") { SpaceLoadingRow() }
                            }
                        }
                        submissionMode == 3 -> {
                            if (seasons.isEmpty()) {
                                item(key = "all-collections-empty") {
                                    SpaceEmpty("还没有合集")
                                }
                            } else {
                                items(
                                    items = seasons.chunked(2),
                                    key = { row ->
                                        row.joinToString("-") {
                                            "${if (it.isSeries) "series" else "season"}-${it.id}"
                                        }
                                    },
                                ) { row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        row.forEach { season ->
                                            SpaceCollectionGridCard(
                                                season = season,
                                                onClick = { loadCollection(season) },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        if (row.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        submissionMode == 2 -> {
                            // 每一条都作为独立的 Lazy 项，不再把几十条一起塞进一个 Column。
                            // 这样滚动时只组合屏幕内可见的几行，投稿页不会一次卡住。
                            itemsIndexed(
                                items = collectionVideos,
                                key = { _, video -> "collection-${video.bvid}" },
                            ) { index, video ->
                                SpaceSubmissionCard(
                                    index = index,
                                    isLast = index == collectionVideos.lastIndex,
                                    modifier = tabEnterModifier,
                                ) {
                                    RelatedRow(
                                        item = video,
                                        metaLabel = com.aritxonly.bdfun.ui.formatDate(
                                            video.pubdate,
                                        ),
                                        onClick = {
                                            TeakNavigator.openVideo(context, video)
                                        },
                                    )
                                }
                            }
                            if (collectionLoading) {
                                item(key = "collection-loading") { SpaceLoadingRow() }
                            } else if (collectionVideos.isEmpty()) {
                                item(key = "collection-empty") { SpaceEmpty("合集里还没有视频") }
                            }
                        }
                        else -> {
                            itemsIndexed(
                                items = allVideos,
                                key = { _, video -> "video-${video.bvid}" },
                            ) { index, video ->
                                SpaceSubmissionCard(
                                    index = index,
                                    isLast = index == allVideos.lastIndex,
                                    modifier = tabEnterModifier,
                                ) {
                                    RelatedRow(
                                        item = video,
                                        metaLabel = com.aritxonly.bdfun.ui.formatDate(
                                            video.pubdate,
                                        ),
                                        onClick = {
                                            TeakNavigator.openVideo(context, video)
                                        },
                                    )
                                }
                            }
                            if (videoLoading) {
                                item(key = "video-loading") { SpaceLoadingRow() }
                            } else if (allVideos.isEmpty()) {
                                item(key = "video-empty") { SpaceEmpty("还没有投稿") }
                            }
                        }
                    }
                }

                else -> {
                    if (shopItems.isEmpty() && shopLoading) {
                        item(key = "shop-loading") { SpaceLoadingRow() }
                    } else if (shopItems.isEmpty()) {
                        item(key = "shop-empty") { SpaceEmpty("还没有小店商品") }
                    } else {
                        items(
                            items = shopItems.chunked(2),
                            key = { row -> "shop-${row.first().url}" },
                        ) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                row.forEach { item ->
                                    SpaceShopCard(
                                        item = item,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (item.url.isNotBlank()) {
                                                runCatching {
                                                    context.startActivity(
                                                        android.content.Intent(
                                                            android.content.Intent.ACTION_VIEW,
                                                            android.net.Uri.parse(item.url),
                                                        ),
                                                    )
                                                }.onFailure { context.showToast("无法打开商品") }
                                            }
                                        },
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        }
        }
    }
}

// ---------------------------------------------------------------- 头部组件

@Composable
private fun SpaceBanner(bitmap: android.graphics.Bitmap?) {
    val dark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 参考图里的头图约 960×600，也就是 16:10；高度跟着屏幕宽度走。
            .aspectRatio(16f / 10f)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // PiliPlus 同款头图滤镜：浅色提亮，深色压暗。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (dark) Color.Black.copy(alpha = 0.35f)
                    else Color.White.copy(alpha = 0.22f),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                    ),
                ),
        )
        // 顶部压暗：大标题和状态栏图标叠在图上时仍然看得清。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.42f),
                        0.42f to Color.Black.copy(alpha = 0.12f),
                        0.62f to Color.Transparent,
                    ),
                ),
        )
    }
}

@Composable
private fun MessageCircleButton(onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, colors.onBackground.copy(alpha = 0.08f), CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.Messages,
            contentDescription = "私信",
            tint = BrandPink,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ChargeGlyph(size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.58f, 0f)
            lineTo(w * 0.12f, h * 0.56f)
            lineTo(w * 0.45f, h * 0.56f)
            lineTo(w * 0.34f, h)
            lineTo(w * 0.90f, h * 0.40f)
            lineTo(w * 0.55f, h * 0.40f)
            close()
        }
        drawPath(path, Color(0xFFFFC107))
    }
}

@Composable
private fun FollowCapsule(
    isSelf: Boolean,
    followed: Boolean,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val shape = com.kyant.shapes.Capsule()
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(if (followed) colors.surfaceContainerHigh else colors.primary)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.45f),
                shape = shape,
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                isSelf -> "编辑资料"
                followed -> "已关注"
                busy -> "处理中…"
                else -> "+ 关注"
            },
            color = if (followed) colors.onBackgroundVariant else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ExpandableSign(text: String) {
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = androidx.compose.ui.text.TextStyle(
        color = colors.onBackgroundVariant,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    )
    var expanded by remember(text) { mutableStateOf(false) }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 260,
            easing = androidx.compose.animation.core.FastOutSlowInEasing,
        ),
        label = "space-sign-expand",
    )
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 右侧给展开图标留出位置，文字不会跑到图标下面。
        val widthPx = with(density) { (maxWidth - 24.dp).toPx() }
            .toInt()
            .coerceAtLeast(1)
        val fullLayout = measurer.measure(
            text = androidx.compose.ui.text.AnnotatedString(text),
            style = style,
            maxLines = Int.MAX_VALUE,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
        )
        val oneLineLayout = measurer.measure(
            text = androidx.compose.ui.text.AnnotatedString(text),
            style = style,
            maxLines = 1,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
        )
        val collapsedPx = oneLineLayout.size.height.toFloat()
        val fullPx = fullLayout.size.height.toFloat()
        val expandable = fullPx > collapsedPx + 1f
        val targetPx = collapsedPx + (fullPx - collapsedPx) * progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { targetPx.toDp() })
                .clipToBounds(),
        ) {
            Text(
                text = text,
                style = style,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier.padding(end = 24.dp),
            )
        }
        if (expandable) {
            // 和视频页简介一样：只在第一行右上角放一个箭头，点一下平滑展开/收起。
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        expanded = !expanded
                    },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        val w = size.width
                        val h = size.height
                        moveTo(w * 0.18f, h * 0.38f)
                        lineTo(w * 0.5f, h * 0.68f)
                        lineTo(w * 0.82f, h * 0.38f)
                    }
                    rotate(degrees = if (expanded) 180f else 0f) {
                        drawPath(
                            path,
                            color = colors.onBackgroundVariant,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.4.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (onMore != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(interactionSource = null, indication = null, onClick = onMore)
                    .padding(start = 6.dp, end = 3.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "查看更多",
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(2.dp))
                androidx.compose.foundation.Canvas(modifier = Modifier.size(7.dp, 10.dp)) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.30f, size.height * 0.18f)
                        lineTo(size.width * 0.72f, size.height * 0.5f)
                        lineTo(size.width * 0.30f, size.height * 0.82f)
                    }
                    drawPath(
                        path = path,
                        color = colors.onBackgroundVariant,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.4.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}

/** 合集 / 系列卡片：和上面的视频卡片同一套排版，右下角把时长换成合集内视频数。 */
@Composable
private fun SpaceCollectionGridCard(
    season: SpaceSeason,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .clip(com.aritxonly.bdfun.ui.FeedCardShape)
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(colors.surfaceContainerHigh),
        ) {
            if (season.cover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(season.cover, crossfade = false),
                    contentDescription = season.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x4D000000)),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${formatCount(season.total.toLong())} 个视频",
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                )
            }
        }
        Text(
            text = season.title,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun SubmissionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primary else colors.surface)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpaceArticleRow(
    article: SpaceArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(ContinuousCornerShape(10.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            if (article.cover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(article.cover, crossfade = false),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatCount(article.view)} 阅读",
                color = colors.onBackgroundVariant,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun SpaceShopCard(
    item: SpaceShopItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(colors.surfaceContainerHigh),
        ) {
            if (item.cover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(item.cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.price.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = item.price,
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            if (item.source.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.source,
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SpaceBangumiRow(
    item: SpaceBangumi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(66.dp)
                .height(88.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            if (item.cover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(item.cover, crossfade = false),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 13.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            val meta = buildString {
                if (item.total > 0) append("全 ${item.total} 话")
                if (item.progress.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(item.progress)
                }
            }
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    color = colors.onBackgroundVariant,
                    fontSize = 10.5.sp,
                )
            }
        }
    }
}

/**
 * 投稿列表里的一条：整组仍然是一张连续的白卡，但拆成逐条 Lazy 项后，
 * 首条圆上角、末条圆下角、中间直角，视觉和原来一整块 Column 一样。
 */
@Composable
private fun SpaceSubmissionCard(
    index: Int,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val isFirst = index == 0
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 16.dp else 0.dp,
        topEnd = if (isFirst) 16.dp else 0.dp,
        bottomStart = if (isLast) 16.dp else 0.dp,
        bottomEnd = if (isLast) 16.dp else 0.dp,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                // 第一条视频和圆角卡片上边的距离再增加 2dp。
                top = if (isFirst) 15.dp else 0.dp,
                bottom = if (isLast) 6.dp else 0.dp,
            )
            .clip(shape)
            .background(colors.surface),
    ) {
        content()
    }
}

@Composable
private fun SpaceLoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        TeakLoading(size = 34.dp)
    }
}

@Composable
private fun SpaceEmpty(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
        )
    }
}

/** 名字显示宽度：中文/符号算 1 个字，两个英文字母算 1 个字。 */
private fun profileNameUnits(text: String): Int {
    var units = 0
    var latin = 0
    for (ch in text) {
        val isLatin = ch in 'A'..'Z' || ch in 'a'..'z'
        if (isLatin) {
            latin++
            if (latin == 2) {
                units++
                latin = 0
            }
        } else {
            if (latin > 0) {
                units++
                latin = 0
            }
            units++
        }
    }
    if (latin > 0) units++
    return units
}

/** 超过 7 个字的名字在第七个字后换行，剩下的放到第二排。 */
private fun breakProfileName(text: String): String {
    if (profileNameUnits(text) <= 7) return text
    var units = 0
    var latin = 0
    var index = 0
    while (index < text.length) {
        val ch = text[index]
        val isLatin = ch in 'A'..'Z' || ch in 'a'..'z'
        if (isLatin) {
            latin++
            if (latin == 2) {
                units++
                latin = 0
            }
        } else {
            if (latin > 0) {
                units++
                latin = 0
            }
            units++
        }
        index++
        if (units >= 7) break
    }
    if (index >= text.length) return text
    return text.substring(0, index) + "\n" + text.substring(index)
}

/** 紧凑数字但不带小数点：7.3 万 → 7万。 */
private fun formatCountNoDecimal(value: Long): String =
    formatCount(value)
        .replace(Regex("\\.\\d+\\s*"), "")
        .replace(" ", "")

@Composable
private fun SpaceStat(label: String, value: Long, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier.offset(y = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            // 粉丝 / 关注 / 获赞不显示小数，例如 7.3 万 → 7万。
            text = formatCountNoDecimal(value),
            color = colors.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            color = colors.onBackgroundVariant,
            fontSize = 12.sp,
        )
    }
}

/** 把内容视觉上移，同时把布局底部多余的空白一起收掉，避免在 Tab 后面留色块。 */
private fun Modifier.trimBottom(trim: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val trimPx = trim.roundToPx()
        val height = (placeable.height - trimPx).coerceAtLeast(0)
        layout(placeable.width, height) {
            placeable.place(0, -trimPx)
        }
    }

/** 头部容器：测量到高度后，按收起位移把占位一起缩短；内容本身由 graphicsLayer 平移。 */
private fun Modifier.collapseHeaderContainer(
    measuredPx: Int,
    collapsePx: Float,
    trimPx: Int = 0,
): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val height = if (measuredPx > 0) {
            (measuredPx - trimPx - collapsePx).roundToInt().coerceAtLeast(0)
        } else {
            placeable.height
        }
        layout(placeable.width, height) {
            placeable.place(0, 0)
        }
    }

/** B ????????????? top_photo ????? */
private const val DEFAULT_SPACE_BANNER =
    "https://i0.hdslb.com/bfs/space/cb1c3ef50e22b6096fde67febe863494caefebad.png"

/** 头图单独加载成 Bitmap，不走长期缓存；每次进入都重新拿。 */
private suspend fun loadSpaceBannerBitmap(
    context: android.content.Context,
    url: String,
): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(1080, 675)
        .allowHardware(false)
        .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
        .diskCachePolicy(coil.request.CachePolicy.DISABLED)
        .build()
    val result = coil.Coil.imageLoader(context).execute(request)
    (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        ?.takeIf { !it.isRecycled }
}

/** 头图缩到 8×8 取平均亮度，决定顶栏图标先显示白还是黑。 */
private fun bitmapLuminance(bitmap: android.graphics.Bitmap): Float {
    val small = android.graphics.Bitmap.createScaledBitmap(bitmap, 8, 8, false)
    val pixels = IntArray(64)
    small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
    if (small !== bitmap) small.recycle()
    var r = 0L
    var g = 0L
    var b = 0L
    pixels.forEach { p ->
        r += (p shr 16) and 0xFF
        g += (p shr 8) and 0xFF
        b += p and 0xFF
    }
    val n = pixels.size
    return Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt()).luminance()
}

