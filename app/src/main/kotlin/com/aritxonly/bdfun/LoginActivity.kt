package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.LoginScreen
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class LoginActivity : ComponentActivity() {
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
                showBackButton = false,
            ) {
                LoginScreen(
                    api = api,
                    onBack = { finish() },
                    onSuccess = { profile ->
                        // 登录态写进进程级单例，MainActivity 的 Compose 状态会立即刷新
                        AppGraph.session.value = profile
                        finish()
                    },
                )
            }
        }
    }
}
