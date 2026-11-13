package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject

/** 播放地址接口返回内容的形状，写进应用日志，方便定位“有 data 但没有地址”。 */

internal fun describePlayData(data: JSONObject): String {
    val dash = data.optJSONObject("dash")
    val video = dash?.optJSONArray("video")
    val audio = dash?.optJSONArray("audio")
    val durl = data.optJSONArray("durl")
    val firstRep = video?.optJSONObject(0)
    val firstRaw = firstRep?.let { it.s("baseUrl").ifBlank { it.s("base_url") } }.orEmpty()
    val first = firstRaw.substringBefore('?')
    val repKeys = firstRep?.keys()?.asSequence()?.take(12)?.joinToString(",").orEmpty()
    val dataKeys = data.keys().asSequence().take(16).joinToString(",")
    return "dataKeys=$dataKeys dash=${dash != null} video=${video?.length() ?: 0} " +
        "audio=${audio?.length() ?: 0} durl=${durl?.length() ?: 0} " +
        "firstBaseLen=${firstRaw.length} repKeys=$repKeys first=${first.take(80)}"
}
