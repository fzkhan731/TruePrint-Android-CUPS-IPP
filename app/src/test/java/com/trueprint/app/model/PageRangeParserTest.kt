package com.trueprint.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageRangeParserTest {
    @Test fun parsesSingleRangesAndKeepsSpecifiedOrder() {
        val result = PageRangeParser.parse("1,4-6,3", 8).getOrThrow()
        assertEquals(listOf(PageRange(1, 1), PageRange(4, 6), PageRange(3, 3)), result)
    }
    @Test fun rejectsInvalidSelections() {
        listOf("0", "-1", "8-3", "1,,3", "abc", "1-", "-5").forEach {
            assertTrue("$it should be invalid", PageRangeParser.parse(it, 10).isFailure)
        }
    }
    @Test fun rejectsOutOfRangePages() { assertTrue(PageRangeParser.parse("1-4", 3).isFailure) }
}
