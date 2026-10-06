package com.proyecto.chambaya.ui.foto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Vista de recorte **cuadrado** para la foto de perfil.
 *
 * Al revés que un visor con zoom: la imagen se muestra **estática y
 * centrada** (encajada completa, sin recortes), y lo que se mueve es el
 * **marco**: se arrastra con un dedo para colocarlo, se estira desde las
 * esquinas o con pellizco para agrandarlo o encogerlo, y el doble toque lo
 * restablece. Lo que queda dentro del marco es exactamente lo que se sube,
 * y como ese cuadrado luego se muestra con `centerCrop` dentro del círculo
 * del perfil, el resultado es un círculo perfecto y bien centrado.
 *
 * Decisiones de diseño:
 *
 *  - **La imagen no se mueve nunca.** Se dibuja encajada (`fit-center`) una
 *    sola vez; así no hay estado de transformación que se pueda desincronizar
 *    del marco.
 *  - **El marco nunca se sale de la imagen.** Tras cada gesto se sujeta
 *    dentro del área dibujada. Sin eso se subiría una imagen con fondo negro.
 *  - **Tamaño mínimo** para que el recorte siempre sirva como avatar.
 *  - **Exportación exacta**: como el ajuste es escala uniforme + traslación
 *    conocidas, la región del marco en la imagen se calcula con una fórmula
 *    cerrada, sin invertir matrices, y la salida no se deforma nunca.
 */
