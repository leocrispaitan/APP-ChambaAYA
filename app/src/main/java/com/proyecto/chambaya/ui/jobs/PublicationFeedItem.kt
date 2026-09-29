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
        list = list.filter { it.publication.category.equals(filters.category, ignoreCase = true) }
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
    list = if (filters.sortNewestFirst) {
        list.sortedByDescending { it.publication.createdAt?.seconds ?: 0 }
    } else {
        list.sortedByDescending { it.likesCount }
    }
    return list
}

fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
    else -> count.toString()
}
