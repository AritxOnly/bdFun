package com.aritxonly.bdfun.ui

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.centeredListWidth
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底栏搜索页。
 *
 * 与首页顶栏那套 [SearchOverlay] 是两条独立入口：这里只在设置里打开
 * 「底栏搜索」后出现，从底栏右侧的圆形按钮进入，整页从下往上推入，
 * 底层内容按 MIUI 文件管理那套参数上移 106dp、缩到 0.92 并模糊。
 *
 * 结构参照反编译里的搜索页：顶部一条玻璃搜索框（左返回、右取消），
 * 未提交时是「搜索记录 + 热搜」，提交后是「视频 / UP主」分段栏 + 结果列表。
 */
@Composable
fun BottomSearchPage(
    api: BiliApi,
    settings: SettingsStore,
    /** 展开进度 0 → 1，由 App 的弹簧驱动。 */
    reveal: Float,
    /** 整页淡入淡出：复刻官方 Activity 的 window 过渡（300ms）。 */
    windowAlpha: Float,
    /** 遮罩透明度：进入 linear 100ms；退出 linear 100ms + 50ms 延迟。 */
    maskAlpha: Float,
    /** 页面内容（搜索栏 + 列表）透明度：进入 linear 100ms + 50ms 延迟；退出 linear 100ms。 */
    contentAlpha: Float,
    submitted: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
    onBackToLanding: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenUser: (SearchUser) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    var results by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var users by remember { mutableStateOf<List<SearchUser>>(emptyList()) }
    var hotKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadedOnce by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var history by remember { mutableStateOf(settings.searchHistory) }

    LaunchedEffect(Unit) {
        if (settings.showHotSearch) {
            hotKeywords = runCatching { api.hotKeywords() }.getOrDefault(emptyList())
        }
    }

    // 输入时防抖拉联想；已经进结果页就不再拉。
    LaunchedEffect(query, submitted) {
        val word = query.trim()
        if (submitted != null || word.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(180)
        suggestions = runCatching { api.searchSuggest(word) }.getOrDefault(emptyList())
    }

    LaunchedEffect(submitted) {
        if (submitted == null) {
            results = emptyList()
            users = emptyList()
            error = null
            loadedOnce = false
            tab = 0
            return@LaunchedEffect
        }
        loading = true
        loadedOnce = false
        error = null
        delay(90)
        runCatching { api.searchUsers(submitted) }
            .onSuccess { users = it }
        runCatching { api.search(keyword = submitted) }
            .onSuccess { results = it }
            .onFailure { error = it.message ?: "搜索失败" }
        loading = false
        loadedOnce = true
    }

    // 进页面自动聚焦弹键盘；进结果页则收起，和原实现一致。
    LaunchedEffect(submitted) {
        if (submitted == null) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        } else {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    val p = reveal.coerceIn(0f, 1f)
    val liftPx = with(density) { TeakSearchTransitionDefaults.ContentLift.toPx() }
    val window = windowAlpha.coerceIn(0f, 1f)
    val mask = maskAlpha.coerceIn(0f, 1f)
    val content = contentAlpha.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = window }
            // 遮罩：linear 100ms（进入）/ 100ms + 50ms 延迟（退出）。
            .background(colors.background.copy(alpha = mask)),
    ) {
        // 拦住透传到首页底下的手势：页面本身是半透明玻璃，空白处不能点穿。
        Box(Modifier.matchParentSize().blockTouches(true))
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 原实现：容器从 +106dp 滑到 0（弹簧），透明度是独立的 linear 100ms(+50ms 延迟)。
                .graphicsLayer {
                    translationY = (1f - p) * liftPx
                    alpha = content
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
                    .height(HomeTopBarHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 搜索页内所有控件都不用玻璃：返回键和搜索框直接给实色表面。
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceContainerHigh)
                        .clickable {
                            if (submitted != null) onBackToLanding() else onCancel()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = MiuixIcons.ChevronBackward,
                        contentDescription = "返回",
                        tint = colors.onBackground,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(Capsule())
                        .background(colors.surfaceContainerHigh),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = "搜索",
                            tint = colors.onBackgroundVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = ComposerTextStyle.copy(color = colors.onBackground),
                            cursorBrush = SolidColor(BrandPink),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    val word = query.trim()
                                    if (word.isNotEmpty()) {
                                        settings.addSearchHistory(word)
                                        history = settings.searchHistory
                                        onSubmit(word)
                                        keyboard?.hide()
                                    }
                                },
                            ),
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            decorationBox = { innerTextField ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = "搜索视频、UP 主",
                                        color = colors.onBackgroundVariant,
                                        style = ComposerTextStyle,
                                        maxLines = 1,
                                    )
                                }
                                innerTextField()
                            },
                        )
                        if (query.isNotEmpty()) {
                            Text(
                                text = "清除",
                                color = colors.onBackgroundVariant,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(Capsule())
                                    .clickable { onQueryChange("") }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                // 与首页顶栏的消息按钮同尺寸同位置：38(返回) + 10 + 胶囊 + 10 + 40。
                Spacer(Modifier.width(10.dp))
                val submitWord = query.trim()
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (submitWord.isEmpty()) {
                                onCancel()
                            } else {
                                settings.addSearchHistory(submitWord)
                                history = settings.searchHistory
                                onSubmit(submitWord)
                                keyboard?.hide()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (submitWord.isEmpty()) "取消" else "搜索",
                        color = if (submitWord.isEmpty()) colors.onBackgroundVariant else BrandPink,
                        fontSize = 13.sp,
                        fontWeight = if (submitWord.isEmpty()) FontWeight.Normal else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }

            if (submitted == null) {
                BottomSearchLanding(
                    history = history,
                    hot = if (settings.showHotSearch) hotKeywords else emptyList(),
                    query = query,
                    suggestions = suggestions,
                    onPickKeyword = { word ->
                        settings.addSearchHistory(word)
                        history = settings.searchHistory
                        onQueryChange(word)
                        onSubmit(word)
                        keyboard?.hide()
                    },
                    onClearHistory = {
                        settings.clearSearchHistory()
                        history = emptyList()
                    },
                )
            } else {
                BottomSearchResults(
                    tab = tab,
                    onTabChange = { tab = it },
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

/* ------------------------------------------------------------- landing */

@Composable
private fun BottomSearchLanding(
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
        modifier = Modifier
            .fillMaxSize()
            .centeredListWidth(scrollState = listState)
            .imePadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 40.dp),
    ) {
        if (query.isNotBlank()) {
            if (suggestions.isNotEmpty()) {
                item(key = "suggest-title") {
                    BottomSectionTitle("搜索建议", null, null)
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
                            Text(
                                text = word,
                                color = colors.onBackground,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPickKeyword(word) }
                                    .padding(vertical = 12.dp, horizontal = 2.dp),
                            )
                        }
                    }
                }
            }
            return@LazyColumn
        }

        if (history.isNotEmpty()) {
            item(key = "history-title") {
                BottomSectionTitle("搜索记录", "清空", onClearHistory)
            }
            item(key = "history-list") {
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
                                Text(
                                    text = word,
                                    color = colors.onBackground,
                                    fontSize = 13.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(Capsule())
                                        .clickable { onPickKeyword(word) }
                                        .padding(vertical = 9.dp, horizontal = 6.dp),
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
                BottomSectionTitle("大家都在搜", null, null)
            }
            item(key = "hot-list") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.surface)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    hot.take(10).forEachIndexed { index, word ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPickKeyword(word) }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = if (index < 3) BrandPink else colors.onBackgroundVariant,
                                fontSize = 13.sp,
                                fontWeight = if (index < 3) FontWeight.Medium else FontWeight.Normal,
                                modifier = Modifier.width(24.dp),
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
                }
            }
        }
    }
}

