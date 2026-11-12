package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.trimUrlTrailing

sealed interface VideoTitleTarget {
    data class Bvid(val value: String) : VideoTitleTarget
    data class Aid(val value: Long) : VideoTitleTarget
    data class Short(val value: String) : VideoTitleTarget
    data object Unsupported : VideoTitleTarget
}

/** 保留原入口与正文识别的不同边界，不在迁移中扩大支持范围。 */
object VideoLinkPolicy {
    private val tokenPattern = Regex(
        "(?i)" +
            "(https?://(?:www\\.|m\\.)?bilibili\\.com/video/(?:BV[0-9A-Za-z]{10}|av\\d+)[^\\s\\u3000<>\"']*)" +
            "|(https?://b23\\.tv/[0-9A-Za-z]+)" +
            "|((?<![0-9A-Za-z])BV[0-9A-Za-z]{10})" +
            "|((?<![0-9A-Za-z])av\\d{3,})",
    )

    private val bvRegex = Regex("(?i)BV[0-9A-Za-z]{10}")
    private val shortRegex = Regex("(?i)https?://b23\\.tv/[0-9A-Za-z]+")
    private val videoRegex = Regex("(?i)https?://(?:www\\.|m\\.)?bilibili\\.com/video/[0-9A-Za-z]+")
    fun tokens(text: String) = tokenPattern.findAll(text).map { trimUrlTrailing(it.value) }.distinct().toList()
    fun isToken(token: String) = tokenPattern.matches(token)
    fun recognizes(text: String) = bvRegex.containsMatchIn(text) || shortRegex.containsMatchIn(text) || videoRegex.containsMatchIn(text)
    fun directBvid(text: String): String? = bvRegex.find(text)?.value?.let { "BV" + it.substring(2) }
    fun shortUrl(text: String): String? = shortRegex.find(text)?.value
    fun titleTarget(token: String): VideoTitleTarget = when {
        token.contains("b23.tv") -> VideoTitleTarget.Short(token)
        token.contains("bilibili.com/video/") -> bvRegex.find(token)?.value?.let { VideoTitleTarget.Bvid(it) } ?: VideoTitleTarget.Unsupported
        token.startsWith("BV", ignoreCase = true) -> VideoTitleTarget.Bvid("BV" + token.substring(2))
        token.startsWith("av", ignoreCase = true) -> token.substring(2).toLongOrNull()?.let { VideoTitleTarget.Aid(it) } ?: VideoTitleTarget.Unsupported
        else -> VideoTitleTarget.Unsupported
    }
}
