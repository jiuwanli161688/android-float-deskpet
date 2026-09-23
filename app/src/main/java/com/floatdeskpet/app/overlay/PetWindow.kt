package com.floatdeskpet.app.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.PathInterpolator
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.util.dpSize
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

class PetWindow(private val context: Context) : PetActions {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val settings = PetSettings.get(context)
    val view = PetSpriteView(context)
    private val sfx = PetSfx(context)
    private val voice = PetVoice(context)
    private val handler = Handler(Looper.getMainLooper())
    private var attached = false
    private var peeking = false
    private var peekSide = 1
    private var restoreX = 0
    private var restoreY = 0
    private var walking = false
    private var flinging = false
    private var flingVx = 0f
    private var flingVy = 0f
    private var lastAmbientAt = 0L
    private var lastBatteryAt = 0L
    private var heavyPaused = false
    private var moveAnim: ValueAnimator? = null
    private var bubbleAttached = false
    private val autonomy = PetAutonomy()
    var onPeekState: ((Boolean) -> Unit)? = null

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

    private val bubbleParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        width = 1
        height = 1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    init {
        view.onBubbleChanged = { syncBubble() }
        view.onDrag = { dx, dy ->
            stopMotion()
            moveBy(dx, dy)
        }
        view.onDragEnd = {
            if (peeking) settleAfterPeekDrag() else persist()
            scheduleIdle()
        }
        view.onFling = { vx, vy -> startFling(vx, vy) }
        view.onTap = {
            if (!peeking) {
                PetStats.onTap(settings)
                sfx.tap()
                say(PetDialogue.tap(settings))
                scheduleIdle()
            }
        }
        view.onExpressionCycle = { pose ->
            PetStats.onCycle(settings)
            sfx.tap()
            say(PetDialogue.pose(settings, pose))
            scheduleIdle()
        }
        view.onLongPressAction = {
            sfx.menu()
            stopMotion()
            scheduleIdle()
        }
        view.onPeekWake = {
            if (peeking) {
                wakeFromPeek()
                true
            } else {
                false
            }
        }
        view.onMenuPet = { pet() }
        view.onMenuFeed = { feed() }
        view.onMenuSleep = { sleep() }
    }

    fun attach() {
        voice.ensure()
        if (attached) {
            applySettings()
            return
        }
        PetStats.applyDecay(settings)
        applySettings()
        if (settings.posX == PetSettings.UNSET || settings.posY == PetSettings.UNSET) {
            placeDefault()
        }
        clamp()
        try {
            wm.addView(view, params)
            attached = true
            attachBubble()
        } catch (t: Throwable) {
            attached = false
            throw t
        }
        handler.postDelayed({ if (attached) sfx.appear() }, 120L)
        handler.postDelayed({
            if (attached) say(PetDialogue.greeting(settings), 3800L)
        }, 500L)
        scheduleIdle()
    }

    fun detach() {
        handler.removeCallbacksAndMessages(null)
        stopMotion()
        moveAnim?.cancel()
        view.hideMenu()
        view.hideBubbleNow()
        detachBubble()
        if (!attached) {
            sfx.release()
            voice.release()
            return
        }
        try {
            wm.removeViewImmediate(view)
        } catch (_: Throwable) {
            try {
                wm.removeView(view)
            } catch (_: Throwable) {
            }
        }
        attached = false
        peeking = false
        sfx.release()
        voice.release()
    }

    fun applySettings() {
        params.width = context.dpSize((settings.sizeDp * 3) / 4)
        params.height = context.dpSize(settings.sizeDp)
        params.flags = baseFlags()
        if (!peeking) {
            if (settings.posX != PetSettings.UNSET) params.x = settings.posX
            if (settings.posY != PetSettings.UNSET) params.y = settings.posY
        }
        view.alpha = settings.opacity / 100f
        view.isEnabled = !settings.passThrough
        view.applyCharacter()
        if (settings.muted || !settings.ttsEnabled) voice.silence() else voice.applyVoice()
        if (attached) {
            if (!peeking) clamp()
            update()
        }
    }

    fun setVisible(show: Boolean) {
        view.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        if (!show) {
            pauseHeavy()
            view.hideMenu()
            view.hideBubbleNow()
        } else if (attached) {
            resumeHeavy()
            sfx.appear()
        }
        if (attached) update()
    }

