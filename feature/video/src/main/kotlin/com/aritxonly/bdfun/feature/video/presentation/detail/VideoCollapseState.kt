package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.domain.VideoDetailScrollUpdate
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

/** 播放器随浏览收起的共享位移。负数 = 向上收起。 */
@Stable
class InlinePlayerCollapseState(
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
fun rememberInlinePlayerCollapseState(videoBvid: String) =
    rememberSaveable(videoBvid, saver = InlinePlayerCollapseState.Saver) {
        InlinePlayerCollapseState()
    }
