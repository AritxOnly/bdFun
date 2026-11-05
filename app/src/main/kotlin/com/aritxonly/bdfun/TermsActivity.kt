package com.aritxonly.bdfun

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.ui.TermsScreen
import com.aritxonly.bdfun.ui.TeakActivityScaffold

/** 每次冷启动先展示的「使用须知」页。 */
class TermsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aritxonly.bdfun.data.LogCollector.init(applicationContext)
        val settings = SettingsStore(applicationContext)
        // 同一版本只提示一次；升级到新版本后再提示一次，而不是每次冷启动都弹。
        if (settings.termsAckedVersion == BuildConfig.VERSION_NAME) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                },
            )
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finishAffinity() },
                showBackButton = false,
                title = null,
            ) {
                TermsScreen(
                    onAccept = {
                        settings.termsAckedVersion = BuildConfig.VERSION_NAME
                        startActivity(
                            Intent(this, MainActivity::class.java).apply {
                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK,
                                )
                            },
                        )
                        finish()
                    },
                    onExit = { finishAffinity() },
                )
            }
        }
    }
}
