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
 * `profile.birthDate` es texto en `dd/MM/aaaa`, no un `Timestamp`.
 *
 * Se guardó así desde la FASE 2 (el `DatePicker` devuelve día, mes y año sueltos y
 * un `Timestamp` obligaría a elegir zona horaria para una fecha que no la tiene) y
 * se mantiene: migrar el documento entero sería un cambio de modelo que no aporta
 * nada ahora. Lo que sí hace falta es un único sitio donde decidir si un texto es
 * una fecha válida, porque la escribe el usuario, el `DatePicker` y ahora también
 * la API de RENIEC.
 */
object BirthDates {

    private val PATRON = Regex("^(\\d{2})/(\\d{2})/(\\d{4})$")

    /** `true` si [value] es una fecha real en `dd/MM/aaaa` y está dentro de lo posible. */
    fun esValida(value: String?): Boolean {
        val partes = PATRON.matchEntire(value?.trim().orEmpty()) ?: return false
        val dia = partes.groupValues[1].toInt()
        val mes = partes.groupValues[2].toInt()
        val anio = partes.groupValues[3].toInt()
        return mes in 1..12 && dia in 1..diasDelMes(mes, anio) &&
            anio in ANIO_MIN..ANIO_MAX
    }

    /**
     * Devuelve [value] si es una fecha válida, o cadena vacía si no lo es.
     *
     * Es el filtro que se aplica a lo que llega de fuera (API de RENIEC): un dato
     * que no se entiende se descarta y el campo queda pendiente para el usuario, en
     * vez de guardar basura que luego no se puede editar.
     */
    fun soloSiValida(value: String?): String =
        if (esValida(value)) value!!.trim() else ""

    private fun diasDelMes(mes: Int, anio: Int): Int = when (mes) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0) 29 else 28
        else -> 0
    }

    /** Nadie que use la app nació antes de 1900 ni dentro de un mes. */
    private const val ANIO_MIN = 1900

    /** Nadie que use la app nació antes de 1900; el techo deja margen de sobra. */
    private const val ANIO_MAX = 2100
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
    val isWorker: Boolean get() = enabled
}

/**
 * Datos persistentes del modo CONTRATANTE.
 *
 * Se mantiene separado de `worker` para que cambiar de modo nunca obligue a
 * repetir los datos del trabajador. El documento de identidad del registro
 * original no se reemplaza; este bloque guarda la identidad que el usuario
 * decidió usar como contratante.
 */
data class EmployerBlock(
    val enabled: Boolean = false,
    val employerType: String = "",
    val businessName: String = "",
    val commercialName: String = "",
    val sector: String = "",
    val documentType: String = "",
    val documentNumber: String = "",
    val documentNumberMasked: String = "",
    val ruc: String? = null,
    val identityName: String = "",
    val workplaceId: String? = null,
    val publishedCount: Int = 0,
    val hiredCount: Int = 0,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0
) {
    val isConfigured: Boolean get() = enabled && employerType.isNotBlank()
    val documentLabel: String
        get() = listOf(documentType, documentNumberMasked.ifBlank { documentNumber })
            .filter { it.isNotBlank() }.joinToString(": ")
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
    val roles: List<String> = listOf(UserRoles.TRABAJADOR),
    val activeRole: String = UserRoles.TRABAJADOR,
    val registrationStatus: String = "",
    val accountStatus: String = "",
    val profile: ProfileBlock = ProfileBlock(),
    val worker: WorkerBlock = WorkerBlock(),
    val employer: EmployerBlock = EmployerBlock(),
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
        private val COMMON_CHECKS: List<Pair<String, (UserProfile) -> Boolean>> = listOf(
            "Foto de perfil" to { it.hasPhoto },
            "Nombre completo" to { it.profile.fullName.isNotBlank() },
            "Nombre de usuario" to { it.profile.username.isNotBlank() },
            "Teléfono" to { it.profile.phone.filter(Char::isDigit).length >= ProfileLimits.PHONE_MIN },
            "Descripción" to { it.profile.bio.isNotBlank() },
            "Distrito" to { it.profile.district.isNotBlank() },
            "Provincia" to { it.profile.province.isNotBlank() },
            "Departamento" to { it.profile.department.isNotBlank() },
            "Privacidad" to { true }
        )

        private val WORKER_CHECKS: List<Pair<String, (UserProfile) -> Boolean>> = listOf(
            "Fecha de nacimiento" to { it.profile.birthDate.isNotBlank() },
            "Género" to { it.profile.gender.isNotBlank() },
            "Años de experiencia" to { it.worker.experienceYears > 0 },
            "Especialidades" to { it.worker.specialties.isNotEmpty() },
            "Habilidades" to { it.worker.skills.isNotEmpty() }
        )

        private val EMPLOYER_CHECKS: List<Pair<String, (UserProfile) -> Boolean>> = listOf(
            "Tipo de contratante" to { it.employer.employerType.isNotBlank() },
            "Identidad del contratante" to { it.employer.documentType.isNotBlank() && it.employer.documentNumber.isNotBlank() },
            "Nombre comercial o negocio" to { it.employer.businessName.isNotBlank() }
        )

        fun checksFor(profile: UserProfile): List<Pair<String, (UserProfile) -> Boolean>> =
            COMMON_CHECKS + if (profile.activeRole == UserRoles.CONTRATANTE) EMPLOYER_CHECKS else WORKER_CHECKS

        fun from(profile: UserProfile): ProfileCompletion {
            val checks = checksFor(profile)
            val faltan = checks.filterNot { it.second(profile) }.map { it.first }
            val completados = checks.size - faltan.size
            return ProfileCompletion(
                percent = (completados * 100) / checks.size,
                missing = faltan
            )
        }

        fun empty(): ProfileCompletion = ProfileCompletion(0, COMMON_CHECKS.map { it.first } + WORKER_CHECKS.map { it.first })
    }
}

