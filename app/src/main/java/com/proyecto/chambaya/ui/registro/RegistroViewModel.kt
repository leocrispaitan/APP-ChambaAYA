package com.proyecto.chambaya.ui.registro

import androidx.lifecycle.ViewModel
import com.proyecto.chambaya.data.model.IdentityDocumentTypes
import com.proyecto.chambaya.data.model.ValidatedIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Extracción no rompiente del estado de RegistroActivity (2500 líneas).
 *
 * RegistroActivity sigue funcionando como antes. Este ViewModel es el
 * destino de la migración gradual:
 *  - Paso 0 rol / Paso 1 identidad / Paso 2 credenciales / Paso 3 OTP / Paso 4 resumen
 *  - Sobrevive a rotación (ViewModel) + a muerte de proceso (PendingRegistrationStore sigue).
 *
 * No se migró MainActivity a Navigation ni se eliminó Compose:
 *  - MainActivity usa hide/show manual con 6 fragments + 12 BottomSheets;
 *    migrar a NavController rompería el feed/chat/publicar en este corte.
 *  - Deps Compose quedan reservadas para futuras pantallas; quitar el plugin
 *    kotlin.compose ahora rompería la caché de build sin beneficio.
 */
data class RegistroUiState(
    val currentStep: Int = 0,
    val selectedRole: String = "",
    val roleSelected: Boolean = false,
    val identityMode: String = IdentityDocumentTypes.DNI,
    val selectedEmployerType: String = "",
    val validatedIdentity: ValidatedIdentity? = null,
    val isDniVerified: Boolean = false,
    val isRucVerified: Boolean = false,
    val registeredUid: String? = null,
    val registeredEmail: String? = null,
    val registeredAuthMethod: String = "",
    val isOtpVerified: Boolean = false,
    val isEmailVerifiedByAuth: Boolean = false,
    val isPhase2Completed: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class RegistroViewModel : ViewModel() {

    private val _state = MutableStateFlow(RegistroUiState())
    val state: StateFlow<RegistroUiState> = _state.asStateFlow()

    fun setStep(step: Int) {
        _state.update { it.copy(currentStep = step, error = null) }
    }

    fun setRole(role: String, employerType: String = "") {
        _state.update {
            it.copy(
                selectedRole = role,
                roleSelected = true,
                selectedEmployerType = employerType,
                error = null
            )
        }
    }

    fun setIdentityMode(mode: String) {
        _state.update { it.copy(identityMode = mode) }
    }

    fun setValidatedIdentity(identity: ValidatedIdentity?) {
        _state.update {
            it.copy(
                validatedIdentity = identity,
                isDniVerified = identity?.documentType == IdentityDocumentTypes.DNI,
                isRucVerified = identity?.documentType == IdentityDocumentTypes.RUC,
                error = null
            )
        }
    }

    fun setCredentials(uid: String?, email: String?, authMethod: String) {
        _state.update {
            it.copy(
                registeredUid = uid,
                registeredEmail = email,
                registeredAuthMethod = authMethod,
                isPhase2Completed = uid != null,
                error = null
            )
        }
    }

    fun setOtpVerified(verified: Boolean) {
        _state.update { it.copy(isOtpVerified = verified) }
    }

    fun setEmailVerifiedByAuth(verified: Boolean) {
        _state.update { it.copy(isEmailVerifiedByAuth = verified) }
    }

    fun setSaving(saving: Boolean) {
        _state.update { it.copy(isSaving = saving) }
    }

    fun setError(message: String?) {
        _state.update { it.copy(error = message, isSaving = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
