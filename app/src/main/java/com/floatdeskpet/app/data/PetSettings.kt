package com.floatdeskpet.app.data

import android.content.Context
import android.content.SharedPreferences

class PetSettings private constructor(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var posX: Int
        get() = prefs.getInt(KEY_X, UNSET)
        set(value) = prefs.edit().putInt(KEY_X, value).apply()

    var posY: Int
        get() = prefs.getInt(KEY_Y, UNSET)
        set(value) = prefs.edit().putInt(KEY_Y, value).apply()

    var sizeDp: Int
        get() = prefs.getInt(KEY_SIZE, 160)
        set(value) = prefs.edit().putInt(KEY_SIZE, value.coerceIn(100, 220)).apply()

    var opacity: Int
        get() = prefs.getInt(KEY_OPACITY, 100)
        set(value) = prefs.edit().putInt(KEY_OPACITY, value.coerceIn(30, 100)).apply()

    var passThrough: Boolean
        get() = prefs.getBoolean(KEY_GHOST, false)
        set(value) = prefs.edit().putBoolean(KEY_GHOST, value).apply()

    var alwaysShow: Boolean
        get() = prefs.getBoolean(KEY_ALWAYS, true)
        set(value) = prefs.edit().putBoolean(KEY_ALWAYS, value).apply()

    var running: Boolean
        get() = prefs.getBoolean(KEY_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_RUNNING, value).apply()

    var visible: Boolean
        get() = prefs.getBoolean(KEY_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_VISIBLE, value).apply()

    var muted: Boolean
        get() = prefs.getBoolean(KEY_MUTE, false)
        set(value) = prefs.edit().putBoolean(KEY_MUTE, value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS, value).apply()

    var outfit: String
        get() = prefs.getString(KEY_OUTFIT, OUTFIT_CASUAL) ?: OUTFIT_CASUAL
        set(value) = prefs.edit().putString(KEY_OUTFIT, value).apply()

    var gender: String
        get() = prefs.getString(KEY_GENDER, GENDER_FEMALE) ?: GENDER_FEMALE
        set(value) = prefs.edit().putString(
            KEY_GENDER,
            if (value == GENDER_MALE) GENDER_MALE else GENDER_FEMALE,
        ).apply()

    var petName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value.trim().take(12)).apply()

    var configured: Boolean
        get() = prefs.getBoolean(KEY_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_CONFIGURED, value).apply()

    var mood: Int
        get() = prefs.getInt(KEY_MOOD, 72)
        set(value) = prefs.edit().putInt(KEY_MOOD, value.coerceIn(0, 100)).apply()

    var affection: Int
        get() = prefs.getInt(KEY_AFFECTION, 48)
        set(value) = prefs.edit().putInt(KEY_AFFECTION, value.coerceIn(0, 100)).apply()

    var feedCount: Int
        get() = prefs.getInt(KEY_FEED, FEED_MAX)
        set(value) = prefs.edit().putInt(KEY_FEED, value.coerceIn(0, FEED_MAX)).apply()

    var lastDecayAt: Long
        get() = prefs.getLong(KEY_DECAY, 0L)
        set(value) = prefs.edit().putLong(KEY_DECAY, value).apply()

    var lastInteractAt: Long
        get() = prefs.getLong(KEY_INTERACT, 0L)
        set(value) = prefs.edit().putLong(KEY_INTERACT, value).apply()

    var lastFeedRegenAt: Long
        get() = prefs.getLong(KEY_FEED_REGEN, 0L)
        set(value) = prefs.edit().putLong(KEY_FEED_REGEN, value).apply()

    val isMale: Boolean get() = gender == GENDER_MALE

    fun displayName(): String {
        val n = petName.trim()
        return n.ifEmpty { defaultName(isMale) }
    }

    fun needsSetup(): Boolean {
        if (configured) return false
        if (prefs.contains(KEY_RUNNING) || prefs.contains(KEY_X) || prefs.contains(KEY_OUTFIT)) {
            if (petName.isBlank()) petName = DEFAULT_FEMALE
            gender = GENDER_FEMALE
            configured = true
            return false
        }
        return true
    }

    fun applySetup(male: Boolean, name: String) {
        gender = if (male) GENDER_MALE else GENDER_FEMALE
        val trimmed = name.trim()
        petName = trimmed.ifEmpty { defaultName(male) }
        if (male && outfit == OUTFIT_PAJAMA) outfit = OUTFIT_CASUAL
        configured = true
    }

    companion object {
        const val PREFS = "pet"
        const val UNSET = Int.MIN_VALUE
        const val KEY_X = "pos_x"
        const val KEY_Y = "pos_y"
        const val KEY_SIZE = "size_dp"
        const val KEY_OPACITY = "opacity"
        const val KEY_GHOST = "pass_through"
        const val KEY_ALWAYS = "always_show"
        const val KEY_RUNNING = "running"
        const val KEY_VISIBLE = "visible"
        const val KEY_MUTE = "muted"
        const val KEY_TTS = "tts_enabled"
        const val KEY_OUTFIT = "outfit"
        const val KEY_GENDER = "gender"
        const val KEY_NAME = "pet_name"
        const val KEY_CONFIGURED = "configured"
        const val KEY_MOOD = "mood"
        const val KEY_AFFECTION = "affection"
        const val KEY_FEED = "feed_count"
        const val KEY_DECAY = "last_decay"
        const val KEY_INTERACT = "last_interact"
        const val KEY_FEED_REGEN = "last_feed_regen"
        const val OUTFIT_CASUAL = "casual"
        const val OUTFIT_PAJAMA = "pajama"
        const val OUTFIT_HOODIE = "hoodie"
        const val GENDER_FEMALE = "female"
        const val GENDER_MALE = "male"
        const val DEFAULT_FEMALE = "杏杏"
        const val DEFAULT_MALE = "阿辰"
        const val FEED_MAX = 8
        const val FEED_REGEN_MS = 3L * 60L * 60L * 1000L

        fun defaultName(male: Boolean): String = if (male) DEFAULT_MALE else DEFAULT_FEMALE

        @Volatile
        private var instance: PetSettings? = null

        fun get(context: Context): PetSettings {
            return instance ?: synchronized(this) {
                instance ?: PetSettings(context).also { instance = it }
            }
        }
    }
}
