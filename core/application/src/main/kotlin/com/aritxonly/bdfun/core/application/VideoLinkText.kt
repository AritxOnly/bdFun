package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.UrlPattern
import com.aritxonly.bdfun.core.common.trimUrlTrailing
import com.aritxonly.bdfun.core.domain.VideoLinkPolicy
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink

object VideoLinkText {
    val urls get() = UrlPattern
    fun trim(raw: String) = trimUrlTrailing(raw)
    fun tokens(text: String) = VideoLinkPolicy.tokens(text)
    fun isBiliToken(token: String) = VideoLinkPolicy.isToken(token)
    fun display(token: String, resolved: Map<String, ResolvedVideoLink>) = resolved[token]?.title ?: token
    fun clickBvid(token: String, resolved: Map<String, ResolvedVideoLink>): String? = resolved[token]?.bvid
        ?: if (token.startsWith("BV", ignoreCase = true)) "BV" + token.substring(2) else null
}
