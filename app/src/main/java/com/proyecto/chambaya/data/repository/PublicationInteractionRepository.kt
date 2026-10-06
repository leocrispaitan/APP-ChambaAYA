package com.proyecto.chambaya.data.repository

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.proyecto.chambaya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 6 (acciones del feed) — likes, guardados, "no me interesa" y denuncias.
 *
 * Cada interacción vive en su propia colección (nunca listas dentro del
 * documento) para no crear arreglos que crezcan sin límite:
 *  - publication_likes/{publicationId_uid}
 *  - publication_saves/{publicationId_uid}
 *  - hidden_publications/{publicationId_uid}
 *  - publication_reports (un doc por denuncia)
 *
 * Los contadores statistics.likes / statistics.saves se mantienen con
 * incrementos atómicos en publications/{id}.
 */
class PublicationInteractionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notifications: NotificationRepository = NotificationRepository(firestore)
) {

    private fun docId(publicationId: String, uid: String) = "${publicationId}_${uid}"

    // ── LIKES ────────────────────────────────────────────────

    suspend fun isLiked(publicationId: String, uid: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COL_LIKES).document(docId(publicationId, uid)).get()
                ).exists()
            }.getOrDefault(false)
        }

    /** Alterna el like. Devuelve el estado final (true = con like). */
    suspend fun toggleLike(context: Context, publicationId: String, uid: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COL_LIKES).document(docId(publicationId, uid))
                val pubRef = firestore.collection(PublicationRepository.COLLECTION).document(publicationId)
                // Transacción atómica: el doc de like y el contador se mueven
                // juntos. Sin esto, dos toques rápidos leen el mismo estado y
                // ambos suman (o restan), dejando el contador en +2 / -1.
                // El contador nunca baja de 0 (clamp): publicar nace en 0 y un
                // unlike sin like previo no puede dejarlo en -1.
                val finalState = Tasks.await(
                    firestore.runTransaction { tx ->
                        val likeSnap = tx.get(ref)
                        val pubSnap = tx.get(pubRef)
                        require(pubSnap.exists()) { "La publicación ya no existe." }
                        val stats = pubSnap.get("statistics") as? Map<*, *>
                        val cur = (stats?.get("likes") as? Number)?.toLong() ?: 0L
                        if (likeSnap.exists()) {
                            tx.delete(ref)
                            val nuevo = (cur - 1).coerceAtLeast(0L)
                            tx.update(
                                pubRef,
                                mapOf(
                                    "statistics.likes" to nuevo,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            false
                        } else {
                            tx.set(
                                ref,
                                mapOf(
                                    "publicationId" to publicationId,
                                    "userUid" to uid,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                            tx.update(
                                pubRef,
                                mapOf(
                                    "statistics.likes" to cur + 1,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            true
                        }
                    }
                )
                if (finalState) {
                    // Aviso al dueño (mejor esfuerzo; push ignora si es su propio like).
                    runCatching {
                        val pub = Tasks.await(pubRef.get())
                        val owner = pub.getString("ownerUid").orEmpty()
                        val title = pub.getString("title").orEmpty()
                        if (owner.isNotBlank()) {
                            notifications.push(
                                recipientUid = owner,
                                type = com.proyecto.chambaya.data.model.NotificationType.NEW_LIKE,
                                title = context.getString(R.string.k_push_like),
                                message = context.getString(R.string.k_push_like_fmt, title.take(60)),
                                senderUid = uid,
                                publicationId = publicationId
                            )
                        }
                    }
                }
                finalState
            }
        }

    // ── GUARDADOS ────────────────────────────────────────────

    suspend fun isSaved(publicationId: String, uid: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COL_SAVES).document(docId(publicationId, uid)).get()
                ).exists()
            }.getOrDefault(false)
        }

    suspend fun savedIds(publicationIds: List<String>, uid: String): Set<String> =
        withContext(Dispatchers.IO) {
            if (publicationIds.isEmpty() || uid.isBlank()) return@withContext emptySet()
            val resultado = mutableSetOf<String>()
            // Lecturas puntuales en paralelo (lotes pequeños del feed).
            publicationIds.chunked(10).forEach { lote ->
                lote.map { pid ->
                    runCatching {
                        Tasks.await(firestore.collection(COL_SAVES).document(docId(pid, uid)).get())
                            .takeIf { it.exists() }?.let { resultado += pid }
                    }
                }
            }
            resultado
        }

    suspend fun likedIds(publicationIds: List<String>, uid: String): Set<String> =
        withContext(Dispatchers.IO) {
            if (publicationIds.isEmpty() || uid.isBlank()) return@withContext emptySet()
            val resultado = mutableSetOf<String>()
            publicationIds.chunked(10).forEach { lote ->
                lote.forEach { pid ->
                    runCatching {
                        Tasks.await(firestore.collection(COL_LIKES).document(docId(pid, uid)).get())
                            .takeIf { it.exists() }?.let { resultado += pid }
                    }
                }
            }
            resultado
        }

    /** Ids guardados por mí (para la bandeja de Guardados). */
    suspend fun mySaves(uid: String, limit: Long = 100): Result<Set<String>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (uid.isBlank()) return@runCatching emptySet()
                Tasks.await(
                    firestore.collection(COL_SAVES)
                        .whereEqualTo("userUid", uid)
                        .limit(limit)
                        .get()
                ).documents.mapNotNull { it.getString("publicationId") }.toSet()
            }
        }

    /** Alterna el guardado. Devuelve el estado final (true = guardado). */
    suspend fun toggleSave(publicationId: String, uid: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COL_SAVES).document(docId(publicationId, uid))
                val pubRef = firestore.collection(PublicationRepository.COLLECTION).document(publicationId)
                // Misma atomicidad que toggleLike: evita +2 / -1 con doble tap
                // y clampea el contador a >= 0.
                Tasks.await(
                    firestore.runTransaction { tx ->
                        val saveSnap = tx.get(ref)
                        val pubSnap = tx.get(pubRef)
                        require(pubSnap.exists()) { "La publicación ya no existe." }
                        val stats = pubSnap.get("statistics") as? Map<*, *>
                        val cur = (stats?.get("saves") as? Number)?.toLong() ?: 0L
                        if (saveSnap.exists()) {
                            tx.delete(ref)
                            tx.update(
                                pubRef,
                                mapOf(
                                    "statistics.saves" to (cur - 1).coerceAtLeast(0L),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            false
                        } else {
                            tx.set(
                                ref,
                                mapOf(
                                    "publicationId" to publicationId,
                                    "userUid" to uid,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                            )
                            tx.update(
                                pubRef,
                                mapOf(
                                    "statistics.saves" to cur + 1,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            true
                        }
                    }
                )
            }
        }

    // ── NO ME INTERESA ───────────────────────────────────────

    suspend fun hide(publicationId: String, uid: String, reason: String = "NOT_INTERESTED"): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COL_HIDDEN).document(docId(publicationId, uid))
                        .set(
                            mapOf(
                                "publicationId" to publicationId,
                                "userUid" to uid,
                                "reason" to reason,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                )
                Unit
            }
        }

    suspend fun hiddenIds(publicationIds: List<String>, uid: String): Set<String> =
        withContext(Dispatchers.IO) {
            if (publicationIds.isEmpty() || uid.isBlank()) return@withContext emptySet()
            val resultado = mutableSetOf<String>()
            publicationIds.forEach { pid ->
                runCatching {
                    Tasks.await(firestore.collection(COL_HIDDEN).document(docId(pid, uid)).get())
                        .takeIf { it.exists() }?.let { resultado += pid }
                }
            }
            resultado
        }

    // ── DENUNCIAS ────────────────────────────────────────────

    suspend fun report(
        publicationId: String,
        reporterUid: String,
        reason: String,
        description: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(reason.isNotBlank()) { "Elige un motivo." }
            Tasks.await(
                firestore.collection(COL_REPORTS).add(
                    mapOf(
                        "publicationId" to publicationId,
                        "reporterUid" to reporterUid,
                        "reason" to reason,
                        "description" to description.trim().take(500),
                        "status" to "PENDING",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            Unit
        }
    }

    companion object {
        const val COL_LIKES = "publication_likes"
        const val COL_SAVES = "publication_saves"
        const val COL_HIDDEN = "hidden_publications"
        const val COL_REPORTS = "publication_reports"
    }
}
