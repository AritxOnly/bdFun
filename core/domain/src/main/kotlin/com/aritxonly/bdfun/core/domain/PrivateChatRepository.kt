package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.message.ChatMessagePage
import com.aritxonly.bdfun.core.model.message.MessageSession

data class ChatUploadedImage(val url: String, val width: Int, val height: Int, val size: Double)

interface PrivateChatRepository {
    suspend fun page(session: MessageSession, before: Long? = null): Outcome<ChatMessagePage>
    suspend fun acknowledge(session: MessageSession, sequence: Long): Outcome<Unit>
    suspend fun myAvatar(): Outcome<String>
    suspend fun uploadImage(reference: String): Outcome<ChatUploadedImage>
    suspend fun sendImage(session: MessageSession, image: ChatUploadedImage): Outcome<Boolean>
    suspend fun sendText(session: MessageSession, text: String): Outcome<Boolean>
}
