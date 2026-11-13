package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.user.QrState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** 迁移前字段形状回归；不是登录、直播在线状态或账号写操作验收。 */
class RemainingResponseMapperTest {
    private fun live(playurl: JSONObject) = JSONObject().put("data", JSONObject()
        .put("playurl_info", JSONObject().put("playurl", playurl)))
    private fun stream(format: String, codec: String, host: String, path: String) = JSONObject()
        .put("format_name", format).put("codec", JSONArray().put(JSONObject()
            .put("codec_name", codec).put("base_url", path)
            .put("url_info", JSONArray().put(JSONObject().put("host", host).put("extra", "?x=1")))))

    @Test fun accountProfileRequiresIsLoginAndKeepsLargeIdVipAndCoinFields() {
        assertNull(mapAccountProfile(JSONObject()))
        val profile = mapAccountProfile(JSONObject("""{"isLogin":true,"mid":3690989657000489,
            "uname":"账号","money":3.5,"vipStatus":1,"vip_label":{"text":"会员"},
            "level_info":{"current_level":6}}"""))!!
        assertEquals(3690989657000489L, profile.mid)
        assertEquals("账号", profile.name)
        assertEquals(3.5, profile.coins, 0.0)
        assertEquals(6, profile.level)
        assertTrue(profile.vip)
        assertEquals("会员", profile.vipLabel)
    }

    @Test fun qrGenerationAndNonSuccessStatesKeepOriginalMessages() {
        val qr = mapQrLogin(JSONObject("""{"url":"qr-url","auth_code":"fixture-key"}"""))
        assertEquals("qr-url", qr.url)
        assertEquals("fixture-key", qr.key)
        for ((code, state) in listOf(86039 to QrState.Waiting, 86101 to QrState.Waiting,
            86090 to QrState.Scanned, 86038 to QrState.Expired)) {
            assertEquals(state, mapQrPollFailure(JSONObject().put("code", code)).state)
        }
        assertEquals("登录失败", mapQrPollFailure(JSONObject()).message)
        assertEquals("server-error", mapQrPollFailure(JSONObject().put("message", "server-error")).message)
    }

    @Test fun appCardsKeepLegacyAidRangeFiltersAndParseObjectOrEncodedArgs() {
        val args = JSONObject().put("aid", 7).put("up_name", "UP").put("up_id", 3690989657000489L)
        val card = JSONObject().put("card_goto", "av").put("args", args).put("bvid", "BV1orW46aEba")
            .put("title", "title").put("cover_left_text_1", "1.2万").put("cover_right_text", "01:23")
        val encoded = JSONObject(card.toString()).put("args", args.toString())
        val tooLarge = JSONObject(card.toString()).put("args", JSONObject().put("aid", 2147483648L))
        val ad = JSONObject(card.toString()).put("card_goto", "ad_av")
        val data = JSONObject().put("items", JSONArray().put(card).put(encoded).put(tooLarge).put(ad))
        val mapped = mapAppRecommendations(data) { "decoded:$it" }
        assertEquals(4, mapped.rawCount)
        assertEquals(2, mapped.items.size)
        assertTrue(mapped.items.all { it.title == "decoded:title" && it.appRecommendation })
        assertEquals(12000L, mapped.items.first().play)
        assertEquals(83, mapped.items.first().duration)
        assertEquals(3690989657000489L, mapped.items.first().ownerMid)
    }

    @Test fun appDislikeReasonsKeepV2FirstAndPlayerDurationOverridesCoverText() {
        val card = JSONObject("""{"card_type":"small_cover_v9","param":"7","bvid":"BV1orW46aEba",
            "player_args":{"duration":12,"cid":99},"cover_right_text":"01:23",
            "three_point_v2":[{"type":"dislike","reasons":[{"id":1,"name":"v2"}]}],
            "three_point":{"dislike_reasons":[{"id":1,"name":"old"},{"id":2,"name":"second"}]}}""")
        val video = mapAppRecommendations(JSONObject().put("items", JSONArray().put(card))) { it }.items.single()
        assertEquals(12, video.duration)
        assertEquals(99L, video.cid)
        assertEquals(listOf("v2", "second"), video.dislikeReasons.map { it.name })
    }

    @Test fun liveRoomKeepsBackgroundPriorityUrlNormalizationAndDefaultStatus() {
        val json = JSONObject("""{"code":0,"data":{"room_info":{"title":"直播","uid":3690989657000489,
            "background":"http://img/background","cover":"//img/cover","online":12345},
            "anchor_info":{"base_info":{"uname":"主播","face":"//img/face"}}}}""")
        val room = mapLiveRoomInfo(json, 99)!!
        assertEquals(99L, room.roomId)
        assertEquals(3690989657000489L, room.uid)
        assertEquals("https://img/background", room.cover)
        assertEquals("https://img/face", room.face)
        assertEquals("1.2 万", room.onlineText)
        assertTrue(room.live)
        assertFalse(room.following)
        assertNull(mapLiveRoomInfo(json.put("code", -1), 99))
        assertEquals("https://img/portrait", mapLivePortraitBackground(JSONObject("""{"data":{
            "room_info":{"app_background":"//img/portrait","background":"ignored"}}}""")))
    }

