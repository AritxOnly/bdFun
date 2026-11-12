package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.infra.bili.BiliParameters.image
import com.aritxonly.bdfun.infra.bili.BiliParameters.avatar
import com.aritxonly.bdfun.infra.bili.BiliParameters.parseCountText
import com.aritxonly.bdfun.infra.bili.BiliParameters.parseDurationText
import com.aritxonly.bdfun.infra.bili.cancellableCatching

internal class DynamicMajor(val value: JSONObject?) {
    val archive = value?.optJSONObject("archive")
    val opus = value?.optJSONObject("opus")
    val article = value?.optJSONObject("article")
    val common = value?.optJSONObject("common")
    val pgc = value?.optJSONObject("pgc")
    val live = value?.optJSONObject("live")
    val liveRcmd = value?.optJSONObject("live_rcmd")
        ?: value?.optJSONObject("subscription_new")?.optJSONObject("live_rcmd")
    val medialist = value?.optJSONObject("medialist")
    val courses = value?.optJSONObject("courses")
    val music = value?.optJSONObject("music")
    val ugcSeason = value?.optJSONObject("ugc_season")
}
