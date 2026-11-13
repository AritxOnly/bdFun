package com.aritxonly.bdfun.data

typealias BiliException = com.aritxonly.bdfun.infra.bili.BiliException

internal fun apiMessage(code: Int, message: String): String =
    com.aritxonly.bdfun.infra.bili.apiMessage(code, message)
