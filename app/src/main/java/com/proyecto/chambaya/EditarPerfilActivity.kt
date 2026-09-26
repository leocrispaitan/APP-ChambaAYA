package com.proyecto.chambaya

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import java.util.Calendar

class EditarPerfilActivity : AppCompatActivity() {

    // Views
    private lateinit var btnCerrar: FrameLayout
    private lateinit var ivAvatar: ShapeableImageView
    private lateinit var btnCambiarFoto: FrameLayout
    private lateinit var etNombre: EditText
    private lateinit var inputFechaNacimiento: ConstraintLayout
    private lateinit var tvFecha: TextView
    private lateinit var etEmail: EditText
    private lateinit var iconEmailVerificado: ImageView
    private lateinit var inputUbicacion: ConstraintLayout
    private lateinit var tvUbicacion: TextView
    private lateinit var etTelefono: EditText
    private lateinit var btnActualizar: MaterialButton

    // Data
    private var selectedImageUri: Uri? = null
    private var selectedDate: String = "19/10/2000"
    private var selectedLocation: String = "Estados Unidos"
    private val locations = arrayOf(
        "Estados Unidos",
        "Perú",
        "México",
        "Argentina",
        "Colombia",
        "Chile",
        "España",
        "Otro"
    )

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                loadImageIntoAvatar(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar barra de estado clara con iconos oscuros
        BarraEstadoUtils.aplicarColor(
            this,
            android.graphics.Color.parseColor("#FFFFFF")
        )
        
        setContentView(R.layout.dialog_editar_perfil)

        initViews()
        setupListeners()
        setupBackPress()
        loadUserData()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                applyExitTransition()
            }
        })
    }

    /**
     * Aplica la transición de salida compatible con todas las versiones de Android
     */
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
        btnCerrar = findViewById(R.id.btnCerrarEditarPerfil)
        ivAvatar = findViewById(R.id.ivEditarPerfilAvatar)
        btnCambiarFoto = findViewById(R.id.btnCambiarFotoPerfil)
        etNombre = findViewById(R.id.etEditarPerfilNombre)
        inputFechaNacimiento = findViewById(R.id.inputFechaNacimiento)
        tvFecha = findViewById(R.id.tvEditarPerfilFecha)
        etEmail = findViewById(R.id.etEditarPerfilEmail)
        iconEmailVerificado = findViewById(R.id.iconEmailVerificado)
        inputUbicacion = findViewById(R.id.inputUbicacion)
        tvUbicacion = findViewById(R.id.tvEditarPerfilUbicacion)
        etTelefono = findViewById(R.id.etEditarPerfilTelefono)
        btnActualizar = findViewById(R.id.btnActualizarPerfil)
    }

    private fun setupListeners() {
        // Botón cerrar
        btnCerrar.setOnClickListener {
            finish()
            applyExitTransition()
        }

        // Cambiar foto de perfil
        btnCambiarFoto.setOnClickListener {
            openImagePicker()
        }

        // Selector de fecha de nacimiento
        inputFechaNacimiento.setOnClickListener {
            showDatePicker()
        }

        // Selector de ubicación
        inputUbicacion.setOnClickListener {
            showLocationPicker()
        }

        // Validación de email en tiempo real
        etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateEmail()
            }
        }

        // Botón actualizar perfil
        btnActualizar.setOnClickListener {
            updateProfile()
        }
    }

    private fun loadUserData() {
        // Aquí cargarías los datos del usuario desde Firebase o tu backend
        // Por ahora, cargamos datos de ejemplo
        
        etNombre.setText("Maria Sanchez")
        tvFecha.text = selectedDate
        etEmail.setText("maria.sanchez@ejemplo.com")
        tvUbicacion.text = selectedLocation
        etTelefono.setText("987 654 321")
        
        // Mostrar ícono de verificación si el email está verificado
        iconEmailVerificado.visibility = View.VISIBLE
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun loadImageIntoAvatar(uri: Uri) {
        // Cargar imagen directamente sin Glide
        try {
            ivAvatar.setImageURI(uri)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // Parsear la fecha actual si existe
        val parts = selectedDate.split("/")
        if (parts.size == 3) {
            calendar.set(Calendar.DAY_OF_MONTH, parts[0].toInt())
            calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
            calendar.set(Calendar.YEAR, parts[2].toInt())
        }

        val datePickerDialog = DatePickerDialog(
            this,
            R.style.CustomDatePickerTheme,
            { _, year, month, dayOfMonth ->
                selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                tvFecha.text = selectedDate
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Establecer fecha máxima (hace 18 años para asegurar mayoría de edad)
        val maxCalendar = Calendar.getInstance()
        maxCalendar.add(Calendar.YEAR, -18)
        datePickerDialog.datePicker.maxDate = maxCalendar.timeInMillis

        // Establecer fecha mínima (hace 100 años)
        val minCalendar = Calendar.getInstance()
        minCalendar.add(Calendar.YEAR, -100)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        datePickerDialog.show()
    }

    private fun showLocationPicker() {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        builder.setTitle("Seleccionar Ubicación")
        
        builder.setItems(locations) { dialog, which ->
            selectedLocation = locations[which]
            tvUbicacion.text = selectedLocation
            dialog.dismiss()
        }
        
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        
        builder.show()
    }

    private fun validateEmail(): Boolean {
        val email = etEmail.text.toString().trim()
        
        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo electrónico"
            iconEmailVerificado.visibility = View.GONE
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Correo electrónico inválido"
            iconEmailVerificado.visibility = View.GONE
            return false
        }
        
        iconEmailVerificado.visibility = View.VISIBLE
        return true
    }

    private fun validateForm(): Boolean {
        val nombre = etNombre.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombre.error = "Ingresa tu nombre completo"
            etNombre.requestFocus()
            return false
        }

        if (nombre.length < 3) {
            etNombre.error = "El nombre debe tener al menos 3 caracteres"
            etNombre.requestFocus()
            return false
        }

        if (!validateEmail()) {
            etEmail.requestFocus()
            return false
        }

        if (telefono.isEmpty()) {
            etTelefono.error = "Ingresa tu número de teléfono"
            etTelefono.requestFocus()
            return false
        }

        if (telefono.length < 9) {
            etTelefono.error = "El teléfono debe tener al menos 9 dígitos"
            etTelefono.requestFocus()
            return false
        }

        return true
    }

    private fun updateProfile() {
        if (!validateForm()) {
            return
        }

        // Aquí implementarías la lógica para actualizar el perfil en Firebase o tu backend
        
        // Mostrar loading
        btnActualizar.isEnabled = false
        btnActualizar.text = "Actualizando..."

        // Simular actualización (reemplazar con tu lógica real)
        btnActualizar.postDelayed({
            // Éxito
            Toast.makeText(
                this,
                "✓ Perfil actualizado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            // Devolver resultado exitoso
            setResult(Activity.RESULT_OK)
            finish()
            applyExitTransition()
        }, 1500)
    }
}
