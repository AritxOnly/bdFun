package com.aritxonly.bdfun.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import com.aritxonly.teak.common.chrome.TeakMotion

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /**
     * 拖动弹幕设置时 setter 会被每个 pointer move 调用一次。
     * 这里把浮点设置合并到一次落盘，避免一次拖动产生几十次 SharedPreferences 写入。
     */
    private val persistHandler = Handler(Looper.getMainLooper())
    private val pendingFloats = LinkedHashMap<String, Float>()
    private val flushPendingFloats = Runnable {
        if (pendingFloats.isNotEmpty()) {
            val editor = prefs.edit()
            pendingFloats.forEach { (key, value) -> editor.putFloat(key, value) }
            pendingFloats.clear()
            editor.apply()
        }
    }

    private fun persistFloatEventually(key: String, value: Float) {
        pendingFloats[key] = value
        persistHandler.removeCallbacks(flushPendingFloats)
        persistHandler.postDelayed(flushPendingFloats, 250L)
    }

    init {
        // 3.6 起首页 Web 推荐默认关闭；老版本把默认值 true 写进了偏好，升级时一次性改回 false。
        if (!prefs.getBoolean("recommend_web_migrated_36", false)) {
            prefs.edit()
                .putBoolean("recommend_web", false)
                .putBoolean("recommend_web_migrated_36", true)
                .apply()
        }
        // 3.7 起默认清晰度为「自动」；只把仍是旧默认 1080P(80) 的用户迁到自动。
        if (!prefs.getBoolean("quality_migrated_37", false)) {
            val editor = prefs.edit().putBoolean("quality_migrated_37", true)
            if (prefs.getInt("quality", 80) == 80) editor.putInt("quality", 0)
            editor.apply()
        }
        // 3.7 起动态视频自动播放默认开启；没动过开关的用户直接置为开。
        if (!prefs.contains("dynamic_auto_play")) {
            prefs.edit().putBoolean("dynamic_auto_play", true).apply()
        }
        // preview-3.9：解码器改为 4 档硬解模式，旧 7 档数值一次性映射过来。
        if (!prefs.getBoolean("decoder_mode_v2", false)) {
            val mapped = when (prefs.getInt("decoder_mode", 0)) {
                2 -> 1 // 旧「仅硬件」
                3, 5 -> 2 // 旧「仅软件」「软件优先」
                6 -> 3 // 旧「禁回退」
                else -> 0 // 自动 / 默认 / 硬件优先
            }
            prefs.edit()
                .putInt("decoder_mode", mapped)
                .putBoolean("decoder_mode_v2", true)
                .apply()
        }
        // preview-3.9：CDN 默认改回「基础URL」；仍停在 auto 的用户迁移回 base。
        if (!prefs.getBoolean("cdn_base_v1", false)) {
            if (!prefs.contains("cdn_service") ||
                prefs.getString("cdn_service", "base") == CdnAutoSelector.ID
            ) {
                prefs.edit().putString("cdn_service", "base").apply()
            }
            prefs.edit().putBoolean("cdn_base_v1", true).apply()
        }
    }

    private val dynamicColorState = mutableStateOf(prefs.getBoolean("dynamic_color", false))
    private val smartRefreshState = mutableStateOf(prefs.getBoolean("smart_refresh", true))
    private val bottomBarRaiseState = mutableStateOf(prefs.getBoolean("bottom_bar_raise", false))
    private val danmakuState = mutableStateOf(prefs.getBoolean("danmaku", true))
    private val qualityState = mutableStateOf(prefs.getInt("quality", 0))
    private val lowPowerState = mutableStateOf(prefs.getBoolean("low_power", false))
    private val darkModeState = mutableStateOf(prefs.getInt("dark_mode", 0))
    /** 色彩风格：0 = AC 粉（B 站粉），1 = Hyper 蓝（Miuix 默认蓝）。 */
    private val colorStyleState = mutableStateOf(prefs.getInt("color_style", 0))
    private val shadowsState = mutableStateOf(prefs.getBoolean("shadows", true))
    private val blurState = mutableStateOf(prefs.getBoolean("blur", true))
    private val danmakuScaleState = mutableStateOf(prefs.getFloat("danmaku_scale", 1f))
    private val danmakuSpeedState = mutableStateOf(prefs.getFloat("danmaku_speed", 1f))
    private val autoRotateState = mutableStateOf(prefs.getBoolean("auto_rotate", true))
    private val immersiveStatusState = mutableStateOf(prefs.getBoolean("immersive_status", false))
    private val notifAskedState = mutableStateOf(prefs.getBoolean("notif_asked", false))
    private val commentFontScaleState =
        mutableStateOf(prefs.getFloat("comment_font_scale", 1.1f))

    /** 硬解模式：0 自动 / 1 硬件解码 / 2 软件解码 / 3 硬件解码（禁止回退）。 */
    private val decoderState = mutableStateOf(prefs.getInt("decoder_mode", 0))
    private val codecPreferenceState = mutableStateOf(prefs.getString("codec_preference", "auto") ?: "auto")
    private val cdnServiceState = mutableStateOf(
        prefs.getString("cdn_service", "base") ?: "base",
    )

    /** 蓝牙耳机延迟补偿：检测到蓝牙输出时，让弹幕按设定延迟对齐。 */
    /** 是否允许后台播放（默认开）。关闭后离开播放器会暂停。 */
    // 弹幕显示设置：全部持久化，重启/换视频都保留
    private val dmOnState = mutableStateOf(prefs.getBoolean("dm_on", true))
    private val dmScaleInlineState = mutableFloatStateOf(prefs.getFloat("dm_scale_inline", 1f))
    private val dmScaleFullscreenState = mutableFloatStateOf(prefs.getFloat("dm_scale_fullscreen", 1f))
    private val dmAreaState = mutableFloatStateOf(prefs.getFloat("dm_area", 1f))
    private val dmSpeedState = mutableFloatStateOf(prefs.getFloat("dm_speed", 1f))
    private val dmAlphaState = mutableFloatStateOf(prefs.getFloat("dm_alpha", 1f))
    /** 弹幕黑色描边宽度（dp），0 表示不描边。 */
    private val dmStrokeState = mutableFloatStateOf(prefs.getFloat("dm_stroke", 1.5f))
    private val dmPortraitGuardState = mutableStateOf(prefs.getBoolean("dm_portrait_guard", false))
    private val dmDedupeState = mutableStateOf(prefs.getBoolean("dm_dedupe", false))
    private val dmSubtitleGuardState = mutableStateOf(prefs.getBoolean("dm_subtitle_guard", false))
    private val dmBlockScrollState = mutableStateOf(prefs.getBoolean("dm_block_scroll", false))
    private val dmBlockFixedState = mutableStateOf(prefs.getBoolean("dm_block_fixed", false))
    private val dmBlockColorState = mutableStateOf(prefs.getBoolean("dm_block_color", false))
    private val dmBlockAdvancedState = mutableStateOf(prefs.getBoolean("dm_block_advanced", false))
    private val dmBlockCountState = mutableStateOf(prefs.getBoolean("dm_block_count", false))
    private val dmKeywordFilterState = mutableStateOf(prefs.getBoolean("dm_keyword_filter", false))
    private val dmKeywordsState = mutableStateOf(
        prefs.getStringSet("dm_keywords", emptySet()).orEmpty().toSet(),
    )

    private val backgroundPlayState = mutableStateOf(prefs.getBoolean("background_play", true))

    /** 进入视频是否自动播放（默认开）。 */
    private val autoPlayState = mutableStateOf(prefs.getBoolean("auto_play", true))

    /** 动态页视频滚到屏幕中心附近时是否自动静音预览播放。 */
    private val dynamicAutoPlayState = mutableStateOf(prefs.getBoolean("dynamic_auto_play", true))

    /** 动态视频自动播放时是否静音（默认静音）。 */
    private val dynamicAutoPlayMutedState =
        mutableStateOf(prefs.getBoolean("dynamic_auto_play_muted", true))

    /** 动态视频自动播放清晰度，默认 720P（64）。 */
    private val dynamicAutoPlayQualityState =
        mutableStateOf(prefs.getInt("dynamic_auto_play_quality", 64))

    private val btLatencyEnabledState = mutableStateOf(prefs.getBoolean("bt_latency_enabled", false))
    private val recommendWebState = mutableStateOf(prefs.getBoolean("recommend_web", false))
    private val showIpLocationState = mutableStateOf(prefs.getBoolean("show_ip_location", true))
    private val showViewerCountState = mutableStateOf(prefs.getBoolean("show_viewer_count", true))
    private val showHotSearchState = mutableStateOf(prefs.getBoolean("show_hot_search", true))
    private val autoCheckUpdateState = mutableStateOf(prefs.getBoolean("auto_check_update", true))
    private val termsAckedVersionState = mutableStateOf(
        prefs.getString("terms_acked_version", "").orEmpty(),
    )
    private val lastSeenVersionState = mutableStateOf(
        prefs.getString("last_seen_version", "").orEmpty(),
    )
    private val splashWallpaperState = mutableStateOf(prefs.getString("splash_wallpaper", "").orEmpty())
    private val loudnessEqState = mutableStateOf(prefs.getBoolean("loudness_eq", false))
    private val resumePlaybackState = mutableStateOf(prefs.getBoolean("resume_playback", true))
    private val gestureBrightnessVolumeState =
        mutableStateOf(prefs.getBoolean("gesture_brightness_volume", true))
    private val gestureCenterFullscreenState =
        mutableStateOf(prefs.getBoolean("gesture_center_fullscreen", true))
    private val gestureBoostSwipeState =
        mutableStateOf(prefs.getBoolean("gesture_boost_swipe", true))
    private val boostDefaultSpeedState =
        mutableFloatStateOf(prefs.getFloat("boost_default_speed", 3f))
    private val showFullscreenTimeState = mutableStateOf(prefs.getBoolean("show_fullscreen_time", true))
    private val showFullscreenActionsState =
        mutableStateOf(prefs.getBoolean("show_fullscreen_actions", true))
    private val btLatencyMsState = mutableStateOf(prefs.getInt("bt_latency_ms", 100))

    /** 「我的」页底部「我的评论与弹幕」入口是否显示（默认显示）。 */
    private val sentHistoryEntryState =
        mutableStateOf(prefs.getBoolean("sent_history_entry", true))

    /** 全局字号倍率（基于当前默认字号，100% 即完全不缩放）。 */
    private val fontScaleState = mutableStateOf(prefs.getFloat("font_scale", 1f))

    /** 全局动效开关（关闭后所有手写补间/弹簧直接落位）。 */
    private val animationsEnabledState = mutableStateOf(prefs.getBoolean("animations_enabled", true))

    init {
        if (!prefs.getBoolean("blur_cpu_v1", false)) {
            blurState.value = true
            prefs.edit()
                .putBoolean("blur", true)
                .putBoolean("blur_pending", false)
                .putBoolean("blur_cpu_v1", true)
                .apply()
        }
        TeakMotion.enabled = animationsEnabledState.value
    }

    var dynamicColor: Boolean
        get() = dynamicColorState.value
        set(value) {
            dynamicColorState.value = value
            prefs.edit().putBoolean("dynamic_color", value).apply()
        }

    private val refractionState = mutableStateOf(prefs.getBoolean("refraction", true))

    /** 玻璃折射（边缘弯折 + 色散），关闭后只保留模糊。 */
    private val previewAckState = mutableStateOf(prefs.getBoolean("preview_notice_acked", false))

    /** 首次启动的预览版说明是否已确认。 */
    var previewNoticeAcked: Boolean
        get() = previewAckState.value
        set(value) {
            previewAckState.value = value
            prefs.edit().putBoolean("preview_notice_acked", value).apply()
        }

    var refractionEnabled: Boolean
        get() = refractionState.value
        set(value) {
            refractionState.value = value
            prefs.edit().putBoolean("refraction", value).apply()
        }

    var smartRefresh: Boolean
        get() = smartRefreshState.value
        set(value) {
            smartRefreshState.value = value
            prefs.edit().putBoolean("smart_refresh", value).apply()
        }

    /** 未开启系统导航条时，把首页底栏和视频页回复框额外抬高。 */
    var bottomBarRaise: Boolean
        get() = bottomBarRaiseState.value
        set(value) {
            bottomBarRaiseState.value = value
            prefs.edit().putBoolean("bottom_bar_raise", value).apply()
        }

    var animationsEnabled: Boolean
        get() = animationsEnabledState.value
        set(value) {
            animationsEnabledState.value = value
            TeakMotion.enabled = value
            prefs.edit().putBoolean("animations_enabled", value).apply()
        }

    var danmakuEnabled: Boolean
        get() = danmakuState.value
        set(value) {
            danmakuState.value = value
            prefs.edit().putBoolean("danmaku", value).apply()
        }

    var qualityPreference: Int
        get() = qualityState.value
        set(value) {
            qualityState.value = value
            prefs.edit().putInt("quality", value).apply()
        }

    var lowPower: Boolean
        get() = lowPowerState.value
        set(value) {
            lowPowerState.value = value
            prefs.edit().putBoolean("low_power", value).apply()
        }

    var darkMode: Int
        get() = darkModeState.value
        set(value) {
            darkModeState.value = value
            prefs.edit().putInt("dark_mode", value).apply()
        }

    var colorStyle: Int
        get() = colorStyleState.value
        set(value) {
            colorStyleState.value = value.coerceIn(0, 1)
            prefs.edit().putInt("color_style", colorStyleState.value).apply()
        }

    var shadowsEnabled: Boolean
        get() = shadowsState.value
        set(value) {
            shadowsState.value = value
            prefs.edit().putBoolean("shadows", value).apply()
        }

    var blurEnabled: Boolean
        get() = blurState.value
        set(value) {
            blurState.value = value
            prefs.edit()
                .putBoolean("blur", value)
                .putBoolean("blur_pending", false)
                .apply()
        }

    /**
     * If the previous process died while blur was armed, disable blur before
     * the first frame so a driver/shader crash cannot brick the app.
     */
    fun prepareBlurSession() {
        // CPU/PixelCopy blur is safe; no driver crash guard needed.
    }

    fun markBlurSessionHealthy() {
        if (prefs.getBoolean("blur_pending", false)) {
            prefs.edit().putBoolean("blur_pending", false).apply()
        }
    }

    var danmakuScale: Float
        get() = danmakuScaleState.value
        set(value) {
            danmakuScaleState.value = value.coerceIn(0.75f, 1.6f)
            prefs.edit().putFloat("danmaku_scale", danmakuScaleState.value).apply()
        }

    var danmakuSpeed: Float
        get() = danmakuSpeedState.value
        set(value) {
            danmakuSpeedState.value = value.coerceIn(0.6f, 1.8f)
            prefs.edit().putFloat("danmaku_speed", danmakuSpeedState.value).apply()
        }

    var autoRotate: Boolean
        get() = autoRotateState.value
        set(value) {
            autoRotateState.value = value
            prefs.edit().putBoolean("auto_rotate", value).apply()
        }

    /** 视频页沉浸状态栏：视频画面延伸到状态栏下方，默认关闭。 */
    var videoImmersiveStatusBar: Boolean
        get() = immersiveStatusState.value
        set(value) {
            immersiveStatusState.value = value
            prefs.edit().putBoolean("immersive_status", value).apply()
        }

    /** 是否已经问过一次通知权限（避免每次进视频都弹）。 */
    /** 评论正文字号倍率：默认 1.1（比原来略大），设置里 0.85~1.6 无极调节。 */
    var commentFontScale: Float
        get() = commentFontScaleState.value
        set(value) {
            commentFontScaleState.value = value.coerceIn(0.85f, 1.6f)
            prefs.edit().putFloat("comment_font_scale", commentFontScaleState.value).apply()
        }

    var notifAsked: Boolean
        get() = notifAskedState.value
        set(value) {
            notifAskedState.value = value
            prefs.edit().putBoolean("notif_asked", value).apply()
        }

    var danmakuOn: Boolean
        get() = dmOnState.value
        set(value) {
            dmOnState.value = value
            prefs.edit().putBoolean("dm_on", value).apply()
        }

    var danmakuScaleInline: Float
        get() = dmScaleInlineState.value
        set(value) {
            dmScaleInlineState.value = value.coerceIn(0.6f, 2f)
            persistFloatEventually("dm_scale_inline", dmScaleInlineState.value)
        }

    var danmakuScaleFullscreen: Float
        get() = dmScaleFullscreenState.value
        set(value) {
            dmScaleFullscreenState.value = value.coerceIn(0.6f, 2f)
            persistFloatEventually("dm_scale_fullscreen", dmScaleFullscreenState.value)
        }

    var danmakuArea: Float
        get() = dmAreaState.value
        set(value) {
            dmAreaState.value = value.coerceIn(0.15f, 1f)
            persistFloatEventually("dm_area", dmAreaState.value)
        }

    var danmakuSpeedPref: Float
        get() = dmSpeedState.value
        set(value) {
            dmSpeedState.value = value.coerceIn(0.5f, 2f)
            persistFloatEventually("dm_speed", dmSpeedState.value)
        }

    var danmakuOpacity: Float
        get() = dmAlphaState.value
        set(value) {
            dmAlphaState.value = value.coerceIn(0.2f, 1f)
            persistFloatEventually("dm_alpha", dmAlphaState.value)
        }

    /** 弹幕描边宽度，单位 dp；无极调节。 */
    var danmakuStroke: Float
        get() = dmStrokeState.value
        set(value) {
            dmStrokeState.value = value.coerceIn(0f, 4f)
            persistFloatEventually("dm_stroke", dmStrokeState.value)
        }

    /** 人像防挡：弹幕避开画面中间的人像区域。 */
    var danmakuPortraitGuard: Boolean
        get() = dmPortraitGuardState.value
        set(value) {
            dmPortraitGuardState.value = value
            prefs.edit().putBoolean("dm_portrait_guard", value).apply()
        }

    var danmakuDedupe: Boolean
        get() = dmDedupeState.value
        set(value) {
            dmDedupeState.value = value
            prefs.edit().putBoolean("dm_dedupe", value).apply()
        }

    var danmakuSubtitleGuard: Boolean
        get() = dmSubtitleGuardState.value
        set(value) {
            dmSubtitleGuardState.value = value
            prefs.edit().putBoolean("dm_subtitle_guard", value).apply()
        }

    var danmakuBlockScroll: Boolean
        get() = dmBlockScrollState.value
        set(value) {
            dmBlockScrollState.value = value
            prefs.edit().putBoolean("dm_block_scroll", value).apply()
        }

    var danmakuBlockFixed: Boolean
        get() = dmBlockFixedState.value
        set(value) {
            dmBlockFixedState.value = value
            prefs.edit().putBoolean("dm_block_fixed", value).apply()
        }

    var danmakuBlockColor: Boolean
        get() = dmBlockColorState.value
        set(value) {
            dmBlockColorState.value = value
            prefs.edit().putBoolean("dm_block_color", value).apply()
        }

    var danmakuBlockAdvanced: Boolean
        get() = dmBlockAdvancedState.value
        set(value) {
            dmBlockAdvancedState.value = value
            prefs.edit().putBoolean("dm_block_advanced", value).apply()
        }

    var danmakuBlockCount: Boolean
        get() = dmBlockCountState.value
        set(value) {
            dmBlockCountState.value = value
            prefs.edit().putBoolean("dm_block_count", value).apply()
        }

    var danmakuKeywordFilter: Boolean
        get() = dmKeywordFilterState.value
        set(value) {
            dmKeywordFilterState.value = value
            prefs.edit().putBoolean("dm_keyword_filter", value).apply()
        }

    var danmakuKeywords: Set<String>
        get() = dmKeywordsState.value
        set(value) {
            dmKeywordsState.value = value
            prefs.edit().putStringSet("dm_keywords", value).apply()
        }

    var backgroundPlay: Boolean
        get() = backgroundPlayState.value
        set(value) {
            backgroundPlayState.value = value
            prefs.edit().putBoolean("background_play", value).apply()
        }

    var autoPlay: Boolean
        get() = autoPlayState.value
        set(value) {
            autoPlayState.value = value
            prefs.edit().putBoolean("auto_play", value).apply()
        }

    var dynamicAutoPlay: Boolean
        get() = dynamicAutoPlayState.value
        set(value) {
            dynamicAutoPlayState.value = value
            prefs.edit().putBoolean("dynamic_auto_play", value).apply()
        }

    var dynamicAutoPlayMuted: Boolean
        get() = dynamicAutoPlayMutedState.value
        set(value) {
            dynamicAutoPlayMutedState.value = value
            prefs.edit().putBoolean("dynamic_auto_play_muted", value).apply()
        }

    var dynamicAutoPlayQuality: Int
        get() = dynamicAutoPlayQualityState.value
        set(value) {
            dynamicAutoPlayQualityState.value = value
            prefs.edit().putInt("dynamic_auto_play_quality", value).apply()
        }

    var bluetoothLatencyEnabled: Boolean
        get() = btLatencyEnabledState.value
        set(value) {
            btLatencyEnabledState.value = value
            prefs.edit().putBoolean("bt_latency_enabled", value).apply()
        }

    /** 首页推荐：true = Web 端推荐，false = App 端推荐。 */
    var recommendWeb: Boolean
        get() = recommendWebState.value
        set(value) {
            recommendWebState.value = value
            prefs.edit().putBoolean("recommend_web", value).apply()
        }

    var showIpLocation: Boolean
        get() = showIpLocationState.value
        set(value) {
            showIpLocationState.value = value
            prefs.edit().putBoolean("show_ip_location", value).apply()
        }

    var showViewerCount: Boolean
        get() = showViewerCountState.value
        set(value) {
            showViewerCountState.value = value
            prefs.edit().putBoolean("show_viewer_count", value).apply()
        }

    var showHotSearch: Boolean
        get() = showHotSearchState.value
        set(value) {
            showHotSearchState.value = value
            prefs.edit().putBoolean("show_hot_search", value).apply()
        }

    var autoCheckUpdate: Boolean
        get() = autoCheckUpdateState.value
        set(value) {
            autoCheckUpdateState.value = value
            prefs.edit().putBoolean("auto_check_update", value).apply()
        }

    /** 已同意使用须知的版本；版本变化后重新提示一次。 */
    var termsAckedVersion: String
        get() = termsAckedVersionState.value
        set(value) {
            termsAckedVersionState.value = value
            prefs.edit().putString("terms_acked_version", value).apply()
        }

    /** 上次启动时看到的版本，用于安装新版本后提示查看更新日志。 */
    var lastSeenVersion: String
        get() = lastSeenVersionState.value
        set(value) {
            lastSeenVersionState.value = value
            prefs.edit().putString("last_seen_version", value).apply()
        }

    var splashWallpaper: String
        get() = splashWallpaperState.value
        set(value) {
            splashWallpaperState.value = value
            prefs.edit().putString("splash_wallpaper", value).apply()
        }

    var loudnessEq: Boolean
        get() = loudnessEqState.value
        set(value) {
            loudnessEqState.value = value
            prefs.edit().putBoolean("loudness_eq", value).apply()
        }

    var resumePlayback: Boolean
        get() = resumePlaybackState.value
        set(value) {
            resumePlaybackState.value = value
            prefs.edit().putBoolean("resume_playback", value).apply()
        }

    var gestureBrightnessVolume: Boolean
        get() = gestureBrightnessVolumeState.value
        set(value) {
            gestureBrightnessVolumeState.value = value
            prefs.edit().putBoolean("gesture_brightness_volume", value).apply()
        }

    /** 在画面中间上下划动切换全屏。 */
    var gestureCenterFullscreen: Boolean
        get() = gestureCenterFullscreenState.value
        set(value) {
            gestureCenterFullscreenState.value = value
            prefs.edit().putBoolean("gesture_center_fullscreen", value).apply()
        }

    /** 长按加速时左右滑动调节倍速。 */
    var gestureBoostSwipe: Boolean
        get() = gestureBoostSwipeState.value
        set(value) {
            gestureBoostSwipeState.value = value
            prefs.edit().putBoolean("gesture_boost_swipe", value).apply()
        }

    /** 长按加速的默认倍率。 */
    var boostDefaultSpeed: Float
        get() = boostDefaultSpeedState.value.coerceIn(1f, 5f)
        set(value) {
            boostDefaultSpeedState.value = value.coerceIn(1f, 5f)
            prefs.edit().putFloat("boost_default_speed", boostDefaultSpeedState.value).apply()
        }

    var showFullscreenTime: Boolean
        get() = showFullscreenTimeState.value
        set(value) {
            showFullscreenTimeState.value = value
            prefs.edit().putBoolean("show_fullscreen_time", value).apply()
        }

    var showFullscreenActions: Boolean
        get() = showFullscreenActionsState.value
        set(value) {
            showFullscreenActionsState.value = value
            prefs.edit().putBoolean("show_fullscreen_actions", value).apply()
        }

    var bluetoothLatencyMs: Int
        get() = btLatencyMsState.value
        set(value) {
            btLatencyMsState.value = value.coerceIn(50, 300)
            prefs.edit().putInt("bt_latency_ms", btLatencyMsState.value).apply()
        }

    var sentHistoryEntry: Boolean
        get() = sentHistoryEntryState.value
        set(value) {
            sentHistoryEntryState.value = value
            prefs.edit().putBoolean("sent_history_entry", value).apply()
        }

    var fontScale: Float
        get() = fontScaleState.value
        set(value) {
            fontScaleState.value = value.coerceIn(0.8f, 1.2f)
            prefs.edit().putFloat("font_scale", fontScaleState.value).apply()
        }

    var decoderMode: Int
        get() = decoderState.value.coerceIn(0, 3)
        set(value) {
            decoderState.value = value.coerceIn(0, 3)
            prefs.edit().putInt("decoder_mode", decoderState.value).apply()
        }

    /** 首选视频编码：auto / avc / hevc / av1 / dvh1。 */
    var codecPreference: String
        get() = codecPreferenceState.value
        set(value) {
            codecPreferenceState.value = value
            prefs.edit().putString("codec_preference", value).apply()
        }

    /** 视频 CDN 服务 id；auto 表示自动测速选择，base 表示不替换。 */
    var cdnService: String
        get() = cdnServiceState.value
        set(value) {
            cdnServiceState.value = value
            prefs.edit().putString("cdn_service", value).apply()
        }

    val searchHistory: List<String>
        get() = prefs.getStringSet("search_history", emptySet()).orEmpty().toList()

    fun addSearchHistory(keyword: String) {
        if (keyword.isBlank()) return
        val next = (listOf(keyword) + searchHistory.filterNot { it == keyword }).take(12)
        prefs.edit().putStringSet("search_history", next.toSet()).apply()
    }

    fun clearSearchHistory() {
        prefs.edit().remove("search_history").apply()
    }

    fun isDynamicRead(mid: Long): Boolean =
        prefs.getStringSet("dynamic_read", emptySet()).orEmpty().contains(mid.toString())

    fun markDynamicRead(mid: Long) {
        val current = prefs.getStringSet("dynamic_read", emptySet()).orEmpty().toMutableSet()
        current.add(mid.toString())
        prefs.edit().putStringSet("dynamic_read", current).apply()
    }
}
