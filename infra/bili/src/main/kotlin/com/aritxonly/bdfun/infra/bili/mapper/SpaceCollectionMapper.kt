package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun mapSpaceCollections(data: JSONObject): List<SpaceSeason> {
    val lists = data.optJSONObject("items_lists")
    val out = ArrayList<SpaceSeason>()
    lists?.optJSONArray("seasons_list")?.objects().orEmpty().forEach { item ->
        val meta = item.optJSONObject("meta") ?: return@forEach
        val id = meta.l("season_id")
        val title = meta.s("name").ifBlank { meta.s("title") }
        val cover = image(meta.s("cover"), 480)
        if (id <= 0L || title.isBlank() || cover.isBlank()) return@forEach
        out += SpaceSeason(
            id = id,
            title = title,
            cover = cover,
            total = meta.optInt("total", 0),
            isSeries = false,
        )
    }
    lists?.optJSONArray("series_list")?.objects().orEmpty().forEach { item ->
        val meta = item.optJSONObject("meta") ?: return@forEach
        val id = meta.l("series_id")
        val title = meta.s("name").ifBlank { meta.s("title") }
        val cover = image(meta.s("cover"), 480)
        if (id <= 0L || title.isBlank() || cover.isBlank()) return@forEach
        out += SpaceSeason(
            id = id,
            title = title,
            cover = cover,
            total = meta.optInt("total", 0),
            isSeries = true,
        )
    }
    return out
}

internal fun mapSpaceArchives(data: JSONObject, mid: Long): List<VideoItem> =
    data.optJSONArray("archives").objects().mapNotNull { archive ->
        val bvid = archive.s("bvid")
        if (bvid.isBlank()) return@mapNotNull null
        val stat = archive.optJSONObject("stat")
        VideoItem(
            bvid = bvid,
            aid = archive.l("aid"),
            title = archive.s("title"),
            cover = image(archive.s("pic"), 480),
            ownerName = "",
            ownerFace = "",
            ownerMid = mid,
            play = stat?.l("view") ?: archive.l("play"),
            danmaku = stat?.l("danmaku") ?: 0L,
            duration = archive.optInt("duration", 0),
            pubdate = archive.l("pubdate"),
            extras = VideoItemExtras(
                cid = archive.l("cid"),
            ),
        )
    }
