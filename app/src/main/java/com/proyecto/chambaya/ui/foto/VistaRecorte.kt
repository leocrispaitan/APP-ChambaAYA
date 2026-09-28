package com.proyecto.chambaya.ui.foto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Vista de recorte **circular** para la foto de perfil.
 *
 * Existe por un motivo concreto: el avatar se pinta en un `ShapeableImageView`
 * con `ShapeAppearance.Circle` y `centerCrop`. Con `centerCrop` una foto
 * vertical de cuerpo entero se recorta sola por el centro y casi siempre deja
 * la cara fuera del círculo, así que el usuario no tiene cómo arreglarlo: no
 * puede decir "centra esto" porque la imagen que él elige no es la que se
 * guarda. Aquí se ve el círculo real, con su rejilla, y lo que queda dentro es
 * exactamente lo que se sube.
 *
 * Decisiones de diseño:
 *
 *  - **El círculo nunca se sale de la imagen.** Tras cada gesto se fuerzan los
 *    límites de desplazamiento para que el cuadrado de recorte quede siempre
 *    cubierto. Sin eso, al ampliar se ven las esquinas vacías y, peor, se
 *    sube una imagen con fondo negro.
 *  - **Ampliar y desplazar anclados al dedo**, no al centro: al pellizcar,
 *    el punto que hay entre los dedos se queda quieto. Es lo que hace que
 *    "centrar la cara" sea preciso en lugar de aproximado.
 *  - **Zoom relativo al encuadre mínimo**, que es justo el que hace que la
 *    imagen tape el círculo. Así el usuario no puede alejarse más de lo
 *    necesario ni acercarse a un estado degenerado.
 *  - **Doble toque** alterna entre el encuadre completo y un acercamiento.
 *
 * La transformación se guarda como escala + desplazamiento, nunca como una
 * [Matrix] arbitraria. Es lo que permite que [exportar] escriba el recorte con
 * una fórmula cerrada y exacta, sin invertir matrices: como solo hay escala
 * uniforme y traslación, la región del recorte en la imagen es siempre un
 * cuadrado, y la salida no se deforma nunca.
 */
