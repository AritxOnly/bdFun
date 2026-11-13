package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

import com.aritxonly.bdfun.feature.video.application.*

@Composable
internal fun DanmakuInputField(runtime: DanmakuInputRuntime) {
    val colors = MiuixTheme.colorScheme
    val text = runtime.form.text
    val sending = runtime.form.sending
    val focusRequester = runtime.focusRequester
    val send = runtime.send
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 46.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { runtime.controller.edit(it) },
                singleLine = true,
                textStyle = ComposerTextStyle.copy(color = colors.onBackground),
                cursorBrush = SolidColor(BrandPink),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                text = "发一条友善的弹幕",
                                color = colors.outline,
                                fontSize = 13.sp,
                            )
                        }
                        inner()
                    }
                },
            )
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComposerIconButton(
                kind = ComposerIcon.Send,
                tint = if (text.isNotBlank() && !sending) BrandPink else colors.outline,
                onClick = { send() },
            )
        }
    }
}
