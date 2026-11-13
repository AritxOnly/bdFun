package com.aritxonly.bdfun.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/* ---------------------------------------------------------------- time */

internal fun formatSessionTime(seconds: Long): String {
    if (seconds <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = seconds * 1000L }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val sameDay = sameYear && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (sameDay) return SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(seconds * 1000L))
    now.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    if (yesterday) {
        return "昨天 " + SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(seconds * 1000L))
    }
    return SimpleDateFormat(if (sameYear) "M月d日" else "yyyy年M月d日", Locale.CHINA)
        .format(Date(seconds * 1000L))
}

internal fun formatMessageDate(seconds: Long): String {
    if (seconds <= 0L) return ""
    return SimpleDateFormat("M月d日", Locale.CHINA).format(Date(seconds * 1000L))
}
