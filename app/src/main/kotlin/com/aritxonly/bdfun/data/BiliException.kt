package com.aritxonly.bdfun.data

class BiliException(
    val code: Int,
    override val message: String,
) : Exception(message)

internal fun apiMessage(code: Int, message: String): String = when (code) {
    -101 -> "请先登录"
    -352, -412 -> "请求被 B 站风控拦截，请稍后再试（$code）"
    -400 -> "请求参数有误"
    -404 -> "内容不存在或已失效"
    -403 -> "没有访问权限"
    62002 -> "稿件不可见"
    62004 -> "稿件审核中"
    else -> message.ifBlank { "请求失败（$code）" }
}
