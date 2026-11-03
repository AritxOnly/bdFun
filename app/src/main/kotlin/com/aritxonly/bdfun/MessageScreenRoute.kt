package com.aritxonly.bdfun

import android.content.Context
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.ui.MessageScreen

@Composable
internal fun MessageScreenRoute(api: BiliApi, context: Context) {
    MessageScreen(
        api = api,
        loggedIn = AppGraph.session.value != null,
        onNeedLogin = { TeakNavigator.openLogin(context) },
        onBack = { (context as? android.app.Activity)?.finish() },
        onOpenVideo = { TeakNavigator.openVideo(context, it) },
        onOpenReplies = { TeakNavigator.openMessageFeed(context, "reply") },
        onOpenLikes = { TeakNavigator.openMessageFeed(context, "like") },
        onOpenFans = { TeakNavigator.openMessageFeed(context, "fans") },
    )
}
