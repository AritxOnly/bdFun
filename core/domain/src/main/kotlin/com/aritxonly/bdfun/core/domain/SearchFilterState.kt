package com.aritxonly.bdfun.core.domain

/** 搜索详情页的筛选偏好；quality 仍仅记录，不增加服务器参数。 */
data class SearchFilterState(
    val order: Int = 0, val published: Int = 0, val duration: Int = 0, val quality: Int = 0,
) {
    val orderParam: String
        get() = when (order) {
            1 -> "pubdate"
            2 -> "click"
            3 -> "dm"
            else -> "totalrank"
        }
    val durationParam: Int get() = duration

    fun beginSecondsAt(nowMillis: () -> Long): Long? {
        val days = when (published) {
            1 -> 1L
            2 -> 7L
            3 -> 183L
            else -> return null
        }
        return nowMillis() / 1000L - days * 24L * 3600L
    }
}
