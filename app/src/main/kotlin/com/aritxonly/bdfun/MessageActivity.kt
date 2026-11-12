package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.ActivityPageSlots

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
        val settings = AppGraph.settings
        setContent {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = ActivityPageOptions(
                    title = "消息",
                ),
                slots = ActivityPageSlots(
                    topBarActions = {
                    MessageTopActions()
                },
                ),
            ) {
                MessageScreenRoute(this)
            }
        }
    }
}
