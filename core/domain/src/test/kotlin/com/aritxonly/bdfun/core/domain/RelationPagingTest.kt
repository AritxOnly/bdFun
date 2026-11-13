package com.aritxonly.bdfun.core.domain



import org.junit.Assert.*
import org.junit.Test

class RelationPagingTest {
    @Test fun requestMatchesHyperPiliForEachListAndOrder() {
        val recent = relationListParams(42, false, 2, false)
        assertEquals(mapOf("vmid" to "42", "pn" to "2", "ps" to "20", "order" to "desc", "order_type" to ""), recent)
        assertEquals("attention", relationListParams(42, false, 1, true)["order_type"])
        assertFalse(relationListParams(42, true, 1, true).containsKey("order_type"))
    }
    @Test fun fullLastPageAndPrivatePartialPagesDoNotLoop() {
        assertNull(relationNextPage(2, 20, 40))
        assertNull(relationNextPage(1, 3, 999))
        assertNull(relationNextPage(3, 0, 999))
        assertEquals(2, relationNextPage(1, 20, 41))
        assertEquals(2, relationNextPage(1, 20, 0))
    }
    @Test fun appDefaultCannotHideCustomWebBannerAndFailuresHaveFallbacks() {
        assertEquals(listOf("custom", "app-day", "app-night"), orderedSpaceBanners("", "", "custom", "app-day", "app-night"))
        assertEquals(listOf("collection", "cover", "custom", "app-day"), orderedSpaceBanners("collection", "cover", "custom", "app-day", "app-day"))
    }
    @Test fun relativeWebPathsUseCdnAndDefaultDoesNotOverrideAppDressing() {
        val default = "bfs/space/cb1c3ef50e22b6096fde67febe863494caefebad.png"
        assertEquals(listOf("app-custom", "https://i0.hdslb.com/$default"), orderedSpaceBanners("", "", default, "app-custom", ""))
        assertEquals("https://i0.hdslb.com/bfs/space/custom.png", normalizeSpaceBanner("/bfs/space/custom.png"))
        assertEquals("https://i1.hdslb.com/image.png", normalizeSpaceBanner("//i1.hdslb.com/image.png"))
    }
    @Test fun collectionCropUsesPublishedLocationAndRejectsMalformedDimensions() {
        assertEquals(-0.5f, spaceBannerAlignment("crop-100-400", 1000.0), 0.0001f)
        assertEquals(0f, spaceBannerAlignment("crop-100-400", 0.0), 0f)
        assertEquals(0f, spaceBannerAlignment("garbage", 1000.0), 0f)
        assertEquals(0f, spaceBannerAlignment("crop-NaN-400", 1000.0), 0f)
    }
}
