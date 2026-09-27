package com.proyecto.chambaya

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import java.util.Calendar

/**
 * Activity profesional para completar perfil en múltiples fases progresivas
 * Implementa navegación simulada sin Firebase (solo frontend funcional)
 * 
 * FASES:
 * 1. Información Básica (DNI, nombre, username, email, teléfono, foto)
 * 2. Información Personal (fecha nacimiento, género, ubicación, bio)
 * 3. Experiencia Profesional (años, especialidades, habilidades)
 * 4. Privacidad y Confirmación (switches de privacidad, resumen)
 */
class EditarPerfilActivity : AppCompatActivity() {

    // ==================== CONTROL DE PASOS ====================
    private var currentStep = 1
    private val totalSteps = 4

    // ==================== HEADER & STEPPER ====================
    private lateinit var btnCerrar: FrameLayout
    private lateinit var tvTitulo: TextView
    private lateinit var tvStepIndicator: TextView
    
    // Stepper visual
    private lateinit var step1Circle: View
    private lateinit var step2Circle: View
    private lateinit var step3Circle: View
    private lateinit var step4Circle: View
    private lateinit var step1Number: TextView
    private lateinit var step2Number: TextView
    private lateinit var step3Number: TextView
    private lateinit var step4Number: TextView
    private lateinit var step1Check: ImageView
    private lateinit var step2Check: ImageView
    private lateinit var step3Check: ImageView
    private lateinit var step4Check: ImageView
    private lateinit var line1: View
    private lateinit var line2: View
    private lateinit var line3: View

    // ==================== CONTENEDORES DE PASOS ====================
    private lateinit var layoutStep1: LinearLayout
    private lateinit var layoutStep2: LinearLayout
    private lateinit var layoutStep3: LinearLayout
    private lateinit var layoutStep4: LinearLayout

    // ==================== PASO 1: INFORMACIÓN BÁSICA ====================
    private lateinit var ivAvatar: ShapeableImageView
    private lateinit var btnCambiarFoto: FrameLayout
    private lateinit var tvDni: TextView
    private lateinit var etNombre: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var iconEmailVerificado: ImageView
    private lateinit var etTelefono: EditText

    // ==================== PASO 2: INFORMACIÓN PERSONAL ====================
    private lateinit var inputFechaNacimiento: ConstraintLayout
    private lateinit var tvFecha: TextView
    private lateinit var rgGenero: RadioGroup
    private lateinit var rbMasculino: RadioButton
    private lateinit var rbFemenino: RadioButton
    private lateinit var rbOtro: RadioButton
    private lateinit var inputUbicacion: ConstraintLayout
    private lateinit var tvUbicacion: TextView
    private lateinit var etBio: EditText

    // ==================== PASO 3: EXPERIENCIA PROFESIONAL ====================
    private lateinit var etExperiencia: EditText
    private lateinit var cbAlbanileria: CheckBox
    private lateinit var cbPintura: CheckBox
    private lateinit var cbCarpinteria: CheckBox
    private lateinit var cbElectricidad: CheckBox
    private lateinit var cbGasfiteria: CheckBox
    private lateinit var cbJardineria: CheckBox
    private lateinit var etHabilidades: EditText

    // ==================== PASO 4: PRIVACIDAD ====================
    private lateinit var switchMostrarTelefono: SwitchCompat
    private lateinit var switchMostrarEmail: SwitchCompat
    private lateinit var switchMostrarUbicacion: SwitchCompat
    private lateinit var tvResumenCompletitud: TextView
    private lateinit var progressBarPerfil: ProgressBar
    private lateinit var tvPorcentajePerfil: TextView

    // ==================== NAVEGACIÓN ====================
    private lateinit var btnAtras: MaterialButton
    private lateinit var btnSiguiente: MaterialButton

    // ==================== DATOS ====================
    private var selectedImageUri: Uri? = null
    private var selectedDate: String = ""
    private var selectedLocation: String = ""
    private var selectedGenero: String = ""
    
