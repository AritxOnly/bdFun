package com.aritxonly.bdfun.presentation

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.application.SessionStateHolder
import com.aritxonly.bdfun.core.application.SessionIdentity
import com.aritxonly.bdfun.core.application.identity
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 旧入口的 Compose 投影；写入统一进入 SessionStateHolder。 */
class SessionPresentationStateHolder internal constructor(
    private val controller: SessionStateHolder, scope: CoroutineScope,
) : MutableState<UserProfile?> {
    private data class Projection(val user: UserProfile?, val identity: SessionIdentity)
    private fun projection() = controller.state.value.let { Projection(it.user, it.identity) }
    private var observed by mutableStateOf(projection())
    val identity: SessionIdentity get() = observed.identity
    override var value: UserProfile?
        get() = observed.user
        set(value) { controller.update(value); observed = projection() }
    override fun component1() = value
    override fun component2(): (UserProfile?) -> Unit = { value = it }
    init { scope.launch { controller.state.collect { observed = Projection(it.user, it.identity) } } }
}
