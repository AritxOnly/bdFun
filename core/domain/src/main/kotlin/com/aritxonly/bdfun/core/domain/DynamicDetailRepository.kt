package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.feed.DynamicItem

interface DynamicDetailRepository {
    suspend fun detail(id: String): Outcome<DynamicItem?>
    suspend fun userIpLocation(mid: Long): Outcome<String>
    suspend fun like(id: String, liked: Boolean): Outcome<Boolean>
}
