package com.aritxonly.bdfun.feature.space.presentation

import androidx.lifecycle.ViewModel
import com.aritxonly.bdfun.core.application.SessionResourceSlot
import com.aritxonly.bdfun.feature.space.application.SpaceStateHolder

/** 由Activity的ViewModelStore持有，不把页面实例放回进程级mid缓存。 */
internal class SpacePageViewModel(private val resources: SessionResourceSlot<SpaceStateHolder>) : ViewModel() {
    fun currentState(): SpaceStateHolder = resources.get()
    override fun onCleared() { resources.close() }
}
