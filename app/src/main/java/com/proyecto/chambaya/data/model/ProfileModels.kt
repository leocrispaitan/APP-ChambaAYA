package com.proyecto.chambaya.data.model

import com.google.firebase.firestore.DocumentSnapshot
import java.util.Locale

/**
 * FASE 2 — PERFIL DEL USUARIO.
 *
 * Modelos de dominio de `users/{uid}` para la fase en la que el usuario completa
 * los datos que NO eran necesarios durante el registro.
 *
 * Reglas del plan maestro que se respetan aquí:
 *  - `RegistrationRepository` (FASE 1) es el dueño de `uid`, `auth`, `identity`
 *    y del nombre oficial. Esta capa solo LEE esos bloques, nunca los escribe.
 *  - El usuario únicamente puede modificar `profile`, `worker` (su parte de
 *    trabajador), `privacy` y `updatedAt`.
 *  - `worker.workCount`, `worker.ratingAverage` y `worker.ratingCount` son
 *    reputación: los calcula la plataforma, no el usuario.
 *  - `worker.profileCompleted` NUNCA se envía como dato del formulario: se
 *    deriva aquí (ver [ProfileCompletion]) para que no pueda quedar inconsistente.
 */

/** Valores admitidos en `profile.gender`. */
object Genders {
    const val MASCULINO = "MASCULINO"
    const val FEMENINO = "FEMENINO"
    const val OTRO = "OTRO"

    val ALL = listOf(MASCULINO, FEMENINO, OTRO)

    fun label(value: String?): String = when (value) {
        MASCULINO -> "Masculino"
        FEMENINO -> "Femenino"
        OTRO -> "Prefiero no decirlo"
        else -> ""
    }

    /** Acepta lo que venga guardado (incluidas etiquetas antiguas en español). */
    fun fromStored(value: String?): String = when (value?.trim()?.lowercase(Locale.ROOT)) {
        "masculino", MASCULINO.lowercase(Locale.ROOT) -> MASCULINO
        "femenino", FEMENINO.lowercase(Locale.ROOT) -> FEMENINO
        "otro", "prefiero no decirlo", MASCULINO -> OTRO
        else -> ""
    }
}

/** Límites de los campos editables. La app y las Rules validan lo mismo. */
object ProfileLimits {
    const val USERNAME_MIN = 3
    const val USERNAME_MAX = 30
    const val PHONE_MIN = 9
    const val PHONE_MAX = 20
    const val BIO_MAX = 500
    const val SKILL_MAX_LENGTH = 60
    const val SKILL_MAX_COUNT = 10
    const val SPECIALTY_MAX = 3
    const val EXPERIENCE_MAX = 70
}

/**
 * `profile` de `users/{uid}`.
 *
 * Los campos de FASE 1 (`firstName`, `lastName`, `fullName`, `country`,
 * `profilePhotoSource`) se conservan; los de FASE 2 se completan aquí.
 */
data class ProfileBlock(
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String = "",
    val username: String = "",
    val usernameNormalized: String = "",
    val phone: String = "",
    val profilePhotoUrl: String = "",
    val profilePhotoPublicId: String = "",
    val profilePhotoSource: String = ProfilePhotoSources.DEFAULT,
    val bio: String = "",
    val district: String = "",
    val province: String = "",
    val department: String = "",
    val birthDate: String = "",
    val gender: String = "",
    val country: String = "Peru"
) {
    /** "Carmen Alto, Ayacucho" o el nombre tal cual si no hay distrito. */
    val locationLabel: String
        get() = listOf(district, department).filter { it.isNotBlank() }.joinToString(", ")

    /** Nunca se guarda una foto ajena: el `publicId` siempre vive en nuestra carpeta. */
    val hasCustomPhoto: Boolean
        get() = profilePhotoSource == ProfilePhotoSources.CUSTOM &&
            profilePhotoUrl.isNotBlank() &&
            profilePhotoPublicId.isNotBlank()
}

/**
 * `worker` de `users/{uid}`: lo que el usuario ofrece como trabajador.
 *
 * `workCount` / `ratingAverage` / `ratingCount` son de la plataforma y por eso
 * se separan de lo que el usuario puede escribir.
 */
