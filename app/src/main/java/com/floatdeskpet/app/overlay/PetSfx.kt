package com.floatdeskpet.app.overlay

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.PetSettings

class PetSfx(context: Context) {
    private val settings = PetSettings.get(context)
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val appear = pool.load(context, R.raw.sfx_appear, 1)
    private val tap = pool.load(context, R.raw.sfx_tap, 1)
    private val menu = pool.load(context, R.raw.sfx_menu, 1)
    private val hide = pool.load(context, R.raw.sfx_hide, 1)

    fun appear() = play(appear)
    fun tap() = play(tap)
    fun menu() = play(menu)
    fun hide() = play(hide)

    fun release() {
        try {
            pool.release()
        } catch (_: Throwable) {
        }
    }

    private fun play(id: Int) {
        if (settings.muted) return
        try {
            pool.play(id, 0.72f, 0.72f, 1, 0, 1f)
        } catch (_: Throwable) {
        }
    }
}
