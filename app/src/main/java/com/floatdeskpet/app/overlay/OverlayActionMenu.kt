package com.floatdeskpet.app.overlay

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.PetSettings

class OverlayActionMenu(
    context: Context,
    private val onPet: () -> Unit,
    private val onFeed: () -> Unit,
    private val onCardio: () -> Unit,
    private val onSleep: () -> Unit,
    private val onHome: () -> Unit,
    private val onDismiss: () -> Unit,
) {
    val root: View = LayoutInflater.from(context).inflate(R.layout.overlay_action_menu, null, false)
    private val settings = PetSettings.get(context)
    private val itemPet = root.findViewById<View>(R.id.itemPet)
    private val itemFeed = root.findViewById<View>(R.id.itemFeed)
    private val itemCardio = root.findViewById<View>(R.id.itemCardio)
    private val itemSleep = root.findViewById<View>(R.id.itemSleep)
    private val itemHome = root.findViewById<View>(R.id.itemHome)
    private val labelPet = root.findViewById<TextView>(R.id.labelPet)
    private val labelFeed = root.findViewById<TextView>(R.id.labelFeed)
    private val labelCardio = root.findViewById<TextView>(R.id.labelCardio)
    private val labelSleep = root.findViewById<TextView>(R.id.labelSleep)
    private val labelHome = root.findViewById<TextView>(R.id.labelHome)

    init {
        itemPet.setOnClickListener { onPet() }
        itemFeed.setOnClickListener { onFeed() }
        itemCardio.setOnClickListener { onCardio() }
        itemSleep.setOnClickListener { onSleep() }
        itemHome.setOnClickListener { onHome() }
        root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                onDismiss()
                true
            } else {
                false
            }
        }
        refresh()
    }

    fun refresh() {
        val male = settings.isMale
        labelPet.text = root.context.getString(if (male) R.string.menu_pet_m else R.string.menu_pet)
        labelFeed.text = root.context.getString(if (male) R.string.menu_feed_m else R.string.menu_feed)
        labelCardio.text = root.context.getString(R.string.action_cardio)
        labelSleep.text = root.context.getString(if (male) R.string.menu_sleep_m else R.string.menu_sleep)
        labelHome.text = root.context.getString(R.string.menu_home)
    }
}
