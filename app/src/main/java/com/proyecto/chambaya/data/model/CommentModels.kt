package com.proyecto.chambaya.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * FASE 10 — Comentarios. Colección: comments/{commentId}
 *
 * Comentar, editar/eliminar el propio, reportar el ajeno.
 */
object CommentStatus {
    const val VISIBLE = "VISIBLE"
}

object CommentLimits {
    const val TEXT_MIN = 1
    const val TEXT_MAX = 500
}

data class PublicationComment(
    val commentId: String = "",
    val publicationId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorPhotoUrl: String = "",
    val text: String = "",
    val status: String = CommentStatus.VISIBLE,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun validateCommentText(context: android.content.Context, text: String): List<String> {
    val errores = mutableListOf<String>()
    if (text.trim().isEmpty()) errores += context.getString(com.proyecto.chambaya.R.string.kv_com_vacio)
    if (text.trim().length > CommentLimits.TEXT_MAX) {
        errores += context.getString(com.proyecto.chambaya.R.string.kv_com_max_fmt, CommentLimits.TEXT_MAX)
    }
    return errores
}

fun DocumentSnapshot.toPublicationComment(): PublicationComment {
    return PublicationComment(
        commentId = getString("commentId") ?: id,
        publicationId = getString("publicationId").orEmpty(),
        authorUid = getString("authorUid").orEmpty(),
        authorName = getString("authorName").orEmpty(),
        authorUsername = getString("authorUsername").orEmpty(),
        authorPhotoUrl = getString("authorPhotoUrl").orEmpty(),
        text = getString("text").orEmpty(),
        status = getString("status") ?: CommentStatus.VISIBLE,
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt")
    )
}
