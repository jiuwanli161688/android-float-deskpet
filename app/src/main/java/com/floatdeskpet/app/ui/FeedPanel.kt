package com.floatdeskpet.app.ui

import android.content.Context
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.floatdeskpet.app.R
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.data.FoodCatalog
import com.floatdeskpet.app.data.FoodItem
import com.floatdeskpet.app.data.FoodKind
import com.floatdeskpet.app.databinding.PanelFeedBinding
import com.google.android.material.chip.Chip

class FeedPanel(
    raw: Context,
    private val onFed: (FoodItem) -> Unit,
    private val onClose: () -> Unit,
) {
    private val context: Context = ContextThemeWrapper(raw, R.style.Theme_FloatDeskPet)
    val root: View
    private val binding: PanelFeedBinding
    private val auth = AuthStore.get(raw)
    private var kind = FoodKind.SNACK
    private var picked: FoodItem? = null
    private var lastConfirmAt = 0L

    init {
        binding = PanelFeedBinding.inflate(LayoutInflater.from(context))
        root = binding.root
        binding.feedCard.isClickable = true
        binding.feedScrim.setOnClickListener { onClose() }
        binding.btnFeedClose.setOnClickListener { onClose() }
        binding.btnFeedConfirm.setOnClickListener { confirm() }
        FoodKind.entries.forEach { tab ->
            val chip = Chip(context, null, com.google.android.material.R.attr.chipStyle).apply {
                text = tab.title
                isCheckable = true
                isChecked = tab == kind
                setOnClickListener {
                    kind = tab
                    picked = null
                    renderGrid()
                    bindHint()
                }
            }
            binding.feedTabs.addView(chip)
        }
        renderGrid()
        bindBalance()
        bindHint()
    }

    fun refresh() {
        bindBalance()
        bindHint()
        renderGrid()
    }

    private fun bindBalance() {
        binding.feedBalance.text = context.getString(R.string.feed_balance, auth.happiness())
    }

    private fun bindHint() {
        val item = picked
        binding.feedHint.text = when {
            item == null -> context.getString(R.string.feed_hint_pick)
            FoodCatalog.canAfford(auth.happiness(), item.cost) ->
                context.getString(R.string.feed_hint_ready, item.name, item.cost)
            else -> context.getString(R.string.feed_hint_short, item.name)
        }
    }

    private fun renderGrid() {
        val grid = binding.feedGrid
        grid.removeAllViews()
        val foods = FoodCatalog.ofKind(kind)
        foods.forEach { food ->
            grid.addView(cell(food), GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
    }

    private fun cell(food: FoodItem): View {
        val selected = picked?.id == food.id
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setBackgroundResource(if (selected) R.drawable.bg_card_selected else R.drawable.bg_food_cell)
            val pad = dp(8)
            setPadding(pad, dp(10), pad, dp(10))
            addView(TextView(context).apply {
                text = food.icon
                textSize = 26f
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = food.name
                setTextColor(context.getColor(R.color.text_main))
                textSize = 12f
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = context.getString(R.string.feed_cost, food.cost)
                setTextColor(context.getColor(R.color.primary))
                textSize = 11f
                gravity = Gravity.CENTER
            })
            setOnClickListener {
                picked = food
                renderGrid()
                bindHint()
            }
        }
    }

    private fun confirm() {
        val now = SystemClock.uptimeMillis()
        if (now - lastConfirmAt < 360L) return
        lastConfirmAt = now
        val item = picked
        if (item == null) {
            binding.feedHint.text = context.getString(R.string.feed_hint_pick)
            return
        }
        if (!auth.spendHappiness(item.cost)) {
            binding.feedHint.text = context.getString(R.string.feed_hint_short, item.name)
            bindBalance()
            return
        }
        onFed(item)
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}
