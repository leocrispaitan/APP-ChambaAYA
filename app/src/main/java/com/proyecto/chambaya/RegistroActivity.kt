package com.proyecto.chambaya

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

/**
 * Flujo de registro rediseñado:
 *  - Paso 0: Selección de rol (Trabajador / Empleador) — sin stepper
 *  - Paso 1: Identidad (Trabajador: DNI+RENIEC | Empleador: DNI o RUC)
 *  - Paso 2: Credenciales (teléfono, correo, contraseña)
 *  - Paso 3: Verificación por OTP de correo (4 dígitos)
 *  - Paso 4: Resumen final y creación de cuenta
 */
class RegistroActivity : AppCompatActivity() {

    private var currentStep = 0
    private var isOtpVerified = false
    private var isDniVerified = false
    private var isRucVerified = false
    private var roleSelected = false
    private var selectedRole = ""
    private var identityMode = "DNI" // "DNI" | "RUC" (solo empleador)
    private var pendingOtp = ""

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
    private lateinit var etDniVerifier: EditText
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
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var ivReqLength: ImageView
    private lateinit var ivReqNumber: ImageView
    private lateinit var ivReqSpecial: ImageView
    private lateinit var btnGoogleRegister: LinearLayout
    private lateinit var btnStep2Back: MaterialButton
    private lateinit var btnStep2Next: MaterialButton

    // Step 3 — OTP de correo
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
        etDniVerifier = findViewById(R.id.etDniVerifier)
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

