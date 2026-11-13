package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsDropdownRow
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow

@Composable
internal fun SettingsPlayerCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "播放器") {
        SettingsPlayerInteractionRows(runtime)
        SettingsPlayerSourceRows(runtime)
        SettingsPlayerContinuationRows(runtime)
    }
}

@Composable
private fun SettingsPlayerInteractionRows(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
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
        title = "亮度/音量划动手势",
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
    TeakSettingsDropdownRow(
        title = "默认播放倍速",
        options = PLAYBACK_SPEED_LABELS,
        selected = PLAYBACK_SPEED_VALUES
            .indexOfFirst { kotlin.math.abs(it - settings.defaultPlaybackSpeed) < 0.01f }
            .coerceAtLeast(0),
        onSelected = { settings.defaultPlaybackSpeed = PLAYBACK_SPEED_VALUES[it] },
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
    TeakSettingsDropdownRow(
        title = "进度条常显",
        options = listOf("关闭", "仅半屏时常显", "仅全屏时常显", "永久显示"),
        selected = settings.progressBarMode,
        onSelected = { settings.progressBarMode = it },
    )
}

@Composable
private fun SettingsPlayerSourceRows(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
    val cdnOptions = runtime.cdnState.labels
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
        description = "如发现播放问题，请选择原地址",
        options = cdnOptions,
        closedValue = runtime.cdn.services[runtime.cdn.selectedIndex(settings.cdnService)].label,
        selected = runtime.cdn.selectedIndex(settings.cdnService),
        panelWidth = 300.dp,
        onSelected = { index ->
            settings.cdnService = runtime.cdn.services[index].id
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
        title = "蓝牙耳机延迟补偿",
        description = "检测到蓝牙耳机设备时自动校准播放延迟",
        checked = settings.bluetoothLatencyEnabled,
        onCheckedChange = { settings.bluetoothLatencyEnabled = it },
    )
}

@Composable
private fun SettingsPlayerContinuationRows(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
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
