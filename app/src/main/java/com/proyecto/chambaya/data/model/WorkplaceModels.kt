package com.proyecto.chambaya.data.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * FASE 4 — LUGAR / ESTABLECIMIENTO (`workplaces/{workplaceId}`).
 *
 * Un contratante tiene UN lugar principal (1:1 con `users/{uid}.employer.workplaceId`).
 * La Fase 5 (publicaciones) lo reutiliza sin volver a pedir dirección ni foto.
 *
 * Correcciones al MD original:
 *  - El MD mezcla `photoPath` con `url/publicId`. Aquí se guardan los tres, igual
 *    que el perfil (`profilePhotoUrl/ProfilePhotoPublicId/ProfilePhotoPath`):
 *    `photoUrl` + `photoPublicId` + `photoPath` (carpeta). Sin foto, los tres vacíos.
 *  - `location` con `0,0` por defecto apuntaba al Atlántico. Ahora es opcional:
 *    `latitude/longitude` nulos = "sin ubicación GPS". La dirección escrita siempre
 *    se guarda, el GPS solo si el usuario lo autoriza.
 *  - `verified` lo escribe solo la plataforma (siempre `false` desde la app).
 *  - `createdAt/updatedAt` son `Timestamp` del servidor: fecha/hora automática,
 *    nunca manual.
 */
object WorkplaceTypes {
    const val VIVIENDA = "VIVIENDA"
    const val LOCAL_COMERCIAL = "LOCAL_COMERCIAL"
    const val EMPRESA = "EMPRESA"
    const val TALLER = "TALLER"
    const val RESTAURANTE = "RESTAURANTE"
    const val OBRA = "OBRA"
    const val CAMPO = "CAMPO"
    const val OTRO = "OTRO"

    val ALL = listOf(
        VIVIENDA, LOCAL_COMERCIAL, EMPRESA, TALLER,
        RESTAURANTE, OBRA, CAMPO, OTRO
    )

    fun isValid(value: String?): Boolean = value in ALL

    /** Etiqueta corta en español para la UI. */
    fun label(value: String?): String = when (value) {
        VIVIENDA -> "Vivienda"
        LOCAL_COMERCIAL -> "Local comercial"
        EMPRESA -> "Empresa"
        TALLER -> "Taller"
        RESTAURANTE -> "Restaurante"
        OBRA -> "Obra"
        CAMPO -> "Campo / chacra"
        OTRO -> "Otro"
        else -> value.orEmpty()
    }
}

/** Límites de la FASE 4. La app y `firestore.rules` validan lo mismo. */
object WorkplaceLimits {
    const val NAME_MIN = 3
    const val NAME_MAX = 120
    const val SECTOR_MAX = 120
    const val DESCRIPTION_MAX = 1000
    const val ADDRESS_MAX = 200
    const val DISTRICT_MAX = 80
}

/** Ubicación GPS opcional. Nula = el usuario no compartió coordenadas. */
data class WorkplaceLocation(
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val hasCoords: Boolean
        get() = latitude != null && longitude != null &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0

    val shortLabel: String
        get() = if (hasCoords) {
            String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude)
        } else ""
}

/**
 * `workplaces/{workplaceId}` tal como lo ve la app.
 *
 * `createdAtMillis/updatedAtMillis` son 0 cuando el servidor aún no los selló
 * (escritura recién hecha sin relectura).
 */
data class Workplace(
    val workplaceId: String = "",
    val ownerUid: String = "",
    val name: String = "",
    val type: String = WorkplaceTypes.OTRO,
    val sector: String = "",
    val description: String = "",
    val address: String = "",
    val district: String = "",
    val province: String = "",
    val department: String = "",
    val location: WorkplaceLocation = WorkplaceLocation(),
    val photoUrl: String = "",
    val photoPublicId: String = "",
    val photoPath: String = "",
    val verified: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val hasPhoto: Boolean get() = photoUrl.isNotBlank() && photoPublicId.isNotBlank()

    /** "Carmen Alto, Huamanga, Ayacucho" o lo que haya. */
    val locationLabel: String
        get() = listOf(district, province, department)
            .filter { it.isNotBlank() }.joinToString(", ")

    val typeLabel: String get() = WorkplaceTypes.label(type)
}

