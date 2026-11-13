package com.aritxonly.teak.common.material

import org.junit.Assert.assertTrue
import org.junit.Test

class ContentControlShadowTest {
    @Test fun whiteCanvasUsesBroadDiffusionWithRestrainedContactEdge() {
        val shadow = teakGlassShadowTokens(false, profile = TeakGlassShadowProfile.ContentControl)
        assertTrue(shadow.ambientRadius > shadow.outerRadius)
        assertTrue(shadow.outerRadius > shadow.edgeRadius)
        assertTrue(shadow.ambientAlpha > 0f)
        assertTrue(shadow.edgeAlpha < shadow.outerAlpha)
        assertTrue(shadow.outerAlpha < 0.12f)
    }
    @Test fun standardPagesKeepTheirExistingShadow() {
        val shadow = teakGlassShadowTokens(false)
        assertTrue(shadow.ambientAlpha == 0f)
        assertTrue(shadow.outerAlpha < teakGlassShadowTokens(false,
            profile = TeakGlassShadowProfile.ContentControl).outerAlpha)
    }
}
