package com.proyecto.chambaya.data.model

/**
 * FASE 1 — REGISTRO Y CREACIÓN DEL USUARIO.
 *
 * Modelos de dominio del flujo de registro.
 *
 * Reglas del plan maestro que se respetan aquí:
 *  - Firebase Authentication es la fuente de identidad (uid).
 *  - Nunca se guardan contraseñas en Firestore.
 *  - Los roles se guardan como lista (`roles`) + `activeRole` para permitir
 *    TRABAJADOR + CONTRATANTE en una sola cuenta (FASE 3).
 *  - El DNI/RUC nunca debe mostrarse públicamente: por eso el modelo expone
 *    siempre una versión enmascarada (`maskedDocumentNumber`).
 *  - Durante el registro SOLO se piden datos mínimos (identidad, credenciales,
 *    OTP y rol). El resto del perfil se completa en `Mi Perfil` (FASE 2).
 */
object UserRoles {
    const val TRABAJADOR = "TRABAJADOR"
    const val CONTRATANTE = "CONTRATANTE"

    fun isValid(role: String?): Boolean = role == TRABAJADOR || role == CONTRATANTE
}

object IdentityDocumentTypes {
    const val DNI = "DNI"
    const val RUC = "RUC"
}

object AuthProviders {
    const val EMAIL = "EMAIL"
    const val GOOGLE = "GOOGLE"
}

/** Método de acceso usado por el usuario (espejo de `auth.provider`). */
object AuthMethods {
    const val EMAIL_PASSWORD = "EMAIL_PASSWORD"
    const val GOOGLE = "GOOGLE"
}

/** Cómo se confirmó la titularidad del correo electrónico. */
object EmailVerificationMethods {
    /** OTP de 6 dígitos generado por ChambAYA (Cloud Function / fallback Firestore). */
    const val CHAMBAYA_OTP = "CHAMBAYA_OTP"

    /** Correo de verificación nativo de Firebase Authentication. */
    const val FIREBASE_EMAIL_LINK = "FIREBASE_EMAIL_LINK"
}

object AccountStatuses {
    const val ACTIVE = "ACTIVE"
    const val SUSPENDED = "SUSPENDED"
    const val BANNED = "BANNED"
}

object RegistrationStatuses {
    const val PENDING = "PENDING"
    const val INCOMPLETE = "INCOMPLETE"
    const val VERIFIED = "VERIFIED"
}

object IdentitySources {
    const val RENIEC = "RENIEC"
    const val SUNAT = "SUNAT"
}

/** Origen de la foto de perfil en `profile.profilePhotoSource`. */
object ProfilePhotoSources {
    const val GOOGLE = "GOOGLE"
    const val DEFAULT = "DEFAULT"
    const val CUSTOM = "CUSTOM"
}

/**
 * Identidad validada en la FASE 1 (DNI contra RENIEC o RUC contra SUNAT).
 *
 * @param documentType DNI o RUC.
 * @param documentNumber Número validado (8 dígitos para DNI, 11 para RUC).
 * @param fullName Nombre completo tal como lo devuelve el padrón oficial.
 * @param firstName Nombres (vacío cuando la identidad es una empresa).
 * @param lastName Apellidos (vacío cuando la identidad es una empresa).
 * @param legalName Razón social (solo RUC).
 * @param source RENIEC o SUNAT.
 * @param statusLabel Estado oficial (SUNAT: "ACTIVO / HABIDO").
 * @param locationLabel Ubicación oficial devuelta por RENIEC.
 */
data class ValidatedIdentity(
    val documentType: String,
    val documentNumber: String,
    val fullName: String,
    val firstName: String = "",
    val lastName: String = "",
    val legalName: String? = null,
    val source: String,
    val statusLabel: String? = null,
    val locationLabel: String? = null,
    val verifiedAtMillis: Long = System.currentTimeMillis()
) {
    val isCompany: Boolean
        get() = documentType == IdentityDocumentTypes.RUC

    /** Nombre a mostrar/guardar: razón social para empresas, nombre propio para personas. */
    val displayName: String
        get() = legalName?.takeIf { it.isNotBlank() } ?: fullName

    /** Versión segura del documento para vistas que no son del propio usuario. */
    val maskedDocumentNumber: String
        get() = maskDocumentNumber(documentNumber)

    companion object {
        fun maskDocumentNumber(value: String?): String {
            val digits = value?.filter { it.isDigit() }.orEmpty()
            if (digits.isEmpty()) return ""
            if (digits.length <= 4) return "*".repeat(digits.length)
            return "*".repeat(digits.length - 4) + digits.takeLast(4)
        }
    }
}

/**
 * Borrador del registro guardado mientras el usuario avanza por los pasos.
 * Existe antes de que exista el `uid` (FASE 1: DNI/RUC).
 */
data class RegistrationDraft(
    val role: String,
    val identity: ValidatedIdentity,
    val accountDisplayName: String = "",
    val accountPhotoUrl: String = ""
)

/**
 * Registro completo y listo para crear `users/{uid}`.
 * Se construye recién cuando Firebase Authentication ya entregó el `uid`.
 */
data class PendingRegistration(
    val uid: String,
    val role: String,
    val email: String,
    val provider: String,
    val authMethod: String,
    val verificationMethod: String,
    val otpVerified: Boolean,
    val identity: ValidatedIdentity,
    val accountDisplayName: String = "",
    val accountPhotoUrl: String = ""
)
