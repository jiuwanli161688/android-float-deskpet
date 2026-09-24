package com.floatdeskpet.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import com.floatdeskpet.app.data.BondStage
import com.floatdeskpet.app.data.CharacterStyle
import com.floatdeskpet.app.data.AlbumStore
import com.floatdeskpet.app.data.CompanionBond
import com.floatdeskpet.app.data.CompanionDay
import com.floatdeskpet.app.data.CompanionMissYou
import com.floatdeskpet.app.data.CompanionSchedule
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.databinding.ActivityMainBinding
import android.app.Dialog
import com.floatdeskpet.app.data.FoodCatalog
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
    private var hubSheetOpen = false
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
            if (panelOpen) closePanel() else closeHubSheet()
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
        binding.fabMenu.setOnClickListener { toggleHubSheet() }
        binding.fabMenu.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
        binding.scrim.setOnClickListener {
            if (panelOpen) closePanel() else closeHubSheet()
        }
        binding.panel.btnClosePanel.setOnClickListener { closePanel() }
        bindHubs()
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
        binding.panel.btnRest.setOnClickListener { togglePet() }
        binding.todayCard.setOnClickListener { refreshTodayLine() }
        binding.chipGoalPet.setOnClickListener { doHomePet() }
        binding.chipGoalFeed.setOnClickListener { openFeedPanel() }
        binding.chipGoalCardio.setOnClickListener { startCardio() }
        binding.panel.btnAlbum.setOnClickListener {
            closePanel()
            startActivity(Intent(this, AlbumActivity::class.java))
        }
        binding.panel.btnWhisper.setOnClickListener { openWhispers() }
        binding.whisperCard.setOnClickListener { openWhispers() }
        binding.scheduleLine.setOnClickListener {
            val now = binding.scheduleLine.text?.toString().orEmpty()
            val wish = CompanionSchedule.next(settings, now.removePrefix(getString(R.string.schedule_label, "").trim()))
            binding.scheduleLine.text = getString(R.string.schedule_label, wish)
        }

        binding.panel.sliderSize.value = settings.sizeDp.toFloat()
        binding.panel.sliderOpacity.value = settings.opacity.toFloat()
        binding.panel.switchAlways.isChecked = settings.alwaysShow
        binding.panel.switchMute.isChecked = settings.muted
        binding.panel.switchTts.isChecked = settings.ttsEnabled
        binding.panel.switchMissYou.isChecked = settings.missYou
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
        binding.panel.switchMute.setOnCheckedChangeListener { _, checked ->
            settings.muted = checked
        }
        binding.panel.switchTts.setOnCheckedChangeListener { _, checked ->
            settings.ttsEnabled = checked
        }
        binding.panel.switchMissYou.setOnCheckedChangeListener { _, checked ->
            settings.missYou = checked
            if (checked) askNotification()
            CompanionMissYou.reschedule(this)
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
        val tick = CompanionDay.tick(this)
        refresh()
        easeHomeIn()
        if (tick.celebrate > 0) {
            binding.preview.showBubble(PetDialogue.streak(settings, tick.celebrate), 1400L)
        }
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
        binding.panel.switchMute.isChecked = settings.muted
        binding.panel.switchTts.isChecked = settings.ttsEnabled
        binding.panel.switchMissYou.isChecked = settings.missYou
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
                binding.panel.btnToggle.visibility = View.VISIBLE
                binding.panel.btnToggle.text = getString(R.string.start_pet)
                binding.panel.btnRest.visibility = View.GONE
            }
            settings.running -> {
                binding.status.text = getString(R.string.status_running, name)
                binding.panel.status.text = getString(R.string.status_running, name)
                binding.panel.btnToggle.visibility = View.GONE
                binding.panel.btnRest.visibility = View.VISIBLE
                binding.panel.btnRest.text = getString(if (settings.isMale) R.string.stop_pet_m else R.string.stop_pet)
            }
            else -> {
                binding.status.text = getString(R.string.status_stopped)
                binding.panel.status.text = getString(R.string.status_stopped)
                binding.panel.btnToggle.visibility = View.VISIBLE
                binding.panel.btnToggle.text = getString(R.string.start_pet)
                binding.panel.btnRest.visibility = View.GONE
            }
        }
        bindDailyCard()
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
            binding.stageGlow.animate().cancel()
            binding.stageGlow.alpha = 0f
            PetStats.onTap(settings)
            sfx.tap()
            pet.showBubble(PetDialogue.tap(settings))
            bindDailyCard()
        }
        pet.onExpressionCycle = { pose ->
            PetStats.onCycle(settings)
            sfx.tap()
            pet.showBubble(PetDialogue.pose(settings, pose))
            bindDailyCard()
        }
        pet.onLongPressAction = { sfx.menu() }
        pet.onMenuPet = { doHomePet() }
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
        closeHubSheet()
        closePanel()
        val dialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val panel = FeedPanel(
            this,
            onFed = { item ->
                dialog.dismiss()
                PetStats.onFed(settings, item.moodBoost)
                CompanionDay.noteFeed(settings, auth)
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
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
        dialog.show()
    }

    private fun startCardio() {
        closePanel()
        binding.preview.playCardio(4500L) {
            val kcal = FoodCatalog.cardioKcal()
            PetStats.onCardio(settings)
            CompanionDay.noteCardio(settings, auth)
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

    private fun doHomePet() {
        PetStats.onPet(settings)
        auth.addHappiness(3)
        CompanionDay.notePet(settings, auth)
        binding.preview.playReaction(PetPose.SHY)
        binding.preview.showBubble(PetDialogue.pet(settings))
        sfx.tap()
        refresh()
    }

    private fun refreshTodayLine() {
        val now = binding.todayLine.text?.toString().orEmpty()
        binding.todayLine.text = PetDialogue.nextTodayLine(settings, now)
        sfx.tap()
    }

    private fun easeHomeIn() {
        binding.preview.animate().cancel()
        binding.preview.alpha = 0.28f
        binding.preview.animate().alpha(1f).setDuration(280).start()
    }

    private fun bindDailyCard() {
        CompanionDay.rolloverGoals(settings)
        val stage = CompanionBond.stage(settings)
        val title = when (stage) {
            BondStage.FIRST -> R.string.bond_first
            BondStage.WARM -> R.string.bond_warm
            BondStage.TACIT -> R.string.bond_tacit
            BondStage.BOND -> R.string.bond_bond
        }
        binding.bondTitle.text = getString(R.string.bond_line, getString(title))
        binding.bondBar.progress = CompanionBond.progress(settings)
        binding.bondHint.text = when (stage) {
            BondStage.BOND -> getString(R.string.bond_max)
            BondStage.FIRST -> getString(R.string.bond_next, getString(R.string.bond_warm))
            BondStage.WARM -> getString(R.string.bond_next, getString(R.string.bond_tacit))
            BondStage.TACIT -> getString(R.string.bond_next, getString(R.string.bond_bond))
        }
        val streak = settings.streakDays
        binding.streakLine.text = if (streak <= 1) {
            getString(R.string.streak_first)
        } else {
            getString(R.string.streak_short, streak)
        }
        if (binding.todayLine.text.isNullOrBlank()) {
            binding.todayLine.text = PetDialogue.todayLine(settings)
        }
        binding.scheduleLine.text = getString(R.string.schedule_label, CompanionSchedule.line(settings))
        bindGoalChip(binding.chipGoalPet, settings.goalPet, R.string.goal_pet_on, R.string.goal_pet_off)
        bindGoalChip(binding.chipGoalFeed, settings.goalFeed, R.string.goal_feed_on, R.string.goal_feed_off)
        bindGoalChip(binding.chipGoalCardio, settings.goalCardio, R.string.goal_cardio_on, R.string.goal_cardio_off)
        binding.happyHeart.text = getString(R.string.happy_heart, auth.happiness())
        binding.happyHint.text = when {
            settings.lastHappyRegenAmount > 0 && settings.lastHappyRegenDay == CompanionDay.todayKey() ->
                getString(R.string.happy_hint_gain, settings.lastHappyRegenAmount)
            auth.happiness() >= CompanionDay.HAPPINESS_SOFT_CAP -> getString(R.string.happy_hint_full)
            else -> getString(R.string.happy_hint_wait)
        }
        binding.panel.happyHint.text = binding.happyHint.text
        val fresh = AlbumStore.get(this).sync(settings)
        maybeCelebrateStage(stage, fresh)
    }

    private fun bindGoalChip(view: android.widget.TextView, on: Boolean, onRes: Int, offRes: Int) {
        view.text = getString(if (on) onRes else offRes)
        view.setBackgroundResource(if (on) R.drawable.bg_goal_on else R.drawable.bg_goal_off)
        view.elevation = if (on) 4f * resources.displayMetrics.density else 0f
    }

    private fun maybeCelebrateStage(stage: BondStage, fresh: List<String>) {
        val seen = settings.lastBondStage
        val up = seen >= 0 && stage.ordinal > seen
        if (up) {
            playGlow()
            binding.preview.animate().cancel()
            binding.preview.animate()
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(180)
                .withEndAction {
                    binding.preview.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
                }
                .start()
            binding.preview.showBubble(getString(R.string.bond_up), 1400L)
            sfx.tap()
        } else if (fresh.isNotEmpty()) {
            binding.preview.showBubble(getString(R.string.unlock_new), 1400L)
        }
        settings.lastBondStage = stage.ordinal
    }

    private fun playGlow() {
        val glow = binding.stageGlow
        glow.animate().cancel()
        glow.alpha = 0f
        glow.animate().alpha(0.9f).setDuration(220).withEndAction {
            glow.animate().alpha(0f).setDuration(1100).start()
        }.start()
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

    private fun bindHubs() {
        binding.panel.hubCompanion.setOnClickListener { expandHub(binding.panel.bodyCompanion) }
        binding.panel.hubInteract.setOnClickListener { expandHub(binding.panel.bodyInteract) }
        binding.panel.hubLook.setOnClickListener { expandHub(binding.panel.bodyLook) }
        binding.panel.hubNest.setOnClickListener { expandHub(binding.panel.bodyNest) }
        binding.panel.hubSettings.setOnClickListener { expandHub(binding.panel.bodySettings) }
        binding.fabHubCompanion.setOnClickListener { openFromFab(binding.panel.bodyCompanion) }
        binding.fabHubInteract.setOnClickListener { openFromFab(binding.panel.bodyInteract) }
        binding.fabHubLook.setOnClickListener { openFromFab(binding.panel.bodyLook) }
        binding.fabHubNest.setOnClickListener { openFromFab(binding.panel.bodyNest) }
        binding.fabHubSettings.setOnClickListener { openFromFab(binding.panel.bodySettings) }
    }

    private fun hubBodies(): List<View> {
        return listOf(
            binding.panel.bodyCompanion,
            binding.panel.bodyInteract,
            binding.panel.bodyLook,
            binding.panel.bodyNest,
            binding.panel.bodySettings,
        )
    }

    private fun expandHub(body: View) {
        hubBodies().forEach {
            it.visibility = if (it == body) View.VISIBLE else View.GONE
        }
    }

    private fun toggleHubSheet() {
        if (panelOpen) {
            closePanel()
            return
        }
        if (hubSheetOpen) closeHubSheet() else openHubSheet()
    }

    private fun openHubSheet() {
        hubSheetOpen = true
        binding.scrim.visibility = View.VISIBLE
        binding.scrim.alpha = 0f
        binding.scrim.animate().alpha(1f).setDuration(180).start()
        binding.fabSheet.visibility = View.VISIBLE
        binding.fabSheet.alpha = 0f
        binding.fabSheet.scaleX = 0.88f
        binding.fabSheet.scaleY = 0.88f
        binding.fabSheet.post {
            binding.fabSheet.pivotX = binding.fabSheet.width.toFloat()
            binding.fabSheet.pivotY = binding.fabSheet.height.toFloat()
        }
        binding.fabSheet.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
        binding.fabMenu.animate().rotation(45f).setDuration(180).start()
        backToClosePanel.isEnabled = true
    }

    private fun closeHubSheet() {
        if (!hubSheetOpen) return
        hubSheetOpen = false
        if (!panelOpen) backToClosePanel.isEnabled = false
        binding.fabSheet.animate().alpha(0f).scaleX(0.88f).scaleY(0.88f).setDuration(140).withEndAction {
            if (!hubSheetOpen) binding.fabSheet.visibility = View.GONE
        }.start()
        if (!panelOpen) {
            binding.scrim.animate().alpha(0f).setDuration(140).withEndAction {
                if (!panelOpen && !hubSheetOpen) binding.scrim.visibility = View.GONE
            }.start()
        }
        binding.fabMenu.animate().rotation(0f).setDuration(160).start()
    }

    private fun openFromFab(body: View) {
        expandHub(body)
        closeHubSheet()
        openPanel()
    }

    private fun openWhispers() {
        closePanel()
        closeHubSheet()
        startActivity(Intent(this, WhisperActivity::class.java))
    }

    private fun openPanel() {
        panelOpen = true
        binding.scrim.visibility = View.VISIBLE
        binding.scrim.animate().cancel()
        binding.scrim.alpha = 1f
        binding.panel.root.isClickable = true
        binding.panel.root.isFocusable = true
        binding.panel.root.visibility = View.VISIBLE
        binding.panel.root.animate().translationX(0f).setDuration(280).start()
        binding.fabMenu.animate().rotation(45f).setDuration(180).start()
        backToClosePanel.isEnabled = true
    }

    private fun closePanel() {
        if (!panelOpen) return
        panelOpen = false
        if (!hubSheetOpen) backToClosePanel.isEnabled = false
        val width = binding.panel.root.width.toFloat().coerceAtLeast(1f)
        binding.scrim.animate().alpha(0f).setDuration(200).withEndAction {
            if (!panelOpen && !hubSheetOpen) binding.scrim.visibility = View.GONE
        }.start()
        binding.panel.root.animate().translationX(width).setDuration(240).withEndAction {
            if (!panelOpen) parkPanel()
        }.start()
        if (!hubSheetOpen) binding.fabMenu.animate().rotation(0f).setDuration(160).start()
    }

    private fun insetFab() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabMenu) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val extra = (20 * resources.displayMetrics.density).toInt()
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + extra
                rightMargin = bars.right + extra
            }
            binding.fabSheet.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + extra + (72 * resources.displayMetrics.density).toInt()
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
