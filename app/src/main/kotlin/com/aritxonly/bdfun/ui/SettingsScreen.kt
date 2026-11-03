package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.data.CdnAutoSelector
import com.aritxonly.bdfun.data.CdnServices
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.chrome.rememberTeakPageScrollState
import com.aritxonly.teak.common.settings.TeakSettingsActionRow
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsDropdownRow
import com.aritxonly.teak.common.settings.TeakSettingsPage
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow
import com.aritxonly.teak.common.settings.TeakSettingsValueRow
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    loggedIn: Boolean = true,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var cdnSpeeds by remember { mutableStateOf(CdnAutoSelector.speedsSnapshot()) }
    // 进设置页时顺带测一次，选项后面才能显示各节点速度。
    LaunchedEffect(Unit) {
        if (!CdnAutoSelector.isFresh() || cdnSpeeds.isEmpty()) {
            CdnAutoSelector.refresh(
                com.aritxonly.bdfun.AppGraph.api,
                force = cdnSpeeds.isEmpty(),
            )
        }
        cdnSpeeds = CdnAutoSelector.speedsSnapshot()
        while (CdnAutoSelector.isProbing()) {
            cdnSpeeds = CdnAutoSelector.speedsSnapshot()
            delay(400)
        }
        cdnSpeeds = CdnAutoSelector.speedsSnapshot()
    }
    val cdnOptions = remember(cdnSpeeds) { cdnMenuLabels(cdnSpeeds) }
    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            settings.splashWallpaper = uri.toString()
        }
    }

    TeakSettingsPage(
        title = "设置",
    ) {
        item(key = "appearance") {
            // 行内文字立刻跟着选中项变，真正的主题切换仍然延迟到退场动画之后
            var pendingDark by remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
            var pendingColor by remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
            androidx.compose.runtime.LaunchedEffect(settings.darkMode) { pendingDark = null }
            androidx.compose.runtime.LaunchedEffect(settings.colorStyle) { pendingColor = null }
            TeakSettingsCard(title = "外观") {
                TeakSettingsDropdownRow(
                    title = "主题",
                    options = listOf("跟随系统", "浅色", "深色"),
                    selected = pendingDark ?: settings.darkMode,
                    // 切换主题会让整棵树重组（100ms+）。等下拉的退场动画跑完再落状态，
                    // 否则动画第一帧就被重组卡住，看起来像"跳一下/播错"。
                    onSelected = { index ->
                        pendingDark = index
                        scope.launch {
                            kotlinx.coroutines.delay(260)
                            settings.darkMode = index
                        }
                    },
                )
                TeakSettingsDropdownRow(
                    title = "色彩风格",
                    options = listOf("AC 粉", "Hyper 蓝"),
                    selected = pendingColor ?: settings.colorStyle,
                    onSelected = { index ->
                        pendingColor = index
                        scope.launch {
                            kotlinx.coroutines.delay(260)
                            settings.colorStyle = index
                            // 立即切全局主色；不用等 Activity 重建
                            com.aritxonly.bdfun.ThemePalette.style = index
                        }
                    },
                )
                TeakSettingsSwitchRow(
                    title = "模糊效果",
                    checked = settings.blurEnabled,
                    onCheckedChange = { settings.blurEnabled = it },
                )
            }
        }

        item(key = "playback") {
            TeakSettingsCard(title = "播放") {
                TeakSettingsSwitchRow(
                    title = "首页使用 Web 端推荐",
                    checked = settings.recommendWeb,
                    onCheckedChange = { settings.recommendWeb = it },
                )
                TeakSettingsSwitchRow(
                    title = "显示正在观看人数",
                    checked = settings.showViewerCount,
                    onCheckedChange = { settings.showViewerCount = it },
                )
                TeakSettingsSwitchRow(
                    title = "播放进度记忆",
                    checked = settings.resumePlayback,
                    onCheckedChange = { settings.resumePlayback = it },
                )
                TeakSettingsSwitchRow(
                    title = "亮度/音量滑动手势",
                    checked = settings.gestureBrightnessVolume,
                    onCheckedChange = { settings.gestureBrightnessVolume = it },
                )
                TeakSettingsSwitchRow(
                    title = "视频中部上下划屏切换/退出全屏模式",
                    checked = settings.gestureCenterFullscreen,
                    onCheckedChange = { settings.gestureCenterFullscreen = it },
                )
                TeakSettingsSwitchRow(
                    title = "长按倍速划动调速",
                    checked = settings.gestureBoostSwipe,
                    onCheckedChange = { settings.gestureBoostSwipe = it },
                )
                TeakSettingsDropdownRow(
                    title = "长按默认倍率",
                    options = BOOST_SPEED_LABELS,
                    selected = BOOST_SPEED_VALUES
                        .indexOfFirst { kotlin.math.abs(it - settings.boostDefaultSpeed) < 0.01f }
                        .coerceAtLeast(0),
                    onSelected = { settings.boostDefaultSpeed = BOOST_SPEED_VALUES[it] },
                )
                TeakSettingsSwitchRow(
                    title = "全屏显示时间",
                    checked = settings.showFullscreenTime,
                    onCheckedChange = { settings.showFullscreenTime = it },
                )
                TeakSettingsSwitchRow(
                    title = "全屏显示互动按钮",
                    checked = settings.showFullscreenActions,
                    onCheckedChange = { settings.showFullscreenActions = it },
                )
                TeakSettingsSwitchRow(
                    title = "响度均衡",
                    checked = settings.loudnessEq,
                    onCheckedChange = { settings.loudnessEq = it },
                )
                TeakSettingsDropdownRow(
                    title = "默认清晰度",
                    options = QUALITY_LABELS,
                    selected = QUALITY_VALUES.indexOf(settings.qualityPreference).coerceAtLeast(0),
                    onSelected = { settings.qualityPreference = QUALITY_VALUES[it] },
                )
                TeakSettingsDropdownRow(
                    title = "硬解模式",
                    description = when (settings.decoderMode) {
                        1 -> "优先硬件解码，失败时自动回退"
                        2 -> "只使用软件解码，兼容性优先"
                        3 -> "只使用硬件解码，失败时不回退"
                        else -> "自动选择，优先硬件解码"
                    },
                    options = DECODER_LABELS,
                    selected = settings.decoderMode.coerceIn(0, DECODER_LABELS.lastIndex),
                    onSelected = { settings.decoderMode = it },
                )
                TeakSettingsDropdownRow(
                    title = "首选解码格式",
                    description = "不可用时自动回退到其他编码",
                    options = CODEC_LABELS,
                    selected = CODEC_VALUES.indexOf(settings.codecPreference).coerceAtLeast(0),
                    onSelected = { settings.codecPreference = CODEC_VALUES[it] },
                )
                TeakSettingsDropdownRow(
                    title = "CDN 设置",
                    options = cdnOptions,
                    selected = cdnSelectedIndex(settings.cdnService),
                    panelWidth = 300.dp,
                    onSelected = { index ->
                        settings.cdnService = CdnServices[index].id
                    },
                )
                TeakSettingsSwitchRow(
                    title = "自动旋转",
                    checked = settings.autoRotate,
                    onCheckedChange = { settings.autoRotate = it },
                )
                TeakSettingsSwitchRow(
                    title = "进入视频自动播放",
                    checked = settings.autoPlay,
                    onCheckedChange = { settings.autoPlay = it },
                )
                TeakSettingsSwitchRow(
                    title = "动态视频自动播放",
                    checked = settings.dynamicAutoPlay,
                    onCheckedChange = { settings.dynamicAutoPlay = it },
                )
                TeakSettingsDropdownRow(
                    title = "动态视频清晰度",
                    options = listOf("1080P", "720P", "480P"),
                    selected = listOf(80, 64, 32)
                        .indexOf(settings.dynamicAutoPlayQuality)
                        .coerceAtLeast(0),
                    onSelected = {
                        settings.dynamicAutoPlayQuality = listOf(80, 64, 32)[it]
                    },
                )
                TeakSettingsSwitchRow(
                    title = "蓝牙耳机延迟补偿",
                    description = "检测到蓝牙耳机设备时自动校准播放延迟",
                    checked = settings.bluetoothLatencyEnabled,
                    onCheckedChange = { settings.bluetoothLatencyEnabled = it },
                )
                if (settings.bluetoothLatencyEnabled) {
                    TeakSettingsDropdownRow(
                        title = "补偿延迟",
                        options = LATENCY_VALUES.map { "$it ms" },
                        selected = LATENCY_VALUES.indexOf(settings.bluetoothLatencyMs)
                            .coerceAtLeast(0),
                        onSelected = { settings.bluetoothLatencyMs = LATENCY_VALUES[it] },
                    )
                }
            }
        }

        item(key = "display") {
            TeakSettingsCard(title = "显示") {
                TeakSettingsSwitchRow(
                    title = "显示热搜",
                    checked = settings.showHotSearch,
                    onCheckedChange = { settings.showHotSearch = it },
                )
                TeakSettingsSwitchRow(
                    title = "沉浸状态栏",
                    checked = settings.videoImmersiveStatusBar,
                    onCheckedChange = { settings.videoImmersiveStatusBar = it },
                )
                TeakSettingsSwitchRow(
                    title = "减弱动态效果",
                    checked = !settings.animationsEnabled,
                    onCheckedChange = { settings.animationsEnabled = !it },
                )
                TeakSettingsSwitchRow(
                    title = "智能刷新率",
                    description = "根据画面状态自动调整屏幕刷新率",
                    checked = settings.smartRefresh,
                    onCheckedChange = { settings.smartRefresh = it },
                )
                TeakSettingsSwitchRow(
                    title = "抬高底栏",
                    description = "解决未开启安卓导航条时底栏位置过低的问题",
                    checked = settings.bottomBarRaise,
                    onCheckedChange = { settings.bottomBarRaise = it },
                )
                TeakSettingsSwitchRow(
                    title = "显示评论 IP 属地",
                    checked = settings.showIpLocation,
                    onCheckedChange = { settings.showIpLocation = it },
                )
                TeakSettingsActionRow(
                    title = "设置开屏壁纸",
                    trailing = if (settings.splashWallpaper.isBlank()) "未设置" else "已设置",
                ) {
                    wallpaperPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
                TeakSettingsActionRow(title = "清除开屏壁纸") {
                    settings.splashWallpaper = ""
                    context.showToast("已清除开屏壁纸")
                }
                GlobalFontScaleRow(
                    value = settings.fontScale,
                    onValueChange = { settings.fontScale = it },
                )
                TeakSettingsSwitchRow(
                    title = "显示「我的评论与弹幕」入口",
                    checked = settings.sentHistoryEntry,
                    onCheckedChange = { settings.sentHistoryEntry = it },
                )
            }
        }

        item(key = "about") {
            val context = androidx.compose.ui.platform.LocalContext.current
            TeakSettingsCard(title = "关于") {
                TeakSettingsValueRow(
                    title = "版本",
                    value = BuildConfig.VERSION_NAME,
                )
                TeakSettingsActionRow(title = "加入讨论组", trailing = "QQ 群 $QQ_GROUP") {
                    openQqGroup(context)
                }
                TeakSettingsActionRow(title = "更新日志") {
                    com.aritxonly.bdfun.TeakNavigator.openChangelog(context)
                }
                TeakSettingsActionRow(title = "捐赠") {
                    com.aritxonly.bdfun.TeakNavigator.openDonation(context)
                }
                TeakSettingsActionRow(title = "引用") {
                    com.aritxonly.bdfun.TeakNavigator.openCredits(context)
                }
                TeakSettingsActionRow(title = "项目地址") {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/QxTeak/bdFun"),
                            ),
                        )
                    }.onFailure { context.showToast("无法打开链接") }
                }
                TeakSettingsActionRow(title = "检查更新") {
                    scope.launch {
                        val info = UpdateChecker.check()
                        if (info != null) {
                            UpdatePromptState.show(info)
                        } else {
                            context.showToast("已是最新版本")
                        }
                    }
                }
                TeakSettingsSwitchRow(
                    title = "自动提示更新",
                    checked = settings.autoCheckUpdate,
                    onCheckedChange = { settings.autoCheckUpdate = it },
                )
            }
        }

        if (loggedIn) {
            item(key = "account") {
                TeakSettingsCard(title = "账号") {
                    TeakSettingsActionRow(
                        title = "退出登录",
                        color = Color(0xFFE5484D),
                        onClick = onLogout,
                    )
                }
            }
        }
    }

}

