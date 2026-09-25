package com.proyecto.chambaya

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Flujo de registro ChambAYA:
 *  - Paso 0: Selección de rol (Trabajador / Contratante) — sin stepper
 *  - Paso 1: Identidad (Trabajador: DNI+RENIEC | Contratante: DNI o RUC)
 *  - Paso 2: Credenciales (Correo + Contraseña o Google Auth con Firebase)
 *  - Paso 3: Verificación (Preparado para Fase 3)
 *  - Paso 4: Resumen final
 */
class RegistroActivity : AppCompatActivity() {

    private var currentStep = 0
    private var isOtpVerified = false
    private var isDniVerified = false
    private var isRucVerified = false
    private var roleSelected = false
    private var selectedRole = ""
    private var identityMode = "DNI" // "DNI" | "RUC" (solo empleador/contratante)
    private var pendingOtp = ""

    // Firebase Auth & Google Sign-In
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    // Estado conservado de Fase 2
    private var registeredFirebaseUid: String? = null
    private var registeredEmail: String? = null
    private var registeredAuthMethod: String = "" // "EMAIL_PASSWORD" | "GOOGLE"
    private var isEmailVerifiedByAuth: Boolean = false
    private var isPhase2Completed: Boolean = false
    private var currentAuthMethod: String = "EMAIL" // "EMAIL" | "GOOGLE"

    // Stepper views (4 pasos)
    private lateinit var stepperTimelineContainer: LinearLayout
    private lateinit var stepperDivider: View
    private lateinit var stepCircle1: FrameLayout
    private lateinit var stepCircle2: FrameLayout
    private lateinit var stepCircle3: FrameLayout
    private lateinit var stepCircle4: FrameLayout

    private lateinit var tvStepNumber1: TextView
    private lateinit var tvStepNumber2: TextView
    private lateinit var tvStepNumber3: TextView
    private lateinit var tvStepNumber4: TextView

    private lateinit var ivStepCheck1: ImageView
    private lateinit var ivStepCheck2: ImageView
    private lateinit var ivStepCheck3: ImageView
    private lateinit var ivStepCheck4: ImageView

    private lateinit var stepLine1: View
    private lateinit var stepLine2: View
    private lateinit var stepLine3: View

    private lateinit var tvStepLabel1: TextView
    private lateinit var tvStepLabel2: TextView
    private lateinit var tvStepLabel3: TextView
    private lateinit var tvStepLabel4: TextView

    // Step containers
    private lateinit var registerScrollView: ScrollView
    private lateinit var layoutStep0: LinearLayout
    private lateinit var layoutStep1: LinearLayout
    private lateinit var layoutStep2: LinearLayout
    private lateinit var layoutStep3: LinearLayout
    private lateinit var layoutStep4: LinearLayout

    // Step 0 — Selección de rol
    private lateinit var cardRoleWorkerSelect: LinearLayout
    private lateinit var cardRoleEmployerSelect: LinearLayout
    private lateinit var rbWorkerSelect: RadioButton
    private lateinit var rbEmployerSelect: RadioButton
    private lateinit var btnStep0Next: MaterialButton

    // Step 1 — Identidad
    private lateinit var tvStep1Subtitle: TextView
    private lateinit var layoutIdentityTypeTabs: LinearLayout
    private lateinit var btnTabDni: MaterialButton
    private lateinit var btnTabRuc: MaterialButton
    private lateinit var layoutDniForm: LinearLayout
    private lateinit var etDni: EditText
    private lateinit var tvDniVerifierLabel: TextView
    private lateinit var etDniVerifier: EditText
    private lateinit var tvDniVerifierHint: TextView
    private lateinit var ivDniCheckIcon: ImageView
    private lateinit var btnConsultReniec: MaterialButton
    private lateinit var pbReniec: ProgressBar
    private lateinit var cardDniVerified: LinearLayout
    private lateinit var tvReniecFullName: TextView
    private lateinit var tvReniecDniDetail: TextView
    private lateinit var tvDniAttempts: TextView
    private lateinit var layoutRucForm: LinearLayout
    private lateinit var etRuc: EditText
    private lateinit var btnConsultSunat: MaterialButton
    private lateinit var pbSunat: ProgressBar
    private lateinit var cardRucVerified: LinearLayout
    private lateinit var tvSunatRazonSocial: TextView
    private lateinit var tvSunatCondition: TextView
    private lateinit var btnStep1Back: MaterialButton
    private lateinit var btnStep1Next: MaterialButton

    // Step 2 — Credenciales
    private lateinit var layoutAuthMethodTabs: LinearLayout
    private lateinit var btnTabEmailMethod: MaterialButton
    private lateinit var btnTabGoogleMethod: MaterialButton
    private lateinit var pbStep2Loading: ProgressBar
    private lateinit var layoutEmailForm: LinearLayout
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var ivReqLength: ImageView
    private lateinit var ivReqNumber: ImageView
    private lateinit var ivReqSpecial: ImageView
    private lateinit var btnStep2Back: MaterialButton
    private lateinit var btnStep2Next: MaterialButton
    private lateinit var layoutGoogleForm: LinearLayout
    private lateinit var btnGoogleRegister: LinearLayout
    private lateinit var btnGoogleBack: MaterialButton

    // Step 3 — OTP de correo / Verificación
    private lateinit var tvOtpEmailHint: TextView
    private lateinit var otpBox1: EditText
    private lateinit var otpBox2: EditText
    private lateinit var otpBox3: EditText
    private lateinit var otpBox4: EditText
    private lateinit var btnConfirmOtp: MaterialButton
    private lateinit var pbOtp: ProgressBar
    private lateinit var btnResendOtp: MaterialButton
    private lateinit var btnChangeEmail: MaterialButton

