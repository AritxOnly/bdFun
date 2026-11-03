package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.SettingsScreen
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val settings = AppGraph.settings
        setContent {
            TeakActivityScaffold(settings = settings, onBack = { finish() }, title = "设置") {
                SettingsScreen(
                    settings = settings,
                    onBack = { finish() },
                    loggedIn = AppGraph.api.isLoggedIn,
                    onLogout = {
                        AppGraph.api.logout()
                        AppGraph.session.value = null
                        finish()
                    },
                )
            }
        }
    }
}
