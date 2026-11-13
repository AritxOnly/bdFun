package com.aritxonly.bdfun.core.domain

/** 对齐 PiliPlus 的官方 App browser scheme；完整编码内层 URL，避免查询参数串到外层。 */
fun officialAppLoginUri(url: String): String? {
    if (!url.startsWith("https://") && !url.startsWith("http://")) return null
    if (url.substringAfter("://").substringBefore('/').substringBefore('?').isBlank()) return null
    val hex = "0123456789ABCDEF"
    val encoded = buildString {
        url.encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and 255
            val character = value.toChar()
            if (character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' || character in "-_.!~*'()") {
                append(character)
            } else {
                append('%'); append(hex[value ushr 4]); append(hex[value and 15])
            }
        }
    }
    return "bilibili://browser?url=$encoded"
}
