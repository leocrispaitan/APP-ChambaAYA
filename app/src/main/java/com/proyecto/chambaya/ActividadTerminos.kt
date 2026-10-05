package com.proyecto.chambaya

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Términos de uso ChambAYA — modo claro.
 * Tarjeta resumen elevada + lista con entrada escalonada M3 + feedback táctil.
 * La aceptación es solo visual (no persiste ni bloquea nada).
 */
class ActividadTerminos : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        // Barra de estado del mismo tono que el fondo: la X y el título quedan debajo, nunca encima.
        BarraEstadoUtils.aplicarColor(this, android.graphics.Color.parseColor("#F7F8FA"))
        setContentView(R.layout.actividad_terminos)

        findViewById<View>(R.id.btnTerminosCerrar)?.setOnClickListener { finish() }

        val lista = findViewById<LinearLayout>(R.id.listaTerminos)
        val tarjetaAceptar = findViewById<View>(R.id.tarjetaAceptar)
        val resumen = findViewById<View>(R.id.tarjetaResumen)
        val check = findViewById<CheckBox>(R.id.checkAcepto)
        val continuar = findViewById<MaterialButton>(R.id.btnContinuar)

        // Entrada escalonada: resumen + filas + aceptar (20-25ms por ítem, 250ms, ease-out).
        val animables = mutableListOf<View>()
        resumen?.let { animables += it }
        if (lista != null) {
            for (i in 0 until lista.childCount) {
                val hijo = lista.getChildAt(i)
                if (hijo is LinearLayout) animables += hijo
            }
        }
        tarjetaAceptar?.let { animables += it }
        animables.forEachIndexed { index, v ->
            v.alpha = 0f
            v.translationY = 24f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 25).toLong())
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Feedback táctil en continuar (micro-escala) + ripple del MaterialButton.
        continuar?.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(120).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
            false
        }

        fun pintarBoton() {
            val ok = check?.isChecked == true
            continuar?.isEnabled = ok
            continuar?.alpha = if (ok) 1f else 0.45f
        }

        tarjetaAceptar?.setOnClickListener {
            check?.isChecked = check?.isChecked != true
            pintarBoton()
        }
        check?.setOnCheckedChangeListener { _, _ -> pintarBoton() }
        continuar?.setOnClickListener {
            Toast.makeText(this, "Gracias por aceptar los términos.", Toast.LENGTH_SHORT).show()
            finish()
        }
        pintarBoton()
    }
}
