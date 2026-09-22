package com.floatdeskpet.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OverlayActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == OverlayService.ACTION_EXIT) {
            OverlayService.stop(context)
        } else {
            OverlayService.start(context, action)
        }
    }
}
