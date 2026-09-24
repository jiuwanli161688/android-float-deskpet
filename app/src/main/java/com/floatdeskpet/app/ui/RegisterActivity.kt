package com.floatdeskpet.app.ui

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import com.floatdeskpet.app.auth.AuthStore
import com.floatdeskpet.app.auth.AuthValidator
import com.floatdeskpet.app.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: AuthStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = AuthStore.get(this)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnRegister.setOnClickListener { submit() }
        binding.linkLogin.setOnClickListener { finish() }
        binding.inputNickname.setOnEditorActionListener { _, actionId, _ ->
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
        binding.confirmLayout.error = null
        binding.nicknameLayout.error = null
        val account = binding.inputAccount.text?.toString().orEmpty()
        val password = binding.inputPassword.text?.toString().orEmpty()
        val confirm = binding.inputConfirm.text?.toString().orEmpty()
        val nickname = binding.inputNickname.text?.toString().orEmpty()
        AuthValidator.accountError(account)?.let {
            binding.accountLayout.error = it
            return
        }
        AuthValidator.passwordError(password)?.let {
            binding.passwordLayout.error = it
            return
        }
        AuthValidator.confirmError(password, confirm)?.let {
            binding.confirmLayout.error = it
            return
        }
        AuthValidator.nicknameError(nickname)?.let {
            binding.nicknameLayout.error = it
            return
        }
        when (val result = auth.register(account, password, nickname)) {
            is AuthStore.Result.Ok -> {
                when (val login = auth.login(account, password)) {
                    is AuthStore.Result.Ok -> AppFlow.route(this)
                    is AuthStore.Result.Err -> binding.accountLayout.error = login.message
                }
            }
            is AuthStore.Result.Err -> binding.accountLayout.error = result.message
        }
    }
}
