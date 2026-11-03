package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class ChangelogVersion(
    val version: String,
    val changes: List<String>,
)

private val CHANGELOG = listOf(
    ChangelogVersion(
        version = "preview-3.9.4",
        changes = listOf(
            "重构弹幕设置与弹幕绘制",
            "视频中部上下划屏切换/退出全屏模式",
            "优化播放器操作手感，修复有时手势失效的问题",
            "修复打开已观看视频时无法自动跳转观看进度的问题",
            "新增 GitHub 检查更新与自动提示",
            "新增显示热搜开关",
            "修复搜索页深色模式仍显示白色背景的问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.8",
        changes = listOf(
            "精简了部分代码",
            "统一已完成页面的视觉效果",
            "新增引用页",
            "新增底栏抬高选项",
            "新增 CDN 设置",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.7",
        changes = listOf(
            "消息页焕新重构",
            "半屏播放器新增听视频与更多设置面板",
            "动态页与评论区支持点击头像或昵称进入个人主页",
            "设置关于页新增捐赠入口",
            "设置新增减弱动态效果选项",
            "优化了部分界面的视觉效果",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.6",
        changes = listOf(
            "修复进入视频时状态栏图标反色异常，以及部分机型动态详情页状态栏显示异常的问题",
            "修复从相关推荐进入视频时出现的声音问题",
            "新增视频缓存功能，支持缓存当前视频、后台下载、离线播放与缓存管理",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.5",
        changes = listOf(
            "焕新半屏播放器 UI，统一全屏与半屏的进度条与播放控件",
            "切换下一集时直接在播放器内加载，不再跳转新的播放页",
            "优化大量 UI 显示效果，修复弹幕、手势、面板与加载相关问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.4",
        changes = listOf(
            "新增听视频、小窗播放、自动连播、循环播放与镜像翻转等功能",
            "播放器功能 UI 与易用性仍在持续完善",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.3",
        changes = listOf(
            "播放器 UI 全新焕新，采用与原版基本一致的布局",
            "优化播放器操作手感，并修复部分 UI 显示问题",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.2",
        changes = listOf(
            "改进视频详情页的部分 UI 与动效",
            "播放器更好地支持 4K 与 HDR 视频",
            "新增响度均衡选项",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.1",
        changes = listOf(
            "新增动态视频自动播放功能",
            "优化动态页排版，并提升部分图标的显示清晰度",
        ),
    ),
    ChangelogVersion(
        version = "preview-3.0",
        changes = listOf(
            "动态页相关功能基本制作完毕",
            "优化部分 UI 显示效果",
            "新增评论 IP 属地显示与播放器观看人数显示选项",
        ),
    ),
    ChangelogVersion(
        version = "preview-2.9",
        changes = listOf(
            "新增色彩风格调整",
            "初步优化「我的」页面的显示效果，使其基本可用",
            "进行部分 UI 调整",
            "新增重新打开视频时是否恢复原播放进度的选项",
            "新增首页推荐使用 Web 端或 App 端接口的选项",
        ),
    ),
    ChangelogVersion(
        version = "preview-2.8",
        changes = listOf(
            "优化设置、收藏、历史记录等页面的 UI",
            "新增全局字号调整",
            "新增我的评论与弹幕记录",
        ),
    ),
    ChangelogVersion(
        version = "preview-2.7",
        changes = listOf(
            "修复无法点赞、点踩的问题",
            "修复状态栏沉浸导致的 UI 问题",
            "新增更多弹幕设置项",
            "新增打开视频自动播放开关",
            "对部分 UI 进行临时焕新与排版调整",
        ),
    ),
)

@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    val cardShape = remember { RoundedCornerShape(22.dp) }
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            TeakLargeTitle(
                title = "更新日志",
                subtitle = "当前版本 ${BuildConfig.VERSION_NAME}",
            )
        }

        CHANGELOG.forEach { entry ->
            item(key = entry.version) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = entry.version,
                        modifier = Modifier.padding(start = 14.dp, bottom = 5.dp),
                        color = colors.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.surface)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = "更新内容",
                            color = colors.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(10.dp))
                        entry.changes.forEachIndexed { index, line ->
                            Row(
                                modifier = Modifier.padding(bottom = 9.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    color = BrandPink,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.width(22.dp),
                                )
                                Text(
                                    text = line,
                                    color = colors.onBackgroundVariant,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

