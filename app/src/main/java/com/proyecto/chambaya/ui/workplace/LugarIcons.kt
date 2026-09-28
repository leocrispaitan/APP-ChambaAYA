package com.proyecto.chambaya.ui.workplace

import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.WorkplaceTypes

/**
 * Iconos locales de los 8 tipos de lugar (FASE 4).
 *
 * El `type` está cerrado por las Rules (VIVIENDA…OTRO), así que un `when`
 * exhaustivo basta. El `sector` es texto libre: ese se resuelve contra
 * `api_oficios.json` con [OficioIcons.cargar] y estos iconos son su respaldo.
 */
object LugarIcons {

    /** Drawable local del tipo indicado. */
    fun local(tipo: String?): Int = when (tipo) {
        WorkplaceTypes.VIVIENDA -> R.drawable.ic_lugar_vivienda
        WorkplaceTypes.LOCAL_COMERCIAL -> R.drawable.ic_lugar_local
        WorkplaceTypes.EMPRESA -> R.drawable.ic_lugar_empresa
        WorkplaceTypes.TALLER -> R.drawable.ic_lugar_taller
        WorkplaceTypes.RESTAURANTE -> R.drawable.ic_lugar_restaurante
        WorkplaceTypes.OBRA -> R.drawable.ic_lugar_obra
        WorkplaceTypes.CAMPO -> R.drawable.ic_lugar_campo
        else -> R.drawable.ic_lugar_otro
    }
}
