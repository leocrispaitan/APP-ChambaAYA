package com.proyecto.chambaya

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(R.color.brand_color)
        window.navigationBarColor = getColor(R.color.surface_light)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true

        setContentView(R.layout.actividad_login)

        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)
        val etEmail = findViewById<android.widget.EditText>(R.id.etEmail)
        val etPassword = findViewById<android.widget.EditText>(R.id.etPassword)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        val btnSignIn = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSignIn)
        val btnGoogleSignIn = findViewById<android.widget.LinearLayout>(R.id.btnGoogleSignIn)
        val tvSignupLink = findViewById<android.widget.TextView>(R.id.tvSignupLink)

        btnBack.setOnClickListener { finish() }

        btnSignIn.setOnClickListener {
            // Frontend únicamente: validación básica. El backend se conectará en un paso futuro.
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            when {
                email.isEmpty() -> Snackbar.make(
                    findViewById(R.id.loginRoot),
                    getString(R.string.auth_error_empty_email),
                    Snackbar.LENGTH_SHORT
                ).show()
                password.isEmpty() -> Snackbar.make(
                    findViewById(R.id.loginRoot),
                    getString(R.string.auth_error_empty_password),
                    Snackbar.LENGTH_SHORT
                ).show()
                else -> Snackbar.make(
                    findViewById(R.id.loginRoot),
                    getString(R.string.auth_wip_message),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        btnGoogleSignIn.setOnClickListener {
            // Frontend únicamente: Google Sign-In se conectará en un paso futuro.
            Snackbar.make(
                findViewById(R.id.loginRoot),
                getString(R.string.auth_wip_message),
                Snackbar.LENGTH_SHORT
            ).show()
        }

        tvSignupLink.setOnClickListener {
            // Registro se implementará en un paso futuro.
        }

        setupPasswordToggle(etPassword, ivTogglePassword)
    }

    private fun setupPasswordToggle(editText: android.widget.EditText, toggle: ImageView) {
        var isPasswordVisible = false
        editText.transformationMethod = PasswordTransformationMethod.getInstance()
        updatePasswordToggleIcon(toggle, isPasswordVisible)

        toggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            editText.transformationMethod = if (isPasswordVisible) {
                null
            } else {
                PasswordTransformationMethod.getInstance()
            }
            editText.setSelection(editText.text?.length ?: 0)
            updatePasswordToggleIcon(toggle, isPasswordVisible)
        }
    }

    private fun updatePasswordToggleIcon(toggle: ImageView, isPasswordVisible: Boolean) {
        toggle.setImageResource(
            if (isPasswordVisible) R.drawable.ic_login_eye_on else R.drawable.ic_login_eye_off
        )
        toggle.contentDescription = getString(
            if (isPasswordVisible) R.string.auth_hide_password else R.string.auth_show_password
        )
    }
}