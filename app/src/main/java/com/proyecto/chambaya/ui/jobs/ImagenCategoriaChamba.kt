package com.proyecto.chambaya.ui.jobs

import com.proyecto.chambaya.R
import java.text.Normalizer
import java.util.Locale

/** Copia de ImagenCategoriaChamba de ChambAYA-APP-main con el mismo mapeo. */
object ImagenCategoriaChamba {
    fun obtener(categoria: String): Int {
        val clave = Normalizer.normalize(categoria, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.ROOT)

        return when {
            clave == "todas" -> R.drawable.image_mi_primera_chamba_1
            "otro trabajo" in clave -> R.drawable.chamba_otro_trabajo_encargo
            "pintor" in clave || "pintura" in clave -> R.drawable.chamba_pintor
            "albanil" in clave || "albanileria" in clave -> R.drawable.chamba_albanil
            "limpieza" in clave -> R.drawable.chamba_limpieza
            "mozo" in clave || "mesero" in clave -> R.drawable.chamba_mozo
            "cocina" in clave -> R.drawable.chamba_ayudante_cocina
            "evento" in clave -> R.drawable.chamba_atencion_eventos
            "reparto" in clave || "delivery" in clave -> R.drawable.chamba_reparto_delivery
            "mudanza" in clave || "carga" in clave -> R.drawable.chamba_mudanzas_carga
            "nino" in clave || "nina" in clave -> R.drawable.chamba_cuidado_ninos
            "adulto" in clave && "mayor" in clave -> R.drawable.chamba_cuidado_adultos_mayores
            "mascota" in clave -> R.drawable.chamba_cuidado_mascotas
            "venta" in clave || "promocion" in clave -> R.drawable.chamba_ventas_promocion
            "volanteo" in clave -> R.drawable.chamba_volanteo
            "lavado" in clave || "auto" in clave -> R.drawable.chamba_lavado_autos
            "tienda" in clave -> R.drawable.chamba_apoyo_tienda
            "empaque" in clave || "almacen" in clave -> R.drawable.chamba_empaque_almacen
            "mandado" in clave || "compra" in clave -> R.drawable.chamba_mandados_compras
            "jardineria" in clave -> R.drawable.chamba_jardineria_ocasional
            "grass" in clave || "cesped" in clave -> R.drawable.chamba_control_grass
            else -> R.drawable.chamba_otro_trabajo_encargo
        }
    }
}
