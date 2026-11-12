package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.CommentThreadComposer(
    transient: CommentThreadPresentationStateHolder, imeVisible: Boolean,
    navBottom: androidx.compose.ui.unit.Dp, focusRequester: FocusRequester, onSend: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    // 面板内回复输入框：点某条回复后出现。
    // 它是浮在内容上的一层，不参与 Column 排版，所以底部的渐变不会被一起顶上去。
    val target = transient.replyTarget
    if (target != null) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = if (imeVisible) 8.dp else 8.dp + navBottom,
                )
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TeakGlassSurface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = Capsule(),
                recipe = TeakGlassRecipes.floatingNavigation(
                    LocalTeakMaterialSpec.current.fineTuning,
                ),
                backdrop = LocalTeakBackdrop.current,
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 44.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        CommentThreadTextField(transient, target, focusRequester)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                    ) {
                        ComposerIconButton(
                            kind = ComposerIcon.Send,
                            tint = if (transient.draft.isNotBlank()) {
                                BrandPink
                            } else {
                                colors.outline
                            },
                            onClick = { onSend() },
                        )
                    }
                }
            }
        }
    }
}
