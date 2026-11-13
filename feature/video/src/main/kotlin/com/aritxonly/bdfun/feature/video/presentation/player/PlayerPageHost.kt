package com.aritxonly.bdfun.ui

/** 页面瞬时投影与暂停注册端口；实现继续使用原页面StateFlow与暂停注册表。 */
interface PlayerPageHost {
    val aid: Long
    val cid: Long
    var danmakuProgressMs: Long
    fun requestAutoNext()
    var controlsVisible: Boolean
    var danmakuPanelOpen: Boolean
    val danmakuInteract: Int
    var orientationLocked: Boolean
    fun registerPause(owner: Any?, hook: (Boolean) -> Unit)
    fun unregisterPause(hook: (Boolean) -> Unit)
}

class PlayerDiagnostics(val log: (String, String) -> Unit, val toast: (String) -> Unit)
