package com.proyecto.chambaya.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * FASE 9 — Calificaciones. Colección: ratings/{ratingId}
 *
 * Solo después de completar el job, en ambas direcciones, una sola vez por
 * (jobId + fromUid).
 */
object RatingLimits {
    const val MIN = 1
    const val MAX = 5
    const val COMMENT_MAX = 500
}

data class Rating(
    val ratingId: String = "",
    val jobId: String = "",
    val publicationId: String = "",
    val publicationTitle: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: Timestamp? = null
)

fun validateRating(rating: Int, comment: String): List<String> {
    val errores = mutableListOf<String>()
    if (rating !in RatingLimits.MIN..RatingLimits.MAX) {
        errores += "Elige de 1 a 5 estrellas."
    }
    if (comment.trim().length > RatingLimits.COMMENT_MAX) {
        errores += "El comentario no puede pasar de ${RatingLimits.COMMENT_MAX} caracteres."
    }
    return errores
}

fun DocumentSnapshot.toRating(): Rating {
    return Rating(
        ratingId = getString("ratingId") ?: id,
        jobId = getString("jobId").orEmpty(),
        publicationId = getString("publicationId").orEmpty(),
        publicationTitle = getString("publicationTitle").orEmpty(),
        fromUid = getString("fromUid").orEmpty(),
        toUid = getString("toUid").orEmpty(),
        rating = (get("rating") as? Number)?.toInt() ?: 0,
        comment = getString("comment").orEmpty(),
        createdAt = getTimestamp("createdAt")
    )
}
