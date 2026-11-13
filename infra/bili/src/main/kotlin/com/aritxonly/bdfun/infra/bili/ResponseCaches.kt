package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.video.SponsorSegment

/** 私信会话名字/头像的小缓存，避免每页重复请求同一批用户。 */
internal class MessageUserCache {
    private val MAX = 200
    private val map = object : LinkedHashMap<Long, Pair<String, String>>(MAX, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Long, Pair<String, String>>?,
        ): Boolean = size > MAX
    }

    @Synchronized
    fun get(mid: Long): Pair<String, String>? = map[mid]

    @Synchronized
    fun put(mid: Long, name: String, avatar: String) {
        if (mid > 0L && name.isNotBlank()) map[mid] = name to avatar
    }
}

internal class SponsorSegmentCache {
    private val values = object : LinkedHashMap<String, List<SponsorSegment>>(100, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<SponsorSegment>>?,
        ): Boolean = size > 100
    }

    @Synchronized
    fun get(bvid: String): List<SponsorSegment>? = values[bvid]

    @Synchronized
    fun put(bvid: String, list: List<SponsorSegment>) {
        values[bvid] = list
    }
}

