package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.data.RelationPage
import com.aritxonly.bdfun.data.RelationUser
import kotlinx.coroutines.CancellationException

/** Failed pages retain their cursor and existing rows; refreshing replaces only after success. */
internal class RelationFeedState(private val load: suspend (Int) -> RelationPage) {
    var items by mutableStateOf<List<RelationUser>>(emptyList()); private set
    var total by mutableIntStateOf(0); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var nextPage by mutableStateOf<Int?>(1); private set
    private var retryReset = false

    suspend fun fetch(reset: Boolean = false) {
        if (loading) return
        val page = if (reset) 1 else nextPage ?: return
        loading = true
        error = null
        retryReset = reset
        try {
            val result = load(page)
            val previous = if (reset) emptyList() else items
            val merged = (previous + result.items).distinctBy { it.mid }
            items = merged
            total = result.total
            nextPage = result.nextPage.takeUnless { !reset && merged.size == previous.size }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message ?: "加载失败，请重试" }
        finally { loading = false }
    }
    suspend fun retry() = fetch(retryReset)
    fun updateRelation(mid: Long, attribute: Int) {
        items = items.map { if (it.mid == mid) it.copy(attribute = attribute) else it }
    }
}
