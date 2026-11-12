package com.aritxonly.bdfun.data

import com.aritxonly.bdfun.core.model.settings.AUTO_CDN_ID
import com.aritxonly.bdfun.infra.bili.rewrite as infraRewrite

typealias CdnService = com.aritxonly.bdfun.infra.bili.CdnService
val CdnServices get() = com.aritxonly.bdfun.infra.bili.CdnServices
internal fun isMcdnResource(url: String) = com.aritxonly.bdfun.infra.bili.isMcdnResource(url)

fun cdnServiceById(id: String): CdnService {
    if (id == AUTO_CDN_ID) {
        // 自动模式还没测出结果时先用备用地址，拿到测速结果后再切到最佳节点。
        return com.aritxonly.bdfun.AppGraph.currentAutomaticCdn() ?: CdnServices.first()
    }
    return CdnServices.firstOrNull { it.id == id } ?: CdnServices.first()
}

fun CdnService.rewrite(url: String, backupUrls: List<String> = emptyList()) = infraRewrite(url, backupUrls)
