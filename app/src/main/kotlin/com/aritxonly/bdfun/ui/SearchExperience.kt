package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.data.VideoItem
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索页：搜索框保持原位不动，这张白纸**以搜索框为原点**从它的大小无缝长到整屏。
 *
 * - 未提交时：搜索记录（胶囊，可单条删 / 清空）+ 热搜榜；
 * - 提交后：同一张白纸换成结果列表。
 *
 * [reveal] 由 App 驱动（0 → 1），既用于展开，也用于内容错峰淡入。
 */
@Composable
fun SearchOverlay(
    api: BiliApi,
    settings: SettingsStore,
    submitted: String?,
    reveal: Float,
    onPickKeyword: (String) -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    var hotKeywords by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (settings.showHotSearch && hotKeywords.isEmpty()) hotKeywords = api.hotKeywords()
    }

    LaunchedEffect(submitted) {
        if (submitted == null) {
            results = emptyList()
            error = null
            return@LaunchedEffect
        }
        loading = true
        error = null
        runCatching { api.search(submitted) }
            .onSuccess { results = it }
            .onFailure { error = it.message ?: "搜索失败" }
        loading = false
    }

    val t = reveal.coerceIn(0f, 1f)
    val contentReveal = t

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        // 搜索框圆心：顶栏里那颗胶囊的中心
        val pillCenterY = with(density) { (statusTop + HomeTopBarHeight / 2).toPx() }
        val pillHeightPx = with(density) { 40.dp.toPx() }
        val pillWidthPx = widthPx * 0.80f


        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = t }
                .background(colors.background),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = HomeTopBarHeight)
                    .graphicsLayer {
                        alpha = contentReveal
                        translationY = (1f - contentReveal) * 22.dp.toPx()
                    },
            ) {
                if (submitted == null) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 32.dp),
                    ) {
                        val history = settings.searchHistory
                        if (history.isNotEmpty()) {
                            item(key = "history-title") {
                                SectionHeader(
                                    title = "搜索记录",
                                    action = "清空",
                                    onAction = { settings.clearSearchHistory() },
                                )
                            }
                            item(key = "history-chips") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(colors.surface)
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                ) {
                                    FlowRowChips(
                                        items = history,
                                        onPick = onPickKeyword,
                                        onRemove = { word ->
                                            settings.clearSearchHistory()
                                            history.filter { it != word }.reversed()
                                                .forEach(settings::addSearchHistory)
                                        },
                                    )
                                }
                                Spacer(Modifier.height(18.dp))
                            }
                        }

                        if (settings.showHotSearch) {
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
                                    hotKeywords.forEachIndexed { index, word ->
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
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (loading && results.isEmpty()) {
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
                            if (results.isEmpty()) {
                                item(key = "error") {
                                    Text(
                                        text = message,
                                        color = colors.onBackgroundVariant,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 24.dp),
                                    )
                                }
                            }
                        }
                        items(items = results, key = { "r" + it.bvid }) { video ->
                            SearchResultRow(video = video, onClick = { onOpenVideo(video) })
                        }
                        if (!loading && results.isEmpty() && error == null) {
                            item(key = "empty") {
                                Text(
                                    text = "没有找到相关视频",
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
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: (() -> Unit)?) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, color = colors.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

/** 简易流式胶囊（不引 FlowRow）：每行 3 个。 */
@Composable
private fun FlowRowChips(items: List<String>, onPick: (String) -> Unit, onRemove: (String) -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { word ->
                    Row(
                        modifier = Modifier
                            .clip(Capsule())
                            .background(colors.surfaceContainerHigh)
                            .clickable { onPick(word) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = word,
                            color = colors.onBackground,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(90.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "×",
                            color = colors.onBackgroundVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onRemove(word) },
                        )
                    }
                }
            }
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
            fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Normal,
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
private fun SearchResultRow(video: VideoItem, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .aspectRatio(16f / 9f)
                .clip(ContinuousCornerShape(12.dp))
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
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(Capsule())
                        .background(Color(0x8C000000))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${video.ownerName} · ${formatCount(video.play)} 播放",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
