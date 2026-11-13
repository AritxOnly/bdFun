package com.aritxonly.teak.common.material

import com.aritxonly.teak.common.material.miuixControlFeedback
import androidx.compose.foundation.border
import com.aritxonly.teak.common.theme.LocalTeakControlContrast
import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.foundation.background
import com.aritxonly.teak.common.chrome.LocalProgressiveTopBarMaterialHosted
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import top.yukonga.miuix.kmp.blur.Backdrop

object TeakGlassDefaults {
    val Shape: Shape = Capsule()
    val IconButtonSize: Dp = 44.dp
}

/**
 * Layout-neutral high-level entry point for Deadliner's soft-glass material.
 *
 * The caller owns size and padding. This layer only resolves the active material, backdrop,
 * tint strategy and optical recipe before delegating to [teakGlassMaterial].
 */
@Composable
fun TeakGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = TeakGlassDefaults.Shape,
    recipe: GlassMaterialRecipe? = null,
    tint: Color? = null,
    tintStyle: GlassTintStyle = GlassTintStyle.Neutral,
    interaction: GlassMaterialInteraction = GlassMaterialInteraction(),
    materialAlpha: Float = if (LocalProgressiveTopBarMaterialHosted.current)
        com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current else 1f,
    /** Optional flat state, enabled only by home controls and segmented selectors. */
    flatBackgroundAlpha: Float = 0f,
    baseAlpha: Float = 1f,
    backdrop: Backdrop? = LocalTeakBackdrop.current,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f
    val resolvedRecipe = recipe ?: TeakGlassRecipes.floatingNavigation(
        advancedMaterial.fineTuning,
    )
    val resolvedTint = tint ?: advancedMaterial.fineTuning
        .softGlassTintColor(isDark)
        .copy(
            alpha = if (advancedMaterial.enabled) {
                advancedMaterial.navigationTintAlpha
            } else {
                0.92f
            },
        )

    Box(
        modifier = modifier.clip(shape).background(
            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainer.copy(
                alpha = flatBackgroundAlpha.coerceIn(0f, 1f) * (1f - materialAlpha.coerceIn(0f, 1f)),
            ),
        ),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = materialAlpha.coerceIn(0f, 1f)
                }
                .background(resolvedTint.copy(alpha = baseAlpha.coerceIn(0f, 1f)))
                .teakGlassMaterial(
                    advancedMaterial = advancedMaterial,
                    backdrop = backdrop,
                    shape = shape,
                    tint = resolvedTint,
                    tintStyle = tintStyle,
                    isDark = isDark,
                    recipe = resolvedRecipe,
                    interaction = interaction,
                ),
        )
        if (LocalTeakControlContrast.current) {
            val contrast = LocalTeakContentColors.current
            Box(Modifier.matchParentSize().background(contrast.controlFill, shape)
                .border(1.dp, contrast.controlOutline, shape))
        }
        content()
    }
}

/** A circular soft-glass host for icon-only top-bar actions. */
@Composable
fun TeakGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = TeakGlassDefaults.IconButtonSize,
    recipe: GlassMaterialRecipe? = null,
    tint: Color? = null,
    tintStyle: GlassTintStyle = GlassTintStyle.Neutral,
    backdrop: Backdrop? = LocalTeakBackdrop.current,
    materialAlpha: Float = if (LocalProgressiveTopBarMaterialHosted.current)
        com.aritxonly.teak.common.chrome.LocalTopBarButtonMaterialProgress.current else 1f,
    flatBackgroundAlpha: Float = 0f,
    neutralTintProgress: Float = 0f,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f

    // Match floating navigation: elevation belongs to the outer frame, not the glass clip.
    Box(
        modifier = modifier
            .size(size)
            .teakGlassShadow(
                shape = TeakGlassDefaults.Shape,
                isDark = isDark,
                alpha = materialAlpha,
                profile = if (LocalProgressiveTopBarMaterialHosted.current) TeakGlassShadowProfile.TopBarControl
                    else TeakGlassShadowProfile.Standard,
            )
            .miuixControlFeedback(enabled = enabled, shape = TeakGlassDefaults.Shape),
    ) {
        TeakGlassSurface(
            modifier = Modifier.fillMaxSize(),
            shape = TeakGlassDefaults.Shape,
            recipe = (recipe ?: TeakGlassRecipes.floatingNavigation(
                LocalTeakMaterialSpec.current.fineTuning,
            )).copy(surfaceRole = GlassSurfaceRole.Control),
            tint = tint,
            tintStyle = tintStyle,
            backdrop = backdrop,
            materialAlpha = materialAlpha,
            flatBackgroundAlpha = flatBackgroundAlpha,
            interaction = GlassMaterialInteraction(neutralTintProgress = neutralTintProgress),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
