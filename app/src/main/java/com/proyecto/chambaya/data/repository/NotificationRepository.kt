package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyecto.chambaya.data.model.AppNotification
import com.proyecto.chambaya.data.model.toAppNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Notificaciones básicas (FASE 7: se crean al postular/aceptar/rechazar;
 * la bandeja completa con push es FASE 14).
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
            Tasks.await(
                firestore.collection(COLLECTION).add(
                    mapOf(
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
            Tasks.await(
                firestore.collection(COLLECTION).add(
                    mapOf(
                        "recipientUid" to uid,
                        "type" to n.type,
                        "title" to n.title,
                        "message" to n.message,
                        "senderUid" to n.senderUid,
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
