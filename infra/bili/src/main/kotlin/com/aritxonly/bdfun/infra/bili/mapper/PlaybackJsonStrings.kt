package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONArray

internal fun jsonStrings(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        array.optString(index).takeIf { it.isNotBlank() }
    }
}
