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

    companion object {
        const val COLLECTION = "notifications"
    }
}
