package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.ActivityPageOptions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.ui.MessageDetailScreen
import com.aritxonly.bdfun.ui.MessageFeedKind
import com.aritxonly.bdfun.ui.TeakActivityScaffold

/** 「回复与@」「收到喜欢」「新增粉丝」的二级消息页。 */
class MessageDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val kind = when (intent?.getStringExtra("kind")) {
            "like" -> MessageFeedKind.Like
            "fans" -> MessageFeedKind.Fans
            "system" -> MessageFeedKind.System
            else -> MessageFeedKind.Reply
        }
        val settings = AppGraph.settings
        val title = when (kind) {
            MessageFeedKind.Reply -> "回复与@"
            MessageFeedKind.Like -> "收到喜欢"
            MessageFeedKind.Fans -> "新增粉丝"
            MessageFeedKind.System -> "系统通知"
        }
        setContent {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = ActivityPageOptions(
                    title = title,
                ),
            ) {
                MessageDetailScreen(kind = kind, initialTab = if (intent?.getStringExtra("kind") == "at") 2 else 0)
            }
        }
    }
}
