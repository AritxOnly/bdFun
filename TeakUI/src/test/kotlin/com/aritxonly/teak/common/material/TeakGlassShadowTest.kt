package com.aritxonly.teak.common.material

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class TeakGlassShadowTest {
    @Test
    fun lightGlassUsesLowDensityThreeStageDiffuseShadow() {
        val light = teakGlassShadowTokens(isDark = false)
        val dark = teakGlassShadowTokens(isDark = true)

        assertEquals(0f, light.offset.x.value, 0.0001f)
        assertEquals(0f, light.offset.y.value, 0.0001f)
        assertEquals(0f, dark.offset.x.value, 0.0001f)
        assertEquals(0f, dark.offset.y.value, 0.0001f)
        assertTrue(light.ambientRadius > light.outerRadius)
        assertTrue(dark.ambientRadius > dark.outerRadius)
        assertTrue(light.outerAlpha > dark.outerAlpha)
        assertTrue(light.edgeAlpha < light.outerAlpha * 0.25f)
        assertTrue(dark.edgeAlpha < dark.outerAlpha * 0.25f)
        assertTrue(light.outerSpread < 0.dp)
        assertTrue(dark.outerSpread < 0.dp)
        assertTrue(light.ambientSpread < light.outerSpread)
        assertTrue(dark.ambientSpread < dark.outerSpread)
    }

    @Test fun selfLightGlassSeparatesFromWhiteWithoutChangingDarkOrOfficialGlass() {
        val self = teakGlassShadowTokens(isDark = false)
        val official = teakGlassShadowTokens(isDark = false, useOfficialMiuixGlass = true)
        assertTrue(self.outerAlpha > official.outerAlpha)
        assertTrue(self.ambientAlpha > official.ambientAlpha)
        assertTrue(self.edgeAlpha < self.outerAlpha * 0.25f)
        assertEquals(teakGlassShadowTokens(true, false), teakGlassShadowTokens(true, true))
        assertEquals(0.025f, official.ambientAlpha, 0.0001f)
        assertEquals(0.050f, official.outerAlpha, 0.0001f)
    }

    @Test fun topBarCompensationOnlyChangesSelfLightAmbientShadow() {
        val base = teakGlassShadowTokens(false)
        val control = teakGlassShadowTokens(false, profile = TeakGlassShadowProfile.TopBarControl)
        assertTrue(control.ambientRadius > base.ambientRadius)
        assertTrue(control.ambientAlpha > base.ambientAlpha)
        assertEquals(base, control.copy(ambientRadius = base.ambientRadius, ambientAlpha = base.ambientAlpha))
        assertEquals(teakGlassShadowTokens(true), teakGlassShadowTokens(true, profile = TeakGlassShadowProfile.TopBarControl))
        assertEquals(teakGlassShadowTokens(false, true), teakGlassShadowTokens(false, true, TeakGlassShadowProfile.TopBarControl))
    }

    @Test fun defaultsMatchCapturedDeadlinerWorkingTree() {
        val light = TeakGlassShadowTokens(
            26.dp, (-2).dp, .03459375f,
            10.dp, (-1).dp, .06490625f,
            2.dp, (-.5).dp, .0094375f,
        )
        assertEquals(light, teakGlassShadowTokens(false))
        assertEquals(light.copy(ambientRadius = 34.dp, ambientAlpha = .043453125f),
            teakGlassShadowTokens(false, profile = TeakGlassShadowProfile.TopBarControl))
        assertEquals(TeakGlassShadowTokens(
            22.dp, (-1).dp, .020f, 11.dp, (-.5).dp, .038f, 4.dp, (-.5).dp, .008f,
        ), teakGlassShadowTokens(true))
        assertEquals(TeakGlassShadowTokens(
            24.dp, (-1).dp, .025f, 12.dp, (-.5).dp, .050f, 4.5.dp, (-.5).dp, .012f,
        ), teakGlassShadowTokens(false, true))
    }

}
