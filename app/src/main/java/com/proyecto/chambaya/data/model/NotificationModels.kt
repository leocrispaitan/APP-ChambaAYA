package com.proyecto.chambaya.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Notificaciones básicas (FASE 7 las crea; la bandeja completa es FASE 14).
 * Colección: notifications/{notificationId}
 */
object NotificationType {
    const val NEW_APPLICATION = "NEW_APPLICATION"
    const val APPLICATION_ACCEPTED = "APPLICATION_ACCEPTED"
    const val APPLICATION_REJECTED = "APPLICATION_REJECTED"
    const val JOB_COMPLETED = "JOB_COMPLETED"
    const val NEW_MESSAGE = "NEW_MESSAGE"
    const val NEW_RATING = "NEW_RATING"
    const val NEW_COMMENT = "NEW_COMMENT"
    const val NEW_LIKE = "NEW_LIKE"
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
