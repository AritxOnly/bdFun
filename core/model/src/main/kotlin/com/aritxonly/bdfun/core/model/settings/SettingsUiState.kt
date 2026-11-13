package com.aritxonly.bdfun.core.model.settings

sealed interface SettingValue {
    data class Flag(val value: Boolean) : SettingValue
    data class Number(val value: Int) : SettingValue
    data class Decimal(val value: Float) : SettingValue
    data class Text(val value: String) : SettingValue
    data class TextSet(val value: Set<String>) : SettingValue
}

data class SettingsUiState(val values: Map<String, SettingValue> = emptyMap(), val revision: Long = 0)
