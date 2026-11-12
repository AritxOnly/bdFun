package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.EmoteRepository

class BiliEmoteRepository(private val backend: BiliBackend) : EmoteRepository {
    override suspend fun replyEmotes() = biliOutcome { backend.replyEmotes() }
}
