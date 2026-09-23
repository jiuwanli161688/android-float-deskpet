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
    fun maleIncludesDashu() {
        assertEquals(
            listOf("wenrou", "qingshuang", "chenwen", "dashu"),
            CharacterStyle.all(true).map { it.id },
        )
        assertEquals(CharacterStyle.DASHU, CharacterStyle.fromId("dashu", true))
        assertEquals(PetSettings.OUTFIT_HOODIE, CharacterStyle.DASHU.suggestedOutfit)
    }
}