        // Step 2
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        ivReqLength = findViewById(R.id.ivReqLength)
        ivReqNumber = findViewById(R.id.ivReqNumber)
        ivReqSpecial = findViewById(R.id.ivReqSpecial)
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister)
        btnStep2Back = findViewById(R.id.btnStep2Back)
        btnStep2Next = findViewById(R.id.btnStep2Next)

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
        btnStep0Next.isEnabled = true
        btnStep0Next.alpha = 1f
    }

    // ==================== PASO 1: IDENTIDAD ====================
    private fun setupStep1() {
        btnTabDni.setOnClickListener {
            selectIdentityTab("DNI")
        }
        btnTabRuc.setOnClickListener {
            selectIdentityTab("RUC")
        }

        btnConsultReniec.setOnClickListener {
            consultReniec()
        }

        btnConsultSunat.setOnClickListener {
            consultSunat()
        }

        // Si el usuario edita el DNI o dígito verificador, la verificación caduca
        setupResetVerifiedOnEdit(etDni) { resetDniVerifiedUi() }
        setupResetVerifiedOnEdit(etDniVerifier) { resetDniVerifiedUi() }
        setupResetVerifiedOnEdit(etRuc) { resetRucVerifiedUi() }

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
                    showToast("Primero confirma tu identidad con RENIEC")
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
            layoutIdentityTypeTabs.visibility = View.GONE
            layoutDniForm.visibility = View.VISIBLE
            layoutRucForm.visibility = View.GONE
            identityMode = "DNI"
            tvStep1Subtitle.text = getString(R.string.register_step2_subtitle)
        } else {
            layoutIdentityTypeTabs.visibility = View.VISIBLE
            selectIdentityTab(identityMode)
        }
    }

    private fun selectIdentityTab(mode: String) {
        identityMode = mode
        val brandColor = ContextCompat.getColor(this, R.color.brand_color)

        if (mode == "DNI") {
            styleTab(btnTabDni, selected = true, brandColor)
            styleTab(btnTabRuc, selected = false, brandColor)
            layoutDniForm.visibility = View.VISIBLE
            layoutRucForm.visibility = View.GONE
            cardRucVerified.visibility = View.GONE
            tvStep1Subtitle.text = getString(R.string.register_step2_subtitle)
        } else {
            styleTab(btnTabRuc, selected = true, brandColor)
            styleTab(btnTabDni, selected = false, brandColor)
            layoutRucForm.visibility = View.VISIBLE
            layoutDniForm.visibility = View.GONE
            tvStep1Subtitle.text = "Valida tu empresa registrada en SUNAT (RUC Activo/Habido)"
        }
    }

    private fun styleTab(btn: MaterialButton, selected: Boolean, brandColor: Int) {
        if (selected) {
            btn.backgroundTintList = ColorStateList.valueOf(brandColor)
            btn.setTextColor(Color.WHITE)
            btn.typeface = android.graphics.Typeface.DEFAULT_BOLD
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            btn.setTextColor(Color.parseColor("#64748B"))
            btn.typeface = android.graphics.Typeface.DEFAULT
        }
    }

    private fun consultReniec() {
        val dni = etDni.text.toString().trim()
        val verifier = etDniVerifier.text.toString().trim()

        when {
            dni.length != 8 -> showToast("El DNI debe tener 8 dígitos")
            verifier.length != 1 -> showToast("Ingresa el dígito de verificación")
            else -> {
                pbReniec.visibility = View.VISIBLE
                btnConsultReniec.isEnabled = false

                Handler(Looper.getMainLooper()).postDelayed({
                    pbReniec.visibility = View.GONE
                    btnConsultReniec.isEnabled = true
                    isDniVerified = true

                    // Simulación RENIEC: nombre oficial según el DNI
                    tvReniecFullName.text = sampleOfficialName(dni)
                    tvReniecDniDetail.text = "DNI: $dni-$verifier · Ayacucho, Huamanga"
                    cardDniVerified.visibility = View.VISIBLE
                    tvDniAttempts.text = "Verificación confirmada (estado: ENCENDIDO)"

                    Snackbar.make(
                        findViewById(R.id.registerRoot),
                        "✓ Identidad confirmada en RENIEC (1 Usuario = 1 DNI)",
                        Snackbar.LENGTH_LONG
                    ).show()
                }, 800)
            }
        }
    }

    private fun sampleOfficialName(dni: String): String {
        val names = arrayOf(
            "JUAN CARLOS PÉREZ QUISPE",
            "MARÍA ELENA QUISPE HUAMÁN",
            "JOSÉ LUIS GUTIÉRREZ FLORES",
            "ROSA MARÍA CONDORI AYALA",
            "PEDRO ANTONIO SULCA VARGAS"
        )
        return names[dni.toInt() % names.size]
    }

    private fun resetDniVerifiedUi() {
        isDniVerified = false
        cardDniVerified.visibility = View.GONE
        tvDniAttempts.text = "Intentos disponibles: 2 de 2"
    }

    private fun consultSunat() {
        val ruc = etRuc.text.toString().trim()
        if (ruc.length != 11) {
            showToast("El RUC debe tener 11 dígitos")
            return
        }

        pbSunat.visibility = View.VISIBLE
        btnConsultSunat.isEnabled = false

        Handler(Looper.getMainLooper()).postDelayed({
            pbSunat.visibility = View.GONE
            btnConsultSunat.isEnabled = true
            isRucVerified = true

            tvSunatRazonSocial.text = "CONSTRUCTORA & MULTISERVICIOS AYACUCHO S.A.C."
            tvSunatCondition.text = "Condición: HABIDO | Estado: ACTIVO"
            cardRucVerified.visibility = View.VISIBLE

            Snackbar.make(
                findViewById(R.id.registerRoot),
                "✓ RUC validado en SUNAT como ACTIVO y HABIDO",
                Snackbar.LENGTH_SHORT
            ).show()
        }, 700)
    }

    private fun resetRucVerifiedUi() {
        isRucVerified = false
        cardRucVerified.visibility = View.GONE
    }

    private fun setupResetVerifiedOnEdit(editText: EditText, onEdited: () -> Unit) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onEdited()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ==================== PASO 2: CREDENCIALES ====================
    private fun setupStep2() {
        setupPasswordToggle(etPassword, ivTogglePassword)
        setupPasswordToggle(etConfirmPassword, ivToggleConfirmPassword)

        // Requisitos de contraseña en vivo
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

        // Google OAuth mock: autocompleta el correo
        btnGoogleRegister.setOnClickListener {
            etEmail.setText("juan.perez@gmail.com")
            Snackbar.make(
                findViewById(R.id.registerRoot),
                "Datos de Google sincronizados",
                Snackbar.LENGTH_SHORT
            ).show()
        }

        btnStep2Back.setOnClickListener {
            updateStep(1)
        }

        btnStep2Next.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val pwd = etPassword.text.toString()
            val confirmPwd = etConfirmPassword.text.toString()

            when {
                email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    showToast("Ingresa un correo electrónico válido")
                phone.length < 9 -> showToast("Ingresa tu número de celular (9 dígitos)")
                pwd.length < 8 -> showToast("La contraseña debe tener al menos 8 caracteres")
                pwd != confirmPwd -> showToast("Las contraseñas no coinciden")
                else -> {
                    isOtpVerified = false
                    pendingOtp = ""
                    updateStep(3)
                }
            }
        }
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

    // ==================== PASO 3: OTP DE CORREO ====================
    private fun setupStep3() {
        setupOtpBox(otpBox1, otpBox2, null)
        setupOtpBox(otpBox2, otpBox3, otpBox1)
        setupOtpBox(otpBox3, otpBox4, otpBox2)
        setupOtpBox(otpBox4, null, otpBox3)

        btnConfirmOtp.setOnClickListener {
            confirmOtp()
        }

        btnResendOtp.setOnClickListener {
            generateOtpCode(resend = true)
        }

        btnChangeEmail.setOnClickListener {
            pendingOtp = ""
            isOtpVerified = false
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
        tvOtpEmailHint.text = "Hemos enviado un código de 4 dígitos a\n${etEmail.text.toString().trim()}"

        clearOtpBoxes()
        btnConfirmOtp.isEnabled = true
        btnConfirmOtp.alpha = 1f
        pbOtp.visibility = View.GONE

        if (pendingOtp.isEmpty()) generateOtpCode(resend = false)
    }

    private fun generateOtpCode(resend: Boolean) {
        pendingOtp = (1000..9999).random().toString()
        clearOtpBoxes()
        pbOtp.visibility = View.GONE
        btnConfirmOtp.isEnabled = true
        btnConfirmOtp.alpha = 1f

        if (resend) {
            Snackbar.make(
                findViewById(R.id.registerRoot),
                "Nuevo código enviado a ${etEmail.text.toString().trim()} (demo: $pendingOtp)",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun clearOtpBoxes() {
        otpBox1.setText("")
        otpBox2.setText("")
        otpBox3.setText("")
        otpBox4.setText("")
        otpBox1.requestFocus()
    }

    private fun confirmOtp() {
        val code = otpBox1.text.toString() + otpBox2.text.toString() +
            otpBox3.text.toString() + otpBox4.text.toString()

        if (code.length != 4) {
            showToast("Ingresa el código de 4 dígitos")
            return
        }

        pbOtp.visibility = View.VISIBLE
        btnConfirmOtp.isEnabled = false
        btnConfirmOtp.alpha = 0.6f

        Handler(Looper.getMainLooper()).postDelayed({
            pbOtp.visibility = View.GONE
            btnConfirmOtp.isEnabled = true
            btnConfirmOtp.alpha = 1f

            if (code == pendingOtp) {
                isOtpVerified = true
                Snackbar.make(
                    findViewById(R.id.registerRoot),
                    "✓ Correo verificado correctamente",
                    Snackbar.LENGTH_SHORT
                ).show()
                updateStep(4)
            } else {
                clearOtpBoxes()
                showToast("Código incorrecto. Revisa tu bandeja de entrada.")
            }
        }, 800)
    }

    // ==================== PASO 4: RESUMEN ====================
    private fun setupStep4() {
        btnStep5Back.setOnClickListener {
            updateStep(3)
        }

        btnFinalizeRegister.setOnClickListener {
            if (!isOtpVerified) {
                showToast("Debes verificar tu correo antes de crear la cuenta")
                return@setOnClickListener
            }
            showSuccessRegistrationDialog()
        }
    }

    private fun populateSummary() {
        val email = etEmail.text.toString().trim()
        tvSummaryEmail.text = if (email.isNotEmpty()) email else "juan.perez@gmail.com"

        val phone = etPhone.text.toString().trim()
        tvSummaryPhone.text = if (phone.isNotEmpty()) "+51 $phone" else "+51 987 654 321"

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
                else "Juan Carlos Pérez Quispe"
            tvSummaryVerifiedBadge.text = "✓ Perfil Verificado Oficial (RENIEC)"
        }

        tvSummaryRole.text = if (isWorker) "Trabajador" else "Empleador / Contratante"
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

    override fun onBackPressed() {
        handleBackAction()
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