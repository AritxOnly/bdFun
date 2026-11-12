package com.aritxonly.bdfun.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class BiliImageUrlTest {
    @Test fun blankInputPreservesEmptyResult() {
        assertEquals("", biliImageUrl(" \n ", 10))
    }
    @Test fun schemesAndOriginalWhitespaceArePreserved() {
        assertEquals("https://i/a", biliImageUrl("//i/a"))
        assertEquals("https://i/a", biliImageUrl("http://i/a"))
        assertEquals(" https://i/a ", biliImageUrl(" https://i/a "))
    }
    @Test fun widthUsesOriginalIntegerAspectCalculation() {
        assertEquals("https://i/a@101w_56h_1c.webp", biliImageUrl("//i/a", 101))
        assertEquals("https://i/a@1w_0h_1c.webp", biliImageUrl("//i/a", 1))
    }
    @Test fun nonPositiveWidthAndExistingAtSignDoNotAppend() {
        assertEquals("https://i/a", biliImageUrl("//i/a", -1))
        assertEquals("https://i/a@old", biliImageUrl("//i/a@old", 100))
        assertEquals("https://i/a?x=@", biliImageUrl("//i/a?x=@", 100))
    }
}
