package com.aritxonly.teak.common.material

enum class TeakModalKind {
    Dialog,
    BottomSheet,
}

data class TeakModalMaterialTuning(
    val backgroundAlpha: Float,
    val blurRadiusDp: Float,
) {
    companion object {
        const val MinBackgroundAlpha = 0.20f
        const val MaxBackgroundAlpha = 0.95f
        const val MinBlurRadiusDp = 0f
        const val MaxBlurRadiusDp = 150f

        const val DialogDefaultBackgroundAlpha = 0.65f
        const val DialogDefaultBlurRadiusDp = 80f
        const val BottomSheetDefaultBackgroundAlpha = 0.70f
        const val BottomSheetDefaultBlurRadiusDp = 80f
    }
}


/** 与 Deadliner 共用选择规则，保留 bdFun 自己的材质参数。 */
fun TeakMaterialFineTuning.miuixModalMaterial(kind: TeakModalKind): TeakModalMaterialTuning {
    val tuning = normalized()
    return when (kind) {
        TeakModalKind.Dialog -> TeakModalMaterialTuning(tuning.dialogBackgroundAlpha, tuning.dialogBlurRadiusDp)
        TeakModalKind.BottomSheet -> TeakModalMaterialTuning(tuning.bottomSheetBackgroundAlpha, tuning.bottomSheetBlurRadiusDp)
    }
}