@Composable
private fun BottomSectionTitle(
    title: String,
    action: String?,
    onAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = colors.onBackgroundVariant,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(Capsule())
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/* ------------------------------------------------------------- results */

@Composable
private fun BottomSearchResults(
    tab: Int,
    onTabChange: (Int) -> Unit,
    results: List<VideoItem>,
    users: List<SearchUser>,
    loading: Boolean,
    loadedOnce: Boolean,
    error: String?,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenUser: (SearchUser) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    Column(modifier = Modifier.fillMaxSize()) {
        // 分段栏参照原实现的分类标题：搜索框下面一行的类别切换。
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            BottomSearchSegmented(
                options = listOf("视频", "UP主"),
                selected = tab,
                onSelect = onTabChange,
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .centeredListWidth(scrollState = listState)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading && results.isEmpty() && users.isEmpty()) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) { TeakLoading() }
                }
            }
            if (error != null && results.isEmpty() && users.isEmpty()) {
                item(key = "error") {
                    Text(
                        text = error,
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }

            if (tab == 0) {
                items(items = results, key = { "r" + it.bvid }) { video ->
                    BottomSearchVideoRow(video = video, onClick = { onOpenVideo(video) })
                }
                if (loadedOnce && !loading && results.isEmpty() && error == null) {
                    item(key = "empty-videos") {
                        Text(
                            text = "没有找到相关视频",
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
            } else {
                items(items = users, key = { "u" + it.mid }) { user ->
                    BottomSearchUserRow(user = user, onClick = { onOpenUser(user) })
                }
                if (loadedOnce && !loading && users.isEmpty() && error == null) {
                    item(key = "empty-users") {
                        Text(
                            text = "没有找到相关用户",
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 结果页的分段栏：实色胶囊 + 选中项白底，不走玻璃。 */
@Composable
private fun BottomSearchSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .clip(Capsule())
            .background(colors.surfaceContainerHigh)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = selected == index
            Box(
                modifier = Modifier
                    .clip(Capsule())
                    .background(if (active) colors.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (active) colors.onBackground else colors.onBackgroundVariant,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun BottomSearchVideoRow(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(126.dp)
                .height(126.dp * 9f / 16f)
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.height(6.dp))
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

@Composable
private fun BottomSearchUserRow(
    user: SearchUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier
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
