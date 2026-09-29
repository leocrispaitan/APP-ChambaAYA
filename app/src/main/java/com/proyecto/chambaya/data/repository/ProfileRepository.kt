package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.proyecto.chambaya.data.model.Genders
import com.proyecto.chambaya.data.model.OficioCatalog
import com.proyecto.chambaya.data.model.PeruLocations
import com.proyecto.chambaya.data.model.ProfileCompletion
import com.proyecto.chambaya.data.model.ProfileDraft
import com.proyecto.chambaya.data.model.EmployerDraft
import com.proyecto.chambaya.data.model.IdentityDocumentTypes
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.ProfileLimits
import com.proyecto.chambaya.data.model.ProfilePhotoSources
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.ValidatedIdentity
import com.proyecto.chambaya.data.model.normalizarUsername
import com.proyecto.chambaya.data.model.toUserProfile
import com.proyecto.chambaya.data.remote.CloudinaryUploader
import com.proyecto.chambaya.data.remote.IdentityValidationResult
import com.proyecto.chambaya.data.remote.IdentityValidationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FASE 2 — Repositorio del perfil del usuario (`users/{uid}`).
 *
 * Complementa a [RegistrationRepository] (FASE 1) sin tocarlo: aquel crea el
 * documento y es el dueño de `uid`, `auth`, `identity` y `registrationStatus`;
 * este solo completa y lee los bloques de la FASE 2.
 *
 * ```
 * users/{uid}
 *   profile  -> + username, usernameNormalized, phone, bio, district,
 *               province, department, birthDate, gender, profilePhotoPath
 *   worker   -> enabled, experienceYears, specialties, skills, profileCompleted
 *   privacy  -> showPhone, showExactAddress, showEmail
 *   statistics -> contadores de la plataforma
 * ```
 *
 * Guaranteías:
 *  - **Actualización parcial**: solo se escriben los campos del borrador. Si el
 *    usuario no tocó un campo, el valor que ya estaba en Firestore se conserva.
 *  - **Nada de la FASE 1 se puede tocar**: `uid`, `auth`, `identity`,
 *    `registrationStatus` y `emailVerified`/`otpVerified` no aparecen jamás en
 *    un `update()` de esta clase.
 *  - **Reputación protegida**: `workCount`, `ratingAverage` y `ratingCount` se
 *    copian tal cual; los calcula la plataforma, no el usuario.
 *  - **`profileCompleted` es derivado**: se recalcula con [computeCompletion]
 *    antes de guardar, así que nunca puede contradecir los datos guardados.
 *  - **Username único y normalizado**: se reserva un documento en
 *    `usernames/{usernameNormalized}` mediante una transacción, de modo que dos
 *    usuarios no puedan tomar el mismo `@usuario` a la vez.
 */
class ProfileRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val identityService: IdentityValidationService = IdentityValidationService()
) {

    // ═══════════════════════════════════════════════════════════════
    //  LECTURA
    // ═══════════════════════════════════════════════════════════════

    /** Lee `users/{uid}` y devuelve el perfil completo de la FASE 2. */
    suspend fun loadProfile(uid: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            Tasks.await(firestore.collection(COLLECTION_USERS).document(uid).get()).toUserProfile()
        }
    }

    /**
     * Crea los bloques de la FASE 2 que todavía no existen.
     *
     * Las cuentas creadas en la FASE 1 no tienen `worker`, `privacy` ni
     * `statistics`, y el `username` puede estar vacío. Esta función los completa
     * una sola vez, con valores neutros y un `@usuario` derivado del nombre.
     *
     * Es idempotente y de escritura mínima: si el documento ya está completo
     * no escribe nada, y si le falta algo solo manda ese algo. Por eso entrar
     * al perfil no necesita permiso de escritura en cada visita.
     *
     * Y es "a mejor hacer": [UserProfile] se construye tolerando bloques
     * ausentes, así que la pantalla puede pintar el perfil aunque esta escritura
     * sea rechazada (reglas sin desplegar, red, ...). Quien la llama debe
     * tratar el fallo como informativo, nunca como error fatal.
     */
    suspend fun ensureProfileInitialized(uid: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COLLECTION_USERS).document(uid)
                val actual = Tasks.await(ref.get())
                val perfil = actual.toUserProfile()

                val employerNecesario = perfil.activeRole == UserRoles.CONTRATANTE && actual.get("employer") !is Map<*, *>
                if (perfil.tieneBloquesFase2 && !employerNecesario) return@runCatching perfil

                // `username` se guarda SIEMPRE normalizado, para que coincida
                // con `usernameNormalized` y con la clave de `usernames/`.
                val username = normalizarUsername(perfil.profile.username)
                    .takeIf { esUsernameValido(it) }
                    ?: generarUsername(uid, perfil.profile.fullName)

                val cambios = mutableMapOf<String, Any?>()

                // `profile` se completa campo a campo para no pisar el nombre ni
                // la foto que puso el registro, y solo se envía lo que cambia.
                val actuales = actual.get("profile") as? Map<*, *> ?: emptyMap<Any, Any>()
                mapOf(
                    "username" to username,
                    "usernameNormalized" to normalizarUsername(username),
                    "phone" to perfil.profile.phone,
                    "bio" to perfil.profile.bio,
                    "district" to perfil.profile.district,
                    "province" to perfil.profile.province,
                    "department" to perfil.profile.department,
                    "birthDate" to perfil.profile.birthDate,
                    "gender" to perfil.profile.gender,
                    "country" to perfil.profile.country
                ).forEach { (campo, valor) ->
                    if (actuales[campo] != valor) cambios["profile.$campo"] = valor
                }

                if (actual.get("worker") !is Map<*, *>) {
                    cambios["worker"] = mapOf(
                        "enabled" to (perfil.roles.contains(UserRoles.TRABAJADOR)),
                        "experienceYears" to 0,
                        "specialties" to emptyList<String>(),
                        "skills" to emptyList<String>(),
                        "workCount" to 0,
                        "ratingAverage" to 0.0,
                        "ratingCount" to 0,
                        "profileCompleted" to 0
                    )
                }

                if (actual.get("privacy") !is Map<*, *>) {
                    cambios["privacy"] = mapOf(
                        "showPhone" to false,
                        "showExactAddress" to false,
                        "showEmail" to false
                    )
                }

                if (actual.get("statistics") !is Map<*, *>) {
                    cambios["statistics"] = mapOf(
                        "applicationsCount" to 0,
                        "publicationsCount" to 0,
                        "completedJobsCount" to 0,
                        "savedPublicationsCount" to 0,
                        "receivedRatingsCount" to 0
                    )
                }

                // Las cuentas que se registraron originalmente como CONTRATANTE
                // llegan con `roles=["CONTRATANTE"]` y no necesitan una segunda
                // cuenta: sembramos su bloque employer desde la identidad ya
                // verificada en FASE 1.
                if (perfil.activeRole == UserRoles.CONTRATANTE && actual.get("employer") !is Map<*, *>) {
                    val identidad = perfil.identity
                    val tipo = if (identidad.documentType == IdentityDocumentTypes.RUC) "EMPRESA" else "PERSONA"
                    cambios["employer"] = mapOf(
                        "enabled" to true,
                        "employerType" to tipo,
                        "businessName" to identidad.identityName.ifBlank { perfil.profile.fullName },
                        "commercialName" to "",
                        "sector" to "",
                        "documentType" to identidad.documentType,
                        "documentNumber" to identidad.documentNumber,
                        "ruc" to identidad.documentNumber.takeIf {
                            identidad.documentType == IdentityDocumentTypes.RUC
                        },
                        "identityName" to identidad.identityName.ifBlank { perfil.profile.fullName },
                        "workplaceId" to null,
                        "publishedCount" to 0,
                        "hiredCount" to 0,
                        "ratingAverage" to 0.0,
                        "ratingCount" to 0
                    )
                }

                cambios["updatedAt"] = FieldValue.serverTimestamp()

                Tasks.await(ref.update(cambios))
                Tasks.await(ref.get()).toUserProfile()
            }
        }

    // ═══════════════════════════════════════════════════════════════
    //  FASE 3 — CONTRATANTE / CAMBIO DE MODO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Activa CONTRATANTE sin crear otra cuenta. La información de `worker` no
     * se toca. Si el usuario ya tenía un perfil employer, se actualizan solo
     * sus datos de contratante y se conserva el mismo uid.
     *
     * Requisitos previos (FASE 3):
     *  - Identidad verificada (DNI/RUC con RENIEC/SUNAT)
     *  - Correo verificado
     *  - Perfil básico completo (nombre, @usuario, teléfono)
     *
     * Validación inteligente:
     *  - Si el documento del employer coincide con el de la identidad verificada,
     *    no se hace llamada HTTP a RENIEC/SUNAT (ya está validado).
     *  - Si es diferente, se valida externamente antes de guardar.
     */
    suspend fun activateContractor(
        uid: String,
        draft: EmployerDraft
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            require(draft.documentType == IdentityDocumentTypes.DNI ||
                draft.documentType == IdentityDocumentTypes.RUC) {
                "Selecciona DNI o RUC."
            }
            val cleanDocument = draft.documentNumber.filter(Char::isDigit)
            require(
                (draft.documentType == IdentityDocumentTypes.DNI && cleanDocument.length == 8) ||
                    (draft.documentType == IdentityDocumentTypes.RUC && cleanDocument.length == 11)
            ) {
                if (draft.documentType == IdentityDocumentTypes.DNI)
                    "El DNI debe tener 8 dígitos."
                else
                    "El RUC debe tener 11 dígitos."
            }

            val ref = firestore.collection(COLLECTION_USERS).document(uid)
            val actual = Tasks.await(ref.get()).toUserProfile()
            require(actual.uid == uid) { "La cuenta no es válida." }

            // ═══════════════════════════════════════════════════════════════
            //  REQUISITOS PREVIOS (FASE 3)
            // ═══════════════════════════════════════════════════════════════
            require(actual.identity.identityVerified) {
                "Tu identidad debe estar verificada para activar el modo contratante."
            }
            require(actual.auth.emailVerified) {
                "Tu correo debe estar verificado para activar el modo contratante."
            }
            require(actual.profile.fullName.isNotBlank() && actual.profile.username.isNotBlank()) {
                "Completa tu perfil básico antes de activar el modo contratante."
            }
            require(actual.profile.phone.isNotBlank()) {
                "Agrega un teléfono a tu perfil antes de activar el modo contratante."
            }

            // ═══════════════════════════════════════════════════════════════
            //  VALIDACIÓN INTELIGENTE
            // ═══════════════════════════════════════════════════════════════
            // Si el documento del employer coincide con el de la identidad
            // verificada, ya está validado: no hace falta llamada HTTP.
            val documentoCoincide = actual.identity.documentType == draft.documentType &&
                actual.identity.documentNumber == cleanDocument

            val identityName = if (documentoCoincide) {
                actual.identity.identityName
            } else {
                // Documento diferente: validar externamente
                val identidad = when (draft.documentType) {
                    IdentityDocumentTypes.RUC -> identityService.validateRuc(cleanDocument)
                    else -> identityService.validateDni(cleanDocument)
                }
                when (identidad) {
                    is IdentityValidationResult.Success -> identidad.identity.fullName
                    is IdentityValidationResult.Rejected ->
                        throw IllegalArgumentException(identidad.message)
                    is IdentityValidationResult.ServiceError ->
                        throw IllegalArgumentException(
                            "El servicio de identidad no está disponible (HTTP ${identidad.httpCode})."
                        )
                    is IdentityValidationResult.NetworkError ->
                        throw IllegalArgumentException("No hay conexión para verificar el documento.")
                }
            }

            val roles = linkedSetOf<String>()
            roles += UserRoles.TRABAJADOR
            roles += UserRoles.CONTRATANTE

            val employer = mapOf(
                "enabled" to true,
                "employerType" to draft.employerType,
                "businessName" to draft.businessName.trim(),
                "commercialName" to draft.commercialName.trim(),
                "sector" to draft.sector.trim(),
                "documentType" to draft.documentType,
                "documentNumber" to cleanDocument,
                "documentNumberMasked" to ValidatedIdentity.maskDocumentNumber(cleanDocument),
                "ruc" to draft.ruc?.filter(Char::isDigit)?.takeIf { it.isNotBlank() },
                "identityName" to identityName,
                "workplaceId" to actual.employer.workplaceId,
                "publishedCount" to actual.employer.publishedCount,
                "hiredCount" to actual.employer.hiredCount,
                "ratingAverage" to actual.employer.ratingAverage,
                "ratingCount" to actual.employer.ratingCount
            )

            Tasks.await(
                ref.update(
                    mapOf(
                        "roles" to roles.toList(),
                        "activeRole" to UserRoles.CONTRATANTE,
                        "employer" to employer,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            Tasks.await(ref.get()).toUserProfile()
        }
    }

    /** Cambia el modo activo sin borrar ningún perfil. */
    suspend fun switchActiveRole(uid: String, role: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(UserRoles.isValid(role)) { "Rol no válido." }
                val ref = firestore.collection(COLLECTION_USERS).document(uid)
                val snapshot = Tasks.await(ref.get())
                val roles = snapshot.get("roles") as? List<*>
                require(roles?.contains(role) == true) {
                    "Ese modo todavía no está activado."
                }
                if (role == UserRoles.CONTRATANTE) {
                    val employer = snapshot.get("employer") as? Map<*, *>
                    require(employer?.get("enabled") == true) {
                        "Completa el perfil de contratante antes de cambiar de modo."
                    }
                }
                Tasks.await(
                    ref.update(
                        mapOf(
                            "activeRole" to role,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                )
                Tasks.await(ref.get()).toUserProfile()
            }
        }

    /**
     * Activa TRABAJADOR por primera vez en una cuenta que nació contratante.
     *
     * Espejo de [activateContractor] pero sin validación de identidad: el
     * trabajador no declara documentos, solo completa su perfil en el wizard
     * (paso 3 = experiencia). Si el rol ya existe, solo cambia el modo.
     *
     * Al cambiar de rol el porcentaje se recalcula solo: los checks de
     * trabajador y de contratante son distintos ([ProfileCompletion]), así
     * que el primer cambio casi siempre baja el % hasta completar lo nuevo.
     */
    suspend fun activateWorker(uid: String): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ref = firestore.collection(COLLECTION_USERS).document(uid)
                val snapshot = Tasks.await(ref.get())
                val actual = snapshot.toUserProfile()
                require(actual.uid == uid) { "La cuenta no es válida." }

                if (actual.roles.contains(UserRoles.TRABAJADOR)) {
                    return@runCatching switchActiveRole(uid, UserRoles.TRABAJADOR).getOrThrow()
                }
                require(actual.roles.contains(UserRoles.CONTRATANTE)) {
                    "Rol actual no válido."
                }

                val roles = linkedSetOf(UserRoles.TRABAJADOR, UserRoles.CONTRATANTE)
                val cambios = mutableMapOf<String, Any?>(
                    "roles" to roles.toList(),
                    "activeRole" to UserRoles.TRABAJADOR,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                // Si el documento no trae bloque `worker` se crea en cero
                // (reputación intacta); si lo trae, solo se habilita.
                if (snapshot.get("worker") !is Map<*, *>) {
                    cambios["worker"] = mapOf(
                        "enabled" to true,
                        "experienceYears" to 0,
                        "experienceDeclared" to false,
                        "specialties" to emptyList<String>(),
                        "skills" to emptyList<String>(),
                        "workCount" to 0,
                        "ratingAverage" to 0.0,
                        "ratingCount" to 0,
                        "profileCompleted" to 0
                    )
                } else {
                    cambios["worker.enabled"] = true
                }
                Tasks.await(ref.update(cambios))
                Tasks.await(ref.get()).toUserProfile()
            }
        }

    // ═══════════════════════════════════════════════════════════════
    //  UNICIDAD DEL USERNAME
    // ═══════════════════════════════════════════════════════════════

    /**
     * ¿El `@usuario` está libre?
     *
     * Siempre disponible para el propio usuario: si el documento de
     * `usernames/{normalizado}` le pertenece, puede volver a usarlo.
     */
    suspend fun isUsernameAvailable(username: String, uid: String): Boolean =
        withContext(Dispatchers.IO) {
            val normalizado = normalizarUsername(username)
            if (normalizado.length < ProfileLimits.USERNAME_MIN) return@withContext false
            runCatching {
                val ref = firestore.collection(COLLECTION_USERNAMES).document(normalizado)
                val snapshot = Tasks.await(ref.get())
                !snapshot.exists() || snapshot.getString("uid") == uid
            }.getOrDefault(false)
        }

    /**
     * Reserva `usernames/{normalizado}` para [uid].
     *
     * La transacción es la que garantiza la unicidad: dos personas que elijan el
     * mismo nombre al mismo tiempo, solo una consigue el `create`.
     */
    private suspend fun reservarUsername(normalizado: String, uid: String, visible: String) {
        val ref = firestore.collection(COLLECTION_USERNAMES).document(normalizado)
        Tasks.await(
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(ref)
                if (snapshot.exists() && snapshot.getString("uid") != uid) {
                    throw UsernameYaTomado(normalizado)
                }
                transaction.set(
                    ref,
                    mapOf(
                        "uid" to uid,
                        "username" to visible,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }
        )
    }

    /** Libera el `@usuario` anterior cuando el usuario lo cambia. */
    private suspend fun liberarUsername(normalizado: String, uid: String) {
        if (normalizado.isBlank()) return
        val ref = firestore.collection(COLLECTION_USERNAMES).document(normalizado)
        runCatching {
            val snapshot = Tasks.await(ref.get())
            if (snapshot.exists() && snapshot.getString("uid") == uid) {
                Tasks.await(ref.delete())
            }
        }
    }

    /**
     * Propone un `@usuario` a partir del nombre: "Maria Sanchez" -> "maria_sanchez".
     *
     * Si ya está tomado se le añade un sufijo corto del `uid` para que siempre
     * quede algo disponible sin preguntar nada.
     */
    private suspend fun generarUsername(uid: String, nombreCompleto: String): String {
        val base = normalizarUsername(nombreCompleto).take(ProfileLimits.USERNAME_MAX - 5)
        val candidatos = listOf(
            base.ifBlank { "chambaya" },
            "${base.ifBlank { "chambaya" }}_${uid.take(4).lowercase()}",
            "usuario_${uid.take(6).lowercase()}"
        )
        return candidatos.firstOrNull { esUsernameValido(it) && isUsernameAvailable(it, uid) }
            ?: "usuario_${uid.take(8).lowercase()}"
    }

    /**
     * ¿El `@usuario` ya normalizado cumple lo que piden las Rules?
     *
     * El rango de caracteres es `a-z 0-9 _ .`, el mismo `^[a-z0-9._]+$` de
     * `isValidUsername` en `firestore.rules`. Si la app fuera más laxa que las
     * Rules, el guardado se denegaría en el servidor en vez de fallar aquí.
     */
    private fun esUsernameValido(value: String) =
        value.length in ProfileLimits.USERNAME_MIN..ProfileLimits.USERNAME_MAX &&
            value.all { it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '.' }

    // ═══════════════════════════════════════════════════════════════
    //  ESCRITURA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Guarda el borrador del wizard de edición.
     *
     * Orden de las operaciones (por si algo falla a medias):
     *  1. se validan los datos,
     *  2. se reserva el `@usuario` nuevo (transacción),
     *  3. se escribe el documento con `update()` parcial,
     *  4. se libera el `@usuario` viejo.
     *
     * Devuelve el perfil ya guardado, con el `profileCompleted` recalculado.
     */
    suspend fun saveProfile(
        uid: String,
        draft: ProfileDraft,
        photoUrl: String? = null,
        photoPublicId: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val ref = firestore.collection(COLLECTION_USERS).document(uid)
            val anterior = Tasks.await(ref.get())
            val previo = anterior.toUserProfile()

            val username = normalizarUsername(draft.username)
            require(esUsernameValido(username)) { "El nombre de usuario no es válido." }

            val anteriorNormalized = previo.profile.usernameNormalized
            val cambiaUsername = username != anteriorNormalized

            // 1) Unicidad: si el @usuario cambió, hay que tomar el documento nuevo
            //    ANTES de escribir el perfil, para no dejar el perfil apuntando a
            //    un nombre que otro usuario ya tomó.
            if (cambiaUsername) {
                reservarUsername(username, uid, draft.username.trim())
            }

            // 2) Actualización parcial: se escriben rutas con punto, así
            //    Firestore solo toca esas hojas y conserva el resto.
            val cambios = mutableMapOf<String, Any?>(
                "profile.fullName" to draft.fullName.trim(),
                "profile.username" to username,
                "profile.usernameNormalized" to username,
                "profile.phone" to draft.phone.trim(),
                "profile.bio" to draft.bio.trim(),
                "profile.birthDate" to draft.birthDate.trim(),
                "profile.gender" to draft.gender,
                "profile.district" to draft.district.trim(),
                "profile.province" to draft.province.trim(),
                "profile.department" to draft.department.trim(),

                "worker.enabled" to draft.workerEnabled,
                "worker.experienceYears" to draft.experienceYears,
                // Distingue "escribí 0 años" de "nunca toqué el campo": sin esto,
                // el que empieza de cero no podía llegar nunca al 100 %.
                "worker.experienceDeclared" to draft.experienceDeclared,
                "worker.specialties" to draft.specialties,
                "worker.skills" to draft.skills,

                // El paso 4 del wizard (qué contacto se muestra) también se
                // guarda: sin esto los interruptores de privacidad se perdían.
                "privacy.showPhone" to draft.showPhone,
                "privacy.showEmail" to draft.showEmail,
                "privacy.showExactAddress" to draft.showExactAddress
            )

            // La foto solo se escribe si se subió una nueva: si no, se conserva.
            if (!photoUrl.isNullOrBlank() && !photoPublicId.isNullOrBlank()) {
                cambios["profile.profilePhotoUrl"] = photoUrl
                cambios["profile.profilePhotoPublicId"] = photoPublicId
                cambios["profile.profilePhotoSource"] = ProfilePhotoSources.CUSTOM
                // `profilePhotoPath` documenta la carpeta de Cloudinary. Se pide la
                // misma función que usa la subida en vez de escribir la ruta aquí:
                // esa carpeta es la que valida `isOwnCloudinaryPhoto` en las Rules,
                // así que si las dos se desincronizan el guardado se deniega.
                cambios["profile.profilePhotoPath"] = CloudinaryUploader.carpetaDePerfil(uid)
            }

            // 3) `profileCompleted` se deriva del perfil resultante, nunca se
            //    envía lo que "_dice_" el formulario.
            cambios["worker.profileCompleted"] = calcularCompletitud(cambios, previo)

            cambios["updatedAt"] = FieldValue.serverTimestamp()
            Tasks.await(ref.update(cambios))

            // 4) El @usuario viejo ya no lo necesita nadie.
            if (cambiaUsername) {
                liberarUsername(anteriorNormalized, uid)
            }

            Tasks.await(ref.get()).toUserProfile()
        }
    }

    /**
     * Calcula el `profileCompleted` que corresponde a [cambios] aplicado sobre
     * [previo].
     *
     * Se construye un perfil en memoria con los valores nuevos y se usa el
     * mismo [ProfileCompletion] que luego pintarán las pantallas, de modo que el
     * número guardado y el número mostrado salen del mismo cálculo.
     */
    private fun calcularCompletitud(
        cambios: Map<String, Any?>,
        previo: UserProfile
    ): Int {
        val perfil = previo.copy(
            profile = previo.profile.copy(
                fullName = (cambios["profile.fullName"] as? String) ?: previo.profile.fullName,
                username = (cambios["profile.username"] as? String) ?: previo.profile.username,
                phone = (cambios["profile.phone"] as? String) ?: previo.profile.phone,
                bio = (cambios["profile.bio"] as? String) ?: previo.profile.bio,
                birthDate = (cambios["profile.birthDate"] as? String) ?: previo.profile.birthDate,
                gender = (cambios["profile.gender"] as? String) ?: previo.profile.gender,
                district = (cambios["profile.district"] as? String) ?: previo.profile.district,
                province = (cambios["profile.province"] as? String) ?: previo.profile.province,
                department = (cambios["profile.department"] as? String) ?: previo.profile.department,
                profilePhotoUrl = (cambios["profile.profilePhotoUrl"] as? String)
                    ?: previo.profile.profilePhotoUrl
            ),
            worker = previo.worker.copy(
                enabled = cambios["worker.enabled"] as? Boolean ?: previo.worker.enabled,
                experienceYears = (cambios["worker.experienceYears"] as? Int)
                    ?: previo.worker.experienceYears,
                experienceDeclared = cambios["worker.experienceDeclared"] as? Boolean
                    ?: previo.worker.experienceDeclared,
                specialties = cambios.listaDeTextos("worker.specialties")
                    ?: previo.worker.specialties,
                skills = cambios.listaDeTextos("worker.skills") ?: previo.worker.skills
            ),
            privacy = previo.privacy.copy(
                showPhone = cambios["privacy.showPhone"] as? Boolean ?: previo.privacy.showPhone,
                showEmail = cambios["privacy.showEmail"] as? Boolean ?: previo.privacy.showEmail,
                showExactAddress = cambios["privacy.showExactAddress"] as? Boolean
                    ?: previo.privacy.showExactAddress
            )
        )
        return perfil.completion().percent
    }

    /**
     * Lee una lista de textos de los cambios pendientes.
     *
     * El mapa de cambios es `Map<String, Any?>` porque se arma para Firestore, así
     * que la lista llega como `List<*>`: se filtra elemento a elemento en vez de
     * hacer un cast directo, que sería unchecked.
     */
    private fun Map<String, Any?>.listaDeTextos(key: String): List<String>? =
        (this[key] as? List<*>)?.mapNotNull { it?.toString() }

    /** Calcula la completitud sin escribir nada (para el resumen del paso 4). */
    fun computeCompletion(perfil: UserProfile): ProfileCompletion = perfil.completion()

    /**
     * Propone un `@usuario` para un perfil que todavía no tiene uno.
     *
     * Las cuentas de la FASE 1 nacen sin `username` y las Rules exigen uno válido
     * para cualquier escritura de la FASE 2. El asistente de edición lo necesita
     * ya en memoria (para pintar el campo y no bloquear el guardado con un error
     * de validación), pero sin tocar Firestore: esta función no consulta
     * `usernames/` ni reserva nada, solo normaliza lo que hay.
     *
     * [ensureProfileInitialized] sigue siendo quien decide el `@usuario` definitivo
     * comprobando la disponibilidad real; esta es la versión optimista para la
     * pantalla, y si más tarde el otro dice que no, el usuario escribe otro.
     */
    fun sugerirUsername(perfil: UserProfile): String {
        val guardado = normalizarUsername(perfil.profile.username)
        if (guardado.length >= ProfileLimits.USERNAME_MIN) return guardado

        val nombre = perfil.profile.fullName.ifBlank { perfil.auth.email.substringBefore("@") }
        val base = normalizarUsername(nombre).take(ProfileLimits.USERNAME_MAX - 5)
        val candidatos = listOf(
            base.ifBlank { "chambaya" },
            "${base.ifBlank { "chambaya" }}_${perfil.uid.take(4).lowercase()}",
            "usuario_${perfil.uid.take(6).lowercase()}"
        )
        return candidatos.firstOrNull { esUsernameValido(it) }
            ?: "usuario_${perfil.uid.take(8).lowercase()}"
    }

    // ═══════════════════════════════════════════════════════════════
    //  VALIDACIÓN (la misma que aplican las Firestore Security Rules)
    // ═══════════════════════════════════════════════════════════════

    /** Errores de validación de un borrador. Vacío = válido. */
    fun validate(draft: ProfileDraft): List<String> {
        val errores = mutableListOf<String>()

        if (draft.fullName.trim().length < 3) {
            errores += "Ingresa tu nombre completo (mínimo 3 caracteres)."
        }

        val username = normalizarUsername(draft.username)
        if (username.length !in ProfileLimits.USERNAME_MIN..ProfileLimits.USERNAME_MAX) {
            errores += "El @usuario debe tener entre ${ProfileLimits.USERNAME_MIN} y " +
                "${ProfileLimits.USERNAME_MAX} caracteres."
        }

        // El teléfono es opcional (el registro no lo pide): si se escribe, tiene que
        // ser válido. Es la misma regla que `isValidPhone` en las Rules.
        val digitos = draft.phone.filter { it.isDigit() }
        if (digitos.isNotEmpty() && digitos.length !in ProfileLimits.PHONE_MIN..ProfileLimits.PHONE_MAX) {
            errores += "El teléfono debe tener entre ${ProfileLimits.PHONE_MIN} y " +
                "${ProfileLimits.PHONE_MAX} dígitos."
        }

        if (draft.bio.trim().length > ProfileLimits.BIO_MAX) {
            errores += "La descripción no puede pasar de ${ProfileLimits.BIO_MAX} caracteres."
        }

        if (draft.gender.isNotBlank() && draft.gender !in Genders.ALL) {
            errores += "El género seleccionado no es válido."
        }

        if (draft.experienceYears !in 0..ProfileLimits.EXPERIENCE_MAX) {
            errores += "Los años de experiencia no son válidos."
        }

        if (draft.specialties.size > ProfileLimits.SPECIALTY_MAX) {
            errores += "Solo puedes elegir ${ProfileLimits.SPECIALTY_MAX} especialidades."
        }

        if (draft.skills.size > ProfileLimits.SKILL_MAX_COUNT) {
            errores += "Máximo ${ProfileLimits.SKILL_MAX_COUNT} habilidades."
        }

        if (draft.department.isNotBlank() && !PeruLocations.esDepartamentoValido(draft.department)) {
            errores += "El departamento seleccionado no existe."
        }
        if (draft.province.isNotBlank() &&
            !PeruLocations.esProvinciaValida(draft.department, draft.province)
        ) {
            errores += "La provincia no pertenece al departamento elegido."
        }
        if (draft.district.isNotBlank() &&
            !PeruLocations.esDistritoValido(draft.department, draft.province, draft.district)
        ) {
            errores += "El distrito no es válido."
        }

        return errores
    }

    /**
     * Convierte texto libre en lista de habilidades.
     *
     * Acepta comas o saltos de línea, quita vacíos y duplicados y corta cada
     * habilidad a un largo razonable.
     */
    fun parseSkills(texto: String): List<String> = texto
        .split(',', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.take(ProfileLimits.SKILL_MAX_LENGTH) }
        .distinctBy { it.lowercase() }
        .take(ProfileLimits.SKILL_MAX_COUNT)

    /**
     * Filtra las especialidades contra el catálogo de oficios.
     *
     * Así el perfil no puede guardar oficios inventados y se mantiene coherente
     * con las categorías de las publicaciones.
     */
    fun filtrarEspecialidades(context: android.content.Context, candidatas: List<String>): List<String> {
        val validas = OficioCatalog.load(context).map { it.categoria }
        return candidatas
            .map { it.trim() }
            .filter { texto ->
                validas.any { OficioCatalog.normalizar(it) == OficioCatalog.normalizar(texto) }
            }
            .distinctBy { OficioCatalog.normalizar(it) }
            .take(ProfileLimits.SPECIALTY_MAX)
    }

    companion object {
        const val COLLECTION_USERS = "users"

        /** Reserva de unicidad de `@usuario`: `usernames/{usernameNormalized}`. */
        const val COLLECTION_USERNAMES = "usernames"
    }
}

/** El `@usuario` ya pertenece a otra cuenta. */
class UsernameYaTomado(val username: String) :
    Exception("El @usuario $username ya está en uso.")
