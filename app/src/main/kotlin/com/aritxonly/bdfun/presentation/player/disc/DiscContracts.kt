package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings

internal class DiscDiagnostics(val onCreate: () -> Unit, val onDispose: () -> Unit, val log: (String) -> Unit)
internal class DiscDependencies(val playback: com.aritxonly.bdfun.core.application.BackgroundDiscPlayback, val diagnostics: DiscDiagnostics)
internal data class DiscGestureGeometry(val swipeThreshold: Float, val touchSlop: Float, val maxDragPx: Float)
internal class DiscRuntime(
    val settings: SettingsBindings, val dependencies: DiscDependencies, val size: Dp,
    val ui: DiscPresentationStateHolder, val scope: kotlinx.coroutines.CoroutineScope,
    val context: android.content.Context, val oneTake: OneTakeAnchor, val coverPx: Int,
    val geometry: DiscGestureGeometry, val session: DiscPlaybackState,
    val openOriginal: () -> Unit,
)
internal data class DiscMotion(val dragX: Float, val dragY: Float, val exitProgress: Float)
internal data class DiscVisual(
    val dragX: Float, val dragY: Float, val exitProgress: Float, val glassBlurDp: Float,
    val breathEnabled: Boolean, val breath01: Float, val breathTint: Color,
)
