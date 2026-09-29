package com.proyecto.chambaya.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Calendar
import java.util.Date

/**
 * FASE 13 — Chat en tiempo real.
 *
 * conversations/{conversationId} + messages en subcolección.
 * El id es determinístico (evita duplicados sin consultas extra):
 * `{publicationId}_{uidA}_{uidB}` (uids ordenados) o `dm_…` sin contexto.
 */
object MessageType {
    const val TEXT = "TEXT"
    const val IMAGE = "IMAGE"
    const val LOCATION = "LOCATION"
    const val SYSTEM = "SYSTEM"
}

object ChatLimits {
    const val TEXT_MAX = 1000
}

data class Conversation(
    val conversationId: String = "",
    val participants: List<String> = emptyList(),
    val publicationId: String = "",
    val publicationTitle: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val createdAt: Timestamp? = null
) {
    fun otherUid(me: String): String =
        participants.firstOrNull { it != me } ?: ""
}

data class ChatMessage(
    val messageId: String = "",
    val senderUid: String = "",
    val type: String = MessageType.TEXT,
    val text: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null
)

/** Id determinístico para la conversación 1:1 (evita duplicadas). */
fun conversationIdFor(publicationId: String, a: String, b: String): String {
    require(a.isNotBlank() && b.isNotBlank() && a != b) { "Conversación no válida." }
    val (x, y) = if (a < b) a to b else b to a
    return if (publicationId.isBlank()) "dm_${x}_${y}" else "${publicationId}_${x}_${y}"
}

/** "12:27 PM" hoy · "Ayer" · "12/03/2025". */
fun chatListTime(ts: Timestamp?): String {
    if (ts == null) return ""
    val date = Date(ts.seconds * 1000)
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance()
    cal.time = date
    val sameDay = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    if (sameDay) {
        return android.text.format.DateFormat.format("h:mm a", date).toString()
    }
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    if (cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    ) {
        return "Ayer"
    }
    return android.text.format.DateFormat.format("dd/MM/yyyy", date).toString()
}

/** "2:15 PM" para burbujas. */
fun chatBubbleTime(ts: Timestamp?): String {
    if (ts == null) return ""
    return android.text.format.DateFormat.format("h:mm a", Date(ts.seconds * 1000)).toString()
}

fun DocumentSnapshot.toConversation(): Conversation {
    return Conversation(
        conversationId = getString("conversationId") ?: id,
        participants = (get("participants") as? List<*>)?.mapNotNull { it?.toString() }.orEmpty(),
        publicationId = getString("publicationId").orEmpty(),
        publicationTitle = getString("publicationTitle").orEmpty(),
        lastMessage = getString("lastMessage").orEmpty(),
        lastMessageAt = getTimestamp("lastMessageAt"),
        createdAt = getTimestamp("createdAt")
    )
}

fun DocumentSnapshot.toChatMessage(): ChatMessage {
    return ChatMessage(
        messageId = getString("messageId") ?: id,
        senderUid = getString("senderUid").orEmpty(),
        type = getString("type") ?: MessageType.TEXT,
        text = getString("text").orEmpty(),
        read = getBoolean("read") ?: false,
        createdAt = getTimestamp("createdAt")
    )
}
