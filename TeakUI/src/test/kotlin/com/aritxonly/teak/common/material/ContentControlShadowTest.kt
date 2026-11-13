package com.aritxonly.teak.common.material

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentControlShadowTest {
    @Test fun detailsOnlyDeepenMatchingGlobalProfileByTenPercent() {
        for (dark in listOf(false, true)) for (official in listOf(false, true)) {
            for (preset in GlassShadowPreset.entries) {
                for ((normal, detail) in listOf(
                    TeakGlassShadowProfile.Standard to TeakGlassShadowProfile.ContentControl,
                    TeakGlassShadowProfile.TopBarControl to TeakGlassShadowProfile.ContentTopBarControl,
                )) {
                    val base = teakGlassShadowTokens(dark, official, normal, preset)
                    val deeper = teakGlassShadowTokens(dark, official, detail, preset)
                    assertEquals(base, deeper.copy(ambientAlpha = base.ambientAlpha,
                        outerAlpha = base.outerAlpha, edgeAlpha = base.edgeAlpha))
                    assertEquals(base.ambientAlpha * 1.10f, deeper.ambientAlpha, 0.000001f)
                    assertEquals(base.outerAlpha * 1.10f, deeper.outerAlpha, 0.000001f)
                    assertEquals(base.edgeAlpha * 1.10f, deeper.edgeAlpha, 0.000001f)
                }
            }
        }
    }
}
