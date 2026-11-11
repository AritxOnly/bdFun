package com.aritxonly.bdfun.core.navigation

sealed interface Route {
    data class Video(val bvid: String) : Route
    data class Space(val mid: Long) : Route
    data class Dynamic(val id: String) : Route
}
