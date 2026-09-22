package com.floatdeskpet.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.floatdeskpet.app.databinding.ActivityPrivacyBinding

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}
