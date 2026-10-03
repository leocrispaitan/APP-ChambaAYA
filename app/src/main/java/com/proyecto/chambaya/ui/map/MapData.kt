package com.proyecto.chambaya.ui.map

import com.proyecto.chambaya.data.model.Publication
import org.maplibre.android.geometry.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mapa funcional: resuelve coordenadas de cada chamba y ordena por cercanía.
 *
 * Fuente de coordenadas (en orden):
 *  1. lat/lng propias de la publicación (cuando existan),
 *  2. coordenadas del lugar/workplace enlazado,
 *  3. centroide del distrito + jitter determinístico (aproximado).
 */
data class MapPin(
    val publication: Publication,
    val position: LatLng,
    val exact: Boolean,
    var distanceKm: Double = -1.0
)

object MapGeo {

    val AYACUCHO = LatLng(-13.1631, -74.2236)

    /** Centroides aproximados (ciudad de Huamanga + cercanías). */
    private val DISTRICTS = mapOf(
        "ayacucho" to LatLng(-13.1631, -74.2236),
        "carmen alto" to LatLng(-13.1764, -74.2185),
        "san juan bautista" to LatLng(-13.1690, -74.2150),
        "jesus nazareno" to LatLng(-13.1580, -74.2220),
        "andres avelino caceres" to LatLng(-13.1500, -74.2100),
        "andrés avelino cáceres" to LatLng(-13.1500, -74.2100),
        "magdalena" to LatLng(-13.1750, -74.2350),
        "huanta" to LatLng(-12.9390, -74.2470),
        "quinua" to LatLng(-13.0430, -74.1360),
        "san miguel" to LatLng(-13.0140, -73.9800),
        "tambo" to LatLng(-13.0833, -74.1833),
        "ica" to LatLng(-14.0670, -75.7286)
    )

    fun centroidOf(district: String): LatLng {
        val key = district.trim().lowercase()
        DISTRICTS[key]?.let { return it }
        return DISTRICTS.entries.firstOrNull { key.contains(it.key) }?.value ?: AYACUCHO
    }

    /** Desplazamiento determinístico ±130m para no apilar pines del mismo distrito. */
    fun jitter(id: String): Pair<Double, Double> {
        val h = id.hashCode()
        val dLat = ((h % 2000).toDouble() / 2000.0 - 0.5) * 0.0024
        val dLng = (((h / 2000) % 2000).toDouble() / 2000.0 - 0.5) * 0.0024
        return dLat to dLng
    }

    fun resolve(
        pub: Publication,
        workplacePos: LatLng?,
        fallback: LatLng = AYACUCHO
    ): MapPin {
        val lat = pub.location.latitude
        val lng = pub.location.longitude
        if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) {
            return MapPin(pub, LatLng(lat, lng), exact = true)
        }
        if (workplacePos != null) {
            return MapPin(pub, workplacePos, exact = true)
        }
        val base = if (pub.location.district.isBlank()) fallback else centroidOf(pub.location.district)
        val (dLat, dLng) = jitter(pub.publicationId.ifBlank { pub.title })
        return MapPin(pub, LatLng(base.latitude + dLat, base.longitude + dLng), exact = false)
    }

    /** Haversine en km. */
    fun distanceKm(a: LatLng, b: LatLng): Double {
        val r = 6371.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val s = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) *
            sin(dLng / 2).pow(2.0)
        return 2 * r * atan2(sqrt(s), sqrt(1 - s))
    }

    fun formatDistance(km: Double): String {
        if (km < 0) return ""
        return if (km < 1) "${(km * 1000).toInt()} m" else String.format("%.1f km", km)
    }
}
