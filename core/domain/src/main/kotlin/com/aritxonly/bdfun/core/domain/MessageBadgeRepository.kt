package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome

fun interface MessageBadgeRepository {
    suspend fun unreadCount(): Outcome<Int>
}
