package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aritxonly.bdfun.feature.library.application.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LibrarySearchPage(content: LibrarySearchContent, actions: LibrarySearchActions) {
    val controller = remember { LibrarySearchStateHolder() }
    val state by controller.state.collectAsState()
    val events = LibrarySearchEvents(controller, actions)
    val colors = MiuixTheme.colorScheme
    Dialog(onDismissRequest = actions.onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect { dialogWindow?.setDimAmount(0f) }
        Column(modifier = Modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
            LibrarySearchHeader(content.title, state, events, actions.onBack)
            if (!state.committed) LibrarySearchHistory(content.history, events) else LibrarySearchResults(content, actions)
        }
    }
}