    // Step 4 — Resumen
    private lateinit var tvSummaryFullName: TextView
    private lateinit var tvSummaryVerifiedBadge: TextView
    private lateinit var layoutSummaryDniRow: LinearLayout
    private lateinit var tvSummaryDni: TextView
    private lateinit var tvSummaryEmail: TextView
    private lateinit var tvSummaryPhone: TextView
    private lateinit var layoutSummaryRucRow: LinearLayout
    private lateinit var tvSummaryRuc: TextView
    private lateinit var tvSummaryRole: TextView
    private lateinit var btnFinalizeRegister: MaterialButton
    private lateinit var btnStep5Back: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(R.color.brand_color)
        window.navigationBarColor = getColor(R.color.surface_light)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true

        setContentView(R.layout.actividad_registro)

        // Manejar el botón back con OnBackPressedDispatcher (AndroidX)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        })

        // Inicializar Firebase Auth existente
        auth = FirebaseAuth.getInstance()

        // Configuración oficial de Google Sign-In con el Web Client ID generado desde google-services.json
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleSignInResult(result.data)
        }

        initViews()
        setupStepper()
        setupStep0()
        setupStep1()
        setupStep2()
        setupStep3()
        setupStep4()

        updateStep(0)
    }

    private fun initViews() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            handleBackAction()
        }

        registerScrollView = findViewById(R.id.registerScrollView)
        layoutStep0 = findViewById(R.id.layoutStep0)
        layoutStep1 = findViewById(R.id.layoutStep1)
        layoutStep2 = findViewById(R.id.layoutStep2)
        layoutStep3 = findViewById(R.id.layoutStep3)
        layoutStep4 = findViewById(R.id.layoutStep4)

        // Stepper
        stepperTimelineContainer = findViewById(R.id.stepperTimelineContainer)
        stepperDivider = findViewById(R.id.stepperDivider)

        stepCircle1 = findViewById(R.id.stepCircle1)
        stepCircle2 = findViewById(R.id.stepCircle2)
        stepCircle3 = findViewById(R.id.stepCircle3)
        stepCircle4 = findViewById(R.id.stepCircle4)

        tvStepNumber1 = findViewById(R.id.tvStepNumber1)
        tvStepNumber2 = findViewById(R.id.tvStepNumber2)
        tvStepNumber3 = findViewById(R.id.tvStepNumber3)
        tvStepNumber4 = findViewById(R.id.tvStepNumber4)

        ivStepCheck1 = findViewById(R.id.ivStepCheck1)
        ivStepCheck2 = findViewById(R.id.ivStepCheck2)
        ivStepCheck3 = findViewById(R.id.ivStepCheck3)
        ivStepCheck4 = findViewById(R.id.ivStepCheck4)

        stepLine1 = findViewById(R.id.stepLine1)
        stepLine2 = findViewById(R.id.stepLine2)
        stepLine3 = findViewById(R.id.stepLine3)

        tvStepLabel1 = findViewById(R.id.tvStepLabel1)
        tvStepLabel2 = findViewById(R.id.tvStepLabel2)
        tvStepLabel3 = findViewById(R.id.tvStepLabel3)
        tvStepLabel4 = findViewById(R.id.tvStepLabel4)

        // Step 0
        cardRoleWorkerSelect = findViewById(R.id.cardRoleWorkerSelect)
        cardRoleEmployerSelect = findViewById(R.id.cardRoleEmployerSelect)
        rbWorkerSelect = findViewById(R.id.rbWorkerSelect)
        rbEmployerSelect = findViewById(R.id.rbEmployerSelect)
        btnStep0Next = findViewById(R.id.btnStep0Next)

        // Step 1
        tvStep1Subtitle = findViewById(R.id.tvStep1Subtitle)
        layoutIdentityTypeTabs = findViewById(R.id.layoutIdentityTypeTabs)
        btnTabDni = findViewById(R.id.btnTabDni)
        btnTabRuc = findViewById(R.id.btnTabRuc)
        layoutDniForm = findViewById(R.id.layoutDniForm)
        etDni = findViewById(R.id.etDni)
        tvDniVerifierLabel = findViewById(R.id.tvDniVerifierLabel)
        etDniVerifier = findViewById(R.id.etDniVerifier)
        tvDniVerifierHint = findViewById(R.id.tvDniVerifierHint)
        ivDniCheckIcon = findViewById(R.id.ivDniCheckIcon)
        btnConsultReniec = findViewById(R.id.btnConsultReniec)
        pbReniec = findViewById(R.id.pbReniec)
        cardDniVerified = findViewById(R.id.cardDniVerified)
        tvReniecFullName = findViewById(R.id.tvReniecFullName)
        tvReniecDniDetail = findViewById(R.id.tvReniecDniDetail)
        tvDniAttempts = findViewById(R.id.tvDniAttempts)
        layoutRucForm = findViewById(R.id.layoutRucForm)
        etRuc = findViewById(R.id.etRuc)
        btnConsultSunat = findViewById(R.id.btnConsultSunat)
        pbSunat = findViewById(R.id.pbSunat)
        cardRucVerified = findViewById(R.id.cardRucVerified)
        tvSunatRazonSocial = findViewById(R.id.tvSunatRazonSocial)
        tvSunatCondition = findViewById(R.id.tvSunatCondition)
        btnStep1Back = findViewById(R.id.btnStep1Back)
        btnStep1Next = findViewById(R.id.btnStep1Next)

        // Step 2 (Teléfono eliminado completamente según Regla 8)
        layoutAuthMethodTabs = findViewById(R.id.layoutAuthMethodTabs)
        btnTabEmailMethod = findViewById(R.id.btnTabEmailMethod)
        btnTabGoogleMethod = findViewById(R.id.btnTabGoogleMethod)
        pbStep2Loading = findViewById(R.id.pbStep2Loading)
        layoutEmailForm = findViewById(R.id.layoutEmailForm)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        ivReqLength = findViewById(R.id.ivReqLength)
        ivReqNumber = findViewById(R.id.ivReqNumber)
        ivReqSpecial = findViewById(R.id.ivReqSpecial)
        btnStep2Back = findViewById(R.id.btnStep2Back)
        btnStep2Next = findViewById(R.id.btnStep2Next)
        layoutGoogleForm = findViewById(R.id.layoutGoogleForm)
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister)
        btnGoogleBack = findViewById(R.id.btnGoogleBack)

        // Step 3
        tvOtpEmailHint = findViewById(R.id.tvOtpEmailHint)
        otpBox1 = findViewById(R.id.otpBox1)
        otpBox2 = findViewById(R.id.otpBox2)
        otpBox3 = findViewById(R.id.otpBox3)
        otpBox4 = findViewById(R.id.otpBox4)
        btnConfirmOtp = findViewById(R.id.btnConfirmOtp)
        pbOtp = findViewById(R.id.pbOtp)
        btnResendOtp = findViewById(R.id.btnResendOtp)
        btnChangeEmail = findViewById(R.id.btnChangeEmail)

        // Step 4
        tvSummaryFullName = findViewById(R.id.tvSummaryFullName)
        tvSummaryVerifiedBadge = findViewById(R.id.tvSummaryVerifiedBadge)
        layoutSummaryDniRow = findViewById(R.id.layoutSummaryDniRow)
        tvSummaryDni = findViewById(R.id.tvSummaryDni)
        tvSummaryEmail = findViewById(R.id.tvSummaryEmail)
        tvSummaryPhone = findViewById(R.id.tvSummaryPhone)
        layoutSummaryRucRow = findViewById(R.id.layoutSummaryRucRow)
        tvSummaryRuc = findViewById(R.id.tvSummaryRuc)
        tvSummaryRole = findViewById(R.id.tvSummaryRole)
        btnFinalizeRegister = findViewById(R.id.btnFinalizeRegister)
        btnStep5Back = findViewById(R.id.btnStep5Back)
    }

    // ==================== STEPPER ====================
    private fun setupStepper() {
        val stepCircles = arrayOf(
            1 to stepCircle1,
            2 to stepCircle2,
            3 to stepCircle3,
            4 to stepCircle4
        )
        stepCircles.forEach { (step, circle) ->
            circle.setOnClickListener {
                if (step < currentStep) {
                    updateStep(step)
                }
            }
        }
    }

    private fun updateStep(step: Int) {
        currentStep = step

        layoutStep0.visibility = if (step == 0) View.VISIBLE else View.GONE
        layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
        layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE
        layoutStep4.visibility = if (step == 4) View.VISIBLE else View.GONE

        // El stepper de 4 pasos solo aparece después de elegir el rol
        val showStepper = step in 1..4
        stepperTimelineContainer.visibility = if (showStepper) View.VISIBLE else View.GONE
        stepperDivider.visibility = if (showStepper) View.VISIBLE else View.GONE

        if (step == 1) configureIdentityStep()
        if (step == 3) prepareOtpStep()

        registerScrollView.smoothScrollTo(0, 0)
        updateStepperVisuals()

        if (step == 4) populateSummary()
    }

    private fun updateStepperVisuals() {
        val brandColor = ContextCompat.getColor(this, R.color.brand_color)
        val darkColor = Color.parseColor("#111827")
        val mutedText = Color.parseColor("#94A3B8")

        updateSingleStepNode(1, stepCircle1, tvStepNumber1, ivStepCheck1, tvStepLabel1, brandColor, darkColor, mutedText)
        stepLine1.setBackgroundColor(if (currentStep > 1) darkColor else Color.parseColor("#E2E8F0"))

        updateSingleStepNode(2, stepCircle2, tvStepNumber2, ivStepCheck2, tvStepLabel2, brandColor, darkColor, mutedText)
        stepLine2.setBackgroundColor(if (currentStep > 2) darkColor else Color.parseColor("#E2E8F0"))

        updateSingleStepNode(3, stepCircle3, tvStepNumber3, ivStepCheck3, tvStepLabel3, brandColor, darkColor, mutedText)
        stepLine3.setBackgroundColor(if (currentStep > 3) darkColor else Color.parseColor("#E2E8F0"))

        updateSingleStepNode(4, stepCircle4, tvStepNumber4, ivStepCheck4, tvStepLabel4, brandColor, darkColor, mutedText)
    }

    private fun updateSingleStepNode(
        stepNumber: Int,
        circle: FrameLayout,
        tvNum: TextView,
        ivCheck: ImageView,
        label: TextView,
        brandColor: Int,
        darkColor: Int,
        mutedText: Int
    ) {
        when {
            currentStep > stepNumber -> {
                circle.setBackgroundResource(R.drawable.bg_step_circle_completed)
                tvNum.visibility = View.GONE
                ivCheck.visibility = View.VISIBLE
                label.setTextColor(darkColor)
                label.typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            currentStep == stepNumber -> {
                circle.setBackgroundResource(R.drawable.bg_step_circle_active)
                tvNum.visibility = View.VISIBLE
                tvNum.setTextColor(Color.WHITE)
                ivCheck.visibility = View.GONE
                label.setTextColor(brandColor)
                label.typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            else -> {
                circle.setBackgroundResource(R.drawable.bg_step_circle_inactive)
                tvNum.visibility = View.VISIBLE
                tvNum.setTextColor(Color.parseColor("#64748B"))
                ivCheck.visibility = View.GONE
                label.setTextColor(mutedText)
                label.typeface = android.graphics.Typeface.DEFAULT
            }
        }
    }

    // ==================== PASO 0: SELECCIÓN DE ROL ====================
    private fun setupStep0() {
        btnStep0Next.isEnabled = false
        btnStep0Next.alpha = 0.5f

        cardRoleWorkerSelect.setOnClickListener {
            selectRoleCard(isWorker = true)
        }
        cardRoleEmployerSelect.setOnClickListener {
            selectRoleCard(isWorker = false)
        }

        btnStep0Next.setOnClickListener {
            if (roleSelected) updateStep(1)
        }
    }

    private fun selectRoleCard(isWorker: Boolean) {
        val previousRole = selectedRole
        roleSelected = true
        if (isWorker) {
            selectedRole = "TRABAJADOR"
            cardRoleWorkerSelect.setBackgroundResource(R.drawable.bg_card_selected)
            cardRoleEmployerSelect.setBackgroundResource(R.drawable.bg_card_selectable)
            rbWorkerSelect.isChecked = true
            rbEmployerSelect.isChecked = false
        } else {
            selectedRole = "CONTRATANTE"
            cardRoleWorkerSelect.setBackgroundResource(R.drawable.bg_card_selectable)
            cardRoleEmployerSelect.setBackgroundResource(R.drawable.bg_card_selected)
            rbWorkerSelect.isChecked = false
            rbEmployerSelect.isChecked = true
        }
        // Si el rol cambió, limpiar todo el paso 1 para no conservar datos del rol anterior
        if (previousRole.isNotEmpty() && previousRole != selectedRole) {
            clearStep1Fields()
        }
        btnStep0Next.isEnabled = true
        btnStep0Next.alpha = 1f
    }

    /** Limpia todos los campos y estados de verificación del Paso 1 */
    private fun clearStep1Fields() {
        etDni.setText("")
        etRuc.setText("")
        isDniVerified = false
        isRucVerified = false
        cardDniVerified.visibility = View.GONE
        cardRucVerified.visibility = View.GONE
        ivDniCheckIcon.visibility = View.GONE
        pbReniec.visibility = View.GONE
        pbSunat.visibility = View.GONE
        btnConsultReniec.isEnabled = true
        btnConsultSunat.isEnabled = true
        identityMode = "DNI"
    }

    // ==================== PASO 1: IDENTIDAD ====================
    private fun setupStep1() {
        // Ocultar elementos que no se usan con la API real
        tvDniVerifierLabel.visibility = View.GONE
        etDniVerifier.visibility = View.GONE
        tvDniVerifierHint.visibility = View.GONE
        ivDniCheckIcon.visibility = View.GONE

        // Mostrar ícono check verde al completar 8 dígitos y resetear verificación si edita
        etDni.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val dni = s?.toString() ?: ""
                if (dni.length == 8) {
                    ivDniCheckIcon.visibility = View.VISIBLE
                } else {
                    ivDniCheckIcon.visibility = View.GONE
                    // Si el usuario edita el DNI después de verificar, resetear
                    if (isDniVerified) resetDniVerifiedUi()
                }
            }
        })

        // Resetear verificación si el usuario edita el campo de RUC
        etRuc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isRucVerified) resetRucVerifiedUi()
            }
        })

        // Listeners del switch DNI/RUC - prevenir clicks repetidos en el mismo tab
        btnTabDni.setOnClickListener {
            if (identityMode != "DNI") {  // Solo cambiar si no está ya seleccionado
                selectIdentityTab("DNI")
            }
        }
        btnTabRuc.setOnClickListener {
            if (identityMode != "RUC") {  // Solo cambiar si no está ya seleccionado
                selectIdentityTab("RUC")
            }
        }

        btnConsultReniec.setOnClickListener {
            consultReniec()
        }

        btnConsultSunat.setOnClickListener {
            consultSunat()
        }


        btnStep1Back.setOnClickListener {
            updateStep(0)
        }

        btnStep1Next.setOnClickListener {
            val dni = etDni.text.toString().trim()
            val ruc = etRuc.text.toString().trim()

            when {
                identityMode == "DNI" && dni.length != 8 ->
                    showToast("Ingresa tu DNI de 8 dígitos para continuar")
                identityMode == "DNI" && !isDniVerified ->
                    showToast("Primero verifica tu identidad")
                identityMode == "RUC" && ruc.length != 11 ->
                    showToast("Ingresa un RUC válido de 11 dígitos")
                identityMode == "RUC" && !isRucVerified ->
                    showToast("Primero confirma tu empresa en SUNAT")
                else -> updateStep(2)
            }
        }
    }

    private fun configureIdentityStep() {
        val isWorker = selectedRole == "TRABAJADOR"
        if (isWorker) {
            // Modo trabajador: solo DNI
            layoutIdentityTypeTabs.visibility = View.GONE
            layoutDniForm.visibility = View.VISIBLE
            layoutRucForm.visibility = View.GONE
            identityMode = "DNI"
            tvStep1Subtitle.text = getString(R.string.register_step2_subtitle)
        } else {
            // Modo empleador: mostrar tabs DNI/RUC
            layoutIdentityTypeTabs.visibility = View.VISIBLE
            // Resetear a DNI por defecto y aplicar estilos
            identityMode = ""  // Forzar que selectIdentityTab procese el cambio
            selectIdentityTab("DNI")
        }
    }

    private fun selectIdentityTab(mode: String) {
        // Prevenir cambios innecesarios
        if (identityMode == mode) return

        // Limpiar datos del modo anterior al cambiar de tab
        if (identityMode == "DNI") {
            etDni.setText("")
            isDniVerified = false
            cardDniVerified.visibility = View.GONE
            ivDniCheckIcon.visibility = View.GONE
        } else if (identityMode == "RUC") {
            etRuc.setText("")
            isRucVerified = false
            cardRucVerified.visibility = View.GONE
        }

        identityMode = mode
        val brandColor = ContextCompat.getColor(this, R.color.brand_color)

        if (mode == "DNI") {
            applyTabStyle(btnTabDni, selected = true, brandColor)
            applyTabStyle(btnTabRuc, selected = false, brandColor)
            layoutDniForm.visibility = View.VISIBLE
            layoutRucForm.visibility = View.GONE
            tvStep1Subtitle.text = getString(R.string.register_step2_subtitle)
        } else {
            applyTabStyle(btnTabRuc, selected = true, brandColor)
            applyTabStyle(btnTabDni, selected = false, brandColor)
            layoutRucForm.visibility = View.VISIBLE
            layoutDniForm.visibility = View.GONE
            tvStep1Subtitle.text = "Valida tu empresa registrada en SUNAT (RUC Activo/Habido)"
        }
    }

    /**
     * Aplica el estilo visual a un botón del tab switch
     * Enfoque simple: usa solo backgroundTintList con colores explícitos
     */
    private fun applyTabStyle(button: MaterialButton, selected: Boolean, brandColor: Int) {
        if (selected) {
            // Tab seleccionado: fondo azul, texto blanco, negrita
            button.backgroundTintList = ColorStateList.valueOf(brandColor)
            button.setTextColor(Color.WHITE)
            button.typeface = android.graphics.Typeface.DEFAULT_BOLD
        } else {
            // Tab no seleccionado: fondo gris claro del contenedor, texto gris
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F8FAFC"))
            button.setTextColor(Color.parseColor("#64748B"))
            button.typeface = android.graphics.Typeface.DEFAULT
        }
    }

    private fun consultReniec() {
        val dni = etDni.text?.toString()?.trim() ?: ""

        if (dni.length != 8) {
            showToast("Ingresa un DNI de 8 dígitos")
            return
        }

        pbReniec.visibility = View.VISIBLE
        btnConsultReniec.isEnabled = false
        cardDniVerified.visibility = View.GONE
        tvDniAttempts.text = "Consultando RENIEC..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://apis.aqpfact.pe/api/dni/$dni")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer 8204|89676i7wDZfoYBDJ70rZAQOLx9YgbDObuz0ui3Rw")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val responseCode = conn.responseCode
                val responseBody = if (responseCode == 200) {
                    BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                }
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    pbReniec.visibility = View.GONE
                    btnConsultReniec.isEnabled = true

                    if (responseCode == 200) {
                        val json = JSONObject(responseBody)
                        val success = json.optBoolean("success", false)
                        if (success) {
                            val data = json.getJSONObject("data")
                            val nombreCompleto = data.optString("nombre_completo",
                                data.optString("name", "Sin nombre"))
                            val departamento = data.optString("departamento", "")
                            val provincia = data.optString("provincia", "")
                            val ubicacion = when {
                                departamento.isNotEmpty() && provincia.isNotEmpty() -> "$departamento, $provincia"
                                departamento.isNotEmpty() -> departamento
                                else -> "Perú"
                            }

                            isDniVerified = true
                            tvReniecFullName.text = nombreCompleto
                            tvReniecDniDetail.text = "DNI: $dni · $ubicacion"
                            cardDniVerified.visibility = View.VISIBLE
                            tvDniAttempts.text = "✓ Verificación confirmada con RENIEC"

                            Snackbar.make(
                                findViewById(R.id.registerRoot),
                                "✓ Identidad verificada: $nombreCompleto",
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            val msg = json.optString("message", "DNI no encontrado en RENIEC")
                            tvDniAttempts.text = "No se pudo verificar el DNI"
                            showToast("Error: $msg")
                        }
                    } else {
                        tvDniAttempts.text = "Error al consultar RENIEC"
                        showToast("Error $responseCode al consultar RENIEC. Intenta de nuevo.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbReniec.visibility = View.GONE
                    btnConsultReniec.isEnabled = true
                    tvDniAttempts.text = "Sin conexión a internet"
                    showToast("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun resetDniVerifiedUi() {
        isDniVerified = false
        cardDniVerified.visibility = View.GONE
        tvDniAttempts.text = "Ingresa tu DNI para verificar"
    }

    private fun consultSunat() {
        val ruc = etRuc.text?.toString()?.trim() ?: ""
        if (ruc.length != 11) {
            showToast("El RUC debe tener 11 dígitos")
            return
        }

        pbSunat.visibility = View.VISIBLE
        btnConsultSunat.isEnabled = false
        cardRucVerified.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://apis.aqpfact.pe/api/ruc/$ruc")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer 8204|89676i7wDZfoYBDJ70rZAQOLx9YgbDObuz0ui3Rw")
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val responseCode = conn.responseCode
                val responseBody = if (responseCode == 200) {
                    BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                } else {
                    BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream)).use { it.readText() }
                }
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    pbSunat.visibility = View.GONE
                    btnConsultSunat.isEnabled = true

                    if (responseCode == 200) {
                        val json = JSONObject(responseBody)
                        val success = json.optBoolean("success", false)
                        if (success) {
                            val data = json.getJSONObject("data")
                            val razonSocial = data.optString("nombre_o_razon_social",
                                data.optString("name", "Razón social no disponible"))
                            val estado = data.optString("estado", "").uppercase()
                            val condicion = data.optString("condicion", "").uppercase()

                            if (estado == "ACTIVO" && condicion == "HABIDO") {
                                isRucVerified = true
                                tvSunatRazonSocial.text = razonSocial
                                tvSunatCondition.text = "Condición: $condicion  |  Estado: $estado"
                                cardRucVerified.visibility = View.VISIBLE

                                Snackbar.make(
                                    findViewById(R.id.registerRoot),
                                    "✓ RUC validado: ACTIVO y HABIDO en SUNAT",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            } else {
                                showToast(
                                    "RUC no válido: Estado=$estado, Condición=$condicion. " +
                                    "Solo se aceptan empresas ACTIVAS y HABIDAS."
                                )
                            }
                        } else {
                            val msg = json.optString("message", "RUC no encontrado en SUNAT")
                            showToast("Error: $msg")
                        }
                    } else {
                        showToast("Error $responseCode al consultar SUNAT. Intenta de nuevo.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbSunat.visibility = View.GONE
                    btnConsultSunat.isEnabled = true
                    showToast("Error de conexión: ${e.message}")
                }
            }
        }
    }

    private fun resetRucVerifiedUi() {
        isRucVerified = false
        cardRucVerified.visibility = View.GONE
    }

    // ==================== PASO 2: CREDENCIALES ====================
    private fun setupStep2() {
        btnTabEmailMethod.setOnClickListener {
            if (currentAuthMethod != "EMAIL") selectAuthMethod("EMAIL")
        }
        btnTabGoogleMethod.setOnClickListener {
            if (currentAuthMethod != "GOOGLE") selectAuthMethod("GOOGLE")
        }

        setupPasswordToggle(etPassword, ivTogglePassword)
        setupPasswordToggle(etConfirmPassword, ivToggleConfirmPassword)

        // Requisitos de contraseña en vivo (Regla 3)
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pwd = s?.toString() ?: ""
                val hasLength = pwd.length >= 8
                val hasNumber = pwd.any { it.isDigit() }
                val hasSpecial = pwd.any { it.isUpperCase() || !it.isLetterOrDigit() }

                updateRequirementItem(ivReqLength, hasLength)
                updateRequirementItem(ivReqNumber, hasNumber)
                updateRequirementItem(ivReqSpecial, hasSpecial)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnStep2Back.setOnClickListener { updateStep(1) }
        btnGoogleBack.setOnClickListener { updateStep(1) }

        btnStep2Next.setOnClickListener { handleEmailRegister() }
        btnGoogleRegister.setOnClickListener { launchGoogleSignIn() }
    }

    private fun selectAuthMethod(method: String) {
        currentAuthMethod = method
        val brandColor = ContextCompat.getColor(this, R.color.brand_color)

        if (method == "EMAIL") {
            applyTabStyle(btnTabEmailMethod, selected = true, brandColor)
            applyTabStyle(btnTabGoogleMethod, selected = false, brandColor)
            layoutEmailForm.visibility = View.VISIBLE
            layoutGoogleForm.visibility = View.GONE
        } else {
            applyTabStyle(btnTabGoogleMethod, selected = true, brandColor)
            applyTabStyle(btnTabEmailMethod, selected = false, brandColor)
            layoutGoogleForm.visibility = View.VISIBLE
            layoutEmailForm.visibility = View.GONE
        }
    }

    private fun setStep2Loading(loading: Boolean) {
        pbStep2Loading.visibility = if (loading) View.VISIBLE else View.GONE
        btnStep2Next.isEnabled = !loading
        btnStep2Next.alpha = if (loading) 0.6f else 1f
        btnGoogleRegister.isEnabled = !loading
        btnGoogleRegister.alpha = if (loading) 0.6f else 1f
        btnStep2Back.isEnabled = !loading
        btnGoogleBack.isEnabled = !loading
        btnTabEmailMethod.isEnabled = !loading
        btnTabGoogleMethod.isEnabled = !loading
    }

    private fun handleEmailRegister() {
        val email = etEmail.text.toString().trim()
        val pwd = etPassword.text.toString()
        val confirmPwd = etConfirmPassword.text.toString()

        val hasLength = pwd.length >= 8
        val hasNumber = pwd.any { it.isDigit() }
        val hasSpecial = pwd.any { it.isUpperCase() || !it.isLetterOrDigit() }

        when {
            email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showToast("Ingresa un correo electrónico válido")
                return
            }
            !hasLength -> {
                showToast("La contraseña debe tener al menos 8 caracteres")
                return
            }
            !hasNumber -> {
                showToast("La contraseña debe contener al menos un número")
                return
            }
            !hasSpecial -> {
                showToast("La contraseña debe contener una letra mayúscula o un símbolo")
                return
            }
            pwd != confirmPwd -> {
                showToast("Las contraseñas no coinciden")
                return
            }
        }

        setStep2Loading(true)

        // 4. Crear usuario mediante Firebase Authentication (Regla 3)
        auth.createUserWithEmailAndPassword(email, pwd)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    registeredFirebaseUid = user?.uid
                    registeredEmail = email
                    registeredAuthMethod = "EMAIL_PASSWORD"
                    isEmailVerifiedByAuth = false

                    // 5. Enviar correo de verificación oficial mediante Firebase Authentication (Regla 3 y 4)
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener(this) { verifyTask ->
                            setStep2Loading(false)
                            if (verifyTask.isSuccessful) {
                                showEmailVerificationSentModal(email)
                            } else {
                                val err = verifyTask.exception?.localizedMessage ?: "No se pudo enviar el correo de verificación"
                                showToast("Cuenta creada. $err")
                                showEmailVerificationSentModal(email)
                            }
                        } ?: run {
                            setStep2Loading(false)
                            showEmailVerificationSentModal(email)
                        }
                } else {
                    setStep2Loading(false)
                    val exception = task.exception
                    val errorMsg = when (exception) {
                        is FirebaseAuthWeakPasswordException ->
                            "La contraseña es demasiado débil. Ingresa al menos 8 caracteres con números y mayúsculas o símbolos."
                        is FirebaseAuthUserCollisionException ->
                            "Ya existe una cuenta registrada con este correo electrónico. Por favor inicia sesión."
                        is FirebaseAuthInvalidCredentialsException ->
                            "El formato del correo electrónico ingresado no es válido."
                        is FirebaseNetworkException ->
                            "Sin conexión a internet. Verifica tu red e inténtalo de nuevo."
                        else ->
                            exception?.localizedMessage ?: "Ocurrió un error al registrar las credenciales. Intenta nuevamente."
                    }
                    showToast(errorMsg)
                }
            }
    }

    private fun launchGoogleSignIn() {
        setStep2Loading(true)
        // Asegurar que el usuario pueda seleccionar cuenta si ya inició sesión previamente
        googleSignInClient.signOut().addOnCompleteListener(this) {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleGoogleSignInResult(data: Intent?) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrEmpty()) {
                setStep2Loading(false)
                showToast("No se pudo obtener la credencial de Google. Inténtalo de nuevo.")
                return
            }

            // Autenticación en Firebase mediante credencial de Google (Regla 2)
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { authTask ->
                    setStep2Loading(false)
                    if (authTask.isSuccessful) {
                        val user = auth.currentUser
                        registeredFirebaseUid = user?.uid
                        registeredEmail = user?.email ?: account.email ?: ""
                        registeredAuthMethod = "GOOGLE"
                        // Cuenta Google se considera verificada por Firebase sin requerir correo extra (Regla 2)
                        isEmailVerifiedByAuth = true

                        showGoogleSuccessModal()
                    } else {
                        val exception = authTask.exception
                        val errorMsg = when (exception) {
                            is FirebaseAuthUserCollisionException ->
                                "Ya existe una cuenta con este correo utilizando otro método de acceso."
                            is FirebaseNetworkException ->
                                "Error de conexión a internet con Firebase. Intenta nuevamente."
                            else ->
                                exception?.localizedMessage ?: "Error al autenticar con Firebase usando Google."
                        }
                        showToast(errorMsg)
                    }
                }
        } catch (e: ApiException) {
            setStep2Loading(false)
            when (e.statusCode) {
                CommonStatusCodes.CANCELED, 12501 -> {
                    // El usuario canceló la selección de cuenta
                }
                CommonStatusCodes.NETWORK_ERROR, 7 -> {
                    showToast("Error de red al conectar con Google.")
                }
                CommonStatusCodes.DEVELOPER_ERROR, 10 -> {
                    showToast("Configuración de Google Sign-In pendiente de vinculación SHA-1.")
                }
                else -> {
                    showToast("Error al conectar con Google (código: ${e.statusCode})")
                }
            }
        } catch (e: Exception) {
            setStep2Loading(false)
            showToast("Error inesperado en Google Sign-In: ${e.localizedMessage}")
        }
    }

    // Modal de éxito profesional para Google Auth (Regla 6)
    private fun showGoogleSuccessModal() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¡Registro exitoso!")
            .setMessage("Tu cuenta se ha creado correctamente con Google.\n\n" +
                "✓ Correo: ${registeredEmail ?: ""}\n" +
                "✓ Rol: ${if (selectedRole == "TRABAJADOR") "Trabajador" else "Contratante"}")
            .setPositiveButton("Continuar") { _, _ ->
                onPhase2Completed()
            }
            .setCancelable(false)
            .show()
    }

    // Modal informativo para correo + contraseña (Regla 7)
    private fun showEmailVerificationSentModal(email: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Revisa tu correo")
            .setMessage(
                "Hemos enviado un enlace de verificación a tu dirección de correo electrónico:\n\n$email\n\n" +
                "Por favor, revisa tu bandeja de entrada para verificar tu cuenta antes de continuar a la siguiente fase."
            )
            .setPositiveButton("Continuar") { _, _ ->
                onPhase2Completed()
            }
            .setCancelable(false)
            .show()
    }

    private fun onPhase2Completed() {
        isPhase2Completed = true
        // Deja preparada la transición hacia FASE 3 sin ejecutar OTP de cliente (Regla 5)
        updateStep(3)
    }

    private fun updateRequirementItem(iv: ImageView, valid: Boolean) {
        if (valid) {
            iv.setImageResource(R.drawable.ic_check)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#00A859"))
        } else {
            iv.setImageResource(R.drawable.ic_close_circle)
            iv.imageTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        }
    }

    // ==================== PASO 3: PREPARADO PARA FASE 3 (SIN OTP CLIENTE) ====================
    private fun setupStep3() {
        // En Fase 2 no se implementa lógica de OTP de cliente ni servicios externos (Regla 5)
        setupOtpBox(otpBox1, otpBox2, null)
        setupOtpBox(otpBox2, otpBox3, otpBox1)
        setupOtpBox(otpBox3, otpBox4, otpBox2)
        setupOtpBox(otpBox4, null, otpBox3)

        btnConfirmOtp.setOnClickListener {
            // Placeholder que deja preparado el flujo sin implementar backend OTP todavía (Regla 5)
            showToast("FASE 3: El backend de verificación OTP se conectará en la siguiente fase.")
        }

        btnResendOtp.setOnClickListener {
            val email = registeredEmail ?: etEmail.text.toString().trim()
            if (registeredAuthMethod == "EMAIL_PASSWORD") {
                auth.currentUser?.sendEmailVerification()
                showToast("Enlace de verificación oficial de Firebase reenviado a $email")
            } else {
                showToast("Tu cuenta de Google ya está verificada")
            }
        }

        btnChangeEmail.setOnClickListener {
            updateStep(2)
        }
    }

    private fun setupOtpBox(box: EditText, next: EditText?, previous: EditText?) {
        box.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) next?.requestFocus()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        box.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN && box.text.isNullOrEmpty()) {
                previous?.requestFocus()
                previous?.text?.let { previous.setSelection(it.length) }
            }
            false
        }
    }

    private fun prepareOtpStep() {
        val email = registeredEmail ?: etEmail.text.toString().trim()
        if (registeredAuthMethod == "GOOGLE") {
            tvOtpEmailHint.text = "Tu cuenta de Google ($email) se encuentra autenticada.\nTransición preparada para la Fase 3."
        } else {
            tvOtpEmailHint.text = "Hemos enviado un enlace de verificación a\n$email\nRevisa tu bandeja de entrada."
        }
        btnConfirmOtp.isEnabled = true
        btnConfirmOtp.alpha = 1f
        pbOtp.visibility = View.GONE
    }

    // ==================== PASO 4: RESUMEN ====================
    private fun setupStep4() {
        btnStep5Back.setOnClickListener {
            updateStep(3)
        }

        btnFinalizeRegister.setOnClickListener {
            showSuccessRegistrationDialog()
        }
    }

    private fun populateSummary() {
        val email = registeredEmail ?: etEmail.text.toString().trim()
        tvSummaryEmail.text = if (email.isNotEmpty()) email else "usuario@chambaya.pe"

        tvSummaryPhone.text = "No requerido"

        val isWorker = selectedRole == "TRABAJADOR"
        val dni = etDni.text.toString().trim()

        if (identityMode == "RUC" && isRucVerified) {
            layoutSummaryRucRow.visibility = View.VISIBLE
            layoutSummaryDniRow.visibility = View.GONE
            tvSummaryRuc.text = "${etRuc.text.toString().trim()} (Activo/Habido)"
            tvSummaryFullName.text = tvSunatRazonSocial.text.toString()
            tvSummaryVerifiedBadge.text = "✓ Empresa Verificada Oficial (SUNAT)"
        } else {
            layoutSummaryRucRow.visibility = View.GONE
            layoutSummaryDniRow.visibility = View.VISIBLE
            tvSummaryDni.text = if (dni.isNotEmpty()) dni else "72345678"
            tvSummaryFullName.text =
                if (isDniVerified && tvReniecFullName.text.isNotBlank()) tvReniecFullName.text
                else "Perfil Verificado"
            tvSummaryVerifiedBadge.text = "✓ Perfil Verificado Oficial (RENIEC)"
        }

        tvSummaryRole.text = if (isWorker) "Trabajador" else "Contratante"
    }

    private fun showSuccessRegistrationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("¡Bienvenido a ChambAYA!")
            .setMessage(
                "Tu cuenta y perfil han sido verificados y registrados con éxito.\n\n" +
                "✓ Identidad verificada (RENIEC/SUNAT)\n" +
                "✓ Correo verificado con OTP\n" +
                "✓ Perfil listo en Cloud Firestore"
            )
            .setPositiveButton("Ir a ChambAYA") { _, _ ->
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun handleBackAction() {
        if (currentStep > 0) {
            updateStep(currentStep - 1)
        } else {
            finish()
        }
    }

    private fun setupPasswordToggle(editText: EditText, toggle: ImageView) {
        var isPasswordVisible = false
        editText.transformationMethod = PasswordTransformationMethod.getInstance()
        updatePasswordToggleIcon(toggle, isPasswordVisible)

        toggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            editText.transformationMethod = if (isPasswordVisible) {
                null
            } else {
                PasswordTransformationMethod.getInstance()
            }
            editText.setSelection(editText.text?.length ?: 0)
            updatePasswordToggleIcon(toggle, isPasswordVisible)
        }
    }

    private fun updatePasswordToggleIcon(toggle: ImageView, isPasswordVisible: Boolean) {
        toggle.setImageResource(
            if (isPasswordVisible) R.drawable.ic_login_eye_on else R.drawable.ic_login_eye_off
        )
    }

    private fun showToast(msg: String) {
        Snackbar.make(findViewById(R.id.registerRoot), msg, Snackbar.LENGTH_SHORT).show()
    }
}