package com.proyecto.chambaya

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Centro de Ayuda — modo claro.
 * Buscador local + acordeón animado + tarjeta de contacto.
 * Todo el contenido vive aquí (sin red), el correo es de contacto y se copia al portapapeles.
 */
class ActividadAyuda : AppCompatActivity() {

    private val faqs: List<Pair<String, String>>
        get() = listOf(
            getString(R.string.ayuda_faq_p1) to getString(R.string.ayuda_faq_r1),
            getString(R.string.ayuda_faq_p2) to getString(R.string.ayuda_faq_r2),
            getString(R.string.ayuda_faq_p3) to getString(R.string.ayuda_faq_r3),
            getString(R.string.ayuda_faq_p4) to getString(R.string.ayuda_faq_r4),
            getString(R.string.ayuda_faq_p5) to getString(R.string.ayuda_faq_r5),
            getString(R.string.ayuda_faq_p6) to getString(R.string.ayuda_faq_r6),
            getString(R.string.ayuda_faq_p7) to getString(R.string.ayuda_faq_r7)
        )

    private val filas = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        BarraEstadoUtils.aplicarColor(this, android.graphics.Color.parseColor("#F7F8FA"))
        setContentView(R.layout.actividad_ayuda)

        findViewById<View>(R.id.btnAyudaAtras)?.setOnClickListener { finish() }

        construirFaq("")
        findViewById<EditText>(R.id.campoBuscar)?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                construirFaq(s?.toString().orEmpty())
            }
        })

        findViewById<View>(R.id.btnCopiarCorreo)?.setOnClickListener {
            val clip = ClipData.newPlainText("soporte", CORREO_SOPORTE)
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
            Toast.makeText(this, R.string.ayuda_copiado, Toast.LENGTH_SHORT).show()
        }
    }

    private fun construirFaq(filtro: String) {
        val lista = findViewById<LinearLayout>(R.id.listaFaq) ?: return
        lista.removeAllViews()
        filas.clear()
        val q = filtro.trim().lowercase()
        val visibles = faqs.filter { (p, r) ->
            q.isBlank() || p.lowercase().contains(q) || r.lowercase().contains(q)
        }
        findViewById<View>(R.id.tvFaqVacio)?.visibility =
            if (visibles.isEmpty()) View.VISIBLE else View.GONE
        visibles.forEachIndexed { i, (pregunta, respuesta) ->
            val fila = layoutInflater.inflate(R.layout.item_faq, lista, false)
            fila.findViewById<TextView>(R.id.tvFaqPregunta).text = pregunta
            val txtR = fila.findViewById<TextView>(R.id.tvFaqRespuesta)
            val flecha = fila.findViewById<TextView>(R.id.tvFaqFlecha)
            txtR.text = respuesta
            fila.setOnClickListener {
                // Acordeón con transición suave (ease-out) + giro de flecha.
                TransitionManager.beginDelayedTransition(lista, AutoTransition().apply { duration = 220 })
                val abierto = txtR.visibility == View.VISIBLE
                txtR.visibility = if (abierto) View.GONE else View.VISIBLE
                flecha.animate().rotation(if (abierto) 0f else 90f).setDuration(200).start()
            }
            lista.addView(fila)
            filas += fila
            // Entrada escalonada solo sin filtro (apertura).
            if (q.isBlank()) {
                fila.alpha = 0f
                fila.translationY = 20f
                fila.animate().alpha(1f).translationY(0f)
                    .setStartDelay((i * 25).toLong()).setDuration(250)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }

    companion object {
        const val CORREO_SOPORTE = "soporte@chambaya.pe"
    }
}
