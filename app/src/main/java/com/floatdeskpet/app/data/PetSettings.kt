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

        @Volatile
        private var instance: PetSettings? = null

        fun get(context: Context): PetSettings {
            return instance ?: synchronized(this) {
                instance ?: PetSettings(context).also { instance = it }
            }
        }
    }
}
