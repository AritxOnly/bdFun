package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface SessionStore {
    val state: StateFlow<SessionState>
    val currentUser: UserProfile? get() = state.value.user
    fun update(user: UserProfile?)
}
