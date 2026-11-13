package com.aritxonly.bdfun.infra.bili.live

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

/** main协议定义的结构样本，非线上弹幕采样；固定字节避免编码/解码同时出错仍通过。 */
class LiveProtocolTest {
    private val golden = "000000120010000100000007000000017b7d"
        .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private val packet = LiveProtocolPacket(1, 7, 1, "{}".toByteArray())

    @Test fun headerAndBodyKeepMainWireFormat() = runBlocking {
        assertArrayEquals(golden, encodeLivePacket(packet))
        assertEquals(listOf(packet), decodeLivePackets(golden))
    }
    @Test fun compressedPacketUnwrapsAndDamagedSiblingDoesNotDiscardFollowingPacket() = runBlocking {
        val output = ByteArrayOutputStream()
        DeflaterOutputStream(output).use { it.write(golden) }
        val compressed = packet.copy(version = 2, body = output.toByteArray())
        assertEquals(listOf(packet), decodeLivePackets(encodeLivePacket(compressed)))
        val damaged = packet.copy(version = 2, body = byteArrayOf(1, 2, 3))
        assertEquals(listOf(packet), decodeLivePackets(encodeLivePacket(damaged) + golden))
    }
}
