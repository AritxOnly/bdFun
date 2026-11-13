package com.aritxonly.bdfun.data

internal fun normalizeSpaceBanner(url: String) = com.aritxonly.bdfun.core.domain.normalizeSpaceBanner(url)
internal fun orderedSpaceBanners(collection: String, cover: String, web: String, day: String, night: String) =
    com.aritxonly.bdfun.core.domain.orderedSpaceBanners(collection, cover, web, day, night)
internal fun spaceBannerAlignment(location: String, height: Double) =
    com.aritxonly.bdfun.core.domain.spaceBannerAlignment(location, height)
