package com.proyecto.chambaya.data.local

import android.content.Context
import android.content.SharedPreferences
import com.proyecto.chambaya.data.model.PendingRegistration
import com.proyecto.chambaya.data.model.RegistrationDraft
import com.proyecto.chambaya.data.model.ValidatedIdentity
import org.json.JSONObject

/**
 * FASE 1 - Persistencia local del registro en curso.
 *
 * Antes el nombre del usuario y el DNI/RUC solo vivian en `TextView`s, por lo que
 * se perdian al recrear la Activity. Aqui se guardan en `SharedPreferences`
 * para poder:
 *  1.- mostrar la tarjeta de "verificado" otra vez,
 *  2.- crear `users/{uid}` aunque el proceso Android mate la app,
 *  3.- trasladar el nombre oficial (RENIEC/SUNAT) hasta Firestore.
 *
 * NO contiene contrasenas: Firebase Authentication es la unica fuente de credenciales.
 */
class PendingRegistrationStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ==================== BORRADOR (FASE 1: DNI/RUC, aun sin uid) ====================

    fun saveDraft(draft: RegistrationDraft) {
        prefs.edit()
            .putString(KEY_DRAFT_ROLE, draft.role)
            .putString(KEY_DRAFT_DISPLAY_NAME, draft.accountDisplayName)
            .putString(KEY_DRAFT_PHOTO_URL, draft.accountPhotoUrl)
            .putString(KEY_DRAFT_IDENTITY, identityToJson(draft.identity).toString())
            .apply()
    }

    fun loadDraft(): RegistrationDraft? {
        val identityJson = prefs.getString(KEY_DRAFT_IDENTITY, null) ?: return null
        val identity = runCatching { identityFromJson(JSONObject(identityJson)) }.getOrNull()
            ?: return null

        return RegistrationDraft(
            role = prefs.getString(KEY_DRAFT_ROLE, "") ?: "",
            identity = identity,
            accountDisplayName = prefs.getString(KEY_DRAFT_DISPLAY_NAME, "") ?: "",
            accountPhotoUrl = prefs.getString(KEY_DRAFT_PHOTO_URL, "") ?: ""
        )
    }

    // ==================== REGISTRO PENDIENTE (ya existe el uid) ====================

    fun savePending(pending: PendingRegistration) {
        val email = pending.email.trim().lowercase()
        prefs.edit()
            .putString(KEY_PENDING_UID, pending.uid)
            .putString(KEY_PENDING_ROLE, pending.role)
            .putString(KEY_PENDING_EMAIL, email)
            .putString(KEY_PENDING_PROVIDER, pending.provider)
            .putString(KEY_PENDING_AUTH_METHOD, pending.authMethod)
            .putString(KEY_PENDING_VERIFICATION_METHOD, pending.verificationMethod)
            .putBoolean(KEY_PENDING_OTP_VERIFIED, pending.otpVerified)
            .putString(KEY_PENDING_DISPLAY_NAME, pending.accountDisplayName)
            .putString(KEY_PENDING_PHOTO_URL, pending.accountPhotoUrl)
            .putString(KEY_PENDING_IDENTITY, identityToJson(pending.identity).toString())
            // Índice correo -> uid: permite recuperar el registro aunque el
            // usuario haya perdido la sesión de Firebase Authentication.
            .putString(KEY_INDEX_EMAIL, email)
            .putString(KEY_INDEX_EMAIL_UID, pending.uid)
            .apply()
    }

    /**
     * Recupera el registro a medias por correo electrónico.
     * Se usa cuando el usuario vuelve a escribir el mismo correo en el
     * sub-paso 2 y Firebase responde "cuenta ya registrada".
     */
    fun loadPendingByEmail(email: String): PendingRegistration? {
        val normalized = email.trim().lowercase()
        if (normalized.isEmpty()) return null

        val indexedEmail = prefs.getString(KEY_INDEX_EMAIL, null) ?: return null
        if (indexedEmail != normalized) return null

        val uid = prefs.getString(KEY_INDEX_EMAIL_UID, null) ?: return null
        return loadPending(uid)
    }

    fun loadPending(uid: String): PendingRegistration? {
        val storedUid = prefs.getString(KEY_PENDING_UID, null) ?: return null
        if (storedUid != uid) return null

        val identityJson = prefs.getString(KEY_PENDING_IDENTITY, null) ?: return null
        val identity = runCatching { identityFromJson(JSONObject(identityJson)) }.getOrNull()
            ?: return null

        return PendingRegistration(
            uid = uid,
            role = prefs.getString(KEY_PENDING_ROLE, "") ?: "",
            email = prefs.getString(KEY_PENDING_EMAIL, "") ?: "",
            provider = prefs.getString(KEY_PENDING_PROVIDER, "") ?: "",
            authMethod = prefs.getString(KEY_PENDING_AUTH_METHOD, "") ?: "",
            verificationMethod = prefs.getString(KEY_PENDING_VERIFICATION_METHOD, "") ?: "",
            otpVerified = prefs.getBoolean(KEY_PENDING_OTP_VERIFIED, false),
            identity = identity,
            accountDisplayName = prefs.getString(KEY_PENDING_DISPLAY_NAME, "") ?: "",
            accountPhotoUrl = prefs.getString(KEY_PENDING_PHOTO_URL, "") ?: ""
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // ==================== Serializacion de identidad ====================

    private fun identityToJson(identity: ValidatedIdentity): JSONObject = JSONObject().apply {
        put("documentType", identity.documentType)
        put("documentNumber", identity.documentNumber)
        put("fullName", identity.fullName)
        put("firstName", identity.firstName)
        put("lastName", identity.lastName)
        put("legalName", identity.legalName ?: JSONObject.NULL)
        put("source", identity.source)
        put("statusLabel", identity.statusLabel ?: JSONObject.NULL)
        put("locationLabel", identity.locationLabel ?: JSONObject.NULL)
        put("verifiedAtMillis", identity.verifiedAtMillis)
    }

    private fun identityFromJson(json: JSONObject): ValidatedIdentity = ValidatedIdentity(
        documentType = json.optString("documentType"),
        documentNumber = json.optString("documentNumber"),
        fullName = json.optString("fullName"),
        firstName = json.optString("firstName"),
        lastName = json.optString("lastName"),
        legalName = json.optStringOrNull("legalName"),
        source = json.optString("source"),
        statusLabel = json.optStringOrNull("statusLabel"),
        locationLabel = json.optStringOrNull("locationLabel"),
        verifiedAtMillis = json.optLong("verifiedAtMillis", System.currentTimeMillis())
    )

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).ifBlank { null }

    companion object {
        private const val PREFS_NAME = "chambaya_pending_registration_v1"

        private const val KEY_DRAFT_ROLE = "draft_role"
        private const val KEY_DRAFT_DISPLAY_NAME = "draft_display_name"
        private const val KEY_DRAFT_PHOTO_URL = "draft_photo_url"
        private const val KEY_DRAFT_IDENTITY = "draft_identity"

        private const val KEY_PENDING_UID = "pending_uid"
        private const val KEY_PENDING_ROLE = "pending_role"
        private const val KEY_PENDING_EMAIL = "pending_email"
        private const val KEY_PENDING_PROVIDER = "pending_provider"
        private const val KEY_PENDING_AUTH_METHOD = "pending_auth_method"
        private const val KEY_PENDING_VERIFICATION_METHOD = "pending_verification_method"
        private const val KEY_PENDING_OTP_VERIFIED = "pending_otp_verified"
        private const val KEY_PENDING_DISPLAY_NAME = "pending_display_name"
        private const val KEY_PENDING_PHOTO_URL = "pending_photo_url"
        private const val KEY_PENDING_IDENTITY = "pending_identity"

        private const val KEY_INDEX_EMAIL = "index_email"
        private const val KEY_INDEX_EMAIL_UID = "index_email_uid"
    }
}
