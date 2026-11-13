package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.flow.StateFlow

/** 页面状态快照契约；实现应保持不可变，更新由所属状态容器统一发出。 */
interface UiState

/** 只暴露状态读取；具体页面事件保留在业务容器，避免统一事件总线。 */
interface StateHolder<out S : UiState> {
    val state: StateFlow<S>
}