    private val departamentos = arrayOf(
        "Ayacucho", "Lima", "Arequipa", "Cusco", "Piura", 
        "La Libertad", "Lambayeque", "Junín", "Ica", "Otro"
    )

    // Launcher para selección de imagen
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                loadImageIntoAvatar(uri)
                showToast("✓ Foto actualizada")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar barra de estado
        BarraEstadoUtils.aplicarColor(this, Color.parseColor("#FFFFFF"))
        
        setContentView(R.layout.dialog_editar_perfil)

        initViews()
        setupListeners()
        setupBackPress()
        loadSimulatedUserData()
        
        // Comenzar en el paso 1
        updateStep(1)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStep > 1) {
                    // Retroceder al paso anterior
                    updateStep(currentStep - 1)
                } else {
                    // Salir de la actividad
                    finish()
                    applyExitTransition()
                }
            }
        })
    }

    private fun applyExitTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.dialog_slide_down,
                R.anim.dialog_slide_down
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.dialog_slide_down, R.anim.dialog_slide_down)
        }
    }

    private fun initViews() {
        // Header
        btnCerrar = findViewById(R.id.btnCerrarEditarPerfil)
        tvTitulo = findViewById(R.id.tvTituloEditarPerfil)
        tvStepIndicator = findViewById(R.id.tvStepIndicator)

        // Stepper circles
        step1Circle = findViewById(R.id.step1Circle)
        step2Circle = findViewById(R.id.step2Circle)
        step3Circle = findViewById(R.id.step3Circle)
        step4Circle = findViewById(R.id.step4Circle)
        step1Number = findViewById(R.id.step1Number)
        step2Number = findViewById(R.id.step2Number)
        step3Number = findViewById(R.id.step3Number)
        step4Number = findViewById(R.id.step4Number)
        step1Check = findViewById(R.id.step1Check)
        step2Check = findViewById(R.id.step2Check)
        step3Check = findViewById(R.id.step3Check)
        step4Check = findViewById(R.id.step4Check)
        line1 = findViewById(R.id.line1)
        line2 = findViewById(R.id.line2)
        line3 = findViewById(R.id.line3)

        // Contenedores
        layoutStep1 = findViewById(R.id.layoutStep1BasicInfo)
        layoutStep2 = findViewById(R.id.layoutStep2PersonalInfo)
        layoutStep3 = findViewById(R.id.layoutStep3WorkerInfo)
        layoutStep4 = findViewById(R.id.layoutStep4Privacy)

        // Paso 1
        ivAvatar = findViewById(R.id.ivEditarPerfilAvatar)
        btnCambiarFoto = findViewById(R.id.btnCambiarFotoPerfil)
        tvDni = findViewById(R.id.tvEditarPerfilDni)
        etNombre = findViewById(R.id.etEditarPerfilNombre)
        etUsername = findViewById(R.id.etEditarPerfilUsername)
        etEmail = findViewById(R.id.etEditarPerfilEmail)
        iconEmailVerificado = findViewById(R.id.iconEmailVerificado)
        etTelefono = findViewById(R.id.etEditarPerfilTelefono)

        // Paso 2
        inputFechaNacimiento = findViewById(R.id.inputFechaNacimiento)
        tvFecha = findViewById(R.id.tvEditarPerfilFecha)
        rgGenero = findViewById(R.id.rgGenero)
        rbMasculino = findViewById(R.id.rbMasculino)
        rbFemenino = findViewById(R.id.rbFemenino)
        rbOtro = findViewById(R.id.rbOtro)
        inputUbicacion = findViewById(R.id.inputUbicacion)
        tvUbicacion = findViewById(R.id.tvEditarPerfilUbicacion)
        etBio = findViewById(R.id.etEditarPerfilBio)

        // Paso 3
        etExperiencia = findViewById(R.id.etEditarPerfilExperiencia)
        cbAlbanileria = findViewById(R.id.cbAlbanileria)
        cbPintura = findViewById(R.id.cbPintura)
        cbCarpinteria = findViewById(R.id.cbCarpinteria)
        cbElectricidad = findViewById(R.id.cbElectricidad)
        cbGasfiteria = findViewById(R.id.cbGasfiteria)
        cbJardineria = findViewById(R.id.cbJardineria)
        etHabilidades = findViewById(R.id.etEditarPerfilHabilidades)

        // Paso 4
        switchMostrarTelefono = findViewById(R.id.switchMostrarTelefono)
        switchMostrarEmail = findViewById(R.id.switchMostrarEmail)
        switchMostrarUbicacion = findViewById(R.id.switchMostrarUbicacion)
        tvResumenCompletitud = findViewById(R.id.tvResumenCompletitud)
        progressBarPerfil = findViewById(R.id.progressBarPerfil)
        tvPorcentajePerfil = findViewById(R.id.tvPorcentajePerfil)

        // Navegación
        btnAtras = findViewById(R.id.btnAtrasStep)
        btnSiguiente = findViewById(R.id.btnSiguienteStep)
    }

    private fun setupListeners() {
        // Cerrar
        btnCerrar.setOnClickListener {
            finish()
            applyExitTransition()
        }

        // Cambiar foto
        btnCambiarFoto.setOnClickListener {
            openImagePicker()
        }

        // Fecha de nacimiento
        inputFechaNacimiento.setOnClickListener {
            showDatePicker()
        }

        // Ubicación
        inputUbicacion.setOnClickListener {
            showLocationPicker()
        }

        // Navegación
        btnAtras.setOnClickListener {
            if (currentStep > 1) {
                updateStep(currentStep - 1)
            }
        }

        btnSiguiente.setOnClickListener {
            handleNextStep()
        }

        // Radio buttons de género
        rgGenero.setOnCheckedChangeListener { _, checkedId ->
            selectedGenero = when (checkedId) {
                R.id.rbMasculino -> "Masculino"
                R.id.rbFemenino -> "Femenino"
                R.id.rbOtro -> "Prefiero no decirlo"
                else -> ""
            }
        }
    }

    private fun loadSimulatedUserData() {
        // Datos simulados del registro (FASE 1 completada)
        tvDni.text = "72345678"
        etNombre.setText("Maria Elena Sanchez Gomez")
        etUsername.setText("maria_chambaya_ayacucho")
        etEmail.setText("maria.sanchez@gmail.com")
        iconEmailVerificado.visibility = View.VISIBLE
        etTelefono.setText("999 123 456")
        
        // Datos opcionales (a completar)
        tvFecha.text = "Selecciona tu fecha"
        tvFecha.setTextColor(Color.parseColor("#9CA3AF"))
        tvUbicacion.text = "Selecciona tu ubicación"
        tvUbicacion.setTextColor(Color.parseColor("#9CA3AF"))
    }

    // ==================== NAVEGACIÓN POR PASOS ====================
    
    private fun updateStep(step: Int) {
        currentStep = step
        
        // Actualizar título e indicador
        tvTitulo.text = when (step) {
            1 -> "Completa tu Perfil"
            2 -> "Información Personal"
            3 -> "Experiencia Profesional"
            4 -> "Privacidad y Confirmación"
            else -> "Completa tu Perfil"
        }
        tvStepIndicator.text = "Paso $step de $totalSteps"

        // Mostrar/ocultar contenedores
        layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
        layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE
        layoutStep4.visibility = if (step == 4) View.VISIBLE else View.GONE

        // Actualizar stepper visual
        updateStepperVisuals()

        // Actualizar botones de navegación
        updateNavigationButtons()

        // Scroll al inicio
        findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollEditarPerfil)
            .smoothScrollTo(0, 0)
    }

    private fun updateStepperVisuals() {
        val brandColor = Color.parseColor("#5B67F7")
        val darkColor = Color.parseColor("#111827")
        val inactiveColor = Color.parseColor("#E2E8F0")
        val mutedText = Color.parseColor("#64748B")

        // Paso 1
        updateSingleStep(1, step1Circle, step1Number, step1Check, brandColor, darkColor, mutedText)
        line1.setBackgroundColor(if (currentStep > 1) darkColor else inactiveColor)

        // Paso 2
        updateSingleStep(2, step2Circle, step2Number, step2Check, brandColor, darkColor, mutedText)
        line2.setBackgroundColor(if (currentStep > 2) darkColor else inactiveColor)

        // Paso 3
        updateSingleStep(3, step3Circle, step3Number, step3Check, brandColor, darkColor, mutedText)
        line3.setBackgroundColor(if (currentStep > 3) darkColor else inactiveColor)

        // Paso 4
        updateSingleStep(4, step4Circle, step4Number, step4Check, brandColor, darkColor, mutedText)
    }

    private fun updateSingleStep(
        stepNumber: Int,
        circle: View,
        numberTv: TextView,
        checkIv: ImageView,
        brandColor: Int,
        darkColor: Int,
        mutedText: Int
    ) {
        when {
            currentStep > stepNumber -> {
                // Completado
                circle.setBackgroundResource(R.drawable.bg_step_circle_completed)
                numberTv.visibility = View.GONE
                checkIv.visibility = View.VISIBLE
            }
            currentStep == stepNumber -> {
                // Activo
                circle.setBackgroundResource(R.drawable.bg_step_circle_active)
                numberTv.visibility = View.VISIBLE
                numberTv.setTextColor(Color.WHITE)
                checkIv.visibility = View.GONE
            }
            else -> {
                // Inactivo
                circle.setBackgroundResource(R.drawable.bg_step_circle_inactive)
                numberTv.visibility = View.VISIBLE
                numberTv.setTextColor(mutedText)
                checkIv.visibility = View.GONE
            }
        }
    }

    private fun updateNavigationButtons() {
        // Botón Atrás solo visible después del paso 1
        btnAtras.visibility = if (currentStep > 1) View.VISIBLE else View.VISIBLE
        btnAtras.isEnabled = currentStep > 1
        btnAtras.alpha = if (currentStep > 1) 1f else 0.5f

        // Botón Siguiente cambia en el último paso
        if (currentStep == totalSteps) {
            btnSiguiente.text = "Finalizar"
            btnSiguiente.icon = null
        } else {
            btnSiguiente.text = "Siguiente"
            btnSiguiente.setIconResource(R.drawable.ic_arrow_forward)
        }
    }

    private fun handleNextStep() {
        // Validar paso actual antes de avanzar
        if (!validateCurrentStep()) {
            return
        }

        if (currentStep < totalSteps) {
            // Avanzar al siguiente paso
            updateStep(currentStep + 1)
        } else {
            // Finalizar y guardar (simulado)
            finalizarActualizacionPerfil()
        }
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> validateStep1()
            2 -> validateStep2()
            3 -> validateStep3()
            4 -> true // Paso 4 no requiere validación obligatoria
            else -> true
        }
    }

    private fun validateStep1(): Boolean {
        val nombre = etNombre.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()

        if (nombre.isEmpty() || nombre.length < 3) {
            etNombre.error = "Ingresa tu nombre completo (mín. 3 caracteres)"
            etNombre.requestFocus()
            showToast("⚠️ Completa tu nombre")
            return false
        }

        if (username.isEmpty() || username.length < 3) {
            etUsername.error = "Ingresa un nombre de usuario válido"
            etUsername.requestFocus()
            showToast("⚠️ Completa tu nombre de usuario")
            return false
        }

        if (telefono.isEmpty() || telefono.length < 9) {
            etTelefono.error = "Ingresa un teléfono válido (mín. 9 dígitos)"
            etTelefono.requestFocus()
            showToast("⚠️ Completa tu teléfono")
            return false
        }

        return true
    }

    private fun validateStep2(): Boolean {
        if (selectedDate.isEmpty()) {
            showToast("⚠️ Selecciona tu fecha de nacimiento")
            return false
        }

        if (selectedGenero.isEmpty()) {
            showToast("⚠️ Selecciona tu género")
            return false
        }

        if (selectedLocation.isEmpty()) {
            showToast("⚠️ Selecciona tu ubicación")
            return false
        }

        return true
    }

    private fun validateStep3(): Boolean {
        val experiencia = etExperiencia.text.toString().trim()
        
        if (experiencia.isEmpty()) {
            etExperiencia.error = "Ingresa tus años de experiencia"
            etExperiencia.requestFocus()
            showToast("⚠️ Ingresa tus años de experiencia")
            return false
        }

        // Validar que al menos una especialidad esté seleccionada
        val tieneEspecialidad = cbAlbanileria.isChecked || cbPintura.isChecked ||
                cbCarpinteria.isChecked || cbElectricidad.isChecked ||
                cbGasfiteria.isChecked || cbJardineria.isChecked

        if (!tieneEspecialidad) {
            showToast("⚠️ Selecciona al menos una especialidad")
            return false
        }

        return true
    }

    private fun finalizarActualizacionPerfil() {
        // Calcular completitud del perfil
        val completitud = calculateProfileCompleteness()
        
        // Mostrar loading
        btnSiguiente.isEnabled = false
        btnSiguiente.text = "Guardando..."

        // Simular guardado (1.5 segundos)
        btnSiguiente.postDelayed({
            // Mostrar éxito
            showSuccessDialog(completitud)
        }, 1500)
    }

    private fun calculateProfileCompleteness(): Int {
        var completedFields = 0
        val totalFields = 15

        // Paso 1
        if (selectedImageUri != null) completedFields++
        if (etNombre.text.isNotEmpty()) completedFields++
        if (etUsername.text.isNotEmpty()) completedFields++
        if (etEmail.text.isNotEmpty()) completedFields++
        if (etTelefono.text.isNotEmpty()) completedFields++

        // Paso 2
        if (selectedDate.isNotEmpty()) completedFields++
        if (selectedGenero.isNotEmpty()) completedFields++
        if (selectedLocation.isNotEmpty()) completedFields++
        if (etBio.text.isNotEmpty()) completedFields++

        // Paso 3
        if (etExperiencia.text.isNotEmpty()) completedFields++
        if (cbAlbanileria.isChecked || cbPintura.isChecked || cbCarpinteria.isChecked ||
            cbElectricidad.isChecked || cbGasfiteria.isChecked || cbJardineria.isChecked
        ) completedFields++
        if (etHabilidades.text.isNotEmpty()) completedFields++

        // Paso 4 (configuraciones de privacidad siempre cuentan)
        completedFields += 3

        return (completedFields * 100) / totalFields
    }

    private fun showSuccessDialog(completitud: Int) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        builder.setTitle("🎉 ¡Perfil Actualizado!")
        builder.setMessage(
            "Tu perfil ha sido actualizado correctamente.\n\n" +
            "✓ Completitud: $completitud%\n" +
            "✓ DNI verificado con RENIEC\n" +
            "✓ Email verificado\n\n" +
            "Tu perfil ahora destaca más en ChambAYA."
        )
        builder.setPositiveButton("Continuar") { dialog, _ ->
            dialog.dismiss()
            setResult(Activity.RESULT_OK)
            finish()
            applyExitTransition()
        }
        builder.setCancelable(false)
        builder.show()
    }

    // ==================== UTILIDADES ====================

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun loadImageIntoAvatar(uri: Uri) {
        try {
            ivAvatar.setImageURI(uri)
        } catch (e: Exception) {
            showToast("Error al cargar imagen")
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.CustomDatePickerTheme,
            { _, year, month, dayOfMonth ->
                selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                tvFecha.text = selectedDate
                tvFecha.setTextColor(Color.parseColor("#111827"))
            },
            calendar.get(Calendar.YEAR) - 25,
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Fecha máxima: hace 18 años (mayoría de edad)
        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.YEAR, -18)
        datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis

        // Fecha mínima: hace 100 años
        val minCalendar = Calendar.getInstance()
        minCalendar.add(Calendar.YEAR, -100)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        datePickerDialog.show()
    }

    private fun showLocationPicker() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        builder.setTitle("Seleccionar Departamento")
        
        builder.setItems(departamentos) { dialog, which ->
            selectedLocation = departamentos[which]
            tvUbicacion.text = selectedLocation
            tvUbicacion.setTextColor(Color.parseColor("#111827"))
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