data class WorkerBlock(
    val enabled: Boolean = true,
    val experienceYears: Int = 0,
    val specialties: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val workCount: Int = 0,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val profileCompleted: Int = 0
) {
    /** `true` si el usuario es trabajador activo (FASE 3 sumará CONTRATANTE). */
    val isWorker: Boolean get() = enabled
}

/** `privacy` de `users/{uid}`. Por defecto nada se muestra públicamente. */
data class PrivacyBlock(
    val showPhone: Boolean = false,
    val showExactAddress: Boolean = false,
    val showEmail: Boolean = false
)

/** `statistics` de `users/{uid}`. Solo las incrementa la plataforma. */
data class StatisticsBlock(
    val applicationsCount: Int = 0,
    val publicationsCount: Int = 0,
    val completedJobsCount: Int = 0,
    val savedPublicationsCount: Int = 0,
    val receivedRatingsCount: Int = 0
)

/**
 * `identity` de `users/{uid}` (FASE 1) en modo SOLO LECTURA.
 *
 * Se modela aquí únicamente para pintar en el perfil qué documento está
 * verificado y con qué padrón. Nunca se envía de vuelta a Firestore.
 */
data class IdentityBlock(
    val documentType: String = "",
    val documentNumber: String = "",
    val documentNumberMasked: String = "",
    val identityVerified: Boolean = false,
    val verifiedWith: String = "",
    val identityName: String = ""
) {
    /** "DNI: 72345678 • Verificado con RENIEC" */
    val verifiedLabel: String
        get() = buildString {
            if (documentType.isNotBlank()) append("$documentType: ")
            append(documentNumberMasked.ifBlank { ValidatedIdentity.maskDocumentNumber(documentNumber) })
            if (verifiedWith.isNotBlank()) append(" • Verificado con $verifiedWith")
        }
}

/** `auth` de `users/{uid}` (FASE 1) en modo SOLO LECTURA. */
data class AuthBlock(
    val email: String = "",
    val emailVerified: Boolean = false,
    val otpVerified: Boolean = false
)

/**
 * `users/{uid}` completo tal como lo ve la pantalla de perfil.
 *
 * `identity` y `auth` se cargan para poder mostrarlos, pero la escritura de la
 * FASE 2 solo toca `profile`, `worker` y `privacy`.
 */
data class UserProfile(
    val uid: String = "",
    val activeRole: String = UserRoles.TRABAJADOR,
    val registrationStatus: String = "",
    val accountStatus: String = "",
    val profile: ProfileBlock = ProfileBlock(),
    val worker: WorkerBlock = WorkerBlock(),
    val privacy: PrivacyBlock = PrivacyBlock(),
    val statistics: StatisticsBlock = StatisticsBlock(),
    val identity: IdentityBlock = IdentityBlock(),
    val auth: AuthBlock = AuthBlock(),
    /**
     * `true` si el documento ya tiene los bloques que aporta la FASE 2
     * (`worker`, `privacy`, `statistics`) y un `@usuario` reservado.
     *
     * Las cuentas de la FASE 1 nacen sin ellos. La pantalla de perfil solo los
     * siembra una vez, y lo hace "a mejor hacer": si esa escritura se rechaza,
     * el perfil se sigue mostrando con los valores por defecto de los modelos.
     */
    val tieneBloquesFase2: Boolean = false
) {
    /**
     * Calcula el porcentaje de completitud a partir del estado real del
     * documento. Se usa para el badge, el banner y el resumen del wizard.
     */
    fun completion(): ProfileCompletion = ProfileCompletion.from(this)

    /** `true` cuando el usuario guardó una foto propia en Cloudinary. */
    val hasPhoto: Boolean
        get() = profile.profilePhotoUrl.isNotBlank()
}

/**
 * Porcentaje de completitud y qué falta para llegar al 100 %.
 *
 * Se DERIVA del documento, nunca se envía como dato: así es imposible que el
 * porcentaje guardado y los datos guardados se contradigan.
 */
