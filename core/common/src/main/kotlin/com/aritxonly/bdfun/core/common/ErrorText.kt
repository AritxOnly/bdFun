package com.aritxonly.bdfun.core.common

fun AppError.messageOr(fallback: String): String = when (this) {
    AppError.LoginExpired -> "请先登录"
    is AppError.Network -> message.ifBlank { fallback }
    is AppError.RiskControl -> message.ifBlank { fallback }
    is AppError.Parsing -> message.ifBlank { fallback }
    is AppError.Unknown -> message.ifBlank { fallback }
}
