package com.proyecto.chambaya.data.repository

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.Rating
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.toRating
import com.proyecto.chambaya.data.model.validateRating
import com.proyecto.chambaya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.round

/**
 * FASE 9 — Calificaciones en ambas direcciones, solo con el job COMPLETED
 * y una sola vez por (jobId + fromUid). Actualiza el promedio del evaluado.
 */
class RatingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val notifications = NotificationRepository(firestore)

    suspend fun existingFor(jobId: String, fromUid: String): Result<Rating?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (jobId.isBlank() || fromUid.isBlank()) return@runCatching null
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("jobId", jobId)
                        .whereEqualTo("fromUid", fromUid)
                        .limit(1)
                        .get()
                ).documents.firstOrNull()?.toRating()
            }
        }

    suspend fun receivedBy(uid: String, limit: Long = 20): Result<List<Rating>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("toUid", uid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                ).documents.map { it.toRating() }
            }
        }

    fun listenReceived(
        uid: String,
        limit: Long = 30,
        onUpdate: (List<Rating>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = firestore.collection(COLLECTION)
        .whereEqualTo("toUid", uid)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            runCatching { snapshot.documents.map { it.toRating() } }
                .onSuccess(onUpdate)
                .onFailure { onError(it as? Exception ?: Exception(it)) }
        }

    /** Jobs ya calificados por [fromUid] (para pintar "Calificar" solo si falta). */
    suspend fun ratedJobIds(jobIds: List<String>, fromUid: String): Result<Set<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val out = mutableSetOf<String>()
                if (jobIds.isEmpty() || fromUid.isBlank()) return@runCatching out
                jobIds.distinct().filter { it.isNotBlank() }.chunked(30).forEach { chunk ->
                    Tasks.await(
                        firestore.collection(COLLECTION)
                            .whereIn("jobId", chunk)
                            .whereEqualTo("fromUid", fromUid)
                            .get()
                    ).documents.map { it.toRating() }
                        .filter { it.fromUid == fromUid }
                        .forEach { out += it.jobId }
                }
                out
            }
        }

    suspend fun rate(
        context: Context,
        rater: UserProfile,
        job: Job,
        stars: Int,
        comment: String
    ): Result<Rating> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validateRating(context, stars, comment)
            require(errores.isEmpty()) { errores.first() }
            require(job.status == JobStatus.COMPLETED) {
                context.getString(R.string.kr_rate_solo_fin)
            }
            val fromUid = rater.uid
            require(fromUid == job.workerUid || fromUid == job.employerUid) {
                context.getString(R.string.kr_rate_ajeno)
            }
            require(existingFor(job.jobId, fromUid).getOrThrow() == null) {
                context.getString(R.string.kr_rate_hecho)
            }
            val toUid = if (fromUid == job.workerUid) job.employerUid else job.workerUid
            val toWorker = toUid == job.workerUid
            // El perfil de la contraparte es privado; calcular el agregado desde
            // sus reseñas evita leer users/{uid}, que las reglas reservan al dueño.
            val ratingsBefore = Tasks.await(
                firestore.collection(COLLECTION).whereEqualTo("toUid", toUid).get()
            ).documents.mapNotNull { (it.get("rating") as? Number)?.toInt() }
            val ref = firestore.collection(COLLECTION).document()
            val userRef = firestore.collection(ProfileRepository.COLLECTION_USERS).document(toUid)
            val oldCount = ratingsBefore.size
            val newAvg = round(((ratingsBefore.sum() + stars).toDouble() / (oldCount + 1)) * 10) / 10.0
            val prefix = if (toWorker) "worker" else "employer"
            val batch = firestore.batch().apply {
                set(
                    ref,
                    mapOf(
                        "ratingId" to ref.id,
                        "jobId" to job.jobId,
                        "publicationId" to job.publicationId,
                        "publicationTitle" to job.publicationTitle,
                        "fromUid" to fromUid,
                        "toUid" to toUid,
                        "rating" to stars,
                        "comment" to comment.trim().take(500),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                update(
                    userRef,
                    mapOf(
                        "$prefix.ratingAverage" to newAvg,
                        "$prefix.ratingCount" to oldCount + 1,
                        "statistics.receivedRatingsCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            Tasks.await(batch.commit())
            // Aviso al evaluado (FASE 14).
            notifications.push(
                recipientUid = toUid,
                type = com.proyecto.chambaya.data.model.NotificationType.NEW_RATING,
                title = context.getString(R.string.k_push_rating),
                message = context.getString(
                    R.string.k_push_rating_fmt,
                    stars,
                    if (stars == 1) context.getString(R.string.k_push_estrella)
                    else context.getString(R.string.k_push_estrellas),
                    job.publicationTitle.take(60)
                ),
                senderUid = fromUid,
                publicationId = job.publicationId
            )
            Tasks.await(ref.get()).toRating()
        }
    }

    companion object {
        const val COLLECTION = "ratings"
    }
}
