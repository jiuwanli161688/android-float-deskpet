package com.floatdeskpet.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: AuthStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = AuthStore.get(this)
        if (auth.isLoggedIn()) {
            AppFlow.route(this)
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLogin.setOnClickListener { submit() }
        binding.linkRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.inputPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }
    }

    private fun submit() {
        binding.accountLayout.error = null
        binding.passwordLayout.error = null
        val account = binding.inputAccount.text?.toString().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()
        when (val result = auth.login(account, password)) {
            is AuthStore.Result.Ok -> AppFlow.route(this)
            is AuthStore.Result.Err -> {
                binding.passwordLayout.error = result.message
                binding.inputPassword.requestFocus()
            }
        }
    }
}
