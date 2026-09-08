package com.proyecto.chambaya

import android.app.Activity
import android.graphics.Color
import androidx.core.view.WindowCompat

object BarraEstadoUtils {

    fun aplicarColor(activity: Activity, colorFondo: Int) {
        activity.window.statusBarColor = colorFondo

        val esFondoClaro = luminosidad(colorFondo) > 0.5
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            .isAppearanceLightStatusBars = esFondoClaro
    }

    private fun luminosidad(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.299 * r + 0.587 * g + 0.114 * b
    }
}