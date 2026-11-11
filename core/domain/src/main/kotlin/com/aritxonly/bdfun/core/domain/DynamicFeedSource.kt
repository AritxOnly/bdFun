package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.feed.DynamicPage
import com.aritxonly.bdfun.core.model.feed.DynamicPortal
import com.aritxonly.bdfun.core.model.feed.DynamicUpPage

/** Service contract verified against Hyper-PiliPlus 0d08dd25 (2026-11-11).
 * Keep server ordering and cursors; the UP feed is not the public space feed.
 */
object DynamicFeedContract {
    const val feedPath = "/x/polymer/web-dynamic/v1/feed/all"
    const val portalPath = "/x/polymer/web-dynamic/v1/portal"
    const val upListPath = "/x/polymer/web-dynamic/v1/uplist"
    const val features = "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,decorationCard,onlyfansAssetsV2,forwardListHidden,ugcDelete,onlyfansQaCard,commentsNewVersion,avatarAutoTheme,sunflowerStyle,cardsEnhance,eva3CardOpus,eva3CardVideo,eva3CardComment,eva3CardVote,eva3CardUser"
    val types = listOf("all", "video", "pgc", "article", "up")
    val labels = listOf("全部", "投稿", "番剧", "专栏", "UP")
    val portalParams = mapOf("up_list_more" to "1", "web_location" to "333.1365")

    fun feedParams(type: String, offset: String, hostMid: Long?): Map<String, String> {
        require(type in types)
        return buildMap {
            if (type == "up") {
                require(hostMid != null && hostMid > 0)
                put("host_mid", hostMid.toString())
            } else put("type", type)
            if (offset.isNotEmpty()) put("offset", offset)
            put("features", features)
        }
    }

    fun upListParams(offset: String) = mapOf(
        "offset" to offset, "platform" to "web", "web_location" to "333.1365",
    )
}

interface DynamicFeedSource {
    suspend fun dynamicPage(type: String, offset: String = "", hostMid: Long? = null): DynamicPage
    suspend fun dynamicPortal(): DynamicPortal
    suspend fun dynamicUpList(offset: String): DynamicUpPage
}

