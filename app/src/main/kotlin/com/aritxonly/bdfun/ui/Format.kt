package com.aritxonly.bdfun.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 播放量/粉丝数一类的紧凑数字：1.2 万 / 3.4 亿。 */
internal fun formatCount(value: Long): String = when {
    value >= 100_000_000 -> "%.1f 亿".format(value / 100_000_000.0)
    value >= 10_000 -> "%.1f 万".format(value / 10_000.0)
    else -> value.toString()
}

/** 视频时长：mm:ss 或 h:mm:ss。 */
internal fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** 秒级时间戳 → yyyy-MM-dd。 */
internal fun formatDate(epochSeconds: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date(epochSeconds * 1000))

/** 秒级时间戳 → yyyy-MM-dd HH:mm（详情页显示具体发布时间）。 */
internal fun formatDateTime(epochSeconds: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(epochSeconds * 1000))
