package com.aritxonly.bdfun.feature.settings.presentation

import androidx.compose.runtime.mutableStateOf
import com.aritxonly.bdfun.core.model.settings.SettingsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** 逐字段投影，避免一次滑杆修改重读全部设置或触发无关消费者重组。 */
internal class SettingsPresentationStateHolder(
    scope: CoroutineScope,
    flow: StateFlow<SettingsUiState>,
    private val afterRefresh: () -> Unit,
) {
    private val properties = LinkedHashMap<String, MutableList<SettingsPropertyStateHolder<*>>>()
    private var previous = flow.value
    init {
        scope.launch {
            flow.collect { next ->
                val old = previous
                previous = next
                if (old.revision != next.revision) refresh()
                else {
                    next.values.filter { (key, value) -> old.values[key] != value }.keys.forEach(::refreshKey)
                    afterRefresh()
                }
            }
        }
    }
    fun refresh() { properties.values.flatten().forEach { it.refresh() }; afterRefresh() }
    private fun refreshKey(key: String) { properties[key]?.forEach { it.refresh() }; afterRefresh() }
    fun <T> property(key: String, read: () -> T, write: (T) -> Unit): ReadWriteProperty<Any?, T> =
        SettingsPropertyStateHolder(read, write) { refreshKey(key) }
            .also { properties.getOrPut(key) { ArrayList() }.add(it) }
    fun <T> readOnly(key: String, read: () -> T): ReadOnlyProperty<Any?, T> = property(key, read, {})

    private class SettingsPropertyStateHolder<T>(
        private val read: () -> T,
        private val write: (T) -> Unit,
        private val changed: () -> Unit,
    ) : ReadWriteProperty<Any?, T> {
        private val observed = mutableStateOf(read())
        fun refresh() { observed.value = read() }
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = observed.value.let { read() }
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { write(value); changed() }
    }
}
