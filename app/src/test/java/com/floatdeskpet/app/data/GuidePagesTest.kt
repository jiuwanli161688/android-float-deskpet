package com.floatdeskpet.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidePagesTest {
    @Test
    fun hasFinishedCarousel() {
        assertTrue(GuidePages.all.size >= 4)
        assertEquals(GuidePages.all.size, GuidePages.all.map { it.titleRes }.toSet().size)
        assertEquals(GuidePages.all.size, GuidePages.all.map { it.bodyRes }.toSet().size)
    }
}
