package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.user.QrLogin
import com.aritxonly.bdfun.core.model.user.QrPoll
import com.aritxonly.bdfun.core.model.user.UserProfile

/** 未登录是成功的 null；接口失败使用 Failure，协程取消继续向上传播。 */
interface AccountRepository {
    suspend fun profile(): Outcome<UserProfile?>
    suspend fun generateQr(): Outcome<QrLogin>
    suspend fun pollQr(key: String): Outcome<QrPoll>
    fun logout()
}

/** 即时登录凭据判断；资料尚未加载时也不能等同于未登录。 */
fun interface LoginStatus { fun isLoggedIn(): Boolean }
