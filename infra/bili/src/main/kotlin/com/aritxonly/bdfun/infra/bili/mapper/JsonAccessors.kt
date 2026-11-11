package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import org.json.JSONArray

fun JSONObject.s(key: String): String {
    if (isNull(key)) return ""
    return optString(key, "").takeIf { it != "null" } ?: ""
}

fun JSONObject.l(key: String): Long = optLong(key, 0L)

fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    val result = ArrayList<JSONObject>(length())
    for (i in 0 until length()) {
        optJSONObject(i)?.let(result::add)
    }
    return result
}
