package com.aritxonly.bdfun.core.domain

sealed interface VideoLinkResolution {
    data class Resolved(val bvid: String) : VideoLinkResolution
    data object Unresolved : VideoLinkResolution
}

/** 沿用外部入口的静默失败：不支持/重定向失败返回Unresolved，协程取消继续抛出。 */
fun interface VideoLinkResolver {
    suspend fun resolve(text: String): VideoLinkResolution
}