    fun pauseHeavy() {
        heavyPaused = true
        stopMotion()
        handler.removeCallbacks(idleWalk)
        handler.removeCallbacks(idleNap)
        handler.removeCallbacks(idlePeek)
        handler.removeCallbacks(idleAmbient)
        handler.removeCallbacks(resumeWalk)
        view.pauseAnim()
    }

    fun resumeHeavy() {
        if (!settings.visible) return
        heavyPaused = false
        if (peeking) {
            view.setPeeking(true)
            return
        }
        view.resumeAnim()
        scheduleIdle()
    }

    override fun pet() {
        if (peeking) wakeFromPeek()
        PetStats.onPet(settings)
        AuthStore.get(context).addHappiness(3)
        view.setNapping(false)
        view.playReaction(PetPose.SHY)
        say(PetDialogue.pet(settings))
        sfx.tap()
        scheduleIdle()
    }

    override fun feed(): Boolean {
        if (peeking) wakeFromPeek()
        if (!PetStats.feed(settings)) {
            view.playReaction(PetPose.SAD)
            say(PetDialogue.noSnack(settings))
            return false
        }
        AuthStore.get(context).addHappiness(6)
        view.setNapping(false)
        view.playReaction(PetPose.HAPPY)
        say(PetDialogue.feed(settings))
        sfx.tap()
        scheduleIdle()
        return true
    }

    override fun sleep() {
        if (peeking) wakeFromPeek()
        PetStats.onSleep(settings)
        enterNap()
        say(PetDialogue.sleep(settings))
        scheduleIdle()
    }

    override fun wakeFromPeek() {
        if (!peeking) return
        peeking = false
        view.setPeeking(false)
        onPeekState?.invoke(false)
        animateTo(restoreX, restoreY) {
            clamp()
            persist()
            update()
            sfx.appear()
            say(PetDialogue.wake(settings))
            view.playReaction(PetPose.WAVE)
            scheduleIdle()
        }
    }

    override fun onShake() {
        if (peeking) wakeFromPeek()
    }

    override fun onBatteryLow() {
        val now = System.currentTimeMillis()
        if (now - lastBatteryAt < 8 * 60_000L) return
        lastBatteryAt = now
        if (peeking) return
        say(PetDialogue.batteryLow(settings), 4200L)
        view.playReaction(PetPose.SAD)
    }

    override fun onCharging() {
        val now = System.currentTimeMillis()
        if (now - lastBatteryAt < 4 * 60_000L) return
        lastBatteryAt = now
        if (peeking) return
        say(PetDialogue.charging(settings), 3600L)
        view.playReaction(PetPose.HAPPY)
    }

    private fun enterNap() {
        walking = false
        flinging = false
        autonomy.cancel()
        view.setWalking(false)
        view.setNapping(true)
    }

    private fun startFling(vx: Float, vy: Float) {
        if (peeking) {
            settleAfterPeekDrag()
            return
        }
        walking = false
        autonomy.cancel()
        flinging = true
        flingVx = vx
        flingVy = vy
        handler.removeCallbacks(motion)
        handler.post(motion)
    }

    private fun startWalk() {
        if (!canAutonomous()) return
        view.setNapping(false)
        walking = true
        flinging = false
        view.setWalking(true)
        autonomy.begin(params.x, params.y, walkBounds(), SystemClock.uptimeMillis())
        view.setFacingRight(autonomy.lastFaceRight)
        handler.removeCallbacks(motion)
        handler.post(motion)
    }

    private val resumeWalk = Runnable { if (canAutonomous()) startWalk() }

    private fun finishWalk(nap: Boolean) {
        walking = false
        autonomy.cancel()
        view.setWalking(false)
        persist()
        if (!canAutonomous()) return
        if (nap) {
            enterNap()
            handler.postDelayed(resumeWalk, (10_000L..18_000L).random())
        } else {
            handler.postDelayed(resumeWalk, (4_500L..9_500L).random())
        }
    }

