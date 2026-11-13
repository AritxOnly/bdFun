package com.aritxonly.bdfun.feature.video.application

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DanmakuInputState(val text: String = "", val sending: Boolean = false, val failed: Boolean = false)
enum class DanmakuInputResult { Success, Failure, Ignored }
class PendingDanmakuInputSend(val text: String, val sender: suspend (String, Int, Int, Int) -> Boolean) {
    var started = false
}
/** 半屏输入固定滚动/25号/白色；失败保留草稿，关闭后不再响应迟到成功。 */
class DanmakuInputStateHolder {
    private val mutable = MutableStateFlow(DanmakuInputState())
    val state = mutable.asStateFlow()
    private var closed = false
    private var pending: PendingDanmakuInputSend? = null
    fun edit(text: String) { if (!closed) mutable.update { it.copy(text = text) } }
    fun append(text: String) = edit(state.value.text + text)
    fun close() { closed = true; pending = null }
    fun beginSend(sender: (suspend (String, Int, Int, Int) -> Boolean)?): PendingDanmakuInputSend? {
        val value = state.value.text.trim()
        if (closed || value.isEmpty() || state.value.sending) return null
        if (sender == null) { mutable.update { it.copy(failed = true) }; return null }
        mutable.update { it.copy(sending = true, failed = false) }
        return PendingDanmakuInputSend(value, sender).also { pending = it }
    }
    suspend fun completeSend(request: PendingDanmakuInputSend): DanmakuInputResult {
        if (closed || pending !== request || request.started) return DanmakuInputResult.Ignored
        request.started = true
        try {
            val ok = try { request.sender(request.text, 1, 25, 0xFFFFFF) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { false }
            currentCoroutineContext().ensureActive()
            if (closed || pending !== request) return DanmakuInputResult.Ignored
            mutable.update { it.copy(text = if (ok) "" else it.text, failed = !ok) }
            return if (ok) DanmakuInputResult.Success else DanmakuInputResult.Failure
        } finally {
            if (pending === request) {
                pending = null
                if (!closed) mutable.update { it.copy(sending = false) }
            }
        }
    }
}
