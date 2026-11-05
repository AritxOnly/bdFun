package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.LibraryBatchOverlay
import com.aritxonly.bdfun.ui.LibraryTopActions
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class LibraryActivity : ComponentActivity() {
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
                title = "收藏",
                topBarActions = { LibraryTopActions() },
                overlay = { LibraryBatchOverlay() },
            ) {
                LibraryScreenRoute(api, this)
            }
        }
    }
}
