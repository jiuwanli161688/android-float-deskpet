package com.floatdeskpet.app.auth

import android.content.Context
import android.content.SharedPreferences
import com.floatdeskpet.app.data.PetSettings

class AuthStore private constructor(context: Context) {
    private val app = context.applicationContext
    private val prefs: SharedPreferences =
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class User(
        val account: String,
        val password: String,
        val nickname: String,
        val happiness: Int = DEFAULT_HAPPINESS,
        val companionReady: Boolean = false,
    )

    fun isLoggedIn(): Boolean = currentAccount().isNotEmpty() && current() != null

    fun currentAccount(): String = prefs.getString(KEY_SESSION, "").orEmpty()

    fun current(): User? {
        val account = currentAccount()
        if (account.isEmpty()) return null
        return users()[account]
    }

    fun exists(account: String): Boolean = users().containsKey(account.trim())

    fun register(account: String, password: String, nickname: String): Result {
        AuthValidator.accountError(account)?.let { return Result.Err(it) }
        AuthValidator.passwordError(password)?.let { return Result.Err(it) }
        AuthValidator.nicknameError(nickname)?.let { return Result.Err(it) }
        val key = account.trim()
        if (exists(key)) return Result.Err(AuthValidator.accountTaken())
        val user = User(
            account = key,
            password = password,
            nickname = nickname.trim(),
        )
        saveUser(user)
        return Result.Ok(user)
    }

    fun login(account: String, password: String): Result {
        AuthValidator.accountError(account)?.let { return Result.Err(it) }
        if (password.isEmpty()) return Result.Err(AuthValidator.passwordError(password)!!)
        val user = users()[account.trim()]
        if (user == null || user.password != password) {
            return Result.Err(AuthValidator.loginMismatch())
        }
        val ready = user.companionReady || PetSettings.get(app).configured
        val synced = if (ready && !user.companionReady) {
            user.copy(companionReady = true).also { saveUser(it) }
        } else {
            user
        }
        prefs.edit().putString(KEY_SESSION, synced.account).apply()
        return Result.Ok(synced)
    }

    fun logout() {
        prefs.edit().remove(KEY_SESSION).apply()
    }

    fun markCompanionReady() {
        val user = current() ?: return
        saveUser(user.copy(companionReady = true))
    }

    fun addHappiness(delta: Int) {
        val user = current() ?: return
        saveUser(user.copy(happiness = (user.happiness + delta).coerceIn(0, 9999)))
    }

    private fun users(): Map<String, User> {
        val raw = prefs.getString(KEY_USERS, "").orEmpty()
        if (raw.isEmpty()) return emptyMap()
        return raw.lineSequence()
            .mapNotNull { parseUser(it) }
            .associateBy { it.account }
    }

    private fun saveUser(user: User) {
        val map = users().toMutableMap()
        map[user.account] = user
        val text = map.values.joinToString("\n") { encodeUser(it) }
        prefs.edit().putString(KEY_USERS, text).apply()
    }

    private fun encodeUser(user: User): String {
        return listOf(
            user.account,
            user.password,
            user.nickname.replace('\t', ' '),
            user.happiness.toString(),
            if (user.companionReady) "1" else "0",
        ).joinToString("\t")
    }

    private fun parseUser(line: String): User? {
        val parts = line.split('\t')
        if (parts.size < 5) return null
        return User(
            account = parts[0],
            password = parts[1],
            nickname = parts[2],
            happiness = parts[3].toIntOrNull() ?: DEFAULT_HAPPINESS,
            companionReady = parts[4] == "1",
        )
    }

    sealed class Result {
        data class Ok(val user: User) : Result()
        data class Err(val message: String) : Result()
    }

    companion object {
        const val PREFS = "auth"
        const val KEY_USERS = "users"
        const val KEY_SESSION = "session_account"
        const val DEFAULT_HAPPINESS = 88

        @Volatile
        private var instance: AuthStore? = null

        fun get(context: Context): AuthStore {
            return instance ?: synchronized(this) {
                instance ?: AuthStore(context).also { instance = it }
            }
        }
    }
}
