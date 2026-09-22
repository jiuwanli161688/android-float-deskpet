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

    var outfit: String
        get() = prefs.getString(KEY_OUTFIT, OUTFIT_CASUAL) ?: OUTFIT_CASUAL
        set(value) = prefs.edit().putString(KEY_OUTFIT, value).apply()

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
        const val KEY_OUTFIT = "outfit"
        const val KEY_MOOD = "mood"
        const val KEY_AFFECTION = "affection"
        const val KEY_FEED = "feed_count"
        const val KEY_DECAY = "last_decay"
        const val KEY_INTERACT = "last_interact"
        const val KEY_FEED_REGEN = "last_feed_regen"
        const val OUTFIT_CASUAL = "casual"
        const val OUTFIT_PAJAMA = "pajama"
        const val OUTFIT_HOODIE = "hoodie"
        const val FEED_MAX = 8
        const val FEED_REGEN_MS = 3L * 60L * 60L * 1000L

        @Volatile
        private var instance: PetSettings? = null

        fun get(context: Context): PetSettings {
            return instance ?: synchronized(this) {
                instance ?: PetSettings(context).also { instance = it }
            }
        }
    }
}
