package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings

import com.aritxonly.bdfun.feature.search.application.BottomSearchStateHolder

@Composable
internal fun BottomSearchEffects(settings: SettingsBindings, controller: BottomSearchStateHolder,
    request: BottomSearchRequest, local: BottomSearchPresentationState, focusRequester: FocusRequester) {
    val query = request.query
    val submitted = request.submitted
    var tab by local::tab
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { controller.loadHotKeywords(settings.showHotSearch) }
    LaunchedEffect(query, submitted) { controller.suggest(query, submitted) }
    LaunchedEffect(submitted) {
        if (submitted == null) tab = 0
        controller.search(submitted)
    }

    // 进页面自动聚焦弹键盘；进结果页则收起，和原实现一致。
    LaunchedEffect(submitted) {
        if (submitted == null) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        } else {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

}