    private fun startPeek() {
        if (!canAutonomous() || peeking) return
        stopMotion()
        view.hideMenu()
        restoreX = params.x
        restoreY = params.y
        val b = bounds()
        peekSide = if (params.x + params.width / 2 > (b.minX + b.maxX) / 2) 1 else -1
        val hide = (params.width * 0.58f).toInt()
        val targetX = if (peekSide < 0) -hide else b.screenW - params.width + hide
        peeking = true
        view.setPeeking(true)
        onPeekState?.invoke(true)
        PetStats.onPeek(settings)
        sfx.hide()
        say(PetDialogue.peek(settings), 2200L)
        animateTo(targetX, params.y)
    }

    private fun settleAfterPeekDrag() {
        if (!peeking) {
            persist()
            return
        }
        val b = bounds()
        val pulledIn = if (peekSide < 0) {
            params.x > -params.width * 0.25f
        } else {
            params.x + params.width < b.screenW + params.width * 0.25f
        }
        if (pulledIn) {
            restoreX = params.x.coerceIn(b.minX, b.maxX)
            restoreY = params.y.coerceIn(b.minY, b.maxY)
            wakeFromPeek()
        } else {
            val hide = (params.width * 0.58f).toInt()
            val targetX = if (peekSide < 0) -hide else b.screenW - params.width + hide
            animateTo(targetX, params.y)
        }
    }

    private fun scheduleIdle() {
        handler.removeCallbacks(idleWalk)
        handler.removeCallbacks(idleNap)
        handler.removeCallbacks(idlePeek)
        handler.removeCallbacks(idleAmbient)
        handler.removeCallbacks(resumeWalk)
        if (!attached || heavyPaused || !settings.visible) return
        handler.postDelayed(idleWalk, 16_000L)
        handler.postDelayed(idleNap, 30_000L)
        handler.postDelayed(idlePeek, 48_000L)
        handler.postDelayed(idleAmbient, 22_000L)
    }

    private val idleWalk = Runnable { if (!peeking) startWalk() }
    private val idleNap = Runnable {
        if (canAutonomous() && !walking) enterNap()
    }
    private val idlePeek = Runnable { startPeek() }
    private val idleAmbient = object : Runnable {
        override fun run() {
            if (canAutonomous() && !view.isMenuOpen()) {
                val now = System.currentTimeMillis()
                if (now - lastAmbientAt > 50_000L) {
                    lastAmbientAt = now
                    val line = if (Random.nextFloat() < 0.4f) {
                        PetDialogue.timeOfDay(settings)
                    } else {
                        PetDialogue.ambient(settings)
                    }
                    say(line, 3400L)
                }
            }
            if (attached && !heavyPaused) handler.postDelayed(this, 70_000L)
        }
    }

    private val motion = object : Runnable {
        override fun run() {
            if (!attached || heavyPaused) return
            var keep = false
            if (flinging) {
                val dt = 0.032f
                moveBy((flingVx * dt).toInt(), (flingVy * dt).toInt())
                bounceFling()
                flingVx *= 0.90f
                flingVy *= 0.90f
                if (hypot(flingVx, flingVy) < 48f) {
                    flinging = false
                    persist()
                    view.playRelease()
                } else {
                    keep = true
                }
            }
            if (walking && !peeking) {
                val stepped = autonomy.step(params.x, params.y, walkBounds(), SystemClock.uptimeMillis())
                params.x = stepped.first
                params.y = stepped.second
                view.setFacingRight(autonomy.lastFaceRight)
                when (stepped.third) {
                    PetAutonomy.Result.MOVE -> {
                        update()
                        keep = true
                    }
                    PetAutonomy.Result.NAP -> {
                        update()
                        finishWalk(nap = true)
                    }
                    PetAutonomy.Result.IDLE -> {
                        update()
                        finishWalk(nap = false)
                    }
                }
            }
            if (keep) handler.postDelayed(this, 32L)
        }
    }

    private fun bounceFling() {
        val b = bounds()
        if (params.x <= b.minX) {
            params.x = b.minX
            flingVx = abs(flingVx) * 0.42f
        } else if (params.x >= b.maxX) {
            params.x = b.maxX
            flingVx = -abs(flingVx) * 0.42f
        }
        if (params.y <= b.minY) {
            params.y = b.minY
            flingVy = abs(flingVy) * 0.42f
        } else if (params.y >= b.maxY) {
            params.y = b.maxY
            flingVy = -abs(flingVy) * 0.42f
        }
    }

    private fun stopMotion() {
        walking = false
        flinging = false
        autonomy.cancel()
        view.setWalking(false)
        handler.removeCallbacks(motion)
        handler.removeCallbacks(resumeWalk)
        moveAnim?.cancel()
        moveAnim = null
    }