class VistaRecorte @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null

    /** Ajuste de imagen completa: escala y origen en píxeles de la vista. */
    private var escalaAjuste = 1f
    private var dibujoX = 0f
    private var dibujoY = 0f
    private var dibujoAncho = 0f
    private var dibujoAlto = 0f

    /** Marco de recorte, en píxeles de la vista. Siempre cuadrado. */
    private val marco = RectF()
    private val rutaMarco = Path()

    private val pinturaImagen = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Velo que atenúa todo lo que queda fuera del marco. */
    private val pinturaVelillo = Paint().apply { color = COLOR_VELLO }

    private val pinturaBorde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dp(2f)
    }

    /** Rejilla de tercios, para colocar la cara en un punto de apoyo. */
    private val pinturaRejilla = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = COLOR_REJILLA
        strokeWidth = dp(1f)
    }

    /** Esquinas blancas del marco, como las guías de recorte del sistema. */
    private val pinturaEsquinas = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dp(4f)
        strokeCap = Paint.Cap.BUTT
    }

    // ── Estado del gesto ────────────────────────────────────────────

    private var modo = MODO_NADA
    private var fijoX = 0f
    private var fijoY = 0f
    private var ultimoX = 0f
    private var ultimoY = 0f

    private val detectorEscala = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (bitmap == null) return false
                redimensionarAnclando(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
        }
    )

    private val detectorGesto = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                if (bitmap == null) return false
                ultimoX = e.x
                ultimoY = e.y
                modo = detectarModo(e.x, e.y)
                return modo != MODO_NADA
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (bitmap == null || modo == MODO_NADA) return false
                if (detectorEscala.isInProgress) return false
                val dx = e2.x - ultimoX
                val dy = e2.y - ultimoY
                ultimoX = e2.x
                ultimoY = e2.y
                if (dx == 0f && dy == 0f) return true
                if (modo == MODO_MOVER) {
                    marco.offset(dx, dy)
                } else {
                    val (fx, fy, sx, sy) = esquina(modo)
                    fijoX = fx
                    fijoY = fy
                    redimensionarEsquina(e2.x, e2.y, sx, sy)
                    return true
                }
                sujetarMarco()
                actualizarRuta()
                invalidate()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (bitmap == null) return false
                reiniciar()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                performClick()
                return true
            }
        }
    )

    // ═══════════════════════════════════════════════════════════════
    //  API PÚBLICA
    // ═══════════════════════════════════════════════════════════════

    /** Carga la imagen a recortar y centra el marco a tamaño completo. */
    fun establecerImagen(origen: Bitmap) {
        bitmap = origen
        encajar()
    }

    /** Vuelve al marco completo, centrado en la imagen. */
    fun reiniciar() {
        encajar()
    }

    /**
     * Lado, en píxeles de la imagen, del cuadrado del marco.
     *
     * Sirve para no exportar más resolución de la que hay: si el recorte mide
     * 400 px de lado, ampliar la salida a 1024 solo produciría un JPEG más
     * grande de la misma foto borrosa.
     */
    fun ladoUtilDeRecorte(): Int {
        val s = escalaAjuste
        if (s <= 0f || marco.width() <= 0f) return 0
        return (marco.width() / s).toInt().coerceAtLeast(1)
    }

    /**
     * Escribe el recorte en un bitmap cuadrado de [lado] px.
     *
     * `null` si todavía no hay imagen o la vista no tiene tamaño.
     */
    fun exportar(lado: Int): Bitmap? {
        val origen = bitmap ?: return null
        if (lado <= 0 || marco.width() <= 0f || escalaAjuste <= 0f) return null

        val s = dibujoAncho / origen.width.toFloat()
        val izq = ((marco.left - dibujoX) / s).toInt().coerceIn(0, origen.width - 1)
        val arriba = ((marco.top - dibujoY) / s).toInt().coerceIn(0, origen.height - 1)
        val der = ((marco.right - dibujoX) / s).toInt().coerceIn(izq + 1, origen.width)
        val abajo = ((marco.bottom - dibujoY) / s).toInt().coerceIn(arriba + 1, origen.height)

        val salida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        Canvas(salida).apply {
            // Negro por si el recorte no llegara a cubrir todo el lienzo.
            // Con los límites aplicados nunca se ve, pero evita transparencia
            // en un JPEG, que no la admite.
            drawColor(Color.BLACK)
            drawBitmap(
                origen,
                Rect(izq, arriba, der, abajo),
                Rect(0, 0, lado, lado),
                pinturaImagen
            )
        }
        return salida
    }

    // ═══════════════════════════════════════════════════════════════
    //  GEOMETRÍA
    // ═══════════════════════════════════════════════════════════════

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bitmap != null) encajar()
    }

    /**
     * Encaja la imagen completa y centra el marco a su tamaño máximo.
     *
     * El marco nace ocupando todo el lado menor dibujado: en una foto
     * vertical cubre el ancho, en una horizontal cubre el alto.
     */
    private fun encajar() {
        val b = bitmap ?: return
        if (width == 0 || height == 0 || b.width == 0 || b.height == 0) return
        val s = min(width / b.width.toFloat(), height / b.height.toFloat())
        escalaAjuste = s
        dibujoAncho = b.width * s
        dibujoAlto = b.height * s
        dibujoX = (width - dibujoAncho) / 2f
        dibujoY = (height - dibujoAlto) / 2f
        val lado = min(dibujoAncho, dibujoAlto)
        marco.set(
            (width - lado) / 2f,
            (height - lado) / 2f,
            (width + lado) / 2f,
            (height + lado) / 2f
        )
        actualizarRuta()
        invalidate()
    }

    private fun ladoMaximo(): Float = min(dibujoAncho, dibujoAlto)

    private fun ladoMinimo(): Float =
        max(ladoMaximo() * PROPORCION_MINIMA, dp(LADO_MINIMO_DP))

    /** Lado actual sujeto a [ladoMinimo]..[ladoMaximo]. */
    private fun sujetarMarco() {
        val lado = marco.width().coerceIn(1f, ladoMaximo())
        val izq = marco.left.coerceIn(dibujoX, dibujoX + dibujoAncho - lado)
        val arriba = marco.top.coerceIn(dibujoY, dibujoY + dibujoAlto - lado)
        marco.set(izq, arriba, izq + lado, arriba + lado)
    }

    /** Esquina opuesta (fija) y dirección de la esquina [modo]. */
    private fun esquina(modo: Int): Esquina {
        return when (modo) {
            MODO_ESQ_SI -> Esquina(marco.right, marco.bottom, -1f, -1f)
            MODO_ESQ_SD -> Esquina(marco.left, marco.bottom, 1f, -1f)
            MODO_ESQ_II -> Esquina(marco.right, marco.top, -1f, 1f)
            else -> Esquina(marco.left, marco.top, 1f, 1f)
        }
    }

    /**
     * Estira la esquina móvil hasta [mx], [my] manteniendo el cuadrado.
     *
     * El lado es el mayor de los dos desplazamientos, para que el dedo nunca
     * "pierda" la esquina que arrastra.
     */
    private fun redimensionarEsquina(mx: Float, my: Float, sx: Float, sy: Float) {
        if (dibujoAncho <= 0f || dibujoAlto <= 0f) return
        val deseado = max(sx * (mx - fijoX), sy * (my - fijoY))
        val maxLadoX = if (sx > 0f) dibujoX + dibujoAncho - fijoX else fijoX - dibujoX
        val maxLadoY = if (sy > 0f) dibujoY + dibujoAlto - fijoY else fijoY - dibujoY
        val lado = deseado.coerceIn(ladoMinimo(), min(min(maxLadoX, maxLadoY), ladoMaximo()))
        marco.set(fijoX, fijoY, fijoX + sx * lado, fijoY + sy * lado)
        marco.sort()
        sujetarMarco()
        actualizarRuta()
        invalidate()
    }

    /** Agranda o encoge el marco alrededor del punto de pellizco. */
    private fun redimensionarAnclando(factor: Float, focoX: Float, focoY: Float) {
        if (dibujoAncho <= 0f || dibujoAlto <= 0f) return
        val lado = (marco.width() * factor).coerceIn(ladoMinimo(), ladoMaximo())
        val cx = focoX.coerceIn(dibujoX + lado / 2f, dibujoX + dibujoAncho - lado / 2f)
        val cy = focoY.coerceIn(dibujoY + lado / 2f, dibujoY + dibujoAlto - lado / 2f)
        marco.set(cx - lado / 2f, cy - lado / 2f, cx + lado / 2f, cy + lado / 2f)
        actualizarRuta()
        invalidate()
    }

    /**
     * Decide qué se arrastra: una esquina cercana al dedo, el interior para
     * mover, o nada si se toca fuera del marco.
     */
    private fun detectarModo(x: Float, y: Float): Int {
        val alcance = dp(RADIO_ESQUINA_DP)
        val cercaIzq = kotlin.math.abs(x - marco.left) <= alcance
        val cercaDer = kotlin.math.abs(x - marco.right) <= alcance
        val cercaArriba = kotlin.math.abs(y - marco.top) <= alcance
        val cercaAbajo = kotlin.math.abs(y - marco.bottom) <= alcance
        return when {
            cercaIzq && cercaArriba -> MODO_ESQ_SI
            cercaDer && cercaArriba -> MODO_ESQ_SD
            cercaIzq && cercaAbajo -> MODO_ESQ_II
            cercaDer && cercaAbajo -> MODO_ESQ_ID
            marco.contains(x, y) -> MODO_MOVER
            else -> MODO_NADA
        }
    }

    private fun actualizarRuta() {
        rutaMarco.reset()
        rutaMarco.addRect(marco, Path.Direction.CW)
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIBUJO
    // ═══════════════════════════════════════════════════════════════

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(COLOR_FONDO)

        val b = bitmap ?: return
        val izq = marco.left
        val arriba = marco.top
        val der = marco.right
        val abajo = marco.bottom

        // 1) La imagen completa, estática y centrada.
        canvas.drawBitmap(
            b, null,
            RectF(dibujoX, dibujoY, dibujoX + dibujoAncho, dibujoY + dibujoAlto),
            pinturaImagen
        )

        // 2) Velo fuera del marco, con los cuatro rectángulos que lo rodean en
        //    vez de `clipOutPath`: funciona igual en API 24-25, donde ese
        //    método todavía no existe, y no necesita capa de composición.
        canvas.drawRect(0f, 0f, width.toFloat(), arriba, pinturaVelillo)
        canvas.drawRect(0f, abajo, width.toFloat(), height.toFloat(), pinturaVelillo)
        canvas.drawRect(0f, arriba, izq, abajo, pinturaVelillo)
        canvas.drawRect(der, arriba, width.toFloat(), abajo, pinturaVelillo)

        // 3) Rejilla de tercios, recortada al marco para no asomar.
        canvas.save()
        canvas.clipPath(rutaMarco)
        val salto = marco.width() / 3f
        for (i in 1..2) {
            val vertical = izq + salto * i
            canvas.drawLine(vertical, arriba, vertical, abajo, pinturaRejilla)
            val horizontal = arriba + salto * i
            canvas.drawLine(izq, horizontal, der, horizontal, pinturaRejilla)
        }
        canvas.restore()

        // 4) Borde fino del marco + esquinas gruesas, como la referencia.
        canvas.drawRect(izq, arriba, der, abajo, pinturaBorde)
        dibujarEsquinas(canvas, izq, arriba, der, abajo)
    }

    /**
     * Las cuatro eles de las esquinas. [largo] es la longitud de cada trazo.
     */
    private fun dibujarEsquinas(
        canvas: Canvas,
        izq: Float,
        arriba: Float,
        der: Float,
        abajo: Float
    ) {
        val largo = min(marco.width() * 0.14f, dp(30f))
        // Superior izquierda.
        canvas.drawLine(izq, arriba, izq + largo, arriba, pinturaEsquinas)
        canvas.drawLine(izq, arriba, izq, arriba + largo, pinturaEsquinas)
        // Superior derecha.
        canvas.drawLine(der - largo, arriba, der, arriba, pinturaEsquinas)
        canvas.drawLine(der, arriba, der, arriba + largo, pinturaEsquinas)
        // Inferior izquierda.
        canvas.drawLine(izq, abajo, izq + largo, abajo, pinturaEsquinas)
        canvas.drawLine(izq, abajo, izq, abajo - largo, pinturaEsquinas)
        // Inferior derecha.
        canvas.drawLine(der - largo, abajo, der, abajo, pinturaEsquinas)
        canvas.drawLine(der, abajo, der, abajo - largo, pinturaEsquinas)
    }

    // ═══════════════════════════════════════════════════════════════
    //  GESTOS
    // ═══════════════════════════════════════════════════════════════

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Sin esto, un `ScrollView` vertical en el mismo layout se queda con el
        // gesto a mitad de camino y el marco no llega a desplazarse del todo.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                modo = MODO_NADA
            }
        }

        detectorEscala.onTouchEvent(event)
        val manejadoGesto = detectorGesto.onTouchEvent(event)
        return manejadoGesto || super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    private fun dp(valor: Float): Float = valor * resources.displayMetrics.density

    private data class Esquina(val fijoX: Float, val fijoY: Float, val sx: Float, val sy: Float)

    private companion object {
        const val MODO_NADA = 0
        const val MODO_MOVER = 1
        const val MODO_ESQ_SI = 2
        const val MODO_ESQ_SD = 3
        const val MODO_ESQ_II = 4
        const val MODO_ESQ_ID = 5

        /** Radio táctil para agarrar una esquina. */
        const val RADIO_ESQUINA_DP = 32f

        /** Lado mínimo del marco, en dp. */
        const val LADO_MINIMO_DP = 72f

        /** Fracción del lado dibujado por debajo de la cual no se encoge. */
        const val PROPORCION_MINIMA = 0.2f

        const val COLOR_FONDO = 0xFF0B0B0F.toInt()
        const val COLOR_VELLO = 0xA6000000.toInt()
        const val COLOR_REJILLA = 0x4DFFFFFF
    }
}
