package com.floatdeskpet.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterStyleTest {
    @Test
    fun femaleIds() {
        assertEquals(listOf("yujie", "luoli", "qingchun"), CharacterStyle.all(false).map { it.id })
    }

    @Test
    fun maleIncludesChenggong() {
        assertEquals(
            listOf("wenrou", "qingshuang", "chenwen", "dashu", "chenggong"),
            CharacterStyle.all(true).map { it.id },
        )
        assertEquals(CharacterStyle.DASHU, CharacterStyle.fromId("dashu", true))
        assertEquals(CharacterStyle.CHENGGONG, CharacterStyle.fromId("chenggong", true))
        assertEquals(PetSettings.OUTFIT_HOODIE, CharacterStyle.DASHU.suggestedOutfit)
        assertEquals(PetSettings.OUTFIT_CASUAL, CharacterStyle.CHENGGONG.suggestedOutfit)
    }

    @Test
    fun dedicatedArtStyles() {
        assertTrue(CharacterStyle.entries.all { it.hasDedicatedArt })
        assertEquals(
            setOf("wenrou", "qingshuang", "chenwen", "dashu", "chenggong"),
            CharacterStyle.all(true).filter { it.hasDedicatedArt }.map { it.id }.toSet(),
        )
    }
}
