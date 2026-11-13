package com.aritxonly.bdfun

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal fun sameChatDay(a: Long, b: Long): Boolean {
    if (a <= 0L || b <= 0L) return a == b
    val ca = Calendar.getInstance().apply { timeInMillis = a * 1000L }
    val cb = Calendar.getInstance().apply { timeInMillis = b * 1000L }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

// 按线程缓存，避免每条聊天记录都新建 formatter。
private val chatTimeThisYear = ThreadLocal.withInitial {
    SimpleDateFormat("M月d日 HH:mm", Locale.CHINA)
}
private val chatTimeOtherYear = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.CHINA)
}

internal fun formatChatTime(seconds: Long): String {
    if (seconds <= 0L) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = seconds * 1000L }
    val formatter = if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)) {
        chatTimeThisYear.get()!!
    } else {
        chatTimeOtherYear.get()!!
    }
    return formatter.format(Date(seconds * 1000L))
}
