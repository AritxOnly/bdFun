package com.aritxonly.bdfun.navigation

import android.content.Context
import android.content.Intent
import com.aritxonly.bdfun.VideoActivity
import com.aritxonly.bdfun.SpaceActivity
import com.aritxonly.bdfun.DynamicDetailActivity
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.infra.storage.RouteJsonCodec

/** Activity 自己持有 Intent 快照，叠开页面与进程重建不共享传参槽。 */
private const val EXTRA_ROUTE = "com.aritxonly.bdfun.ROUTE"

internal fun createRouteIntent(context: Context, route: Route): Intent {
    val codec = RouteJsonCodec()
    val target = when (route) {
        is Route.Video -> VideoActivity::class.java
        is Route.Space -> SpaceActivity::class.java
        is Route.Dynamic -> DynamicDetailActivity::class.java
        is Route.PrivateChat -> com.aritxonly.bdfun.PrivateChatActivity::class.java
    }
    return Intent(context, target).putExtra(EXTRA_ROUTE, codec.encode(route)).apply {
        if (route is Route.Video) {
            putExtra("fromVideoPage", route.fromVideoPage)
            if (route.bvid.isNotBlank()) putExtra("bvid", route.bvid)
            putExtra("aid", route.aid)
        }
        if (route is Route.PrivateChat) {
            putExtra("talker_id", route.talkerId); putExtra("session_type", route.sessionType)
            putExtra("name", route.name); putExtra("avatar", route.avatar)
            putExtra("unread", route.unread); putExtra("max_seqno", route.maxSeqno)
        }
    }
}

internal fun readRouteIntent(intent: Intent): Route? = intent.getStringExtra(EXTRA_ROUTE)?.let(RouteJsonCodec()::decode)
