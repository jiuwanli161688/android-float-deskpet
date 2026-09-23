package com.floatdeskpet.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.GuidePage
import com.floatdeskpet.app.data.GuidePages
import com.floatdeskpet.app.data.GuideStore
import com.floatdeskpet.app.databinding.ActivityGuideBinding
import com.floatdeskpet.app.databinding.ItemGuidePageBinding

class GuideActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGuideBinding
    private lateinit var guide: GuideStore
    private val pages = GuidePages.all

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        guide = GuideStore.get(this)
        if (!AppFlow.enter(this)) return
        binding = ActivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pager.adapter = GuidePagerAdapter(pages)
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                syncChrome(position)
            }
        })
        buildDots()
        binding.btnSkip.setOnClickListener { finishGuide() }
        binding.btnNext.setOnClickListener {
            val last = pages.lastIndex
            if (binding.pager.currentItem < last) {
                binding.pager.currentItem = binding.pager.currentItem + 1
            } else {
                finishGuide()
            }
        }
        syncChrome(0)
    }

    private fun finishGuide() {
        guide.completed = true
        AppFlow.route(this)
    }

    private fun buildDots() {
        binding.dots.removeAllViews()
        val gap = (7 * resources.displayMetrics.density).toInt()
        pages.forEachIndexed { index, _ ->
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.bg_guide_dot)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                val lp = android.widget.LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                if (index > 0) lp.marginStart = gap
                layoutParams = lp
            }
            binding.dots.addView(dot)
        }
    }

    private fun syncChrome(position: Int) {
        val last = position >= pages.lastIndex
        binding.btnSkip.visibility = if (last) View.INVISIBLE else View.VISIBLE
        binding.btnNext.text = getString(if (last) R.string.guide_start else R.string.guide_next)
        binding.btnNext.contentDescription = getString(
            if (last) R.string.guide_start_desc else R.string.guide_next_desc,
        )
        for (i in 0 until binding.dots.childCount) {
            binding.dots.getChildAt(i).isSelected = i == position
        }
    }

    private class GuidePagerAdapter(
        private val pages: List<GuidePage>,
    ) : RecyclerView.Adapter<GuidePagerAdapter.Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemGuidePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(pages[position])
        }

        override fun getItemCount(): Int = pages.size

        class Holder(private val binding: ItemGuidePageBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(page: GuidePage) {
                binding.art.setImageResource(page.artRes)
                binding.art.contentDescription = binding.root.context.getString(page.artDescRes)
                binding.title.text = binding.root.context.getString(page.titleRes)
                binding.body.text = binding.root.context.getString(page.bodyRes)
            }
        }
    }
}
