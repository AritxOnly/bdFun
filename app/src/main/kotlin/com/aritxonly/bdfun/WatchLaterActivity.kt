package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.ui.LibraryBatchOverlay
import com.aritxonly.bdfun.ui.LibraryScreen
import com.aritxonly.bdfun.ui.LibraryTopActions
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class WatchLaterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val api = AppGraph.api
        val settings = AppGraph.settings
        setContent {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                title = "稍后再看",
                topBarActions = { LibraryTopActions() },
                overlay = { LibraryBatchOverlay() },
            ) {
                WatchLaterActivityRoute(api, this)
            }
        }
    }
}

@Composable
internal fun WatchLaterActivityRoute(api: BiliApi, context: android.content.Context) {
    LibraryScreen(
        api = api,
        loggedIn = AppGraph.session.value != null,
        onNeedLogin = { TeakNavigator.openLogin(context) },
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenVideo = { TeakNavigator.openVideo(context, it) },
        fixedMode = 2,
        title = "稍后再看",
    )
}
