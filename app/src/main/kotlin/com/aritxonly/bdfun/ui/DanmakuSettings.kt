package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.AppGraph

/**
 * 弹幕显示设置：全部持久化在 [com.aritxonly.bdfun.data.SettingsStore]，
 * 重启 App 或切换视频后都会保留，不会重置。
 */
object DanmakuSettings {
    private val store get() = AppGraph.settings

    var enabled: Boolean
        get() = store.danmakuOn
        set(value) {
            store.danmakuOn = value
        }

    /** 半屏字号倍率。 */
    var scaleInline: Float
        get() = store.danmakuScaleInline
        set(value) {
            store.danmakuScaleInline = value
        }

    /** 全屏字号倍率（与半屏分开）。 */
    var scaleFullscreen: Float
        get() = store.danmakuScaleFullscreen
        set(value) {
            store.danmakuScaleFullscreen = value
        }

    var speed: Float
        get() = store.danmakuSpeedPref
        set(value) {
            store.danmakuSpeedPref = value
        }

    /** 弹幕铺满的高度比例：0.15（一行左右）~ 1.0（全屏）。 */
    var area: Float
        get() = store.danmakuArea
        set(value) {
            store.danmakuArea = value
        }

    /** 弹幕层整体透明度 20%~100%。 */
    var opacity: Float
        get() = store.danmakuOpacity
        set(value) {
            store.danmakuOpacity = value
        }

    /** 黑色描边宽度（dp），0 为关闭。 */
    var stroke: Float
        get() = store.danmakuStroke
        set(value) {
            store.danmakuStroke = value
        }

    /** 人像防挡：弹幕避开画面中间的人像区域。 */
    var portraitGuard: Boolean
        get() = store.danmakuPortraitGuard
        set(value) {
            store.danmakuPortraitGuard = value
        }

    /** 相同文本只保留第一条。 */
    var dedupe: Boolean
        get() = store.danmakuDedupe
        set(value) {
            store.danmakuDedupe = value
        }

    /** 底部字幕防挡：底部约 18% 不排弹幕。 */
    var subtitleGuard: Boolean
        get() = store.danmakuSubtitleGuard
        set(value) {
            store.danmakuSubtitleGuard = value
        }

    var blockScroll: Boolean
        get() = store.danmakuBlockScroll
        set(value) { store.danmakuBlockScroll = value }

    var blockFixed: Boolean
        get() = store.danmakuBlockFixed
        set(value) { store.danmakuBlockFixed = value }

    var blockColor: Boolean
        get() = store.danmakuBlockColor
        set(value) { store.danmakuBlockColor = value }

    var blockAdvanced: Boolean
        get() = store.danmakuBlockAdvanced
        set(value) { store.danmakuBlockAdvanced = value }

    var blockCount: Boolean
        get() = store.danmakuBlockCount
        set(value) { store.danmakuBlockCount = value }

    var keywordFilter: Boolean
        get() = store.danmakuKeywordFilter
        set(value) { store.danmakuKeywordFilter = value }

    var keywords: Set<String>
        get() = store.danmakuKeywords
        set(value) { store.danmakuKeywords = value }

    /** 恢复弹幕设置的默认值（「重置弹幕设置」）。 */
    fun resetDefaults() {
        enabled = true
        scaleInline = 1f
        scaleFullscreen = 1f
        speed = 1f
        area = 1f
        opacity = 1f
        stroke = 1.5f
        dedupe = false
        subtitleGuard = false
        portraitGuard = false
        blockScroll = false
        blockFixed = false
        blockColor = false
        blockAdvanced = false
        blockCount = false
        keywordFilter = false
        keywords = emptySet()
    }
}
