package com.floatdeskpet.app.ui

import android.app.Activity
import android.content.Intent
import com.floatdeskpet.app.auth.AuthStore

object AppFlow {
    fun route(activity: Activity) {
        val dest = destination(activity)
        if (activity::class.java == dest) return
        activity.startActivity(
            Intent(activity, dest)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        activity.finish()
    }

    fun destination(activity: Activity): Class<out Activity> {
        val auth = AuthStore.get(activity)
        val user = auth.current()
        return when {
            !auth.isLoggedIn() || user == null -> LoginActivity::class.java
            !user.companionReady -> SetupActivity::class.java
            else -> MainActivity::class.java
        }
    }
}
