package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.infra.bili.BiliParameters.image
import com.aritxonly.bdfun.infra.bili.BiliParameters.avatar
import com.aritxonly.bdfun.infra.bili.BiliParameters.parseCountText
import com.aritxonly.bdfun.infra.bili.BiliParameters.parseDurationText
import com.aritxonly.bdfun.infra.bili.cancellableCatching

internal fun parseDynamicLiveRecommendation(liveRcmd: JSONObject?): DynamicLiveRecommendation? {
    val liveInfo = liveRcmd?.s("content")?.takeIf { it.isNotBlank() }
        ?.let { cancellableCatching { JSONObject(it).optJSONObject("live_play_info") }.getOrNull() }
    return liveInfo?.let {
        DynamicLiveRecommendation(
            roomId = it.l("room_id"), title = it.s("title"), cover = image(it.s("cover"), 720),
            live = it.optInt("live_status") == 1, areaName = it.s("area_name"),
            watched = it.optJSONObject("watched_show")?.s("text_large").orEmpty(),
        )
    }
}
