package com.floatdeskpet.app.overlay

import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.floatdeskpet.app.R
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.data.FoodCatalog
import com.floatdeskpet.app.data.FoodItem
import com.floatdeskpet.app.data.FoodKind

class OverlayFeedPanel(
    raw: Context,
    private val onFed: (FoodItem) -> Unit,
    private val onClose: () -> Unit,
) {
    private val context = raw.applicationContext
    private val auth = AuthStore.get(context)
    private var kind = FoodKind.SNACK
    private var picked: FoodItem? = null
    private var lastConfirmAt = 0L
    private val density = context.resources.displayMetrics.density

    private val hint: TextView
    private val balance: TextView
    private val grid: GridLayout
    private val tabs: LinearLayout
    val root: View

    init {
        val scrim = View(context).apply {
            setBackgroundColor(0x66000000)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClose() }
        }
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_card)
            val pad = dp(18)
            setPadding(pad, pad, pad, pad)
        }
        card.addView(TextView(context).apply {
            text = context.getString(R.string.feed_title)
            setTextColor(context.getColor(R.color.text_main))
            textSize = 22f
            paint.isFakeBoldText = true
        })
        balance = TextView(context).apply {
            setTextColor(context.getColor(R.color.text_muted))
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
        }
        card.addView(balance)
        tabs = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        card.addView(tabs)
        grid = GridLayout(context).apply {
            columnCount = 3
            setPadding(0, dp(8), 0, 0)
        }
        card.addView(grid)
        hint = TextView(context).apply {
            setTextColor(context.getColor(R.color.text_muted))
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        }
        card.addView(hint)
        val confirm = TextView(context).apply {
            text = context.getString(R.string.feed_confirm)
            gravity = Gravity.CENTER
            minHeight = dp(52)
            isClickable = true
            isFocusable = true
            setBackgroundColor(context.getColor(R.color.primary))
            setTextColor(Color.WHITE)
            textSize = 16f
            paint.isFakeBoldText = true
            setPadding(0, dp(12), 0, dp(12))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(10)
            layoutParams = lp
            setOnClickListener { confirm() }
        }
        card.addView(confirm)
        val close = TextView(context).apply {
            text = context.getString(R.string.feed_close)
            gravity = Gravity.CENTER
            minHeight = dp(48)
            isClickable = true
            isFocusable = true
            setTextColor(context.getColor(R.color.primary))
            textSize = 15f
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(4)
            layoutParams = lp
            setOnClickListener { onClose() }
        }
        card.addView(close)
        val scroll = ScrollView(context).apply {
            isFillViewport = true
            isClickable = true
            addView(
                card,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ).apply {
                    leftMargin = dp(22)
                    rightMargin = dp(22)
                    topMargin = dp(24)
                    bottomMargin = dp(24)
                },
            )
        }
        root = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
            addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        FoodKind.entries.forEach { tab ->
            tabs.addView(tabChip(tab))
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

    private fun tabChip(tab: FoodKind): TextView {
        return TextView(context).apply {
            text = tab.title
            gravity = Gravity.CENTER
            minHeight = dp(40)
            isClickable = true
            isFocusable = true
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setBackgroundResource(if (tab == kind) R.drawable.bg_card_selected else R.drawable.bg_food_cell)
            setTextColor(context.getColor(R.color.text_main))
            textSize = 13f
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginEnd = dp(4)
            layoutParams = lp
            setOnClickListener {
                kind = tab
                picked = null
                refreshTabs()
                renderGrid()
                bindHint()
            }
        }
    }

    private fun refreshTabs() {
        tabs.removeAllViews()
        FoodKind.entries.forEach { tabs.addView(tabChip(it)) }
    }

    private fun bindBalance() {
        balance.text = context.getString(R.string.feed_balance, auth.happiness())
    }

    private fun bindHint() {
        val item = picked
        hint.text = when {
            item == null -> context.getString(R.string.feed_hint_pick)
            FoodCatalog.canAfford(auth.happiness(), item.cost) ->
                context.getString(R.string.feed_hint_ready, item.name, item.cost)
            else -> context.getString(R.string.feed_hint_short, item.name)
        }
    }

    private fun renderGrid() {
        grid.removeAllViews()
        FoodCatalog.ofKind(kind).forEach { food ->
            grid.addView(
                cell(food),
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                },
            )
        }
    }

    private fun cell(food: FoodItem): View {
        val selected = picked?.id == food.id
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            minimumHeight = dp(88)
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
        if (now - lastConfirmAt < 280L) return
        lastConfirmAt = now
        val item = picked
        if (item == null) {
            hint.text = context.getString(R.string.feed_hint_pick)
            return
        }
        if (!auth.spendHappiness(item.cost)) {
            hint.text = context.getString(R.string.feed_hint_short, item.name)
            bindBalance()
            return
        }
        onFed(item)
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}
