package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.proyecto.chambaya.data.model.IdentityDocumentTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unicidad global de DNI/RUC — colección `identity_reservations`.
 *
 * Un documento por documento de identidad, id determinístico:
 * `identity_reservations/{TIPO_NUMERO}` (p. ej. `DNI_72345678`).
 * La transacción garantiza que dos cuentas no puedan registrar el mismo
 * documento a la vez: solo la primera reserva consigue el `create`.
 *
 * Privacidad: `get` puntual permitido a autenticados (para comprobar el
 * propio documento), `list` denegado (nadie puede enumerar DNIs). El número
 * completo solo viaja en el id que el usuario ya conoce (su propio DNI/RUC).
 *
 * Casos límite:
 * - Reintento del mismo usuario (registro a medias): si la reserva ya es
 *   mía (`uid` coincide), se considera disponible.
 * - Duplicados legacy (anteriores a esta colección): gana quien primero
 *   reserve; el segundo recibe [DocumentoYaRegistrado] y debe usar otro
 *   documento o recuperar su cuenta original.
 */
class IdentityReservationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Reserva el documento para [uid]. Idempotente para el dueño.
     * Lanza [DocumentoYaRegistrado] si pertenece a otra cuenta.
     */
    suspend fun reserve(documentType: String, documentNumber: String, uid: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tipo = documentType.trim().uppercase()
                val numero = documentNumber.filter(Char::isDigit)
                require(uid.isNotBlank()) { "Sesión no válida." }
                require(esFormatoValido(tipo, numero)) { "Documento no válido." }
                val ref = firestore.collection(COLLECTION).document(reservationId(tipo, numero))
                Tasks.await(
                    firestore.runTransaction { tx ->
                        val snap = tx.get(ref)
                        if (snap.exists()) {
                            val duenio = snap.getString("uid").orEmpty()
                            if (duenio != uid) {
                                throw DocumentoYaRegistrado(tipo, numero)
                            }
                            // Ya es mía: refresco auditoría y listo.
                            tx.update(ref, "updatedAt", FieldValue.serverTimestamp())
                        } else {
                            tx.set(
                                ref,
                                mapOf(
                                    "documentType" to tipo,
                                    "documentNumber" to numero,
                                    "uid" to uid,
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                        null
                    }
                )
                Unit
            }
        }

    /**
     * ¿El documento está libre para [uid]? true si no existe o si ya es mío.
     * Se usa en el sub-paso 1 para avisar temprano sin bloquear.
     */
    suspend fun isAvailable(documentType: String, documentNumber: String, uid: String): Boolean =
        withContext(Dispatchers.IO) {
            val tipo = documentType.trim().uppercase()
            val numero = documentNumber.filter(Char::isDigit)
            if (!esFormatoValido(tipo, numero)) return@withContext false
            runCatching {
                val snap = Tasks.await(
                    firestore.collection(COLLECTION).document(reservationId(tipo, numero)).get()
                )
                !snap.exists() || snap.getString("uid") == uid || uid.isBlank()
            }.getOrDefault(false)
        }

    companion object {
        const val COLLECTION = "identity_reservations"

        fun reservationId(documentType: String, documentNumber: String): String =
            "${documentType.trim().uppercase()}_${documentNumber.filter(Char::isDigit)}"

        fun esFormatoValido(tipo: String, numero: String): Boolean =
            (tipo == IdentityDocumentTypes.DNI && numero.matches(Regex("^[0-9]{8}$"))) ||
                (tipo == IdentityDocumentTypes.RUC && numero.matches(Regex("^[0-9]{11}$")))
    }
}

/** El DNI/RUC ya pertenece a otra cuenta de ChambAYA. */
class DocumentoYaRegistrado(val documentType: String, val documentNumber: String) :
    Exception("Ese $documentType ya está registrado en otra cuenta. Si es tuyo, inicia sesión con esa cuenta.")
