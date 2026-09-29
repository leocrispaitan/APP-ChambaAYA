package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyecto.chambaya.data.model.CommentStatus
import com.proyecto.chambaya.data.model.PublicationComment
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.toPublicationComment
import com.proyecto.chambaya.data.model.validateCommentText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 10 — Comentarios de la publicación.
 */
class CommentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun list(publicationId: String, limit: Long = 50): Result<List<PublicationComment>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (publicationId.isBlank()) return@runCatching emptyList()
                Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("publicationId", publicationId)
                        .orderBy("createdAt", Query.Direction.ASCENDING)
                        .limit(limit)
                        .get()
                ).documents.map { it.toPublicationComment() }
                    .filter { it.status == CommentStatus.VISIBLE }
            }
        }

    fun listen(
        publicationId: String,
        limit: Long = 50,
        onUpdate: (List<PublicationComment>) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection(COLLECTION)
            .whereEqualTo("publicationId", publicationId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(limit)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener
                runCatching {
                    snap.documents.map { it.toPublicationComment() }
                        .filter { it.status == CommentStatus.VISIBLE }
                }.onSuccess(onUpdate)
                    .onFailure { onError(it as? Exception ?: Exception(it)) }
            }
    }

    suspend fun add(
        authorUid: String,
        publicationId: String,
        perfil: UserProfile,
        text: String
    ): Result<PublicationComment> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validateCommentText(text)
            require(errores.isEmpty()) { errores.first() }
            require(authorUid.isNotBlank() && publicationId.isNotBlank()) { "Sesión no válida." }
            val ref = firestore.collection(COLLECTION).document()
            val now = FieldValue.serverTimestamp()
            Tasks.await(
                ref.set(
                    mapOf(
                        "commentId" to ref.id,
                        "publicationId" to publicationId,
                        "authorUid" to authorUid,
                        "authorName" to perfil.profile.fullName.ifBlank { perfil.profile.username },
                        "authorUsername" to perfil.profile.username,
                        "authorPhotoUrl" to perfil.profile.profilePhotoUrl,
                        "text" to text.trim().take(500),
                        "status" to CommentStatus.VISIBLE,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            )
            runCatching {
                Tasks.await(
                    firestore.collection(PublicationRepository.COLLECTION)
                        .document(publicationId)
                        .update("statistics.comments", FieldValue.increment(1))
                )
            }
            Tasks.await(ref.get()).toPublicationComment()
        }
    }

    suspend fun edit(authorUid: String, commentId: String, newText: String): Result<PublicationComment> =
        withContext(Dispatchers.IO) {
            runCatching {
                val errores = validateCommentText(newText)
                require(errores.isEmpty()) { errores.first() }
                val ref = firestore.collection(COLLECTION).document(commentId)
                val snap = Tasks.await(ref.get())
                require(snap.exists()) { "El comentario ya no existe." }
                require(snap.getString("authorUid") == authorUid) { "Ese comentario no es tuyo." }
                Tasks.await(
                    ref.update(
                        mapOf(
                            "text" to newText.trim().take(500),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                )
                Tasks.await(ref.get()).toPublicationComment()
            }
        }

    suspend fun delete(authorUid: String, comment: PublicationComment): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(comment.authorUid == authorUid) { "Ese comentario no es tuyo." }
                Tasks.await(firestore.collection(COLLECTION).document(comment.commentId).delete())
                runCatching {
                    Tasks.await(
                        firestore.collection(PublicationRepository.COLLECTION)
                            .document(comment.publicationId)
                            .update("statistics.comments", FieldValue.increment(-1))
                    )
                }
                Unit
            }
        }

    suspend fun report(
        publicationId: String,
        commentId: String,
        reporterUid: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(reason.isNotBlank()) { "Elige un motivo." }
            Tasks.await(
                firestore.collection(PublicationInteractionRepository.COL_REPORTS).add(
                    mapOf(
                        "publicationId" to publicationId,
                        "commentId" to commentId,
                        "reporterUid" to reporterUid,
                        "reason" to reason,
                        "description" to "",
                        "status" to "PENDING",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            Unit
        }
    }

    companion object {
        const val COLLECTION = "comments"
    }
}
