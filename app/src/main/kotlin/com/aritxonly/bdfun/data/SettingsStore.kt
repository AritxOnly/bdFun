package com.aritxonly.bdfun.data

import android.content.Context
import com.aritxonly.bdfun.core.domain.SettingsRepository
import com.aritxonly.bdfun.feature.settings.application.SettingsStateHolder
import com.aritxonly.bdfun.feature.settings.presentation.SettingsPresentationStateHolder
import com.aritxonly.bdfun.infra.storage.AndroidSettingsPreferences
import com.aritxonly.bdfun.infra.storage.JsonSearchHistoryCodec
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlinx.coroutines.*

typealias ScrollTopPages = com.aritxonly.bdfun.core.model.settings.ScrollTopPages

/** 迁移兼容门面：旧页面逐字段观察，新页面使用 repository 和 state。 */
class SettingsStore(context: Context) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller = SettingsStateHolder.create(AndroidSettingsPreferences(context), JsonSearchHistoryCodec(), scope)
    val repository: SettingsRepository get() = controller
    val state get() = controller.state
    private val projection = SettingsPresentationStateHolder(scope, state) { TeakMotion.enabled = controller.animationsEnabled }
    var dynamicColor: Boolean by projection.property("dynamicColorState", { controller.dynamicColor }, { controller.dynamicColor = it })
    var previewNoticeAcked: Boolean by projection.property("previewAckState", { controller.previewNoticeAcked }, { controller.previewNoticeAcked = it })
    var refractionEnabled: Boolean by projection.property("refractionState", { controller.refractionEnabled }, { controller.refractionEnabled = it })
    var smartRefresh: Boolean by projection.property("smartRefreshState", { controller.smartRefresh }, { controller.smartRefresh = it })
    var bottomBarRaise: Boolean by projection.property("bottomBarRaiseState", { controller.bottomBarRaise }, { controller.bottomBarRaise = it })
    var animationsEnabled: Boolean by projection.property("animationsEnabledState", { controller.animationsEnabled }, { controller.animationsEnabled = it })
    var danmakuEnabled: Boolean by projection.property("danmakuState", { controller.danmakuEnabled }, { controller.danmakuEnabled = it })
    var qualityPreference: Int by projection.property("qualityState", { controller.qualityPreference }, { controller.qualityPreference = it })
    var lowPower: Boolean by projection.property("lowPowerState", { controller.lowPower }, { controller.lowPower = it })
    var darkMode: Int by projection.property("darkModeState", { controller.darkMode }, { controller.darkMode = it })
    var colorStyle: Int by projection.property("colorStyleState", { controller.colorStyle }, { controller.colorStyle = it })
    var interfaceStyle: Int by projection.property("interfaceStyleState", { controller.interfaceStyle }, { controller.interfaceStyle = it })
    var homeCardStyle: Int by projection.property("homeCardStyleState", { controller.homeCardStyle }, { controller.homeCardStyle = it })
    var shadowsEnabled: Boolean by projection.property("shadowsState", { controller.shadowsEnabled }, { controller.shadowsEnabled = it })
    var scrollTopEnabled: Boolean by projection.property("scrollTopEnabledState", { controller.scrollTopEnabled }, { controller.scrollTopEnabled = it })
    var scrollTopSide: Int by projection.property("scrollTopSideState", { controller.scrollTopSide }, { controller.scrollTopSide = it })
    var scrollTopPages: Set<String> by projection.property("scrollTopPagesState", { controller.scrollTopPages }, { controller.scrollTopPages = it })
    var blurEnabled: Boolean by projection.property("blurState", { controller.blurEnabled }, { controller.blurEnabled = it })
    var danmakuScale: Float by projection.property("danmakuScaleState", { controller.danmakuScale }, { controller.danmakuScale = it })
    var danmakuSpeed: Float by projection.property("danmakuSpeedState", { controller.danmakuSpeed }, { controller.danmakuSpeed = it })
    var autoRotate: Boolean by projection.property("autoRotateState", { controller.autoRotate }, { controller.autoRotate = it })
    var videoImmersiveStatusBar: Boolean by projection.property("immersiveStatusState", { controller.videoImmersiveStatusBar }, { controller.videoImmersiveStatusBar = it })
    var commentFontScale: Float by projection.property("commentFontScaleState", { controller.commentFontScale }, { controller.commentFontScale = it })
    var notifAsked: Boolean by projection.property("notifAskedState", { controller.notifAsked }, { controller.notifAsked = it })
    var danmakuOn: Boolean by projection.property("dmOnState", { controller.danmakuOn }, { controller.danmakuOn = it })
    var danmakuScaleInline: Float by projection.property("dmScaleInlineState", { controller.danmakuScaleInline }, { controller.danmakuScaleInline = it })
    var danmakuScaleFullscreen: Float by projection.property("dmScaleFullscreenState", { controller.danmakuScaleFullscreen }, { controller.danmakuScaleFullscreen = it })
    var danmakuArea: Float by projection.property("dmAreaState", { controller.danmakuArea }, { controller.danmakuArea = it })
    var danmakuSpeedPref: Float by projection.property("dmSpeedState", { controller.danmakuSpeedPref }, { controller.danmakuSpeedPref = it })
    var danmakuOpacity: Float by projection.property("dmAlphaState", { controller.danmakuOpacity }, { controller.danmakuOpacity = it })
    var danmakuStroke: Float by projection.property("dmStrokeState", { controller.danmakuStroke }, { controller.danmakuStroke = it })
    var danmakuLowRefresh: Boolean by projection.property("dmLowRefreshState", { controller.danmakuLowRefresh }, { controller.danmakuLowRefresh = it })
    var danmakuPortraitGuard: Boolean by projection.property("dmPortraitGuardState", { controller.danmakuPortraitGuard }, { controller.danmakuPortraitGuard = it })
    var danmakuDedupe: Boolean by projection.property("dmDedupeState", { controller.danmakuDedupe }, { controller.danmakuDedupe = it })
    var danmakuSubtitleGuard: Boolean by projection.property("dmSubtitleGuardState", { controller.danmakuSubtitleGuard }, { controller.danmakuSubtitleGuard = it })
    var danmakuBlockScroll: Boolean by projection.property("dmBlockScrollState", { controller.danmakuBlockScroll }, { controller.danmakuBlockScroll = it })
    var danmakuBlockFixed: Boolean by projection.property("dmBlockFixedState", { controller.danmakuBlockFixed }, { controller.danmakuBlockFixed = it })
    var danmakuBlockColor: Boolean by projection.property("dmBlockColorState", { controller.danmakuBlockColor }, { controller.danmakuBlockColor = it })
    var danmakuBlockAdvanced: Boolean by projection.property("dmBlockAdvancedState", { controller.danmakuBlockAdvanced }, { controller.danmakuBlockAdvanced = it })
    var danmakuBlockCount: Boolean by projection.property("dmBlockCountState", { controller.danmakuBlockCount }, { controller.danmakuBlockCount = it })
    var danmakuKeywordFilter: Boolean by projection.property("dmKeywordFilterState", { controller.danmakuKeywordFilter }, { controller.danmakuKeywordFilter = it })
    var danmakuKeywords: Set<String> by projection.property("dmKeywordsState", { controller.danmakuKeywords }, { controller.danmakuKeywords = it })
    var backgroundPlay: Boolean by projection.property("backgroundPlayState", { controller.backgroundPlay }, { controller.backgroundPlay = it })
    var stopOnVideoPageExit: Boolean by projection.property("stopOnVideoPageExitState", { controller.stopOnVideoPageExit }, { controller.stopOnVideoPageExit = it })
    var showDiscAfterVideo: Boolean by projection.property("showDiscAfterVideoState", { controller.showDiscAfterVideo }, { controller.showDiscAfterVideo = it })
    var exitVideoControl: Int by projection.property("exitVideoControlState", { controller.exitVideoControl }, { controller.exitVideoControl = it })
    var discBreathEnabled: Boolean by projection.property("discBreathEnabledState", { controller.discBreathEnabled }, { controller.discBreathEnabled = it })
    var discBreathCoverColor: Boolean by projection.property("discBreathCoverColorState", { controller.discBreathCoverColor }, { controller.discBreathCoverColor = it })
    var autoPlay: Boolean by projection.property("autoPlayState", { controller.autoPlay }, { controller.autoPlay = it })
    var autoPlayNext: Boolean by projection.property("autoPlayNextState", { controller.autoPlayNext }, { controller.autoPlayNext = it })
    var dynamicAutoPlay: Boolean by projection.property("dynamicAutoPlayState", { controller.dynamicAutoPlay }, { controller.dynamicAutoPlay = it })
    var dynamicAutoPlayMuted: Boolean by projection.property("dynamicAutoPlayMutedState", { controller.dynamicAutoPlayMuted }, { controller.dynamicAutoPlayMuted = it })
    var dynamicAutoPlayQuality: Int by projection.property("dynamicAutoPlayQualityState", { controller.dynamicAutoPlayQuality }, { controller.dynamicAutoPlayQuality = it })
    var bluetoothLatencyEnabled: Boolean by projection.property("btLatencyEnabledState", { controller.bluetoothLatencyEnabled }, { controller.bluetoothLatencyEnabled = it })
    var recommendWeb: Boolean by projection.property("recommendWebState", { controller.recommendWeb }, { controller.recommendWeb = it })
    var showIpLocation: Boolean by projection.property("showIpLocationState", { controller.showIpLocation }, { controller.showIpLocation = it })
    var showViewerCount: Boolean by projection.property("showViewerCountState", { controller.showViewerCount }, { controller.showViewerCount = it })
    var showHotSearch: Boolean by projection.property("showHotSearchState", { controller.showHotSearch }, { controller.showHotSearch = it })
    var autoCheckUpdate: Boolean by projection.property("autoCheckUpdateState", { controller.autoCheckUpdate }, { controller.autoCheckUpdate = it })
    var onboardingAcked: Boolean by projection.property("onboardingAcked", { controller.onboardingAcked }, { controller.onboardingAcked = it })
    var termsAckedVersion: String by projection.property("termsAckedVersionState", { controller.termsAckedVersion }, { controller.termsAckedVersion = it })
    var lastSeenVersion: String by projection.property("lastSeenVersionState", { controller.lastSeenVersion }, { controller.lastSeenVersion = it })
    var splashWallpaper: String by projection.property("splashWallpaperState", { controller.splashWallpaper }, { controller.splashWallpaper = it })
    var loudnessEq: Boolean by projection.property("loudnessEqState", { controller.loudnessEq }, { controller.loudnessEq = it })
    var resumePlayback: Boolean by projection.property("resumePlaybackState", { controller.resumePlayback }, { controller.resumePlayback = it })
    var gestureBrightnessVolume: Boolean by projection.property("gestureBrightnessVolumeState", { controller.gestureBrightnessVolume }, { controller.gestureBrightnessVolume = it })
    var gestureCenterFullscreen: Boolean by projection.property("gestureCenterFullscreenState", { controller.gestureCenterFullscreen }, { controller.gestureCenterFullscreen = it })
    var gestureBoostSwipe: Boolean by projection.property("gestureBoostSwipeState", { controller.gestureBoostSwipe }, { controller.gestureBoostSwipe = it })
    var defaultPlaybackSpeed: Float by projection.property("defaultPlaybackSpeedState", { controller.defaultPlaybackSpeed }, { controller.defaultPlaybackSpeed = it })
    var boostDefaultSpeed: Float by projection.property("boostDefaultSpeedState", { controller.boostDefaultSpeed }, { controller.boostDefaultSpeed = it })
    var showFullscreenTime: Boolean by projection.property("showFullscreenTimeState", { controller.showFullscreenTime }, { controller.showFullscreenTime = it })
    var showFullscreenActions: Boolean by projection.property("showFullscreenActionsState", { controller.showFullscreenActions }, { controller.showFullscreenActions = it })
    var progressBarMode: Int by projection.property("progressBarModeState", { controller.progressBarMode }, { controller.progressBarMode = it })
    var sponsorSkipEnabled: Boolean by projection.property("sponsorSkipState", { controller.sponsorSkipEnabled }, { controller.sponsorSkipEnabled = it })
    var hotDanmakuEnabled: Boolean by projection.property("hotDanmakuState", { controller.hotDanmakuEnabled }, { controller.hotDanmakuEnabled = it })
    var bluetoothLatencyMs: Int by projection.property("btLatencyMsState", { controller.bluetoothLatencyMs }, { controller.bluetoothLatencyMs = it })
    var sentHistoryEntry: Boolean by projection.property("sentHistoryEntryState", { controller.sentHistoryEntry }, { controller.sentHistoryEntry = it })
    var fontScale: Float by projection.property("fontScaleState", { controller.fontScale }, { controller.fontScale = it })
    var decoderMode: Int by projection.property("decoderState", { controller.decoderMode }, { controller.decoderMode = it })
    var codecPreference: String by projection.property("codecPreferenceState", { controller.codecPreference }, { controller.codecPreference = it })
    var cdnService: String by projection.property("cdnServiceState", { controller.cdnService }, { controller.cdnService = it })
    val searchHistory: List<String> by projection.readOnly("searchHistory") { controller.searchHistory }
    fun isScrollTopPageEnabled(page: String): Boolean = controller.isScrollTopPageEnabled(page)
    fun setScrollTopPageEnabled(page: String, enabled: Boolean) { controller.setScrollTopPageEnabled(page, enabled); projection.refresh() }
    fun prepareBlurSession() { controller.prepareBlurSession(); projection.refresh() }
    fun markBlurSessionHealthy() { controller.markBlurSessionHealthy(); projection.refresh() }
    fun addSearchHistory(keyword: String) { controller.addSearchHistory(keyword); projection.refresh() }
    fun clearSearchHistory() { controller.clearSearchHistory(); projection.refresh() }
    override fun close() { controller.flush(); scope.cancel() }
}
