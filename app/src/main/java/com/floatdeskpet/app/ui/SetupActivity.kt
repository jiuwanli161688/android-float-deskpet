package com.floatdeskpet.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.floatdeskpet.app.R
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.data.GuideStore
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.databinding.ActivitySetupBinding
import com.floatdeskpet.app.overlay.PetFrames

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding
    private lateinit var settings: PetSettings
    private lateinit var auth: AuthStore
    private var male = true
    private var editing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = PetSettings.get(this)
        auth = AuthStore.get(this)
        if (!GuideStore.get(this).completed || !auth.isLoggedIn()) {
            AppFlow.route(this)
            return
        }
        editing = intent.getBooleanExtra(EXTRA_EDIT, false)
        if (!editing && auth.current()?.companionReady == true) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        male = if (editing) settings.isMale else true
        syncGender()
        binding.chipFemale.setOnClickListener {
            if (editing) return@setOnClickListener
            male = false
            syncGender()
        }
        binding.chipMale.setOnClickListener {
            if (editing) return@setOnClickListener
            male = true
            syncGender()
        }
        binding.btnGenderNext.setOnClickListener { showNameStep() }
        binding.btnGenderSkip.setOnClickListener {
            if (!editing) {
                male = true
                syncGender()
            }
            showNameStep()
        }
        if (editing) showNameStep()
        binding.btnNameDone.setOnClickListener { finishSetup(skip = false) }
        binding.btnNameSkip.setOnClickListener { finishSetup(skip = true) }
        binding.inputName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                finishSetup(skip = false)
                true
            } else {
                false
            }
        }
    }

    private fun showNameStep() {
        binding.stepGender.visibility = View.GONE
        binding.stepName.visibility = View.VISIBLE
        applyArt(binding.namePreview)
        binding.nameHint.text = getString(R.string.setup_name_body)
        if (editing && settings.petName.isNotBlank()) {
            binding.inputName.setText(settings.petName)
        } else {
            binding.inputName.setText("")
        }
        binding.inputName.requestFocus()
    }

    private fun finishSetup(skip: Boolean) {
        val typed = binding.inputName.text?.toString().orEmpty()
        val name = if (skip) "" else typed
        settings.applySetup(if (editing) settings.isMale else male, name)
        auth.markCompanionReady()
        if (editing) {
            finish()
        } else {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            finish()
        }
    }

    private fun syncGender() {
        binding.chipFemale.isSelected = !male
        binding.chipMale.isSelected = male
        binding.chipFemale.setBackgroundResource(if (male) R.drawable.bg_card else R.drawable.bg_card_selected)
        binding.chipMale.setBackgroundResource(if (male) R.drawable.bg_card_selected else R.drawable.bg_card)
        applyArt(binding.genderPreview)
        binding.genderPreview.contentDescription = getString(
            if (male) R.string.setup_male_label else R.string.setup_female_label,
        )
    }

    private fun applyArt(view: android.widget.ImageView) {
        val gender = if (male) PetSettings.GENDER_MALE else PetSettings.GENDER_FEMALE
        view.setImageResource(PetFrames.of(gender, PetSettings.OUTFIT_CASUAL).idle)
    }

    companion object {
        const val EXTRA_EDIT = "edit"
    }
}
