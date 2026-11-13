package com.aritxonly.bdfun.infra.bili

import android.net.Uri
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.ChatUploadedImage
import com.aritxonly.bdfun.core.domain.PrivateChatRepository
import com.aritxonly.bdfun.core.model.message.MessageSession
import kotlinx.coroutines.CancellationException

class BiliPrivateChatRepository(private val backend: BiliBackend) : PrivateChatRepository {
    override suspend fun page(session: MessageSession, before: Long?) = privateChatOutcome("历史消息加载失败") {
        backend.privateMessagePage(session.talkerId, session.sessionType, before = before)
    }
    override suspend fun acknowledge(session: MessageSession, sequence: Long) = privateChatOutcome("标记已读失败") {
        backend.markSessionsRead(listOf(session.copy(unread = 1, maxSeqno = sequence))); Unit
    }
    override suspend fun myAvatar() = privateChatOutcome("") { backend.nav()?.face.orEmpty() }
    override suspend fun uploadImage(reference: String) = privateChatOutcome("发送失败，请稍后重试") {
        decodeChatUploadedImage(backend.uploadCommentImage(Uri.parse(reference)))
    }
    override suspend fun sendImage(session: MessageSession, image: ChatUploadedImage) = privateChatOutcome("发送失败，请稍后重试") {
        backend.sendPrivateMessage(session.talkerId, "", session.sessionType, encodeChatImage(image))
    }
    override suspend fun sendText(session: MessageSession, text: String) = privateChatOutcome("发送失败，请稍后重试") {
        backend.sendPrivateMessage(session.talkerId, text, session.sessionType)
    }
}

/** 保留私信页原始异常文案，取消交给页面/账号作用域处理。 */
internal suspend fun <T> privateChatOutcome(fallback: String, block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (cancel: CancellationException) {
    throw cancel
} catch (failure: Exception) {
    Outcome.Failure(AppError.Unknown(failure.message ?: fallback))
}
