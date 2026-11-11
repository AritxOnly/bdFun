package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.*

internal class MemorySettingsPreferences(val values: MutableMap<String, Any> = linkedMapOf()) : SettingsPreferences {
    var applies = 0
    override fun contains(key: String) = values.containsKey(key)
    override fun getBoolean(key: String, default: Boolean) = values[key] as? Boolean ?: default
    override fun getInt(key: String, default: Int) = values[key] as? Int ?: default
    override fun getFloat(key: String, default: Float) = values[key] as? Float ?: default
    override fun getString(key: String, default: String?) = values[key] as? String ?: default
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, default: Set<String>?) = (values[key] as? Set<String>)?.toSet() ?: default
    override fun edit(): SettingsEditor = Editor()
    private inner class Editor : SettingsEditor {
        private val changes = linkedMapOf<String, Any?>()
        override fun putBoolean(key: String, value: Boolean) = apply { changes[key] = value }
        override fun putInt(key: String, value: Int) = apply { changes[key] = value }
        override fun putFloat(key: String, value: Float) = apply { changes[key] = value }
        override fun putString(key: String, value: String?) = apply { changes[key] = value }
        override fun putStringSet(key: String, value: Set<String>?) = apply { changes[key] = value?.toSet() }
        override fun remove(key: String) = apply { changes[key] = null }
        override fun apply() { applies++; changes.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value } }
    }
}

internal object TestHistoryCodec : SearchHistoryCodec {
    override fun decode(raw: String) = raw.split('\u0001').filter { it.isNotBlank() }
    override fun encode(items: List<String>) = items.joinToString("\u0001")
}
