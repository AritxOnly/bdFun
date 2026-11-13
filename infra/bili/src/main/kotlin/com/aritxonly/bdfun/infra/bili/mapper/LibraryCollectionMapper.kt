package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.domain.relationNextPage
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun mapFavoriteFolderIds(root: JSONObject): Set<Long> {
    val data = root.optJSONArray("data") ?: root.optJSONObject("data")?.optJSONArray("list")
    val out = mutableSetOf<Long>()
    data?.let { arr ->
        for (i in 0 until arr.length()) {
            val item = arr.opt(i)
            when (item) {
                is Number -> out.add(item.toLong())
                is JSONObject -> {
                    val id = item.l("id").takeIf { it > 0 } ?: item.l("media_id")
                    if (id > 0) out.add(id)
                }
            }
        }
    }
    return out
}

internal fun mapFavoriteFolders(data: JSONObject): List<FavoriteFolder> =
    data.optJSONArray("list").objects().map { folder ->
        FavoriteFolder(
            id = folder.l("id"),
            title = folder.s("title"),
            count = folder.optInt("media_count", 0),
            cover = image(folder.s("cover"), 300),
        )
    }

internal fun mapFavoriteVideos(data: JSONObject): Pair<List<VideoItem>, Boolean> {
    val medias = data.optJSONArray("medias").objects()
    val videos = medias.mapNotNull { media ->
        val bvid = media.s("bvid")
        if (bvid.isBlank()) return@mapNotNull null
        val upper = media.optJSONObject("upper")
        val cnt = media.optJSONObject("cnt_info")
        VideoItem(
            bvid = bvid,
            aid = media.l("id"),
            title = media.s("title"),
            cover = image(media.s("cover"), 420),
            ownerName = upper?.s("name").orEmpty(),
            ownerFace = "",
            ownerMid = upper?.l("mid") ?: 0,
            play = cnt?.l("play") ?: 0,
            danmaku = cnt?.l("danmaku") ?: 0,
            duration = media.optInt("duration", 0),
            pubdate = media.l("pubtime"),
        )
    }
    return videos to data.optBoolean("has_more", medias.size >= 40)
}