/**
 * Datos que envía `EditarPerfilActivity` al finalizar los 4 pasos.
 *
 * Solo contiene campos editables: si algo falta en el borrador, el
 * repositorio conserva el valor que ya estaba en Firestore.
 */
data class EmployerDraft(
    val employerType: String,
    val businessName: String,
    val commercialName: String,
    val sector: String,
    val documentType: String,
    val documentNumber: String,
    val identityName: String,
    val ruc: String? = null
)

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

internal fun DocumentSnapshot.employerBlock(): EmployerBlock {
    val data = get("employer") as? Map<*, *> ?: emptyMap<Any, Any>()
    return EmployerBlock(
        enabled = data["enabled"] as? Boolean ?: false,
        employerType = data.str("employerType"),
        businessName = data.str("businessName"),
        commercialName = data.str("commercialName"),
        sector = data.str("sector"),
        documentType = data.str("documentType"),
        documentNumber = data.str("documentNumber"),
        documentNumberMasked = data.str("documentNumberMasked"),
        ruc = data.str("ruc").ifBlank { null },
        identityName = data.str("identityName"),
        workplaceId = data.str("workplaceId").ifBlank { null },
        publishedCount = (data["publishedCount"] as? Number)?.toInt() ?: 0,
        hiredCount = (data["hiredCount"] as? Number)?.toInt() ?: 0,
        ratingAverage = (data["ratingAverage"] as? Number)?.toDouble() ?: 0.0,
        ratingCount = (data["ratingCount"] as? Number)?.toInt() ?: 0
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
internal fun DocumentSnapshot.toUserProfile(): UserProfile {
    val rolesParsed = (get("roles") as? List<*>)?.mapNotNull { it?.toString() }
        ?.filter(UserRoles::isValid)
        ?.ifEmpty { listOf(UserRoles.TRABAJADOR) }
        ?: listOf(getString("activeRole") ?: getString("role") ?: UserRoles.TRABAJADOR)
    val workerParsed = workerBlock()
    return UserProfile(
    uid = id,
    roles = rolesParsed,
    activeRole = getString("activeRole") ?: getString("role") ?: UserRoles.TRABAJADOR,
    registrationStatus = getString("registrationStatus").orEmpty(),
    accountStatus = getString("accountStatus").orEmpty(),
    profile = profileBlock(),
    worker = workerParsed.copy(enabled = rolesParsed.contains(UserRoles.TRABAJADOR) && workerParsed.enabled),
    employer = employerBlock(),
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
}

// ── helpers de lectura tolerante ──────────────────────────────────

private fun Map<*, *>.str(key: String): String = this[key]?.toString()?.trim().orEmpty()

private fun Map<*, *>.strList(key: String): List<String> =
    (this[key] as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf(String::isNotEmpty) }
        .orEmpty()