    @Test fun liveQualitiesUseAcceptedLevelsAndPreserveDictionaryFallback() {
        val play = JSONObject("""{"g_qn_desc":[{"qn":20000,"desc":"4K"},{"qn":10000,"desc":"原画"},
            {"qn":10000,"desc":"duplicate"}],"stream":[{"format":[{"codec":[{"accept_qn":[80,400,10000,400,0]}]}]}]}""")
        assertEquals(listOf(400 to "蓝光", 10000 to "原画"), mapLiveQualities(live(play)))
        play.remove("stream")
        assertEquals(listOf(20000 to "4K", 10000 to "原画"), mapLiveQualities(live(play)))
        play.put("stream", JSONArray("""[{"format":[{"codec":[{"accept_qn":[80,150]}]}]}]"""))
        assertEquals(listOf(80 to "流畅", 150 to "高清"), mapLiveQualities(live(play)))
    }

    @Test fun livePlayUrlPrefersFlvAvcButKeepsFirstCandidateFallbackAndFirstHost() {
        val formats = JSONArray().put(stream("ts", "hevc", "https://first", "/one"))
            .put(stream("flv", "avc", "https://preferred", "/two"))
        val play = JSONObject().put("stream", JSONArray().put(JSONObject().put("format", formats)))
        assertEquals("https://preferred/two?x=1", parseLivePlayUrl(live(play)))
        formats.remove(1)
        assertEquals("https://first/one?x=1", parseLivePlayUrl(live(play)))
        assertNull(parseLivePlayUrl(JSONObject()))
    }

    @Test fun liveDanmakuHostsKeepSecureStableOrderDeduplicationAndTokenRequirement() {
        val data = JSONObject("""{"code":0,"data":{"token":"fixture-token","host_list":[
            {"host":"ws","ws_port":2244},{"host":"secure","wss_port":8443},
            {"host":"preferred","wss_port":443},{"host":"preferred","wss_port":443},
            {"host":"invalid"}]}}""")
        val result = mapLiveDanmuInfo(data)!!
        assertEquals("fixture-token", result.first)
        assertEquals(listOf("wss://preferred:443/sub", "wss://secure:8443/sub", "ws://ws:2244/sub"), result.second)
        data.getJSONObject("data").put("token", "")
        assertNull(mapLiveDanmuInfo(data))
    }

    @Test fun liveEmoticonsKeepLastDuplicateAndExistingRoomIdExtraction() {
        val json = JSONObject("""{"code":0,"data":{"data":[{"emoticons":[
            {"emoji":"[doge]","url":"first"},{"emoji":"[doge]","url":"last"},{"emoji":"[]","url":"invalid"}]}]}}""")
        assertEquals(mapOf("doge" to "last"), mapLiveEmoticons(json))
        assertEquals(77L, readLiveHotRoomId(JSONObject("""{"data":{"list":[{"roomid":77},{"roomid":88}]}}""")))
        assertEquals(99L, readLiveRoomId(JSONObject("""{"data":{"room_id":99}}""")))
    }

    @Test fun subtitleTrackAliasesAndCueMinimumDurationPreserveOriginalUnits() {
        val tracks = mapSubtitleTracks(JSONObject("""{"subtitle":{"subtitles":[{},
            {"lan":"zh-CN","subtitle_url_v2":"//sub/cues","ai_type":1}]}}"""))
        assertEquals(1, tracks.size)
        assertEquals("zh-CN", tracks.single().id)
        assertEquals("zh-CN", tracks.single().label)
        assertEquals("https://sub/cues", tracks.single().url)
        assertTrue(tracks.single().isAi)
        val cues = mapSubtitleCues("""{"body":[{"content":"  text  ","from":1.5,"to":1.6},{"content":" "}]}""")
        assertEquals(1500L, cues.single().fromMs)
        assertEquals(1800L, cues.single().toMs)
        assertEquals("text", cues.single().text)
    }

    @Test fun sponsorSegmentsFilterInvalidRangesAndKeepOriginalMilliseconds() {
        val segments = mapSponsorSegments("""[{}, {"segment":[1.25,2.5],"category":"sponsor"},
            {"segment":[3,3]},{"segment":[1]}]""")
        assertEquals(1, segments.size)
        assertEquals(1250L, segments.single().startMs)
        assertEquals(2500L, segments.single().endMs)
        assertEquals("skip", segments.single().actionType)
        assertEquals(7L to "7人", mapVideoOnline(JSONObject("""{"count":7,"total":"7人"}""")))
    }
    @Test fun progressiveFallbackCdnSampleAndCloudResumeUseOriginalFieldPaths() {
        assertEquals("first", readProgressiveFallback(JSONObject("""{"durl":[{"url":"first"},{"url":"second"}]}""")))
        assertEquals("", readProgressiveFallback(JSONObject()))
        val data = JSONObject("""{"dash":{"video":[{"base_url":"https://mcdn/v1/resource",
            "backup_url":["","https://upos/upgcxcode/video"]}]}}""")
        assertEquals("https://upos/upgcxcode/video", mapCdnSampleUrl(data) { it.contains("mcdn") })
        assertNull(mapCdnSampleUrl(JSONObject()) { false })
        assertEquals(10000L, mapCloudResumePositionMs(JSONObject("""{"last_play_cid":7,"last_play_time":10}"""), 7, 99))
        assertEquals(0L, mapCloudResumePositionMs(JSONObject("""{"last_play_cid":8,"last_play_time":10}"""), 7, 99))
    }
}
