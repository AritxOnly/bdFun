package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoPage

/** 前后台共用选集顺序：多 P 优先，接合集其余单集，按 cid 去重。 */
fun videoPlaybackPages(detail: VideoDetail): List<VideoPage> {
    val episodes = detail.episodes.mapIndexed { index, episode ->
        VideoPage(episode.cid, index + 1, episode.title, episode.duration)
    }
    if (detail.pages.size > 1) return (detail.pages + episodes).distinctBy { it.cid }
    if (episodes.size > 1) return episodes
    return detail.pages
}