data class ProfileCompletion(
    val percent: Int,
    val missing: List<String>
) {
    val isComplete: Boolean get() = percent >= 100

    companion object {
        /** Campos que el usuario puede rellenar desde "Editar perfil". */
        val CHECKS: List<Pair<String, (UserProfile) -> Boolean>> = listOf(
            // Cuenta cualquier foto, incluida la que dejó la FASE 1 desde Google:
            // lo que se pide es que el perfil tenga una, no que sea de Cloudinary.
            "Foto de perfil" to { it.hasPhoto },
            "Nombre completo" to { it.profile.fullName.isNotBlank() },
            "Nombre de usuario" to { it.profile.username.isNotBlank() },
            "Teléfono" to { it.profile.phone.filter(Char::isDigit).length >= ProfileLimits.PHONE_MIN },
            "Descripción" to { it.profile.bio.isNotBlank() },
            "Distrito" to { it.profile.district.isNotBlank() },
            "Provincia" to { it.profile.province.isNotBlank() },
            "Departamento" to { it.profile.department.isNotBlank() },
            "Fecha de nacimiento" to { it.profile.birthDate.isNotBlank() },
            "Género" to { it.profile.gender.isNotBlank() },
            "Años de experiencia" to { it.worker.experienceYears > 0 },
            "Especialidades" to { it.worker.specialties.isNotEmpty() },
            "Habilidades" to { it.worker.skills.isNotEmpty() },
            "Privacidad" to { true }
        )

        fun from(profile: UserProfile): ProfileCompletion {
            val faltan = CHECKS.filterNot { it.second(profile) }.map { it.first }
            val completados = CHECKS.size - faltan.size
            return ProfileCompletion(
                percent = (completados * 100) / CHECKS.size,
                missing = faltan
            )
        }

        fun empty(): ProfileCompletion = ProfileCompletion(0, CHECKS.map { it.first })
    }
}

/**
 * Datos que envía `EditarPerfilActivity` al finalizar los 4 pasos.
 *
 * Solo contiene campos editables: si algo falta en el borrador, el
 * repositorio conserva el valor que ya estaba en Firestore.
 */
