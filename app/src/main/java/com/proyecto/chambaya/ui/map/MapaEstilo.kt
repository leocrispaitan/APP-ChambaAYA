package com.proyecto.chambaya.ui.map

/**
 * Estilo satelital compartido de los mapas (MapTiler híbrido).
 *
 * Vive aquí para no duplicar la URL en cada pantalla que incrusta un mapa
 * (`FragmentoMapas` la trae inline desde antes; las nuevas la toman de aquí).
 */
object MapaEstilo {
    const val MAPTILER_API_KEY = "1Yoce1uTsAXtugiPDqc5"
    const val HIBRIDO_URL =
        "https://api.maptiler.com/maps/hybrid-v4/style.json?key=$MAPTILER_API_KEY"

    /** Ayacucho · Plaza Mayor: centro por defecto si no hay punto previo. */
    const val DEF_LAT = -13.1631
    const val DEF_LNG = -74.2236
}
