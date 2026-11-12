package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.bdfun.feature.comment.application.CommentActionsStateHolder
import kotlinx.coroutines.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 菜单延时清理与页面同生命周期；复制/分享的系统交互留在表现层。 */
internal class CommentMenuStateHolder(
    private val context: Context, private val scope: CoroutineScope,
    private val actions: CommentActionsStateHolder, private val bvid: String,
    private val onReply: (CommentItem) -> Unit, private val onDelete: (Long) -> Unit,
) {
    var target by mutableStateOf<CommentItem?>(null)
        private set
    var visible by mutableStateOf(false)
        private set
    var anchor by mutableStateOf(Rect.Zero)
        private set
    fun open(item: CommentItem, bounds: Rect) {
        if (!actions.isActive) return
        anchor = bounds
        target = item
        visible = true
    }
    fun hide() { visible = false }
    fun dismiss() {
        if (!actions.isActive) return
        hide()
        scope.launch { delay(360); if (!visible) target = null }
    }
    fun reply(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        scope.launch { delay(360); target = null; if (actions.isActive) onReply(item) }
    }
    fun copy(item: CommentItem, withName: Boolean) {
        if (!actions.isActive) return
        hide()
        context.copyToClipboard("评论", if (withName) "${item.uname}：${item.message}" else item.message)
        context.showToast("已复制")
        scope.launch { delay(360); target = null }
    }
    fun share(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        runCatching {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "${item.uname}：${item.message}\nhttps://www.bilibili.com/video/$bvid")
            }
            context.startActivity(android.content.Intent.createChooser(intent, "分享评论"))
        }
        scope.launch { delay(360); target = null }
    }
    fun report(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        scope.launch {
            val ok = actions.report(item.rpid)
            if (!actions.isActive) return@launch
            context.showToast(if (ok) "已举报" else "举报失败")
            delay(360); target = null
        }
    }
    fun block(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        scope.launch {
            val ok = actions.block(item.mid)
            if (!actions.isActive) return@launch
            context.showToast(if (ok) "已加入黑名单" else "操作失败")
            delay(360); target = null
        }
    }
    fun delete(item: CommentItem) {
        if (!actions.isActive) return
        hide()
        scope.launch {
            val ok = actions.delete(item.rpid)
            if (!actions.isActive) return@launch
            if (ok) { onDelete(item.rpid); context.showToast("已删除") } else context.showToast("删除失败")
            delay(360); target = null
        }
    }
}

@Composable
internal fun CommentMenu(menu: CommentMenuStateHolder, myMid: Long) {
    val colors = MiuixTheme.colorScheme
    val target = menu.target
    TeakGlassMenu(
        visible = menu.visible && target != null, onDismiss = menu::dismiss,
        anchorInWindow = menu.anchor, minWidth = 168.dp, maxWidth = 216.dp, maxHeight = 420.dp,
    ) {
        if (target != null) {
            CommentMenuRow("回复", MenuGlyphKind.Reply, colors.onBackground) { menu.reply(target) }
            CommentMenuRow("复制", MenuGlyphKind.Copy, colors.onBackground) { menu.copy(target, false) }
            CommentMenuRow("自由复制", MenuGlyphKind.Copy, colors.onBackground) { menu.copy(target, true) }
            CommentMenuRow("分享", null, colors.onBackground) { menu.share(target) }
            CommentMenuRow("举报", MenuGlyphKind.Report, Color(0xFFE5484D)) { menu.report(target) }
            CommentMenuRow("加入黑名单", MenuGlyphKind.Block, Color(0xFFE5484D)) { menu.block(target) }
            if (myMid > 0 && target.mid == myMid) {
                CommentMenuRow("删除", MenuGlyphKind.Delete, Color(0xFFE5484D), danger = true) { menu.delete(target) }
            }
        }
    }
}
