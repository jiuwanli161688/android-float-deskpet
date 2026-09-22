package com.floatdeskpet.app.overlay

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.floatdeskpet.app.R
import kotlin.math.abs

class PetSpriteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var onDrag: ((dx: Int, dy: Int) -> Unit)? = null
    var onDragEnd: (() -> Unit)? = null
    var onTap: (() -> Unit)? = null

    private val image = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
    }
    private val handler = Handler(Looper.getMainLooper())
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private val idleHold = intArrayOf(
        R.drawable.pet_idle_0, R.drawable.pet_idle_0, R.drawable.pet_idle_0,
        R.drawable.pet_idle_0, R.drawable.pet_idle_0, R.drawable.pet_idle_0,
        R.drawable.pet_idle_1,
        R.drawable.pet_idle_0, R.drawable.pet_idle_0, R.drawable.pet_idle_0,
        R.drawable.pet_idle_0,
        R.drawable.pet_idle_2, R.drawable.pet_idle_2, R.drawable.pet_idle_2,
        R.drawable.pet_idle_2,
        R.drawable.pet_idle_0, R.drawable.pet_idle_0,
        R.drawable.pet_idle_1,
    )
    private val tapSeq = intArrayOf(
        R.drawable.pet_tap_0, R.drawable.pet_tap_0, R.drawable.pet_tap_0,
        R.drawable.pet_tap_1, R.drawable.pet_tap_1, R.drawable.pet_tap_1, R.drawable.pet_tap_1,
    )

    private var index = 0
    private var reacting = false
    private var lastX = 0f
    private var lastY = 0f
    private var downX = 0f
    private var downY = 0f
    private var dragging = false
    private val idleMs = 170L
    private val reactMs = 80L

    private val breath = ObjectAnimator.ofFloat(this, "translationY", 0f, -8f).apply {
        duration = 1800
        repeatMode = ObjectAnimator.REVERSE
        repeatCount = ObjectAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
    }

    private val tick = object : Runnable {
        override fun run() {
            if (reacting) {
                if (index >= tapSeq.size) {
                    reacting = false
                    index = 0
                    image.setImageResource(idleHold[0])
                    handler.postDelayed(this, idleMs)
                    return
                }
                image.setImageResource(tapSeq[index])
                index += 1
                handler.postDelayed(this, reactMs)
            } else {
                image.setImageResource(idleHold[index % idleHold.size])
                index += 1
                handler.postDelayed(this, idleMs)
            }
        }
    }

    init {
        isClickable = true
        clipToPadding = false
        clipChildren = false
        addView(
            image,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER),
        )
        image.setImageResource(R.drawable.pet_idle_0)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.removeCallbacks(tick)
        handler.post(tick)
        if (!breath.isStarted) breath.start()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        breath.cancel()
        translationY = 0f
        super.onDetachedFromWindow()
    }

    fun playReaction() {
        reacting = true
        index = 0
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
        animate()
            .scaleX(1.12f)
            .scaleY(1.12f)
            .setDuration(90)
            .withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(180).start()
            }
            .start()
    }

    fun playRelease() {
        animate().cancel()
        animate()
            .scaleX(1.08f)
            .scaleY(0.94f)
            .setDuration(80)
            .withEndAction {
                animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            .start()
        reacting = true
        index = 0
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                downX = event.rawX
                downY = event.rawY
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastX).toInt()
                val dy = (event.rawY - lastY).toInt()
                if (!dragging && (abs(event.rawX - downX) > slop || abs(event.rawY - downY) > slop)) {
                    dragging = true
                }
                if (dragging && (dx != 0 || dy != 0)) {
                    onDrag?.invoke(dx, dy)
                    lastX = event.rawX
                    lastY = event.rawY
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    playRelease()
                    onDragEnd?.invoke()
                } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                    playReaction()
                    onTap?.invoke()
                }
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
