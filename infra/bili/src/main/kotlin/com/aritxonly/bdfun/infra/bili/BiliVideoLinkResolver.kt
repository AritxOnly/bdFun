package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.VideoLinkResolution
import com.aritxonly.bdfun.core.domain.VideoLinkResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 线程边界留在基础设施，原识别与HttpURLConnection重定向规则不变。 */
class BiliVideoLinkResolver : VideoLinkResolver {
    override suspend fun resolve(text: String): VideoLinkResolution = withContext(Dispatchers.IO) {
        resolveVideoLink(text)?.let { VideoLinkResolution.Resolved(it) } ?: VideoLinkResolution.Unresolved
    }
}
