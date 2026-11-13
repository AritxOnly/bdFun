package com.aritxonly.bdfun.infra.storage

import org.junit.Assert.*
import org.junit.Test

class CdnSpeedsCodecTest {
    @Test fun roundTripPreservesLegacyColonCommaFormatAndOrder() {
        val scores = linkedMapOf("ali" to 1.25, "cos" to 4.5)
        assertEquals("ali:1.25,cos:4.5", encodeCdnSpeeds(scores))
        assertEquals(scores, decodeCdnSpeeds("ali:1.25,cos:4.5"))
    }
    @Test fun malformedFragmentsAreSkippedAndLastDuplicateWins() {
        assertTrue(decodeCdnSpeeds("  ").isEmpty())
        assertEquals(mapOf("ali" to 2.0, "key:part" to 3.0), decodeCdnSpeeds("ali:1,bad,:4,cos:oops,ali:2,key:part:3"))
    }
}
