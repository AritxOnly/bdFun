package com.aritxonly.bdfun.feature.library.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.LibraryRepository
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class LibraryLifetimeTest {
    private class Fake : LibraryRepository {
        val calls=mutableListOf<String>()
        var hook:suspend (String)->Unit={}
        val videos=(1..2).map { VideoItem("BV$it",it.toLong(),"title","","UP","",0,0,0,10,0) }
        val entries=(1..2).map { HistoryItem("BV$it","title","","UP",0,0,10,aid=it.toLong()) }
        private suspend fun hit(key:String) { calls+=key; hook(key) }
        override suspend fun folders():Outcome<List<FavoriteFolder>> { hit("folders"); return Outcome.Success(listOf(FavoriteFolder(1,"source",2,""),FavoriteFolder(2,"target",2,""))) }
        override suspend fun videos(folderId:Long,page:Int):Outcome<Pair<List<VideoItem>,Boolean>> { hit("videos"); return Outcome.Success(videos to true) }
        override suspend fun history(max:Long,viewAt:Long,business:String):Outcome<HistoryPage> { hit("history"); return Outcome.Success(HistoryPage(entries,10,20,"archive",true)) }
        override suspend fun enrich(items:List<HistoryItem>):Outcome<List<HistoryItem>> { hit("enrich"); return Outcome.Success(items) }
        override suspend fun watchLater():Outcome<List<HistoryItem>> { hit("later"); return Outcome.Success(entries) }
        override suspend fun addFavorite(aid:Long,folderId:Long):Outcome<Boolean> { hit("add"); return Outcome.Success(true) }
        override suspend fun removeFavorite(aid:Long,folderId:Long):Outcome<Boolean> { hit("remove"); return Outcome.Success(true) }
        override suspend fun deleteHistory(aid:Long):Outcome<Boolean> { hit("deleteHistory"); return Outcome.Success(true) }
        override suspend fun deleteWatchLater(aid:Long):Outcome<Boolean> { hit("deleteLater"); return Outcome.Success(true) }
    }
    @Test fun closedControllerCannotStartAnyFutureRequest() = runBlocking {
        val fake=Fake(); val h=LibraryStateHolder(fake); h.close(); val closed=h.state.value
        h.loadFavorites(); h.loadFolder(0); h.loadMoreFolders(); h.loadHistory(); h.loadMoreHistory(); h.loadWatchLater()
        assertNull(h.deleteSelected(0,setOf("BV1"))); assertNull(h.transferSelected(true,FavoriteFolder(2,"target",0,""),setOf("BV1")))
        assertTrue(fake.calls.isEmpty()); assertEquals(closed,h.state.value)
    }
    @Test fun closeRejectsAllLateLoadStagesAndDoesNotContinueEnrichmentOrFolderRequests() = runBlocking {
        for(stage in listOf("folders","videos","history","enrich","later")) {
            val fake=Fake(); val h=LibraryStateHolder(fake)
            if(stage=="videos") h.loadFavorites()
            val gate=CompletableDeferred<Unit>(); fake.hook={ if(it==stage)gate.await() }
            val job=launch(start=CoroutineStart.UNDISPATCHED) {
                when(stage) { "folders"->h.loadFavorites(); "videos"->h.loadFolder(1); "later"->h.loadWatchLater(); else->h.loadHistory() }
            }
            h.close(); val closed=h.state.value; val count=fake.calls.size
            gate.complete(Unit); job.join()
            assertTrue(job.isCancelled); assertEquals(count,fake.calls.size); assertEquals(closed,h.state.value)
            assertTrue(closed.loading.values.none { it }); assertFalse(closed.busy)
        }
    }
    @Test fun closeReleasesBothPaginationFlagsWithoutAdvancingLateCursor() = runBlocking {
        for(mode in listOf(0,1)) {
            val fake=Fake(); val h=LibraryStateHolder(fake); h.loadFavorites(); h.loadHistory()
            val gate=CompletableDeferred<Unit>(); fake.hook={ if(it==if(mode==0)"videos" else "history")gate.await() }
            val job=launch(start=CoroutineStart.UNDISPATCHED) { if(mode==0)h.loadMoreFolders() else h.loadMoreHistory() }
            h.close(); val closed=h.state.value; gate.complete(Unit); job.join()
            assertEquals(closed,h.state.value); assertFalse(closed.favorites.loadingMore); assertFalse(closed.history.loadingMore)
        }
    }
    @Test fun closeStopsDeleteLoopForAllModesAndSuppressesLateRemoval() = runBlocking {
        for(mode in 0..2) {
            val fake=Fake(); val h=LibraryStateHolder(fake); h.loadFavorites(); h.loadHistory(); h.loadWatchLater()
            val key=listOf("remove","deleteHistory","deleteLater")[mode]; val gate=CompletableDeferred<Unit>()
            fake.hook={ if(it==key)gate.await() }
            val job=launch(start=CoroutineStart.UNDISPATCHED) { h.deleteSelected(mode,setOf("BV1","BV2")) }
            h.close(); val closed=h.state.value; gate.complete(Unit); job.join()
            assertEquals(1,fake.calls.count { it==key }); assertEquals(closed,h.state.value); assertFalse(h.state.value.busy)
        }
    }
    @Test fun closeAfterAddPreventsSourceDeleteAndNextTransfer() = runBlocking {
        val fake=Fake(); val h=LibraryStateHolder(fake); h.loadFavorites()
        val gate=CompletableDeferred<Unit>(); fake.hook={ if(it=="add")gate.await() }
        val job=launch(start=CoroutineStart.UNDISPATCHED) { h.transferSelected(true,FavoriteFolder(2,"target",0,""),setOf("BV1","BV2")) }
        h.close(); val closed=h.state.value; gate.complete(Unit); job.join()
        assertEquals(1,fake.calls.count { it=="add" }); assertFalse("remove" in fake.calls); assertEquals(closed,h.state.value)
    }
    @Test fun confirmedPartialDeletionRemainsRemovedWhenLaterRequestIsClosed() = runBlocking {
        val fake=Fake(); val h=LibraryStateHolder(fake); h.loadFavorites(); val gate=CompletableDeferred<Unit>()
        fake.hook={ if(it=="remove" && fake.calls.count { call->call=="remove" }==2)gate.await() }
        val job=launch(start=CoroutineStart.UNDISPATCHED) { h.deleteSelected(0,setOf("BV1","BV2")) }
        assertEquals(listOf("BV2"),h.state.value.favorites.videos.map { it.bvid })
        h.close(); val closed=h.state.value; gate.complete(Unit); job.join(); assertEquals(closed,h.state.value)
    }
    @Test fun swallowedTransportCancellationCannotContinueButOrdinaryRetryCan() = runBlocking {
        val fake=Fake(); val h=LibraryStateHolder(fake)
        fake.hook={ if(it=="folders")try { awaitCancellation() } catch(_:CancellationException) {} }
        val job=launch(start=CoroutineStart.UNDISPATCHED) { h.loadFavorites() }; job.cancelAndJoin()
        assertEquals(listOf("folders"),fake.calls); assertFalse(h.state.value.loading[0]==true)
        fake.hook={}; h.loadFavorites(); assertTrue(h.state.value.favorites.ready)
    }

    @Test fun accountInvalidationStopsTransferBeforeComposeDisposalAndRejectsFutureCalls() = runBlocking {
        var owner=1L; val fake=Fake(); val h=LibraryStateHolder(fake) { owner==1L }; h.loadFavorites()
        val gate=CompletableDeferred<Unit>(); fake.hook={ if(it=="add")gate.await() }
        val job=launch(start=CoroutineStart.UNDISPATCHED) { h.transferSelected(true,FavoriteFolder(2,"target",0,""),setOf("BV1","BV2")) }
        owner=2L; gate.complete(Unit); job.join()
        assertTrue(job.isCancelled); assertFalse("remove" in fake.calls); assertFalse(h.isActive)
        val before=fake.calls.size
        h.loadFavorites(); h.loadHistory(); h.loadWatchLater()
        assertNull(h.deleteSelected(0,setOf("BV1")))
        assertNull(h.transferSelected(false,FavoriteFolder(2,"target",0,""),setOf("BV1")))
        assertEquals(before,fake.calls.size)
    }
}
