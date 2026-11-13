package com.aritxonly.bdfun.ui

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakTopGlassButton
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor

/** 右上角操作放在 Activity 的 overlay 里：真玻璃必须在 backdrop 采集层之外，否则会 SIGSEGV。 */
@Composable
fun MessageTopActions() {
    val MessagePageBus = LocalPageChromeState.current

    TeakTopGlassButton(onClick = { MessagePageBus.requestReadAll() }) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = MiuixIcons.Delete, contentDescription = "全部已读",
            tint = teakGlassGlyphColor(),
            modifier = Modifier.size(21.dp),
        )
    }
    TeakTopGlassButton(onClick = { MessagePageBus.requestMore() }) {
        ThreeDotsGlyph()
    }
}