/**
 * Lo que el formulario puede escribir. Sin ids, sin dueño, sin verificación
 * y sin timestamps: todo eso lo pone el repositorio/servidor.
 */
data class WorkplaceDraft(
    val name: String = "",
    val type: String = WorkplaceTypes.OTRO,
    val sector: String = "",
    val description: String = "",
    val address: String = "",
    val district: String = "",
    val province: String = "",
    val department: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    companion object {
        fun from(w: Workplace): WorkplaceDraft = WorkplaceDraft(
            name = w.name,
            type = w.type,
            sector = w.sector,
            description = w.description,
            address = w.address,
            district = w.district,
            province = w.province,
            department = w.department,
            latitude = w.location.latitude,
            longitude = w.location.longitude
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Lectura tolerante de Firestore
// ═══════════════════════════════════════════════════════════════════

internal fun DocumentSnapshot.toWorkplace(): Workplace {
    val loc = get("location") as? Map<*, *>
    return Workplace(
        workplaceId = getString("workplaceId")?.trim().orEmpty().ifBlank { id },
        ownerUid = getString("ownerUid").orEmpty(),
        name = getString("name").orEmpty(),
        type = getString("type")?.takeIf { WorkplaceTypes.isValid(it) }
            ?: WorkplaceTypes.OTRO,
        sector = getString("sector").orEmpty(),
        description = getString("description").orEmpty(),
        address = getString("address").orEmpty(),
        district = getString("district").orEmpty(),
        province = getString("province").orEmpty(),
        department = getString("department").orEmpty(),
        location = WorkplaceLocation(
            latitude = (loc?.get("latitude") as? Number)?.toDouble(),
            longitude = (loc?.get("longitude") as? Number)?.toDouble()
        ),
        photoUrl = getString("photoUrl").orEmpty(),
        photoPublicId = getString("photoPublicId").orEmpty(),
        photoPath = getString("photoPath").orEmpty(),
        verified = getBoolean("verified") ?: false,
        createdAtMillis = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
        updatedAtMillis = getTimestamp("updatedAt")?.toDate()?.time ?: 0L
    )
}

/** Errores de validación del borrador. Vacío = válido. */
fun validateWorkplaceDraft(draft: WorkplaceDraft): List<String> {
    val errores = mutableListOf<String>()
    val nombre = draft.name.trim()
    if (nombre.length !in WorkplaceLimits.NAME_MIN..WorkplaceLimits.NAME_MAX) {
        errores += "El nombre del lugar debe tener entre ${WorkplaceLimits.NAME_MIN} y " +
            "${WorkplaceLimits.NAME_MAX} caracteres."
    }
    if (!WorkplaceTypes.isValid(draft.type)) {
        errores += "El tipo de lugar no es válido."
    }
    if (draft.sector.trim().length > WorkplaceLimits.SECTOR_MAX) {
        errores += "El sector no puede pasar de ${WorkplaceLimits.SECTOR_MAX} caracteres."
    }
    if (draft.description.trim().length > WorkplaceLimits.DESCRIPTION_MAX) {
        errores += "La descripción no puede pasar de ${WorkplaceLimits.DESCRIPTION_MAX} caracteres."
    }
    if (draft.address.trim().length > WorkplaceLimits.ADDRESS_MAX) {
        errores += "La dirección no puede pasar de ${WorkplaceLimits.ADDRESS_MAX} caracteres."
    }
    listOf(
        "distrito" to draft.district,
        "provincia" to draft.province,
        "departamento" to draft.department
    ).forEach { (campo, valor) ->
        if (valor.trim().length > WorkplaceLimits.DISTRICT_MAX) {
            errores += "El $campo no puede pasar de ${WorkplaceLimits.DISTRICT_MAX} caracteres."
        }
    }
    // Ubicación GPS: o vienen las dos coordenadas válidas, o ninguna.
    val lat = draft.latitude
    val lng = draft.longitude
    if ((lat == null) != (lng == null)) {
        errores += "La ubicación GPS está incompleta."
    } else if (lat != null && lng != null &&
        (lat !in -90.0..90.0 || lng !in -180.0..180.0)
    ) {
        errores += "Las coordenadas GPS no son válidas."
    }
    return errores
}
