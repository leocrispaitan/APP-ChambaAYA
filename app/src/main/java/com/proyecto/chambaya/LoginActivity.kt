package com.proyecto.chambaya

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

/**
 * LoginActivity - Inicio de sesión con Firebase Authentication
 * 
 * Métodos de autenticación soportados:
 * 1. Correo electrónico y contraseña (Firebase Auth)
 * 2. Google Sign-In (Firebase Auth + Google)
 * 
 * Flujo de autenticación:
 * - Validar credenciales con Firebase Authentication
 * - Verificar que el usuario existe en Firestore (colección "users")
 * - Redirigir a MainActivity si el login es exitoso
 */
class LoginActivity : AppCompatActivity() {

    // Firebase Authentication & Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    // Views
    private lateinit var btnBack: ImageButton
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnSignIn: MaterialButton
    private lateinit var btnGoogleSignIn: LinearLayout
    private lateinit var tvSignupLink: TextView
    private lateinit var pbLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(R.color.brand_color)
        window.navigationBarColor = getColor(R.color.surface_light)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = true

        setContentView(R.layout.actividad_login)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configurar Google Sign-In (mismo Web Client ID que en RegistroActivity)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Launcher para el resultado de Google Sign-In
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleSignInResult(result.data)
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        tvSignupLink = findViewById(R.id.tvSignupLink)

        // ProgressBar para estados de carga (crear si no existe en el layout)
        pbLoading = ProgressBar(this).apply {
            visibility = View.GONE
            isIndeterminate = true
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        // Toggle visibilidad de contraseña
        setupPasswordToggle(etPassword, ivTogglePassword)

        // Botón de inicio de sesión con correo y contraseña
        btnSignIn.setOnClickListener {
            handleEmailPasswordLogin()
        }

        // Botón de inicio de sesión con Google
        btnGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
        }

