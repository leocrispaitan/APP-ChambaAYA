package com.proyecto.chambaya

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla completa de idiomas.
 * Cambia el idioma de TODA la app (español por defecto, inglés, quechua):
 * guarda y aplica vía [IdiomaManager.setLanguage], que recrea las pantallas
 * con el nuevo locale. La selección actual se marca al abrir.
 */
class ActividadIdioma : AppCompatActivity() {

    private var seleccionado = 1 // 0 inglés, 1 español, 2 quechua

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_idioma)

        seleccionado = when (IdiomaManager.getSavedLanguage(this)) {
            "en" -> 0
            "qu" -> 2
            else -> 1
        }

        val rEn = findViewById<ImageView>(R.id.radioIngles)
        val rEs = findViewById<ImageView>(R.id.radioEspanol)
        val rQu = findViewById<ImageView>(R.id.radioQuechua)

        fun pintar() {
            rEn.setImageResource(if (seleccionado == 0) R.drawable.ic_lang_radio_on else R.drawable.ic_lang_radio_off)
            rEs.setImageResource(if (seleccionado == 1) R.drawable.ic_lang_radio_on else R.drawable.ic_lang_radio_off)
            rQu.setImageResource(if (seleccionado == 2) R.drawable.ic_lang_radio_on else R.drawable.ic_lang_radio_off)
        }

        fun elegir(cual: Int, codigo: String) {
            seleccionado = cual
            pintar()
            // Aplica en toda la app (la activity se recrea sola con el nuevo idioma).
            IdiomaManager.setLanguage(this, codigo)
        }

        findViewById<View>(R.id.filaIdiomaIngles)?.setOnClickListener { elegir(0, "en") }
        findViewById<View>(R.id.filaIdiomaEspanol)?.setOnClickListener { elegir(1, "es") }
        findViewById<View>(R.id.filaIdiomaQuechua)?.setOnClickListener { elegir(2, "qu") }
        findViewById<View>(R.id.btnIdiomaAtras)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnIdiomaListo)?.setOnClickListener { finish() }

        pintar()
    }
}
