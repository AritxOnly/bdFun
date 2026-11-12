package com.aritxonly.bdfun.core.common

val UrlPattern = Regex("https?://[^\\s\\u3000<>\"']+")

private val UrlTrailing = charArrayOf(
    '.', ',', ';', ':', '!', '?', ')', ']', '}',
    '，', '。', '！', '？', '、', '；', '：', '）', '】', '」', '』',
)

fun trimUrlTrailing(raw: String): String {
    var end = raw.length
    while (end > 0 && raw[end - 1] in UrlTrailing) end--
    return raw.substring(0, end)
}
