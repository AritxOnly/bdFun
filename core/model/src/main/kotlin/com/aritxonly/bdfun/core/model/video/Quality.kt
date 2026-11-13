package com.aritxonly.bdfun.core.model.video

data class Quality(
    val id: Int,
    val label: String,
    val width: Int,
    val height: Int,
    val frameRate: String,
    val codec: String,
    val videoUrl: String,
    /** CDN 替换前的原始地址，CDN 403 时用来重试。 */
    val rawVideoUrl: String = "",
)
