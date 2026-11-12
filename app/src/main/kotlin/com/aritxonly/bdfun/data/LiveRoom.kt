package com.aritxonly.bdfun.data

typealias LiveRoomInfo = com.aritxonly.bdfun.core.model.live.LiveRoomInfo
typealias LiveChatMessage = com.aritxonly.bdfun.core.model.live.LiveChatMessage
typealias LiveChatKind = com.aritxonly.bdfun.core.model.live.LiveChatKind

/**
 * 第一阶段的假数据：让界面先跑起来，不依赖网络。
 * 接真实接口后整个对象可以删掉。
 */
object LiveMock {

    val room = LiveRoomInfo(
        roomId = 7734200L,
        title = "随便聊聊，顺便看看新版本做得怎么样了",
        uname = "阿茶Teak",
        uid = 946974L,
        onlineText = "1.2 万",
        areaText = "虚拟日常",
        following = false,
    )

    private val users: List<Pair<String, Long?>> = listOf(
        "晚风不燥" to 0x7BD0FFL,
        "一只咸鱼" to null,
        "橘子汽水" to 0xFFB86BL,
        "路过的猫" to null,
        "今天也要早睡" to 0xA8E6CFL,
        "小破站老用户" to null,
        "薯条要蘸酱" to 0xFFC2D1L,
        "摸鱼中" to null,
        "夜航星" to 0xB9A7FFL,
        "不想上班" to null,
        "海边的风" to 0x9BE7FFL,
        "键盘侠本侠" to null,
    )

    private val lines = listOf(
        "刚进来，主播今天状态不错啊",
        "这个界面是新版本的吗？看着好清爽",
        "前排放个板凳",
        "能不能演示一下横屏效果",
        "画质好清楚，用的什么线路",
        "弹幕好像比之前快了",
        "主播讲讲这次更新改了啥",
        "我来晚了，刚才错过了什么",
        "这个粉色好好看",
        "求个关注，谢谢主播",
        "别刷屏啊兄弟们",
        "笑死，这也能行",
        "声音有点小，能调大一点吗",
        "刚下载，第一次用",
        "这波操作可以",
    )

    /** 开场就有的消息，模拟「进来时已经在聊」的状态。 */
    val initialMessages: List<LiveChatMessage> = listOf(
        LiveChatMessage(1L, LiveChatKind.SYSTEM, text = "欢迎 晚风不燥 进入直播间"),
        LiveChatMessage(
            2L,
            LiveChatKind.DANMAKU,
            uname = "晚风不燥",
            text = "刚刚开始吗？",
            nameColor = 0x7BD0FFL,
        ),
        LiveChatMessage(
            3L,
            LiveChatKind.GIFT,
            uname = "橘子汽水",
            text = "赠送 辣条 x10",
        ),
        LiveChatMessage(
            4L,
            LiveChatKind.DANMAKU,
            uname = "路过的猫",
            text = "蹲一个横屏演示",
        ),
        LiveChatMessage(
            5L,
            LiveChatKind.SUPER_CHAT,
            uname = "薯条要蘸酱",
            text = "支持一下，希望能把直播弹幕做出来",
            amountText = "￥30",
        ),
    )

    private var seed = 100L

    /** 生成一条新的假消息（按权重随机）。 */
    fun nextMessage(): LiveChatMessage {
        seed += 1
        val roll = (seed * 37 % 100).toInt()
        val (user, color) = users[(seed % users.size).toInt()]
        val line = lines[((seed * 13) % lines.size).toInt()]
        return when {
            roll < 6 -> LiveChatMessage(
                id = seed,
                kind = LiveChatKind.SYSTEM,
                text = "欢迎 $user 进入直播间",
            )
            roll < 11 -> LiveChatMessage(
                id = seed,
                kind = LiveChatKind.GIFT,
                uname = user,
                text = "赠送 " + listOf("辣条 x5", "小心心 x1", "牛哇 x1").random(),
            )
            else -> LiveChatMessage(
                id = seed,
                kind = LiveChatKind.DANMAKU,
                uname = user,
                text = line,
                nameColor = color,
            )
        }
    }
}
