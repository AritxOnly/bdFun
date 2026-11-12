package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.Dp

/** Shared geometry for the player, details, and Activity-owned overlays. */
data class VideoPaneLayout(
    val widthDp: Float,
    val detailsWidthDp: Float = 0f,
) {
    val isSplit: Boolean get() = detailsWidthDp > 0f
    val playerWidthDp: Float get() = widthDp - detailsWidthDp
}

/** Landscape uses 70/30, reserving at least 300 dp for readable details/comments. */
fun resolveVideoPaneLayout(widthDp: Float, heightDp: Float): VideoPaneLayout {
    val split = widthDp >= 600f && heightDp > 0f && widthDp > heightDp
    return VideoPaneLayout(widthDp, if (split) maxOf(300f, widthDp * 0.3f) else 0f)
}

data class VideoActivityLayout(
    val paneLayout: VideoPaneLayout, val statusBarTop: Dp,
    val fullscreen: Boolean, val pictureInPicture: Boolean,
    val overlayAlpha: Float, val playerTopInset: Dp,
) {
    val splitPanes get() = paneLayout.isSplit && !fullscreen
}
