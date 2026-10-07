package com.proyecto.chambaya

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val openMainRunnable = Runnable {
        startActivity(Intent(this, BienvenidaActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_splash)

        // Si la vez anterior se cerró sola, se muestra el error antes de avanzar.
        if (mostrarCrashSiExiste()) return
        // Entrada moderna: el grupo central aparece con fundido + subida suave.
        findViewById<View>(R.id.splashCenterGroup)?.let { grupo ->
            grupo.alpha = 0f
            grupo.translationY = 36f
            grupo.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(550L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        handler.postDelayed(openMainRunnable, SPLASH_DURATION_MS)
    }

    /**
     * Muestra el error guardado del último cierre para tomar captura.
     * Devuelve true si lo mostró (el avance se retoma con Continuar).
     */
    private fun mostrarCrashSiExiste(): Boolean {
        val prefs = getSharedPreferences(ChambayaApp.PREFS_SOPORTE, MODE_PRIVATE)
        if (!prefs.getBoolean(ChambayaApp.KEY_HUBO_CRASH, false)) return false
        prefs.edit().remove(ChambayaApp.KEY_HUBO_CRASH).apply()
        val traza = runCatching {
            openFileInput(ChambayaApp.ARCHIVO_CRASH).bufferedReader().use { it.readText() }
        }.getOrDefault("Sin detalle.")
        val vista = layoutInflater.inflate(R.layout.dialog_crash, null)
        vista.findViewById<TextView>(R.id.tvCrashTrace).text = traza.take(4000)
        val dialogo = AlertDialog.Builder(this).setView(vista).setCancelable(false).create()
        vista.findViewById<View>(R.id.btnCrashShare)?.setOnClickListener {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, traza.take(8000))
                    },
                    "Enviar error"
                )
            )
        }
        vista.findViewById<View>(R.id.btnCrashGo)?.setOnClickListener {
            dialogo.dismiss()
            handler.postDelayed(openMainRunnable, 300)
        }
        dialogo.show()
        return true
    }

    override fun onDestroy() {
        handler.removeCallbacks(openMainRunnable)
        super.onDestroy()
    }

    private companion object {
        const val SPLASH_DURATION_MS = 1300L
    }
}
