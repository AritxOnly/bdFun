package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.message.ChatMessagePage
import com.aritxonly.bdfun.core.model.message.MessageSession

data class ChatUploadedImage(val url: String, val width: Int, val height: Int, val size: Double, val imageType: String = "jpg")

interface PrivateChatRepository {
    suspend fun withdraw(session: MessageSession, message: com.aritxonly.bdfun.core.model.message.ChatMessage): Outcome<Unit> =
        Outcome.Failure(com.aritxonly.bdfun.core.common.AppError.Unknown("当前服务不支持撤回"))
    suspend fun report(message: com.aritxonly.bdfun.core.model.message.ChatMessage, reason: Int, description: String): Outcome<Unit> =
        Outcome.Failure(com.aritxonly.bdfun.core.common.AppError.Unknown("当前服务不支持举报"))
    suspend fun page(session: MessageSession, before: Long? = null): Outcome<ChatMessagePage>
    suspend fun acknowledge(session: MessageSession, sequence: Long): Outcome<Unit>
    suspend fun myAvatar(): Outcome<String>
    suspend fun uploadImage(reference: String): Outcome<ChatUploadedImage>
    suspend fun sendImage(session: MessageSession, image: ChatUploadedImage): Outcome<Boolean>
    suspend fun sendText(session: MessageSession, text: String): Outcome<Boolean>
}
