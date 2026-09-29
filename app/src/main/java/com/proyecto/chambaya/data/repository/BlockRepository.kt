package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 12 — Bloqueos y denuncias de usuarios.
 *
 * El id de bloqueo es determinístico (`{blocker}_{blocked}`): las reglas
 * pueden impedir chatear si existe en cualquier dirección.
 */
class BlockRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun blockId(blockerUid: String, blockedUid: String) = "${blockerUid}_${blockedUid}"

    suspend fun block(blockerUid: String, blockedUid: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(blockerUid.isNotBlank() && blockedUid.isNotBlank()) { "Sesión no válida." }
                require(blockerUid != blockedUid) { "No puedes bloquearte a ti mismo." }
                Tasks.await(
                    firestore.collection(COL_BLOCKS).document(blockId(blockerUid, blockedUid))
                        .set(
                            mapOf(
                                "blockerUid" to blockerUid,
                                "blockedUid" to blockedUid,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                )
                Unit
            }
        }

    suspend fun unblock(blockerUid: String, blockedUid: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COL_BLOCKS).document(blockId(blockerUid, blockedUid)).delete()
                )
                Unit
            }
        }

    suspend fun isBlocked(blockerUid: String, blockedUid: String): Boolean =
        withContext(Dispatchers.IO) {
            if (blockerUid.isBlank() || blockedUid.isBlank()) return@withContext false
            runCatching {
                Tasks.await(
                    firestore.collection(COL_BLOCKS).document(blockId(blockerUid, blockedUid)).get()
                ).exists()
            }.getOrDefault(false)
        }

    /** Uids que bloqueé (para filtrar feed y chats). */
    suspend fun myBlocks(uid: String): Result<Set<String>> = withContext(Dispatchers.IO) {
        runCatching {
            if (uid.isBlank()) return@runCatching emptySet()
            Tasks.await(
                firestore.collection(COL_BLOCKS)
                    .whereEqualTo("blockerUid", uid)
                    .limit(200)
                    .get()
            ).documents.mapNotNull { it.getString("blockedUid") }.toSet()
        }
    }

    suspend fun reportUser(
        reporterUid: String,
        reportedUid: String,
        reason: String,
        description: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(reporterUid.isNotBlank() && reportedUid.isNotBlank()) { "Sesión no válida." }
            require(reporterUid != reportedUid) { "No puedes denunciarte a ti mismo." }
            require(reason.isNotBlank()) { "Elige un motivo." }
            Tasks.await(
                firestore.collection(COL_USER_REPORTS).add(
                    mapOf(
                        "reportedUid" to reportedUid,
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
        const val COL_BLOCKS = "user_blocks"
        const val COL_USER_REPORTS = "user_reports"
    }
}
