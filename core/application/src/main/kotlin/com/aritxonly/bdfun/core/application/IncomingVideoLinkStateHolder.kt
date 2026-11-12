package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.recognizesVideoLink
import com.aritxonly.bdfun.core.domain.VideoLinkResolution
import com.aritxonly.bdfun.core.domain.VideoLinkResolver
import com.aritxonly.bdfun.core.navigation.Route
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 原Activity生命周期内的两个最近原始文本，不跨进程/旋转保存。 */
data class IncomingVideoLinkState(val lastHandledLink: String = "", val lastClipboard: String = "")

class PendingVideoLink internal constructor(val text: String, internal val owner: Any) {
    internal var started = false
}

/** 不同链接保留独立在途请求；不能改成“只允许最新请求导航”。 */
class IncomingVideoLinkStateHolder(private val resolver: VideoLinkResolver) {
    private val mutable = MutableStateFlow(IncomingVideoLinkState())
    val state = mutable.asStateFlow()
    private val owner = Any()
    private var closed = false

    fun begin(text: String): PendingVideoLink? {
        val current = state.value
        if (closed || text.isBlank() || text == current.lastHandledLink) return null
        if (!recognizesVideoLink(text)) return null
        mutable.value = current.copy(lastHandledLink = text)
        return PendingVideoLink(text, owner)
    }
    fun clipboard(text: String): PendingVideoLink? {
        val current = state.value
        if (closed || text.isBlank() || text == current.lastClipboard) return null
        mutable.value = current.copy(lastClipboard = text)
        return if (recognizesVideoLink(text)) begin(text) else null
    }
    suspend fun resolve(request: PendingVideoLink): Route.Video? {
        if (closed || request.owner !== owner || request.started) return null
        request.started = true
        val result = resolver.resolve(request.text)
        currentCoroutineContext().ensureActive()
        if (closed) return null
        return when (result) {
            is VideoLinkResolution.Resolved -> Route.Video(bvid = result.bvid)
            VideoLinkResolution.Unresolved -> null
        }
    }
    fun close() { closed = true }
}
