package com.proyecto.chambaya

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

class BienvenidaActivity : AppCompatActivity() {

    private lateinit var imgWelcomeIllustration: ImageView
    private lateinit var tvWelcomeTitle: TextView
    private lateinit var tvWelcomeSubtitle: TextView
    private lateinit var btnSkip: TextView
    private lateinit var btnNext: com.google.android.material.button.MaterialButton
    private lateinit var btnLanguage: ImageButton
    private lateinit var indicators: List<View>

    private var currentSlideIndex = 0
    private var indicatorAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(R.color.surface_light)
        window.navigationBarColor = getColor(R.color.surface_light)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true

        setContentView(R.layout.actividad_bienvenida)

        imgWelcomeIllustration = findViewById(R.id.imgWelcomeIllustration)
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle)
        tvWelcomeSubtitle = findViewById(R.id.tvWelcomeSubtitle)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)
        btnLanguage = findViewById(R.id.btnLanguage)
        btnLanguage.setOnClickListener { showLanguagePopup() }

        indicators = listOf(
            findViewById(R.id.indicatorOne),
            findViewById(R.id.indicatorTwo),
            findViewById(R.id.indicatorThree),
            findViewById(R.id.indicatorFour)
        )

        indicators.forEachIndexed { index, indicator ->
            indicator.setOnClickListener { showSlide(index, restartProgress = true) }
        }

        btnNext.setOnClickListener { goToNextSlide() }
        btnSkip.setOnClickListener { openAccessOptions() }

        showSlide(0, restartProgress = true)
    }

    override fun onResume() {
        super.onResume()
        if (::indicators.isInitialized) {
            startIndicatorProgress()
        }
    }

    override fun onPause() {
        indicatorAnimator?.cancel()
        super.onPause()
    }

    override fun onDestroy() {
        indicatorAnimator?.cancel()
        indicatorAnimator = null
        super.onDestroy()
    }

    private fun goToNextSlide() {
        val nextIndex = (currentSlideIndex + 1) % getSlides().size
        if (nextIndex == 0) {
            openAccessOptions()
        } else {
            showSlide(nextIndex, restartProgress = true)
        }
    }

    private fun openAccessOptions() {
        indicatorAnimator?.cancel()
        startActivity(Intent(this, OpcionesAccesoActivity::class.java))
        finish()
    }

    private fun showSlide(index: Int, restartProgress: Boolean) {
        currentSlideIndex = index
        val slide = getSlides()[index]
        imgWelcomeIllustration.setImageResource(slide.imageRes)
        tvWelcomeTitle.text = localizedString(slide.titleRes)
        tvWelcomeSubtitle.text = localizedString(slide.subtitleRes)

        indicators.forEachIndexed { indicatorIndex, indicator ->
            val isSelected = indicatorIndex == index
            indicator.setBackgroundResource(
                if (isSelected) R.drawable.bg_indicator_active else R.drawable.bg_indicator_inactive
            )
            indicator.layoutParams = indicator.layoutParams.apply {
                width = dp(8)
                height = dp(8)
            }
        }

        if (restartProgress) {
            startIndicatorProgress()
        }
    }

    private fun startIndicatorProgress() {
        indicatorAnimator?.cancel()
        val activeIndicator = indicators[currentSlideIndex]
        val collapsedWidth = dp(8)
        val expandedWidth = dp(34)

        activeIndicator.layoutParams = activeIndicator.layoutParams.apply {
            width = collapsedWidth
            height = dp(8)
        }
        activeIndicator.requestLayout()

        indicatorAnimator = ValueAnimator.ofInt(collapsedWidth, expandedWidth).apply {
            duration = SLIDE_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                activeIndicator.layoutParams = activeIndicator.layoutParams.apply {
                    width = animator.animatedValue as Int
                }
                activeIndicator.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var wasCancelled = false

                override fun onAnimationCancel(animation: Animator) {
                    wasCancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!wasCancelled) {
                        val nextIndex = (currentSlideIndex + 1) % getSlides().size
                        if (nextIndex == 0) {
                            openAccessOptions()
                        } else {
                            showSlide(nextIndex, restartProgress = true)
                        }
                    }
                }
            })
            start()
        }
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
        val screenWidth = resources.displayMetrics.widthPixels
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
        btnSkip.text = localizedString(R.string.welcome_skip)
        btnNext.text = localizedString(R.string.welcome_next)
        updateCurrentSlideText()
    }

    private fun updateCurrentSlideText() {
        val slide = getSlides()[currentSlideIndex]
        tvWelcomeTitle.text = localizedString(slide.titleRes)
        tvWelcomeSubtitle.text = localizedString(slide.subtitleRes)
    }

    private fun localizedString(@StringRes resId: Int): String {
        return localizedContext.getString(resId)
    }

    private val localizedContext: Context
        get() = IdiomaManager.createLocalizedContext(this)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class WelcomeSlide(
        val imageRes: Int,
        val titleRes: Int,
        val subtitleRes: Int
    )

    private fun getSlides() = listOf(
        WelcomeSlide(
            R.drawable.welcome_illustration,
            R.string.welcome_slide_1_title,
            R.string.welcome_slide_1_subtitle
        ),
        WelcomeSlide(
            R.drawable.welcome_illustration_jobs,
            R.string.welcome_slide_2_title,
            R.string.welcome_slide_2_subtitle
        ),
        WelcomeSlide(
            R.drawable.welcome_illustration_map,
            R.string.welcome_slide_3_title,
            R.string.welcome_slide_3_subtitle
        ),
        WelcomeSlide(
            R.drawable.welcome_illustration_chat,
            R.string.welcome_slide_4_title,
            R.string.welcome_slide_4_subtitle
        )
    )

    private companion object {
        const val SLIDE_DURATION_MS = 3500L
    }
}
