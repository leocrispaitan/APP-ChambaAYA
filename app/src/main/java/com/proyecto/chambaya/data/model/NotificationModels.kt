package com.proyecto.chambaya.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Notificaciones FASE 14 (bandeja completa).
 * Colección: notifications/{notificationId}
 *
 * Las crean los repositorios (postulación, chat, calificación, comentario,
 * like, job) con [NotificationRepository.push]; la bandeja las muestra con
 * [com.proyecto.chambaya.ui.jobs.NotificationsSheet].
 */
object NotificationType {
    const val NEW_APPLICATION = "NEW_APPLICATION"
    const val APPLICATION_ACCEPTED = "APPLICATION_ACCEPTED"
    const val APPLICATION_REJECTED = "APPLICATION_REJECTED"
    const val APPLICATION_WITHDRAWN = "APPLICATION_WITHDRAWN"
    const val JOB_IN_PROGRESS = "JOB_IN_PROGRESS"
    const val JOB_COMPLETED = "JOB_COMPLETED"
    const val JOB_CANCELLED = "JOB_CANCELLED"
    const val NEW_MESSAGE = "NEW_MESSAGE"
    const val NEW_RATING = "NEW_RATING"
    const val NEW_COMMENT = "NEW_COMMENT"
    const val NEW_LIKE = "NEW_LIKE"
    const val PUBLICATION_EXPIRING = "PUBLICATION_EXPIRING"
    const val NEW_NEARBY_PUBLICATION = "NEW_NEARBY_PUBLICATION"

    /** Título por defecto cuando el documento no trae uno. */
    fun defaultTitle(type: String): String = when (type) {
        NEW_APPLICATION -> "Nueva postulación"
        APPLICATION_ACCEPTED -> "¡Fuiste seleccionado!"
        APPLICATION_REJECTED -> "Postulación decidida"
        APPLICATION_WITHDRAWN -> "Postulación retirada"
        JOB_IN_PROGRESS -> "Trabajo en curso"
        JOB_COMPLETED -> "Trabajo completado"
        JOB_CANCELLED -> "Trabajo cancelado"
        NEW_MESSAGE -> "Nuevo mensaje"
        NEW_RATING -> "Nueva calificación"
        NEW_COMMENT -> "Nuevo comentario"
        NEW_LIKE -> "Nuevo me gusta"
        PUBLICATION_EXPIRING -> "Tu chamba vence pronto"
        NEW_NEARBY_PUBLICATION -> "Nueva chamba cerca de ti"
        else -> "Aviso"
    }
}

data class AppNotification(
    val notificationId: String = "",
    val recipientUid: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val senderUid: String = "",
    val publicationId: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

fun DocumentSnapshot.toAppNotification(): AppNotification {
    return AppNotification(
        notificationId = getString("notificationId") ?: id,
        recipientUid = getString("recipientUid").orEmpty(),
        type = getString("type").orEmpty(),
        title = getString("title").orEmpty(),
        message = getString("message").orEmpty(),
        senderUid = getString("senderUid").orEmpty(),
        publicationId = getString("publicationId").orEmpty(),
        read = getBoolean("read") ?: false,
        createdAt = getTimestamp("createdAt")
    )
}
