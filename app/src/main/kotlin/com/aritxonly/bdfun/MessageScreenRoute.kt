package com.aritxonly.bdfun

import android.content.Context
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.ui.MessageScreen

@Composable
internal fun MessageScreenRoute(context: Context) {
    MessageScreen(
        loggedIn = AppGraph.session.value != null,
        onNeedLogin = { TeakNavigator.openLogin(context) },
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenReplies = { TeakNavigator.openMessageFeed(context, "reply") },
        onOpenLikes = { TeakNavigator.openMessageFeed(context, "like") },
        onOpenFans = { TeakNavigator.openMessageFeed(context, "fans") },
    )
}
