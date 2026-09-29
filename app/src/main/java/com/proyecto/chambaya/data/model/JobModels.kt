package com.proyecto.chambaya.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * FASE 8 — Trabajos. Colección: jobs/{jobId}
 *
 * Nace ACCEPTED cuando el contratante acepta una postulación. Flujo:
 * ACCEPTED → IN_PROGRESS → COMPLETED (o CANCELLED en cualquier punto).
 */
object JobStatus {
    const val ACCEPTED = "ACCEPTED"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED = "COMPLETED"
    const val CANCELLED = "CANCELLED"

    fun label(status: String): String = when (status) {
        ACCEPTED -> "Aceptado"
        IN_PROGRESS -> "En curso"
        COMPLETED -> "Completado"
        CANCELLED -> "Cancelado"
        else -> status
    }

    /** Transiciones válidas desde cada estado. */
    fun nextFrom(status: String): List<String> = when (status) {
        ACCEPTED -> listOf(IN_PROGRESS, CANCELLED)
        IN_PROGRESS -> listOf(COMPLETED, CANCELLED)
        else -> emptyList()
    }
}

data class AgreedPayment(
    val amount: Double = 0.0,
    val currency: String = "PEN"
)

data class Job(
    val jobId: String = "",
    val applicationId: String = "",
    val publicationId: String = "",
    val publicationTitle: String = "",
    val workerUid: String = "",
    val employerUid: String = "",
    val status: String = JobStatus.ACCEPTED,
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val agreedPayment: AgreedPayment = AgreedPayment(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun DocumentSnapshot.toJob(): Job {
    val pay = get("agreedPayment") as? Map<*, *>
    return Job(
        jobId = getString("jobId") ?: id,
        applicationId = getString("applicationId").orEmpty(),
        publicationId = getString("publicationId").orEmpty(),
        publicationTitle = getString("publicationTitle").orEmpty(),
        workerUid = getString("workerUid").orEmpty(),
        employerUid = getString("employerUid").orEmpty(),
        status = getString("status") ?: JobStatus.ACCEPTED,
        startedAt = getTimestamp("startedAt"),
        completedAt = getTimestamp("completedAt"),
        agreedPayment = AgreedPayment(
            amount = (pay?.get("amount") as? Number)?.toDouble() ?: 0.0,
            currency = (pay?.get("currency") as? String) ?: "PEN"
        ),
        createdAt = getTimestamp("createdAt"),
        updatedAt = getTimestamp("updatedAt")
    )
}
