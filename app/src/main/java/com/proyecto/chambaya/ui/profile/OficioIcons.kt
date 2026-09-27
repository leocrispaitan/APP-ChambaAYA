package com.proyecto.chambaya.ui.profile

import android.content.Context
import android.widget.ImageView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.load
import coil.request.ImageRequest
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.OficioCatalog

/**
 * FASE 2 — Iconos de los oficios.
 *
 * `api_oficios.json` trae la URL del SVG en Cloudinary, pero mientras la imagen
 * carga (o si falla la red) hace falta un icono local coherente con el diseño.
 * Se resuelve por la CATEGORÍA, que es lo que se guarda en
 * `worker.specialties`, y no por el nombre del puesto.
 */
object OficioIcons {

    /** Drawable local que corresponde a la categoría indicada. */
    fun local(categoria: String): Int {
        val c = OficioCatalog.normalizar(categoria)
        return when {
            c.contains("construccion") -> R.drawable.ic_skill_masonry
            c.contains("pintura") -> R.drawable.ic_skill_paint_roller
            c.contains("jardin") -> R.drawable.ic_skill_gardening
            c.contains("limpieza") -> R.drawable.ic_cat_limpieza
            c.contains("tecnico") || c.contains("electricidad") ||
                c.contains("gasfiteria") || c.contains("plomeria") ->
                R.drawable.ic_cat_tecnico
            c.contains("transporte") || c.contains("delivery") ||
                c.contains("reparto") || c.contains("carga") ->
                R.drawable.ic_cat_delivery
            c.contains("ayudante") || c.contains("general") -> R.drawable.ic_skill_helper
            else -> R.drawable.ic_profile_wrench
        }
    }

    /**
     * Carga el SVG del catálogo en [imageView], con [fallback] mientras tanto.
     *
     * [loader] debe traer `SvgDecoder`; se puede reutilizar uno solo para todos
     * los iconos de una misma pantalla.
     */
    fun cargar(
        imageView: ImageView,
        categoria: String,
        loader: ImageLoader?,
        fallback: Int = local(categoria)
    ) {
        val url = OficioCatalog.iconoDe(imageView.context, categoria)
        if (url == null || loader == null) {
            imageView.load(fallback)
            return
        }
        val request = ImageRequest.Builder(imageView.context)
            .data(url)
            .target(imageView)
            .placeholder(fallback)
            .error(fallback)
            .build()
        loader.enqueue(request)
    }

    /** `ImageLoader` compartido para los SVG del catálogo de oficios. */
    fun nuevoImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()

    /**
     * Avatar del usuario: la foto de Firestore y, cuando no hay foto, sus
     * iniciales.
     *
     * Las cuentas que se registraron con correo y contraseña nunca pasaron por
     * Google, así que `profilePhotoUrl` llega vacío: en ese caso se dibujan las
     * iniciales del nombre. También se usan como `placeholder` y como `error`,
     * para que mientras baja la imagen (o si la URL está rota) no se vea el
     * ícono genérico.
     *
     * @param nombre nombre completo: de aquí salen las iniciales.
     * @param semilla lo que fija el color y la reserva si no hay nombre
     *               (`@usuario` o `uid`).
     */
    fun cargarAvatar(
        imageView: ShapeableImageView,
        url: String,
        nombre: String? = null,
        semilla: String? = null
    ) {
        val iniciales = InicialesDrawable.de(nombre, semilla)
        if (url.isBlank()) {
            imageView.setImageDrawable(iniciales)
            return
        }
        imageView.load(url) {
            placeholder(iniciales)
            error(iniciales)
            crossfade(true)
        }
    }
}
