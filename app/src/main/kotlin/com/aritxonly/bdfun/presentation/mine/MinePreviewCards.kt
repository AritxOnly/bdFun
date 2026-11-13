package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.feature.mine.application.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MinePreviewCard(kind: MinePreviewKind, section: MinePreviewSection, navigation: MineNavigation, retry: () -> Unit) {
    val openAll = when (kind) {
        MinePreviewKind.History -> navigation.onOpenHistory
        MinePreviewKind.Favorites -> navigation.onOpenLibrary
        MinePreviewKind.WatchLater -> navigation.onOpenWatchLater
    }
    val title = when (kind) {
        MinePreviewKind.History -> "历史记录"
        MinePreviewKind.Favorites -> "我的收藏"
        MinePreviewKind.WatchLater -> "稍后再看"
    }
    Column(Modifier.fillMaxWidth().clip(ContinuousCornerShape(22.dp))
        .background(MiuixTheme.colorScheme.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), fontSize = 15.sp, color = MiuixTheme.colorScheme.onBackground)
            Text("查看全部 ＞", Modifier.clickable(onClick = openAll).padding(vertical = 6.dp),
                fontSize = 12.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
        }
        when {
            section.loading || (!section.loaded && section.error == null) -> MinePreviewMessage("加载中…")
            section.error != null -> Text("加载失败，点击重试", Modifier.fillMaxWidth().clickable(onClick = retry).padding(12.dp),
                color = MiuixTheme.colorScheme.primary, fontSize = 13.sp)
            kind == MinePreviewKind.Favorites -> MineFolderPreviews(section, openAll)
            section.entries.isEmpty() -> MinePreviewMessage(if (kind == MinePreviewKind.History) "还没有观看记录" else "还没有待看视频")
            else -> MineVideoPreviews(section, kind == MinePreviewKind.History, navigation)
        }
    }
}

@Composable
private fun MineFolderPreviews(section: MinePreviewSection, openAll: () -> Unit) {
    if (section.folders.isEmpty()) MinePreviewMessage("还没有收藏夹")
    section.folders.forEach { folder ->
        Row(Modifier.fillMaxWidth().clickable(onClick = openAll), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(rememberImageRequest(folder.cover), null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(88.dp, 55.dp).clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(folder.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onBackground)
                Text("${folder.count} 个视频", fontSize = 12.sp, color = MiuixTheme.colorScheme.onBackgroundVariant)
            }
        }
    }
}

@Composable
private fun MineVideoPreviews(section: MinePreviewSection, history: Boolean, navigation: MineNavigation) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        section.entries.forEach { entry ->
            Column(Modifier.weight(1f).clickable { navigation.onOpenVideo(entry.mineVideo(history)) },
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)) {
                    AsyncImage(rememberImageRequest(entry.cover), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                    if (entry.duration > 0) Text(formatDuration(entry.duration), color = Color.White,
                        fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                            .clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 4.dp, vertical = 2.dp))
                    if (history && entry.duration > 0 && entry.progress > 0) Box(Modifier.align(Alignment.BottomStart)
                        .fillMaxWidth((entry.progress.toFloat() / entry.duration).coerceIn(0f, 1f))
                        .height(3.dp).background(MiuixTheme.colorScheme.primary))
                }
                Text(entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onBackground)
                Text(if (history && entry.progress < 0) "已看完" else if (history && entry.progress > 0)
                    "看到 ${entry.progress / 60}:${(entry.progress % 60).toString().padStart(2, '0')}" else entry.author,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onBackgroundVariant)
            }
        }
        if (section.entries.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun MinePreviewMessage(message: String) {
    Text(message, Modifier.padding(vertical = 14.dp), fontSize = 13.sp,
        color = MiuixTheme.colorScheme.onBackgroundVariant)
}
