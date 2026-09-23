package com.floatdeskpet.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NameBankTest {
    private val cliche = setOf("小白", "小美", "阿强", "宝贝", "妹妹", "哥哥", "小明", "杏杏", "阿辰")

    @Test
    fun hasEnoughNames() {
        assertTrue(NameBank.female.size >= 8)
        assertTrue(NameBank.male.size >= 8)
    }

    @Test
    fun avoidsCliche() {
        (NameBank.female + NameBank.male).forEach { name ->
            assertFalse(name, cliche.contains(name))
        }
    }

    @Test
    fun pickIsRoundRobin() {
        val first = NameBank.pick(true, 0)
        val wrapped = NameBank.pick(true, NameBank.male.size)
        assertEquals(NameBank.male[0], first)
        assertEquals(first, wrapped)
        assertEquals(NameBank.female[3], NameBank.pick(false, 3))
    }
}
