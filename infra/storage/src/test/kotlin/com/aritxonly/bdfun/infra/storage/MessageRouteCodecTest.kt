package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.model.message.MessageNavigation
import com.aritxonly.bdfun.core.navigation.Route
import org.junit.Assert.*
import org.junit.Test

class MessageRouteCodecTest {
    @Test fun likesRetainOriginalContentTargetAcrossProcessRecreation() {
        val codec = RouteJsonCodec()
        val route = Route.MessageTools("likes", "42", count = 9,
            navigation = MessageNavigation("BV1orW46aEba", 8, 10, 11, 12, 1, 9000, "bilibili://video/8"))
        assertEquals(route, codec.decode(codec.encode(route)))
    }
    @Test fun secondaryInboxAndSettingsRoundTripWithoutGlobalState() {
        val codec = RouteJsonCodec()
        listOf(Route.MessageInbox(4, "消息垃圾箱"), Route.MessageTools("settings", type = 19),
            Route.MessageTools("chat", "123")).forEach { assertEquals(it, codec.decode(codec.encode(it))) }
    }
}
