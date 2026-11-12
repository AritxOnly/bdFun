package com.aritxonly.bdfun.core.model.live

const val LIVE_DANMAKU_MESSAGE_OPERATION = 5

data class LiveDanmakuPacket(val version: Int, val operation: Int, val sequence: Int, val body: ByteArray) {
    override fun equals(other: Any?): Boolean = other is LiveDanmakuPacket &&
        version == other.version && operation == other.operation && sequence == other.sequence && body.contentEquals(other.body)
    override fun hashCode(): Int {
        var result = version
        result = 31 * result + operation
        result = 31 * result + sequence
        return 31 * result + body.contentHashCode()
    }
}
