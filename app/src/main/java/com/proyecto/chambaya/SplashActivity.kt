package com.proyecto.chambaya

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val openMainRunnable = Runnable {
        startActivity(Intent(this, BienvenidaActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(R.color.brand_color)
        window.navigationBarColor = getColor(R.color.brand_color)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false

        setContentView(R.layout.actividad_splash)

        handler.postDelayed(openMainRunnable, SPLASH_DURATION_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(openMainRunnable)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1200L
    }
}
