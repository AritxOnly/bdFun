package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.comment.EmoteItem

interface EmoteRepository { suspend fun replyEmotes(): Outcome<List<EmoteItem>> }
