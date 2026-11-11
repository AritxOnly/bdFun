package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.user.UserProfile

interface SessionStore {
    val currentUser: UserProfile?
    fun update(user: UserProfile?)
}
