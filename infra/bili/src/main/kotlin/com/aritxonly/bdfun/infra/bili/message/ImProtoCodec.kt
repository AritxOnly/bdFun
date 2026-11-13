package com.aritxonly.bdfun.infra.bili.message

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 字段表取自固定上游描述符；仅在网络边界使用 JSON，中间层不认识协议字段。 */
internal class ImProtoCodec(private val schema: JSONObject) {
    fun encode(type: String, value: JSONObject): ByteArray {
        val output = ByteArrayOutputStream()
        fields(type).forEach { field ->
            val name = field.getString("1")
            if (!value.has(name)) return@forEach
            val values = if (field.optInt("4") == 3) value.getJSONArray(name) else JSONArray().put(value.get(name))
            for (index in 0 until values.length()) write(output, field, values.get(index))
        }
        return output.toByteArray()
    }

    fun decode(type: String, bytes: ByteArray, depth: Int = 0): JSONObject {
        require(depth < 40) { "IM 协议嵌套过深" }
        val fields = fields(type).associateBy { it.getInt("3") }
        val input = ProtoInput(bytes)
        val value = JSONObject()
        while (input.remaining > 0) {
            val tag = input.varint().toInt()
            require(tag ushr 3 > 0) { "无效 IM 字段" }
            val wire = tag and 7
            val field = fields[tag ushr 3]
            if (field == null) { input.skip(wire); continue }
            val kind = field.getInt("5")
            val repeated = field.optInt("4") == 3
            if (wire == 2 && kind !in listOf(9, 11, 12) && repeated) {
                val packed = ProtoInput(input.block())
                while (packed.remaining > 0) append(value, field, scalar(packed, kind), true)
            } else {
                require(wire == wireType(kind)) { "IM 字段类型不匹配" }
                val decoded: Any = when (kind) {
                    9 -> input.block().toString(Charsets.UTF_8)
                    11 -> decode(field.getString("6").removePrefix("."), input.block(), depth + 1)
                    12 -> java.util.Base64.getEncoder().encodeToString(input.block())
                    else -> scalar(input, kind)
                }
                append(value, field, decoded, repeated)
            }
        }
        return value
    }

    private fun fields(type: String): List<JSONObject> {
        val list = schema.getJSONArray(type)
        return (0 until list.length()).map { list.getJSONObject(it) }
    }

    private fun append(value: JSONObject, field: JSONObject, decoded: Any, repeated: Boolean) {
        val name = field.getString("1")
        if (repeated) {
            val array = value.optJSONArray(name) ?: JSONArray().also { value.put(name, it) }
            array.put(decoded)
        } else value.put(name, decoded)
    }

    private fun scalar(input: ProtoInput, kind: Int): Any = when (kind) {
        1 -> Double.fromBits(input.fixed(8))
        2 -> Float.fromBits(input.fixed(4).toInt()).toDouble()
        6, 16 -> input.fixed(8)
        7, 15 -> input.fixed(4)
        8 -> input.varint() != 0L
        17, 18 -> input.varint().let { (it ushr 1) xor -(it and 1) }
        else -> input.varint()
    }

    private fun wireType(kind: Int) = when (kind) { 1, 6, 16 -> 1; 2, 7, 15 -> 5; 9, 11, 12 -> 2; else -> 0 }

    private fun write(output: ByteArrayOutputStream, field: JSONObject, value: Any) {
        val kind = field.getInt("5")
        val wire = wireType(kind)
        output.varint((field.getInt("3") shl 3 or wire).toLong())
        when (wire) {
            2 -> {
                val bytes = when (kind) {
                    11 -> encode(field.getString("6").removePrefix("."), value as JSONObject)
                    12 -> java.util.Base64.getDecoder().decode(value.toString())
                    else -> value.toString().toByteArray()
                }
                output.varint(bytes.size.toLong()); output.write(bytes)
            }
            1, 5 -> {
                val number = (value as Number)
                val bits = when (kind) { 1 -> number.toDouble().toBits(); 2 -> number.toFloat().toBits().toLong(); else -> number.toLong() }
                val bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(bits).array()
                output.write(bytes, 0, if (wire == 1) 8 else 4)
            }
            else -> {
                val number = if (value is Boolean) if (value) 1L else 0L else value.toString().toLong()
                output.varint(if (kind in listOf(17, 18)) (number shl 1) xor (number shr 63) else number)
            }
        }
    }
}

internal fun ByteArrayOutputStream.varint(number: Long) {
    var value = number
    while (value and -128L != 0L) { write((value.toInt() and 127) or 128); value = value ushr 7 }
    write(value.toInt())
}

private class ProtoInput(private val bytes: ByteArray) {
    private var position = 0
    val remaining get() = bytes.size - position
    fun varint(): Long {
        var value = 0L
        for (shift in 0..63 step 7) {
            require(remaining > 0) { "IM 协议被截断" }
            val byte = bytes[position++].toInt() and 255
            require(shift != 63 || byte <= 1) { "IM 整数溢出" }
            value = value or ((byte and 127).toLong() shl shift)
            if (byte and 128 == 0) return value
        }
        error("无效 IM 整数")
    }
    fun block(): ByteArray {
        val length = varint()
        require(length in 0..remaining.toLong()) { "无效 IM 数据长度" }
        return bytes.copyOfRange(position, position + length.toInt()).also { position += length.toInt() }
    }
    fun fixed(count: Int): Long {
        require(remaining >= count) { "IM 协议被截断" }
        var value = 0L
        repeat(count) { value = value or ((bytes[position++].toLong() and 255) shl (it * 8)) }
        return value
    }
    fun skip(wire: Int) { when (wire) { 0 -> varint(); 1 -> fixed(8); 2 -> block(); 5 -> fixed(4); else -> error("不支持的 IM wire type") } }
}
