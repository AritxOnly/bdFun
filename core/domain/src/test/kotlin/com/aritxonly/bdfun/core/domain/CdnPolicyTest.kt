package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.playback.CdnService
import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class CdnPolicyTest {
    private val policy = CdnPolicy(
        inspect = { runCatching { URI(it).let { u -> CdnUrlParts(u.host.orEmpty(), u.path.orEmpty()) } }.getOrNull() },
        replaceHost = { url, host -> url.replace(URI(url).rawAuthority, host) },
    )
    private val upos = "https://upos-old.bilivideo.com/upgcxcode/a.m4s?token=a%2Fb"
    private val mcdn = "https://node.mcdn.bilivideo.cn:8082/v1/resource/a"
    private val target = CdnService("test", "test", "upos-target.bilivideo.com")

    @Test fun originalModeRetainsExactUrl() {
        assertEquals(upos, policy.rewrite(CdnService("base", "base"), upos, emptyList()))
    }
    @Test fun backupModeUsesFirstNonBlankUrl() {
        assertEquals(upos, policy.rewrite(CdnService("backup", "backup", useBackup = true), mcdn, listOf("", upos)))
    }
    @Test fun mcdnTicketCannotBeRewrittenWithoutPortableBackup() {
        assertTrue(policy.isMcdnResource(mcdn))
        assertEquals(mcdn, policy.rewrite(target, mcdn, emptyList()))
    }
    @Test fun portableBackupCanReplaceMcdnAndRetainsQueryBytes() {
        assertEquals("https://upos-target.bilivideo.com/upgcxcode/a.m4s?token=a%2Fb", policy.rewrite(target, mcdn, listOf(upos)))
    }
    @Test fun automaticModePrefersUposOverOtherPortableNode() {
        val other = "https://other.bilivideo.com/upgcxcode/a.m4s"
        assertEquals(upos, policy.rewrite(CdnService("auto", "auto", auto = true), other, listOf(upos)))
    }
    @Test fun unrelatedPathsAndEmptyUrlsKeepFallbackBehavior() {
        val ordinary = "https://example.com/video.mp4"
        assertEquals(ordinary, policy.rewrite(target, ordinary, emptyList()))
        assertEquals(upos, policy.rewrite(target, "", listOf("", upos)))
        assertFalse(policy.isMcdnResource("https://node.mcdn.bilivideo.cn/upgcxcode/a"))
        assertFalse(policy.isMcdnResource("https://node.example.com/v1/resource/a"))
    }
}
