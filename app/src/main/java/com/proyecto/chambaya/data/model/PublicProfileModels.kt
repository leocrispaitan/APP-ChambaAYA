package com.proyecto.chambaya.data.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Perfil PÚBLICO (FASE 6/7, base de FASE 17).
 *
 * Colección: public_profiles/{uid} — solo datos visibles para todos.
 * Nunca DNI, correo, teléfono ni dirección exacta. Los agregados
 * (publicaciones, trabajos, reputación) se calculan en vivo desde las
 * colecciones abiertas, nunca se duplican aquí.
 */
data class PublicWorkerBlock(
    val enabled: Boolean = false,
    val experienceYears: Int = 0,
    val specialties: List<String> = emptyList(),
    val skills: List<String> = emptyList()
)

data class PublicEmployerBlock(
    val enabled: Boolean = false,
    val employerType: String = "",
    val businessName: String = "",
    val commercialName: String = "",
    val sector: String = "",
    val workplaceId: String? = null
)

data class PublicProfile(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val district: String = "",
    val province: String = "",
    val identityVerified: Boolean = false,
    val worker: PublicWorkerBlock = PublicWorkerBlock(),
    val employer: PublicEmployerBlock = PublicEmployerBlock()
) {
    /** Nombre visible: la entidad si hay, si no la persona. */
    fun displayName(): String =
        employer.businessName.ifBlank { fullName.ifBlank { "Usuario ChambAYA" } }
}

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toPublicProfile(): PublicProfile {
    val w = get("worker") as? Map<*, *>
    val e = get("employer") as? Map<*, *>
    fun str(m: Map<*, *>?, key: String): String = (m?.get(key) as? String).orEmpty()
    fun strs(m: Map<*, *>?, key: String): List<String> =
        (m?.get(key) as? List<*>)?.mapNotNull { it?.toString() }.orEmpty()
    return PublicProfile(
        uid = getString("uid") ?: id,
        fullName = getString("fullName").orEmpty(),
        username = getString("username").orEmpty(),
        photoUrl = getString("photoUrl").orEmpty(),
        bio = getString("bio").orEmpty(),
        district = getString("district").orEmpty(),
        province = getString("province").orEmpty(),
        identityVerified = getBoolean("identityVerified") ?: false,
        worker = PublicWorkerBlock(
            enabled = (w?.get("enabled") as? Boolean) ?: false,
            experienceYears = (w?.get("experienceYears") as? Number)?.toInt() ?: 0,
            specialties = strs(w, "specialties"),
            skills = strs(w, "skills")
        ),
        employer = PublicEmployerBlock(
            enabled = (e?.get("enabled") as? Boolean) ?: false,
            employerType = str(e, "employerType"),
            businessName = str(e, "businessName"),
            commercialName = str(e, "commercialName"),
            sector = str(e, "sector"),
            workplaceId = str(e, "workplaceId").ifBlank { null }
        )
    )
}
