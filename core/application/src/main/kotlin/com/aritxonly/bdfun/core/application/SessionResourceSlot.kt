package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.CancellationException

/** revision区分同一账号重新登录，避免A→B→A被StateFlow合并后复用旧状态。 */
data class SessionIdentity(val mid: Long?, val revision: Long)

val SessionState.identity: SessionIdentity get() = SessionIdentity(user?.mid, revision)
val SessionStore.identity: SessionIdentity get() = state.value.identity

/** 单一账号资源槽；读取也核对身份，不依赖异步collector及时收到每次切换。 */
class SessionResourceSlot<T>(
    private val current: () -> SessionIdentity,
    private val create: () -> T,
    private val release: (T) -> Unit,
) : AutoCloseable {
    private data class Entry<T>(val owner: SessionIdentity, val value: T)
    private var entry: Entry<T>? = null
    private var closed = false

    @Synchronized
    fun get(): T {
        check(!closed) { "账号资源槽已关闭" }
        val owner = current()
        entry?.takeIf { it.owner == owner }?.let { return it.value }
        clear()
        val value = create()
        // 工厂回调也可能触发登录/退出；旧身份创建的资源不能交给新页面。
        if (closed || owner != current()) {
            release(value)
            throw CancellationException("登录态已变化")
        }
        entry = Entry(owner, value)
        return value
    }

    @Synchronized
    fun clearStale() { if (entry?.owner != current()) clear() }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        clear()
    }

    private fun clear() {
        val previous = entry
        entry = null
        previous?.let { release(it.value) }
    }
}
