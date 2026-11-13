package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.core.model.video.VideoItem

interface LibraryRepository {
    suspend fun folders(): Outcome<List<FavoriteFolder>>
    suspend fun videos(folderId: Long, page: Int): Outcome<Pair<List<VideoItem>, Boolean>>
    suspend fun history(max: Long, viewAt: Long, business: String): Outcome<HistoryPage>
    suspend fun enrich(items: List<HistoryItem>): Outcome<List<HistoryItem>>
    suspend fun watchLater(): Outcome<List<HistoryItem>>
    suspend fun removeFavorite(aid: Long, folderId: Long): Outcome<Boolean>
    suspend fun addFavorite(aid: Long, folderId: Long): Outcome<Boolean>
    suspend fun deleteHistory(aid: Long): Outcome<Boolean>
    suspend fun deleteWatchLater(aid: Long): Outcome<Boolean>
}
