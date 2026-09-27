package com.proyecto.chambaya.ui.profile

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

/**
 * Avatar con las iniciales de la persona, para cuando todavía no tiene foto.
 *
 * Las cuentas creadas con correo y contraseña nacen sin `profilePhotoUrl`: en
 * lugar del ícono genérico se dibujan sus iniciales sobre un color estable,
 * igual que hace Google. El color no es aleatorio, sale de un hash de la
 * semilla (el `@usuario` o, si no hay, el `uid`), así que la misma persona
 * conserva el mismo color en "Mi Perfil", en Ajustes y en el wizard de edición.
 *
 * Es un `Drawable` y no un `TextView` superpuesto para no tocar ningún layout:
 * los tres avatares son `ShapeableImageView` con `ShapeAppearance.Circle`, así
 * que basta con asignar el drawable y el recorte circular lo aplica la vista.
 */
class InicialesDrawable(
    iniciales: String,
    @param:ColorInt private val colorFondo: Int,
    @param:ColorInt private val colorTexto: Int = Color.WHITE
) : Drawable() {

    /** Iniciales ya normalizadas: sin espacios, en mayúsculas y de 1 a 2 letras. */
    private val letras: List<String> = iniciales
        .filter { it.isLetterOrDigit() }
        .uppercase(Locale.ROOT)
        .take(MAX_LETRAS)
        .map { it.toString() }

    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTexto
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorFondo
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val limites: Rect = bounds
        if (limites.isEmpty) return

        val ancho = limites.width().toFloat()
        val alto = limites.height().toFloat()
        val lado = min(ancho, alto)

        // Relleno completo: el círculo (o el radio que toque) lo recorta la vista.
        canvas.drawRect(
            limites.left.toFloat(),
            limites.top.toFloat(),
            limites.right.toFloat(),
            limites.bottom.toFloat(),
            fondo
        )

        if (letras.isEmpty()) return

        // Una sola inicial ocupa más tamaño que dos dentro de la misma caja.
        texto.textSize = lado * if (letras.size == 1) TAMANO_UNA_LETRA else TAMANO_DOS_LETRAS
        val separacion = texto.textSize * SEPARACION

        val anchoTexto = letras.fold(0f) { suma, letra -> suma + texto.measureText(letra) } +
            separacion * (letras.size - 1)
        val metricas = texto.fontMetrics
        val lineaBase = alto / 2f - (metricas.ascent + metricas.descent) / 2f

        var x = (ancho - anchoTexto) / 2f
        letras.forEach { letra ->
            canvas.drawText(letra, x, lineaBase, texto)
            x += texto.measureText(letra) + separacion
        }
    }

    /** El fondo es opaco y sin filtros: se ignora el alpha que pida la vista. */
    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated en Java; Android ya no lo usa", ReplaceWith(""))
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    companion object {
        private const val MAX_LETRAS = 2
        private const val TAMANO_UNA_LETRA = 0.44f
        private const val TAMANO_DOS_LETRAS = 0.34f
        private const val SEPARACION = 0.08f

        /**
         * Paleta de fondos. Todos son lo bastante oscuros para que el texto
         * blanco se lea, y están en la gama del diseño (azul de marca, violeta
         * del perfil, magenta del header).
         */
        private val PALETA = intArrayOf(
            0xFF2E6FF3.toInt(), // azul de marca
            0xFF6C3CE0.toInt(), // violeta
            0xFFB5359E.toInt(), // magenta
            0xFF0E9F8E.toInt(), // verde azulado
            0xFFE0603A.toInt(), // naranja
            0xFF1F8A70.toInt(), // verde
            0xFFC2410C.toInt(), // naranja oscuro
            0xFF4F46E5.toInt()  // índigo
        )

        /**
         * Iniciales de un nombre: "María Elena Quispe" -> "MQ".
         *
         * Se cogen de las dos primeras palabras con letras y van en mayúsculas.
         * Si solo hay una palabra se usa una inicial, que es lo que hace Google
         * con las cuentas de un solo nombre.
         */
        fun inicialesDe(nombre: String?): String {
            val palabras = (nombre ?: "")
                .split(' ', ' ', '\n', '\t', '-', '_', '/')
                .mapNotNull { palabra ->
                    palabra.firstOrNull { it.isLetterOrDigit() }?.toString()
                }

            return when {
                palabras.isEmpty() -> ""
                palabras.size == 1 -> palabras[0].uppercase(Locale.ROOT)
                else -> (palabras[0] + palabras[1]).uppercase(Locale.ROOT)
            }
        }

        /**
         * Iniciales con cadenas de reserva.
         *
         * Si no hay nombre se cae al correo y, en último caso, al `uid`: mejor
         * una inicial que un avatar vacío.
         */
        fun inicialesDe(nombre: String?, correo: String?, uid: String?): String {
            inicialesDe(nombre).takeIf { it.isNotEmpty() }?.let { return it }
            inicialesDe(correo?.substringBefore('@')).takeIf { it.isNotEmpty() }
                ?.let { return it }
            return uid.orEmpty()
                .firstOrNull { it.isLetterOrDigit() }
                ?.uppercase(Locale.ROOT)
                .orEmpty()
        }

        /** Color estable de una persona: el mismo `uid` da siempre el mismo color. */
        @ColorInt
        fun colorDe(semilla: String?): Int {
            val clave = semilla.orEmpty().trim().lowercase(Locale.ROOT)
            if (clave.isEmpty()) return PALETA.first()
            return PALETA[abs(clave.hashCode()) % PALETA.size]
        }

        /**
         * Drawable de iniciales listo para `setImageDrawable`.
         *
         * @param nombre nombre completo: es lo que da las iniciales.
         * @param semilla lo que decide el color: el `@usuario` o el `uid`.
         */
        fun de(nombre: String?, semilla: String?): InicialesDrawable = InicialesDrawable(
            iniciales = inicialesDe(nombre, null, semilla),
            colorFondo = colorDe(semilla)
        )
    }
}
