package com.floatdeskpet.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.databinding.ActivityMainBinding
import com.floatdeskpet.app.overlay.OverlayService
import com.floatdeskpet.app.util.OverlayPermission

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: PetSettings
    private var pendingStart = false

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refresh()
        if (pendingStart && OverlayPermission.granted(this)) {
            pendingStart = false
            OverlayService.start(this)
            refresh()
        }
    }

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = PetSettings.get(this)

        binding.btnPermission.setOnClickListener {
            overlayLauncher.launch(OverlayPermission.settingsIntent(this))
        }
        binding.btnNotif.setOnClickListener { askNotification() }
        binding.btnToggle.setOnClickListener { togglePet() }
        binding.privacyLink.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }

        binding.sliderSize.value = settings.sizeDp.toFloat()
        binding.sliderOpacity.value = settings.opacity.toFloat()
        binding.switchAlways.isChecked = settings.alwaysShow
        binding.switchGhost.isChecked = settings.passThrough
        applyPreview()

        binding.sliderSize.addOnChangeListener { _, value, fromUser ->
            settings.sizeDp = value.toInt()
            applyPreview()
            if (fromUser) binding.labelSize.text = getString(R.string.label_size, value.toInt())
        }
        binding.sliderOpacity.addOnChangeListener { _, value, fromUser ->
            settings.opacity = value.toInt()
            applyPreview()
            if (fromUser) binding.labelOpacity.text = getString(R.string.label_opacity, value.toInt())
        }
        binding.switchAlways.setOnCheckedChangeListener { _, checked ->
            settings.alwaysShow = checked
        }
        binding.switchGhost.setOnCheckedChangeListener { _, checked ->
            settings.passThrough = checked
        }
    }

    override fun onResume() {
        super.onResume()
        if (settings.alwaysShow && settings.running && OverlayPermission.granted(this)) {
            OverlayService.start(this)
        }
        refresh()
    }

    private fun togglePet() {
        if (settings.running) {
            OverlayService.stop(this)
            refresh()
            return
        }
        if (!OverlayPermission.granted(this)) {
            pendingStart = true
            OverlayPermission.toastNeed(this)
            overlayLauncher.launch(OverlayPermission.settingsIntent(this))
            return
        }
        askNotification()
        OverlayService.start(this)
        refresh()
    }

    private fun refresh() {
        val granted = OverlayPermission.granted(this)
        binding.permCard.visibility = if (granted && hasNotif()) View.GONE else View.VISIBLE
        binding.btnPermission.visibility = if (granted) View.GONE else View.VISIBLE
        binding.btnNotif.visibility = if (granted && !hasNotif()) View.VISIBLE else View.GONE
        binding.notifHint.visibility = binding.btnNotif.visibility
        binding.switchAlways.isChecked = settings.alwaysShow
        binding.switchGhost.isChecked = settings.passThrough
        binding.sliderSize.value = settings.sizeDp.toFloat()
        binding.sliderOpacity.value = settings.opacity.toFloat()
        binding.labelSize.text = getString(R.string.label_size, settings.sizeDp)
        binding.labelOpacity.text = getString(R.string.label_opacity, settings.opacity)
        applyPreview()

        when {
            !granted -> {
                binding.status.text = getString(R.string.status_need_perm)
                binding.btnToggle.text = getString(R.string.start_pet)
            }
            settings.running -> {
                binding.status.text = getString(R.string.status_running)
                binding.btnToggle.text = getString(R.string.stop_pet)
            }
            else -> {
                binding.status.text = getString(R.string.status_stopped)
                binding.btnToggle.text = getString(R.string.start_pet)
            }
        }
    }

    private fun applyPreview() {
        binding.preview.alpha = settings.opacity / 100f
        val h = (settings.sizeDp * resources.displayMetrics.density).toInt()
        val w = (h * 3) / 4
        val lp = binding.preview.layoutParams
        lp.width = w
        lp.height = h
        binding.preview.layoutParams = lp
    }

    private fun hasNotif(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun askNotification() {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotif()) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
