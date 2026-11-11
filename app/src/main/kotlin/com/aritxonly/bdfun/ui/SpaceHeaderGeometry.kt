package com.aritxonly.bdfun.ui

/** A missing, not-yet-visible marker is below the viewport; it must not appear over the banner. */
internal fun spaceTabOffset(firstVisibleIndex: Int, markerOffset: Int?, pinnedTop: Float): Float =
    markerOffset?.toFloat()?.coerceAtLeast(pinnedTop)
        ?: if (firstVisibleIndex > 0) pinnedTop else Float.POSITIVE_INFINITY
