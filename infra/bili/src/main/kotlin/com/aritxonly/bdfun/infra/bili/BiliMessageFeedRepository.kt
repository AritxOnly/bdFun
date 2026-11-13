package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.MessageFeedRepository
import com.aritxonly.bdfun.core.model.message.MessageCursor

class BiliMessageFeedRepository(private val backend: BiliBackend) : MessageFeedRepository {
    override suspend fun page(type: Int, cursor: MessageCursor?) = biliOutcome { backend.messagesPage(type, cursor) }
    override suspend fun followers(page: Int) = biliOutcome { backend.newFollowers(page) }
    override suspend fun follow(mid: Long, follow: Boolean) = biliOutcome { backend.followUser(mid, follow) }
    override suspend fun dynamic(id: String) = biliOutcome { backend.dynamicDetail(id) }
    override suspend fun delete(type: Int?, id: String) = biliOutcome {
        if (type == null) backend.deleteSystemMessage(id) else backend.deleteFeedMessage(type, id)
        Unit
    }
    override suspend fun notice(id: String, enabled: Boolean) = biliOutcome { backend.setLikeNotice(id, enabled); Unit }
}
