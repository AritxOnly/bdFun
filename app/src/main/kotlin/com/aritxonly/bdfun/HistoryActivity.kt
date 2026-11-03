package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.ui.LibraryScreen
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val api = AppGraph.api
        val settings = AppGraph.settings
        setContent {
            TeakActivityScaffold(settings = settings, onBack = { finish() }, title = "历史") {
                HistoryActivityRoute(api, this)
            }
        }
    }
}

@Composable
internal fun HistoryActivityRoute(api: BiliApi, context: android.content.Context) {
    LibraryScreen(
        api = api,
        loggedIn = AppGraph.session.value != null,
        onNeedLogin = { TeakNavigator.openLogin(context) },
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenVideo = { TeakNavigator.openVideo(context, it) },
        fixedMode = 1,
        title = "历史",
    )
}
