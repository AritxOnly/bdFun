package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.LibraryRepository
import com.aritxonly.bdfun.core.model.library.HistoryItem

class BiliLibraryRepository(private val backend: BiliBackend) : LibraryRepository {
    override suspend fun folders() = biliOutcome { backend.favoriteFolders() }
    override suspend fun videos(folderId: Long, page: Int) = biliOutcome { backend.favoriteVideosPage(folderId, page) }
    override suspend fun history(max: Long, viewAt: Long, business: String) = biliOutcome { backend.historyPage(max, viewAt, business) }
    override suspend fun enrich(items: List<HistoryItem>) = biliOutcome { backend.enrichHistoryStats(items) }
    override suspend fun watchLater() = biliOutcome { backend.watchLater() }
    override suspend fun removeFavorite(aid: Long, folderId: Long) = biliOutcome { backend.removeFavoriteAll(aid, listOf(folderId)) }
    override suspend fun addFavorite(aid: Long, folderId: Long) = biliOutcome { backend.setFavorite(aid, folderId, true) }
    override suspend fun deleteHistory(aid: Long) = biliOutcome { backend.deleteHistory(aid) }
    override suspend fun deleteWatchLater(aid: Long) = biliOutcome { backend.deleteWatchLater(aid) }
}
