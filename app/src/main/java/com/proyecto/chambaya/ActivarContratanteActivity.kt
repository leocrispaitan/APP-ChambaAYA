package com.proyecto.chambaya

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.data.model.EmployerDraft
import com.proyecto.chambaya.data.model.IdentityDocumentTypes
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.remote.IdentityValidationResult
import com.proyecto.chambaya.data.remote.IdentityValidationService
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.launch

/**
 * FASE 3 — Activación del modo CONTRATANTE.
 *
 * No crea otra cuenta. Reutiliza el uid actual y guarda un bloque `employer`
 * separado del bloque `worker`.
 */
class ActivarContratanteActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val repository = ProfileRepository()
    private val identityService = IdentityValidationService()

    private lateinit var rgTipo: RadioGroup
    private lateinit var rbPersona: RadioButton
    private lateinit var rbEmpresa: RadioButton
    private lateinit var rbNegocio: RadioButton
    private lateinit var rbIndependiente: RadioButton
    private lateinit var tvDocumentoLabel: TextView
    private lateinit var etDocumento: EditText
    private lateinit var etNombre: EditText
    private lateinit var etComercial: EditText
    private lateinit var etSector: EditText
    private lateinit var tvDocumentoEstado: TextView
    private lateinit var btnGuardar: MaterialButton

    private var selectedDocumentType = IdentityDocumentTypes.DNI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activar_contratante)

        bindViews()
        bindListeners()
        cargarExistente()
        actualizarTipo()
        autocompletarDniTrabajador()
    }

    private fun bindViews() {
        rgTipo = findViewById(R.id.rgTipoContratante)
        rbPersona = findViewById(R.id.rbContratantePersona)
        rbEmpresa = findViewById(R.id.rbContratanteEmpresa)
        rbNegocio = findViewById(R.id.rbContratanteNegocio)
        rbIndependiente = findViewById(R.id.rbContratanteIndependiente)
        tvDocumentoLabel = findViewById(R.id.tvDocumentoContratanteLabel)
        etDocumento = findViewById(R.id.etDocumentoContratante)
        etNombre = findViewById(R.id.etNombreContratante)
        etComercial = findViewById(R.id.etNombreComercialContratante)
        etSector = findViewById(R.id.etSectorContratante)
        tvDocumentoEstado = findViewById(R.id.tvDocumentoContratanteEstado)
        btnGuardar = findViewById(R.id.btnGuardarContratante)
    }

    private fun bindListeners() {
        findViewById<android.view.View>(R.id.btnCerrarContratante).setOnClickListener { finish() }
        rgTipo.setOnCheckedChangeListener { _, _ -> actualizarTipo() }
        btnGuardar.setOnClickListener { guardar() }
    }

    private fun cargarExistente() {
        val perfil = ProfileCache.perfil ?: return
        val employer = perfil.employer
        if (!employer.isConfigured) return

        when (employer.employerType) {
            "PERSONA" -> rbPersona.isChecked = true
            "EMPRESA" -> rbEmpresa.isChecked = true
            "NEGOCIO" -> rbNegocio.isChecked = true
            "INDEPENDIENTE" -> rbIndependiente.isChecked = true
        }
        selectedDocumentType = employer.documentType.ifBlank { IdentityDocumentTypes.DNI }
        etDocumento.setText(employer.documentNumber)
        etNombre.setText(employer.businessName)
        etComercial.setText(employer.commercialName)
        etSector.setText(employer.sector)
    }

    /**
     * Autocompleta el DNI del trabajador cuando el tipo es PERSONA/INDEPENDIENTE.
     *
     * Si el usuario ya se registró como trabajador con un DNI verificado, ese
     * mismo DNI se usa para el rol de contratante (PERSONA/INDEPENDIENTE).
     * No hace falta volver a validarlo: ya pasó por RENIEC/SUNAT en FASE 1.
     */
    private fun autocompletarDniTrabajador() {
        val perfil = ProfileCache.perfil ?: return
        val identidad = perfil.identity

        // Solo autocompletar si el tipo es PERSONA o INDEPENDIENTE (DNI)
        val tipo = tipoSeleccionado()
        if (tipo != "PERSONA" && tipo != "INDEPENDIENTE") return

        // Solo si el trabajador tiene un DNI verificado
        if (!identidad.identityVerified) return
        if (identidad.documentType != IdentityDocumentTypes.DNI) return
        if (identidad.documentNumber.isBlank()) return

        // Autocompletar el documento
        etDocumento.setText(identidad.documentNumber)
        selectedDocumentType = IdentityDocumentTypes.DNI

        // Autocompletar el nombre con el nombre oficial del padrón
        if (etNombre.text.isNullOrBlank()) {
            etNombre.setText(identidad.identityName)
        }
    }

    private fun tipoSeleccionado(): String = when {
        rbEmpresa.isChecked -> "EMPRESA"
        rbNegocio.isChecked -> "NEGOCIO"
        rbIndependiente.isChecked -> "INDEPENDIENTE"
        else -> "PERSONA"
    }

    private fun actualizarTipo() {
        val tipo = tipoSeleccionado()
        val requiereRuc = tipo == "EMPRESA" || tipo == "NEGOCIO"
        selectedDocumentType = if (requiereRuc) IdentityDocumentTypes.RUC else IdentityDocumentTypes.DNI
        tvDocumentoLabel.text = if (selectedDocumentType == IdentityDocumentTypes.RUC) {
            "RUC"
        } else {
            "DNI"
        }
        etDocumento.hint = if (selectedDocumentType == IdentityDocumentTypes.RUC) {
            "Ingresa el RUC de la empresa o negocio"
        } else {
            "Ingresa tu DNI"
        }
        etDocumento.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        etDocumento.filters = arrayOf(
            android.text.InputFilter.LengthFilter(if (selectedDocumentType == IdentityDocumentTypes.RUC) 11 else 8)
        )
        etNombre.hint = if (tipo == "PERSONA" || tipo == "INDEPENDIENTE") {
            "Nombre del contratante"
        } else {
            "Razón social / nombre legal"
        }
        etComercial.isVisible = tipo != "PERSONA"
        etSector.isVisible = tipo != "PERSONA"

        // Si cambió a PERSONA/INDEPENDIENTE, autocompletar el DNI del trabajador
        if (!requiereRuc) {
            autocompletarDniTrabajador()
        }
    }

    private fun guardar() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Tu sesión expiró. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show()
            return
        }

        // ═══════════════════════════════════════════════════════════════
        //  VERIFICAR REQUISITOS PREVIOS
        // ═══════════════════════════════════════════════════════════════
        val perfil = ProfileCache.perfil
        if (perfil != null) {
            if (!perfil.identity.identityVerified) {
                mostrarError("Tu identidad debe estar verificada para activar el modo contratante.")
                return
            }
            if (!perfil.auth.emailVerified) {
                mostrarError("Tu correo debe estar verificado para activar el modo contratante.")
                return
            }
            if (perfil.profile.fullName.isBlank() || perfil.profile.username.isBlank()) {
                mostrarError("Completa tu perfil básico antes de activar el modo contratante.")
                return
            }
            if (perfil.profile.phone.isBlank()) {
                mostrarError("Agrega un teléfono a tu perfil antes de activar el modo contratante.")
                return
            }
        }

        val tipo = tipoSeleccionado()
        val documento = etDocumento.text.toString().filter(Char::isDigit)
        val nombre = etNombre.text.toString().trim()
        if (nombre.length < 3) {
            etNombre.error = "Ingresa el nombre del contratante."
            return
        }
        val esperado = if (selectedDocumentType == IdentityDocumentTypes.RUC) 11 else 8
        if (documento.length != esperado) {
            etDocumento.error = "El ${selectedDocumentType} debe tener $esperado dígitos."
            return
        }
        if ((tipo == "EMPRESA" || tipo == "NEGOCIO") && etComercial.text.toString().trim().length < 2) {
            etComercial.error = "Ingresa el nombre comercial."
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text = "Verificando..."

        lifecycleScope.launch {
            // ═══════════════════════════════════════════════════════════
            //  VALIDACIÓN INTELIGENTE
            // ═══════════════════════════════════════════════════════════
            // Si el documento coincide con la identidad verificada del trabajador,
            // no hace falta llamada HTTP: ya está validado.
            val documentoCoincide = perfil?.let {
                it.identity.documentType == selectedDocumentType &&
                it.identity.documentNumber == documento
            } ?: false

            val identityName = if (documentoCoincide) {
                perfil?.identity?.identityName ?: nombre
            } else {
                // Documento diferente: validar externamente
                val identidad = when (selectedDocumentType) {
                    IdentityDocumentTypes.RUC -> identityService.validateRuc(documento)
                    else -> identityService.validateDni(documento)
                }

                when (identidad) {
                    is IdentityValidationResult.Success -> identidad.identity.fullName
                    is IdentityValidationResult.Rejected -> {
                        mostrarError(identidad.message)
                        return@launch
                    }
                    is IdentityValidationResult.ServiceError -> {
                        mostrarError("El servicio de identidad no está disponible (HTTP ${identidad.httpCode}).")
                        return@launch
                    }
                    is IdentityValidationResult.NetworkError -> {
                        mostrarError("No hay conexión para verificar el documento.")
                        return@launch
                    }
                }
            }

            val draft = EmployerDraft(
                employerType = tipo,
                businessName = if (selectedDocumentType == IdentityDocumentTypes.RUC) identityName else nombre,
                commercialName = etComercial.text.toString().trim(),
                sector = etSector.text.toString().trim(),
                documentType = selectedDocumentType,
                documentNumber = documento,
                identityName = identityName,
                ruc = documento.takeIf { selectedDocumentType == IdentityDocumentTypes.RUC }
            )
            repository.activateContractor(uid, draft)
                .onSuccess {
                    ProfileCache.perfil = it
                    setResult(Activity.RESULT_OK)
                    Toast.makeText(this@ActivarContratanteActivity, "Modo contratante activado.", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure {
                    Log.e(TAG, "No se pudo activar contratante", it)
                    mostrarError(it.message ?: "No se pudo guardar el perfil.")
                }
        }
    }

    private fun mostrarError(mensaje: String) {
        btnGuardar.isEnabled = true
        btnGuardar.text = "Activar modo contratante"
        tvDocumentoEstado.text = mensaje
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "ActivarContratante"
    }
}
