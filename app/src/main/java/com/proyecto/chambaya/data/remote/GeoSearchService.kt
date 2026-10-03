package com.proyecto.chambaya.data.remote

import com.proyecto.chambaya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geocode Earth (Pelias): autocompletado y búsqueda de calles, lugares,
 * restaurantes y direcciones — estilo Google Maps.
 *
 * Se usa en:
 *  - el feed de chambas (elegir calle/zona y ver solo lo que hay cerca),
 *  - el mapa (buscar calles/lugares y centrar la zona),
 *  - el registro del local (dirección con coordenadas reales).
 *
 * Notas sobre el servicio (verificado contra la API):
 *  - `boundary.circle` NO está soportado: se filtra por país y el orden por
 *    relevancia llega a devolver primero un "Plaza de Armas" de otra ciudad.
 *    Por eso los resultados se reordenan por distancia al punto de interés.
 *  - `boundary.region` tampoco es válido (solo `boundary.country`).
 *  - El autocompletado excluye la capa `address`; para direcciones exactas
 *    hay que usar `search` (de ahí que `autocomplete` no devuelva jamás
 *    "número de puerta").
 *  - La geometría siempre llega como Point [lon, lat].
 *
 * Límites del plan de prueba: 1000 req/día. Los llamadores usan debounce
 * y `size` pequeño para no quemar cuota.
 */
data class GeoPlace(
    val name: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val layer: String = "",
    val street: String = "",
    val locality: String = "",
    val county: String = "",
    val region: String = ""
) {

    /** Distancia en línea recta desde otro punto (km). */
    fun distanceKmTo(lat: Double, lng: Double): Double =
        GeoDistance.km(latitude, longitude, lat, lng)

    /** Zona más específica disponible: barrio → distrito → región. */
    val zona: String get() = locality.ifBlank { county }.ifBlank { region }

    /**
     * Segunda línea de la sugerencia: "Jr. Tres Mascaras, Huamanga".
     * Si no hay datos propios, cae en la etiqueta cruda de Pelias
     * ("Restaurante La Terraza, AY, Perú").
     */
    val subtitle: String
        get() {
            val partes = mutableListOf<String>()
            if (street.isNotBlank() && !street.equals(name, ignoreCase = true)) partes += street
            if (zona.isNotBlank()) partes += zona
            return when {
                partes.isNotEmpty() -> partes.joinToString(", ")
                !label.equals(name, ignoreCase = true) -> label
                else -> ""
            }
        }
}

/** Haversine en km, sin dependencias de mapas (capa de datos). */
object GeoDistance {
    fun km(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).let { it * it }
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }

    /** "450 m" / "3.2 km". */
    fun format(km: Double): String =
        if (km < 1) "${(km * 1000).toInt()} m" else String.format(java.util.Locale.US, "%.1f km", km)
}

object GeoLayers {
    const val VENUE = "venue"
    const val ADDRESS = "address"
    const val STREET = "street"
    const val LOCALITY = "locality"
    const val NEIGHBOURHOOD = "neighbourhood"
    const val COUNTY = "county"
    const val REGION = "region"

    /** Icono según tipo de resultado. */
    fun iconFor(layer: String): Int = when (layer) {
        VENUE -> R.drawable.ic_pin_restaurant
        LOCALITY, COUNTY, REGION -> R.drawable.ic_home_location
        else -> R.drawable.ic_home_marker
    }

    /** Rótulo corto en español para la fila de sugerencia. */
    fun humanFor(layer: String): String = when (layer) {
        VENUE -> "Lugar"
        ADDRESS -> "Dirección"
        STREET -> "Calle"
        NEIGHBOURHOOD -> "Barrio"
        LOCALITY -> "Ciudad"
        COUNTY -> "Distrito"
        REGION -> "Región"
        else -> "Lugar"
    }
}

