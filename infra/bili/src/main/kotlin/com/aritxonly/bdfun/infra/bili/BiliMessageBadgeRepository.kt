package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.MessageBadgeRepository

class BiliMessageBadgeRepository(private val load: suspend () -> Int) : MessageBadgeRepository {
    override suspend fun unreadCount() = biliOutcome { load() }
}
