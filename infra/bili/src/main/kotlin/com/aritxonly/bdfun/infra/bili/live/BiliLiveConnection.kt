package com.aritxonly.bdfun.infra.bili.live

import com.aritxonly.bdfun.core.domain.LiveDanmakuConnection
import com.aritxonly.bdfun.core.model.live.LiveDanmakuPacket
import kotlinx.coroutines.flow.collect

class BiliLiveConnection(private val client: LiveDanmakuClient) : LiveDanmakuConnection {
    override fun connect(urls: List<String>, token: String, roomId: Long, uid: Long) = client.connect(urls, token, roomId, uid)
    override fun disconnect() = client.disconnect()
    override suspend fun collectMessages(consumer: suspend (LiveDanmakuPacket) -> Unit) {
        client.messageFlow.collect { consumer(LiveDanmakuPacket(it.version, it.operation, it.sequence, it.body)) }
    }
}
