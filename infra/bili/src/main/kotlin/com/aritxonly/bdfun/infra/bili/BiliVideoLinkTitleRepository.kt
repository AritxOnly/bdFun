package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.VideoLinkPolicy
import com.aritxonly.bdfun.core.domain.VideoLinkTitleRepository
import com.aritxonly.bdfun.core.domain.VideoTitleTarget
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BiliVideoLinkTitleRepository(
    private val loadTitle: suspend (String) -> String,
    private val resolveAid: suspend (Long) -> String,
    private val resolveShort: (String) -> String? = { resolveVideoLink(it) },
) : VideoLinkTitleRepository {
    override suspend fun resolve(token: String) = withContext(Dispatchers.IO) { biliOutcome {
        val bvid = when (val target = VideoLinkPolicy.titleTarget(token)) {
            is VideoTitleTarget.Bvid -> target.value
            is VideoTitleTarget.Aid -> resolveAid(target.value)
            is VideoTitleTarget.Short -> resolveShort(target.value)
            VideoTitleTarget.Unsupported -> null
        }
        if (bvid.isNullOrBlank()) null else {
            val title = loadTitle(bvid)
            if (title.isBlank()) null else ResolvedVideoLink(bvid, title)
        }
    } }
}
