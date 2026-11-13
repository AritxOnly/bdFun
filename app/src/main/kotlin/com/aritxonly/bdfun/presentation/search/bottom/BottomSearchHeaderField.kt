package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.RowScope.BottomSearchHeaderField(
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
    Box(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(Capsule())
            .background(colors.surfaceContainerHigh),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Search,
                contentDescription = "搜索",
                tint = colors.onBackgroundVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            BottomSearchInput(settings, request, actions, local, focusRequester)
            if (query.isNotEmpty()) {
                Text(
                    text = "清除",
                    color = colors.onBackgroundVariant,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(Capsule())
                        .clickable { onQueryChange("") }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}
