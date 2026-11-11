package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.playback.CdnService

data class CdnUrlParts(val host: String, val path: String)

/** 把平台 URL 解析隔离在适配器外，避免域规则依赖 Android Uri。 */
class CdnPolicy(
    private val inspect: (String) -> CdnUrlParts?,
    private val replaceHost: (String, String) -> String,
) {
    fun rewrite(service: CdnService, url: String, backups: List<String>): String {
        if (service.useBackup) return backups.firstOrNull { it.isNotBlank() } ?: url
        if (url.isBlank()) return backups.firstOrNull { it.isNotBlank() }.orEmpty()
        val host = service.host
        val portable = bestPortableUrl(url, backups)
        if (service.auto && host == null) return portable ?: url
        if (host == null) return url
        return portable?.let { replaceHost(it, host) } ?: url
    }

    fun isMcdnResource(url: String): Boolean {
        val parts = inspect(url) ?: return false
        val host = parts.host.lowercase()
        return SUFFIXES.any { host.endsWith(it) } && RESOURCE.matches(parts.path)
    }

    private fun bestPortableUrl(url: String, backups: List<String>): String? {
        val candidates = (listOf(url) + backups).filter {
            it.isNotBlank() && inspect(it)?.path?.contains("/upgcxcode/") == true && !isMcdnResource(it)
        }
        return candidates.firstOrNull { inspect(it)?.host?.startsWith("upos-") == true } ?: candidates.firstOrNull()
    }

    private companion object {
        val SUFFIXES = listOf(".mcdn.bilivideo.cn", ".mcdn.bilivideo.com", ".mcdn.bilivideo.net")
        val RESOURCE = Regex("^/v\\d+/resource(/.*)?$")
    }
}
