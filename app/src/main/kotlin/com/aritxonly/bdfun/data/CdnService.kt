package com.aritxonly.bdfun.data

import android.net.Uri

/** 与 PiliPlus 对齐的 CDN 选项；host 为 null 表示使用接口原始地址 / 备用地址。 */
data class CdnService(
    val id: String,
    val label: String,
    val host: String? = null,
    val useBackup: Boolean = false,
) {
    fun rewrite(url: String, backupUrls: List<String> = emptyList()): String {
        if (useBackup) return backupUrls.firstOrNull { it.isNotBlank() } ?: url
        val targetHost = host ?: return url
        val uri = Uri.parse(url)
        if (uri.host.isNullOrBlank()) return url
        return uri.buildUpon().authority(targetHost).build().toString()
    }
}

val CdnServices = listOf(
    CdnService("base", "基础URL"),
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
        // 自动模式还没测出结果时先用基础地址，拿到测速结果后自动切到最佳节点。
        return CdnAutoSelector.currentService() ?: CdnServices.first()
    }
    return CdnServices.firstOrNull { it.id == id } ?: CdnServices.first()
}
