package com.aritxonly.bdfun.presentation

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import com.aritxonly.bdfun.core.application.SessionStateHolder
import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.AccountRepository
import com.aritxonly.bdfun.core.model.user.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class SessionPresentationStateHolderTest {
    private class QueuedDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.addLast(block) }
        fun drain() { while (tasks.isNotEmpty()) tasks.removeFirst().run() }
    }
    private fun user(mid: Long) = UserProfile(mid, "用户", "", "", 1, 0.0, false, "")
    private class Account : AccountRepository {
        var profile: Outcome<UserProfile?> = Outcome.Success(null)
        override suspend fun profile() = profile
        override suspend fun generateQr(): Outcome<QrLogin> = error("unused")
        override suspend fun pollQr(key: String): Outcome<QrPoll> = error("unused")
        override fun logout() = Unit
    }
    private fun fixture(block: (SessionPresentationStateHolder, SessionStateHolder, Account, QueuedDispatcher) -> Unit) {
        val dispatcher = QueuedDispatcher(); val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val account = Account(); val controller = SessionStateHolder(account); controller.update(user(1))
        val view = SessionPresentationStateHolder(controller, scope); dispatcher.drain()
        try { block(view, controller, account, dispatcher) } finally { scope.cancel(); dispatcher.drain() }
    }
    private fun observing(view: SessionPresentationStateHolder, block: (()->Int) -> Unit) {
        Snapshot.sendApplyNotifications()
        var changes = 0
        val observer = SnapshotStateObserver { it() }; observer.start()
        try {
            observer.observeReads(Any(), { changes++ }) { view.value }
            block { Snapshot.sendApplyNotifications(); changes }
        } finally { observer.stop(); observer.clear() }
    }
    @Test fun conflatedRoundTripInvalidatesReadersEvenWhenFinalProfileIsEqual() = fixture { view, controller, _, dispatcher ->
        val original = view.identity
        observing(view) { changes ->
            controller.update(user(2)); controller.update(user(1)); dispatcher.drain()
            assertEquals(1L, view.value?.mid); assertEquals(original.revision + 2, view.identity.revision)
            assertEquals(1, changes())
        }
    }
    @Test fun guestRoundTripAlsoInvalidates() = fixture { view, controller, _, dispatcher ->
        view.value = null; dispatcher.drain(); val revision = view.identity.revision
        observing(view) { changes ->
            controller.update(user(1)); controller.update(null); dispatcher.drain()
            assertNull(view.value); assertEquals(revision + 2, view.identity.revision); assertEquals(1, changes())
        }
    }
    @Test fun compatibilityWritesPublishProfileAndIdentitySynchronously() = fixture { view, controller, _, _ ->
        val revision = view.identity.revision
        view.value = user(2)
        assertEquals(controller.state.value.revision, view.identity.revision)
        assertEquals(revision + 1, view.identity.revision); assertEquals(2L, view.value?.mid)
    }
    @Test fun refreshingAndTransientFailureDoNotInvalidateUnchangedProfile() = fixture { view, controller, account, dispatcher ->
        val identity = view.identity
        account.profile = Outcome.Failure(AppError.Network("offline"))
        observing(view) { changes ->
            runBlocking { controller.refresh() }; dispatcher.drain()
            assertEquals(identity, view.identity); assertEquals(0, changes())
        }
    }
    @Test fun sameAccountProfileRefreshUpdatesFieldsWithoutChangingIdentity() = fixture { view, controller, account, dispatcher ->
        val identity = view.identity
        account.profile = Outcome.Success(user(1).copy(name = "新资料"))
        observing(view) { changes ->
            runBlocking { controller.refresh() }; dispatcher.drain()
            assertEquals(identity, view.identity); assertEquals("新资料", view.value?.name); assertEquals(1, changes())
        }
    }
}
