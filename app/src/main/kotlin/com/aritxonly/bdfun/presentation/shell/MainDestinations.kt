package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Album

internal data class Destination(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
internal fun rememberMainDestinations(): List<Destination> {
    return remember {
        listOf(
            Destination("home", "首页", MiuixIcons.Home),
            Destination("dynamics", "动态", MiuixIcons.Album),
            Destination("mine", "我的", MiuixIcons.ContactsCircle),
        )
    }
}
