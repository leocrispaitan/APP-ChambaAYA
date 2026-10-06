package com.proyecto.chambaya.data.repository

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.model.PublicationDraft
import com.proyecto.chambaya.data.model.PublicationImage
import com.proyecto.chambaya.data.model.PublicationStatus
import com.proyecto.chambaya.data.model.PublicationType
import com.proyecto.chambaya.data.model.PublicationVisibility
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.toPublication
import com.proyecto.chambaya.data.model.validatePublicationDraft
import com.proyecto.chambaya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 5 — Repositorio de publications/{publicationId}.
 *
 * Responsabilidades:
 *  - crear / editar / cambiar estado / eliminar (solo dueño contratante)
 *  - feed público (ACTIVE + PUBLIC) para la Fase 6
 *  - "mis publicaciones" del contratante
 *  - contadores de estadísticas vía incrementos atómicos
 *
 * Los datos del publicador (nombre, foto, verificación...) se copian desde
 * users/{uid} al crear: no se piden de nuevo en el formulario (FASE 5.2).
 */
class PublicationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun newPublicationId(): String =
        firestore.collection(COLLECTION).document().id

    suspend fun create(
        context: Context,
        uid: String,
        publicationId: String,
        draft: PublicationDraft,
        perfil: UserProfile,
        workplaceId: String = "",
        workplaceName: String = "",
        workplacePhotoUrl: String = "",
        workplaceLat: Double? = null,
        workplaceLng: Double? = null,
        images: List<PublicationImage> = emptyList(),
        featured: Boolean = false,
        type: String = PublicationType.JOB_OFFER
    ): Result<Publication> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validatePublicationDraft(context, draft)
            require(errores.isEmpty()) { errores.first() }
            require(uid.isNotBlank()) { context.getString(R.string.k_rate_sesion) }
            require(publicationId.isNotBlank()) { context.getString(R.string.kr_pub_id) }
            require(images.size <= 3) { context.getString(R.string.kr_pub_fotos_max) }
            when (type) {
                PublicationType.JOB_OFFER -> {
                    require(perfil.roles.contains(UserRoles.CONTRATANTE) && perfil.employer.enabled) {
                        context.getString(R.string.kr_pub_empleador)
                    }
                }
                PublicationType.WORKER_AVAILABILITY -> {
                    require(perfil.roles.contains(UserRoles.TRABAJADOR) && perfil.worker.enabled) {
                        context.getString(R.string.kr_pub_empleador)
                    }
                }
                else -> error("Tipo de publicación no válido")
            }
            // Las fotos deben venir de chambaya/fotos-publicaciones/{uid}/{publicationId}/
            // (misma carpeta que CloudinaryUploader.carpetaDePublicacion y firestore.rules).
            val prefijo = "chambaya/fotos-publicaciones/$uid/$publicationId/"
            require(images.all { it.url.contains(prefijo) && it.publicId.startsWith(prefijo) }) {
                context.getString(R.string.kr_pub_fotos_malas)
            }

            // FASE 5.2 — Identidad del publicador: la ENTIDAD (lugar/negocio)
            // cuando existe; el nombre personal solo como respaldo si el
            // contratante aún no registró su entidad.
            val nombreEntidad = workplaceName.trim()
            val nombrePublicador = if (type == PublicationType.WORKER_AVAILABILITY) {
                perfil.profile.fullName
            } else {
                nombreEntidad.ifBlank { perfil.employer.businessName.ifBlank { perfil.profile.fullName } }
            }
            val fotoPublicador = workplacePhotoUrl.ifBlank { perfil.profile.profilePhotoUrl }

            val ref = firestore.collection(COLLECTION).document(publicationId)
            val now = FieldValue.serverTimestamp()
            val payload = mutableMapOf<String, Any?>(
                "publicationId" to publicationId,
                "ownerUid" to uid,
                "status" to PublicationStatus.ACTIVE,
                "visibility" to PublicationVisibility.PUBLIC,
                "type" to type,
                "title" to draft.title.trim(),
                "description" to draft.description.trim(),
                "category" to draft.category.trim(),
                "subcategory" to "",
                "skillsRequired" to draft.skillsRequired.map { it.trim() }.filter { it.isNotEmpty() },
                "payment" to mapOf(
                    "amount" to draft.amount,
                    "currency" to "PEN",
                    "period" to draft.period,
                    "negotiable" to draft.negotiable
                ),
                "schedule" to mapOf(
                    "startDate" to null,
                    "endDate" to null,
                    "startTime" to draft.startTime.trim(),
                    "endTime" to draft.endTime.trim()
                ),
                "workersNeeded" to draft.workersNeeded,
                "requiresExperience" to draft.requiresExperience,
                "workersHired" to 0,
                "location" to mapOf(
                    "district" to draft.district.trim(),
                    "province" to perfil.profile.province.ifBlank { "Huamanga" },
                    "department" to perfil.profile.department.ifBlank { "Ayacucho" },
                    // GPS del local: el pin del mapa queda fijo desde que nace.
                    "latitude" to workplaceLat,
                    "longitude" to workplaceLng,
                    "exactAddress" to draft.exactAddress.trim()
                ),
                "workplaceId" to workplaceId,
                "workplaceName" to workplaceName,
                "images" to images.map { mapOf("url" to it.url, "publicId" to it.publicId) },
                "publisher" to mapOf(
                    "uid" to uid,
                    "name" to nombrePublicador,
                    "username" to perfil.profile.username,
                    "photoUrl" to fotoPublicador,
                    "verified" to perfil.identity.identityVerified,
                    "employerType" to if (type == PublicationType.JOB_OFFER) perfil.employer.employerType else "",
                    "sector" to if (type == PublicationType.JOB_OFFER) perfil.employer.sector else ""
                ),
                "statistics" to mapOf(
                    "views" to 0, "likes" to 0, "comments" to 0,
                    "shares" to 0, "saves" to 0, "applications" to 0
                ),
                "featured" to featured,
                "createdAt" to now,
                "updatedAt" to now,
                "expiresAt" to null
            )
            Tasks.await(ref.set(payload))
            // Contador del perfil (mejor esfuerzo: no bloquea la publicación).
            runCatching {
                val updates = mutableMapOf<String, Any?>(
                    "statistics.publicationsCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (type == PublicationType.JOB_OFFER) {
                    updates["employer.publishedCount"] = FieldValue.increment(1)
                }
                Tasks.await(
                    firestore.collection(ProfileRepository.COLLECTION_USERS).document(uid)
                        .update(updates)
                )
            }
            Tasks.await(ref.get()).toPublication()
        }
    }

    suspend fun update(
        context: Context,
        uid: String,
        publicationId: String,
        draft: PublicationDraft,
        images: List<PublicationImage>? = null
    ): Result<Publication> = withContext(Dispatchers.IO) {
        runCatching {
            val errores = validatePublicationDraft(context, draft)
            require(errores.isEmpty()) { errores.first() }
            val ref = firestore.collection(COLLECTION).document(publicationId)
            val actual = Tasks.await(ref.get())
            require(actual.exists()) { context.getString(R.string.kr_pub_gone) }
            require(actual.getString("ownerUid") == uid) { context.getString(R.string.kr_pub_ajena) }
            val status = actual.getString("status").orEmpty()
            require(status in listOf(PublicationStatus.ACTIVE, PublicationStatus.PAUSED)) {
                context.getString(R.string.kr_pub_editar)
            }
            val cambios = mutableMapOf<String, Any?>(
                "title" to draft.title.trim(),
                "description" to draft.description.trim(),
                "category" to draft.category.trim(),
                "skillsRequired" to draft.skillsRequired,
                "payment.amount" to draft.amount,
                "payment.period" to draft.period,
                "payment.negotiable" to draft.negotiable,
                "workersNeeded" to draft.workersNeeded,
                "requiresExperience" to draft.requiresExperience,
                "location.district" to draft.district.trim(),
                "location.exactAddress" to draft.exactAddress.trim(),
                "schedule.startTime" to draft.startTime.trim(),
                "schedule.endTime" to draft.endTime.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (images != null) {
                require(images.size <= 3) { context.getString(R.string.kr_pub_fotos_max) }
                val prefijo = "chambaya/fotos-publicaciones/$uid/$publicationId/"
                require(images.all { it.url.contains(prefijo) && it.publicId.startsWith(prefijo) }) {
                    "Las fotos de la publicación no son válidas."
                }
                cambios["images"] = images.map { mapOf("url" to it.url, "publicId" to it.publicId) }
            }
            Tasks.await(ref.update(cambios))
            Tasks.await(ref.get()).toPublication()
        }
    }

    suspend fun changeStatus(context: Context, uid: String, publicationId: String, nuevo: String): Result<Publication> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(nuevo in PublicationStatus.ALL) { context.getString(R.string.kr_pub_estado) }
                val ref = firestore.collection(COLLECTION).document(publicationId)
                val actual = Tasks.await(ref.get())
                require(actual.exists()) { context.getString(R.string.kr_pub_gone) }
                require(actual.getString("ownerUid") == uid) { context.getString(R.string.kr_pub_ajena) }
                Tasks.await(ref.update(mapOf("status" to nuevo, "updatedAt" to FieldValue.serverTimestamp())))
                Tasks.await(ref.get()).toPublication()
            }
        }

    suspend fun delete(context: Context, uid: String, publicationId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COLLECTION).document(publicationId)
                val actual = Tasks.await(ref.get())
                if (actual.exists()) {
                    require(actual.getString("ownerUid") == uid) { context.getString(R.string.kr_pub_ajena) }
                    Tasks.await(ref.delete())
                    // Contadores denormalizados (mejor esfuerzo; la rama
                    // isAllowedPublicationCounterUpdate los autoriza en ±1).
                    runCatching {
                        Tasks.await(
                            firestore.collection(ProfileRepository.COLLECTION_USERS).document(uid)
                                .update(
                                    mapOf(
                                        "employer.publishedCount" to FieldValue.increment(-1),
                                        "statistics.publicationsCount" to FieldValue.increment(-1),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                )
                        )
                    }
                }
                // Las fotos de Cloudinary se dejan: su borrado exige firma (backend).
            }
        }

    /**
     * Repara los contadores denormalizados comparando con el total real.
     *
     * Las publicaciones creadas cuando las reglas aún no permitían el ±1
     * quedaron con `publishedCount = 0`. Esta función avanza paso a paso
     * (±1 por escritura, lo único que autoriza la regla) hasta igualar
     * [actual]. Se llama una vez por sesión desde Mis publicaciones.
     */
    suspend fun syncPublishedCount(uid: String, actual: Int) {
        if (uid.isBlank() || actual < 0) return
        withContext(Dispatchers.IO) {
            val ref = firestore.collection(ProfileRepository.COLLECTION_USERS).document(uid)
            repeat(12) {
                val snap = runCatching { Tasks.await(ref.get()) }.getOrNull()
                    ?: return@withContext
                val emp = snap.get("employer") as? Map<*, *> ?: return@withContext
                // Sin mapa statistics la rama de contadores no aplica.
                if (snap.get("statistics") !is Map<*, *>) return@withContext
                val cur = (emp["publishedCount"] as? Number)?.toInt() ?: return@withContext
                if (cur == actual) return@withContext
                val delta = if (actual > cur) 1L else -1L
                val ok = runCatching {
                    Tasks.await(
                        ref.update(
                            mapOf(
                                "employer.publishedCount" to FieldValue.increment(delta),
                                "statistics.publicationsCount" to FieldValue.increment(delta),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                }.isSuccess
                if (!ok) return@withContext
            }
        }
    }

    suspend fun getById(publicationId: String): Result<Publication?> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (publicationId.isBlank()) return@runCatching null
                val snap = Tasks.await(firestore.collection(COLLECTION).document(publicationId).get())
                if (!snap.exists()) null else snap.toPublication()
            }
        }

    /** Varias publicaciones por id (guardados, historial). Sin índice compuesto. */
    suspend fun getByIds(ids: List<String>): Result<List<Publication>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val clean = ids.distinct().filter { it.isNotBlank() }
                if (clean.isEmpty()) return@runCatching emptyList()
                val out = mutableListOf<Publication>()
                clean.chunked(30).forEach { chunk ->
                    Tasks.await(
                        firestore.collection(COLLECTION)
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                            .get()
                    ).documents.map { it.toPublication() }.forEach { out += it }
                }
                // Mismo orden de entrada.
                out.sortedBy { clean.indexOf(it.publicationId) }
            }
        }

    /** Feed público: ACTIVE + PUBLIC, recientes primero. */
    suspend fun feed(limit: Long = 30): Result<List<Publication>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val featured = Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("featured", true)
                        .get()
                ).documents.map { it.toPublication() }
                val recent = Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("status", PublicationStatus.ACTIVE)
                        .whereEqualTo("visibility", PublicationVisibility.PUBLIC)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                ).documents.map { it.toPublication() }
                sortFeed(featured.filter { it.status == PublicationStatus.ACTIVE && it.visibility == PublicationVisibility.PUBLIC } + recent, limit)
            }
        }

    private fun sortFeed(items: List<Publication>, limit: Long): List<Publication> =
        items.distinctBy { it.publicationId }
            .sortedWith(
                compareByDescending<Publication> { it.featured }
                    .thenByDescending { it.createdAt?.seconds ?: 0 }
            )
            .take(limit.coerceAtLeast(0).toInt())

    /** Publicaciones del contratante (todos los estados). */
    suspend fun byOwner(uid: String, limit: Long = 50): Result<List<Publication>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val snap = Tasks.await(
                    firestore.collection(COLLECTION)
                        .whereEqualTo("ownerUid", uid)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get()
                )
                snap.documents.map { it.toPublication() }
            }
        }

    /** Feed en TIEMPO REAL: avisa cada vez que una publicación cambia.
     *
     * El Fragment lo engancha en `onResume` y lo suelta en `onPause`: así lo
     * que se publica desde `fragmento_publicar` aparece en `fragmento_chambas`
     * sin cerrar la app. Devuelve el registro para poder cancelarlo.
     */
    fun listenFeed(
        limit: Long = 40,
        onUpdate: (List<Publication>) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        val collection = firestore.collection(COLLECTION)
        val featuredQuery = collection
            .whereEqualTo("featured", true)
        val recentQuery = collection
            .whereEqualTo("status", PublicationStatus.ACTIVE)
            .whereEqualTo("visibility", PublicationVisibility.PUBLIC)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

        val lock = Any()
        var featuredItems: List<Publication>? = null
        var recentItems: List<Publication>? = null
        fun mergeIfReady(): List<Publication>? = synchronized(lock) {
            val featured = featuredItems ?: return@synchronized null
            val recent = recentItems ?: return@synchronized null
            sortFeed(
                featured.filter {
                    it.status == PublicationStatus.ACTIVE && it.visibility == PublicationVisibility.PUBLIC
                } + recent,
                limit
            )
        }

        val featuredListener = featuredQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            synchronized(lock) { featuredItems = snapshot.documents.map { it.toPublication() } }
            mergeIfReady()?.let(onUpdate)
        }
        val recentListener = recentQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot == null) return@addSnapshotListener
            synchronized(lock) { recentItems = snapshot.documents.map { it.toPublication() } }
            mergeIfReady()?.let(onUpdate)
        }
        return object : com.google.firebase.firestore.ListenerRegistration {
            override fun remove() {
                featuredListener.remove()
                recentListener.remove()
            }
        }
    }

    /** "Mis publicaciones" en TIEMPO REAL (todos los estados del dueño). */
    fun listenByOwner(
        uid: String,
        limit: Long = 50,
        onUpdate: (List<Publication>) -> Unit,
        onError: (Exception) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection(COLLECTION)
            .whereEqualTo("ownerUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, e ->
                if (e != null) { onError(e); return@addSnapshotListener }
                if (snap == null) return@addSnapshotListener
                runCatching { snap.documents.map { it.toPublication() } }
                    .onSuccess(onUpdate)
                    .onFailure { onError(it as? Exception ?: Exception(it)) }
            }
    }

    /** Suma una vista (mejor esfuerzo, sin bloquear la UI). */
    suspend fun registerView(publicationId: String) {
        runCatching {
            withContext(Dispatchers.IO) {
                Tasks.await(
                    firestore.collection(COLLECTION).document(publicationId)
                        .update("statistics.views", FieldValue.increment(1))
                )
            }
        }
    }

    suspend fun registerShare(publicationId: String) {
        runCatching {
            withContext(Dispatchers.IO) {
                Tasks.await(
                    firestore.collection(COLLECTION).document(publicationId)
                        .update("statistics.shares", FieldValue.increment(1))
                )
            }
        }
    }

    companion object {
        const val COLLECTION = "publications"
    }
}
