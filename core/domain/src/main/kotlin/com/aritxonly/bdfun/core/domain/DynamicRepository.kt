package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.feed.*

interface DynamicRepository {
    suspend fun dynamicPage(type: String, offset: String, hostMid: Long?): Outcome<DynamicPage>
    suspend fun dynamicPortal(): Outcome<DynamicPortal>
    suspend fun dynamicUpList(offset: String): Outcome<DynamicUpPage>
    suspend fun likeDynamic(id: String, like: Boolean): Outcome<Boolean>
}
