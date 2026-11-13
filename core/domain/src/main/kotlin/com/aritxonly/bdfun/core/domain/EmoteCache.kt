package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.comment.EmoteItem

data class CachedEmotes(val items: List<EmoteItem>, val savedAt: Long)

interface EmoteCache {
    suspend fun read(accountId: Long): Outcome<CachedEmotes?>
    suspend fun write(accountId: Long, snapshot: CachedEmotes): Outcome<Unit>
}
