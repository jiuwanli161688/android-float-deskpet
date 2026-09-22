package com.floatdeskpet.app.overlay

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeWake(
    context: Context,
    private val onShake: () -> Unit,
) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var running = false
    private var lastShakeAt = 0L

    fun start() {
        if (running || sensor == null || manager == null) return
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        running = true
    }

    fun stop() {
        if (!running) return
        try {
            manager?.unregisterListener(this)
        } catch (_: Throwable) {
        }
        running = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val g = sqrt(ax * ax + ay * ay + az * az)
        if (g < 17f) return
        val now = System.currentTimeMillis()
        if (now - lastShakeAt < 900L) return
        lastShakeAt = now
        onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
