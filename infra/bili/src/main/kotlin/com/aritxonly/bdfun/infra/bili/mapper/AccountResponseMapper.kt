package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.infra.bili.cancellableCatching
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.biliAidToBvid as aidToBvid
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import org.json.JSONObject
import org.json.JSONArray

internal fun mapAccountProfile(data: JSONObject): UserProfile? {
    if (!data.optBoolean("isLogin")) return null
    val level = data.optJSONObject("level_info")?.optInt("current_level") ?: 0
    val vip = data.optJSONObject("vip_label")
    return UserProfile(
        mid = data.l("mid"),
        name = data.s("uname"),
        face = avatar(data.s("face"), 160),
        sign = data.s("sign"),
        level = level,
        coins = data.optDouble("money", 0.0),
        vip = data.optInt("vipStatus") == 1,
        vipLabel = vip?.s("text").orEmpty(),
    )
}

internal fun mapQrLogin(data: JSONObject): QrLogin = QrLogin(url = data.s("url"), key = data.s("auth_code"))

internal fun mapQrPollFailure(root: JSONObject): QrPoll = when (root.optInt("code", -1)) {
        86039, 86101 -> QrPoll(QrState.Waiting, "等待扫码")
        86090 -> QrPoll(QrState.Scanned, "已扫码，请在手机上确认")
        86038 -> QrPoll(QrState.Expired, "二维码已失效")
        else -> QrPoll(
            QrState.Error,
            root.optString("message").ifBlank { "登录失败" },
        )
}
