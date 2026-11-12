package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import com.aritxonly.bdfun.core.application.VideoPageStateHolder
import com.aritxonly.bdfun.core.application.PlaybackPauseRegistry
import kotlinx.coroutines.*

internal val LocalVideoPageState = staticCompositionLocalOf<VideoPagePresentationState> {
    error("视频与评论页面必须提供自己的 VideoPagePresentationState")
}

/** Compose 只投影页面 StateFlow；图片与短生命周期回调留在表现层。 */
internal class VideoPagePresentationState(
    scope: CoroutineScope,
    private val pauses: PlaybackPauseRegistry,
    val readCache: com.aritxonly.bdfun.feature.video.application.VideoReadCacheScope,
    val automaticQuality: com.aritxonly.bdfun.core.domain.AutomaticVideoQualitySelector,
    val settings: com.aritxonly.bdfun.core.domain.SettingsRepository,
    private val holder: VideoPageStateHolder = VideoPageStateHolder(),
) {
    private var snapshot by mutableStateOf(holder.state.value)
    init { scope.launch(start = CoroutineStart.UNDISPATCHED) { holder.state.collect { snapshot = holder.state.value } } }
    private fun update(transform: (com.aritxonly.bdfun.core.application.VideoPageState) -> com.aritxonly.bdfun.core.application.VideoPageState) {
        holder.update(transform)
        snapshot = holder.state.value
    }
    var aid: Long
        get() = snapshot.aid
        set(value) = update { it.copy(
            identity = it.identity.copy(
                aid = value,
            ),
        ) }
    var cid: Long
        get() = snapshot.cid
        set(value) = update { it.copy(
            identity = it.identity.copy(
                cid = value,
            ),
        ) }
    var currentBvid: String
        get() = snapshot.currentBvid
        set(value) = update { it.copy(
            identity = it.identity.copy(
                currentBvid = value,
            ),
        ) }
    var autoNextRequest: Int
        get() = snapshot.autoNextRequest
        set(value) = update { it.copy(
            requests = it.requests.copy(
                autoNextRequest = value,
            ),
        ) }
    var danmakuOn: Boolean
        get() = snapshot.danmakuOn
        set(value) = update { it.copy(
            player = it.player.copy(
                danmakuOn = value,
            ),
        ) }
    var playerLiftPx: Float
        get() = snapshot.playerLiftPx
        set(value) = update { it.copy(
            player = it.player.copy(
                playerLiftPx = value,
            ),
        ) }
    var playerBottomPx: Float
        get() = snapshot.playerBottomPx
        set(value) = update { it.copy(
            player = it.player.copy(
                playerBottomPx = value,
            ),
        ) }
    var danmakuInputClose: Int
        get() = snapshot.danmakuInputClose
        set(value) = update { it.copy(
            requests = it.requests.copy(
                danmakuInputClose = value,
            ),
        ) }
    var danmakuProgressMs: Long
        get() = snapshot.danmakuProgressMs
        set(value) = update { it.copy(
            player = it.player.copy(
                danmakuProgressMs = value,
            ),
        ) }
    var replyEditing: Boolean
        get() = snapshot.replyEditing
        set(value) = update { it.copy(
            reply = it.reply.copy(
                replyEditing = value,
            ),
        ) }
    var requestReplyFocus: Int
        get() = snapshot.requestReplyFocus
        set(value) = update { it.copy(
            requests = it.requests.copy(
                requestReplyFocus = value,
            ),
        ) }
    var consumedReplyFocus: Int
        get() = snapshot.consumedReplyFocus
        set(value) = update { it.copy(
            requests = it.requests.copy(
                consumedReplyFocus = value,
            ),
        ) }
    var dismissReply: Int
        get() = snapshot.dismissReply
        set(value) = update { it.copy(
            requests = it.requests.copy(
                dismissReply = value,
            ),
        ) }
    var consumedDismissReply: Int
        get() = snapshot.consumedDismissReply
        set(value) = update { it.copy(
            requests = it.requests.copy(
                consumedDismissReply = value,
            ),
        ) }
    var commentMenuOpen: Boolean
        get() = snapshot.commentMenuOpen
        set(value) = update { it.copy(
            reply = it.reply.copy(
                commentMenuOpen = value,
            ),
        ) }
    var commentThreadVisible: Boolean
        get() = snapshot.commentThreadVisible
        set(value) = update { it.copy(
            reply = it.reply.copy(
                commentThreadVisible = value,
            ),
        ) }
    var fullscreen: Boolean
        get() = snapshot.fullscreen
        set(value) = update { it.copy(
            player = it.player.copy(
                fullscreen = value,
            ),
        ) }
    var controlsVisible: Boolean
        get() = snapshot.controlsVisible
        set(value) = update { it.copy(
            player = it.player.copy(
                controlsVisible = value,
            ),
        ) }
    var danmakuPanelOpen: Boolean
        get() = snapshot.danmakuPanelOpen
        set(value) = update { it.copy(
            player = it.player.copy(
                danmakuPanelOpen = value,
            ),
        ) }
    var danmakuInteract: Int
        get() = snapshot.danmakuInteract
        set(value) = update { it.copy(
            requests = it.requests.copy(
                danmakuInteract = value,
            ),
        ) }
    var orientationLocked: Boolean
        get() = snapshot.orientationLocked
        set(value) = update { it.copy(
            player = it.player.copy(
                orientationLocked = value,
            ),
        ) }
    var replyName: String?
        get() = snapshot.replyName
        set(value) = update { it.copy(
            reply = it.reply.copy(
                replyName = value,
            ),
        ) }
    var replyRoot: Long
        get() = snapshot.replyRoot
        set(value) = update { it.copy(
            reply = it.reply.copy(
                replyRoot = value,
            ),
        ) }
    var replyParent: Long
        get() = snapshot.replyParent
        set(value) = update { it.copy(
            reply = it.reply.copy(
                replyParent = value,
            ),
        ) }
    var panelBackdrop by mutableStateOf<ImageBitmap?>(null)
    var commentSender: (suspend (String, String?) -> Boolean)? = null
    var danmakuSender: (suspend (String, Int, Int, Int) -> Boolean)? = null
    var playerCollapseScroll: ((Float) -> Float)? = null
    val playerUi get() = this
    val orientation get() = Orientation(this)
    val replyTarget get() = Reply(this)
    fun requestAutoNext() { autoNextRequest++ }
    fun registerPause(owner: Any?, hook: (Boolean) -> Unit) = pauses.register(owner, hook)
    fun unregisterPause(hook: (Boolean) -> Unit) = pauses.unregister(hook)
    fun pauseFor(owner: Any) = pauses.pauseFor(owner)
    fun pauseAll(allowResume: Boolean = true) = pauses.pauseAll(allowResume)

    class Orientation(private val page: VideoPagePresentationState) {
        var locked: Boolean
            get() = page.orientationLocked
            set(value) { page.orientationLocked = value }
    }
    class Reply(private val page: VideoPagePresentationState) {
        var name: String?
            get() = page.replyName
            set(value) { page.replyName = value }
        var root: Long
            get() = page.replyRoot
            set(value) { page.replyRoot = value }
        var parent: Long
            get() = page.replyParent
            set(value) { page.replyParent = value }
        fun set(name: String, root: Long, parent: Long) {
            page.update { it.copy(
                reply = it.reply.copy(
                    replyName = name,
                    replyRoot = root,
                    replyParent = parent,
                ),
            ) }
        }
        fun clear() { page.update { it.copy(
            reply = it.reply.copy(
                replyName = null,
                replyRoot = 0L,
                replyParent = 0L,
            ),
        ) } }
    }
}
