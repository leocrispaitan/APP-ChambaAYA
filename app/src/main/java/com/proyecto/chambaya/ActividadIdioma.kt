package com.proyecto.chambaya

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

/**
 * Pantalla completa de idiomas — SOLO DISEÑO.
 * English / Español / Quechua (Perú). La selección es visual, no cambia el locale.
 */
class ActividadIdioma : AppCompatActivity() {

    private var seleccionado = 0 // 0 inglés, 1 español, 2 quechua

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actividad_idioma)

        val rEn = findViewById<ImageView>(R.id.radioIngles)
        val rEs = findViewById<ImageView>(R.id.radioEspanol)
        val rQu = findViewById<ImageView>(R.id.radioQuechua)

        fun pintar() {
            rEn.setImageResource(if (seleccionado == 0) R.drawable.ic_lang_radio_on else R.drawable.ic_lang_radio_off)
            rEs.setImageResource(if (seleccionado == 1) R.drawable.ic_lang_radio_on else R.drawable.ic_lang_radio_off)
            rQu.setImageResource(if (seleccionado == 2) R.drawable.ic_lang_radio_on else R.drawable.ic_lang_radio_off)
        }

        findViewById<View>(R.id.filaIdiomaIngles)?.setOnClickListener { seleccionado = 0; pintar() }
        findViewById<View>(R.id.filaIdiomaEspanol)?.setOnClickListener { seleccionado = 1; pintar() }
        findViewById<View>(R.id.filaIdiomaQuechua)?.setOnClickListener { seleccionado = 2; pintar() }
        findViewById<View>(R.id.btnIdiomaAtras)?.setOnClickListener { finish() }
        findViewById<View>(R.id.btnIdiomaListo)?.setOnClickListener { finish() }

        pintar()
    }
}