private val QUALITY_LABELS = listOf("自动", "HDR", "4K", "1080P+", "1080P", "720P", "480P")
private val QUALITY_VALUES = listOf(0, 125, 120, 112, 80, 64, 32)
private val LATENCY_VALUES = listOf(50, 100, 150, 200, 250, 300)
private val BOOST_SPEED_VALUES = listOf(1.5f, 2f, 2.5f, 3f, 4f, 5f)
private val BOOST_SPEED_LABELS = BOOST_SPEED_VALUES.map {
    if (it % 1f == 0f) "${it.toInt()}×" else "${it}×"
}
private val DECODER_LABELS = listOf(
    "自动（推荐）",
    "硬件解码",
    "软件解码",
    "硬件解码（禁回退）",
)

/** 选项文本 = 节点名 + 最近一次测速成绩。 */
private fun cdnMenuLabels(speeds: Map<String, Double>): List<String> {
    return buildList {
        CdnServices.forEach { add(withSpeed(it.label, speeds[it.id])) }
    }
}

private fun withSpeed(label: String, speed: Double?): String =
    if (speed != null && speed > 0.01) {
        "$label  ${String.format(java.util.Locale.US, "%.1f", speed)} MB/s"
    } else {
        label
    }

private fun cdnSelectedIndex(id: String): Int {
    val index = CdnServices.indexOfFirst { it.id == id }
    return if (index < 0) 0 else index
}

