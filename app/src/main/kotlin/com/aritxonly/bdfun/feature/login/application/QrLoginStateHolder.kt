package com.aritxonly.bdfun.feature.login.application

import com.aritxonly.bdfun.core.domain.officialAppLoginUri
import com.aritxonly.bdfun.core.domain.QrLoginRepository
import com.aritxonly.bdfun.core.domain.QrLoginResult
import com.aritxonly.bdfun.core.model.user.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QrLoginState(val qr: QrLogin? = null, val displayQr: QrLogin? = null,
    val status: String = "正在获取二维码…", val expired: Boolean = false, val loading: Boolean = true)
data class QrLoginCompletion(val profile: UserProfile?)

/** 轮询由页面 key 作用域持有；关闭或更换二维码后不再发布旧结果。 */
internal class QrLoginStateHolder(private val repository: QrLoginRepository,
    private val waitForPoll: suspend () -> Unit = { delay(1500) }) {
    private val mutable = MutableStateFlow(QrLoginState())
    val state = mutable.asStateFlow()
    private var closed = false
    private var generating = false
    fun close() { closed = true }
    fun officialAppLoginLink(): String? {
        val current = state.value
        if (closed || current.loading || current.expired) return null
        return current.displayQr?.url?.let(::officialAppLoginUri)
    }
    suspend fun generate() {
        if (closed || generating) return
        generating = true
        mutable.update { it.copy(displayQr = null, loading = true, expired = false, status = "正在获取二维码…") }
        try {
            val result = repository.generate()
            currentCoroutineContext().ensureActive()
            if (closed) return
            mutable.update { current -> when (result) {
                is QrLoginResult.Success -> current.copy(qr = result.value, displayQr = result.value,
                    status = "请用哔哩哔哩手机客户端扫码")
                is QrLoginResult.Failure -> current.copy(expired = true,
                    status = "二维码获取失败：${result.message ?: "网络异常"}")
            } }
        } finally {
            generating = false
            if (!closed) mutable.update { it.copy(loading = false) }
        }
    }
    suspend fun poll(key: String): QrLoginCompletion? {
        while (isCurrent(key)) {
            waitForPoll()
            if (!isCurrent(key)) return null
            val result = repository.poll(key)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(key)) return null
            val poll = (result as? QrLoginResult.Success)?.value ?: continue
            when (poll.state) {
                QrState.Success -> return QrLoginCompletion(poll.profile)
                QrState.Scanned -> mutable.update { it.copy(status = "已扫码，请在手机上确认登录") }
                QrState.Expired -> { expire("二维码已过期，请刷新"); return null }
                QrState.Error -> { expire(poll.message.ifBlank { "登录失败" }); return null }
                QrState.Waiting -> Unit
            }
        }
        return null
    }
    private fun isCurrent(key: String) = !closed && state.value.qr?.key == key
    private fun expire(message: String) { mutable.update { it.copy(expired = true, status = message) } }
}
