package com.floatdeskpet.app.data

import android.content.Context

class GuideStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var completed: Boolean
        get() = prefs.getBoolean(KEY_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_DONE, value).apply()

    companion object {
        const val PREFS = "guide"
        const val KEY_DONE = "completed"

        @Volatile
        private var instance: GuideStore? = null

        fun get(context: Context): GuideStore {
            return instance ?: synchronized(this) {
                instance ?: GuideStore(context).also { instance = it }
            }
        }
    }
}
