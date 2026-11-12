package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.VideoDetail

/** 已有 BV/AV 入口读取契约；取消继续抛出，其他失败通过 Outcome 返回。 */
interface VideoEntryRepository {
    suspend fun byBvid(bvid: String): Outcome<VideoDetail>
    suspend fun byAid(aid: Long): Outcome<VideoDetail>
}
