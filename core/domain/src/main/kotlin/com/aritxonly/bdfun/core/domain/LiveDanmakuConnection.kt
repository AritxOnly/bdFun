package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.live.LiveChatMessage
import com.aritxonly.bdfun.core.model.live.LiveDanmakuPacket

interface LiveDanmakuConnection {
    fun connect(urls: List<String>, token: String, roomId: Long, uid: Long)
    fun disconnect()
    suspend fun collectMessages(consumer: suspend (LiveDanmakuPacket) -> Unit)
}

fun interface LiveMessageDecoder {
    fun decode(raw: String, id: Long): LiveChatMessage?
}
