package com.aritxonly.bdfun.ui

import org.junit.Assert.*
import org.junit.Test

class SwipeCardPagerPhysicsTest {
    @Test fun accelerationRetainsOriginalStiffnessAndDamping() {
        assertEquals(-406f, swipePagerAcceleration(2f, 1, .2f), 0f)
        assertEquals(400f, swipePagerAcceleration(0f, 1, 0f), 0f)
        assertEquals(0f, swipePagerAcceleration(1f, 1, 0f), 0f)
    }
    @Test fun stopThresholdsAreStrictForBothPositionAndVelocity() {
        assertTrue(swipePagerAtRest(.0014f, 0, .019f))
        assertFalse(swipePagerAtRest(.0015f, 0, 0f)); assertFalse(swipePagerAtRest(0f, 0, .02f))
        assertFalse(swipePagerAtRest(0f, 0, -.02f)); assertTrue(swipePagerAtRest(-.0014f, 0, -.019f))
    }
    @Test fun edgeResistanceDoesNotClampAndOnlyAppliesOutsideRange() {
        assertEquals(-.32f, swipePagerResistance(-1f, 3), 0f)
        assertEquals(2.32f, swipePagerResistance(3f, 3), 0f)
        assertEquals(1.4f, swipePagerResistance(1.4f, 3), 0f)
        assertEquals(.32f, swipePagerResistance(1f, 1), 0f)
    }
    @Test fun targetUsesPredictionAndRoundingThenBounds() {
        assertEquals(2, swipePagerTarget(1.4f, 1f, 1, 4))
        assertEquals(1, swipePagerTarget(1.4f, -1f, 1, 4))
        assertEquals(0, swipePagerTarget(0f, -10f, 0, 4))
        assertEquals(3, swipePagerTarget(3f, 10f, 3, 4))
    }
    @Test fun shortMotionKeepsOriginalSelectedEvenIfPageCountShrank() {
        assertEquals(4, swipePagerTarget(4f, 0f, 4, 3))
        assertEquals(2, swipePagerTarget(4f, .25f, 4, 3))
        assertEquals(2, swipePagerTarget(4.13f, 0f, 4, 3))
    }
}