class VistaRecorte @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null

    private val matriz = Matrix()

    /** Escala mínima: la que hace que la imagen tape justo el círculo. */
    private var escalaMinima = 1f

    /** Ampliación del usuario sobre [escalaMinima]. 1 = encuadre completo. */
    private var zoom = 1f

    private var desplazamientoX = 0f
    private var desplazamientoY = 0f

    /** Lado del cuadrado (y del círculo) de recorte, en píxeles de la vista. */
    private var ladoRecorte = 0f

    private val centroX: Float get() = width / 2f
    private val centroY: Float get() = height / 2f
    private val escalaActual: Float get() = escalaMinima * zoom

    private val rutaCirculo = Path()

    private val pinturaImagen = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /** Velo que atenúa todo lo que queda fuera del círculo. */
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

    private val detectorEscala = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (bitmap == null) return false
                val nuevo = (zoom * detector.scaleFactor).coerceIn(1f, ZOOM_MAXIMO)
                aplicarZoomAnclando(nuevo, detector.focusX, detector.focusY)
                return true
            }
        }
    )

    private val detectorGesto = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean = true

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (bitmap == null) return false
                // `distanceX/Y` es el desplazamiento del gesto anterior al
                // actual, o sea lo contrario al dedo: se resta para que la
                // imagen siga al dedo.
                desplazamientoX -= distanceX
                desplazamientoY -= distanceY
                ajustarDesplazamientos()
                actualizarMatriz()
                invalidate()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (bitmap == null) return false
                val objetivo = if (zoom > 1.05f) 1f else ZOOM_DOBLE_TOQUE
                aplicarZoomAnclando(objetivo, e.x, e.y)
                return true
            }
        }
    )

    // ═══════════════════════════════════════════════════════════════
    //  API PÚBLICA
    // ═══════════════════════════════════════════════════════════════

    /** Carga la imagen a recortar y vuelve al encuadre completo. */
    fun establecerImagen(origen: Bitmap) {
        bitmap = origen
        reiniciar()
    }

    /** Vuelve al encuadre completo, sin zoom ni desplazamiento. */
    fun reiniciar() {
        zoom = 1f
        desplazamientoX = 0f
        desplazamientoY = 0f
        recalcularEscalaMinima()
        ajustarDesplazamientos()
        actualizarMatriz()
        invalidate()
    }

    /**
     * Lado, en píxeles de la imagen, del cuadrado que queda dentro del círculo.
     *
     * Sirve para no exportar más resolución de la que hay: si el recorte mide
     * 400 px de lado, ampliar la salida a 1024 solo produciría un JPEG más
     * grande de la misma foto borrosa.
     */
    fun ladoUtilDeRecorte(): Int {
        val e = escalaActual
        if (e <= 0f || ladoRecorte <= 0f) return 0
        return (ladoRecorte / e).toInt().coerceAtLeast(1)
    }

    /**
     * Escribe el recorte en un bitmap cuadrado de [lado] px.
     *
     * `null` si todavía no hay imagen o la vista no tiene tamaño.
     *
     * La matriz de la salida sale de la transformación real de la vista, no de
     * invertirla: como la transformación es `escala uniforme + traslación`, la
     * región del círculo es un cuadrado en coordenadas de imagen y basta con
     * escalar ese cuadrado al lienzo de salida.
     */
    fun exportar(lado: Int): Bitmap? {
        val origen = bitmap ?: return null
        if (lado <= 0 || ladoRecorte <= 0f) return null

        val escalaSalida = lado / ladoRecorte
        val salida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        Canvas(salida).apply {
            // Negro por si el recorte no llegara a cubrir todo el lienzo. Con
            // los límites aplicados nunca se ve, pero evita transparencia en
            // un JPEG, que no la admite.
            drawColor(Color.BLACK)

            val m = Matrix()
            m.setScale(escalaSalida * escalaActual, escalaSalida * escalaActual)
            m.postTranslate(
                escalaSalida * (desplazamientoX + ladoRecorte / 2f - escalaActual * origen.width / 2f),
                escalaSalida * (desplazamientoY + ladoRecorte / 2f - escalaActual * origen.height / 2f)
            )
            drawBitmap(origen, m, pinturaImagen)
        }
        return salida
    }

    // ═══════════════════════════════════════════════════════════════
    //  GEOMETRÍA
    // ═══════════════════════════════════════════════════════════════

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        ladoRecorte = min(w, h) * FRACCION_RECORTE
        val radio = ladoRecorte / 2f
        rutaCirculo.reset()
        rutaCirculo.addCircle(centroX, centroY, radio, Path.Direction.CW)
        reiniciar()
    }

    private fun recalcularEscalaMinima() {
        val b = bitmap ?: return
        if (ladoRecorte <= 0f) return
        // La mayor de las dos: la imagen tiene que tapar el círculo por los dos
        // lados, y la escala que más amplía es la que manda.
        escalaMinima = max(ladoRecorte / b.width, ladoRecorte / b.height)
    }

    /**
     * Impide que el círculo se salga de la imagen.
     *
     * La imagen transformada ocupa `[centro - tamaño/2, centro + tamaño/2]`, y
     * para que cubra el círculo hace falta que cada semilado de la imagen sea al
     * menos el semilado del círculo más lo que se haya desplazado. De ahí los
     * topes `±(ladoImagen - ladoRecorte) / 2`.
     */
    private fun ajustarDesplazamientos() {
        val b = bitmap ?: return
        val e = escalaActual
        if (e <= 0f) return
        val topeX = max(0f, (b.width * e - ladoRecorte) / 2f)
        val topeY = max(0f, (b.height * e - ladoRecorte) / 2f)
        desplazamientoX = desplazamientoX.coerceIn(-topeX, topeX)
        desplazamientoY = desplazamientoY.coerceIn(-topeY, topeY)
    }

    private fun actualizarMatriz() {
        val b = bitmap ?: return
        matriz.reset()
        matriz.postTranslate(centroX + desplazamientoX, centroY + desplazamientoY)
        matriz.postScale(escalaActual, escalaActual)
        matriz.postTranslate(-b.width / 2f, -b.height / 2f)
    }

    /**
     * Cambia el zoom dejando fijo el punto [focoX], [focoY] de la vista.
     *
     * Sea `u` la posición respecto del centro, `d` el desplazamiento y `e` la
     * escala, el punto de imagen bajo `u` es `(u - d) / e`. Para que siga bajo
     * `u` al cambiar a `e'`, hace falta `d' = (u - d) (1 - e' / e)`.
     */
    private fun aplicarZoomAnclando(nuevoZoom: Float, focoX: Float, focoY: Float) {
        val escalaPrevia = escalaMinima * zoom
        if (escalaPrevia <= 0f) return

        val factor = 1f - (escalaMinima * nuevoZoom) / escalaPrevia
        val focoRelX = focoX - centroX
        val focoRelY = focoY - centroY

        desplazamientoX = (focoRelX - desplazamientoX) * factor
        desplazamientoY = (focoRelY - desplazamientoY) * factor
        zoom = nuevoZoom

        // El tope manda sobre el anclaje: al llegar al límite, el punto se
        // despega del dedo, que es lo único posible sin salirse de la imagen.
        ajustarDesplazamientos()
        actualizarMatriz()
        invalidate()
    }

    // ═══════════════════════════════════════════════════════════════
    //  DIBUJO
    // ═══════════════════════════════════════════════════════════════

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(COLOR_FONDO)

        val b = bitmap ?: return
        val cx = centroX
        val cy = centroY
        val radio = ladoRecorte / 2f

        // 1) La imagen, solo dentro del círculo: lo que se ve aquí es lo que se sube.
        canvas.save()
        canvas.clipPath(rutaCirculo)
        canvas.drawBitmap(b, matriz, pinturaImagen)
        canvas.restore()

        // 2) Velo fuera del círculo, con los cuatro rectángulos que lo rodean en
        //    vez de `clipOutPath`: funciona igual en API 24-25, donde ese método
        //    todavía no existe, y no necesita capa de composición.
        canvas.save()
        canvas.drawRect(0f, 0f, width.toFloat(), cy - radio, pinturaVelillo)
        canvas.drawRect(0f, cy + radio, width.toFloat(), height.toFloat(), pinturaVelillo)
        canvas.drawRect(0f, cy - radio, cx - radio, cy + radio, pinturaVelillo)
        canvas.drawRect(cx + radio, cy - radio, width.toFloat(), cy + radio, pinturaVelillo)
        canvas.restore()

        // 3) Rejilla de tercios, recortada al círculo para no asomar.
        canvas.save()
        canvas.clipPath(rutaCirculo)
        val salto = ladoRecorte / 3f
        for (i in 1..2) {
            val vertical = cx - radio + salto * i
            canvas.drawLine(vertical, cy - radio, vertical, cy + radio, pinturaRejilla)
            val horizontal = cy - radio + salto * i
            canvas.drawLine(cx - radio, horizontal, cx + radio, horizontal, pinturaRejilla)
        }
        canvas.restore()

        // 4) Borde del círculo, para que se vea el límite exacto del recorte.
        canvas.drawCircle(cx, cy, radio, pinturaBorde)
    }

    // ═══════════════════════════════════════════════════════════════
    //  GESTOS
    // ═══════════════════════════════════════════════════════════════

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Sin esto, un `ScrollView` vertical en el mismo layout se queda con el
        // gesto a mitad de camino y la imagen no llega a desplazarse del todo.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }

        val manejadoGesto = detectorGesto.onTouchEvent(event)
        val manejadoEscala = detectorEscala.onTouchEvent(event)
        return manejadoGesto || manejadoEscala || super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    private fun dp(valor: Float): Float = valor * resources.displayMetrics.density

    private companion object {
        /**
         * Fracción del lado menor de la vista que ocupa el círculo.
         *
         * Antes de 0.82 el círculo se comía demasiado borde: en un móvil alto
         * sobraba espacio vertical y el avatar quedaba pequeño, y el usuario
         * tenía que ampliar para ver nada. Ahora el velillo es una franja fina.
         */
        const val FRACCION_RECORTE = 0.82f

        /** Zoom máximo sobre la escala mínima: 8x ya es un encuadre muy cerrado. */
        const val ZOOM_MAXIMO = 8f

        /** Al que salta el doble toque. */
        const val ZOOM_DOBLE_TOQUE = 2.5f

        const val COLOR_FONDO = 0xFF0B0B0F.toInt()
        const val COLOR_VELLO = 0xA6000000.toInt()
        const val COLOR_REJILLA = 0x4DFFFFFF
    }
}
