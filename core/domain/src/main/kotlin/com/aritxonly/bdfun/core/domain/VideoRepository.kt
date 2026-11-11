package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem

/** 失败通过 Outcome.Failure 返回；取消协程必须继续抛出，避免误显示为网络错误。 */
interface VideoRepository {
    suspend fun videoDetail(bvid: String): Outcome<VideoDetail>
    suspend fun related(bvid: String): Outcome<List<VideoItem>>
}
