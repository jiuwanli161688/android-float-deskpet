package com.floatdeskpet.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.util.dpSize

class PetWindow(private val context: Context) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val settings = PetSettings.get(context)
    val view = PetSpriteView(context)
    private var attached = false
    private val params = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
        flags = baseFlags()
        width = context.dpSize((settings.sizeDp * 3) / 4)
        height = context.dpSize(settings.sizeDp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    init {
        view.onDrag = { dx, dy -> moveBy(dx, dy) }
        view.onDragEnd = { persist() }
    }

    fun attach() {
        if (attached) {
            applySettings()
            return
        }
        applySettings()
        if (settings.posX == PetSettings.UNSET || settings.posY == PetSettings.UNSET) {
            placeDefault()
        }
        clamp()
        try {
            wm.addView(view, params)
            attached = true
        } catch (t: Throwable) {
            attached = false
            throw t
        }
    }

    fun detach() {
        if (!attached) return
        try {
            wm.removeViewImmediate(view)
        } catch (_: Throwable) {
            try {
                wm.removeView(view)
            } catch (_: Throwable) {
            }
        }
        attached = false
    }

    fun applySettings() {
        params.width = context.dpSize((settings.sizeDp * 3) / 4)
        params.height = context.dpSize(settings.sizeDp)
        params.flags = baseFlags()
        if (settings.posX != PetSettings.UNSET) params.x = settings.posX
        if (settings.posY != PetSettings.UNSET) params.y = settings.posY
        view.alpha = settings.opacity / 100f
        view.isEnabled = !settings.passThrough
        if (attached) {
            clamp()
            try {
                wm.updateViewLayout(view, params)
            } catch (_: Throwable) {
            }
        }
    }

    fun setVisible(show: Boolean) {
        view.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        if (attached) {
            try {
                wm.updateViewLayout(view, params)
            } catch (_: Throwable) {
            }
        }
    }

    private fun moveBy(dx: Int, dy: Int) {
        params.x += dx
        params.y += dy
        clamp()
        if (attached) {
            try {
                wm.updateViewLayout(view, params)
            } catch (_: Throwable) {
            }
        }
    }

    private fun persist() {
        settings.posX = params.x
        settings.posY = params.y
    }

    private fun placeDefault() {
        val dm = context.resources.displayMetrics
        params.x = (dm.widthPixels - params.width - context.dpSize(16)).coerceAtLeast(0)
        params.y = (dm.heightPixels / 3).coerceAtLeast(0)
        persist()
    }

    private fun clamp() {
        val dm = context.resources.displayMetrics
        val maxX = (dm.widthPixels - params.width).coerceAtLeast(0)
        val maxY = (dm.heightPixels - params.height).coerceAtLeast(0)
        params.x = params.x.coerceIn(0, maxX)
        params.y = params.y.coerceIn(0, maxY)
    }

    private fun baseFlags(): Int {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (settings.passThrough) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        return flags
    }
}
