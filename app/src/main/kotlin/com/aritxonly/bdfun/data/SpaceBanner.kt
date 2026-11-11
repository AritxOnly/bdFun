package com.aritxonly.bdfun.data

internal fun normalizeSpaceBanner(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
    url.removePrefix("/").startsWith("bfs/") -> "https://i0.hdslb.com/${url.removePrefix("/")}"
    else -> url
}

internal fun orderedSpaceBanners(collection: String, cover: String, web: String, day: String, night: String): List<String> {
    // acc/info returns this built-in default even when the App has a personal dressing.
    val defaultWeb = web.contains("cb1c3ef50e22b6096fde67febe863494caefebad")
    return (if (defaultWeb) listOf(collection, cover, day, night, web)
        else listOf(collection, cover, web, day, night)).filter { it.isNotBlank() }
        .map(::normalizeSpaceBanner).distinct()
}

/** HyperPiliPlus TopImage.location: skip the prefix, use the selected crop's vertical centre. */
internal fun spaceBannerAlignment(location: String, height: Double): Float {
    if (!height.isFinite() || height <= 0) return 0f
    val crop = location.split('-').drop(1).take(2).mapNotNull { it.toDoubleOrNull() }
    if (crop.size != 2) return 0f
    val offset = (crop.sum() / height - 1).toFloat()
    return if (offset.isFinite()) offset.coerceIn(-1f, 1f) else 0f
}
