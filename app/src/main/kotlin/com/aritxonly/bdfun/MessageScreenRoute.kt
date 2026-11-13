package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToMessageFeed
import com.aritxonly.bdfun.navigateToLogin
import android.content.Context
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.ui.MessageScreen

@Composable
internal fun MessageScreenRoute(context: Context) {
    MessageScreen(
        loggedIn = AppGraph.session.value != null,
        onNeedLogin = { navigateToLogin(context) },
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenReplies = { navigateToMessageFeed(context, "reply") },
        onOpenLikes = { navigateToMessageFeed(context, "like") },
        onOpenFans = { navigateToMessageFeed(context, "fans") },
    )
}
