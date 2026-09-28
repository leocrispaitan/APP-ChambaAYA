package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.proyecto.chambaya.data.model.Workplace
import com.proyecto.chambaya.data.model.WorkplaceDraft
import com.proyecto.chambaya.data.model.toWorkplace
import com.proyecto.chambaya.data.model.validateWorkplaceDraft
import com.proyecto.chambaya.data.remote.CloudinaryUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 4 — Repositorio de `workplaces/{workplaceId}`.
 *
 * Relación 1:1 con el contratante: `users/{uid}.employer.workplaceId`.
 * No toca nada de FASE 1-3 salvo ese campo de enlace.
 *
 * Orden de escritura en [create]:
 *  1. se genera el id sin escribir (`collection().document().id`),
 *  2. se crea `workplaces/{id}` con la foto ya subida a
 *     `chambaya/fotos-lugares/{uid}/{id}`,
 *  3. se enlaza `users/{uid}.employer.workplaceId`.
 * Si el enlace falla, se borra el lugar para no dejar huérfanos.
 */
class WorkplaceRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Lee un lugar por id. `null` si no existe. */
    suspend fun loadById(workplaceId: String): Result<Workplace?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (workplaceId.isBlank()) return@runCatching null
                val snap = Tasks.await(
                    firestore.collection(COLLECTION_WORKPLACES).document(workplaceId).get()
                )
                if (!snap.exists()) null else snap.toWorkplace()
            }
        }

    /**
     * Lugar principal del contratante.
     *
     * Primero mira `users/{uid}.employer.workplaceId` (ruta rápida y barata);
     * si está vacío, busca por `ownerUid` (migración / datos antiguos).
     */
    suspend fun loadByOwner(uid: String): Result<Workplace?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val userSnap = Tasks.await(
                    firestore.collection(ProfileRepository.COLLECTION_USERS).document(uid).get()
                )
                val linkedId = ((userSnap.get("employer") as? Map<*, *>)?.get("workplaceId") as? String)
                    .orEmpty().trim()
                if (linkedId.isNotBlank()) {
                    val snap = Tasks.await(
                        firestore.collection(COLLECTION_WORKPLACES).document(linkedId).get()
                    )
                    if (snap.exists()) return@runCatching snap.toWorkplace()
                }
                val query = Tasks.await(
                    firestore.collection(COLLECTION_WORKPLACES)
                        .whereEqualTo("ownerUid", uid)
                        .limit(1)
                        .get()
                )
                query.documents.firstOrNull()?.toWorkplace()
            }
        }

    /** Genera un id nuevo sin escribir, para subir la foto a su carpeta final. */
    fun newWorkplaceId(): String =
        firestore.collection(COLLECTION_WORKPLACES).document().id

    /**
     * Crea el lugar y lo enlaza al perfil employer.
     *
     * [photoUrl]/[photoPublicId] vienen de [CloudinaryUploader.uploadWorkplacePhoto];
     * vacíos = lugar sin foto (válido).
     */
    suspend fun create(
        uid: String,
        workplaceId: String,
        draft: WorkplaceDraft,
        photoUrl: String = "",
        photoPublicId: String = ""
    ): Result<Workplace> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validateWorkplaceDraft(draft)
            require(errores.isEmpty()) { errores.first() }
            require(uid.isNotBlank()) { "Sesión no válida." }
            require(workplaceId.isNotBlank()) { "Identificador no válido." }

            val ref = firestore.collection(COLLECTION_WORKPLACES).document(workplaceId)
            val payload = draftPayload(uid, workplaceId, draft, photoUrl, photoPublicId)
                .toMutableMap()
            payload["verified"] = false
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["updatedAt"] = FieldValue.serverTimestamp()
            Tasks.await(ref.set(payload))

            // Enlace 1:1 con el contratante. Si falla, se revierte el lugar.
            try {
                vincular(uid, workplaceId)
            } catch (e: Exception) {
                runCatching { Tasks.await(ref.delete()) }
                throw e
            }
            Tasks.await(ref.get()).toWorkplace()
        }
    }

    /** Edita el lugar. No cambia dueño, verificación ni creación. */
    suspend fun update(
        uid: String,
        workplaceId: String,
        draft: WorkplaceDraft,
        photoUrl: String? = null,
        photoPublicId: String? = null
    ): Result<Workplace> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validateWorkplaceDraft(draft)
            require(errores.isEmpty()) { errores.first() }
            val ref = firestore.collection(COLLECTION_WORKPLACES).document(workplaceId)
            val actual = Tasks.await(ref.get())
            require(actual.exists()) { "El lugar ya no existe." }
            require(actual.getString("ownerUid") == uid) { "Ese lugar no te pertenece." }

            val cambios = draftPayload(uid, workplaceId, draft, null, null)
                .toMutableMap()
            // La foto solo se toca si se subió una nueva.
            if (photoUrl != null && photoPublicId != null) {
                cambios["photoUrl"] = photoUrl
                cambios["photoPublicId"] = photoPublicId
                cambios["photoPath"] = if (photoPublicId.isNotBlank()) {
                    CloudinaryUploader.carpetaDeLugar(uid, workplaceId)
                } else ""
            }
            cambios["updatedAt"] = FieldValue.serverTimestamp()
            // Nunca desde la app: dueño, verificación y creación son inmutables.
            cambios.remove("ownerUid")
            cambios.remove("workplaceId")
            cambios.remove("verified")
            cambios.remove("createdAt")
            Tasks.await(ref.update(cambios))
            Tasks.await(ref.get()).toWorkplace()
        }
    }

    /**
     * Elimina el lugar y desenlaza `employer.workplaceId` si apuntaba a él.
     * La foto de Cloudinary se deja: su borrado exige firma (API Secret) y vive
     * en el backend (Fase 16), no en la app.
     */
    suspend fun delete(uid: String, workplaceId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COLLECTION_WORKPLACES).document(workplaceId)
                val actual = Tasks.await(ref.get())
                if (actual.exists()) {
                    require(actual.getString("ownerUid") == uid) { "Ese lugar no te pertenece." }
                    Tasks.await(ref.delete())
                }
                desvincularSiApunta(uid, workplaceId)
            }
        }

    /** `users/{uid}.employer.workplaceId = workplaceId`. */
    suspend fun vincular(uid: String, workplaceId: String) {
        val ref = firestore.collection(ProfileRepository.COLLECTION_USERS).document(uid)
        Tasks.await(
            ref.update(
                mapOf(
                    "employer.workplaceId" to workplaceId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        )
    }

    private suspend fun desvincularSiApunta(uid: String, workplaceId: String) {
        val ref = firestore.collection(ProfileRepository.COLLECTION_USERS).document(uid)
        val snap = runCatching { Tasks.await(ref.get()) }.getOrNull() ?: return
        val actual = ((snap.get("employer") as? Map<*, *>)?.get("workplaceId") as? String).orEmpty()
        if (actual == workplaceId) {
            runCatching {
                Tasks.await(
                    ref.update(
                        mapOf(
                            "employer.workplaceId" to null,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                )
            }
        }
    }

    private fun draftPayload(
        uid: String,
        workplaceId: String,
        draft: WorkplaceDraft,
        photoUrl: String?,
        photoPublicId: String?
    ): Map<String, Any?> {
        val payload = linkedMapOf<String, Any?>(
            "workplaceId" to workplaceId,
            "ownerUid" to uid,
            "name" to draft.name.trim(),
            "type" to draft.type,
            "sector" to draft.sector.trim(),
            "description" to draft.description.trim(),
            "address" to draft.address.trim(),
            "district" to draft.district.trim(),
            "province" to draft.province.trim(),
            "department" to draft.department.trim(),
            "location" to mapOf(
                "latitude" to draft.latitude,
                "longitude" to draft.longitude
            )
        )
        if (photoUrl != null && photoPublicId != null) {
            payload["photoUrl"] = photoUrl
            payload["photoPublicId"] = photoPublicId
            payload["photoPath"] = if (photoPublicId.isNotBlank()) {
                CloudinaryUploader.carpetaDeLugar(uid, workplaceId)
            } else ""
        }
        return payload
    }

    fun validate(draft: WorkplaceDraft): List<String> = validateWorkplaceDraft(draft)

    companion object {
        const val COLLECTION_WORKPLACES = "workplaces"
    }
}
