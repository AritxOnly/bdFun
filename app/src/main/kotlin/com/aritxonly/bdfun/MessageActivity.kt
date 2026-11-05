package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.MessageTopActions
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class MessageActivity : ComponentActivity() {
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
                title = "消息",
                topBarActions = {
                    MessageTopActions()
                },
            ) {
                MessageScreenRoute(api, this)
            }
        }
    }
}
