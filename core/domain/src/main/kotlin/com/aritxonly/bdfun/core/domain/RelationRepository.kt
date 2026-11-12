package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.user.RelationPage

interface RelationRepository {
    suspend fun users(mid: Long, fans: Boolean, page: Int, frequent: Boolean): Outcome<RelationPage>
    suspend fun attribute(mid: Long): Outcome<Int?>
    suspend fun follow(mid: Long, followed: Boolean): Outcome<Unit>
}
