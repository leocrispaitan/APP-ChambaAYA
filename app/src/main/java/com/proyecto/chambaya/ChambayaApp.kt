package com.proyecto.chambaya

import android.app.Application
import android.util.Log

/**
 * Guarda el último cierre inesperado para poder verlo en el siguiente arranque.
 * No cambia ningún flujo: si no hubo crash, la app arranca igual que siempre.
 */
class ChambayaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { hilo, error ->
            runCatching {
                openFileOutput(ARCHIVO_CRASH, MODE_PRIVATE).use {
                    it.write(Log.getStackTraceString(error).toByteArray())
                }
                getSharedPreferences(PREFS_SOPORTE, MODE_PRIVATE).edit()
                    .putBoolean(KEY_HUBO_CRASH, true).apply()
            }
            anterior?.uncaughtException(hilo, error)
        }
    }

    companion object {
        const val ARCHIVO_CRASH = "ultimo_crash.txt"
        const val PREFS_SOPORTE = "soporte"
        const val KEY_HUBO_CRASH = "hubo_crash"
    }
}
