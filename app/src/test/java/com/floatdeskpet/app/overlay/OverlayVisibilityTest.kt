package com.floatdeskpet.app.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayVisibilityTest {
    @Test
    fun hideWhenHomeFront() {
        assertFalse(OverlayVisibility.shouldShow(userVisible = true, homeFront = true))
        assertTrue(OverlayVisibility.shouldShow(userVisible = true, homeFront = false))
        assertFalse(OverlayVisibility.shouldShow(userVisible = false, homeFront = false))
    }
}
