package com.aritxonly.teak.common.material

data class TeakMaterialFineTuning(
    val saturationMultiplier: Float = 1.05f,
    val noiseCoefficient: Float = 0.095f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val topBarTintAlpha: Float = 0.85f,
    val navigationTintAlpha: Float = 0.90f,
    val dialogBackgroundAlpha: Float = TeakModalMaterialTuning.DialogDefaultBackgroundAlpha,
    val dialogBlurRadiusDp: Float = TeakModalMaterialTuning.DialogDefaultBlurRadiusDp,
    val bottomSheetBackgroundAlpha: Float = TeakModalMaterialTuning.BottomSheetDefaultBackgroundAlpha,
    val bottomSheetBlurRadiusDp: Float = TeakModalMaterialTuning.BottomSheetDefaultBlurRadiusDp,
    val glassBlurRadiusMultiplier: Float = 0.25f,
    val glassNoiseMultiplier: Float = 1f,
    val glassTintAlphaMultiplier: Float = 0.90f,
    val glassTintLightGray: Float = 1f,
    val glassTintDarkGray: Float = 0.12f,
    val refractionEnabled: Boolean = true,
    val refractionHeightDp: Float = 18f,
    val refractionAmountDp: Float = 18f,
    val refractionDepthEffect: Float = 0.60f,
    val chromaticAberrationDp: Float = 1f,
    val edgeHighlightAlpha: Float = 0.95f,
    val edgeHighlightGray: Float = 1f,
    val darkHighlightMultiplier: Float = 0.20f,
    val fallbackEdgeWidthDp: Float = 0.5f,
    val fallbackLightAlpha: Float = 0.46f,
    val fallbackDarkAlpha: Float = 0.08f,
    val postRefractionBlurEnabled: Boolean = true,
    val postRefractionBlurRadiusDp: Float = 0.5f,
) {
    fun normalized(): TeakMaterialFineTuning = copy(
        saturationMultiplier = saturationMultiplier.safeCoerceIn(0.75f, 1.25f, 1.05f),
        noiseCoefficient = noiseCoefficient.safeCoerceIn(0f, 0.20f, 0.095f),
        brightness = brightness.safeCoerceIn(-0.20f, 0.20f, 0f),
        contrast = contrast.safeCoerceIn(0.75f, 1.25f, 1f),
        topBarTintAlpha = topBarTintAlpha.safeCoerceIn(0.20f, 1f, 0.85f),
        navigationTintAlpha = navigationTintAlpha.safeCoerceIn(0.20f, 1f, 0.90f),
        dialogBackgroundAlpha = dialogBackgroundAlpha.safeCoerceIn(
            TeakModalMaterialTuning.MinBackgroundAlpha,
            TeakModalMaterialTuning.MaxBackgroundAlpha,
            TeakModalMaterialTuning.DialogDefaultBackgroundAlpha,
        ),
        dialogBlurRadiusDp = dialogBlurRadiusDp.safeCoerceIn(
            TeakModalMaterialTuning.MinBlurRadiusDp,
            TeakModalMaterialTuning.MaxBlurRadiusDp,
            TeakModalMaterialTuning.DialogDefaultBlurRadiusDp,
        ),
        bottomSheetBackgroundAlpha = bottomSheetBackgroundAlpha.safeCoerceIn(
            TeakModalMaterialTuning.MinBackgroundAlpha,
            TeakModalMaterialTuning.MaxBackgroundAlpha,
            TeakModalMaterialTuning.BottomSheetDefaultBackgroundAlpha,
        ),
        bottomSheetBlurRadiusDp = bottomSheetBlurRadiusDp.safeCoerceIn(
            TeakModalMaterialTuning.MinBlurRadiusDp,
            TeakModalMaterialTuning.MaxBlurRadiusDp,
            TeakModalMaterialTuning.BottomSheetDefaultBlurRadiusDp,
        ),
        glassBlurRadiusMultiplier = glassBlurRadiusMultiplier.safeCoerceIn(0.10f, 1f, 0.25f),
        glassNoiseMultiplier = glassNoiseMultiplier.safeCoerceIn(0f, 2f, 1f),
        glassTintAlphaMultiplier = glassTintAlphaMultiplier.safeCoerceIn(0.50f, 1.50f, 0.90f),
        glassTintLightGray = glassTintLightGray.safeCoerceIn(0f, 1f, 1f),
        glassTintDarkGray = glassTintDarkGray.safeCoerceIn(0f, 1f, 0.12f),
        refractionHeightDp = refractionHeightDp.safeCoerceIn(0f, 32f, 18f),
        refractionAmountDp = refractionAmountDp.safeCoerceIn(0f, 32f, 18f),
        refractionDepthEffect = refractionDepthEffect.safeCoerceIn(0f, 0.60f, 0.60f),
        chromaticAberrationDp = chromaticAberrationDp.safeCoerceIn(0f, 4f, 1f),
        edgeHighlightAlpha = edgeHighlightAlpha.safeCoerceIn(0f, 1f, 0.95f),
        edgeHighlightGray = edgeHighlightGray.safeCoerceIn(0f, 1f, 1f),
        darkHighlightMultiplier = darkHighlightMultiplier.safeCoerceIn(0.10f, 2f, 0.20f),
        fallbackEdgeWidthDp = fallbackEdgeWidthDp.safeCoerceIn(0f, 2f, 0.5f),
        fallbackLightAlpha = fallbackLightAlpha.safeCoerceIn(0f, 1f, 0.46f),
        fallbackDarkAlpha = fallbackDarkAlpha.safeCoerceIn(0f, 1f, 0.08f),
        postRefractionBlurRadiusDp = postRefractionBlurRadiusDp.safeCoerceIn(0f, 2f, 0.5f),
    )

    companion object {
        val Default = TeakMaterialFineTuning()

        internal val DefaultsV3 = TeakMaterialFineTuning(
            dialogBackgroundAlpha = 0.62f,
            dialogBlurRadiusDp = 48f,
            bottomSheetBackgroundAlpha = 0.66f,
            bottomSheetBlurRadiusDp = 64f,
        )

        internal val DefaultsV2 = TeakMaterialFineTuning(
            saturationMultiplier = 1f,
            noiseCoefficient = 0.09f,
            topBarTintAlpha = 0.85f,
            navigationTintAlpha = 0.90f,
            glassBlurRadiusMultiplier = 0.4f,
            glassTintAlphaMultiplier = 1f,
            glassTintLightGray = 1f,
            glassTintDarkGray = 0.08f,
            refractionAmountDp = 16f,
            refractionDepthEffect = 0.24f,
            chromaticAberrationDp = 0f,
            edgeHighlightAlpha = 0.18f,
            darkHighlightMultiplier = 0.40f,
            postRefractionBlurRadiusDp = 0.35f,
        )

        internal val DefaultsV1 = DefaultsV2.copy(
            edgeHighlightAlpha = 0.11f,
            darkHighlightMultiplier = 1.35f,
            fallbackLightAlpha = 0.42f,
            fallbackDarkAlpha = 0.14f,
        )
    }
}

private fun Float.safeCoerceIn(minimum: Float, maximum: Float, fallback: Float): Float =
    if (isFinite()) coerceIn(minimum, maximum) else fallback
