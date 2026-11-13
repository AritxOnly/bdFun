package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BottomSearchBackButton(request: BottomSearchRequest, actions: BottomSearchActions) {
    val colors = MiuixTheme.colorScheme
    val submitted = request.submitted
    val query = request.query
    val onQueryChange = actions.onQueryChange
    val onSubmit = actions.onSubmit
    val onCancel = actions.onCancel
    val onBackToLanding = actions.onBackToLanding
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .clickable {
            if (submitted != null) onBackToLanding() else onCancel()
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.ChevronBackward,
            contentDescription = "返回",
            tint = colors.onBackground,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun BottomSearchSubmitButton(settings: SettingsBindings, request: BottomSearchRequest,
    actions: BottomSearchActions, local: BottomSearchPresentationState) {
    val colors = MiuixTheme.colorScheme
    val submitted = request.submitted
    val query = request.query
    val onQueryChange = actions.onQueryChange
    val onSubmit = actions.onSubmit
    val onCancel = actions.onCancel
    val onBackToLanding = actions.onBackToLanding
    val keyboard = LocalSoftwareKeyboardController.current
    var history by local::history
    val submitWord = query.trim()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable {
            if (submitWord.isEmpty()) {
                onCancel()
            } else {
                settings.addSearchHistory(submitWord)
                history = settings.searchHistory
                onSubmit(submitWord)
                keyboard?.hide()
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (submitWord.isEmpty()) "取消" else "搜索",
            color = if (submitWord.isEmpty()) colors.onBackgroundVariant else BrandPink,
            fontSize = 13.sp,
            fontWeight = if (submitWord.isEmpty()) FontWeight.Normal else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
