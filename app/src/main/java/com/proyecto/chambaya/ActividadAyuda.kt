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

    private val faqs = listOf(
        "¿Cómo postulo a una chamba?" to
            "Abre la oferta en Chambas, revisa el pago, el distrito y lo que piden, y pulsa Postular. El contratante verá tu perfil y te llegará un aviso si te aceptan o no.",
        "¿Cómo publico un trabajo?" to
            "Activa tu modo contratante, registra tu lugar y crea la publicación con título, pago en soles, distrito y fotos. Solo los contratantes verificados pueden publicar.",
        "¿Cómo me pagan?" to
            "El pago se acuerda directo entre las partes: monto, periodo y si es negociable aparecen en la publicación. ChambAYA no retiene ni cobra los pagos.",
        "¿Por qué verifican mi DNI o RUC?" to
            "Para que todos confíen: validamos tu identidad una sola vez con RENIEC o SUNAT. Tu documento nunca se muestra en público, solo tu nombre y tu verificación.",
        "¿Cómo funcionan las calificaciones?" to
            "Solo se califica el trabajo completado, de 1 a 5 estrellas y una sola vez por trabajo en cada dirección. Tu promedio aparece en tu perfil.",
        "¿Cómo reporto o bloqueo a alguien?" to
            "En la publicación o el chat usa Denunciar y en el perfil usa Bloquear. Cada reporte queda en revisión por moderación.",
        "¿Puedo ser trabajador y contratante?" to
            "Sí, con la misma cuenta: tu rol se definió al registrarte y el otro se activa desde tu perfil, sin crear otra cuenta."
    )

    private val filas = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
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
            Toast.makeText(this, "Correo copiado.", Toast.LENGTH_SHORT).show()
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
