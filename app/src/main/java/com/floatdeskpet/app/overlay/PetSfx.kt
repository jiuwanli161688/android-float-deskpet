package com.floatdeskpet.app.overlay

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.PetSettings

class PetSfx(context: Context) {
    private val app = context.applicationContext
    private val settings = PetSettings.get(app)
    private var pool: SoundPool? = null
    private var appear = 0
    private var tap = 0
    private var menu = 0
    private var hide = 0

    fun appear() = play(load { appear })
    fun tap() = play(load { tap })
    fun menu() = play(load { menu })
    fun hide() = play(load { hide })

    fun release() {
        try {
            pool?.release()
        } catch (_: Throwable) {
        }
        pool = null
        appear = 0
        tap = 0
        menu = 0
        hide = 0
    }

    private fun load(id: () -> Int): Int {
        if (settings.muted) return 0
        ensure()
        return id()
    }

    private fun play(id: Int) {
        if (id == 0) return
        try {
            pool?.play(id, 0.72f, 0.72f, 1, 0, 1f)
        } catch (_: Throwable) {
        }
    }

    private fun ensure() {
        if (pool != null) return
        try {
            val created = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()
            appear = created.load(app, R.raw.sfx_appear, 1)
            tap = created.load(app, R.raw.sfx_tap, 1)
            menu = created.load(app, R.raw.sfx_menu, 1)
            hide = created.load(app, R.raw.sfx_hide, 1)
            pool = created
        } catch (_: Throwable) {
            pool = null
        }
    }
}
