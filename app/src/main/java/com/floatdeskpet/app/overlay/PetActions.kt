package com.floatdeskpet.app.overlay

interface PetActions {
    fun pet()
    fun feed(): Boolean
    fun sleep()
    fun wakeFromPeek()
    fun onShake()
    fun onBatteryLow()
    fun onCharging()
}
