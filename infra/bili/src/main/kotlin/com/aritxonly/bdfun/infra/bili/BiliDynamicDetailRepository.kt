package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.DynamicDetailRepository
import com.aritxonly.bdfun.core.model.feed.DynamicItem

class BiliDynamicDetailRepository(
    private val loadDetail: suspend (String) -> DynamicItem?,
    private val loadIp: suspend (Long) -> String,
    private val sendLike: suspend (String, Boolean) -> Boolean,
) : DynamicDetailRepository {
    override suspend fun detail(id: String) = biliOutcome { loadDetail(id) }
    override suspend fun userIpLocation(mid: Long) = biliOutcome { loadIp(mid) }
    override suspend fun like(id: String, liked: Boolean) = biliOutcome { sendLike(id, liked) }
}
