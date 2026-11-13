package com.aritxonly.bdfun.infra.bili.message

import com.aritxonly.bdfun.core.model.message.*
import org.json.JSONObject

internal fun mapImSettings(data: JSONObject): ImSettingsPage {
    val settings = data.optJSONArray("settings").imObjects().map { entry ->
        val value = entry.getJSONObject("value")
        val switch = value.optJSONObject("switch")
        val select = value.optJSONObject("select")
        val redirect = value.optJSONObject("redirect")
        val page = redirect?.optJSONObject("setting_page")
        val choices = (select?.optJSONArray("item") ?: redirect?.optJSONObject("window_select")?.optJSONArray("item"))
            .imObjects().map { ImSettingChoice(it.optInt("item_type"), it.optString("text"), it.optBoolean("selected")) }
        val content = switch ?: redirect ?: value.optJSONObject("text") ?: JSONObject()
        ImSetting(entry.optInt("key"), content.optString("title").ifBlank { content.optString("text") },
            content.optString("subtitle"), switch?.optBoolean("switch_on"), choices,
            page?.takeIf { it.has("parent_setting_type") }?.optInt("parent_setting_type"),
            page?.optString("url").orEmpty().ifBlank { redirect?.optJSONObject("other_page")?.optString("url").orEmpty() },
            redirect?.optString("selected_summary").orEmpty(), value.toString())
    }.sortedBy { it.key }
    return ImSettingsPage(data.optString("page_title"), settings)
}

internal fun settingUpdate(setting: ImSetting, enabled: Boolean?, choice: Int?): JSONObject {
    val value = JSONObject(setting.token)
    if (enabled != null) value.getJSONObject("switch").put("switch_on", enabled)
    if (choice != null) {
        val select = value.optJSONObject("select") ?: value.getJSONObject("redirect").getJSONObject("window_select")
        select.getJSONArray("item").imObjects().forEach { it.put("selected", it.optInt("item_type") == choice) }
        value.optJSONObject("redirect")?.put("selected_summary", select.getJSONArray("item").imObjects()
            .firstOrNull { it.optBoolean("selected") }?.optString("text").orEmpty())
    }
    return JSONObject().put("settings", org.json.JSONArray().put(JSONObject().put("key", setting.key).put("value", value)))
}
