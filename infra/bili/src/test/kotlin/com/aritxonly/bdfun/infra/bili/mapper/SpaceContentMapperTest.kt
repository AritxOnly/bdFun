package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SpaceContentMapperTest {
    private fun sample(name: String) = javaClass.classLoader!!.getResourceAsStream("json/$name")!!
        .bufferedReader().use { JSONObject(it.readText()).getJSONObject("data") }

    @Test fun realProfileBriefAndNavCountKeepOriginalImageBoundary() {
        val brief = readSpaceBrief(sample("space-info-2283296.json"))!!
        assertEquals("小猪P_", brief.first)
        assertTrue(brief.second.startsWith("https://"))
        assertEquals(515L, readSpaceVideoCount(sample("space-nav-2283296.json")))
        assertNull(readSpaceBrief(JSONObject()))
        assertEquals("", readSpaceIpLocation(JSONObject()))
        assertEquals(0L, readSpaceLikeCount(null))
        val raw = JSONObject("""{"bvid":"BV1orW46aEba","title":"title","length":"01:23",
            "mid":3690989657000489,"cid":43443290870}""")
        val video = mapSpaceVideo(raw) { "decoded:$it" }!!
        assertEquals("decoded:title", video.title)
        assertEquals(83, video.duration)
        assertEquals(3690989657000489L, video.ownerMid)
        assertEquals(43443290870L, video.cid)
        assertNull(mapSpaceVideo(JSONObject()) { it })
    }

    @Test fun collectionsPreserveSeasonsThenSeriesAndDoNotIntroduceDeduplication() {
        val result = mapSpaceCollections(JSONObject("""{"items_lists":{
            "seasons_list":[{"meta":{"season_id":7,"name":"season","cover":"//img/s.jpg"}},
                {"meta":{"season_id":7,"title":"repeat","cover":"//img/s.jpg"}},{"meta":{"season_id":8,"name":"invalid"}}],
            "series_list":[{"meta":{"series_id":7,"title":"series","cover":"//img/t.jpg","total":9}}]}}"""))
        assertEquals(listOf(7L, 7L, 7L), result.map { it.id })
        assertEquals(listOf("season", "repeat", "series"), result.map { it.title })
        assertEquals(listOf(false, false, true), result.map { it.isSeries })
        assertEquals(9, result.last().total)
    }

    @Test fun archivesKeepCidOwnerAndNullOnlyStatFallback() {
        val result = mapSpaceArchives(JSONObject("""{"archives":[{},
            {"bvid":"one","cid":43443290870,"play":99,"stat":{}},
            {"bvid":"two","play":7,"duration":8}]}"""), 3690989657000489)
        assertEquals(listOf("one", "two"), result.map { it.bvid })
        assertEquals(0L, result.first().play)
        assertEquals(7L, result.last().play)
        assertEquals(43443290870L, result.first().cid)
        assertTrue(result.all { it.ownerMid == 3690989657000489L })
    }

    @Test fun articleCoverPresenceAndBangumiFilteringKeepOriginalRules() {
        val articles = mapSpaceArticles(JSONObject("""{"articles":[{},
            {"id":7,"covers":[""],"cover":"//img/fallback.jpg","stats":{},"view":99},
            {"id":8,"cover":"//img/fallback.jpg","view":9,"publish_time":12}]}"""))
        assertEquals("", articles.first().cover)
        assertEquals(0L, articles.first().view)
        assertTrue(articles.last().cover.contains("@480w"))
        assertEquals(9L, articles.last().view)
        val bangumi = mapSpaceBangumi(JSONObject("""{"list":[{"season_id":1},
            {"season_id":2,"title":"番剧","progress":"进度","total_count":-1}]}""")).single()
        assertEquals(2L, bangumi.seasonId)
        assertEquals("进度", bangumi.progress)
        assertEquals(-1, bangumi.total)
    }

    @Test fun shopKeepsPriceSpacingLabelOrderAndSourceFallback() {
        val result = mapSpaceShop(JSONObject("""{"data":[{},
            {"title":"商品","netPrice":{"pricePrefix":"起","priceSymbol":"¥","netPrice":"9.9"},
            "belowLabels":[{"title":"A"},{"title":""},{"title":"B"}],"itemSourceName":"fallback","cardUrl":"url"},
            {"title":"第二项","itemSourceName":"fallback"}]}"""))
        assertEquals(2, result.size)
        assertEquals("起 ¥9.9", result.first().price)
        assertEquals("A|B", result.first().source)
        assertEquals("url", result.first().url)
        assertEquals("", result.last().price)
        assertEquals("fallback", result.last().source)
    }
}
