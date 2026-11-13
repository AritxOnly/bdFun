package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.domain.relationNextPage
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun mapRelationTargetIds(data: JSONObject): List<Long> =
    data.optJSONArray("list").objects().map { it.l("mid") }.filter { it > 0 }.distinct()

internal fun mapRelationPage(data: JSONObject, relations: JSONObject?, isLoggedIn: () -> Boolean, page: Int): RelationPage {
    val raw = data.optJSONArray("list").objects()
    val items = raw.mapNotNull { item ->
        val id = item.l("mid").takeIf { it > 0 } ?: return@mapNotNull null
        RelationUser(id, item.s("uname"), avatar(item.s("face"), 160), item.s("sign"),
            if (!isLoggedIn()) 0 else relations?.optJSONObject(id.toString())?.optInt("attribute", 0))
    }
    val total = data.optInt("total", 0)
    return RelationPage(items, total, relationNextPage(page, raw.size, total))
}

internal fun mapRelationAttribute(data: JSONObject): Int = data.optInt("attribute", 0)
internal fun mapRelationStat(data: JSONObject): Pair<Long, Long> = data.l("following") to data.l("follower")

internal fun mapFollowings(data: JSONObject): List<FollowedUser> =
    data.optJSONArray("list").objects().map { user ->
        FollowedUser(
            mid = user.l("mid"),
            name = user.s("uname"),
            face = avatar(user.s("face"), 160),
            sign = user.s("sign"),
        )
    }
