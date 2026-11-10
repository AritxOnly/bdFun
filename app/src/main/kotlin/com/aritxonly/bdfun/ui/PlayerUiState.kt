package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 播放器全屏状态（跨 VideoScreen / VideoActivity 共享）。 */
object PlayerUiState {
    var fullscreen by mutableStateOf(false)

    /**
     * 播放器控件当前是否可见。
     * 半屏（视频详情竖屏）时页面返回键跟着它一起显隐，不再一直挂在播放器左上角。
     */
    var controlsVisible by mutableStateOf(false)

    /** 播放器右上角「⋯」弹窗（画在 Activity 覆盖层，点外面才能可靠收起）。 */
    var danmakuPanelOpen by mutableStateOf(false)

    /** 弹幕设置面板里的任何操作都自增，用来重置播放器控件的自动渐隐计时。 */
    var danmakuInteract by mutableIntStateOf(0)
}

/** 视频页全屏锁：锁定期间忽略系统方向变化。 */
object VideoOrientationLock {
    @Volatile
    var locked: Boolean = false
}

/** 回复目标：点某条评论的「回复」后写进来，底部回复栏读它显示占位符并决定 root/parent。 */
object ReplyTarget {
    var name by mutableStateOf<String?>(null)
    var root by mutableLongStateOf(0L)
    var parent by mutableLongStateOf(0L)

    fun set(name: String, root: Long, parent: Long) {
        this.name = name
        this.root = root
        this.parent = parent
    }

    fun clear() {
        name = null
        root = 0L
        parent = 0L
    }
}

/**
 * 视频页跨层通信：
 * - Activity 的 overlay 里画着「弹幕开关 / 发弹幕 / 回复栏」，它们在采集层之外（玻璃铁律），
 *   拿不到 VideoScreen / CommentScreen 内部的 api 与状态，所以通过这里注册发送器。
 * - 发送器只在该页面组合期间存在，onDispose 时清空，避免持有已销毁页面的引用。
 */
object VideoPageBus {
    var aid by mutableLongStateOf(0L)
    var cid by mutableLongStateOf(0L)

    /** 播完且没有下一分P时，请求 Activity 打开播放队列里的下一个视频。 */
    var autoNextRequest by mutableIntStateOf(0)
        private set

    fun requestAutoNext() { autoNextRequest++ }

    /** 发的弹幕是否启用（由「弹」按钮切换，同步 DanmakuSettings）。 */
    var danmakuOn by mutableStateOf(true)

    /** 打开弹幕设置时抓的一帧画面，用作面板的磨砂底。 */
    var panelBackdrop by mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)

    /** 请求关闭弹幕输入条（让它先渐隐再移除）。 */
    var danmakuInputClose by mutableIntStateOf(0)

    /** 播放器当前进度（发弹幕时用作 progress）。 */
    var danmakuProgressMs by mutableLongStateOf(0L)

    var commentSender: (suspend (String, String?) -> Boolean)? = null
    var danmakuSender: (suspend (String, Int, Int, Int) -> Boolean)? = null

    /**
     * 当前进程里所有还活着的播放器的暂停入口。
     * 相关推荐会在旧 Activity 销毁前再开一个新播放器，单例槽位会被新实例覆盖，
     * 旧实例就再也停不下来，所以这里改成注册表，暂停时把所有实例都停掉。
     */
    private val pauseHooks = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    fun registerPause(hook: () -> Unit) {
        if (!pauseHooks.contains(hook)) pauseHooks.add(hook)
    }

    fun unregisterPause(hook: () -> Unit) {
        pauseHooks.remove(hook)
    }

    fun pauseAll() {
        pauseHooks.toList().forEach { hook -> runCatching { hook() } }
    }

    /** 回复栏是否处于可编辑状态（键盘/表情面板展开）——Activity 据此决定要不要吃掉空白点击。 */
    var replyEditing by mutableStateOf(false)

    /** 点某条评论的「回复」后自增，回复栏据此请求焦点。 */
    var requestReplyFocus by mutableIntStateOf(0)

    /** 已经处理过的 focus 请求号：放在进程级，回复栏重建时不会重放旧请求。 */
    var consumedReplyFocus by mutableIntStateOf(0)

    /** 点击空白处要求收起回复栏（清焦点、收键盘、收表情面板）。 */
    var dismissReply by mutableIntStateOf(0)

    var consumedDismissReply by mutableIntStateOf(0)

    /** 评论长按菜单是否打开：打开时底部回复栏先渐隐，关闭后再渐显。 */
    var commentMenuOpen by mutableStateOf(false)
}
