package com.floatdeskpet.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MissYouReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            CompanionMissYou.ACTION -> CompanionMissYou.onAlarm(context)
            CompanionMissYou.ACTION_OFF -> CompanionMissYou.disable(context)
        }
    }
}
