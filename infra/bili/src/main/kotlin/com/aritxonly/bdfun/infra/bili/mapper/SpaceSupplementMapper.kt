package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject

internal fun readSpaceBrief(data: JSONObject): Pair<String, String>? {
    val name = data.s("name")
    return if (name.isBlank()) null else name to avatar(data.s("face"), 120)
}

internal fun readSpaceIpLocation(data: JSONObject): String = data.s("ip_location")
internal fun readSpaceVideoCount(data: JSONObject): Long = data.l("video")
internal fun readSpaceLikeCount(data: JSONObject?): Long = data?.l("likes") ?: 0L
