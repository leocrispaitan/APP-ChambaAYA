package com.proyecto.chambaya.data.repository

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.proyecto.chambaya.data.model.Workplace
import com.proyecto.chambaya.data.model.WorkplaceDraft
import com.proyecto.chambaya.data.model.toWorkplace
import com.proyecto.chambaya.data.model.validateWorkplaceDraft
import com.proyecto.chambaya.data.remote.CloudinaryUploader
import com.proyecto.chambaya.R
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
     * si está vacío o el documento enlazado ya no existe, busca por `ownerUid`
     * (migración / enlace roto).
     *
     * Cada paso está aislado: si la lectura del enlace falla (red, documento
     * movido, id inválido), igual se intenta la búsqueda por dueño en vez de
     * devolver `null` y dejar al usuario atascado en "Registra tu lugar".
     *
     * Si la búsqueda por dueño encuentra el lugar pero el enlace estaba roto o
     * ausente, se re-enlaza `employer.workplaceId` como mejor esfuerzo para que
     * la próxima lectura entre por la vía rápida.
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
                    val porEnlace = runCatching {
                        Tasks.await(
                            firestore.collection(COLLECTION_WORKPLACES).document(linkedId).get()
                        ).takeIf { it.exists() }?.toWorkplace()
                    }.onFailure {
                        android.util.Log.w(TAG, "loadByOwner: falló el enlace $linkedId, buscando por dueño", it)
                    }.getOrNull()
                    if (porEnlace != null) return@runCatching porEnlace
                }
                val query = Tasks.await(
                    firestore.collection(COLLECTION_WORKPLACES)
                        .whereEqualTo("ownerUid", uid)
                        .limit(1)
                        .get()
                )
                val encontrado = query.documents.firstOrNull()?.toWorkplace()
                if (encontrado != null && linkedId != encontrado.workplaceId) {
                    runCatching { vincular(uid, encontrado.workplaceId) }
                        .onFailure {
                            android.util.Log.w(TAG, "loadByOwner: no se pudo re-enlazar ${encontrado.workplaceId}", it)
                        }
                }
                encontrado
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
        context: Context,
        uid: String,
        workplaceId: String,
        draft: WorkplaceDraft,
        photoUrl: String = "",
        photoPublicId: String = ""
    ): Result<Workplace> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validateWorkplaceDraft(context, draft)
            require(errores.isEmpty()) { errores.first() }
            require(uid.isNotBlank()) { context.getString(R.string.k_rate_sesion) }
            require(workplaceId.isNotBlank()) { context.getString(R.string.kr_lugar_id) }

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
        context: Context,
        uid: String,
        workplaceId: String,
        draft: WorkplaceDraft,
        photoUrl: String? = null,
        photoPublicId: String? = null
    ): Result<Workplace> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validateWorkplaceDraft(context, draft)
            require(errores.isEmpty()) { errores.first() }
            val ref = firestore.collection(COLLECTION_WORKPLACES).document(workplaceId)
            val actual = Tasks.await(ref.get())
            require(actual.exists()) { context.getString(R.string.kr_lugar_gone) }
            require(actual.getString("ownerUid") == uid) { context.getString(R.string.kr_lugar_ajeno) }

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
    suspend fun delete(context: Context, uid: String, workplaceId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COLLECTION_WORKPLACES).document(workplaceId)
                val actual = Tasks.await(ref.get())
                if (actual.exists()) {
                    require(actual.getString("ownerUid") == uid) { context.getString(R.string.kr_lugar_ajeno) }
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
        // El perfil público muestra el lugar: se re-sincroniza (mejor esfuerzo).
        runCatching { ProfileRepository(firestore).syncPublicProfile(uid) }
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
                runCatching { ProfileRepository(firestore).syncPublicProfile(uid) }
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

    fun validate(context: Context, draft: WorkplaceDraft): List<String> = validateWorkplaceDraft(context, draft)

    companion object {
        const val COLLECTION_WORKPLACES = "workplaces"
        private const val TAG = "WorkplaceRepository"
    }
}
