package com.aritxonly.bdfun.core.application

import org.junit.Assert.*
import org.junit.Test

class CommentLikeStoreTest {
    @Test fun snapshotsKeepOnlyConfiguredNumberOfEntries() {
        val store = CommentLikeStore(2)
        store.put(1, true, 1); store.put(2, true, 2); store.put(3, false, 3)
        assertEquals(setOf(2L, 3L), store.state.value.keys)
    }
    @Test fun clearingAccountRejectsOldRequestRollback() {
        val store = CommentLikeStore()
        val old = store.generation
        store.put(1, true, 3)
        store.clear()
        store.put(1, false, 2, old)
        assertTrue(store.state.value.isEmpty())
    }
    @Test fun updatingEntryDoesNotMutatePreviouslyPublishedSnapshot() {
        val store = CommentLikeStore()
        store.put(1, true, 1)
        val old = store.state.value
        store.put(1, false, 0)
        assertEquals(CommentLike(true, 1), old[1])
        assertEquals(CommentLike(false, 0), store.state.value[1])
    }
    @Test fun emptyAccountClearStillPublishesNewLifetimeForPresentationOwners() {
        val store = CommentLikeStore()
        assertEquals(0L, store.generationState.value)
        store.clear(); assertEquals(1L, store.generationState.value)
        assertEquals(store.generation, store.generationState.value)
        store.clear(); assertEquals(2L, store.generationState.value)
        assertTrue(store.state.value.isEmpty())
    }
}
