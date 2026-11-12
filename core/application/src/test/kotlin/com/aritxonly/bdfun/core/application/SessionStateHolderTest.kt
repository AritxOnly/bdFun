package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.AccountRepository
import com.aritxonly.bdfun.core.model.user.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SessionStateHolderTest {
    private fun user(mid: Long) = UserProfile(mid, "姓名", "", "", 1, 0.0, false, "")
    private class Fake : AccountRepository {
        var profile: Outcome<UserProfile?> = Outcome.Success(null)
        var gate: CompletableDeferred<Unit>? = null
        var logoutCount = 0
        override suspend fun profile(): Outcome<UserProfile?> { gate?.await(); return profile }
        override suspend fun generateQr(): Outcome<QrLogin> = error("unused")
        override suspend fun pollQr(key: String): Outcome<QrPoll> = error("unused")
        override fun logout() { logoutCount++ }
    }
    @Test fun loginDuringOldRefreshCannotBeOverwritten() = runBlocking {
        val repository = Fake(); repository.gate = CompletableDeferred()
        val holder = SessionStateHolder(repository)
        val request = launch(start = CoroutineStart.UNDISPATCHED) { holder.refresh() }
        holder.update(user(2)); repository.gate!!.complete(Unit); request.join()
        assertEquals(2L, holder.currentUser?.mid)
        assertFalse(holder.state.value.refreshing)
    }
    @Test fun networkFailureKeepsSignedInUser() = runBlocking {
        val repository = Fake(); repository.profile = Outcome.Failure(AppError.Network("offline"))
        val holder = SessionStateHolder(repository); holder.update(user(1)); holder.refresh()
        assertEquals(1L, holder.currentUser?.mid)
        assertEquals(AppError.Network("offline"), holder.state.value.error)
    }
    @Test fun expiredLoginClearsUser() = runBlocking {
        val repository = Fake(); repository.profile = Outcome.Failure(AppError.LoginExpired)
        val holder = SessionStateHolder(repository); holder.update(user(1)); holder.refresh()
        assertNull(holder.currentUser)
    }
    @Test fun logoutInvalidatesInFlightRefreshAndClearsCookiesThroughRepository() = runBlocking {
        val repository = Fake(); repository.profile = Outcome.Success(user(1)); repository.gate = CompletableDeferred()
        val holder = SessionStateHolder(repository); holder.update(user(1))
        val request = launch(start = CoroutineStart.UNDISPATCHED) { holder.refresh() }
        holder.logout(); repository.gate!!.complete(Unit); request.join()
        assertNull(holder.currentUser); assertEquals(1, repository.logoutCount)
    }
    @Test fun cancelledRefreshUnlocksWithoutInventingError() = runBlocking {
        val repository = Fake(); repository.gate = CompletableDeferred()
        val holder = SessionStateHolder(repository)
        val request = launch(start = CoroutineStart.UNDISPATCHED) { holder.refresh() }
        request.cancelAndJoin()
        assertFalse(holder.state.value.refreshing); assertNull(holder.state.value.error)
    }

    @Test fun refreshIdentityChangesAdvanceRevisionAndReleaseBusy() = runBlocking {
        val repository = Fake(); val holder = SessionStateHolder(repository)
        repository.profile = Outcome.Success(user(1)); holder.refresh()
        val first = holder.state.value.revision
        assertEquals(1L, first); assertFalse(holder.state.value.refreshing)
        repository.profile = Outcome.Success(user(2)); holder.refresh()
        repository.profile = Outcome.Success(user(1)); holder.refresh()
        assertEquals(first + 2, holder.state.value.revision)
        assertFalse(holder.state.value.refreshing)
    }
    @Test fun expiredOrNullProfileInvalidatesIdentityOnlyOnce() = runBlocking {
        val repository = Fake(); val holder = SessionStateHolder(repository); holder.update(user(1))
        val initial = holder.state.value.revision
        repository.profile = Outcome.Failure(AppError.LoginExpired); holder.refresh()
        assertEquals(initial + 1, holder.state.value.revision); assertFalse(holder.state.value.refreshing)
        holder.refresh(); assertEquals(initial + 1, holder.state.value.revision)
        holder.update(user(1)); val signed = holder.state.value.revision
        repository.profile = Outcome.Success(null); holder.refresh()
        assertEquals(signed + 1, holder.state.value.revision); assertNull(holder.currentUser)
        assertFalse(holder.state.value.refreshing)
    }
    @Test fun sameAccountProfileRefreshAndTransientErrorsKeepRevision() = runBlocking {
        val repository = Fake(); val holder = SessionStateHolder(repository); holder.update(user(1))
        val initial = holder.state.value.revision
        repository.profile = Outcome.Success(user(1).copy(name = "updated")); holder.refresh()
        assertEquals(initial, holder.state.value.revision); assertEquals("updated", holder.currentUser?.name)
        repository.profile = Outcome.Failure(AppError.Network("offline")); holder.refresh()
        assertEquals(initial, holder.state.value.revision); assertFalse(holder.state.value.refreshing)
    }

    @Test fun resourceSlotUsesActualSessionRevisionAcrossConflatedIdentityChanges() = runBlocking {
        val repository = Fake(); val holder = SessionStateHolder(repository); holder.update(user(1))
        var released = 0
        val slot = SessionResourceSlot({ holder.identity }, { Any() }, { released++ })
        val original = slot.get()
        holder.update(user(2)); holder.update(user(1))
        val replacement = slot.get()
        assertNotSame(original, replacement); assertEquals(1, released)
        repository.profile = Outcome.Success(user(1).copy(name = "新资料")); holder.refresh()
        assertSame(replacement, slot.get())
        repository.profile = Outcome.Failure(AppError.LoginExpired); holder.refresh()
        assertNotSame(replacement, slot.get()); assertEquals(2, released)
        slot.close(); assertEquals(3, released)
    }
}
