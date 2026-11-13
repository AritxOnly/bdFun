package com.aritxonly.bdfun.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.RowScope.BottomSearchInput(
    settings: SettingsBindings, request: BottomSearchRequest, actions: BottomSearchActions,
    local: BottomSearchPresentationState, focusRequester: FocusRequester,
) {
    val colors = MiuixTheme.colorScheme
    val submitted = request.submitted
    val query = request.query
    val onQueryChange = actions.onQueryChange
    val onSubmit = actions.onSubmit
    val onCancel = actions.onCancel
    val onBackToLanding = actions.onBackToLanding
    val keyboard = LocalSoftwareKeyboardController.current
    var history by local::history
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = ComposerTextStyle.copy(color = colors.onBackground),
        cursorBrush = SolidColor(BrandPink),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                val word = query.trim()
                if (word.isNotEmpty()) {
                    settings.addSearchHistory(word)
                    history = settings.searchHistory
                    onSubmit(word)
                    keyboard?.hide()
                }
            },
        ),
        modifier = Modifier.weight(1f).focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            if (query.isEmpty()) {
                Text(
                    text = "搜索视频、UP 主",
                    color = colors.onBackgroundVariant,
                    style = ComposerTextStyle,
                    maxLines = 1,
                )
            }
            innerTextField()
        },
    )
}
