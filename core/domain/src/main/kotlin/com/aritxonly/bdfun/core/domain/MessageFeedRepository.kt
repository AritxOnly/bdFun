package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.model.feed.DynamicItem

interface MessageFeedRepository {
    suspend fun page(type: Int, cursor: MessageCursor?): Outcome<MessageFeedPage>
    suspend fun followers(page: Int): Outcome<List<MessageItem>>
    suspend fun follow(mid: Long, follow: Boolean): Outcome<Boolean>
    suspend fun dynamic(id: String): Outcome<DynamicItem?>
    suspend fun delete(type: Int?, id: String): Outcome<Unit>
    suspend fun notice(id: String, enabled: Boolean): Outcome<Unit>
}
