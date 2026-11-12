package com.aritxonly.bdfun.feature.relations.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.RelationRepository
import com.aritxonly.bdfun.core.model.user.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RelationFeedStateHolderTest {
    private val unknown = RelationUser(2, "user", "", "")
    private class Fake : RelationRepository {
        var list: Outcome<RelationPage> = Outcome.Success(RelationPage(emptyList(), 0, null))
        var relation: Outcome<Int?> = Outcome.Success(0)
        var action: Outcome<Unit> = Outcome.Success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        var reads = 0; var queries = 0
        val follows = mutableListOf<Pair<Long, Boolean>>()
        override suspend fun users(mid: Long, fans: Boolean, page: Int, frequent: Boolean): Outcome<RelationPage> {
            reads++; gate?.await(); return list
        }
        override suspend fun attribute(mid: Long): Outcome<Int?> { queries++; gate?.await(); return relation }
        override suspend fun follow(mid: Long, followed: Boolean): Outcome<Unit> {
            follows += mid to followed; gate?.await(); return action
        }
    }
    private fun holder(fake: Fake, owner: () -> Long? = { 1L }, loggedIn: () -> Boolean = { true }) =
        RelationFeedStateHolder(fake, 10, false, false, 1L, owner, loggedIn)
    @Test fun unknownAttributeOnlyResolvesStatusAndNeedsAnotherClickToFollow() = runBlocking {
        val fake = Fake().apply { list = Outcome.Success(RelationPage(listOf(unknown), 1, null)) }
        val holder = holder(fake); holder.fetch()
        assertNull(holder.changeFollow(unknown)); assertEquals(1, fake.queries); assertTrue(fake.follows.isEmpty())
        assertEquals(0, holder.items.single().attribute)
        assertEquals("关注成功", holder.changeFollow(holder.items.single()))
        assertEquals(listOf(2L to true), fake.follows); assertEquals(2, holder.items.single().attribute)
        assertEquals("已取消关注", holder.changeFollow(holder.items.single()))
        assertEquals(0, holder.items.single().attribute)
    }
    @Test fun missingAttributeKeepsRetryActionAndOriginalError() = runBlocking {
        val fake = Fake().apply { relation = Outcome.Success(null) }; val holder = holder(fake)
        assertEquals("关注状态获取失败，请重试", holder.changeFollow(unknown)); assertTrue(fake.follows.isEmpty())
        assertTrue(holder.state.value.busyUsers.isEmpty())
    }
    @Test fun loggedOutDoesNotRequestOrMarkUserBusy() = runBlocking {
        val fake = Fake(); val holder = holder(fake, loggedIn = { false })
        assertEquals("请先登录", holder.changeFollow(unknown)); assertEquals(0, fake.queries)
        assertTrue(holder.state.value.busyUsers.isEmpty())
    }
    @Test fun concurrentClicksDoNotDuplicateAndCancellationReleasesBusy() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }; val holder = holder(fake)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.changeFollow(unknown.copy(attribute = 0)) }
        assertEquals(setOf(2L), holder.state.value.busyUsers)
        assertNull(holder.changeFollow(unknown.copy(attribute = 0))); assertEquals(1, fake.follows.size)
        job.cancelAndJoin(); assertTrue(holder.state.value.busyUsers.isEmpty())
        fake.gate = null; assertEquals("关注成功", holder.changeFollow(unknown.copy(attribute = 0)))
    }
    @Test fun accountChangeRejectsLateRowsAndFutureRequests() = runBlocking {
        var owner: Long? = 1
        val fake = Fake().apply { gate = CompletableDeferred(); list = Outcome.Success(RelationPage(listOf(unknown), 1, null)) }
        val holder = holder(fake, owner = { owner })
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.fetch() }
        owner = null; fake.gate!!.complete(Unit); job.join(); holder.fetch()
        assertTrue(holder.items.isEmpty()); assertFalse(holder.loading); assertEquals(1, fake.reads)
        assertNull(holder.changeFollow(unknown)); assertEquals(0, fake.queries)
    }
    @Test fun closeRejectsLateRelationAndNeverContinuesToFollow() = runBlocking {
        val fake = Fake().apply { gate = CompletableDeferred() }; val holder = holder(fake)
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.changeFollow(unknown) }
        holder.close(); fake.gate!!.complete(Unit); assertNull(job.await())
        assertTrue(fake.follows.isEmpty()); assertTrue(holder.state.value.busyUsers.isEmpty())
        holder.fetch(); assertEquals(0, fake.reads)
    }
    @Test fun followFailurePreservesRowsAndRawBlankMessage() = runBlocking {
        val user = unknown.copy(attribute = 6)
        val fake = Fake().apply { list = Outcome.Success(RelationPage(listOf(user), 1, null)); action = Outcome.Failure(AppError.Unknown("")) }
        val holder = holder(fake); holder.fetch()
        assertEquals("", holder.changeFollow(user)); assertEquals(6, holder.items.single().attribute)
        assertTrue(holder.state.value.busyUsers.isEmpty()); assertEquals(listOf(2L to false), fake.follows)
    }
    @Test fun closedFollowCompletionDoesNotPublishSuccessToastOrAttribute() = runBlocking {
        val user = unknown.copy(attribute = 0)
        val fake = Fake().apply { list = Outcome.Success(RelationPage(listOf(user), 1, null)) }; val holder = holder(fake)
        holder.fetch(); fake.gate = CompletableDeferred()
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.changeFollow(user) }
        holder.close(); fake.gate!!.complete(Unit); assertNull(job.await())
        assertEquals(0, holder.items.single().attribute); assertTrue(holder.state.value.busyUsers.isEmpty())
    }
}
