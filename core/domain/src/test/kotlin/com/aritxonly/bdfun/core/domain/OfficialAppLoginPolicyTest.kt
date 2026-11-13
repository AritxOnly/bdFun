package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

class OfficialAppLoginPolicyTest {
    @Test fun encodesWholeQrUrlWithoutLeakingInnerQueryParameters() {
        val url = "https://passport.bilibili.com/h5-app/passport/scan?auth_code=sample&source=tv#确认"
        val result = URI(officialAppLoginUri(url)!!)
        assertEquals("bilibili", result.scheme)
        assertEquals("browser", result.host)
        assertFalse(result.rawQuery.contains('&'))
        assertEquals(url, URLDecoder.decode(result.rawQuery.removePrefix("url="), Charsets.UTF_8))
        assertNull(result.fragment)
    }
    @Test fun existingEscapesPlusAndSpacesAreEncodedExactlyOnce() {
        val url = "https://passport.bilibili.com/scan?code=a%2Fb+c d"
        val result = officialAppLoginUri(url)!!
        assertTrue(result.contains("%252F"))
        assertTrue(result.contains("%2B"))
        assertTrue(result.contains("%20"))
        assertEquals(url, URLDecoder.decode(result.substringAfter("?url="), Charsets.UTF_8))
    }
    @Test fun absentAndNonWebUrlsCannotLaunchAnotherScheme() {
        listOf("", "bilibili://video/1", "javascript:alert(1)", "https://", "http://?code=1").forEach {
            assertNull(officialAppLoginUri(it))
        }
    }
}
