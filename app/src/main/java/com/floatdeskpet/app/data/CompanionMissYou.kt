package com.floatdeskpet.app.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.floatdeskpet.app.R
import com.floatdeskpet.app.overlay.OverlayVisibility
import com.floatdeskpet.app.ui.MainActivity
import java.util.Calendar

object CompanionMissYou {
    const val ACTION = "com.floatdeskpet.app.MISS_YOU"
    const val ACTION_OFF = "com.floatdeskpet.app.MISS_YOU_OFF"
    const val CHANNEL = "miss_you"
    const val NOTIF_ID = 18
    private const val REQ_AFTERNOON = 41
    private const val REQ_EVENING = 42
    private const val GAP_MS = 4L * 60L * 60L * 1000L

    fun reschedule(context: Context) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java)
        val settings = PetSettings.get(app)
        cancel(am, app)
        if (!settings.missYou || settings.muted) return
        setWindow(am, app, REQ_AFTERNOON, 14, 30)
        setWindow(am, app, REQ_EVENING, 20, 30)
    }

    fun onAlarm(context: Context) {
        val app = context.applicationContext
        val settings = PetSettings.get(app)
        if (!settings.missYou || settings.muted) return
        if (OverlayVisibility.homeFront) return
        if (!settings.running) return
        val now = System.currentTimeMillis()
        if (settings.lastInteractAt > 0L && now - settings.lastInteractAt < GAP_MS) return
        val today = CompanionDay.todayKey()
        if (settings.missYouDay != today) {
            settings.missYouDay = today
            settings.missYouCount = 0
        }
        if (settings.missYouCount >= 2) return
        if (!canNotify(app)) return
        settings.missYouCount = settings.missYouCount + 1
        post(app, settings)
    }

    fun disable(context: Context) {
        val app = context.applicationContext
        PetSettings.get(app).missYou = false
        reschedule(app)
        NotificationManagerCompat.from(app).cancel(NOTIF_ID)
    }

    private fun post(context: Context, settings: PetSettings) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            43,
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val off = PendingIntent.getBroadcast(
            context,
            44,
            Intent(context, MissYouReceiver::class.java).setAction(ACTION_OFF),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val name = settings.displayName()
        val text = if (settings.missYouCount == 1) {
            context.getString(R.string.miss_you_text_1, name)
        } else {
            context.getString(R.string.miss_you_text_2, name)
        }
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pet)
            .setContentTitle(context.getString(R.string.miss_you_title, name))
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.miss_you_off_action), off)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID, n)
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            CHANNEL,
            context.getString(R.string.miss_you_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        ch.setShowBadge(false)
        ch.enableVibration(false)
        ch.setSound(null, null)
        nm.createNotificationChannel(ch)
    }

    private fun setWindow(am: AlarmManager, context: Context, req: Int, hour: Int, minute: Int) {
        val t = nextAt(hour, minute)
        am.setAndAllowWhileIdle(AlarmManager.RTC, t, pi(context, req))
    }

    private fun cancel(am: AlarmManager, context: Context) {
        am.cancel(pi(context, REQ_AFTERNOON))
        am.cancel(pi(context, REQ_EVENING))
    }

    private fun pi(context: Context, req: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            req,
            Intent(context, MissYouReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextAt(hour: Int, minute: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= System.currentTimeMillis() + 60_000L) {
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.timeInMillis
    }
}
