package com.floatdeskpet.app

import android.app.Application
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.data.PetSettings

class FloatDeskPetApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PetSettings.get(this)
        AuthStore.get(this)
    }
}
