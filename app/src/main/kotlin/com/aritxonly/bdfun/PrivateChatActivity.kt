package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aritxonly.bdfun.data.MessageSession

class PrivateChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val route = com.aritxonly.bdfun.navigation.RouteIntents.read(intent) as? com.aritxonly.bdfun.core.navigation.Route.PrivateChat
        val talkerId = route?.talkerId ?: intent?.getLongExtra("talker_id", 0L) ?: 0L
        val sessionType = route?.sessionType ?: intent?.getIntExtra("session_type", 1) ?: 1
        val name = (route?.name ?: intent?.getStringExtra("name")).orEmpty().ifBlank { "私信" }
        val avatar = (route?.avatar ?: intent?.getStringExtra("avatar")).orEmpty()
        com.aritxonly.bdfun.data.LogCollector.record(
            "CHAT",
            "open talker=$talkerId type=$sessionType name=$name",
        )
        val session = MessageSession(
            id = "1_$talkerId",
            talkerId = talkerId,
            sessionType = sessionType,
            name = name,
            avatar = avatar,
            preview = "",
            time = 0L,
            unread = route?.unread ?: intent?.getIntExtra("unread", 0) ?: 0,
            isSystem = false,
            maxSeqno = route?.maxSeqno ?: intent?.getLongExtra("max_seqno", 0L) ?: 0L,
        )
        setContent {
            PrivateChatRoot(session = session)
        }
    }
}