data class ProfileDraft(
    val fullName: String = "",
    val username: String = "",
    val phone: String = "",
    val bio: String = "",
    val birthDate: String = "",
    val gender: String = "",
    val department: String = "",
    val province: String = "",
    val district: String = "",
    val experienceYears: Int = 0,
    val specialties: List<String> = emptyList(),
    val skills: List<String> = emptyList(),
    val showPhone: Boolean = false,
    val showEmail: Boolean = false,
    val showExactAddress: Boolean = false,
    val workerEnabled: Boolean = true
) {
    /** `null` cuando el usuario no eligió foto: se conserva la que ya tenía. */
    val photoPublicId: String? = null
    val photoUrl: String? = null

    companion object {
        /** Construye el borrador inicial a partir de lo que hay en Firestore. */
        fun from(profile: UserProfile): ProfileDraft = ProfileDraft(
            fullName = profile.profile.fullName,
            username = profile.profile.username,
            phone = profile.profile.phone,
            bio = profile.profile.bio,
            birthDate = profile.profile.birthDate,
            gender = profile.profile.gender,
            department = profile.profile.department,
            province = profile.profile.province,
            district = profile.profile.district,
            experienceYears = profile.worker.experienceYears,
            specialties = profile.worker.specialties,
            skills = profile.worker.skills,
            showPhone = profile.privacy.showPhone,
            showEmail = profile.privacy.showEmail,
            showExactAddress = profile.privacy.showExactAddress,
            workerEnabled = profile.worker.enabled
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  Lectura de `users/{uid}`
// ═══════════════════════════════════════════════════════════════════

/** Lee los bloques de la FASE 2 tolerando documentos que aún no los tienen. */
internal fun DocumentSnapshot.profileBlock(): ProfileBlock {
    val data = get("profile") as? Map<*, *> ?: emptyMap<Any, Any>()
    return ProfileBlock(
        firstName = data.str("firstName"),
        lastName = data.str("lastName"),
        fullName = data.str("fullName"),
        username = data.str("username"),
        usernameNormalized = data.str("usernameNormalized"),
        phone = data.str("phone"),
        profilePhotoUrl = data.str("profilePhotoUrl"),
        profilePhotoPublicId = data.str("profilePhotoPublicId"),
        profilePhotoSource = data.str("profilePhotoSource")
            .ifBlank { ProfilePhotoSources.DEFAULT },
        bio = data.str("bio"),
        district = data.str("district"),
        province = data.str("province"),
        department = data.str("department"),
        birthDate = data.str("birthDate"),
        gender = Genders.fromStored(data.str("gender")),
        country = data.str("country").ifBlank { "Peru" }
    )
}

internal fun DocumentSnapshot.workerBlock(): WorkerBlock {
    val data = get("worker") as? Map<*, *> ?: emptyMap<Any, Any>()
    return WorkerBlock(
        enabled = data["enabled"] as? Boolean ?: true,
        experienceYears = (data["experienceYears"] as? Number)?.toInt() ?: 0,
        specialties = data.strList("specialties"),
        skills = data.strList("skills"),
        workCount = (data["workCount"] as? Number)?.toInt() ?: 0,
        ratingAverage = (data["ratingAverage"] as? Number)?.toDouble() ?: 0.0,
        ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0,
        profileCompleted = (data["profileCompleted"] as? Number)?.toInt() ?: 0
    )
}

internal fun DocumentSnapshot.privacyBlock(): PrivacyBlock {
    val data = get("privacy") as? Map<*, *> ?: emptyMap<Any, Any>()
    return PrivacyBlock(
        showPhone = data["showPhone"] as? Boolean ?: false,
        showExactAddress = data["showExactAddress"] as? Boolean ?: false,
        showEmail = data["showEmail"] as? Boolean ?: false
    )
}

internal fun DocumentSnapshot.statisticsBlock(): StatisticsBlock {
    val data = get("statistics") as? Map<*, *> ?: emptyMap<Any, Any>()
    return StatisticsBlock(
        applicationsCount = (data["applicationsCount"] as? Number)?.toInt() ?: 0,
        publicationsCount = (data["publicationsCount"] as? Number)?.toInt() ?: 0,
        completedJobsCount = (data["completedJobsCount"] as? Number)?.toInt() ?: 0,
        savedPublicationsCount = (data["savedPublicationsCount"] as? Number)?.toInt() ?: 0,
        receivedRatingsCount = (data["receivedRatingsCount"] as? Number)?.toInt() ?: 0
    )
}

/** Bloque `identity` de la FASE 1, en solo lectura. */
internal fun DocumentSnapshot.identityBlock(): IdentityBlock {
    val data = get("identity") as? Map<*, *> ?: emptyMap<Any, Any>()
    return IdentityBlock(
        documentType = data.str("documentType"),
        documentNumber = data.str("documentNumber"),
        documentNumberMasked = data.str("documentNumberMasked"),
        identityVerified = data["identityVerified"] as? Boolean ?: false,
        verifiedWith = data.str("verifiedWith"),
        identityName = data.str("identityName")
    )
}

/** Bloque `auth` de la FASE 1, en solo lectura. */
internal fun DocumentSnapshot.authBlock(): AuthBlock {
    val data = get("auth") as? Map<*, *> ?: emptyMap<Any, Any>()
    return AuthBlock(
        email = data.str("email").ifBlank { getString("email").orEmpty() },
        emailVerified = data["emailVerified"] as? Boolean ?: (getBoolean("emailVerified") ?: false),
        otpVerified = data["otpVerified"] as? Boolean ?: (getBoolean("otpVerified") ?: false)
    )
}

/** Construye el [UserProfile] completo desde el documento de Firestore. */
internal fun DocumentSnapshot.toUserProfile(): UserProfile = UserProfile(
    uid = id,
    activeRole = getString("activeRole") ?: getString("role") ?: UserRoles.TRABAJADOR,
    registrationStatus = getString("registrationStatus").orEmpty(),
    accountStatus = getString("accountStatus").orEmpty(),
    profile = profileBlock(),
    worker = workerBlock(),
    privacy = privacyBlock(),
    statistics = statisticsBlock(),
    identity = identityBlock(),
    auth = authBlock(),
    // Los bloques de la FASE 2 se siembran con una escritura aparte, y solo si
    // faltan: la pantalla de perfil no puede depender de que esa escritura se
    // autorice, así que se limitan a leer el documento.
    tieneBloquesFase2 = get("worker") is Map<*, *> &&
        get("privacy") is Map<*, *> &&
        get("statistics") is Map<*, *> &&
        !getString("profile.username").isNullOrBlank()
)

// ── helpers de lectura tolerante ──────────────────────────────────

private fun Map<*, *>.str(key: String): String = this[key]?.toString()?.trim().orEmpty()

private fun Map<*, *>.strList(key: String): List<String> =
    (this[key] as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
        .orEmpty()
