package com.aritxonly.bdfun.infra.bili

import android.net.Uri
import com.aritxonly.bdfun.core.domain.CdnPolicy
import com.aritxonly.bdfun.core.domain.CdnUrlParts

typealias CdnService = com.aritxonly.bdfun.core.model.playback.CdnService

private val cdnPolicy = CdnPolicy(
    inspect = { url -> runCatching {
        val uri = Uri.parse(url)
        CdnUrlParts(uri.host.orEmpty(), uri.path.orEmpty())
    }.getOrNull() },
    replaceHost = { url, host ->
        val uri = Uri.parse(url)
        if (uri.host.isNullOrBlank()) url else uri.buildUpon().authority(host).build().toString()
    },
)

fun CdnService.rewrite(url: String, backupUrls: List<String> = emptyList()): String =
    cdnPolicy.rewrite(this, url, backupUrls)
fun isMcdnResource(url: String): Boolean = cdnPolicy.isMcdnResource(url)

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

