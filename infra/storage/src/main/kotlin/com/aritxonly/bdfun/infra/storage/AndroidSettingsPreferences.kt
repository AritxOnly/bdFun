package com.aritxonly.bdfun.infra.storage

import android.content.Context
import android.content.SharedPreferences
import com.aritxonly.bdfun.core.domain.SettingsPreferences
import com.aritxonly.bdfun.core.domain.SettingsEditor
import com.aritxonly.bdfun.core.domain.SearchHistoryCodec
import org.json.JSONArray

class AndroidSettingsPreferences(context: Context) : SettingsPreferences {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    override fun contains(key: String) = prefs.contains(key)
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun getFloat(key: String, default: Float) = prefs.getFloat(key, default)
    override fun getString(key: String, default: String?) = prefs.getString(key, default)
    override fun getStringSet(key: String, default: Set<String>?) = prefs.getStringSet(key, default)?.toSet()
    override fun edit(): SettingsEditor = AndroidSettingsEditor(prefs.edit())
}

private class AndroidSettingsEditor(private val editor: SharedPreferences.Editor) : SettingsEditor {
    override fun putBoolean(key: String, value: Boolean) = apply { editor.putBoolean(key, value) }
    override fun putInt(key: String, value: Int) = apply { editor.putInt(key, value) }
    override fun putFloat(key: String, value: Float) = apply { editor.putFloat(key, value) }
    override fun putString(key: String, value: String?) = apply { editor.putString(key, value) }
    override fun putStringSet(key: String, value: Set<String>?) = apply { editor.putStringSet(key, value?.toSet()) }
    override fun remove(key: String) = apply { editor.remove(key) }
    override fun apply() = editor.apply()
}

class JsonSearchHistoryCodec : SearchHistoryCodec {
    override fun decode(raw: String): List<String> = try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val value = array.optString(i)
                if (value.isNotBlank()) add(value)
            }
        }
    } catch (e: Exception) { emptyList() }
    override fun encode(items: List<String>): String = JSONArray(items).toString()
}
