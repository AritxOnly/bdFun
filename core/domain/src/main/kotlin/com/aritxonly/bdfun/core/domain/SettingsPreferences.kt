package com.aritxonly.bdfun.core.domain

/** 旧偏好格式的最小存储契约，领域和应用层不依赖 Android。 */
interface SettingsPreferences {
    fun contains(key: String): Boolean
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getFloat(key: String, default: Float): Float
    fun getString(key: String, default: String?): String?
    fun getStringSet(key: String, default: Set<String>?): Set<String>?
    fun edit(): SettingsEditor
}

interface SettingsEditor {
    fun putBoolean(key: String, value: Boolean): SettingsEditor
    fun putInt(key: String, value: Int): SettingsEditor
    fun putFloat(key: String, value: Float): SettingsEditor
    fun putString(key: String, value: String?): SettingsEditor
    fun putStringSet(key: String, value: Set<String>?): SettingsEditor
    fun remove(key: String): SettingsEditor
    fun apply()
}

interface SearchHistoryCodec {
    fun decode(raw: String): List<String>
    fun encode(items: List<String>): String
}
