package com.aritxonly.teak.common.modal

import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.*
import com.aritxonly.teak.common.modal.sheet.sheetBottomCornerRadius
import org.junit.Assert.assertEquals
import org.junit.Test

class ModalPolicyTest {
    @Test fun attachedAndFloatingSheetsUseDifferentBottomCorners() {
        assertEquals(2.dp, sheetBottomCornerRadius(411.dp, 560.dp))
        assertEquals(2.dp, sheetBottomCornerRadius(560.dp, 560.dp))
        assertEquals(36.dp, sheetBottomCornerRadius(1200.dp, 560.dp))
    }

    @Test fun modalResolverPreservesAppTuningInsteadOfDeadlinerDefaults() {
        val tuning = TeakMaterialFineTuning.Default.copy(
            dialogBackgroundAlpha = 0.61f, dialogBlurRadiusDp = 90f,
            bottomSheetBackgroundAlpha = 0.72f, bottomSheetBlurRadiusDp = 64f,
        )
        assertEquals(TeakModalMaterialTuning(0.61f, 90f), tuning.miuixModalMaterial(TeakModalKind.Dialog))
        assertEquals(TeakModalMaterialTuning(0.72f, 64f), tuning.miuixModalMaterial(TeakModalKind.BottomSheet))
    }

    @Test fun invalidModalTuningCannotReachTheShader() {
        val tuning = TeakMaterialFineTuning.Default.copy(dialogBackgroundAlpha = Float.NaN,
            bottomSheetBlurRadiusDp = Float.POSITIVE_INFINITY)
        assertEquals(TeakModalMaterialTuning.DialogDefaultBackgroundAlpha,
            tuning.miuixModalMaterial(TeakModalKind.Dialog).backgroundAlpha)
        assertEquals(TeakModalMaterialTuning.BottomSheetDefaultBlurRadiusDp,
            tuning.miuixModalMaterial(TeakModalKind.BottomSheet).blurRadiusDp)
    }
}
