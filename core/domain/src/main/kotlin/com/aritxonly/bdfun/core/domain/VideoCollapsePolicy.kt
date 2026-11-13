package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.PlayerCollapseMode
import kotlin.math.abs

data class VideoDetailScrollUpdate(
    val nextOffsetPx: Float,
    val consumedDeltaPx: Float,
)

/** 上滑折叠播放器（preScroll）。 */
fun reduceVideoDetailPreScroll(
    currentOffsetPx: Float,
    deltaPx: Float,
    minOffsetPx: Float,
    inlinePortraitScrollEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    layoutAlreadyCollapsed: Boolean = false,
    minUpdateDeltaPx: Float = 0.75f,
): VideoDetailScrollUpdate? {
    if (!inlinePortraitScrollEnabled || isPortraitFullscreen) return null
    if (layoutAlreadyCollapsed) return null
    if (deltaPx >= 0f) return null
    return reduceVideoDetailScrollOffset(
        currentOffsetPx = currentOffsetPx,
        deltaPx = deltaPx,
        minOffsetPx = minOffsetPx,
        minUpdateDeltaPx = minUpdateDeltaPx,
    )
}

/** 下滑展开播放器（postScroll）。 */
fun reduceVideoDetailPostScroll(
    currentOffsetPx: Float,
    deltaPx: Float,
    minOffsetPx: Float,
    inlinePortraitScrollEnabled: Boolean,
    isPortraitFullscreen: Boolean,
    layoutAlreadyCollapsed: Boolean = false,
    minUpdateDeltaPx: Float = 0.75f,
): VideoDetailScrollUpdate? {
    if (!inlinePortraitScrollEnabled || isPortraitFullscreen) return null
    if (layoutAlreadyCollapsed) return null
    if (deltaPx <= 0f) return null
    return reduceVideoDetailScrollOffset(
        currentOffsetPx = currentOffsetPx,
        deltaPx = deltaPx,
        minOffsetPx = minOffsetPx,
        minUpdateDeltaPx = minUpdateDeltaPx,
    )
}

private fun reduceVideoDetailScrollOffset(
    currentOffsetPx: Float,
    deltaPx: Float,
    minOffsetPx: Float,
    maxOffsetPx: Float = 0f,
    minUpdateDeltaPx: Float,
): VideoDetailScrollUpdate? {
    if (abs(deltaPx) < minUpdateDeltaPx) return null
    val nextOffset = (currentOffsetPx + deltaPx).coerceIn(minOffsetPx, maxOffsetPx)
    val consumedDelta = nextOffset - currentOffsetPx
    if (abs(consumedDelta) < minUpdateDeltaPx) return null
    return VideoDetailScrollUpdate(
        nextOffsetPx = nextOffset,
        consumedDeltaPx = consumedDelta,
    )
}

// ---------------------------------------------------------------------------
// PortraitDetailPresentationPolicy（只保留手机竖屏详情用得到的部分）
// ---------------------------------------------------------------------------

/**
 * 暂停时切到简介/评论，播放器要能被手势一直收到 56dp 的播放工具栏。
 * BiliPai 里简介和评论都在 0..1，这里 bdFun 只有两页：0=简介，1=评论。
 */
fun shouldHidePausedInlinePlayerForComments(
    selectedTabIndex: Int,
    isPlaybackPaused: Boolean,
    isCommentThreadVisible: Boolean = false,
): Boolean = isPlaybackPaused && (selectedTabIndex in 0..1 || isCommentThreadVisible)

fun shouldEnableInlinePortraitScrollTransform(
    collapseMode: PlayerCollapseMode,
    selectedTabIndex: Int,
    isVerticalVideo: Boolean,
    isPlaybackPaused: Boolean = false,
    isCommentThreadVisible: Boolean = false,
): Boolean {
    if (shouldHidePausedInlinePlayerForComments(selectedTabIndex, isPlaybackPaused, isCommentThreadVisible)) {
        return true
    }
    if (!collapseMode.enablesVideoOrientation(isVerticalVideo)) return false
    if (collapseMode == PlayerCollapseMode.PAUSED_ONLY && !isPlaybackPaused) return false
    return when (selectedTabIndex) {
        0 -> collapseMode.enablesIntro
        1 -> collapseMode.enablesComment
        else -> true
    }
}

/** 暂停浏览详情时收起画面，可保留固定高度的恢复播放工具栏。 */
fun resolveInlinePlayerCollapsedViewportHeightDp(
    standardCollapsedHeightDp: Float,
    hidePausedPlayerForComments: Boolean,
    retainPlayToolbar: Boolean = false,
): Float = if (hidePausedPlayerForComments) {
    if (retainPlayToolbar) 56f else 0f
} else standardCollapsedHeightDp.coerceAtLeast(0f)

// ---------------------------------------------------------------------------
// PlayerCollapseAutoPausePolicy
// ---------------------------------------------------------------------------

fun resolvePlayerCollapsedForAutoPause(
    swipeHidePlayerEnabled: Boolean,
    playerHeightOffsetPx: Float,
    videoHeightPx: Float,
    compactForCommentTab: Boolean,
    restoreRequested: Boolean = false,
): Boolean {
    if (restoreRequested) return false
    if (compactForCommentTab) return true
    if (!swipeHidePlayerEnabled) return false
    if (videoHeightPx <= 0f) return false
    return playerHeightOffsetPx <= -videoHeightPx + 1f
}

fun shouldAutoPauseOnPlayerCollapse(
    autoPauseEnabled: Boolean,
    isPlayerCollapsed: Boolean,
    isPlaying: Boolean,
    isPortraitFullscreen: Boolean = false,
): Boolean = autoPauseEnabled && isPlayerCollapsed && isPlaying && !isPortraitFullscreen

fun shouldAutoResumeOnPlayerExpand(
    autoPauseEnabled: Boolean,
    isPlayerCollapsed: Boolean,
    wasAutoPausedByCollapse: Boolean,
    isPortraitFullscreen: Boolean = false,
): Boolean = autoPauseEnabled &&
    (!isPlayerCollapsed || isPortraitFullscreen) &&
    wasAutoPausedByCollapse
