package com.floatdeskpet.app.auth

object AuthValidator {
    private val accountRegex = Regex("^[A-Za-z0-9]+$")

    fun accountError(raw: String): String? {
        val account = raw.trim()
        if (account.isEmpty()) return "先起个账号吧，好让我记得你。"
        if (account.length < 5) return "再写长一点点，至少五个字符。"
        if (account.length > 18) return "太长啦，十八个字符以内就好。"
        if (!accountRegex.matches(account)) return "账号只用字母和数字就好，像给自己起的小暗号。"
        return null
    }

    fun passwordError(password: String): String? {
        if (password.isEmpty()) return "还差一句只有我们知道的暗号。"
        if (password.length < 4) return "再写长一点点，至少四个字，好让我认得你。"
        if (password.length > 32) return "太长啦，三十二个字以内就好。"
        return null
    }

    fun confirmError(password: String, confirm: String): String? {
        if (confirm.isEmpty()) return "再写一遍暗号，确认是同一句。"
        if (password != confirm) return "两次写的不太一样，再对一下？"
        return null
    }

    fun nicknameError(raw: String): String? {
        val name = raw.trim()
        if (name.isEmpty()) return "给自己起个称呼吧，我会这么叫你。"
        if (name.length > 12) return "称呼短一点就好，十二个字以内。"
        return null
    }

    fun loginMismatch(): String = "账号和暗号对不上。再想想，或者重新留下一个。"

    fun accountTaken(): String = "这个账号已经有人用过啦，换一个试试？"
}
