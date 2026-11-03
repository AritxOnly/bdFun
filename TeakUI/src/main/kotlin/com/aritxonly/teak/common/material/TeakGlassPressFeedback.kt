package com.aritxonly.teak.common.material

internal object TeakGlassPressFeedbackDefaults {
    const val PressedScale = 0.94f
    const val RefractionScaleBoost = 0.06f
    const val HighlightScaleBoost = 0.12f
    const val LightHighlightAlpha = 0.07f
    const val DarkHighlightAlpha = 0.11f
}

internal fun teakGlassPressScale(progress: Float): Float {
    val fraction = progress.coerceIn(0f, 1f)
    return 1f + (TeakGlassPressFeedbackDefaults.PressedScale - 1f) * fraction
}

internal fun teakGlassPressHighlightAlpha(
    progress: Float,
    isDark: Boolean,
): Float = progress.coerceIn(0f, 1f) * if (isDark) {
    TeakGlassPressFeedbackDefaults.DarkHighlightAlpha
} else {
    TeakGlassPressFeedbackDefaults.LightHighlightAlpha
}

internal fun teakGlassPressMaterialInteraction(progress: Float): GlassMaterialInteraction {
    val fraction = progress.coerceIn(0f, 1f)
    return GlassMaterialInteraction(
        refractionScale = 1f + TeakGlassPressFeedbackDefaults.RefractionScaleBoost * fraction,
        highlightScale = 1f + TeakGlassPressFeedbackDefaults.HighlightScaleBoost * fraction,
    )
}
