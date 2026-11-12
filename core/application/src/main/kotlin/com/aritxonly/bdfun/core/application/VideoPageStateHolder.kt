package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VideoPageState(
    val aid: Long = 0L,
    val cid: Long = 0L,
    val currentBvid: String = "",
    val autoNextRequest: Int = 0,
    val danmakuOn: Boolean = true,
    val playerLiftPx: Float = 0f,
    val playerBottomPx: Float = 0f,
    val danmakuInputClose: Int = 0,
    val danmakuProgressMs: Long = 0L,
    val replyEditing: Boolean = false,
    val requestReplyFocus: Int = 0,
    val consumedReplyFocus: Int = 0,
    val dismissReply: Int = 0,
    val consumedDismissReply: Int = 0,
    val commentMenuOpen: Boolean = false,
    val commentThreadVisible: Boolean = false,
    val fullscreen: Boolean = false,
    val controlsVisible: Boolean = false,
    val danmakuPanelOpen: Boolean = false,
    val danmakuInteract: Int = 0,
    val orientationLocked: Boolean = false,
    val replyName: String? = null,
    val replyRoot: Long = 0L,
    val replyParent: Long = 0L,
)

/** 每个详情页独占一份跨层状态，页面叠开时不覆盖旧页面的浮层与回复目标。 */
class VideoPageStateHolder {
    private val mutableState = MutableStateFlow(VideoPageState())
    val state = mutableState.asStateFlow()
    fun update(transform: (VideoPageState) -> VideoPageState) { mutableState.update(transform) }
}
