package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.data.PlayerCollapseMode
import kotlin.math.abs

/**
 * 这一整份逻辑照搬 BiliPai：
 * - feature/video/policy/VideoDetailScrollCoordinator.kt
 * - feature/video/screen/VideoDetailInlineCollapseState.kt
 * - feature/video/screen/PortraitDetailPresentationPolicy.kt（相关判定）
 * - feature/video/screen/PlayerCollapseAutoPausePolicy.kt
 *
 * 关键点：播放器的收起不是「位移一个 overlay」，而是共享一段负的 offsetPx，
 * 由它同时推导播放器高度、缩放比例和页面内容的内边距；手势只改 offsetPx。
 */

internal data class VideoDetailScrollUpdate(
    val nextOffsetPx: Float,
    val consumedDeltaPx: Float,
)

/** 上滑折叠播放器（preScroll）。 */
internal fun reduceVideoDetailPreScroll(
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
internal fun reduceVideoDetailPostScroll(
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

/** 播放器随浏览收起的共享位移。负数 = 向上收起。 */
@Stable
internal class InlinePlayerCollapseState(
    initialOffsetPx: Float = 0f,
    initialRestoreRequested: Boolean = false,
) {
    var offsetPx by mutableFloatStateOf(initialOffsetPx)
        private set

    var restoreRequested by mutableStateOf(initialRestoreRequested)
        private set

    /**
     * 切页可能让手势范围变小（暂停时评论页可把播放器隐藏到 56dp）。
     * 共享位移因此被收回到新范围内；这属于布局校正而不是跟手拖动，
     * 高度需要用动画过渡而不是瞬间跳。
     */
    var layoutReconciled by mutableStateOf(false)
        private set

    fun updateOffset(value: Float) {
        offsetPx = value
    }

    fun reset() {
        offsetPx = 0f
        restoreRequested = false
        layoutReconciled = false
    }

    fun reconcileLayout(collapseEnabled: Boolean, collapseRangePx: Float) {
        if (offsetPx >= 0f) return
        if (!collapseEnabled) {
            restore()
            return
        }
        val minOffsetPx = -collapseRangePx.coerceAtLeast(0f)
        if (offsetPx < minOffsetPx) {
            offsetPx = minOffsetPx
            layoutReconciled = true
        }
    }

    fun consumeScroll(update: VideoDetailScrollUpdate?): Float {
        if (update == null || update.consumedDeltaPx == 0f) return 0f
        restoreRequested = false
        layoutReconciled = false
        offsetPx = update.nextOffsetPx
        return update.consumedDeltaPx
    }

    fun restore() {
        offsetPx = 0f
        restoreRequested = true
        layoutReconciled = false
    }

    companion object {
        val Saver = listSaver<InlinePlayerCollapseState, Any>(
            save = { listOf(it.offsetPx, it.restoreRequested) },
            restore = {
                InlinePlayerCollapseState(
                    initialOffsetPx = it[0] as Float,
                    initialRestoreRequested = it[1] as Boolean,
                )
            },
        )
    }
}

@Composable
internal fun rememberInlinePlayerCollapseState(videoBvid: String) =
    rememberSaveable(videoBvid, saver = InlinePlayerCollapseState.Saver) {
        InlinePlayerCollapseState()
    }

// ---------------------------------------------------------------------------
// PortraitDetailPresentationPolicy（只保留手机竖屏详情用得到的部分）
// ---------------------------------------------------------------------------

/**
 * 暂停时切到简介/评论，播放器要能被手势一直收到 56dp 的播放工具栏。
 * BiliPai 里简介和评论都在 0..1，这里 bdFun 只有两页：0=简介，1=评论。
 */
internal fun shouldHidePausedInlinePlayerForComments(
    selectedTabIndex: Int,
    isPlaybackPaused: Boolean,
    isCommentThreadVisible: Boolean = false,
): Boolean = isPlaybackPaused && (selectedTabIndex in 0..1 || isCommentThreadVisible)

internal fun shouldEnableInlinePortraitScrollTransform(
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
internal fun resolveInlinePlayerCollapsedViewportHeightDp(
    standardCollapsedHeightDp: Float,
    hidePausedPlayerForComments: Boolean,
    retainPlayToolbar: Boolean = false,
): Float = if (hidePausedPlayerForComments) {
    if (retainPlayToolbar) 56f else 0f
} else standardCollapsedHeightDp.coerceAtLeast(0f)

// ---------------------------------------------------------------------------
// PlayerCollapseAutoPausePolicy
// ---------------------------------------------------------------------------

internal fun resolvePlayerCollapsedForAutoPause(
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

internal fun shouldAutoPauseOnPlayerCollapse(
    autoPauseEnabled: Boolean,
    isPlayerCollapsed: Boolean,
    isPlaying: Boolean,
    isPortraitFullscreen: Boolean = false,
): Boolean = autoPauseEnabled && isPlayerCollapsed && isPlaying && !isPortraitFullscreen

internal fun shouldAutoResumeOnPlayerExpand(
    autoPauseEnabled: Boolean,
    isPlayerCollapsed: Boolean,
    wasAutoPausedByCollapse: Boolean,
    isPortraitFullscreen: Boolean = false,
): Boolean = autoPauseEnabled &&
    (!isPlayerCollapsed || isPortraitFullscreen) &&
    wasAutoPausedByCollapse
