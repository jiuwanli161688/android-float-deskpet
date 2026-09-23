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
import androidx.core.widget.doAfterTextChanged
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.databinding.ActivityMainBinding
import com.floatdeskpet.app.overlay.OverlayService
import com.floatdeskpet.app.overlay.PetFrames
import com.floatdeskpet.app.overlay.PetStats
import com.floatdeskpet.app.util.OverlayPermission

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: PetSettings
    private var pendingStart = false
    private var bindingName = false

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
        settings = PetSettings.get(this)
        if (settings.needsSetup()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPermission.setOnClickListener {
            overlayLauncher.launch(OverlayPermission.settingsIntent(this))
        }
        binding.btnNotif.setOnClickListener { askNotification() }
        binding.btnToggle.setOnClickListener { togglePet() }
        binding.privacyLink.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
        binding.btnChangeCharacter.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java).putExtra(SetupActivity.EXTRA_EDIT, true))
        }

        binding.sliderSize.value = settings.sizeDp.toFloat()
        binding.sliderOpacity.value = settings.opacity.toFloat()
        binding.switchAlways.isChecked = settings.alwaysShow
        binding.switchGhost.isChecked = settings.passThrough
        binding.switchMute.isChecked = settings.muted
        binding.switchTts.isChecked = settings.ttsEnabled
        applyPreview()

        binding.sliderSize.addOnChangeListener { _, value, fromUser ->
            settings.sizeDp = value.toInt()
            applyPreview()
            if (fromUser) bindSizeLabel(value.toInt())
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
        binding.switchMute.setOnCheckedChangeListener { _, checked ->
            settings.muted = checked
        }
        binding.switchTts.setOnCheckedChangeListener { _, checked ->
            settings.ttsEnabled = checked
        }
        binding.genderGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val male = id == R.id.genderMale
            if (settings.isMale == male) return@addOnButtonCheckedListener
            val oldDefault = PetSettings.defaultName(!male)
            settings.gender = if (male) PetSettings.GENDER_MALE else PetSettings.GENDER_FEMALE
            if (settings.petName.isBlank() || settings.petName == oldDefault) {
                settings.petName = ""
                syncNameField()
            }
            if (male && settings.outfit == PetSettings.OUTFIT_PAJAMA) {
                settings.outfit = PetSettings.OUTFIT_CASUAL
            }
            applyPreview()
            refreshHero()
            syncOutfitGroup()
            bindOutfitDesc()
        }
        binding.inputName.doAfterTextChanged {
            if (bindingName) return@doAfterTextChanged
            settings.petName = it?.toString().orEmpty()
            refreshHero()
        }
        syncOutfitGroup()
        binding.outfitGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            settings.outfit = when (id) {
                R.id.outfitPajama -> PetSettings.OUTFIT_PAJAMA
                R.id.outfitHoodie -> PetSettings.OUTFIT_HOODIE
                else -> PetSettings.OUTFIT_CASUAL
            }
            applyPreview()
        }
        refreshHero()
        bindSizeLabel(settings.sizeDp)
        bindOutfitDesc()
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized) return
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
        binding.switchMute.isChecked = settings.muted
        binding.switchTts.isChecked = settings.ttsEnabled
        binding.sliderSize.value = settings.sizeDp.toFloat()
        binding.sliderOpacity.value = settings.opacity.toFloat()
        bindSizeLabel(settings.sizeDp)
        binding.labelOpacity.text = getString(R.string.label_opacity, settings.opacity)
        PetStats.applyDecay(settings)
        binding.statsLine.text = getString(
            R.string.stats_line,
            settings.mood,
            settings.affection,
            settings.feedCount,
        )
        syncGenderGroup()
        syncNameField()
        syncOutfitGroup()
        applyPreview()
        refreshHero()

        when {
            !granted -> {
                binding.status.text = getString(R.string.status_need_perm)
                binding.btnToggle.text = getString(R.string.start_pet)
            }
            settings.running -> {
                binding.status.text = getString(R.string.status_running, settings.displayName())
                binding.btnToggle.text = getString(if (settings.isMale) R.string.stop_pet_m else R.string.stop_pet)
            }
            else -> {
                binding.status.text = getString(R.string.status_stopped)
                binding.btnToggle.text = getString(R.string.start_pet)
            }
        }
    }

    private fun refreshHero() {
        val name = settings.displayName()
        binding.heroTitle.text = getString(R.string.hero_title, name)
        binding.preview.contentDescription = name
    }

    private fun applyPreview() {
        binding.preview.setImageResource(PetFrames.of(settings).idle)
        binding.preview.alpha = settings.opacity / 100f
        val h = (settings.sizeDp * resources.displayMetrics.density).toInt()
        val w = (h * 3) / 4
        val lp = binding.preview.layoutParams
        lp.width = w
        lp.height = h
        binding.preview.layoutParams = lp
    }

    private fun syncGenderGroup() {
        val id = if (settings.isMale) R.id.genderMale else R.id.genderFemale
        if (binding.genderGroup.checkedButtonId != id) {
            binding.genderGroup.check(id)
        }
    }

    private fun syncNameField() {
        binding.nameLayout.hint = PetSettings.defaultName(settings.isMale)
        val current = binding.inputName.text?.toString().orEmpty()
        if (current != settings.petName) {
            bindingName = true
            binding.inputName.setText(settings.petName)
            bindingName = false
        }
    }

    private fun syncOutfitGroup() {
        if (settings.isMale && settings.outfit == PetSettings.OUTFIT_PAJAMA) {
            settings.outfit = PetSettings.OUTFIT_CASUAL
        }
        binding.outfitPajama.visibility = if (settings.isMale) View.GONE else View.VISIBLE
        bindOutfitDesc()
        val id = when (settings.outfit) {
            PetSettings.OUTFIT_PAJAMA -> R.id.outfitPajama
            PetSettings.OUTFIT_HOODIE -> R.id.outfitHoodie
            else -> R.id.outfitCasual
        }
        if (binding.outfitGroup.checkedButtonId != id) {
            binding.outfitGroup.check(id)
        }
    }

    private fun bindSizeLabel(size: Int) {
        val word = when {
            size < 140 -> getString(R.string.size_small)
            size > 184 -> getString(R.string.size_large)
            else -> getString(R.string.size_medium)
        }
        binding.labelSize.text = getString(R.string.label_size, word)
    }

    private fun bindOutfitDesc() {
        binding.outfitDesc.text = getString(
            if (settings.isMale) R.string.outfit_desc_m else R.string.outfit_desc_f,
        )
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
