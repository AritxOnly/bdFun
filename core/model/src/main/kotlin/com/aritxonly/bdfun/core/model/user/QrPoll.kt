package com.aritxonly.bdfun.core.model.user

data class QrPoll(
    val state: QrState,
    val message: String,
    val profile: UserProfile? = null,
)
