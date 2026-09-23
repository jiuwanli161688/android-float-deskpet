package com.floatdeskpet.app.ui

import android.app.Activity
import android.content.Intent
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.data.GuideStore

object AppFlow {
    fun enter(activity: Activity): Boolean {
        val dest = destination(activity)
        if (activity::class.java == dest) return true
        activity.startActivity(
            Intent(activity, dest)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        activity.finish()
        return false
    }

    fun route(activity: Activity) {
        enter(activity)
    }

    fun destination(activity: Activity): Class<out Activity> {
        if (!GuideStore.get(activity).completed) return GuideActivity::class.java
        val auth = AuthStore.get(activity)
        val user = auth.current()
        return when {
            !auth.isLoggedIn() || user == null -> LoginActivity::class.java
            !user.companionReady -> SetupActivity::class.java
            else -> MainActivity::class.java
        }
    }
}
