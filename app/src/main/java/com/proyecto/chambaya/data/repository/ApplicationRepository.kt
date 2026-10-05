package com.proyecto.chambaya.data.repository

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyecto.chambaya.data.model.ApplicationStatus
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobApplication
import com.proyecto.chambaya.data.model.NotificationType
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.model.PublicationStatus
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.toJobApplication
import com.proyecto.chambaya.data.model.validateApplicationMessage
import com.proyecto.chambaya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 7 — Postulaciones.
 *
 * El trabajador postula/retira; el contratante acepta/rechaza. Al aceptar
 * nace el job (FASE 8), se suma el contratado y se notifica a ambas partes.
 */
data class DecideResult(
    val application: JobApplication,
    val job: Job?,
    /** true si con esta aceptación se cubrieron todas las vacantes. */
    val publicationFilled: Boolean
)

class ApplicationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val jobs: JobRepository = JobRepository(firestore),
    private val publications: PublicationRepository = PublicationRepository(firestore),
    private val notifications: NotificationRepository = NotificationRepository(firestore)
) {

    fun newApplicationId(): String =
        firestore.collection(COLLECTION).document().id

    /** PENDING existente del trabajador en esa publicación (anti-duplicado). */
    suspend fun pendingFor(publicationId: String, workerUid: String): Result<JobApplication?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (publicationId.isBlank() || workerUid.isBlank()) return@runCatching null
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("publicationId", publicationId)
                        .whereEqualTo("workerUid", workerUid)
                        .whereEqualTo("status", ApplicationStatus.PENDING)
                        .limit(1)
                        .get()
                ).documents.firstOrNull()?.toJobApplication()
            }
        }

    /** Última postulación del trabajador en esa publicación (cualquier estado). */
    suspend fun lastFor(publicationId: String, workerUid: String): Result<JobApplication?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (publicationId.isBlank() || workerUid.isBlank()) return@runCatching null
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("publicationId", publicationId)
                        .whereEqualTo("workerUid", workerUid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                ).documents.firstOrNull()?.toJobApplication()
            }
        }

    suspend fun apply(
        context: Context,
        workerUid: String,
        publication: Publication,
        perfil: UserProfile,
        message: String
    ): Result<JobApplication> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validateApplicationMessage(context, message)
            require(errores.isEmpty()) { errores.first() }
            require(workerUid.isNotBlank()) { context.getString(R.string.k_rate_sesion) }
            require(workerUid != publication.ownerUid) { context.getString(R.string.kr_propia) }
            require(publication.status == PublicationStatus.ACTIVE) {
                context.getString(R.string.kr_no_acepta)
            }
            require(pendingFor(publication.publicationId, workerUid).getOrThrow() == null) {
                context.getString(R.string.kr_ya_postulado)
            }
            val ref = firestore.collection(COLLECTION).document()
            val now = FieldValue.serverTimestamp()
            Tasks.await(
                ref.set(
                    mapOf(
                        "applicationId" to ref.id,
                        "publicationId" to publication.publicationId,
                        "publicationTitle" to publication.title,
                        "workerUid" to workerUid,
                        "employerUid" to publication.ownerUid,
                        "status" to ApplicationStatus.PENDING,
                        "worker" to mapOf(
                            "name" to perfil.profile.fullName,
                            "username" to perfil.profile.username,
                            "photoUrl" to perfil.profile.profilePhotoUrl,
                            "experienceYears" to perfil.worker.experienceYears,
                            "ratingAverage" to perfil.worker.ratingAverage,
                            "ratingCount" to perfil.worker.ratingCount
                        ),
                        "message" to message.trim().take(500),
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            )
            // Contador + aviso al contratante (mejor esfuerzo).
            runCatching {
                Tasks.await(
                    firestore.collection(PublicationRepository.COLLECTION)
                        .document(publication.publicationId)
                        .update("statistics.applications", FieldValue.increment(1))
                )
            }
            notifications.push(
                recipientUid = publication.ownerUid,
                type = NotificationType.NEW_APPLICATION,
                title = context.getString(R.string.k_push_nueva_postulacion),
                message = context.getString(
                    R.string.k_push_postulacion_fmt,
                    perfil.profile.fullName.ifBlank { context.getString(R.string.k_push_trabajador) },
                    publication.title.take(60)
                ),
                senderUid = workerUid,
                publicationId = publication.publicationId
            )
            Tasks.await(ref.get()).toJobApplication()
        }
    }

    suspend fun withdraw(context: Context, workerUid: String, applicationId: String): Result<JobApplication> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COLLECTION).document(applicationId)
                val snap = Tasks.await(ref.get())
                require(snap.exists()) { context.getString(R.string.kr_solicitud_gone) }
                val app = snap.toJobApplication()
                require(app.workerUid == workerUid) { context.getString(R.string.kr_solicitud_tuya) }
                require(app.status == ApplicationStatus.PENDING) {
                    context.getString(R.string.kr_solicitud_retiro)
                }
                Tasks.await(
                    ref.update(
                        mapOf(
                            "status" to ApplicationStatus.WITHDRAWN,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                )
                Tasks.await(ref.get()).toJobApplication()
            }
        }

    /**
     * Acepta o rechaza. Al aceptar: crea (o reutiliza) el job, suma el
     * contratado y finaliza la publicación si se cubrieron las vacantes.
     */
    suspend fun decide(context: Context, employerUid: String, applicationId: String, accept: Boolean): Result<DecideResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COLLECTION).document(applicationId)
                val snap = Tasks.await(ref.get())
                require(snap.exists()) { context.getString(R.string.kr_solicitud_gone) }
                var app = snap.toJobApplication()
                require(app.employerUid == employerUid) { context.getString(R.string.kr_solicitud_ajena) }
                require(app.status == ApplicationStatus.PENDING) { context.getString(R.string.kr_solicitud_hecha) }

                if (!accept) {
                    Tasks.await(
                        ref.update(
                            mapOf(
                                "status" to ApplicationStatus.REJECTED,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                    app = Tasks.await(ref.get()).toJobApplication()
                    notifications.push(
                        recipientUid = app.workerUid,
                        type = NotificationType.APPLICATION_REJECTED,
                        title = context.getString(R.string.k_push_no_seleccionado),
                        message = context.getString(R.string.k_push_no_seleccionado_msg, app.publicationTitle.take(60)),
                        senderUid = employerUid,
                        publicationId = app.publicationId
                    )
                    return@runCatching DecideResult(app, null, false)
                }

                val pub = publications.getById(app.publicationId).getOrNull()
                require(pub != null) { context.getString(R.string.kr_pub_gone) }
                require(pub.ownerUid == employerUid) { context.getString(R.string.kr_pub_ajena) }

                // Idempotente: si el job ya existe (reintento), se reutiliza.
                val job = jobs.findByApplication(app.applicationId).getOrNull()
                    ?: jobs.create(
                        employerUid = employerUid,
                        application = app,
                        agreedAmount = pub.payment.amount,
                        publicationTitle = pub.title
                    ).getOrThrow()

                Tasks.await(
                    ref.update(
                        mapOf(
                            "status" to ApplicationStatus.ACCEPTED,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                )
                app = Tasks.await(ref.get()).toJobApplication()

                val hired = pub.workersHired + 1
                runCatching {
                    Tasks.await(
                        firestore.collection(PublicationRepository.COLLECTION)
                            .document(pub.publicationId)
                            .update("workersHired", hired)
                    )
                }
                var filled = false
                if (hired >= pub.workersNeeded) {
                    filled = publications.changeStatus(context, employerUid, pub.publicationId, PublicationStatus.FINISHED)
                        .isSuccess
                }
                notifications.push(
                    recipientUid = app.workerUid,
                    type = NotificationType.APPLICATION_ACCEPTED,
                    title = context.getString(R.string.k_push_seleccionado),
                    message = context.getString(R.string.k_push_seleccionado_msg, app.publicationTitle.take(60)),
                    senderUid = employerUid,
                    publicationId = app.publicationId
                )
                DecideResult(app, job, filled)
            }
        }

    suspend fun listByEmployer(employerUid: String, limit: Long = 50): Result<List<JobApplication>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("employerUid", employerUid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                ).documents.map { it.toJobApplication() }
            }
        }

    suspend fun listByWorker(workerUid: String, limit: Long = 50): Result<List<JobApplication>> =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("workerUid", workerUid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                ).documents.map { it.toJobApplication() }
            }
        }

    companion object {
        const val COLLECTION = "applications"
    }
}
