package com.proyecto.chambaya.ui.jobs

import com.proyecto.chambaya.data.model.Publication

/**
 * FASE 6 — Ítem del feed con estado de interacción del usuario actual.
 *
 * [Publication] es inmutable (viene de Firestore); los flags like/save se
 * guardan aquí para pintarse al instante sin re-consultar.
 */
data class PublicationFeedItem(
    val publication: Publication,
    var liked: Boolean = false,
    var saved: Boolean = false,
    var likesCount: Long = publication.statistics.likes,
    var savesCount: Long = publication.statistics.saves
)

/** Filtros de la Fase 6 (buscador + bottom sheet). */
data class PublicationFilters(
    val query: String = "",
    val category: String = "",
    val district: String = "",
    val minAmount: Double = 0.0,
    val onlyNegotiable: Boolean = false,
    val sortNewestFirst: Boolean = true
) {
    fun isEmpty(): Boolean =
        query.isBlank() && category.isBlank() && district.isBlank() &&
            minAmount <= 0 && !onlyNegotiable
}

/** Aplica [filters] sobre la lista completa (el feed es pequeño: < 100 docs). */
fun List<PublicationFeedItem>.applyFilters(filters: PublicationFilters): List<PublicationFeedItem> {
    val q = filters.query.trim().lowercase()
    var list = this
    if (q.isNotBlank()) {
        list = list.filter {
            val p = it.publication
            p.title.lowercase().contains(q) ||
                p.description.lowercase().contains(q) ||
                p.category.lowercase().contains(q) ||
                p.publisher.name.lowercase().contains(q) ||
                p.skillsRequired.any { s -> s.lowercase().contains(q) }
        }
    }
    if (filters.category.isNotBlank()) {
        list = list.filter { categoriaCoincide(filters.category, it.publication.category) }
    }
    if (filters.district.isNotBlank()) {
        list = list.filter { it.publication.location.district.equals(filters.district, ignoreCase = true) }
    }
    if (filters.minAmount > 0) {
        list = list.filter { it.publication.payment.amount >= filters.minAmount }
    }
    if (filters.onlyNegotiable) {
        list = list.filter { it.publication.payment.negotiable }
    }
    return if (filters.sortNewestFirst) {
        list.sortedWith(
            compareByDescending<PublicationFeedItem> { it.publication.featured }
                .thenByDescending { it.publication.createdAt?.seconds ?: 0 }
        )
    } else {
        list.sortedWith(
            compareByDescending<PublicationFeedItem> { it.publication.featured }
                .thenByDescending { it.likesCount }
        )
    }
}

fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
    else -> count.toString()
}

/**
 * ¿La categoría publicada pasa el filtro elegido?
 *
 * El feed filtra con 20 categorías con foto (Pintor, Albañil…), pero lo
 * publicado guarda las 6 de `api_oficios.json` (Pintura, Construcción…).
 * Con `equals` exacto casi todo daba 0 resultados. Aquí se normaliza
 * (minúsculas, sin tildes) y se agrupa por alias para que Pintor≈Pintura,
 * Albañil≈Construcción, Reparto≈Delivery, etc.
 */
fun categoriaCoincide(filtro: String, publicada: String): Boolean {
    val f = normalizarCategoria(filtro)
    val p = normalizarCategoria(publicada)
    if (f.isBlank() || p.isBlank()) return false
    if (f == p) return true
    if (grupoCategoria(f) == grupoCategoria(p)) return true
    // Contención para variantes ("Limpieza profunda" vs "Limpieza").
    return (f.length >= 4 && p.contains(f)) || (p.length >= 4 && f.contains(p))
}

private fun normalizarCategoria(s: String): String =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(java.util.Locale.ROOT)
        .trim()

/** Clave canónica: las dos taxonomías colapsan al mismo grupo. */
private fun grupoCategoria(n: String): String = when {
    "pint" in n -> "pintor"
    "albanil" in n || "construc" in n -> "albanil"
    "limpieza" in n -> "limpieza"
    "delivery" in n || "reparto" in n -> "delivery"
    "jardin" in n -> "jardineria"
    "mecanica" in n || "mecanico" in n || "lavado" in n || n == "auto" || "autos" in n -> "mecanica"
    "mozo" in n || "mesero" in n -> "mozo"
    "cocina" in n -> "cocina"
    "evento" in n -> "eventos"
    "mudanza" in n || n == "carga" || "carga" in n -> "mudanzas"
    "nino" in n || "nina" in n -> "ninos"
    "adulto" in n -> "adultos"
    "mascota" in n -> "mascotas"
    "venta" in n || "promoc" in n -> "ventas"
    "volante" in n -> "volanteo"
    "tienda" in n -> "tienda"
    "empaque" in n || "almacen" in n -> "almacen"
    "mandado" in n || "compra" in n || "encargo" in n -> "mandados"
    "grass" in n || "cesped" in n || "cesped" in n -> "grass"
    "otro" in n -> "otro"
    else -> n
}
