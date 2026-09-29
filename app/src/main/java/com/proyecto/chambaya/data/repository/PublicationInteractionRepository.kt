package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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
    suspend fun toggleLike(publicationId: String, uid: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COL_LIKES).document(docId(publicationId, uid))
                val existe = Tasks.await(ref.get()).exists()
                val pubRef = firestore.collection(PublicationRepository.COLLECTION).document(publicationId)
                if (existe) {
                    Tasks.await(ref.delete())
                    runCatching { Tasks.await(pubRef.update("statistics.likes", FieldValue.increment(-1))) }
                    false
                } else {
                    Tasks.await(
                        ref.set(
                            mapOf(
                                "publicationId" to publicationId,
                                "userUid" to uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                    runCatching { Tasks.await(pubRef.update("statistics.likes", FieldValue.increment(1))) }
                    true
                }
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
                val existe = Tasks.await(ref.get()).exists()
                val pubRef = firestore.collection(PublicationRepository.COLLECTION).document(publicationId)
                if (existe) {
                    Tasks.await(ref.delete())
                    runCatching { Tasks.await(pubRef.update("statistics.saves", FieldValue.increment(-1))) }
                    false
                } else {
                    Tasks.await(
                        ref.set(
                            mapOf(
                                "publicationId" to publicationId,
                                "userUid" to uid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                    runCatching { Tasks.await(pubRef.update("statistics.saves", FieldValue.increment(1))) }
                    true
                }
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
