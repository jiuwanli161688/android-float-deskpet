package com.floatdeskpet.app.overlay

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.PetSettings
import kotlin.math.abs
import kotlin.math.hypot

class PetSpriteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null
    var onFling: ((vx: Float, vy: Float) -> Unit)? = null
    var onTap: (() -> Unit)? = null
    var onExpressionCycle: ((PetPose) -> Unit)? = null
    var onLongPressAction: (() -> Unit)? = null
    var onPeekWake: (() -> Boolean)? = null
    var onMenuPet: (() -> Unit)? = null
    var onMenuFeed: (() -> Unit)? = null
    var onMenuSleep: (() -> Unit)? = null

    private val settings = PetSettings.get(context)
    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
    }
    var onBubbleChanged: (() -> Unit)? = null

    val speechBubble = TextView(context).apply {
        setBackgroundResource(R.drawable.bg_bubble)
        setTextColor(context.getColor(R.color.text_main))
        textSize = 12f
        gravity = Gravity.CENTER
        visibility = GONE
        maxLines = 3
        includeFontPadding = false
        val p = dp(8)
        setPadding(p, dp(6), p, dp(6))
    }
    private val zzz = TextView(context).apply {
        text = "Zzz"
        setTextColor(context.getColor(R.color.secondary))
        textSize = 11f
        visibility = GONE
        alpha = 0f
    }
    private val menu = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        visibility = GONE
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFling = ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat()

    private var frames = PetFrames.of(settings)
    private var index = 0
    private var reacting = false
    private var reactSeq: IntArray = intArrayOf()
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private var suppressDrag = false
    private var peeking = false
    private var napping = false
    private var walking = false
    private var paused = false
    private var cycleIdx = 0
    private var tracker: VelocityTracker? = null
    private val idleMs = 200L
    private val napMs = 500L
    private val reactMs = 90L
    private val cyclePoses = arrayOf(PetPose.HAPPY, PetPose.SHY, PetPose.SLEEP, PetPose.SAD)
    private val chipPet: TextView
    private val chipFeed: TextView
    private val chipSleep: TextView

    private val breath = ObjectAnimator.ofFloat(image, "translationY", 0f, -5.2f).apply {
        duration = 2300
        repeatMode = ObjectAnimator.REVERSE
        repeatCount = ObjectAnimator.INFINITE
        interpolator = PathInterpolator(0.42f, 0f, 0.58f, 1f)
    }

    private val tick = object : Runnable {
        override fun run() {
            if (paused || peeking) return
            if (reacting) {
                if (index >= reactSeq.size) {
                    reacting = false
                    index = 0
                    image.setImageResource(idleFrame())
                    handler.postDelayed(this, if (napping) napMs else idleMs)
                    return
                }
                image.setImageResource(reactSeq[index])
                index += 1
                handler.postDelayed(this, reactMs)
            } else {
                val seq = idleSeq()
                image.setImageResource(seq[index % seq.size])
                index += 1
                handler.postDelayed(this, if (napping) napMs else idleMs)
            }
        }
    }

    private val hideBubble = Runnable {
        speechBubble.animate().alpha(0f).setDuration(220).withEndAction {
            speechBubble.visibility = GONE
            speechBubble.alpha = 1f
            onBubbleChanged?.invoke()
        }.start()
    }

    private val gestures = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (menu.visibility == VISIBLE) {
                    hideMenu()
                    return true
                }
                if (onPeekWake?.invoke() == true) return true
                playReaction(PetPose.JUMP)
                onTap?.invoke()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (onPeekWake?.invoke() == true) return true
                val pose = cycleExpression()
                onExpressionCycle?.invoke(pose)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (dragging || peeking) return
                suppressDrag = true
                showMenu()
                onLongPressAction?.invoke()
            }
        },
    )

    init {
        isClickable = true
        clipToPadding = false
        clipChildren = false
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))
        addView(
            zzz,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply {
                topMargin = dp(18)
                marginEnd = dp(10)
            },
        )
        chipPet = chip("") { hideMenu(); onMenuPet?.invoke() }
        chipFeed = chip("") { hideMenu(); onMenuFeed?.invoke() }
        chipSleep = chip("") { hideMenu(); onMenuSleep?.invoke() }
        menu.addView(chipPet)
        menu.addView(chipFeed)
        menu.addView(chipSleep)
        addView(
            menu,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
                topMargin = dp(28)
            },
        )
        applyCharacter()
    }

    fun applyOutfit() = applyCharacter()

    fun applyCharacter() {
        frames = PetFrames.of(settings)
        image.setImageResource(if (napping) frames.sleep else idleFrame())
        val name = settings.displayName()
        contentDescription = name
        image.contentDescription = name
        refreshMenuLabels()
    }

    fun setWalking(value: Boolean) {
        if (walking == value) return
        walking = value
        restartBreath()
    }

    fun setPeeking(value: Boolean) {
        peeking = value
        if (value) {
            hideMenu()
            pauseAnim()
            image.setImageResource(frames.idle)
        } else if (!paused) {
            resumeAnim()
        }
    }

    fun setNapping(value: Boolean) {
        val was = napping
        napping = value
        if (value) walking = false
        restartBreath()
        index = 0
        if (value && !was) {
            zzz.visibility = if (!peeking) VISIBLE else GONE
            zzz.animate().cancel()
            zzz.alpha = 0f
            zzz.animate().alpha(1f).setDuration(280).start()
            image.animate().cancel()
            image.animate().alpha(0.58f).setDuration(180).withEndAction {
                image.setImageResource(frames.sleep)
                image.animate().alpha(1f).setDuration(260).start()
            }.start()
        } else if (!value && was) {
            zzz.animate().cancel()
            zzz.animate().alpha(0f).setDuration(160).withEndAction {
                zzz.visibility = GONE
                zzz.alpha = 1f
            }.start()
            image.animate().cancel()
            image.animate().alpha(0.58f).setDuration(150).withEndAction {
                image.setImageResource(idleFrame())
                image.animate().alpha(1f).setDuration(220).start()
            }.start()
        } else {
            zzz.animate().cancel()
            zzz.alpha = 1f
            zzz.visibility = if (value && !peeking) VISIBLE else GONE
            image.setImageResource(if (value) frames.sleep else idleFrame())
        }
    }

    fun pauseAnim() {
        paused = true
        handler.removeCallbacks(tick)
        if (breath.isStarted) breath.cancel()
        image.translationY = 0f
    }

    fun resumeAnim() {
        paused = false
        if (peeking) return
        handler.removeCallbacks(tick)
        handler.post(tick)
        restartBreath()
    }

    fun showBubble(text: String, durationMs: Long = 3200L) {
        handler.removeCallbacks(hideBubble)
        speechBubble.animate().cancel()
        speechBubble.alpha = 1f
        speechBubble.text = text
        speechBubble.visibility = VISIBLE
        onBubbleChanged?.invoke()
        handler.postDelayed(hideBubble, durationMs)
    }

    fun hideBubbleNow() {
        handler.removeCallbacks(hideBubble)
        speechBubble.animate().cancel()
        speechBubble.visibility = GONE
        speechBubble.alpha = 1f
        onBubbleChanged?.invoke()
    }

    fun setFacingRight(right: Boolean) {
        val s = if (right) 1f else -1f
        if (abs(image.scaleX - s) < 0.04f) return
        image.animate().cancel()
        image.animate()
            .scaleX(s)
            .setDuration(260L)
            .setInterpolator(PathInterpolator(0.42f, 0f, 0.58f, 1f))
            .start()
    }

    fun playReaction(pose: PetPose) {
        if (peeking) return
        if (napping) setNapping(false)
        reacting = true
        index = 0
        reactSeq = reactionSeq(pose)
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
        animate()
            .scaleX(1.10f)
            .scaleY(1.10f)
            .setDuration(120)
            .setInterpolator(PathInterpolator(0.3f, 0f, 0.4f, 1f))
            .withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(220)
                    .setInterpolator(PathInterpolator(0.4f, 0f, 0.2f, 1f))
                    .start()
            }
            .start()
    }

    fun playRelease() {
        if (peeking) return
        animate().cancel()
        animate()
            .scaleX(1.07f)
            .scaleY(0.96f)
            .setDuration(110)
            .withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }
            .start()
        reacting = true
        index = 0
        reactSeq = reactionSeq(PetPose.JUMP)
    }

    fun cycleExpression(): PetPose {
        val pose = cyclePoses[cycleIdx % cyclePoses.size]
        cycleIdx += 1
        playReaction(pose)
        return pose
    }

    fun hideMenu() {
        if (menu.visibility != VISIBLE) return
        menu.animate().cancel()
        menu.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(140).withEndAction {
            menu.visibility = GONE
            menu.alpha = 1f
            menu.scaleX = 1f
            menu.scaleY = 1f
        }.start()
    }

    fun isMenuOpen(): Boolean = menu.visibility == VISIBLE

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        paused = false
        handler.removeCallbacks(tick)
        handler.post(tick)
        restartBreath()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(hideBubble)
        breath.cancel()
        image.translationY = 0f
        tracker?.recycle()
        tracker = null
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        if (tracker == null) tracker = VelocityTracker.obtain()
        tracker?.addMovement(event)
        if (!dragging) gestures.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                downX = event.rawX
                downY = event.rawY
                dragging = false
                suppressDrag = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (suppressDrag) return true
                val dx = (event.rawX - lastX).toInt()
                val dy = (event.rawY - lastY).toInt()
                if (!dragging && (abs(event.rawX - downX) > slop || abs(event.rawY - downY) > slop)) {
                    dragging = true
                    hideMenu()
                }
                if (dragging && (dx != 0 || dy != 0)) {
                    onDrag?.invoke(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = dragging
                dragging = false
                suppressDrag = false
                if (wasDragging) {
                    tracker?.computeCurrentVelocity(1000)
                    val vx = tracker?.xVelocity ?: 0f
                    val vy = tracker?.yVelocity ?: 0f
                    if (event.actionMasked == MotionEvent.ACTION_UP && hypot(vx, vy) > minFling) {
                        onFling?.invoke(vx, vy)
                    } else {
                        playRelease()
                        onDragEnd?.invoke()
                    }
                }
                tracker?.recycle()
                tracker = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun restartBreath() {
        breath.cancel()
        image.translationY = 0f
        when {
            napping -> {
                breath.setFloatValues(0f, -3.2f)
                breath.duration = 3100
            }
            walking -> {
                breath.setFloatValues(0f, -4.2f)
                breath.duration = 480
            }
            else -> {
                breath.setFloatValues(0f, -5.2f)
                breath.duration = 2300
            }
        }
        if (!paused && !peeking) breath.start()
    }

    private fun refreshMenuLabels() {
        chipPet.text = context.getString(if (settings.isMale) R.string.menu_pet_m else R.string.menu_pet)
        chipFeed.text = context.getString(if (settings.isMale) R.string.menu_feed_m else R.string.menu_feed)
        chipSleep.text = context.getString(if (settings.isMale) R.string.menu_sleep_m else R.string.menu_sleep)
    }

    private fun showMenu() {
        refreshMenuLabels()
        menu.animate().cancel()
        menu.alpha = 0f
        menu.scaleX = 0.85f
        menu.scaleY = 0.85f
        menu.visibility = VISIBLE
        menu.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start()
    }

    private fun idleFrame(): Int {
        return when (PetStats.tier(settings.mood)) {
            MoodTier.SAD -> frames.tilt
            MoodTier.LOW -> frames.blink
            else -> frames.idle
        }
    }

    private fun idleSeq(): IntArray {
        if (napping) {
            return intArrayOf(frames.sleep, frames.sleep, frames.sleep, frames.blink, frames.sleep)
        }
        return when (PetStats.tier(settings.mood)) {
            MoodTier.HAPPY -> intArrayOf(
                frames.idle, frames.idle, frames.idle, frames.blink,
                frames.idle, frames.tapWave, frames.idle, frames.idle, frames.tilt,
            )
            MoodTier.OK -> intArrayOf(
                frames.idle, frames.idle, frames.idle, frames.idle,
                frames.blink, frames.idle, frames.idle, frames.tilt, frames.tilt,
            )
            MoodTier.LOW -> intArrayOf(
                frames.tilt, frames.tilt, frames.idle, frames.blink, frames.tilt, frames.sleep,
            )
            MoodTier.SAD -> intArrayOf(
                frames.tilt, frames.sleep, frames.tilt, frames.blink, frames.tilt,
            )
        }
    }

    private fun reactionSeq(pose: PetPose): IntArray {
        val r = frames.res(pose)
        val wave = frames.tapWave
        val jump = frames.tapJump
        return when (pose) {
            PetPose.JUMP -> intArrayOf(jump, jump, jump, wave, wave, wave, wave)
            PetPose.WAVE, PetPose.HAPPY -> intArrayOf(wave, wave, wave, wave, jump, wave)
            PetPose.SHY -> intArrayOf(frames.shy, frames.shy, frames.shy, frames.shy, frames.shy)
            PetPose.SLEEP -> intArrayOf(frames.sleep, frames.sleep, frames.sleep, frames.sleep)
            PetPose.SAD -> intArrayOf(frames.tilt, frames.sleep, frames.tilt, frames.tilt)
            else -> intArrayOf(r, r, r, wave)
        }
    }

    private fun chip(label: String, click: () -> Unit): TextView {
        val padH = dp(10)
        val padV = dp(6)
        val gap = dp(4)
        return TextView(context).apply {
            text = label
            setPadding(padH, padV, padH, padV)
            setBackgroundResource(R.drawable.bg_menu_chip)
            setTextColor(context.getColor(R.color.text_main))
            textSize = 12f
            setOnClickListener { click() }
            val lp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            lp.marginStart = gap
            lp.marginEnd = gap
            layoutParams = lp
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics,
        ).toInt()
    }
}
