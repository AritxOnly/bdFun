package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings

@Composable
internal fun BottomSearchHeader(settings: SettingsBindings, request: BottomSearchRequest,
    actions: BottomSearchActions, local: BottomSearchPresentationState, focusRequester: FocusRequester) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            .height(HomeTopBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 搜索页内所有控件都不用玻璃：返回键和搜索框直接给实色表面。
        BottomSearchBackButton(request, actions)
        Spacer(Modifier.width(10.dp))
        BottomSearchHeaderField(settings, request, actions, local, focusRequester)
        // 与首页顶栏的消息按钮同尺寸同位置：38(返回) + 10 + 胶囊 + 10 + 40。
        Spacer(Modifier.width(10.dp))
        BottomSearchSubmitButton(settings, request, actions, local)
    }
}
