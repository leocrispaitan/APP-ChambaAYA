package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.proyecto.chambaya.data.model.ChatMessage
import com.proyecto.chambaya.data.model.ChatLimits
import com.proyecto.chambaya.data.model.Conversation
import com.proyecto.chambaya.data.model.MessageType
import com.proyecto.chambaya.data.model.NotificationType
import com.proyecto.chambaya.data.model.conversationIdFor
import com.proyecto.chambaya.data.model.toChatMessage
import com.proyecto.chambaya.data.model.toConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 13 — Conversaciones y mensajes en tiempo real.
 */
class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val blocks: BlockRepository = BlockRepository(firestore),
    private val notifications: NotificationRepository = NotificationRepository(firestore)
) {

    /**
     * Abre (o crea) la conversación 1:1. Falla si hay bloqueo en
     * cualquier dirección.
     */
    suspend fun ensureConversation(
        myUid: String,
        otherUid: String,
        publicationId: String = "",
        publicationTitle: String = ""
    ): Result<Conversation> = withContext(Dispatchers.IO) {
        runCatching {
            require(myUid.isNotBlank() && otherUid.isNotBlank() && myUid != otherUid) {
                "Conversación no válida."
            }
            if (blocks.isBlocked(myUid, otherUid)) {
                throw IllegalStateException("Desbloquea a este usuario para chatear.")
            }
            if (blocks.isBlocked(otherUid, myUid)) {
                throw IllegalStateException("No puedes iniciar este chat por ahora.")
            }
            val id = conversationIdFor(publicationId, myUid, otherUid)
            // OJO: no usar get() directo aquí — si el doc no existe, la regla
            // `isParticipant(resource.data)` deniega la lectura. La consulta
            // por campo (allow list abierto) devuelve vacío sin problema.
            val existing = Tasks.await(
                firestore.collection(COLLECTION)
                    .whereEqualTo("conversationId", id)
                    .limit(1)
                    .get()
            ).documents.firstOrNull()?.toConversation()
            if (existing != null) return@runCatching existing
            val ref = firestore.collection(COLLECTION).document(id)
            val now = FieldValue.serverTimestamp()
            val participants = listOf(myUid, otherUid).sorted()
            Tasks.await(
                ref.set(
                    mapOf(
                        "conversationId" to id,
                        "participants" to participants,
                        "publicationId" to publicationId,
                        "publicationTitle" to publicationTitle.take(120),
                        "lastMessage" to "",
                        "lastMessageAt" to now,
                        "createdAt" to now
                    )
                )
            )
            Tasks.await(ref.get()).toConversation()
        }
    }

    fun listenMine(
        uid: String,
        onUpdate: (List<Conversation>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION)
            .whereArrayContains("participants", uid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener
                runCatching { snap.documents.map { it.toConversation() } }
                    .onSuccess(onUpdate)
                    .onFailure { onError(it as? Exception ?: Exception(it)) }
            }
    }

    fun listenMessages(
        conversationId: String,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION).document(conversationId)
            .collection(SUB_MESSAGES)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(200)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener
                runCatching { snap.documents.map { it.toChatMessage() } }
                    .onSuccess(onUpdate)
                    .onFailure { onError(it as? Exception ?: Exception(it)) }
            }
    }

    suspend fun sendMessage(senderUid: String, conversationId: String, text: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = text.trim().take(ChatLimits.TEXT_MAX)
                require(clean.isNotEmpty()) { "Escribe un mensaje." }
                require(senderUid.isNotBlank() && conversationId.isNotBlank()) { "Sesión no válida." }
                val convRef = firestore.collection(COLLECTION).document(conversationId)
                val conv = Tasks.await(convRef.get()).takeIf { it.exists() }?.toConversation()
                    ?: throw IllegalArgumentException("La conversación ya no existe.")
                require(conv.participants.contains(senderUid)) { "Esa conversación no es tuya." }
                val msgRef = convRef.collection(SUB_MESSAGES).document()
                val now = FieldValue.serverTimestamp()
                Tasks.await(
                    msgRef.set(
                        mapOf(
                            "messageId" to msgRef.id,
                            "senderUid" to senderUid,
                            "type" to MessageType.TEXT,
                            "text" to clean,
                            "read" to false,
                            "createdAt" to now
                        )
                    )
                )
                runCatching {
                    Tasks.await(
                        convRef.update(
                            mapOf(
                                "lastMessage" to clean.take(160),
                                "lastMessageAt" to now
                            )
                        )
                    )
                }
                val other = conv.otherUid(senderUid)
                if (other.isNotBlank()) {
                    notifications.push(
                        recipientUid = other,
                        type = NotificationType.NEW_MESSAGE,
                        title = "Nuevo mensaje",
                        message = clean.take(120),
                        senderUid = senderUid,
                        publicationId = conv.publicationId
                    )
                }
                Unit
            }
        }

    /** Marca como leídos los mensajes del otro (lote de hasta 50). */
    suspend fun markRead(uid: String, conversationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val snap = Tasks.await(
                    firestore.collection(COLLECTION).document(conversationId)
                        .collection(SUB_MESSAGES)
                        .whereEqualTo("read", false)
                        .limit(50)
                        .get()
                )
                val batch = firestore.batch()
                var n = 0
                snap.documents.map { it.toChatMessage() }
                    .filter { !it.read && it.senderUid != uid }
                    .forEach {
                        batch.update(
                            firestore.collection(COLLECTION).document(conversationId)
                                .collection(SUB_MESSAGES).document(it.messageId),
                            "read", true
                        )
                        n++
                    }
                if (n > 0) Tasks.await(batch.commit())
                Unit
            }
        }

    /** No leídos de una conversación (para el badge de la lista). */
    suspend fun unreadIn(conversationId: String, uid: String): Int =
        withContext(Dispatchers.IO) {
            runCatching {
                Tasks.await(
                    firestore.collection(COLLECTION).document(conversationId)
                        .collection(SUB_MESSAGES)
                        .whereEqualTo("read", false)
                        .limit(100)
                        .get()
                ).documents.map { it.toChatMessage() }
                    .count { it.senderUid != uid }
            }.getOrDefault(0)
        }

    companion object {
        const val COLLECTION = "conversations"
        const val SUB_MESSAGES = "messages"
    }
}
