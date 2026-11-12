package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.feed.*
import org.json.JSONArray
import org.json.JSONObject

/** 只编码本应用的导航快照，不使用 Java 序列化或进程内的全局传参槽。 */
class RouteJsonCodec {
    fun encode(route: Route): String = JSONObject().apply {
        put("version", 1)
        when (route) {
            is Route.Space -> { put("type", "space"); put("mid", route.mid) }
            is Route.PrivateChat -> {
                put("type", "private-chat"); put("talkerId", route.talkerId); put("sessionType", route.sessionType)
                put("name", route.name); put("avatar", route.avatar); put("unread", route.unread); put("maxSeqno", route.maxSeqno)
            }
            is Route.Video -> {
                put("type", "video"); put("bvid", route.bvid); put("aid", route.aid)
                put("snapshot", route.snapshot?.let(::video))
                put("fromVideoPage", route.fromVideoPage); put("commentRoot", route.commentRoot)
                put("commentTarget", route.commentTarget); put("seekMs", route.seekMs)
            }
            is Route.Dynamic -> {
                put("type", "dynamic"); put("id", route.id); put("snapshot", route.snapshot?.let(::dynamic))
            }
        }
    }.toString()

    fun decode(raw: String): Route? = try {
        val value = JSONObject(raw)
        if (value.optInt("version") != 1) null else when (value.optString("type")) {
            "space" -> Route.Space(value.getLong("mid"))
            "private-chat" -> Route.PrivateChat(value.getLong("talkerId"), value.optInt("sessionType", 1),
                value.optString("name"), value.optString("avatar"), value.optInt("unread"), value.optLong("maxSeqno"))
            "video" -> Route.Video(value.optString("bvid"), value.optLong("aid"),
                value.optJSONObject("snapshot")?.let(::readVideo), value.optBoolean("fromVideoPage"),
                value.optLong("commentRoot"), value.optLong("commentTarget"), value.optLong("seekMs"))
            "dynamic" -> Route.Dynamic(value.getString("id"), value.optJSONObject("snapshot")?.let(::readDynamic))
            else -> null
        }
    } catch (_: Exception) { null }

    private fun video(v: VideoItem): JSONObject = JSONObject().apply {
        put("bvid", v.bvid); put("aid", v.aid); put("title", v.title); put("cover", v.cover)
        put("ownerName", v.ownerName); put("ownerFace", v.ownerFace); put("ownerMid", v.ownerMid)
        put("play", v.play); put("danmaku", v.danmaku); put("duration", v.duration); put("pubdate", v.pubdate)
        put("cid", v.cid); put("reason", v.reason); put("isFollowed", v.isFollowed)
        put("startPositionMs", v.startPositionMs); put("categoryName", v.categoryName)
        put("recommendationTrackId", v.recommendationTrackId); put("appRecommendation", v.appRecommendation)
        put("dislikeReasons", JSONArray(v.dislikeReasons.map { JSONObject().put("id", it.id).put("name", it.name) }))
    }

    private fun readVideo(v: JSONObject) = VideoItem(
        v.getString("bvid"), v.getLong("aid"), v.getString("title"), v.getString("cover"),
        v.getString("ownerName"), v.getString("ownerFace"), v.getLong("ownerMid"), v.getLong("play"),
        v.getLong("danmaku"), v.getInt("duration"), v.getLong("pubdate"), v.optLong("cid"),
        v.optString("reason"), v.optBoolean("isFollowed"), v.optLong("startPositionMs"), v.optString("categoryName"),
        v.optString("recommendationTrackId"), v.optBoolean("appRecommendation"),
        v.optJSONArray("dislikeReasons").objects().map { RecommendationReason(it.getLong("id"), it.getString("name")) },
    )

    private fun dynamic(d: DynamicItem): JSONObject = JSONObject().apply {
        put("id", d.id); put("name", d.name); put("face", d.face); put("mid", d.mid); put("time", d.time)
        put("text", d.text); put("images", JSONArray(d.images)); put("video", d.video?.let(::video))
        put("like", d.like); put("comment", d.comment); put("forward", d.forward); put("liked", d.liked)
        put("commentOid", d.commentOid); put("commentType", d.commentType); put("title", d.title)
        put("orig", d.orig?.let(::dynamic)); put("authorVip", d.authorVip); put("isCollection", d.isCollection)
        put("pgcCover", d.pgcCover); put("pgcTitle", d.pgcTitle); put("pgcLabel", d.pgcLabel)
        put("moduleTag", d.moduleTag); put("ipLocation", d.ipLocation)
        put("liveRecommendation", d.liveRecommendation?.let {
            JSONObject().put("roomId", it.roomId).put("title", it.title).put("cover", it.cover)
                .put("live", it.live).put("areaName", it.areaName).put("watched", it.watched)
        })
    }

    private fun readDynamic(d: JSONObject): DynamicItem = DynamicItem(
        d.getString("id"), d.getString("name"), d.getString("face"), d.getLong("mid"), d.getString("time"),
        d.getString("text"), d.getJSONArray("images").let { a -> List(a.length()) { a.getString(it) } },
        d.optJSONObject("video")?.let(::readVideo), d.getInt("like"), d.getInt("comment"), d.getInt("forward"),
        d.optBoolean("liked"), d.getString("commentOid"), d.getInt("commentType"), d.optString("title"),
        d.optJSONObject("orig")?.let(::readDynamic), d.optBoolean("authorVip"), d.optBoolean("isCollection"),
        d.optString("pgcCover"), d.optString("pgcTitle"), d.optString("pgcLabel"), d.optString("moduleTag"),
        d.optJSONObject("liveRecommendation")?.let {
            DynamicLiveRecommendation(it.getLong("roomId"), it.getString("title"), it.getString("cover"),
                it.getBoolean("live"), it.getString("areaName"), it.getString("watched"))
        }, d.optString("ipLocation"),
    )

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }
}
