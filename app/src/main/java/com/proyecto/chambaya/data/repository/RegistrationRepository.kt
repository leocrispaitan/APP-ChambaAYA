package com.proyecto.chambaya.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.proyecto.chambaya.data.model.AccountStatuses
import com.proyecto.chambaya.data.model.AuthMethods
import com.proyecto.chambaya.data.model.AuthProviders
import com.proyecto.chambaya.data.model.EmailVerificationMethods
import com.proyecto.chambaya.data.model.IdentityNameParser
import com.proyecto.chambaya.data.model.PendingRegistration
import com.proyecto.chambaya.data.model.ProfilePhotoSources
import com.proyecto.chambaya.data.model.RegistrationStatuses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Estado real del registro de una cuenta, para distinguir
 * "cuenta ya registrada" de "registro a medias" (bug de reanudación de FASE 1).
 */
enum class UserRegistrationState {
    /** No existe documento `users/{uid}`: la cuenta de Auth quedó huérfana. */
    MISSING,

    /** Existe `users/{uid}` pero el registro todavía no se completó. */
    INCOMPLETE,

    /** Registro completo: la cuenta ya está dada de alta en ChambAYA. */
    COMPLETE,

    /** No se pudo consultar (sin red o error de permisos): estado indeterminado. */
    UNKNOWN
}

/**
 * FASE 1 — Repositorio de `users/{uid}`.
 *
 * Única fuente de verdad para crear el documento de usuario al terminar el registro.
 * Antes el documento se armaba "a mano" en tres lugares distintos
 * (`RegistroActivity`, fallback de OTP y Cloud Function) y solo guardaba el correo.
 *
 * Documento resultante (FASE 1 del plan maestro):
 *
 * ```
 * users/{uid}
 *   uid                    -> uid de Firebase Authentication
 *   accountStatus          -> "ACTIVE"
 *   registrationStatus     -> "VERIFIED"
 *   roles[]                -> ["TRABAJADOR"] | ["CONTRATANTE"]
 *   activeRole             -> rol principal
 *   auth.provider          -> "EMAIL" | "GOOGLE"
 *   auth.email             -> correo de la cuenta
 *   auth.emailVerified     -> true
 *   auth.otpVerified       -> true cuando el correo se confirmó con OTP de ChambAYA
 *   auth.verificationMethod-> "CHAMBAYA_OTP" | "FIREBASE_EMAIL_LINK"
 *   identity.documentType  -> "DNI" | "RUC"
 *   identity.documentNumber-> número validado (solo lectura del propietario)
 *   identity.documentNumberMasked -> versión segura para vistas públicas
 *   identity.identityVerified     -> true
 *   identity.verifiedWith -> "RENIEC" | "SUNAT"
 *   identity.identityName -> nombre oficial (persona o razón social)
 *   identity.identityStatus / location -> datos oficiales del padrón
 *   identity.verifiedAt   -> Timestamp
 *   profile.firstName / lastName / fullName -> nombre del usuario (REGISTRO)
 *   profile.profilePhotoUrl / profilePhotoPublicId / profilePhotoSource
 *   profile.country
 *   createdAt / updatedAt / lastLoginAt -> Timestamp
 * ```
 *
 * NO se guarda: contraseñas, imágenes en Base64 ni datos de FASE 2 en adelante.
 */
class RegistrationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Crea `users/{uid}` con los datos mínimos de la FASE 1.
     *
     * Es idempotente: si el documento ya tiene la identidad y el nombre
     * guardados, solo actualiza `updatedAt` / `lastLoginAt` (esto respeta las
     * Firestore Security Rules). Si quedó incompleto, completa los campos.
     */
    suspend fun finalizeRegistration(
        pending: PendingRegistration,
        firebaseUser: FirebaseUser? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userRef = firestore.collection(COLLECTION_USERS).document(pending.uid)
            val existing = Tasks.await(userRef.get())

            val payload = if (!existing.exists() || !existing.hasFase1Data()) {
                buildRegistrationDocument(pending, firebaseUser)
            } else {
                buildRefreshDocument()
            }

            Tasks.await(userRef.set(payload, SetOptions.merge()))
        }.map { }
    }

    /**
     * Consulta el estado real del registro de `users/{uid}`.
     *
     * Se usa antes de mostrar "esta cuenta ya está registrada": si el documento
     * no existe o está incompleto, el registro debe **reanudarse** en lugar de
     * bloquear al usuario.
     */
    suspend fun fetchRegistrationState(uid: String): UserRegistrationState =
        withContext(Dispatchers.IO) {
            runCatching {
                val snapshot = Tasks.await(
                    firestore.collection(COLLECTION_USERS).document(uid).get()
                )
                when {
                    !snapshot.exists() -> UserRegistrationState.MISSING
                    isRegistered(snapshot) -> UserRegistrationState.COMPLETE
                    else -> UserRegistrationState.INCOMPLETE
                }
            }.getOrDefault(UserRegistrationState.UNKNOWN)
        }

    /**
     * Mismo criterio que usa `LoginActivity` para dar acceso a la app.
     * Garantiza el invariante: *si el usuario puede entrar por login,
     * no se le debe ofrecer un registro nuevo*.
     */
    fun isRegistered(snapshot: DocumentSnapshot): Boolean {
        val registrationStatus = snapshot.getString("registrationStatus").orEmpty()
        val accountStatus = snapshot.getString("accountStatus").orEmpty()
        return registrationStatus in RegistrationStatuses.REGISTERED ||
            accountStatus == AccountStatuses.ACTIVE
    }

    /**
     * `true` cuando el documento ya guarda la identidad oficial y el nombre
     * del usuario (bloques `identity` y `profile` de la FASE 1).
     */
    private fun DocumentSnapshot.hasFase1Data(): Boolean {
        val identity = get("identity") as? Map<*, *> ?: return false
        val profile = get("profile") as? Map<*, *> ?: return false
        val documentNumber = identity["documentNumber"] as? String ?: return false
        val identityName = identity["identityName"] as? String ?: return false
        val fullName = profile["fullName"] as? String ?: return false
        return documentNumber.isNotBlank() && identityName.isNotBlank() && fullName.isNotBlank()
    }

    /** Registra el último ingreso sin tocar datos de identidad ni verificaciones. */
    suspend fun touchLastLogin(uid: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userRef = firestore.collection(COLLECTION_USERS).document(uid)
            if (!Tasks.await(userRef.get()).exists()) return@runCatching
            Tasks.await(userRef.set(buildRefreshDocument(), SetOptions.merge()))
        }.map { }
    }

    /** Payload completo de alta (documento nuevo). */
    fun buildRegistrationDocument(
        pending: PendingRegistration,
        firebaseUser: FirebaseUser? = null
    ): Map<String, Any?> {
        val now = FieldValue.serverTimestamp()
        val identity = pending.identity
        val email = pending.email.ifBlank { firebaseUser?.email.orEmpty() }.trim().lowercase()
        val provider = pending.provider.ifBlank { resolveProvider(firebaseUser) }
        val authMethod = pending.authMethod.ifBlank { resolveAuthMethod(provider) }
        val verificationMethod = pending.verificationMethod.ifBlank {
            if (pending.otpVerified) {
                EmailVerificationMethods.CHAMBAYA_OTP
            } else {
                EmailVerificationMethods.FIREBASE_EMAIL_LINK
            }
        }

        val (firstName, lastName) = resolvePersonalNames(pending)
        val photoUrl = pending.accountPhotoUrl.trim()

        return linkedMapOf(
            // --- Núcleo de la cuenta ---
            "uid" to pending.uid,
            "accountStatus" to AccountStatuses.ACTIVE,
            "registrationStatus" to RegistrationStatuses.VERIFIED,
            "roles" to listOf(pending.role),
            "activeRole" to pending.role,

            // --- Credenciales (sin contraseña: vive en Firebase Auth) ---
            "auth" to linkedMapOf(
                "provider" to provider,
                "email" to email,
                "emailVerified" to true,
                "otpVerified" to pending.otpVerified,
                "verificationMethod" to verificationMethod
            ),

            // --- Identidad validada por padrón oficial ---
            "identity" to linkedMapOf(
                "documentType" to identity.documentType,
                "documentNumber" to identity.documentNumber,
                "documentNumberMasked" to identity.maskedDocumentNumber,
                "identityVerified" to true,
                "verifiedWith" to identity.source,
                "identityName" to identity.displayName,
                "identityStatus" to (identity.statusLabel ?: ""),
                "location" to (identity.locationLabel ?: ""),
                "verifiedAt" to now
            ),

            // --- Nombre del usuario (lo que faltaba guardar) ---
            "profile" to linkedMapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "fullName" to identity.displayName.ifBlank { pending.accountDisplayName },
                "profilePhotoUrl" to photoUrl,
                "profilePhotoPublicId" to "",
                "profilePhotoSource" to if (photoUrl.isNotEmpty()) {
                    ProfilePhotoSources.GOOGLE
                } else {
                    ProfilePhotoSources.DEFAULT
                },
                "country" to COUNTRY
            ),

            // --- Auditoría ---
            "createdAt" to now,
            "updatedAt" to now,
            "lastLoginAt" to now,

            // --- Espejo de compatibilidad ---
            // Campos raíz que ya leía `LoginActivity`; se conservan para no
            // romper datos existentes ni otras pantallas de la app.
            "email" to email,
            "role" to pending.role,
            "emailVerified" to true,
            "otpVerified" to pending.otpVerified,
            "authMethod" to authMethod,
            "verifiedAt" to now
        )
    }

    /**
     * Payload mínimo para un documento ya creado.
     * Solo toca los campos que el usuario puede modificar por Rules.
     */
    fun buildRefreshDocument(): Map<String, Any?> = linkedMapOf(
        "updatedAt" to FieldValue.serverTimestamp(),
        "lastLoginAt" to FieldValue.serverTimestamp()
    )

    /** Nombres para `profile`: en RUC se usa la razón social como nombre completo. */
    private fun resolvePersonalNames(pending: PendingRegistration): Pair<String, String> {
        val identity = pending.identity
        if (identity.isCompany) return "" to ""
        if (identity.firstName.isNotBlank() || identity.lastName.isNotBlank()) {
            return identity.firstName to identity.lastName
        }
        // Respaldo: nombre entregado por Google cuando el padrón no lo detalla.
        val fromGoogle = IdentityNameParser.parseFirstAndLastName(pending.accountDisplayName)
        return fromGoogle.firstName to fromGoogle.lastName
    }

    private fun resolveProvider(firebaseUser: FirebaseUser?): String =
        if (firebaseUser?.providerData?.any { it.providerId == AuthProviders.GOOGLE } == true) {
            AuthProviders.GOOGLE
        } else {
            AuthProviders.EMAIL
        }

    private fun resolveAuthMethod(provider: String): String =
        if (provider == AuthProviders.GOOGLE) AuthMethods.GOOGLE else AuthMethods.EMAIL_PASSWORD

    companion object {
        const val COLLECTION_USERS = "users"
        private const val COUNTRY = "Peru"
    }
}
