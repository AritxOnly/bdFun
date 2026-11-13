package com.aritxonly.bdfun.data

import org.json.JSONObject
import org.json.JSONArray
import com.aritxonly.bdfun.infra.bili.mapper.s as infraString
import com.aritxonly.bdfun.infra.bili.mapper.l as infraLong
import com.aritxonly.bdfun.infra.bili.mapper.objects as infraObjects

internal fun JSONObject.s(key: String) = infraString(key)
internal fun JSONObject.l(key: String) = infraLong(key)
internal fun JSONArray?.objects() = infraObjects()
