package com.aritxonly.bdfun.core.common

/** 沿用旧图片 URL 规则；已带裁切参数的链接不能再追加尺寸。 */
fun biliImageUrl(url: String, width: Int = 0): String {
    if (url.isBlank()) return ""
    var result = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
        else -> url
    }
    if (width > 0 && !result.contains("@")) {
        result += "@${width}w_${(width * 9 / 16)}h_1c.webp"
    }
    return result
}
