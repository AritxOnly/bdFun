package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.infra.bili.BiliParameters.image
import com.aritxonly.bdfun.infra.bili.BiliParameters.avatar
import com.aritxonly.bdfun.infra.bili.BiliParameters.parseCountText
import com.aritxonly.bdfun.infra.bili.BiliParameters.parseDurationText
import com.aritxonly.bdfun.infra.bili.cancellableCatching

internal fun parseDynamicVideo(archive: JSONObject?, ugcSeason: JSONObject?, author: JSONObject): VideoItem? {
    return archive?.let {
        VideoItem(
            bvid = it.s("bvid"),
            aid = it.l("aid"),
            title = it.s("title"),
            cover = image(it.s("cover"), 480),
            ownerName = author.s("name"),
            ownerFace = avatar(author.s("face"), 120),
            ownerMid = author.l("mid"),
            play = parseCountText(it.optJSONObject("stat")?.optString("play").orEmpty()),
            danmaku = parseCountText(it.optJSONObject("stat")?.optString("danmaku").orEmpty()),
            duration = parseDurationText(it.s("duration_text")),
            pubdate = 0,
            cid = 0,
        )
    } ?: ugcSeason?.let {
        VideoItem(
            bvid = it.s("bvid"),
            aid = it.l("aid"),
            title = it.s("title"),
            cover = image(it.s("cover"), 480),
            ownerName = author.s("name"),
            ownerFace = avatar(author.s("face"), 120),
            ownerMid = author.l("mid"),
            play = parseCountText(it.optJSONObject("stat")?.optString("play").orEmpty()),
            danmaku = parseCountText(it.optJSONObject("stat")?.optString("danmaku").orEmpty()),
            duration = parseDurationText(it.s("duration_text")),
            pubdate = 0,
            cid = 0,
        )
    }
}
