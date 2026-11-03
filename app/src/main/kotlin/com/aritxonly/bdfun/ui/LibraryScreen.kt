package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.FavoriteFolder
import com.aritxonly.bdfun.data.HistoryItem
import com.aritxonly.bdfun.data.VideoItem
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val MODES = listOf("收藏", "历史", "稍后再看")

@Composable
fun LibraryScreen(
    api: BiliApi,
    loggedIn: Boolean,
    onNeedLogin: () -> Unit,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    /** 非空表示只显示这一个分类（收藏=0 / 历史=1 / 稍后再看=2），并隐藏切换标签。 */
    fixedMode: Int? = null,
    title: String = "收藏",
) {
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()

    var mode by remember { mutableIntStateOf(fixedMode ?: 0) }
    var folders by remember { mutableStateOf<List<FavoriteFolder>>(emptyList()) }
    var folderIndex by remember { mutableIntStateOf(0) }
    var folderVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var entries by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    suspend fun loadFolder(index: Int) {
        val folder = folders.getOrNull(index) ?: return
        loading = true
        error = null
        folderVideos = runCatching { api.favoriteVideos(folder.id) }
            .onFailure { error = it.message ?: "收藏夹加载失败" }
            .getOrDefault(emptyList())
        loading = false
    }

    LaunchedEffect(loggedIn, mode) {
        if (!loggedIn) return@LaunchedEffect
        loading = true
        error = null
        when (mode) {
            0 -> {
                folders = runCatching { api.favoriteFolders() }
                    .onFailure { error = it.message ?: "收藏夹加载失败" }
                    .getOrDefault(emptyList())
                if (folders.isEmpty()) {
                    error = error ?: "还没有收藏夹"
                    loading = false
                } else {
                    folderIndex = 0
                    loadFolder(0)
                }
            }

            1 -> {
                val loaded = runCatching { api.history() }
                    .onFailure { error = it.message ?: "历史记录加载失败" }
                    .getOrDefault(emptyList())
                entries = loaded
                loading = false
                // 先显示历史列表，播放量/弹幕数在后台补齐，避免逐条请求把页面卡住。
                if (loaded.isNotEmpty()) {
                    scope.launch {
                        val enriched = runCatching { api.enrichHistoryStats(loaded) }
                            .getOrDefault(loaded)
                        if (mode == 1) entries = enriched
                    }
                }
            }

            else -> {
                entries = runCatching { api.watchLater() }
                    .onFailure { error = it.message ?: "稍后再看加载失败" }
                    .getOrDefault(emptyList())
                loading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {

        if (!loggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "登录后可查看收藏与历史",
                    color = colors.onBackgroundVariant,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.primary)
                        .clickable { onNeedLogin() }
                        .padding(horizontal = 26.dp, vertical = 11.dp),
                ) {
                    Text(
                        text = "扫码登录",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            return@Column
        }



        Box(modifier = Modifier.fillMaxSize()) {
            val listState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
            TrackTeakPageScroll(listState)
            LazyVerticalGrid(
                state = listState,
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = TeakChromeDefaults.ContentHorizontalPadding,
                    end = TeakChromeDefaults.ContentHorizontalPadding,
                    top = TeakChromeDefaults.ContentTopPadding,
                    bottom = TeakChromeDefaults.ContentBottomPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
                    TeakLargeTitle(
                        title = title,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
                if (fixedMode == null) {
                    item(key = "modes", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MODES.forEachIndexed { index, label ->
                                val active = mode == index
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (active) colors.primary else colors.surfaceContainerHigh)
                                        .clickable { mode = index }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        text = label,
                                        color = if (active) Color.White else colors.onBackground,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }

                if (mode == 0 && folders.isNotEmpty()) {

                    item(key = "folders", span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(folders.size) { index ->
                        val folder = folders[index]
                        val active = index == folderIndex
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (active) {
                                        colors.primary.copy(alpha = 0.16f)
                                    } else {
                                        colors.surfaceContainerHigh
                                    },
                                )
                                .clickable {
                                    folderIndex = index
                                    scope.launch { loadFolder(index) }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "${folder.title} ${folder.count}",
                                color = if (active) colors.primary else colors.onBackgroundVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
                    }
                }
                if (mode == 0) {
                    items(items = folderVideos, key = { it.bvid }) { video ->
                        VideoGridCard(video = video, onClick = { onOpenVideo(video) })
                    }
                } else {
                    items(items = entries, key = { it.bvid }) { entry ->
                        VideoGridCard(
                            video = VideoItem(
                                bvid = entry.bvid,
                                aid = 0,
                                title = entry.title,
                                cover = entry.cover,
                                ownerName = entry.author,
                                ownerFace = "",
                                ownerMid = 0,
                                play = entry.play.toLong(),
                                danmaku = entry.danmaku.toLong(),
                                duration = entry.duration,
                                pubdate = entry.viewedAt,
                            ),
                            onClick = {
                                onOpenVideo(
                                    VideoItem(
                                        bvid = entry.bvid,
                                        aid = 0,
                                        title = entry.title,
                                        cover = entry.cover,
                                        ownerName = entry.author,
                                        ownerFace = "",
                                        ownerMid = 0,
                                        play = entry.play.toLong(),
                                        danmaku = entry.danmaku.toLong(),
                                        duration = entry.duration,
                                        pubdate = entry.viewedAt,
                                    ),
                                )
                            },
                            progress = if (entry.duration > 0) {
                                entry.progress.toFloat() / entry.duration
                            } else {
                                0f
                            },
                        )
                    }
                }

                error?.let { message ->
                    item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(vertical = 20.dp),
                            color = colors.onBackgroundVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            if (loading) {
                // 收藏 / 历史 / 稍后再看：小电视放在内容区域正中，并缩小一号
                TeakLoading(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary,
                    size = 48.dp,
                )
            }
        }
    }
}

@Composable
private fun MediaRow(
    cover: String,
    title: String,
    subtitle: String,
    duration: Int,
    progress: Int,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val shape = remember { ContinuousCornerShape(12.dp) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Box {
            AsyncImage(
                model = rememberImageRequest(cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(148.dp)
                    .aspectRatio(16f / 9f)
                    .clip(shape)
                    .background(colors.surfaceContainerHigh),
            )
            // 已看进度条（历史记录）
            if (duration > 0 && progress > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth((progress.toFloat() / duration).coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(colors.primary),
                )
            }
            if (duration > 0) {
                Text(
                    text = formatDuration(duration),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
