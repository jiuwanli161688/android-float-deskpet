package com.floatdeskpet.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.floatdeskpet.app.R
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.databinding.ActivityProfileBinding
import com.floatdeskpet.app.overlay.OverlayService

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: AuthStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = AuthStore.get(this)
        if (!auth.isLoggedIn()) {
            AppFlow.route(this)
            return
        }
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnRedo.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java).putExtra(SetupActivity.EXTRA_EDIT, true))
        }
        binding.btnPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
        binding.btnLogout.setOnClickListener { logout() }
    }

    override fun onResume() {
        super.onResume()
        if (!auth.isLoggedIn()) {
            AppFlow.route(this)
            return
        }
        bindUser()
    }

    private fun bindUser() {
        val user = auth.current() ?: return
        binding.nickname.text = user.nickname
        binding.account.text = getString(R.string.profile_account, user.account)
        binding.happiness.text = getString(R.string.profile_happiness, user.happiness)
    }

    private fun logout() {
        OverlayService.stop(this)
        auth.logout()
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        finish()
    }
}
