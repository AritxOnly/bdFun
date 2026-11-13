package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.DynamicRepository
import com.aritxonly.bdfun.core.model.feed.*

class BiliDynamicRepository(
    private val page: suspend (String, String, Long?) -> DynamicPage,
    private val portal: suspend () -> DynamicPortal,
    private val upList: suspend (String) -> DynamicUpPage,
    private val like: suspend (String, Boolean) -> Boolean,
) : DynamicRepository {
    override suspend fun dynamicPage(type: String, offset: String, hostMid: Long?) = biliOutcome { page(type, offset, hostMid) }
    override suspend fun dynamicPortal() = biliOutcome { portal() }
    override suspend fun dynamicUpList(offset: String) = biliOutcome { upList(offset) }
    override suspend fun likeDynamic(id: String, like: Boolean) = biliOutcome { like(id, like) }
}
