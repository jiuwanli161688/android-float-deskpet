package com.floatdeskpet.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {
    @Test
    fun accountAcceptsLettersAndDigits() {
        assertNull(AuthValidator.accountError("xiaoWan7"))
        assertNull(AuthValidator.accountError("abcde"))
        assertNull(AuthValidator.accountError("A1b2c3d4e5f6g7h8"))
    }

    @Test
    fun accountRejectsEmptyShortLongAndSymbols() {
        assertNotNull(AuthValidator.accountError(""))
        assertNotNull(AuthValidator.accountError("ab12"))
        assertNotNull(AuthValidator.accountError("abcdefghijklmnopqrs"))
        assertNotNull(AuthValidator.accountError("晚晴123"))
        assertNotNull(AuthValidator.accountError("hello_world"))
    }

    @Test
    fun passwordAndConfirm() {
        assertNotNull(AuthValidator.passwordError(""))
        assertNotNull(AuthValidator.passwordError("abc"))
        assertNull(AuthValidator.passwordError("abcd"))
        assertNotNull(AuthValidator.confirmError("abcd", ""))
        assertNotNull(AuthValidator.confirmError("abcd", "abce"))
        assertNull(AuthValidator.confirmError("abcd", "abcd"))
    }

    @Test
    fun nicknameRequired() {
        assertNotNull(AuthValidator.nicknameError("   "))
        assertNull(AuthValidator.nicknameError("晚风"))
    }

    @Test
    fun messagesAreChineseAndWarm() {
        val msg = AuthValidator.accountError("!!")
        assertNotNull(msg)
        assertTrue(msg!!.any { it in '\u4e00'..'\u9fff' })
        assertEquals("账号和暗号对不上。再想想，或者重新留下一个。", AuthValidator.loginMismatch())
    }
}
