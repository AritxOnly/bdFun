package com.aritxonly.bdfun.data

import android.content.Context
import com.aritxonly.bdfun.feature.settings.application.SettingsStateHolder
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.infra.storage.AndroidSettingsPreferences
import com.aritxonly.bdfun.infra.storage.JsonSearchHistoryCodec
import kotlinx.coroutines.*

typealias ScrollTopPages = com.aritxonly.bdfun.core.model.settings.ScrollTopPages

/** 旧构造入口仅负责 Android 存储装配，界面使用 SettingsBindings。 */
class SettingsStore private constructor(context: Context, scope: CoroutineScope) : SettingsBindings(
    scope, SettingsStateHolder.create(AndroidSettingsPreferences(context), JsonSearchHistoryCodec(), scope),
) {
    constructor(context: Context) : this(context, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
}
