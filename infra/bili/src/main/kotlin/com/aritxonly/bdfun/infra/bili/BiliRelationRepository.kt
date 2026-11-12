package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.RelationRepository
import kotlinx.coroutines.CancellationException

class BiliRelationRepository(private val backend: BiliBackend) : RelationRepository {
    override suspend fun users(mid: Long, fans: Boolean, page: Int, frequent: Boolean) =
        relationResult("加载失败，请重试") { backend.relationUsers(mid, fans, page, frequent) }
    override suspend fun attribute(mid: Long) = relationResult("操作失败，请重试") { backend.relation(mid) }
    override suspend fun follow(mid: Long, followed: Boolean) = relationResult("操作失败，请重试") {
        backend.followUser(mid, followed); Unit
    }
}
/** 关系页只展示原异常文案；不把原空串变成统一错误提示。 */
internal suspend fun <T> relationResult(fallback: String, block: suspend () -> T): Outcome<T> = try {
    Outcome.Success(block())
} catch (cancel: CancellationException) {
    throw cancel
} catch (failure: Exception) {
    Outcome.Failure(AppError.Unknown(failure.message ?: fallback))
}
