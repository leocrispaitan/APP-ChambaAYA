package com.proyecto.chambaya.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * FASE 7 — Postulaciones. Colección: applications/{applicationId}
 * Ver CHAMBAYA_IMPLEMENTACION_FASES.md § FASE 7.
 *
 * Anti-duplicado: un solo PENDING por (workerUid + publicationId). Si fue
 * WITHDRAWN o REJECTED, el trabajador puede volver a postularse (nuevo doc).
 */
object ApplicationStatus {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val REJECTED = "REJECTED"
    const val CANCELLED = "CANCELLED"
    const val WITHDRAWN = "WITHDRAWN"

    fun label(status: String): String = when (status) {
        PENDING -> "Pendiente"
        ACCEPTED -> "Aceptado"
        REJECTED -> "Rechazado"
        CANCELLED -> "Cancelado"
        WITHDRAWN -> "Retirado"
        else -> status
    }

    fun isFinal(status: String): Boolean =
        status == REJECTED || status == CANCELLED || status == WITHDRAWN
}

/** Foto del trabajador al momento de postularse (no se pide de nuevo). */
data class ApplicantSnapshot(
    val name: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val experienceYears: Int = 0,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0
)

data class JobApplication(
    val applicationId: String = "",
    val publicationId: String = "",
    val publicationTitle: String = "",
    val workerUid: String = "",
    val employerUid: String = "",
    val status: String = ApplicationStatus.PENDING,
    val worker: ApplicantSnapshot = ApplicantSnapshot(),
    val message: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

object ApplicationLimits {
    const val MESSAGE_MAX = 500
}

fun validateApplicationMessage(message: String): List<String> {
    val errores = mutableListOf<String>()
    if (message.trim().length > ApplicationLimits.MESSAGE_MAX) {
        errores += "El mensaje no puede pasar de ${ApplicationLimits.MESSAGE_MAX} caracteres."
    }
    return errores
}

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toJobApplication(): JobApplication {
    val w = get("worker") as? Map<*, *>
    return JobApplication(
        applicationId = getString("applicationId") ?: id,
        publicationId = getString("publicationId").orEmpty(),
        publicationTitle = getString("publicationTitle").orEmpty(),
        workerUid = getString("workerUid").orEmpty(),
        employerUid = getString("employerUid").orEmpty(),
        status = getString("status") ?: ApplicationStatus.PENDING,
        worker = ApplicantSnapshot(
            name = (w?.get("name") as? String).orEmpty(),
            username = (w?.get("username") as? String).orEmpty(),
            photoUrl = (w?.get("photoUrl") as? String).orEmpty(),
            experienceYears = (w?.get("experienceYears") as? Number)?.toInt() ?: 0,
            ratingAverage = (w?.get("ratingAverage") as? Number)?.toDouble() ?: 0.0,
            ratingCount = (w?.get("ratingCount") as? Number)?.toInt() ?: 0
        ),
        message = getString("message").orEmpty(),
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt")
    )
}
