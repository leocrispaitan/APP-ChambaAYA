package com.proyecto.chambaya.data.repository

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyecto.chambaya.data.model.AppNotification
import com.proyecto.chambaya.data.model.toAppNotification
import com.proyecto.chambaya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 14 — Bandeja de notificaciones completa.
 *
 * Los repositorios crean avisos con [push] (postulación, chat, calificación,
 * comentario, like, job). La bandeja ([listenMine]) los muestra en vivo y el
 * badge de no leídos sale de [unreadCount] / [listenUnreadCount].
 *
 * Sin push nativo (FCM): la app avisa en primer plano por listeners. El push
 * remoto queda como mejora futura sin cambiar este contrato.
 */
class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun push(
        recipientUid: String,
        type: String,
        title: String,
        message: String,
        senderUid: String,
        publicationId: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (recipientUid.isBlank() || recipientUid == senderUid) return@runCatching
            // `notificationId` se guarda explícito (antes solo vivía en el id del
            // documento): así `toAppNotification()` no depende del fallback.
            val ref = firestore.collection(COLLECTION).document()
            Tasks.await(
                ref.set(
                    mapOf(
                        "notificationId" to ref.id,
                        "recipientUid" to recipientUid,
                        "type" to type,
                        "title" to title.trim().take(120),
                        "message" to message.trim().take(300),
                        "senderUid" to senderUid,
                        "publicationId" to publicationId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            Unit
        }
    }

    /**
     * Aviso de cambio de estado de un trabajo (FASE 8→14).
     * [otherUid] es la contraparte de quien ejecuta la transición.
     */
    suspend fun pushJobEvent(
        context: Context,
        recipientUid: String,
        senderUid: String,
        to: String,
        publicationTitle: String,
        publicationId: String = ""
    ): Result<Unit> {
        val (type, title, message) = when (to) {
            com.proyecto.chambaya.data.model.JobStatus.IN_PROGRESS -> Triple(
                com.proyecto.chambaya.data.model.NotificationType.JOB_IN_PROGRESS,
                context.getString(R.string.k_push_job_curso),
                context.getString(R.string.k_push_job_curso_msg, publicationTitle.take(60))
            )
            com.proyecto.chambaya.data.model.JobStatus.COMPLETED -> Triple(
                com.proyecto.chambaya.data.model.NotificationType.JOB_COMPLETED,
                context.getString(R.string.k_push_job_fin),
                context.getString(R.string.k_push_job_fin_msg, publicationTitle.take(60))
            )
            com.proyecto.chambaya.data.model.JobStatus.CANCELLED -> Triple(
                com.proyecto.chambaya.data.model.NotificationType.JOB_CANCELLED,
                context.getString(R.string.k_push_job_cancel),
                context.getString(R.string.k_push_job_cancel_msg, publicationTitle.take(60))
            )
            else -> return Result.success(Unit)
        }
        return push(recipientUid, type, title, message, senderUid, publicationId)
    }

    suspend fun unreadCount(uid: String, limit: Long = 100): Int =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("recipientUid", uid)
                        .whereEqualTo("read", false)
                        .limit(limit)
                        .get()
                ).size()
            }.getOrDefault(0)
        }

    /**
     * Contador en vivo para el badge (campanita): emite el nº de no leídos
     * cada vez que cambia la bandeja. Sin costo extra de índices (reusa
     * recipientUid + read).
     */
    fun listenUnreadCount(
        uid: String,
        onUpdate: (Int) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection(COLLECTION)
            .whereEqualTo("recipientUid", uid)
            .whereEqualTo("read", false)
            .limit(100)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                onUpdate(snap?.size() ?: 0)
            }
    }

    /**
     * Limpieza de avisos antiguos (p. ej. >90 días): evita que la bandeja
     * crezca sin límite. Se llama al abrir la app, a mejor esfuerzo.
     */
    suspend fun cleanupOld(uid: String, olderThanDays: Long = 90): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (uid.isBlank()) return@runCatching 0
                val limite = com.google.firebase.Timestamp(
                    java.util.Date(System.currentTimeMillis() - olderThanDays * 24 * 60 * 60 * 1000)
                )
                val snap = Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("recipientUid", uid)
                        .whereLessThan("createdAt", limite)
                        .limit(100)
                        .get()
                )
                if (snap.isEmpty) return@runCatching 0
                val batch = firestore.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                Tasks.await(batch.commit())
                snap.size()
            }
        }

    fun listenMine(
        uid: String,
        limit: Long = 30,
        onUpdate: (List<AppNotification>) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection(COLLECTION)
            .whereEqualTo("recipientUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener
                runCatching { snap.documents.map { it.toAppNotification() } }
                    .onSuccess(onUpdate)
                    .onFailure { onError(it as? Exception ?: Exception(it)) }
            }
    }

    suspend fun markAllRead(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val snap = Tasks.await(
                firestore.collection(COLLECTION)
                    .whereEqualTo("recipientUid", uid)
                    .whereEqualTo("read", false)
                    .limit(100)
                    .get()
            )
            val batch = firestore.batch()
            snap.documents.forEach { batch.update(it.reference, "read", true) }
            Tasks.await(batch.commit())
            Unit
        }
    }

    /** Marca una o varias como leídas (true) o no leídas (false). */
    suspend fun setRead(uid: String, ids: List<String>, read: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(uid.isNotBlank() && ids.isNotEmpty()) { "Avisos no válidos." }
            val batch = firestore.batch()
            ids.chunked(400).forEach { lote ->
                lote.forEach { batch.update(firestore.collection(COLLECTION).document(it), "read", read) }
            }
            Tasks.await(batch.commit())
            Unit
        }
    }

    /** Elimina una notificación propia. */
    suspend fun deleteOne(uid: String, notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(uid.isNotBlank() && notificationId.isNotBlank()) { "Aviso no válido." }
            val ref = firestore.collection(COLLECTION).document(notificationId)
            val actual = Tasks.await(ref.get())
            require(actual.exists()) { "El aviso ya no existe." }
            require(actual.getString("recipientUid") == uid) { "Ese aviso no te pertenece." }
            Tasks.await(ref.delete())
            Unit
        }
    }

    /** Elimina todas mis notificaciones (lotes de 100). */
    suspend fun deleteAll(uid: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(uid.isNotBlank()) { "Sesión no válida." }
            val snap = Tasks.await(
                firestore.collection(COLLECTION)
                    .whereEqualTo("recipientUid", uid)
                    .limit(100)
                    .get()
            )
            if (snap.isEmpty) return@runCatching 0
            val batch = firestore.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            Tasks.await(batch.commit())
            snap.size()
        }
    }

    /** Restaura un aviso borrado (botón Deshacer): conserva contenido y hora. */
    suspend fun restore(uid: String, n: AppNotification): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(uid.isNotBlank() && n.recipientUid == uid) { "Aviso no válido." }
            val ref = firestore.collection(COLLECTION).document()
            // senderUid nunca vacío: las reglas exigen sender y recipient no vacíos.
            // Los avisos antiguos podían venir sin sender; se restaura como propio.
            val remitente = n.senderUid.ifBlank { uid }
            Tasks.await(
                ref.set(
                    mapOf(
                        "notificationId" to ref.id,
                        "recipientUid" to uid,
                        "type" to n.type,
                        "title" to n.title,
                        "message" to n.message,
                        "senderUid" to remitente,
                        "publicationId" to n.publicationId,
                        "read" to n.read,
                        "createdAt" to (n.createdAt ?: FieldValue.serverTimestamp())
                    )
                )
            )
            Unit
        }
    }

    companion object {
        const val COLLECTION = "notifications"
    }
}
