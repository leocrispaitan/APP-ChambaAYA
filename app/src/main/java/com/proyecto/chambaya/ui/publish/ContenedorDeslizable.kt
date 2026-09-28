package com.proyecto.chambaya.ui.publish

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * FrameLayout que avisa cuando la persona desliza horizontalmente.
 *
 * No intercepta ni consume los toques de sus hijos: solo "escucha" el gesto,
 * así los botones de dentro siguen funcionando igual.
 *
 * [alDeslizar] recibe +1 al deslizar a la izquierda (sección siguiente)
 * y -1 al deslizar a la derecha (sección anterior).
 */
class ContenedorDeslizable @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var alDeslizar: ((direccion: Int) -> Unit)? = null

    private val config = ViewConfiguration.get(context)

    private val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val inicio = e1 ?: return false
                val dx = e2.x - inicio.x
                val dy = e2.y - inicio.y
                val esHorizontal = abs(dx) > abs(dy) * 1.5f
                val esSuficiente = abs(dx) > config.scaledTouchSlop * 4 &&
                    abs(velocityX) > config.scaledMinimumFlingVelocity * 2
                if (esHorizontal && esSuficiente) {
                    alDeslizar?.invoke(if (dx < 0) 1 else -1)
                    return true
                }
                return false
            }
        }
    )

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        detector.onTouchEvent(ev)
        // Los hijos reciben el evento con normalidad. Devolvemos true para seguir
        // recibiendo MOVE/UP aunque se toque una zona vacía (sin vistas clicables).
        super.dispatchTouchEvent(ev)
        return true
    }
}