    private fun canAutonomous(): Boolean {
        return attached && settings.visible && !heavyPaused && !peeking && !view.isMenuOpen() && !settings.passThrough
    }

    private fun moveBy(dx: Int, dy: Int) {
        params.x += dx
        params.y += dy
        if (!peeking) clamp()
        update()
    }

    private fun persist() {
        if (peeking) return
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
        val b = bounds()
        params.x = params.x.coerceIn(b.minX, b.maxX)
        params.y = params.y.coerceIn(b.minY, b.maxY)
    }

    private fun animateTo(x: Int, y: Int, end: (() -> Unit)? = null) {
        moveAnim?.cancel()
        val sx = params.x
        val sy = params.y
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = 460L
        anim.interpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
        anim.addUpdateListener {
            val t = it.animatedValue as Float
            params.x = (sx + (x - sx) * t).toInt()
            params.y = (sy + (y - sy) * t).toInt()
            update()
        }
        anim.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                end?.invoke()
            }
        })
        moveAnim = anim
        anim.start()
    }

    private fun say(text: String, durationMs: Long = 3200L) {
        view.showBubble(text, durationMs)
        voice.speak(text)
    }

    private fun update() {
        if (!attached) return
        try {
            wm.updateViewLayout(view, params)
        } catch (_: Throwable) {
        }
        syncBubble()
    }

    private fun attachBubble() {
        if (bubbleAttached) return
        try {
            wm.addView(view.speechBubble, bubbleParams)
            bubbleAttached = true
            syncBubble()
        } catch (_: Throwable) {
            bubbleAttached = false
        }
    }

    private fun detachBubble() {
        if (!bubbleAttached) return
        try {
            wm.removeViewImmediate(view.speechBubble)
        } catch (_: Throwable) {
            try {
                wm.removeView(view.speechBubble)
            } catch (_: Throwable) {
            }
        }
        bubbleAttached = false
    }

    private fun syncBubble() {
        if (!attached || !bubbleAttached) return
        val bubble = view.speechBubble
        val show = bubble.visibility == View.VISIBLE &&
            view.visibility == View.VISIBLE &&
            !heavyPaused
        if (!show) {
            if (bubbleParams.width != 1 || bubbleParams.height != 1) {
                bubbleParams.width = 1
                bubbleParams.height = 1
                try {
                    wm.updateViewLayout(bubble, bubbleParams)
                } catch (_: Throwable) {
                }
            }
            return
        }
        val dm = context.resources.displayMetrics
        val maxW = min(context.dpSize(200), (dm.widthPixels * 3) / 5).coerceAtLeast(1)
        bubble.maxWidth = maxW
        bubble.measure(
            View.MeasureSpec.makeMeasureSpec(maxW, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val bw = bubble.measuredWidth.coerceAtLeast(1)
        val bh = bubble.measuredHeight.coerceAtLeast(1)
        val placed = SpeechBubbleLayout.place(
            params.x,
            params.y,
            params.width,
            params.height,
            bw,
            bh,
            dm.widthPixels,
            dm.heightPixels,
            context.dpSize(8),
            context.dpSize(28),
            context.dpSize(16),
            context.dpSize(10),
        )
        bubbleParams.width = bw
        bubbleParams.height = bh
        bubbleParams.x = placed.first
        bubbleParams.y = placed.second
        try {
            wm.updateViewLayout(bubble, bubbleParams)
        } catch (_: Throwable) {
        }
    }

    private data class Bounds(val minX: Int, val maxX: Int, val minY: Int, val maxY: Int, val screenW: Int)

    private fun walkBounds(): WalkBounds {
        val b = bounds()
        return WalkBounds(b.minX, b.maxX, b.minY, b.maxY)
    }

    private fun bounds(): Bounds {
        val dm = context.resources.displayMetrics
        val margin = context.dpSize(8)
        val minX = margin
        val minY = context.dpSize(24)
        val maxX = (dm.widthPixels - params.width - margin).coerceAtLeast(minX)
        val maxY = (dm.heightPixels - params.height - context.dpSize(48)).coerceAtLeast(minY)
        return Bounds(minX, maxX, minY, maxY, dm.widthPixels)
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
