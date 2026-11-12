package com.aritxonly.bdfun.core.application

import java.util.concurrent.CopyOnWriteArrayList

/** 暂停跨越页面作用域；新旧 Activity 同时存活时仍要同时停住，保持原有退出所有权。 */
class PlaybackPauseRegistry {
    private data class Registration(val owner: Any?, val pause: (Boolean) -> Unit)
    private val hooks = CopyOnWriteArrayList<Registration>()
    fun register(owner: Any?, hook: (Boolean) -> Unit) {
        if (hooks.none { it.pause === hook }) hooks.add(Registration(owner, hook))
    }
    fun unregister(hook: (Boolean) -> Unit) { hooks.removeAll { it.pause === hook } }
    fun pauseFor(owner: Any) { hooks.filter { it.owner === owner }.forEach { it.pause(false) } }
    fun pauseAll(allowResume: Boolean = true) { hooks.toList().forEach { runCatching { it.pause(allowResume) } } }
}
