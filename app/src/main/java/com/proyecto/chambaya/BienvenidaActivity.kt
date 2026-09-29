package com.proyecto.chambaya

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import kotlin.math.abs

class BienvenidaActivity : AppCompatActivity() {

    private lateinit var imgWelcomeIllustration: ImageView
    private lateinit var tvWelcomeTitle: TextView
    private lateinit var tvWelcomeSubtitle: TextView
    private lateinit var btnNext: com.google.android.material.button.MaterialButton
    private lateinit var btnSkip: TextView
    private lateinit var btnLanguage: ImageButton
    private lateinit var indicators: List<View>
    private lateinit var gestureDetector: GestureDetectorCompat

    private var currentSlideIndex = 0
    private var indicatorAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)

        // Modo inmersivo edge-to-edge con barras transparentes sobre la foto
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContentView(R.layout.actividad_bienvenida)

        // Ajuste dinámico de márgenes de acuerdo al notch / barra de navegación
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.welcomeRoot)) { _, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            findViewById<View>(R.id.brandGroup).updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarInset.top + dp(16)
            }
            findViewById<View>(R.id.bottomContentGroup).updatePadding(
                bottom = navBarInset.bottom + dp(28)
            )
            insets
        }

        imgWelcomeIllustration = findViewById(R.id.imgWelcomeIllustration)
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle)
        tvWelcomeSubtitle = findViewById(R.id.tvWelcomeSubtitle)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
        btnLanguage = findViewById(R.id.btnLanguage)
        btnLanguage.setOnClickListener { showLanguagePopup() }

        // Exactamente 3 indicadores
        indicators = listOf(
            findViewById(R.id.indicatorOne),
            findViewById(R.id.indicatorTwo),
            findViewById(R.id.indicatorThree)
        )

        indicators.forEachIndexed { index, indicator ->
            indicator.setOnClickListener { showSlide(index, restartProgress = true) }
        }

        // Navegación con botón principal único
        btnNext.setOnClickListener { goToNextSlide() }
        btnSkip.setOnClickListener { openAccessOptions() }

        // Soporte de deslizamiento táctil horizontal (Swipe gesture)
        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            private val swipeThreshold = dp(40)
            private val swipeVelocityThreshold = dp(40)

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) > abs(diffY) &&
                    abs(diffX) > swipeThreshold &&
                    abs(velocityX) > swipeVelocityThreshold
                ) {
                    if (diffX < 0) {
                        goToNextSlide()
                    } else {
                        goToPreviousSlide()
                    }
                    return true
                }
                return false
            }
        }
        gestureDetector = GestureDetectorCompat(this, gestureListener)

        updateLocalizedTexts()
        showSlide(0, restartProgress = true)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (::gestureDetector.isInitialized) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
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
        val nextIndex = currentSlideIndex + 1
        if (nextIndex >= getSlides().size) {
            openAccessOptions()
        } else {
            showSlide(nextIndex, restartProgress = true)
        }
    }

    private fun goToPreviousSlide() {
        if (currentSlideIndex > 0) {
            showSlide(currentSlideIndex - 1, restartProgress = true)
        }
    }

    private fun openAccessOptions() {
        indicatorAnimator?.cancel()
        startActivity(Intent(this, OpcionesAccesoActivity::class.java))
        finish()
    }

    private fun showSlide(index: Int, restartProgress: Boolean) {
        val previousIndex = currentSlideIndex
        currentSlideIndex = index
        val slide = getSlides()[index]

        // Transición suave entre imágenes de fondo (Crossfade)
        if (previousIndex != index) {
            imgWelcomeIllustration.animate()
                .alpha(0.35f)
                .setDuration(180)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    imgWelcomeIllustration.setImageResource(slide.imageRes)
                    imgWelcomeIllustration.animate()
                        .alpha(1f)
                        .setDuration(240)
                        .start()
                }
                .start()

            // Transición suave en el título y subtítulo
            tvWelcomeTitle.animate()
                .alpha(0f)
                .translationY(dp(6).toFloat())
                .setDuration(150)
                .withEndAction {
                    tvWelcomeTitle.text = localizedString(slide.titleRes)
                    tvWelcomeTitle.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(220)
                        .start()
                }
                .start()

            tvWelcomeSubtitle.animate()
                .alpha(0f)
                .translationY(dp(4).toFloat())
                .setDuration(150)
                .withEndAction {
                    tvWelcomeSubtitle.text = localizedString(slide.subtitleRes)
                    tvWelcomeSubtitle.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(220)
                        .start()
                }
                .start()
        } else {
            imgWelcomeIllustration.setImageResource(slide.imageRes)
            tvWelcomeTitle.text = localizedString(slide.titleRes)
            tvWelcomeSubtitle.text = localizedString(slide.subtitleRes)
        }

        // Actualizar estados visuales de los 3 indicadores
        indicators.forEachIndexed { indicatorIndex, indicator ->
            val isSelected = indicatorIndex == index
            indicator.setBackgroundResource(
                if (isSelected) R.drawable.bg_indicator_active else R.drawable.bg_indicator_inactive
            )
            indicator.layoutParams = indicator.layoutParams.apply {
                width = dp(if (isSelected) 32 else 8)
                height = dp(6)
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
        val expandedWidth = dp(32)

        activeIndicator.layoutParams = activeIndicator.layoutParams.apply {
            width = collapsedWidth
            height = dp(6)
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
                        val nextIndex = currentSlideIndex + 1
                        if (nextIndex >= getSlides().size) {
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
        btnNext.text = localizedString(R.string.welcome_get_started)
        btnSkip.text = localizedString(R.string.welcome_account_prompt_login)
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

    // Las 3 imágenes de fondo con sus respectivos textos
    private fun getSlides() = listOf(
        WelcomeSlide(
            R.drawable.bienvenido_a_chambaya,
            R.string.welcome_slide_1_title,
            R.string.welcome_slide_1_subtitle
        ),
        WelcomeSlide(
            R.drawable.encuentra_chambass_cerca,
            R.string.welcome_slide_2_title,
            R.string.welcome_slide_2_subtitle
        ),
        WelcomeSlide(
            R.drawable.chatea_y_consigue_trabajo,
            R.string.welcome_slide_4_title,
            R.string.welcome_slide_4_subtitle
        )
    )

    private companion object {
        const val SLIDE_DURATION_MS = 3800L
    }
}