class GeoSearchService(
    private val apiKey: String = GEOCODE_EARTH_KEY
) {

    /**
     * Sugerencias mientras se escribe (rápido, tolerante a typos).
     * Sesgo hacia [focusLat]/[focusLng] y Perú.
     */
    suspend fun autocomplete(
        query: String,
        focusLat: Double = AYACUCHO_LAT,
        focusLng: Double = AYACUCHO_LNG,
        limit: Int = 8
    ): Result<List<GeoPlace>> = withContext(Dispatchers.IO) {
        runCatching {
            if (query.trim().length < MIN_CHARS) return@runCatching emptyList()
            val params = "text=${enc(query)}" +
                "&focus.point.lat=$focusLat&focus.point.lon=$focusLng" +
                "&boundary.country=PE&layers=$LAYERS" +
                "&size=${limit.coerceIn(1, 10)}&lang=es"
            parse(get("autocomplete?$params"), focusLat, focusLng)
        }
    }

    /** Búsqueda completa (Enter): devuelve lo mejor + distrito si aplica. */
    suspend fun search(
        query: String,
        focusLat: Double = AYACUCHO_LAT,
        focusLng: Double = AYACUCHO_LNG,
        limit: Int = 8
    ): Result<List<GeoPlace>> = withContext(Dispatchers.IO) {
        runCatching {
            if (query.trim().isEmpty()) return@runCatching emptyList()
            val params = "text=${enc(query)}" +
                "&focus.point.lat=$focusLat&focus.point.lon=$focusLng" +
                "&boundary.country=PE&size=${limit.coerceIn(1, 10)}&lang=es"
            parse(get("search?$params"), focusLat, focusLng)
        }
    }

    /** Calle/lugar de unas coordenadas (etiquetas reales, no solo distrito). */
    suspend fun reverse(latitude: Double, longitude: Double): Result<GeoPlace?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val params = "point.lat=$latitude&point.lon=$longitude&size=1&lang=es"
                parse(get("reverse?$params"), latitude, longitude).firstOrNull()
            }
        }

    // ── HTTP ────────────────────────────────────────────────

    private fun get(pathAndQuery: String): String {
        var conn: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$pathAndQuery&api_key=$apiKey")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ChambAYA-Android")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IllegalStateException("Geocode Earth $code: ${body.take(160)}")
            }
            return body
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Convierte la respuesta en [GeoPlace] y la ordena por cercanía al punto
     * de interés: Pelias prioriza relevancia global y con `boundary.country`
     * una calle homónima de otra ciudad puede salir antes que la de al lado.
     */
    private fun parse(body: String, focusLat: Double, focusLng: Double): List<GeoPlace> {
        val out = mutableListOf<GeoPlace>()
        val features = runCatching { JSONObject(body).optJSONArray("features") }.getOrNull()
            ?: return out
        for (i in 0 until features.length()) {
            val f = features.optJSONObject(i) ?: continue
            val coords = f.optJSONObject("geometry")?.optJSONArray("coordinates")
            if (coords == null || coords.length() < 2) continue
            val p = f.optJSONObject("properties") ?: continue
            out += GeoPlace(
                name = p.optString("name").ifBlank { p.optString("label") },
                label = p.optString("label").ifBlank { p.optString("name") },
                latitude = coords.optDouble(1),
                longitude = coords.optDouble(0),
                layer = p.optString("layer"),
                street = p.optString("street"),
                locality = p.optString("locality"),
                county = p.optString("county"),
                region = p.optString("region")
            )
        }
        return out
            .filter { it.label.isNotBlank() && it.latitude != 0.0 && it.longitude != 0.0 }
            .distinctBy { "${it.latitude},${it.longitude},${it.name}" }
            .sortedBy { it.distanceKmTo(focusLat, focusLng) }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    companion object {
        const val GEOCODE_EARTH_KEY = "ge-5ccb1e4850e35f89"
        private const val BASE_URL = "https://api.geocode.earth/v1"
        private const val TIMEOUT_MS = 12_000
        /** Por debajo de 3 letras Pelias devuelve basura o error de cuota. */
        const val MIN_CHARS = 3
        const val AYACUCHO_LAT = -13.1631
        const val AYACUCHO_LNG = -74.2236

        /** Capas útiles para un app de chambas (fuera marinas/continentes). */
        private const val LAYERS =
            "venue,address,street,intersection,locality,neighbourhood,county,region"
    }
}