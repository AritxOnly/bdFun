package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.settings.ScrollTopPages

internal class MessageSettingsStateHolder(
    prefs: SettingsPreferences,
    values: SettingsValuesStateHolder,
    floats: FloatSettingsWriter,
    codec: SearchHistoryCodec,
) : SettingsGroupStateHolder(prefs, values, floats, codec), MessageSettings {
    private val showIpLocationState = stateCell("showIpLocationState", prefs.getBoolean("show_ip_location", true))

    private val sentHistoryEntryState =
        stateCell("sentHistoryEntryState", prefs.getBoolean("sent_history_entry", true))

    /** 全局字号倍率（基于当前默认字号，100% 即完全不缩放）。 */

    override var showIpLocation: Boolean
        get() = showIpLocationState.value
        set(value) {
            showIpLocationState.value = value
            prefs.edit().putBoolean("show_ip_location", value).apply()
        }

    override var sentHistoryEntry: Boolean
        get() = sentHistoryEntryState.value
        set(value) {
            sentHistoryEntryState.value = value
            prefs.edit().putBoolean("sent_history_entry", value).apply()
        }

}
