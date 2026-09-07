package com.proyecto.chambaya

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton

class OpcionesAccesoActivity : AppCompatActivity() {

    private lateinit var btnLanguage: ImageButton
    private lateinit var btnGetStarted: MaterialButton
    private lateinit var btnSignup: MaterialButton
    private lateinit var btnLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(R.color.surface_light)
        window.navigationBarColor = getColor(R.color.surface_light)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true

        setContentView(R.layout.actividad_opciones_acceso)

        btnLanguage = findViewById(R.id.btnLanguage)
        btnGetStarted = findViewById(R.id.btnGetStarted)
        btnSignup = findViewById(R.id.btnSignup)
        btnLogin = findViewById(R.id.btnLogin)

        setupClickListeners()
        updateLocalizedTexts()
    }

    private fun setupClickListeners() {
        btnLanguage.setOnClickListener { showLanguagePopup() }
        btnGetStarted.setOnClickListener { /* Navegación al modo invitado: se implementará luego */ }
        btnSignup.setOnClickListener { /* Registro: se implementará en un paso posterior */ }
        btnLogin.setOnClickListener { openLogin() }
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun showLanguagePopup() {
        val popupRoot = layoutInflater.inflate(R.layout.popup_seleccion_idioma, null)
        val currentLanguage = IdiomaManager.getSavedLanguage(this)

        val optionSpanish = popupRoot.findViewById<LinearLayout>(R.id.optionSpanish)
        val optionEnglish = popupRoot.findViewById<LinearLayout>(R.id.optionEnglish)
        val optionQuechua = popupRoot.findViewById<LinearLayout>(R.id.optionQuechua)
        val checkSpanish = popupRoot.findViewById<ImageView>(R.id.ivCheckSpanish)
        val checkEnglish = popupRoot.findViewById<ImageView>(R.id.ivCheckEnglish)
        val checkQuechua = popupRoot.findViewById<ImageView>(R.id.ivCheckQuechua)

        updatePopupSelection(
            optionSpanish, optionEnglish, optionQuechua,
            checkSpanish, checkEnglish, checkQuechua,
            currentLanguage
        )

        val popupWidth = dp(220)
        val popup = PopupWindow(
            popupRoot,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        popup.isFocusable = true
        popup.isOutsideTouchable = true
        popup.isClippingEnabled = true
        popup.elevation = dp(12).toFloat()

        optionSpanish.setOnClickListener {
            selectLanguage("es", popup)
        }
        optionEnglish.setOnClickListener {
            selectLanguage("en", popup)
        }
        optionQuechua.setOnClickListener {
            selectLanguage("qu", popup)
        }

        val anchor = btnLanguage
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val margin = dp(20)

        val x = (location[0] + anchor.width - popupWidth).coerceAtLeast(margin)
        val y = location[1] + anchor.height + dp(6)

        popup.showAtLocation(anchor, Gravity.TOP or Gravity.START, x, y)
    }

    private fun updatePopupSelection(
        optionSpanish: LinearLayout,
        optionEnglish: LinearLayout,
        optionQuechua: LinearLayout,
        checkSpanish: ImageView,
        checkEnglish: ImageView,
        checkQuechua: ImageView,
        language: String
    ) {
        optionSpanish.isSelected = language == "es"
        optionEnglish.isSelected = language == "en"
        optionQuechua.isSelected = language == "qu"
        checkSpanish.visibility = if (language == "es") View.VISIBLE else View.GONE
        checkEnglish.visibility = if (language == "en") View.VISIBLE else View.GONE
        checkQuechua.visibility = if (language == "qu") View.VISIBLE else View.GONE
    }

    private fun selectLanguage(language: String, popup: PopupWindow) {
        if (IdiomaManager.getSavedLanguage(this) != language) {
            IdiomaManager.saveLanguage(this, language)
            updateLocalizedTexts()
        }
        popup.dismiss()
    }

    private fun updateLocalizedTexts() {
        btnLanguage.contentDescription = localizedString(R.string.action_change_language)
        val tvAccessTitle: TextView = findViewById(R.id.tvAccessTitle)
        tvAccessTitle.text = localizedString(R.string.access_options_title)
        btnGetStarted.text = localizedString(R.string.welcome_get_started)
        btnSignup.text = localizedString(R.string.access_btn_signup)
        btnLogin.text = localizedString(R.string.access_btn_login)
    }

    private fun localizedString(@StringRes resId: Int): String {
        return IdiomaManager.createLocalizedContext(this).getString(resId)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
