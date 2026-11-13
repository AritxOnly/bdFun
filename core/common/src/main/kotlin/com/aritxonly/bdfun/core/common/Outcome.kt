package com.aritxonly.bdfun.core.common

sealed interface AppError {
    data class Network(val message: String) : AppError
    data object LoginExpired : AppError
    data class RiskControl(val message: String) : AppError
    data class Parsing(val message: String) : AppError
    data class Unknown(val message: String) : AppError
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}
