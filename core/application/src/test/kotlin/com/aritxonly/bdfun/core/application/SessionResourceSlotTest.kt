package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class SessionResourceSlotTest {
    private class Fixture {
        var owner = SessionIdentity(1, 1)
        var created = 0
        val released = mutableListOf<Int>()
        val slot = SessionResourceSlot({ owner }, { ++created }, { released += it })
    }

    @Test fun returningToSameSessionRetainsResource() {
        val f = Fixture()
        assertEquals(1, f.slot.get()); assertEquals(1, f.slot.get())
        f.slot.clearStale()
        assertEquals(1, f.created); assertTrue(f.released.isEmpty())
    }
    @Test fun rapidAccountRoundTripCannotResurrectOldResource() {
        val f = Fixture(); f.slot.get()
        f.owner = SessionIdentity(2, 2)
        f.owner = SessionIdentity(1, 3) // 模拟collector未获得中间B状态。
        assertEquals(2, f.slot.get())
        assertEquals(listOf(1), f.released)
    }
    @Test fun explicitSameAccountLoginStillInvalidates() {
        val f = Fixture(); f.slot.get(); f.owner = f.owner.copy(revision = 2)
        f.slot.clearStale(); f.slot.clearStale()
        assertEquals(listOf(1), f.released); assertEquals(2, f.slot.get())
    }
    @Test fun guestLoginLogoutAndGuestRoundTripUseDistinctResources() {
        val f = Fixture(); f.owner = SessionIdentity(null, 0); f.slot.get()
        f.owner = SessionIdentity(1, 1); assertEquals(2, f.slot.get())
        f.owner = SessionIdentity(null, 2); assertEquals(3, f.slot.get())
        assertEquals(listOf(1, 2), f.released)
    }
    @Test fun delayedInvalidationKeepsNewlyCreatedCurrentResource() {
        val f = Fixture(); f.slot.get(); f.owner = SessionIdentity(2, 2)
        f.slot.get(); f.slot.clearStale()
        assertEquals(2, f.slot.get()); assertEquals(listOf(1), f.released)
    }
    @Test fun closeReleasesOnceAndNeverReopens() {
        val f = Fixture(); f.slot.get(); f.slot.close(); f.slot.close(); f.slot.clearStale()
        assertEquals(listOf(1), f.released)
        assertThrows(IllegalStateException::class.java) { f.slot.get() }
    }
    @Test fun failedReplacementCanRetryWithoutReleasingOldTwice() {
        var owner = SessionIdentity(1, 1); var fail = false; var count = 0
        val released = mutableListOf<Int>()
        val slot = SessionResourceSlot({ owner }, { if (fail) error("factory"); ++count }, { released += it })
        slot.get(); owner = SessionIdentity(2, 2); fail = true
        assertThrows(IllegalStateException::class.java) { slot.get() }
        fail = false
        assertEquals(2, slot.get()); assertEquals(listOf(1), released)
    }
    @Test fun identityChangeInsideFactoryDisposesResultInsteadOfPublishingIt() {
        var owner = SessionIdentity(1, 1); val released = mutableListOf<Int>()
        val slot = SessionResourceSlot({ owner }, { owner = SessionIdentity(2, 2); 42 }, { released += it })
        assertThrows(CancellationException::class.java) { slot.get() }
        assertEquals(listOf(42), released)
    }
    @Test fun closeInsideFactoryDoesNotLeakItsResult() {
        val released = mutableListOf<Int>(); lateinit var slot: SessionResourceSlot<Int>
        slot = SessionResourceSlot({ SessionIdentity(1, 1) }, { slot.close(); 42 }, { released += it })
        assertThrows(CancellationException::class.java) { slot.get() }
        slot.close(); assertEquals(listOf(42), released)
    }
    @Test fun containerInstancesHaveIndependentResources() {
        val a = Fixture(); val b = Fixture(); a.slot.get(); b.slot.get()
        a.slot.close(); assertTrue(b.released.isEmpty()); assertEquals(1, b.slot.get())
    }
}
