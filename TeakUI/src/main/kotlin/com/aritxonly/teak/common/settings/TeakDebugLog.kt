package com.aritxonly.teak.common.settings

/**
 * TeakUI 模块不能直接依赖 App 的日志实现，这里留一个很小的回调口。
 * App 启动时把 sink 接到 LogCollector，调试包就能把菜单事件写进「日志抓取」。
 */
object TeakDebugLog {
    /** 只有 debug 包会打开；release 里所有埋点直接短路。 */
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var sink: ((tag: String, message: String) -> Unit)? = null

    fun record(tag: String, message: String) {
        if (!enabled) return
        runCatching { sink?.invoke(tag, message) }
    }
}