private val CODEC_LABELS = listOf(
    "自动",
    "AVC (H.264)",
    "HEVC (H.265)",
    "AV1",
    "DVH1",
)

private val CODEC_VALUES = listOf("auto", "avc", "hevc", "av1", "dvh1")

private const val QQ_GROUP = "852442572"

/** 加入讨论组：优先拉起 QQ 群卡片，没有 QQ 时复制群号并提示。 */
private fun openQqGroup(context: android.content.Context) {
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse(
            "mqqapi://card/show_pslcard?src_type=internal&version=1" +
                "&uin=$QQ_GROUP&card_type=group&source=qrcode",
        ),
    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { context.startActivity(intent) }.isSuccess) return
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
        as? android.content.ClipboardManager
    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("QQ群号", QQ_GROUP))
    context.showToast("未检测到 QQ，已复制群号 $QQ_GROUP")
}

/** 全局字号：默认 100%，只缩放文字 sp，不改控件尺寸。 */
@Composable
private fun GlobalFontScaleRow(value: Float, onValueChange: (Float) -> Unit) {
    val colors = MiuixTheme.colorScheme
    // 100% 固定落在滑轨正中
    val min = 0.8f
    val max = 1.2f
    // 拖动时只改本地字号，松手再应用全局 density，避免手势被打断
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    val fraction = ((sliderValue - min) / (max - min)).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "全局字号", color = colors.onBackground, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(sliderValue * 100).toInt()}%",
                color = colors.onBackgroundVariant,
                fontSize = 13.5.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    // 靠近 100% 时吸附到 100%
                    sliderValue = if (kotlin.math.abs(it - 1f) < 0.025f) 1f else it
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = min..max,
                steps = 0,
                onValueChangeFinished = { onValueChange(sliderValue) },
                showKeyPoints = false,
                keyPoints = listOf(1f),
                magnetThreshold = 0.035f,
            )
            // 100% 位置的小圆点：滑块正好在 100% 时被圆钮盖住，不重叠
            if (kotlin.math.abs(fraction - 0.5f) > 0.02f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(colors.onBackground.copy(alpha = 0.22f)),
                )
            }
        }
    }
}
