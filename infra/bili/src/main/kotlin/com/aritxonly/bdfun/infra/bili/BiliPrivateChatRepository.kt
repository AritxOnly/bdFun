package com.aritxonly.bdfun.infra.bili

import android.net.Uri
import com.aritxonly.bdfun.infra.bili.message.*
import com.aritxonly.bdfun.infra.bili.mapper.mapPrivateMessagePage
import com.aritxonly.bdfun.core.model.message.ChatMessage
import org.json.JSONObject
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.ChatUploadedImage
import com.aritxonly.bdfun.core.domain.PrivateChatRepository
import com.aritxonly.bdfun.core.model.message.MessageSession
import kotlinx.coroutines.CancellationException

class BiliPrivateChatRepository(private val backend: BiliBackend) : PrivateChatRepository {
    override suspend fun withdraw(session: MessageSession, message: ChatMessage) = privateChatOutcome("撤回失败") {
        check(message.isMine && !message.withdrawn) { "只能撤回自己的消息" }
        backend.api.imSend(session.talkerId, message.id.toString(), 5); Unit
    }
    override suspend fun report(message: ChatMessage, reason: Int, description: String) = privateChatOutcome("举报失败") {
        backend.api.dataRequest("/x/bplus/im/report/add", mapOf("biz_code" to "4", "accused_uid" to message.senderId.toString(),
            "object_id" to message.senderId.toString(), "reason_type" to reason.toString(), "reason_desc" to description,
            "module" to "604", "comment" to JSONObject().put("group_id", 0).put("msg_key", message.id.toString()).toString(),
            "extra" to "{\"msg_keys\":[]}", "csrf" to backend.csrf), base = BILI_VC, method = "POST"); Unit
    }
    override suspend fun page(session: MessageSession, before: Long?) = privateChatOutcome("历史消息加载失败") {
        mapPrivateMessagePage(backend.api.imHistory(session.talkerId, before), backend.mid, before)
    }
    override suspend fun acknowledge(session: MessageSession, sequence: Long) = privateChatOutcome("标记已读失败") {
        backend.markSessionsRead(listOf(session.copy(unread = 1, maxSeqno = sequence))); Unit
    }
    override suspend fun myAvatar() = privateChatOutcome("") { backend.nav()?.face.orEmpty() }
    override suspend fun uploadImage(reference: String) = privateChatOutcome("发送失败，请稍后重试") {
        backend.api.imUploadImage(reference)
    }
    override suspend fun sendImage(session: MessageSession, image: ChatUploadedImage) = privateChatOutcome("发送失败，请稍后重试") {
        backend.api.imSend(session.talkerId, encodeChatImage(image), 2)
    }
    override suspend fun sendText(session: MessageSession, text: String) = privateChatOutcome("发送失败，请稍后重试") {
        backend.api.imSend(session.talkerId, JSONObject().put("content", text).toString())
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
