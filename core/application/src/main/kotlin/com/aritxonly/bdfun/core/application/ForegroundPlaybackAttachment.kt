package com.aritxonly.bdfun.core.application

/** 页面仍存活时挂接进程播放桥；owner避免旧页面注销新会话。 */
interface ForegroundPlaybackAttachment {
    fun updateFullscreen(owner: Any, value: Boolean)
    fun attach(token: Any, playback: PlaybackController, actions: Actions)
    fun detach(token: Any)

    interface Actions {
        fun next()
        fun previous()
        fun togglePlayPause()
        fun boostStart()
        fun boostEnd()
        fun seekTo(ms: Long)
        fun exit()
    }
}