        // Link para ir al registro
        tvSignupLink.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        // Recuperar contraseña
        tvForgotPassword.setOnClickListener {
            handleForgotPassword()
        }
    }

    // ==================== INICIO DE SESIÓN CON EMAIL Y CONTRASEÑA ====================
    private fun handleEmailPasswordLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validaciones frontend
        when {
            email.isEmpty() -> {
                showToast("Ingresa tu correo electrónico")
                etEmail.requestFocus()
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showToast("El formato del correo electrónico no es válido")
                etEmail.requestFocus()
                return
            }
            password.isEmpty() -> {
                showToast("Ingresa tu contraseña")
                etPassword.requestFocus()
                return
            }
            password.length < 6 -> {
                showToast("La contraseña debe tener al menos 6 caracteres")
                etPassword.requestFocus()
                return
            }
        }

        setLoading(true)

        // Autenticar con Firebase Authentication
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid

                    if (uid != null) {
                        // Verificar que el usuario existe en Firestore
                        verifyUserInFirestore(uid, "EMAIL_PASSWORD")
                    } else {
                        setLoading(false)
                        showToast("Error al obtener la información del usuario")
                    }
                } else {
                    setLoading(false)
                    val exception = task.exception
                    val errorMsg = when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            "No existe una cuenta con este correo electrónico. Regístrate primero."
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            "Contraseña incorrecta. Verifica tus credenciales e inténtalo de nuevo."
                        }
                        is FirebaseNetworkException -> {
                            "Sin conexión a internet. Verifica tu red e inténtalo de nuevo."
                        }
                        else -> {
                            exception?.localizedMessage ?: "Error al iniciar sesión. Inténtalo de nuevo."
                        }
                    }
                    showToast(errorMsg)
                }
            }
    }

    // ==================== INICIO DE SESIÓN CON GOOGLE ====================
    private fun launchGoogleSignIn() {
        setLoading(true)
        // Cerrar sesión previa para permitir seleccionar cuenta
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
                setLoading(false)
                showToast("No se pudo obtener la credencial de Google. Inténtalo de nuevo.")
                return
            }

            // Autenticar en Firebase con la credencial de Google
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { authTask ->
                    if (authTask.isSuccessful) {
                        val user = auth.currentUser
                        val uid = user?.uid

                        if (uid != null) {
                            // Verificar que el usuario existe en Firestore
                            verifyUserInFirestore(uid, "GOOGLE")
                        } else {
                            setLoading(false)
                            showToast("Error al obtener la información del usuario")
                        }
                    } else {
                        setLoading(false)
                        val exception = authTask.exception
                        val errorMsg = when (exception) {
                            is FirebaseNetworkException -> {
                                "Error de conexión a internet con Firebase. Intenta nuevamente."
                            }
                            else -> {
                                exception?.localizedMessage ?: "Error al autenticar con Firebase usando Google."
                            }
                        }
                        showToast(errorMsg)
                    }
                }
        } catch (e: ApiException) {
            setLoading(false)
            when (e.statusCode) {
                CommonStatusCodes.CANCELED, 12501 -> {
                    // El usuario canceló la selección de cuenta - no mostrar error
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
            setLoading(false)
            showToast("Error inesperado en Google Sign-In: ${e.localizedMessage}")
        }
    }

    // ==================== VERIFICACIÓN EN FIRESTORE ====================
    /**
     * Verifica que el usuario autenticado existe en Firestore
     * y tiene los datos completos de registro
     */
    private fun verifyUserInFirestore(uid: String, authMethod: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                setLoading(false)

                if (document.exists()) {
                    // Usuario existe en Firestore - Login exitoso
                    val email = document.getString("email") ?: ""
                    val role = document.getString("role") ?: "TRABAJADOR"
                    val registrationStatus = document.getString("registrationStatus") ?: ""

                    // Verificar que el registro está completo
                    if (registrationStatus == "VERIFIED" || registrationStatus == "COMPLETED") {
                        showToast("¡Bienvenido de nuevo!")
                        navigateToMainActivity()
                    } else {
                        showToast("Tu registro no está completo. Completa el proceso de registro primero.")
                        auth.signOut()
                    }
                } else {
                    // Usuario autenticado en Firebase Auth pero NO existe en Firestore
                    showToast("No se encontró tu perfil en la base de datos. Completa el registro primero.")
                    auth.signOut()
                    
                    // Opcional: redirigir al registro para completar el perfil
                    // startActivity(Intent(this, RegistroActivity::class.java))
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showToast("Error al verificar tu cuenta: ${e.localizedMessage}")
                auth.signOut()
            }
    }

    // ==================== RECUPERAR CONTRASEÑA ====================
    private fun handleForgotPassword() {
        val email = etEmail.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Ingresa tu correo electrónico para recuperar tu contraseña")
            etEmail.requestFocus()
            return
        }

        setLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    showToast("✅ Correo de recuperación enviado a $email. Revisa tu bandeja de entrada.")
                } else {
                    val exception = task.exception
                    val errorMsg = when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            "No existe una cuenta con este correo electrónico."
                        }
                        is FirebaseNetworkException -> {
                            "Sin conexión a internet. Verifica tu red."
                        }
                        else -> {
                            "Error al enviar el correo de recuperación: ${exception?.localizedMessage}"
                        }
                    }
                    showToast(errorMsg)
                }
            }
    }

    // ==================== NAVEGACIÓN ====================
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    // ==================== UI HELPERS ====================
    private fun setLoading(loading: Boolean) {
        btnSignIn.isEnabled = !loading
        btnSignIn.alpha = if (loading) 0.6f else 1f
        btnGoogleSignIn.isEnabled = !loading
        btnGoogleSignIn.alpha = if (loading) 0.6f else 1f
        etEmail.isEnabled = !loading
        etPassword.isEnabled = !loading
        tvForgotPassword.isEnabled = !loading
        tvSignupLink.isEnabled = !loading

        // Mostrar indicador de carga en el botón
        if (loading) {
            btnSignIn.text = "Iniciando sesión..."
        } else {
            btnSignIn.text = getString(R.string.auth_signin)
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
        toggle.contentDescription = getString(
            if (isPasswordVisible) R.string.auth_hide_password else R.string.auth_show_password
        )
    }

    private fun showToast(message: String) {
        Snackbar.make(findViewById(R.id.loginRoot), message, Snackbar.LENGTH_LONG).show()
    }
}