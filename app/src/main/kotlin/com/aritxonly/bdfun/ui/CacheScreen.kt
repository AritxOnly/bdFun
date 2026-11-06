package com.aritxonly.bdfun.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.aritxonly.bdfun.TeakNavigator
import com.aritxonly.bdfun.data.CacheStatus
import com.aritxonly.bdfun.data.CachedVideo
import com.aritxonly.bdfun.data.VideoCacheManager
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsPage
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CacheScreen() {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val entries = VideoCacheManager.items

    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 标题照常渲染，但空提示覆盖整屏居中，不受大标题占位影响。
            TeakSettingsPage(title = "离线缓存", subtitle = null) {}
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "还没有缓存的视频",
                    color = colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text = "在播放器「更多」中选择「缓存」即可下载",
                    modifier = Modifier.padding(top = 6.dp),
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                )
            }
        }
        return
    }

    TeakSettingsPage(
        title = "离线缓存",
        subtitle = "共 ${entries.size} 个 · ${formatCacheSize(entries.sumOf { it.totalBytes })}",
    ) {
        items(entries, key = { it.id }) { entry ->
            val oneTake = rememberOneTakeAnchor()
            val cacheRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
            TeakSettingsCard(
                modifier = Modifier.padding(bottom = 10.dp).then(oneTake.modifier),
            ) {
                CacheRow(
                    entry = entry,
                    onOpen = {
                        oneTake.open(
                            context = context,
                            video = entry.toVideoItem(),
                            radiusPx = cacheRadiusPx,
                        ) { TeakNavigator.openVideo(context, entry.toVideoItem()) }
                    },
                    onDelete = {
                        VideoCacheManager.delete(entry.id)
                        context.showToast("已删除缓存")
                    },
                )
            }
        }
    }
}

@Composable
private fun CacheRow(
    entry: CachedVideo,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = entry.status == CacheStatus.Completed,
                onClick = onOpen,
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(118.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = rememberImageRequest(entry.localCover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                color = colors.onBackground,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = when (entry.status) {
                    CacheStatus.Completed ->
                        "${entry.qualityLabel} · ${formatCacheSize(entry.totalBytes)}"
                    CacheStatus.Failed -> entry.error ?: "缓存失败"
                    else -> "${entry.qualityLabel} · ${(entry.progress * 100).toInt()}%"
                },
                color = if (entry.status == CacheStatus.Failed) {
                    Color(0xFFE5484D)
                } else {
                    colors.onBackgroundVariant
                },
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.status == CacheStatus.Queued || entry.status == CacheStatus.Downloading) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surfaceContainerHigh),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(entry.progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .background(BrandPink),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (entry.status == CacheStatus.Completed) {
                Text(
                    text = "播放",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(interactionSource = null, indication = null, onClick = onOpen)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = BrandPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            Text(
                text = "删除",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = onDelete,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
    }
}

private fun formatCacheSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1024) {
        "%.2f GB".format(mb / 1024)
    } else {
        "%.1f MB".format(mb)
    }
}
