package com.floatdeskpet.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.floatdeskpet.app.R
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.data.CharacterStyle
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.databinding.ActivityMainBinding
import android.app.Dialog
import com.floatdeskpet.app.data.FoodCatalog
import com.floatdeskpet.app.overlay.MoodTier
import com.floatdeskpet.app.overlay.OverlayService
import com.floatdeskpet.app.overlay.OverlayVisibility
import com.floatdeskpet.app.overlay.PetDialogue
import com.floatdeskpet.app.overlay.PetFrames
import com.floatdeskpet.app.overlay.PetPose
import com.floatdeskpet.app.overlay.PetSfx
import com.floatdeskpet.app.overlay.PetStats
import com.floatdeskpet.app.util.OverlayPermission
import com.google.android.material.chip.Chip

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: PetSettings
    private lateinit var auth: AuthStore
    private var pendingStart = false
    private var bindingName = false
    private var panelOpen = false
    private var styleGuard = false
    private lateinit var sfx: PetSfx

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

    private val backToClosePanel = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            closePanel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = PetSettings.get(this)
        auth = AuthStore.get(this)
        if (!AppFlow.enter(this)) return
        OverlayVisibility.homeFront = true
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sfx = PetSfx(this)
        onBackPressedDispatcher.addCallback(this, backToClosePanel)

        binding.profileChip.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.fabMenu.setOnClickListener { openPanel() }
        binding.scrim.setOnClickListener { closePanel() }
        binding.panel.btnClosePanel.setOnClickListener { closePanel() }
        binding.panel.btnProfile.setOnClickListener {
            closePanel()
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.btnPermission.setOnClickListener { requestOverlay() }
        binding.btnNotif.setOnClickListener { askNotification() }
        binding.panel.btnPermission.setOnClickListener { requestOverlay() }
        binding.panel.btnNotif.setOnClickListener { askNotification() }
        binding.panel.btnToggle.setOnClickListener { togglePet() }
        binding.panel.privacyLink.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
        binding.panel.btnChangeCharacter.setOnClickListener {
            closePanel()
            startActivity(Intent(this, SetupActivity::class.java).putExtra(SetupActivity.EXTRA_EDIT, true))
        }
        binding.panel.btnFeed.setOnClickListener { openFeedPanel() }
        binding.panel.btnCardio.setOnClickListener { startCardio() }

        binding.panel.sliderSize.value = settings.sizeDp.toFloat()
        binding.panel.sliderOpacity.value = settings.opacity.toFloat()
        binding.panel.switchAlways.isChecked = settings.alwaysShow
        binding.panel.switchGhost.isChecked = settings.passThrough
        binding.panel.switchMute.isChecked = settings.muted
        binding.panel.switchTts.isChecked = settings.ttsEnabled
        applyHomeArt()
        bindHomePet()

        binding.panel.sliderSize.addOnChangeListener { _, value, fromUser ->
            settings.sizeDp = value.toInt()
            if (fromUser) bindSizeLabel(value.toInt())
        }
        binding.panel.sliderOpacity.addOnChangeListener { _, value, fromUser ->
            settings.opacity = value.toInt()
            if (fromUser) binding.panel.labelOpacity.text = getString(R.string.label_opacity, value.toInt())
        }
        binding.panel.switchAlways.setOnCheckedChangeListener { _, checked ->
            settings.alwaysShow = checked
        }
        binding.panel.switchGhost.setOnCheckedChangeListener { _, checked ->
            settings.passThrough = checked
        }
        binding.panel.switchMute.setOnCheckedChangeListener { _, checked ->
            settings.muted = checked
        }
        binding.panel.switchTts.setOnCheckedChangeListener { _, checked ->
            settings.ttsEnabled = checked
        }
        binding.panel.genderGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val male = id == R.id.genderMale
            if (settings.isMale == male) return@addOnButtonCheckedListener
            settings.applyGender(male)
            syncNameField()
            applyHomeArt()
            refreshHero()
            syncOutfitGroup()
            rebuildStyleChips()
            bindOutfitDesc()
            bindStyleBlurb()
        }
        binding.panel.inputName.doAfterTextChanged {
            if (bindingName) return@doAfterTextChanged
            settings.petName = it?.toString().orEmpty()
            refreshHero()
        }
        syncOutfitGroup()
        binding.panel.outfitGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            settings.outfit = when (id) {
                R.id.outfitPajama -> PetSettings.OUTFIT_PAJAMA
                R.id.outfitHoodie -> PetSettings.OUTFIT_HOODIE
                else -> PetSettings.OUTFIT_CASUAL
            }
            applyHomeArt()
        }
        rebuildStyleChips()
        refreshHero()
        bindSizeLabel(settings.sizeDp)
        bindOutfitDesc()
        bindStyleBlurb()
        insetFab()
        binding.panel.root.post {
            if (!panelOpen) {
                binding.panel.root.translationX = binding.panel.root.width.toFloat().coerceAtLeast(1f)
                parkPanel()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        OverlayVisibility.homeFront = true
        OverlayService.syncVisibility(this)
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized) return
        if (!AppFlow.enter(this)) return
        OverlayVisibility.homeFront = true
        if (settings.alwaysShow && settings.running && OverlayPermission.granted(this)) {
            OverlayService.start(this)
        }
        OverlayService.syncVisibility(this)
        refresh()
    }

    override fun onStop() {
        OverlayVisibility.homeFront = false
        OverlayService.syncVisibility(this)
        super.onStop()
    }

    override fun onDestroy() {
        if (::sfx.isInitialized) sfx.release()
        super.onDestroy()
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
            requestOverlay()
            return
        }
        askNotification()
        OverlayService.start(this)
        refresh()
    }

    private fun refresh() {
        val granted = OverlayPermission.granted(this)
        binding.permCard.visibility = if (granted && hasNotif()) View.GONE else View.VISIBLE
        binding.panel.permCard.visibility = if (granted && hasNotif()) View.GONE else View.VISIBLE
        binding.btnPermission.visibility = if (granted) View.GONE else View.VISIBLE
        binding.panel.btnPermission.visibility = if (granted) View.GONE else View.VISIBLE
        binding.btnNotif.visibility = if (granted && !hasNotif()) View.VISIBLE else View.GONE
        binding.panel.btnNotif.visibility = if (granted && !hasNotif()) View.VISIBLE else View.GONE
        binding.notifHint.visibility = binding.btnNotif.visibility
        binding.panel.notifHint.visibility = binding.panel.btnNotif.visibility
        binding.panel.switchAlways.isChecked = settings.alwaysShow
        binding.panel.switchGhost.isChecked = settings.passThrough
        binding.panel.switchMute.isChecked = settings.muted
        binding.panel.switchTts.isChecked = settings.ttsEnabled
        binding.panel.sliderSize.value = settings.sizeDp.toFloat()
        binding.panel.sliderOpacity.value = settings.opacity.toFloat()
        bindSizeLabel(settings.sizeDp)
        binding.panel.labelOpacity.text = getString(R.string.label_opacity, settings.opacity)
        PetStats.applyDecay(settings)
        binding.panel.statsLine.text = getString(
            R.string.stats_line,
            settings.mood,
            settings.affection,
            auth.happiness(),
        )
        syncGenderGroup()
        syncNameField()
        syncOutfitGroup()
        applyHomeArt()
        refreshHero()
        bindUserChip()
        bindStyleBlurb()

        val name = settings.displayName()
        when {
            !granted -> {
                binding.status.text = getString(R.string.status_need_perm)
                binding.panel.status.text = getString(R.string.status_need_perm)
                binding.panel.btnToggle.text = getString(R.string.start_pet)
            }
            settings.running -> {
                binding.status.text = getString(R.string.status_running, name)
                binding.panel.status.text = getString(R.string.status_running, name)
                binding.panel.btnToggle.text = getString(if (settings.isMale) R.string.stop_pet_m else R.string.stop_pet)
            }
            else -> {
                binding.status.text = getString(R.string.status_stopped)
                binding.panel.status.text = getString(R.string.status_stopped)
                binding.panel.btnToggle.text = getString(R.string.start_pet)
            }
        }
        bindMoodLine()
    }

    private fun refreshHero() {
        val name = settings.displayName()
        binding.heroTitle.text = getString(R.string.hero_title, name)
        binding.preview.contentDescription = name
    }

    private fun bindHomePet() {
        val pet = binding.preview
        pet.dragEnabled = false
        pet.embedBubble()
        pet.onTap = {
            PetStats.onTap(settings)
            sfx.tap()
            pet.showBubble(PetDialogue.tap(settings))
            bindMoodLine()
        }
        pet.onExpressionCycle = { pose ->
            PetStats.onCycle(settings)
            sfx.tap()
            pet.showBubble(PetDialogue.pose(settings, pose))
            bindMoodLine()
        }
        pet.onLongPressAction = { sfx.menu() }
        pet.onMenuPet = {
            PetStats.onPet(settings)
            auth.addHappiness(3)
            pet.playReaction(PetPose.SHY)
            pet.showBubble(PetDialogue.pet(settings))
            sfx.tap()
            refresh()
        }
        pet.onMenuFeed = { openFeedPanel() }
        pet.onMenuCardio = { startCardio() }
        pet.onMenuSleep = {
            PetStats.onSleep(settings)
            pet.setNapping(true)
            pet.showBubble(PetDialogue.sleep(settings))
            refresh()
        }
    }

    private fun openFeedPanel() {
        closePanel()
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val panel = FeedPanel(
            this,
            onFed = { item ->
                dialog.dismiss()
                PetStats.onFed(settings, item.moodBoost)
                binding.preview.setNapping(false)
                binding.preview.playReaction(PetPose.HAPPY)
                binding.preview.showBubble(PetDialogue.feed(settings, item.name))
                sfx.tap()
                refresh()
            },
            onClose = { dialog.dismiss() },
        )
        dialog.setContentView(panel.root)
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun startCardio() {
        closePanel()
        binding.preview.playCardio(4500L) {
            val kcal = FoodCatalog.cardioKcal()
            PetStats.onCardio(settings)
            binding.preview.showBubble(PetDialogue.cardio(settings, kcal), 4200L)
            sfx.tap()
            refresh()
        }
    }

    private fun applyHomeArt() {
        binding.preview.applyCharacter()
        val frames = PetFrames.of(settings)
        binding.panel.stylePreview.setImageResource(frames.idle)
        CharacterStyle.tint(binding.panel.stylePreview, settings)
    }

    private fun bindUserChip() {
        val user = auth.current() ?: return
        binding.profileChip.text = user.nickname
    }

    private fun bindMoodLine() {
        val res = when (PetStats.tier(settings.mood)) {
            MoodTier.HAPPY -> R.string.home_mood_happy
            MoodTier.OK -> R.string.home_mood_ok
            MoodTier.LOW -> R.string.home_mood_low
            MoodTier.SAD -> R.string.home_mood_sad
        }
        binding.moodLine.text = getString(res, settings.displayName())
    }

    private fun syncGenderGroup() {
        val id = if (settings.isMale) R.id.genderMale else R.id.genderFemale
        if (binding.panel.genderGroup.checkedButtonId != id) {
            binding.panel.genderGroup.check(id)
        }
    }

    private fun syncNameField() {
        binding.panel.nameLayout.hint = getString(R.string.setup_name_hint)
        val current = binding.panel.inputName.text?.toString().orEmpty()
        if (current != settings.petName) {
            bindingName = true
            binding.panel.inputName.setText(settings.petName)
            bindingName = false
        }
    }

    private fun syncOutfitGroup() {
        if (settings.isMale && settings.outfit == PetSettings.OUTFIT_PAJAMA) {
            settings.outfit = PetSettings.OUTFIT_CASUAL
        }
        binding.panel.outfitPajama.visibility = if (settings.isMale) View.GONE else View.VISIBLE
        bindOutfitDesc()
        val id = when (settings.outfit) {
            PetSettings.OUTFIT_PAJAMA -> R.id.outfitPajama
            PetSettings.OUTFIT_HOODIE -> R.id.outfitHoodie
            else -> R.id.outfitCasual
        }
        if (binding.panel.outfitGroup.checkedButtonId != id) {
            binding.panel.outfitGroup.check(id)
        }
    }

    private fun rebuildStyleChips() {
        val group = binding.panel.styleGroup
        styleGuard = true
        group.removeAllViews()
        val current = settings.resolvedStyle()
        CharacterStyle.all(settings.isMale).forEach { style ->
            val chip = Chip(this, null, com.google.android.material.R.attr.chipStyle).apply {
                text = getString(style.titleRes())
                isCheckable = true
                isChecked = style == current
                tag = style.id
                setOnClickListener {
                    if (styleGuard) return@setOnClickListener
                    settings.applyStyle(style)
                    applyHomeArt()
                    syncOutfitGroup()
                    bindStyleBlurb()
                }
            }
            group.addView(chip)
        }
        styleGuard = false
    }

    private fun bindStyleBlurb() {
        binding.panel.styleBlurb.text = getString(settings.resolvedStyle().blurbRes())
    }

    private fun bindSizeLabel(size: Int) {
        val word = when {
            size < 140 -> getString(R.string.size_small)
            size > 184 -> getString(R.string.size_large)
            else -> getString(R.string.size_medium)
        }
        binding.panel.labelSize.text = getString(R.string.label_size, word)
    }

    private fun bindOutfitDesc() {
        binding.panel.outfitDesc.text = getString(
            when {
                settings.resolvedStyle().hasDedicatedArt -> R.string.outfit_desc_style
                settings.isMale -> R.string.outfit_desc_m
                else -> R.string.outfit_desc_f
            },
        )
    }

    private fun requestOverlay() {
        OverlayPermission.open(this, overlayLauncher)
    }

    private fun parkPanel() {
        binding.panel.root.visibility = View.GONE
        binding.panel.root.isClickable = false
        binding.panel.root.isFocusable = false
    }

    private fun openPanel() {
        panelOpen = true
        binding.scrim.visibility = View.VISIBLE
        binding.scrim.alpha = 0f
        binding.scrim.animate().alpha(1f).setDuration(220).start()
        binding.panel.root.isClickable = true
        binding.panel.root.isFocusable = true
        binding.panel.root.visibility = View.VISIBLE
        binding.panel.root.animate().translationX(0f).setDuration(280).start()
        backToClosePanel.isEnabled = true
    }

    private fun closePanel() {
        if (!panelOpen) return
        panelOpen = false
        backToClosePanel.isEnabled = false
        val width = binding.panel.root.width.toFloat().coerceAtLeast(1f)
        binding.scrim.animate().alpha(0f).setDuration(200).withEndAction {
            binding.scrim.visibility = View.GONE
        }.start()
        binding.panel.root.animate().translationX(width).setDuration(240).withEndAction {
            if (!panelOpen) parkPanel()
        }.start()
    }

    private fun insetFab() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabMenu) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extra = (20 * resources.displayMetrics.density).toInt()
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + extra
                rightMargin = bars.right + extra
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.fabMenu)
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
