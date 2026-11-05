package com.aritxonly.bdfun.data

import android.net.Uri

/** 与 PiliPlus 对齐的 CDN 选项；host 为 null 表示使用接口原始地址 / 备用地址。 */
data class CdnService(
    val id: String,
    val label: String,
    val host: String? = null,
    val useBackup: Boolean = false,
    val auto: Boolean = false,
) {
    fun rewrite(url: String, backupUrls: List<String> = emptyList()): String {
        if (useBackup) return backupUrls.firstOrNull { it.isNotBlank() } ?: url
        if (url.isBlank()) return backupUrls.firstOrNull { it.isNotBlank() }.orEmpty()
        val targetHost = host
        val portable = bestPortableUrl(url, backupUrls)
        if (auto && targetHost == null) {
            // 自动模式还没测出结果时，优先用 B 站给的备用地址，避免直接连 mcdn 的 8082 端口。
            return portable ?: url
        }
        if (targetHost == null) return url
        // mcdn 地址是“指定仓库”的取货单，换 host 会被拒绝（403）；没有可用备用地址时保持原样。
        return portable?.let { replaceHost(it, targetHost) } ?: url
    }
}

private val McdnHostSuffixes = listOf(
    ".mcdn.bilivideo.cn",
    ".mcdn.bilivideo.com",
    ".mcdn.bilivideo.net",
)
private val McdnPath = Regex("^/v\\d+/resource(/.*)?$")

/** mcdn 的 `/vN/resource/...` 地址只接受 mcdn 前端，不能换到 upos 节点。 */
internal fun isMcdnResource(url: String): Boolean = runCatching {
    val uri = Uri.parse(url)
    val host = uri.host.orEmpty().lowercase()
    McdnHostSuffixes.any { host.endsWith(it) } && McdnPath.matches(uri.path.orEmpty())
}.getOrDefault(false)

private fun isUpgcxcode(url: String): Boolean =
    runCatching { Uri.parse(url).path.orEmpty().contains("/upgcxcode/") }.getOrDefault(false)

private fun isUposHost(url: String): Boolean =
    runCatching { Uri.parse(url).host.orEmpty().startsWith("upos-") }.getOrDefault(false)

/**
 * 在 baseUrl + backupUrl 里挑一个“可以安全换 host”的地址。
 * 优先 upos 节点，其次是 B 站给的备份节点；完全找不到就返回 null。
 */
private fun bestPortableUrl(url: String, backupUrls: List<String>): String? {
    val candidates = (listOf(url) + backupUrls)
        .filter { it.isNotBlank() && isUpgcxcode(it) && !isMcdnResource(it) }
    return candidates.firstOrNull(::isUposHost) ?: candidates.firstOrNull()
}

private fun replaceHost(url: String, targetHost: String): String {
    val uri = Uri.parse(url)
    if (uri.host.isNullOrBlank()) return url
    return uri.buildUpon().authority(targetHost).build().toString()
}

val CdnServices = listOf(
    CdnService("auto", "自动", auto = true),
    CdnService("base", "原地址"),
    CdnService("backup", "备用URL", useBackup = true),
    CdnService("ali", "ali（阿里云）", "upos-sz-mirrorali.bilivideo.com"),
    CdnService("alib", "alib（阿里云）", "upos-sz-mirroralib.bilivideo.com"),
    CdnService("alio1", "alio1（阿里云）", "upos-sz-mirroralio1.bilivideo.com"),
    CdnService("cos", "cos（腾讯云）", "upos-sz-mirrorcos.bilivideo.com"),
    CdnService("cosb", "cosb（腾讯云，VOD加速类型）", "upos-sz-mirrorcosb.bilivideo.com"),
    CdnService("coso1", "coso1（腾讯云）", "upos-sz-mirrorcoso1.bilivideo.com"),
    CdnService("hw", "hw（华为云，融合CDN）", "upos-sz-mirrorhw.bilivideo.com"),
    CdnService("hwb", "hwb（华为云，融合CDN）", "upos-sz-mirrorhwb.bilivideo.com"),
    CdnService("hwo1", "hwo1（华为云，融合CDN）", "upos-sz-mirrorhwo1.bilivideo.com"),
    CdnService("08c", "08c（华为云，融合CDN）", "upos-sz-mirror08c.bilivideo.com"),
    CdnService("08h", "08h（华为云，融合CDN）", "upos-sz-mirror08h.bilivideo.com"),
    CdnService("08ct", "08ct（华为云，融合CDN）", "upos-sz-mirror08ct.bilivideo.com"),
    CdnService("tf_hw", "tf_hw（华为云）", "upos-tf-all-hw.bilivideo.com"),
    CdnService("tf_tx", "tf_tx（腾讯云）", "upos-tf-all-tx.bilivideo.com"),
    CdnService("akamai", "akamai（Akamai海外）", "upos-hz-mirrorakam.akamaized.net"),
    CdnService("aliov", "aliov（阿里云海外）", "upos-sz-mirroraliov.bilivideo.com"),
    CdnService("cosov", "cosov（腾讯云海外）", "upos-sz-mirrorcosov.bilivideo.com"),
    CdnService("hwov", "hwov（华为云海外）", "upos-sz-mirrorhwov.bilivideo.com"),
    CdnService("hk_bcache", "hk_bcache（Bilibili海外）", "cn-hk-eq-bcache-01.bilivideo.com"),
)

fun cdnServiceById(id: String): CdnService {
    if (id == CdnAutoSelector.ID) {
        // 自动模式还没测出结果时先用备用地址，拿到测速结果后再切到最佳节点。
        return CdnAutoSelector.currentService() ?: CdnServices.first()
    }
    return CdnServices.firstOrNull { it.id == id } ?: CdnServices.first()
}
