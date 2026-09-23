package com.floatdeskpet.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.CompanionDay
import com.floatdeskpet.app.data.CompanionMissYou
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.ui.MainActivity
import com.floatdeskpet.app.util.OverlayPermission

class OverlayService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var settings: PetSettings
    private var window: PetWindow? = null
    private var shake: ShakeWake? = null
    private var lastCharging: Boolean? = null
    private var toldLow = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> window?.pauseHeavy()
                Intent.ACTION_SCREEN_ON -> window?.resumeHeavy()
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val pct = level * 100 / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            if (pct in 0..15 && !charging && !toldLow) {
                toldLow = true
                window?.onBatteryLow()
            }
            if (charging && lastCharging == false) {
                window?.onCharging()
            }
            if (charging) toldLow = false
            lastCharging = charging
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = PetSettings.get(this)
        ensureChannel()
        settings.prefs.registerOnSharedPreferenceChangeListener(this)
        shake = ShakeWake(this) { window?.onShake() }
        registerSys(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })
        registerSys(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_EXIT -> {
                quit()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_VISIBLE -> settings.visible = !settings.visible
            ACTION_TOGGLE_GHOST -> settings.passThrough = !settings.passThrough
            ACTION_OPEN_FEED -> {
                if (settings.running && OverlayPermission.granted(this)) {
                    startAsForeground()
                    ensureWindow()
                    window?.openFeed()
                    return START_STICKY
                }
            }
            ACTION_CARDIO -> {
                if (settings.running && OverlayPermission.granted(this)) {
                    startAsForeground()
                    ensureWindow()
                    window?.cardio()
                    return START_STICKY
                }
            }
            ACTION_SYNC_VIS -> {
                if (settings.running && OverlayPermission.granted(this)) {
                    startAsForeground()
                    ensureWindow()
                    applyVisibility()
                    return START_STICKY
                }
            }
        }
        if (!OverlayPermission.granted(this) || !settings.running) {
            quit()
            return START_NOT_STICKY
        }
        try {
            startAsForeground()
            ensureWindow()
        } catch (_: Throwable) {
            quit()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        settings.prefs.unregisterOnSharedPreferenceChangeListener(this)
        shake?.stop()
        shake = null
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Throwable) {
        }
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Throwable) {
        }
        teardownWindow()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!settings.running) {
            quit()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PetSettings.KEY_SIZE,
            PetSettings.KEY_OPACITY,
            PetSettings.KEY_GHOST,
            PetSettings.KEY_X,
            PetSettings.KEY_Y,
            PetSettings.KEY_OUTFIT,
            PetSettings.KEY_GENDER,
            PetSettings.KEY_STYLE,
            PetSettings.KEY_NAME,
            PetSettings.KEY_MUTE,
            PetSettings.KEY_TTS,
            -> {
                window?.applySettings()
                if (key == PetSettings.KEY_MUTE) CompanionMissYou.reschedule(this)
            }
            PetSettings.KEY_MISS_YOU -> CompanionMissYou.reschedule(this)
            PetSettings.KEY_VISIBLE -> applyVisibility()
            PetSettings.KEY_RUNNING -> if (!settings.running) quit()
        }
        refreshNotification()
    }

    private fun ensureWindow() {
        if (!OverlayPermission.granted(this)) {
            quit()
            return
        }
        val w = window ?: PetWindow(this).also { created ->
            created.onPeekState = { peeking ->
                if (peeking) shake?.start() else shake?.stop()
            }
            window = created
        }
        try {
            w.attach()
            CompanionDay.tick(this)
            CompanionMissYou.reschedule(this)
            applyVisibility()
        } catch (_: Throwable) {
            quit()
        }
    }

    private fun applyVisibility() {
        window?.setVisible(OverlayVisibility.shouldShow(settings.visible))
    }

    private fun teardownWindow() {
        shake?.stop()
        try {
            window?.detach()
        } catch (_: Throwable) {
        }
        window = null
    }

    private fun quit() {
        settings.running = false
        teardownWindow()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
        }
        stopSelf()
    }

    private fun startAsForeground() {
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun refreshNotification() {
        if (!settings.running) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            pendingFlags(),
        )
        val hideShow = actionPi(1, ACTION_TOGGLE_VISIBLE)
        val ghost = actionPi(2, ACTION_TOGGLE_GHOST)
        val exit = actionPi(3, ACTION_EXIT)
        val hideLabel = if (settings.visible) getString(R.string.notif_hide) else getString(R.string.notif_show)
        val ghostLabel = if (settings.passThrough) getString(R.string.notif_ghost_off) else getString(R.string.notif_ghost_on)
        val text = if (settings.visible) getString(R.string.notif_text) else getString(R.string.notif_hidden_text)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pet)
            .setContentTitle(getString(R.string.notif_title, settings.displayName()))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(open)
            .addAction(0, hideLabel, hideShow)
            .addAction(0, ghostLabel, ghost)
            .addAction(0, getString(R.string.notif_exit), exit)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun actionPi(req: Int, action: String): PendingIntent {
        return PendingIntent.getBroadcast(
            this,
            req,
            Intent(this, OverlayActionReceiver::class.java).setAction(action),
            pendingFlags(),
        )
    }

    private fun pendingFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            CHANNEL,
            getString(R.string.notif_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        ch.setShowBadge(false)
        ch.enableVibration(false)
        ch.setSound(null, null)
        nm.createNotificationChannel(ch)
    }

    private fun registerSys(receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    companion object {
        const val CHANNEL = "pet_running"
        const val NOTIF_ID = 17
        const val ACTION_TOGGLE_VISIBLE = "com.floatdeskpet.app.TOGGLE_VISIBLE"
        const val ACTION_TOGGLE_GHOST = "com.floatdeskpet.app.TOGGLE_GHOST"
        const val ACTION_EXIT = "com.floatdeskpet.app.EXIT"
        const val ACTION_OPEN_FEED = "com.floatdeskpet.app.OPEN_FEED"
        const val ACTION_CARDIO = "com.floatdeskpet.app.CARDIO"
        const val ACTION_SYNC_VIS = "com.floatdeskpet.app.SYNC_VIS"

        fun syncVisibility(context: Context) {
            if (!PetSettings.get(context).running) return
            start(context, ACTION_SYNC_VIS)
        }

        fun start(context: Context, action: String? = null) {
            val app = context.applicationContext
            val settings = PetSettings.get(app)
            if (action == null) {
                settings.running = true
                if (!settings.visible) settings.visible = true
            }
            val intent = Intent(app, OverlayService::class.java).setAction(action)
            ContextCompat.startForegroundService(app, intent)
        }

        fun stop(context: Context) {
            val app = context.applicationContext
            PetSettings.get(app).running = false
            app.stopService(Intent(app, OverlayService::class.java))
        }
    }
}
