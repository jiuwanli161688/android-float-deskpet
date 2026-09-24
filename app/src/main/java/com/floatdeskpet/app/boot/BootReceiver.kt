package com.floatdeskpet.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.floatdeskpet.app.data.CompanionMissYou
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.overlay.OverlayService
import com.floatdeskpet.app.util.OverlayPermission

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = PetSettings.get(context)
        CompanionMissYou.reschedule(context)
        if (settings.alwaysShow && settings.running && OverlayPermission.granted(context)) {
            OverlayService.start(context)
        }
    }
}
