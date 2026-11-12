package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.decodeSharedVideo
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import org.json.JSONObject

internal fun mapFeedUnreadCounts(data: JSONObject): Map<String, Int> =
    listOf("reply", "at", "like", "sys_msg").associateWith { data.optInt(it).coerceAtLeast(0) }

internal fun mapTotalUnread(privateData: JSONObject, feed: JSONObject): Int =
    listOf("follow_unread", "unfollow_unread").sumOf { privateData.optInt(it).coerceAtLeast(0) } +
        listOf("at", "like", "reply", "sys_msg").sumOf { feed.optInt(it).coerceAtLeast(0) }
