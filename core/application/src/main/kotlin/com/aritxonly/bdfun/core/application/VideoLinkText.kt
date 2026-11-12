package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.videoLinkTokens
import com.aritxonly.bdfun.core.domain.isVideoLinkToken
import com.aritxonly.bdfun.core.common.UrlPattern
import com.aritxonly.bdfun.core.common.trimUrlTrailing
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink

val videoTextUrls get() = UrlPattern
fun trimVideoTextToken(raw: String) = trimUrlTrailing(raw)
fun videoTextTokens(text: String) = videoLinkTokens(text)
fun isBiliVideoTextToken(token: String) = isVideoLinkToken(token)
fun displayVideoTextToken(token: String, resolved: Map<String, ResolvedVideoLink>) = resolved[token]?.title ?: token
fun videoTextClickBvid(token: String, resolved: Map<String, ResolvedVideoLink>): String? = resolved[token]?.bvid
    ?: if (token.startsWith("BV", ignoreCase = true)) "BV" + token.substring(2) else null
