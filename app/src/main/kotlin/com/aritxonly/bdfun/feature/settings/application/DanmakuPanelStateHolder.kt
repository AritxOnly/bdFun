package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.DanmakuSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class DanmakuPanelUiState(
    val showKeywords: Boolean = false, val showList: Boolean = false, val keyword: String = "",
)
internal class DanmakuPanelStateHolder(private val settings: DanmakuSettings) {
    private val mutable = MutableStateFlow(DanmakuPanelUiState())
    val state = mutable.asStateFlow()
    fun toggleKeywords() { mutable.update { it.copy(showKeywords = !it.showKeywords) } }
    fun toggleList() { mutable.update { it.copy(showList = !it.showList) } }
    fun changeKeyword(value: String) { mutable.update { it.copy(keyword = value) } }
    fun addKeyword() {
        val value = mutable.value.keyword.trim()
        if (value.isNotBlank()) {
            settings.danmakuKeywords = settings.danmakuKeywords + value
            mutable.update { it.copy(keyword = "") }
        }
    }
    fun clearKeywords() { settings.danmakuKeywords = emptySet() }
    fun reset() { resetDanmakuDefaults(settings); mutable.value = DanmakuPanelUiState() }
}

/** 与旧「重置弹幕设置」逐项一致，不扩展为重置所有弹幕相关设置。 */
internal fun resetDanmakuDefaults(settings: DanmakuSettings) {
    settings.danmakuOn = true
    settings.danmakuScaleInline = 1f
    settings.danmakuScaleFullscreen = 1f
    settings.danmakuSpeedPref = 1f
    settings.danmakuArea = 1f
    settings.danmakuOpacity = 1f
    settings.danmakuStroke = 1.5f
    settings.danmakuLowRefresh = true
    settings.danmakuDedupe = false
    settings.danmakuSubtitleGuard = false
    settings.danmakuPortraitGuard = false
    settings.danmakuBlockScroll = false
    settings.danmakuBlockFixed = false
    settings.danmakuBlockColor = false
    settings.danmakuBlockAdvanced = false
    settings.danmakuBlockCount = false
    settings.danmakuKeywordFilter = false
    settings.danmakuKeywords = emptySet()
}
