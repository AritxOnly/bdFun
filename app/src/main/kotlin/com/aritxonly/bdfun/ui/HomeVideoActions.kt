package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.data.RecommendationReason
import com.aritxonly.bdfun.data.VideoItem
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.SearchDevice
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ActionSheetRow(label: String, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(24.dp), tint = colors.onSurface)
            Spacer(Modifier.width(14.dp))
        }
        Text(label, color = if (enabled) colors.onSurface else colors.onBackgroundVariant, fontSize = 16.sp)
    }
}

@Composable
internal fun HomeVideoActions(video: VideoItem?, state: HomeFeedState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var category by remember(video?.bvid) { mutableStateOf(video?.categoryName.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var categoryLoading by remember(video?.bvid) { mutableStateOf(false) }
    LaunchedEffect(video?.bvid) {
        if (video != null && category.isBlank()) {
            categoryLoading = true
            try { category = AppGraph.api.view(video.bvid).tname }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { /* Show an explicit unavailable state, never invent a category. */ }
            finally { categoryLoading = false }
        }
    }
    fun perform(action: suspend (VideoItem) -> String) {
        val target = video ?: return
        if (busy) return
        busy = true
        scope.launch {
            try {
                context.showToast(action(target))
                onDismiss()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { context.showToast(e.message ?: "操作失败，请重试") }
            finally { busy = false }
        }
    }
    fun reason(target: VideoItem, creator: Boolean): RecommendationReason? =
        if (!target.appRecommendation) RecommendationReason(if (creator) 4 else 1, "")
        else target.dislikeReasons.firstOrNull {
            if (creator) it.name.contains("UP", ignoreCase = true)
            else it.name.contains("不感兴趣") || it.name.contains("当前视频")
        }
    TeakActionSheet(visible = video != null, onDismiss = { if (!busy) onDismiss() }) {
        val colors = MiuixTheme.colorScheme
        Column(Modifier.fillMaxWidth().clip(ContinuousCornerShape(22.dp)).background(colors.surface)) {
            ActionSheetRow(if (busy) "正在处理…" else "添加至稍后再看", !busy, MiuixIcons.SearchDevice) {
                perform { check(AppGraph.api.addWatchLater(it.aid)) { "添加失败" }; "已添加至稍后再看" }
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().clip(ContinuousCornerShape(22.dp)).background(colors.surface)) {
            ActionSheetRow("不感兴趣：当前视频", !busy, MiuixIcons.Close) {
                perform { target ->
                    val feedback = reason(target, false) ?: error("推荐接口未提供此项反馈，请刷新推荐后重试")
                    AppGraph.api.dislikeRecommendation(target, feedback)
                    state.dismissRecommendation(target)
                    "已反馈，减少此类推荐"
                }
            }
            ActionSheetRow("不感兴趣：UP", !busy && (video?.ownerMid ?: 0L) > 0, MiuixIcons.Contacts) {
                perform { target ->
                    val feedback = reason(target, true) ?: error("推荐接口未提供 UP 反馈，请刷新推荐后重试")
                    AppGraph.api.dislikeRecommendation(target, feedback)
                    state.dismissRecommendation(target, creator = true)
                    "已反馈，不再推荐此 UP"
                }
            }
            val categoryReason = video?.dislikeReasons?.firstOrNull {
                category.isNotBlank() && it.name.contains(category)
            }
            ActionSheetRow("不感兴趣：" + category.ifBlank { if (categoryLoading) "分类加载中…" else "分类暂不可用" },
                !busy && category.isNotBlank(), MiuixIcons.Album) {
                perform { target ->
                    if (categoryReason != null) AppGraph.api.dislikeRecommendation(target, categoryReason)
                    state.dismissRecommendation(target, category = category)
                    if (categoryReason != null) "已反馈，减少 $category 推荐" else "本次浏览不再显示 $category 推荐"
                }
            }
            if (video != null && !categoryLoading && category.isNotBlank() && categoryReason == null) {
                Text("此推荐源的分类过滤仅在本次浏览生效", color = colors.onBackgroundVariant,
                    fontSize = 12.sp, modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 14.dp))
            }
        }
    }
}
