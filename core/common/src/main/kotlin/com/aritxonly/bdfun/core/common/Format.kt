package com.aritxonly.bdfun.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 播放量/粉丝数一类的紧凑数字：1.2 万 / 3.4 亿。 */
fun formatCount(value: Long): String = when {
    value >= 100_000_000 -> "%.1f 亿".format(value / 100_000_000.0)
    value >= 10_000 -> "%.1f 万".format(value / 10_000.0)
    else -> value.toString()
}

/** 视频时长：mm:ss 或 h:mm:ss。 */
fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// SimpleDateFormat 不是线程安全的，按线程缓存一份，避免每条记录都新建。
private val dateFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
}
private val dateTimeFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
}

/** 秒级时间戳 → yyyy-MM-dd。 */
fun formatDate(epochSeconds: Long): String =
    dateFormatter.get()!!.format(Date(epochSeconds * 1000))

/** 秒级时间戳 → yyyy-MM-dd HH:mm（详情页显示具体发布时间）。 */
fun formatDateTime(epochSeconds: Long): String =
    dateTimeFormatter.get()!!.format(Date(epochSeconds * 1000))
