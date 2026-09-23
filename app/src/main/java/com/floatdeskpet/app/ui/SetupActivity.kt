package com.floatdeskpet.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.floatdeskpet.app.R
import com.floatdeskpet.app.data.PetSettings
import com.floatdeskpet.app.databinding.ActivitySetupBinding
import com.floatdeskpet.app.overlay.PetFrames

class SetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetupBinding
    private lateinit var settings: PetSettings
    private var male = false
    private var editing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = PetSettings.get(this)
        editing = intent.getBooleanExtra(EXTRA_EDIT, false)
        if (!editing && !settings.needsSetup()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        male = if (editing) settings.isMale else false
        syncGenderCards()
        binding.cardFemale.setOnClickListener {
            male = false
            syncGenderCards()
        }
        binding.cardMale.setOnClickListener {
            male = true
            syncGenderCards()
        }
        binding.btnGenderNext.setOnClickListener { showNameStep() }
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
        binding.namePreview.setImageResource(PetFrames.of(if (male) PetSettings.GENDER_MALE else PetSettings.GENDER_FEMALE, PetSettings.OUTFIT_CASUAL).idle)
        val def = PetSettings.defaultName(male)
        binding.nameLayout.hint = def
        binding.nameHint.text = getString(R.string.setup_name_body, def)
        if (editing && settings.petName.isNotBlank() && settings.petName != def) {
            binding.inputName.setText(settings.petName)
        } else {
            binding.inputName.setText("")
        }
        binding.inputName.requestFocus()
    }

    private fun finishSetup(skip: Boolean) {
        val typed = binding.inputName.text?.toString().orEmpty()
        val name = if (skip) "" else typed
        settings.applySetup(male, name)
        if (editing) {
            finish()
        } else {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun syncGenderCards() {
        binding.cardFemale.setBackgroundResource(if (male) R.drawable.bg_card else R.drawable.bg_card_selected)
        binding.cardMale.setBackgroundResource(if (male) R.drawable.bg_card_selected else R.drawable.bg_card)
    }

    companion object {
        const val EXTRA_EDIT = "edit"
    }
}
