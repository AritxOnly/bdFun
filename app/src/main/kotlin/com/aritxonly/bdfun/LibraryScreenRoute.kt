package com.aritxonly.bdfun

import android.content.Context
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.ui.LibraryScreen

@Composable
internal fun LibraryScreenRoute(api: BiliApi, context: Context) {
    LibraryScreen(
        api = api,
        loggedIn = AppGraph.session.value != null,
        onNeedLogin = { TeakNavigator.openLogin(context) },
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenVideo = { TeakNavigator.openVideo(context, it) },
        fixedMode = 0,
        title = "收藏",
    )
}
