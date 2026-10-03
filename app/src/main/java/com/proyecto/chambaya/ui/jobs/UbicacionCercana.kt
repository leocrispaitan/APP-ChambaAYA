package com.proyecto.chambaya.ui.jobs

import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.remote.GeoDistance
import com.proyecto.chambaya.data.remote.GeoLayers
import com.proyecto.chambaya.data.remote.GeoPlace
import com.proyecto.chambaya.ui.map.MapGeo
import org.maplibre.android.geometry.LatLng

/**
 * Lugar elegido en el buscador del feed (Geocode Earth): una calle, un
 * restaurante, un mercado o un distrito. Al elegirlo, el feed se recorta a un
 * radio alrededor del punto y se ordena por cercanía.
 */
data class UbicacionActiva(
    val place: GeoPlace,
    val radiusKm: Double = radiusFor(place.layer)
) {
    val nombre: String get() = place.name.ifBlank { place.label }
    val subtitulo: String get() = place.subtitle

    /** Distancia de una chamba a este punto (km). */
    fun distKm(pub: Publication): Double {
        val pos = pub.posicionAproximada()
        return GeoDistance.km(place.latitude, place.longitude, pos.latitude, pos.longitude)
    }

    companion object {
        /** Una calle o un local: radio caminable. */
        const val RADIO_CALLE = 3.5

        /** Un distrito o una ciudad: radio urbano. */
        const val RADIO_LOCALIDAD = 12.0

        /** El radio depende del tipo de resultado de Pelias. */
        fun radiusFor(layer: String): Double = when (layer) {
            GeoLayers.LOCALITY, GeoLayers.COUNTY, GeoLayers.REGION -> RADIO_LOCALIDAD
            else -> RADIO_CALLE
        }
    }
}

/**
 * Coordenadas de la chamba, en el mismo orden de preferencia que el mapa:
 *  1. las propias de la publicación,
 *  2. el centroide del distrito con un jitter determinístico (aproximado).
 */
private fun Publication.posicionAproximada(): LatLng {
    val lat = location.latitude
    val lng = location.longitude
    if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) return LatLng(lat, lng)
    val base = if (location.district.isBlank()) MapGeo.AYACUCHO else MapGeo.centroidOf(location.district)
    val (dLat, dLng) = MapGeo.jitter(publicationId.ifBlank { title })
    return LatLng(base.latitude + dLat, base.longitude + dLng)
}

/**
 * Recorta el feed al radio de [ubicacion] y lo deja por cercanía.
 * Sin ubicación activa devuelve la lista tal cual.
 */
fun List<PublicationFeedItem>.applyLocation(ubicacion: UbicacionActiva?): List<PublicationFeedItem> {
    if (ubicacion == null) return this
    return asSequence()
        .map { it to ubicacion.distKm(it.publication) }
        .filter { (_, km) -> km <= ubicacion.radiusKm }
        .sortedBy { (_, km) -> km }
        .map { (item, _) -> item }
        .toList()
}