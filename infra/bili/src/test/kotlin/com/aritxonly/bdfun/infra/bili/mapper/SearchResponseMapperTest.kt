package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SearchResponseMapperTest {
    @Test fun realSuggestionsRetainTermPriorityDistinctOrderAndEightItemLimit() {
        val data = javaClass.classLoader!!.getResourceAsStream("json/search-suggest-anonymous.json")!!
            .bufferedReader().use { JSONObject(it.readText()) }
        val result = mapSearchSuggestions(data) { error("real sample has terms") }
        assertEquals(8, result.size)
        assertEquals("灭霸的爸爸叫灭建国", result.first())
        val variant = JSONObject("""{"result":{"tag":[{"term":"term","name":"ignored"},
            {"term":"term"},{"name":"name"}]}}""")
        assertEquals(listOf("term", "decoded:name"), mapSearchSuggestions(variant) { "decoded:$it" })
    }
    @Test fun videosAndUsersKeepExplicitHtmlBoundaryAndLegacyNumberParsing() {
        val data = JSONObject("""{"result":[{}, {"bvid":"BV1orW46aEba","aid":117584544403664,
            "title":"title","author":"author","mid":3690989657000489,"play":"1.2万",
            "danmaku":"7","duration":"01:02","pubdate":123,
            "uname":"user","usign":"sign","fans":"9","videos":"bad","level":6}]}""")
        val video = mapSearchVideos(data) { "decoded:$it" }.single()
        assertEquals("decoded:title", video.title)
        assertEquals("decoded:author", video.ownerName)
        assertEquals(3690989657000489L, video.ownerMid)
        assertEquals(0L, video.play)
        assertEquals(7L, video.danmaku)
        assertEquals(62, video.duration)
        val user = mapSearchUsers(data) { "decoded:$it" }.single()
        assertEquals("decoded:user", user.name)
        assertEquals("decoded:sign", user.sign)
        assertEquals(9L, user.fans)
        assertEquals(0L, user.videos)
        assertEquals(6, user.level)
    }
    @Test fun hotwordSourcesRetainDuplicatesAndOriginalFallbackFields() {
        val data = JSONObject("""{"trending":{"list":[{"keyword":" word "},{"keyword":"word"},
            {"keyword":"word"},{"keyword":""}]}}""")
        assertEquals(listOf(" word ", "word", "word"), mapSearchDiscovery(data))
        assertEquals(mapSearchDiscovery(data), mapHotSearch(data))
        assertEquals(listOf("keyword", "name"), mapHotwordSearch(JSONObject("""{"data":[
            {"keyword":"keyword","name":"ignored"},{"name":"name"},{}]}""")))
        assertTrue(mapHotwordSearch(JSONObject()).isEmpty())
    }
}
