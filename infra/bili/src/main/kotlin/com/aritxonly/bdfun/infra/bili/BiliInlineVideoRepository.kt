package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.InlineVideoPreferences
import com.aritxonly.bdfun.core.domain.InlineVideoRepository
import com.aritxonly.bdfun.core.model.video.PlayInfo
import com.aritxonly.bdfun.core.model.video.VideoDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** 进程容器持有两份各 12 项的缓存；保留原 BV 键与淘汰方式，不合并并发请求。 */
class BiliInlineVideoRepository(
    private val loadDetail: suspend (String) -> VideoDetail,
    private val loadPlayInfo: suspend (VideoDetail, InlineVideoPreferences) -> PlayInfo,
) : InlineVideoRepository {
    private val details = ConcurrentHashMap<String, VideoDetail>()
    private val playInfos = ConcurrentHashMap<String, PlayInfo>()

    override fun cachedPlayInfo(bvid: String) = playInfos[bvid]

    override suspend fun load(bvid: String, preferences: () -> InlineVideoPreferences) = withContext(Dispatchers.IO) {
        biliOutcome {
            val detail = details[bvid] ?: loadDetail(bvid).also { put(details, bvid, it) }
            playInfos[bvid] ?: loadPlayInfo(detail, preferences()).also { put(playInfos, bvid, it) }
        }
    }

    private fun <T> put(map: MutableMap<String, T>, bvid: String, value: T) {
        map[bvid] = value
        while (map.size > 12) {
            val first = map.keys.firstOrNull() ?: break
            map.remove(first)
        }
    }
}
