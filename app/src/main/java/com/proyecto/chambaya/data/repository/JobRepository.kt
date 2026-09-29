package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.proyecto.chambaya.data.model.AgreedPayment
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobApplication
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.toJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 8 — Trabajos que nacen al aceptar una postulación.
 */
class JobRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun create(
        employerUid: String,
        application: JobApplication,
        agreedAmount: Double,
        publicationTitle: String
    ): Result<Job> = withContext(Dispatchers.IO) {
        runCatching {
            require(employerUid.isNotBlank()) { "Sesión no válida." }
            require(employerUid == application.employerUid) { "Solo el contratante crea el trabajo." }
            require(agreedAmount > 0) { "Monto no válido." }
            val ref = firestore.collection(COLLECTION).document()
            val now = FieldValue.serverTimestamp()
            Tasks.await(
                ref.set(
                    mapOf(
                        "jobId" to ref.id,
                        "applicationId" to application.applicationId,
                        "publicationId" to application.publicationId,
                        "publicationTitle" to publicationTitle,
                        "workerUid" to application.workerUid,
                        "employerUid" to application.employerUid,
                        "status" to JobStatus.ACCEPTED,
                        "startedAt" to null,
                        "completedAt" to null,
                        "agreedPayment" to mapOf("amount" to agreedAmount, "currency" to "PEN"),
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            )
            Tasks.await(ref.get()).toJob()
        }
    }

    suspend fun findByApplication(applicationId: String): Result<Job?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (applicationId.isBlank()) return@runCatching null
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("applicationId", applicationId)
                        .limit(1)
                        .get()
                ).documents.firstOrNull()?.toJob()
            }
        }

    suspend fun findByPublicationAndWorker(publicationId: String, workerUid: String): Result<Job?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (publicationId.isBlank() || workerUid.isBlank()) return@runCatching null
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("publicationId", publicationId)
                        .whereEqualTo("workerUid", workerUid)
                        .limit(1)
                        .get()
                ).documents.firstOrNull()?.toJob()
            }
        }

    suspend fun getById(jobId: String): Result<Job?> = withContext(Dispatchers.IO) {
        runCatching {
            if (jobId.isBlank()) return@runCatching null
            Tasks.await(firestore.collection(COLLECTION).document(jobId).get())
                .takeIf { it.exists() }?.toJob()
        }
    }

    /** Jobs de varias postulaciones de una sola pasada (vista del contratante). */
    suspend fun findByApplicationIds(ids: List<String>): Result<Map<String, Job>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val out = mutableMapOf<String, Job>()
                ids.distinct().filter { it.isNotBlank() }.chunked(30).forEach { chunk ->
                    Tasks.await(
                        firestore.collection(COLLECTION)
                            .whereIn("applicationId", chunk)
                            .get()
                    ).documents.map { it.toJob() }.forEach { out[it.applicationId] = it }
                }
                out
            }
        }

    /** Jobs donde el usuario es contratante (recientes primero). */
    suspend fun listByEmployer(employerUid: String, limit: Long = 100): Result<List<Job>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("employerUid", employerUid)
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                ).documents.map { it.toJob() }
            }
        }

    /** Jobs donde el usuario es trabajador (recientes primero). */
    suspend fun listByWorker(workerUid: String, limit: Long = 100): Result<List<Job>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("workerUid", workerUid)
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                ).documents.map { it.toJob() }
            }
        }

    /**
     * Avanza el estado respetando [JobStatus.nextFrom].
     * Solo participantes; al completar se sella completedAt.
     */
    suspend fun transition(uid: String, jobId: String, to: String): Result<Job> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COLLECTION).document(jobId)
                val snap = Tasks.await(ref.get())
                require(snap.exists()) { "El trabajo ya no existe." }
                val job = snap.toJob()
                require(uid == job.workerUid || uid == job.employerUid) {
                    "Ese trabajo no te involucra."
                }
                require(to in JobStatus.nextFrom(job.status)) {
                    "Ese cambio de estado no es válido."
                }
                val cambios = mutableMapOf<String, Any?>(
                    "status" to to,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (to == JobStatus.IN_PROGRESS && job.startedAt == null) {
                    cambios["startedAt"] = FieldValue.serverTimestamp()
                }
                if (to == JobStatus.COMPLETED) {
                    cambios["completedAt"] = FieldValue.serverTimestamp()
                }
                Tasks.await(ref.update(cambios))
                Tasks.await(ref.get()).toJob()
            }
        }

    companion object {
        const val COLLECTION = "jobs"
    }
}
