package com.proyecto.chambaya

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.data.model.BirthDates
import com.proyecto.chambaya.data.model.Genders
import com.proyecto.chambaya.data.model.IdentitySources
import com.proyecto.chambaya.data.model.OficioCatalog
import com.proyecto.chambaya.data.model.PeruLocations
import com.proyecto.chambaya.data.model.ProfileCompletion
import com.proyecto.chambaya.data.model.ProfileDraft
import com.proyecto.chambaya.data.model.ProfileLimits
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.esUsernameValido
import com.proyecto.chambaya.data.model.normalizarUsername
import com.proyecto.chambaya.data.remote.CloudinaryUploader
import com.proyecto.chambaya.data.remote.PhotoUploadResult
import com.proyecto.chambaya.data.repository.PadronRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.UsernameYaTomado
import com.proyecto.chambaya.data.repository.motivoFirestore
import com.proyecto.chambaya.ui.profile.EspecialidadSelectorAdapter
import com.proyecto.chambaya.ui.profile.OficioIcons
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.LinkedHashSet

/**
 * FASE 2 — Asistente de edición del perfil, en cuatro pasos.
 *
 * Antes de esta fase esta pantalla era una maqueta: el nombre, el DNI, el
 * `@usuario`, el correo y el teléfono estaban escritos en el código, las
 * especialidades eran seis `CheckBox` fijos y el botón "Finalizar" solo
 * enseñaba un `Toast`. Ahora todo eso sale de `users/{uid}` y termina en
 * Firestore.
 *
 * PASOS
 *  1. Información básica  — foto, DNI (solo lectura), nombre, @usuario, correo
 *                          (solo lectura) y teléfono.
 *  2. Información personal — fecha de nacimiento, género y ubicación en cascada
 *                          (departamento → provincia → distrito).
 *  3. Experiencia         — años, especialidades del catálogo de oficios y
 *                          habilidades.
 *  4. Privacidad          — qué contacto se muestra y resumen de completitud.
 *
 * Qué NO se puede tocar desde aquí (y por eso son de solo lectura): `uid`,
 * `identity` (DNI/RUC verificado con RENIEC), `auth` (correo verificado) y el
 * estado de registro. Todo eso es de la FASE 1 y lo escribe
 * `RegistrationRepository`; el `username` además se reserva en `usernames/`
 * para que sea único.
 */
class EditarPerfilActivity : AppCompatActivity() {

    // ═══════════════════════════════════════════════════════════════
    //  DATOS
    // ═══════════════════════════════════════════════════════════════

    private val repository = ProfileRepository()
    private val padronRepository = PadronRepository()
    private val uploader = CloudinaryUploader()

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    /** `uid` de la sesión; `null` si el usuario ya no está autenticado. */
    private var uid: String? = null

    /** Lo que hay ahora mismo en Firestore. Base de la vista previa y del guardado. */
    private var perfilCargado: UserProfile? = null

    // ==================== CONTROL DE PASOS ====================
    private var currentStep = 1

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
    private lateinit var tvDniNota: TextView
    private lateinit var iconDniVerificado: ImageView
    private lateinit var etNombre: EditText
    private lateinit var etUsername: EditText
    private lateinit var tvUsernameEstado: TextView
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
    private lateinit var headerEspecialidadesSelector: ConstraintLayout
    private lateinit var tvEspecialidadesResumen: TextView
    private lateinit var ivChevronEspecialidades: ImageView
    private lateinit var panelEspecialidadesSelector: LinearLayout
    private lateinit var etBuscarOficio: EditText
    private lateinit var rvEspecialidades: RecyclerView
    private lateinit var tvSinResultadosOficio: TextView
    private lateinit var etHabilidades: EditText

    private var modoContratante = false

    // ==================== PASO 4: PRIVACIDAD ====================
    private lateinit var switchMostrarTelefono: SwitchCompat
    private lateinit var switchMostrarEmail: SwitchCompat
    private lateinit var switchMostrarUbicacion: SwitchCompat
    private lateinit var tvResumenTitulo: TextView
    private lateinit var tvResumenCompletitud: TextView
    private lateinit var progressBarPerfil: ProgressBar
    private lateinit var tvPorcentajePerfil: TextView

    // ==================== NAVEGACIÓN ====================
    private lateinit var btnAtras: MaterialButton
    private lateinit var btnSiguiente: MaterialButton

    // ==================== ESTADO DEL FORMULARIO ====================

    /** Foto elegida en esta sesión y todavía NO subida a Cloudinary. */
    private var selectedImageUri: Uri? = null

    /** `true` si hay una foto nueva pendiente de subir. */
    private var fotoPendiente = false

    private var selectedDate: String = ""
    private var selectedGenero: String = ""

    private var selectedDepartamento: String = ""
    private var selectedProvincia: String = ""
    private var selectedDistrito: String = ""

    /** Oficios marcados en el paso 3, en el orden en que se eligieron. */
    private val especialidadesElegidas = LinkedHashSet<String>()

    /** Comprobación de disponibilidad del `@usuario`, con retardo para no spamear. */
    private var jobUsername: Job? = null

    /** Evita que un watcher dispare actualizaciones durante la carga inicial. */
    private var llenandoFormulario = false

    /**
     * `true` si el usuario ya tocó el campo del `@usuario`.
     *
     * Importa porque el formulario se llena antes de que la siembra de los bloques
     * termine: cuando esa siembra reserva un `@usuario` libre, solo se sustituye el
     * del campo si el usuario no ha escrito nada, para no pisarle lo tecleado.
     */
    private var usernameTocado = false

    /**
     * `true` si se entró por "Completar perfil" en vez de por "Editar perfil".
     *
     * Solo en el primer caso se salta al primer paso con datos pendientes; desde
     * "Editar perfil" siempre se recorre el asistente entero desde el principio.
     */
    private var entradaCompletar = false

    private lateinit var adapterOficios: EspecialidadSelectorAdapter

    /** `true` mientras el panel de especialidades (buscador + lista) está desplegado. */
    private var panelEspecialidadesExpandido = false

    // Launcher para selección de imagen
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                fotoPendiente = true
                // Vista previa inmediata: la subida real ocurre al guardar, para
                // no dejar imágenes huérfanas en Cloudinary si el usuario cancela.
                ivAvatar.load(uri) { crossfade(true) }
                showToast(getString(R.string.edit_perfil_foto_elegida))
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
        setupTeclado()
        setupBackPress()
        setupSelectorOficios()

        // "Completar perfil" aterriza en el primer paso con datos pendientes (eso se
        // decide en [primerPasoIncompleto], en cuanto se ha leído el perfil);
        // "Editar perfil" recorre los cuatro desde el principio.
        val inicio = intent.getIntExtra(EXTRA_START_STEP, PASO_INICIO_EDITAR)
            .coerceIn(1, PASOS_TOTAL)
        entradaCompletar = inicio == PASO_INICIO_COMPLETAR
        updateStep(inicio)

        cargarPerfil()
    }

    /**
     * Evita que el teclado tape el campo que se esta escribiendo.
     *
     * Con `targetSdk 36` el modo edge-to-edge es obligatorio desde Android 15 y
     * `adjustResize` deja de redimensionar la ventana, por lo que el teclado se
     * superpone al formulario. Aqui se hace a mano lo que hacia `adjustResize`:
     * se reserva el alto del teclado como padding de la RAIZ, de modo que la
     * ventana util se encoge de verdad y el `NestedScrollView` (que mide entre el
     * header y el footer) recibe un alto menor en lugar de recortarse.
     *
     * El padding va en la raiz y no en el scroll a proposito: en el scroll solo
     * recortaria el final del formulario y dejaria una franja vacia.
     *
     * En versiones antiguas, donde la ventana si se redimensiona sola, el inset
     * del teclado llega en 0 porque ya no queda nada que reservar, asi que el
     * padding no se duplica.
     */
    private fun setupTeclado() {
        val root = findViewById<View>(R.id.rootEditarPerfil)
        val scroll = findViewById<NestedScrollView>(R.id.scrollEditarPerfil)
        var paddingTeclado = 0

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val nuevo = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            if (nuevo != paddingTeclado) {
                paddingTeclado = nuevo
                v.updatePadding(bottom = nuevo)
                // Tras el dibujado, no antes: asi el alto del scroll ya es el nuevo
                // y el desplazamiento si tiene efecto.
                v.doOnPreDraw { desplazarAlCampoEnFoco(scroll) }
            }
            insets
        }

        // Al pasar de un campo a otro con el teclado ya abierto los insets no se
        // vuelven a emitir, asi que tambien hay que escuchar el cambio de foco.
        scroll.camposEditables().forEach { campo ->
            campo.setOnFocusChangeListener { _, tieneFoco ->
                if (!tieneFoco) return@setOnFocusChangeListener
                // Al cambiar de campo el alto del scroll NO cambia, asi que el
                // recolocado se puede hacer ya mismo. Se repite tras el siguiente
                // layout porque el framework aplica su propio desplazamiento al
                // cambiar el foco y a veces deja el campo otra vez bajo el teclado.
                desplazarAlCampoEnFoco(scroll)
                scroll.doOnPreDraw { desplazarAlCampoEnFoco(scroll) }
            }
        }
    }

    /**
     * Desplaza el scroll con calculo propio para dejar el campo enfocado dentro de
     * la zona visible.
     *
     * No se usa `requestChildRectangleOnScreen` porque con un `Rect` vacio el
     * resultado no es fiable y el formulario se quedaba sin subir.
     */
    private fun desplazarAlCampoEnFoco(scroll: NestedScrollView) {
        val campo = scroll.findFocus() as? EditText ?: return
        if (campo.height == 0) return

        val altoVisible = scroll.height - scroll.paddingTop - scroll.paddingBottom
        if (altoVisible <= 0) return

        val posCampo = IntArray(2)
        val posScroll = IntArray(2)
        campo.getLocationInWindow(posCampo)
        scroll.getLocationInWindow(posScroll)

        // `getLocationInWindow` ya descuenta el scroll actual, asi que `arriba` es
        // la distancia del campo al borde superior de la zona visible.
        val margen = (24 * resources.displayMetrics.density).toInt()
        val arriba = posCampo[1] - posScroll[1]
        val abajo = arriba + campo.height

        val destino = when {
            arriba < margen -> scroll.scrollY + arriba - margen
            abajo > altoVisible - margen -> scroll.scrollY + abajo - altoVisible + margen
            else -> return
        }

        val contenido = scroll.getChildAt(0) ?: return
        val maximo = (contenido.height - altoVisible).coerceAtLeast(0)
        scroll.scrollTo(scroll.scrollX, destino.coerceIn(0, maximo))
    }

    /**
     * Cierra el teclado y quita el foco del campo activo.
     *
     * Se llama cuando el usuario ya no esta escribiendo: al cambiar de fase con
     * Atrás/Siguiente y al marcar una especialidad.
     */
    private fun ocultarTeclado() {
        val campo = currentFocus
        if (campo is EditText) campo.clearFocus()
        val teclado = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        teclado?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    /** Recorre el arbol del formulario y devuelve todos los EditText. */
    private fun View.camposEditables(): List<EditText> {
        if (this is EditText) return listOf(this)
        if (this !is ViewGroup) return emptyList()
        return (0 until childCount).flatMap { getChildAt(it).camposEditables() }
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
        tvDniNota = findViewById(R.id.tvDniNota)
        iconDniVerificado = findViewById(R.id.iconDniVerificado)
        etNombre = findViewById(R.id.etEditarPerfilNombre)
        etUsername = findViewById(R.id.etEditarPerfilUsername)
        tvUsernameEstado = findViewById(R.id.tvUsernameEstado)
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
        headerEspecialidadesSelector = findViewById(R.id.headerEspecialidadesSelector)
        tvEspecialidadesResumen = findViewById(R.id.tvEspecialidadesResumen)
        ivChevronEspecialidades = findViewById(R.id.ivChevronEspecialidades)
        panelEspecialidadesSelector = findViewById(R.id.panelEspecialidadesSelector)
        etBuscarOficio = findViewById(R.id.etBuscarOficio)
        rvEspecialidades = findViewById(R.id.rvEspecialidadesSelector)
        tvSinResultadosOficio = findViewById(R.id.tvSinResultadosOficio)
        etHabilidades = findViewById(R.id.etEditarPerfilHabilidades)

        // Paso 4
        switchMostrarTelefono = findViewById(R.id.switchMostrarTelefono)
        switchMostrarEmail = findViewById(R.id.switchMostrarEmail)
        switchMostrarUbicacion = findViewById(R.id.switchMostrarUbicacion)
        tvResumenTitulo = findViewById(R.id.tvResumenTitulo)
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

        // Tocar el avatar también abre la galería, no solo el botón de cámara.
        ivAvatar.setOnClickListener { openImagePicker() }

        // Cambiar foto
        btnCambiarFoto.setOnClickListener {
            openImagePicker()
        }

        // Fecha de nacimiento
        inputFechaNacimiento.setOnClickListener {
            ocultarTeclado()
            showDatePicker()
        }

        // Ubicación: cascada departamento -> provincia -> distrito
        inputUbicacion.setOnClickListener {
            ocultarTeclado()
            showLocationPicker()
        }

        // Navegación
        btnAtras.setOnClickListener {
            if (currentStep > 1) {
                ocultarTeclado()
                val anterior = if (modoContratante && currentStep == 4) 2 else currentStep - 1
                updateStep(anterior)
            }
        }

        btnSiguiente.setOnClickListener {
            ocultarTeclado()
            handleNextStep()
        }

        // Radio buttons de género
        rgGenero.setOnCheckedChangeListener { _, checkedId ->
            selectedGenero = when (checkedId) {
                R.id.rbMasculino -> Genders.MASCULINO
                R.id.rbFemenino -> Genders.FEMENINO
                R.id.rbOtro -> Genders.OTRO
                else -> ""
            }
            if (!llenandoFormulario) refrescarResumen()
        }

        // El @usuario se comprueba contra `usernames/` mientras se escribe, con
        // retardo para no lanzar una lectura por cada tecla.
        etUsername.addTextChangedListener(alCambiarTexto {
            if (llenandoFormulario) return@alCambiarTexto
            usernameTocado = true
            programarComprobacionUsername()
        })

        // Cualquier cambio del formulario altera el resumen del paso 4.
        listOf(etNombre, etTelefono, etBio, etExperiencia, etHabilidades).forEach { campo ->
            campo.addTextChangedListener(alCambiarTexto { if (!llenandoFormulario) refrescarResumen() })
        }

        // Búsqueda de oficios: filtra la lista del paso 3.
        etBuscarOficio.addTextChangedListener(alCambiarTexto {
            if (!llenandoFormulario) filtrarOficios(it?.toString().orEmpty())
        })
    }

    /**
     * Envoltorio de [TextWatcher] para no repetir el boilerplate de los tres
     * métodos en cada campo.
     */
    private fun alCambiarTexto(accion: (Editable?) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) = accion(s)
    }

    // ═══════════════════════════════════════════════════════════════
    //  CARGA DEL PERFIL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Lee `users/{uid}` y rellena el formulario.
     *
     * Son tres pasos separados a propósito, para que ninguno pueda vaciar la
     * pantalla:
     *
     *  1. `loadProfile` es una **lectura pura** y las Rules la autorizan siempre al
     *     dueño (`allow get: if isOwner(uid)`). Si esto falla no hay perfil que
     *     mostrar, así que sí se avisa.
     *  2. `ensureProfileInitialized` **escribe** los bloques de la FASE 2 que las
     *     cuentas de la FASE 1 no tienen. Es un extra: si las Rules no lo admiten
     *     (`PERMISSION_DENIED`) el formulario ya está lleno y aquí solo se anota el
     *     motivo en el log. Antes esta escritura era también la lectura, y por eso
     *     un problema de reglas dejaba la pantalla en blanco con un error.
     *  3. [prellenarDesdePadron] trae cumpleaños, género y ubicación oficial.
     */
    private fun cargarPerfil() {
        val actual = auth.currentUser?.uid
        if (actual == null) {
            showToast(getString(R.string.profile_error_sesion))
            finish()
            applyExitTransition()
            return
        }
        uid = actual

        lifecycleScope.launch {
            // 1) Lectura: el formulario se llena sí o sí.
            val inicial = repository.loadProfile(actual).getOrElse { error ->
                showToast(getString(R.string.profile_error_cargar, motivoFirestore(error)))
                return@launch
            }
            perfilCargado = inicial
            llenarFormulario(inicial)
            if (entradaCompletar) updateStep(primerPasoIncompleto(inicial))

            // 2) Siembra de los bloques de la FASE 2 (escritura opcional).
            if (!inicial.tieneBloquesFase2) {
                repository.ensureProfileInitialized(actual)
                    .onSuccess { datos ->
                        perfilCargado = datos
                        adoptarUsernameReservado(datos)
                    }
                    .onFailure { error ->
                        Log.w(TAG, "No se pudieron crear los bloques del perfil: ${motivoFirestore(error)}")
                        if (isFinishing || isDestroyed) return@onFailure
                        showToast(getString(R.string.profile_error_sembrar, motivoFirestore(error)))
                    }
            }

            // 3) Lo que RENIEC/SUNAT ya sabía y el registro no guardó.
            prellenarDesdePadron(inicial)
        }
    }

    /**
     * Rellena cumpleaños, género y ubicación con lo que devuelve el padrón oficial.
     *
     * Solo se toca lo que está **vacío**: si el usuario ya escribió algo en esos
     * campos (o ya estaban guardados), se respeta. Nunca pisa lo que hay, así que
     * da igual que la respuesta llegue tarde y el usuario haya empezado a escribir.
     *
     * No escribe nada: los datos se quedan en el formulario y se guardan con el
     * resto cuando el usuario pulse "Finalizar". Si el padrón no responde, el
     * formulario se queda como estaba y esos campos siguen siendo pendientes.
     */
    private fun prellenarDesdePadron(perfil: UserProfile) {
        lifecycleScope.launch {
            val datos = padronRepository.datosQueFaltan(perfil).getOrNull()
                ?.takeIf { it.tieneAlgo }
                ?: return@launch
            if (isFinishing || isDestroyed) return@launch

            llenandoFormulario = true
            var cambios = 0

            if (selectedDate.isBlank() && datos.birthDate.isNotEmpty()) {
                selectedDate = datos.birthDate
                pintarFecha()
                cambios++
            }

            if (selectedGenero.isBlank() && datos.gender.isNotEmpty()) {
                selectedGenero = datos.gender
                when (selectedGenero) {
                    Genders.MASCULINO -> rbMasculino.isChecked = true
                    Genders.FEMENINO -> rbFemenino.isChecked = true
                    Genders.OTRO -> rbOtro.isChecked = true
                }
                cambios++
            }

            if (selectedDepartamento.isBlank() && datos.department.isNotEmpty()) {
                selectedDepartamento = datos.department
                cambios++
            }
            if (selectedProvincia.isBlank() && datos.province.isNotEmpty()) {
                selectedProvincia = datos.province
                cambios++
            }
            if (selectedDistrito.isBlank() && datos.district.isNotEmpty()) {
                selectedDistrito = datos.district
                cambios++
            }

            if (cambios == 0) {
                llenandoFormulario = false
                return@launch
            }

            // `pintarUbicacion` ya refresca el resumen del paso 4 con todo lo nuevo.
            pintarUbicacion()
            if (datos.birthDate.isNotEmpty() || datos.gender.isNotEmpty()) {
                // La nota de arriba decía "Verificado con RENIEC" fijo; con un RUC
                // eso es falso, así que se nombra el padrón que realmente se usó.
                tvDniNota.text = getString(
                    R.string.edit_perfil_padron_nota,
                    if (perfil.identity.verifiedWith == IdentitySources.SUNAT) {
                        IdentitySources.SUNAT
                    } else {
                        IdentitySources.RENIEC
                    }
                )
            }
            Log.i(TAG, "Datos del padron completados: $cambios campo(s)")

            llenandoFormulario = false
        }
    }

    /**
     * Primer paso que todavía tiene algo pendiente.
     *
     * Es lo que hace útil el botón "Completar perfil" del banner: en vez de abrir
     * siempre un paso fijo, aterriza donde el usuario puede subir el porcentaje de
     * verdad. Si no falta nada, se abre el resumen (paso 4).
     *
     * Los nombres comparados son las etiquetas que devuelve
     * [ProfileCompletion.checksFor], que son las que el resumen del paso 4
     * enseña al usuario: si se renombraran allí, dejarían de casar y habría que
     * actualizar las dos listas.
     */
    private fun primerPasoIncompleto(perfil: UserProfile): Int {
        val faltan = perfil.completion().missing
        return when {
            faltan.any { it in CAMPOS_PASO_1 } -> 1
            faltan.any { it in CAMPOS_PASO_2 } -> 2
            faltan.any { it in CAMPOS_PASO_3 } -> 3
            else -> PASOS_TOTAL
        }
    }

    /**
     * Cuando la siembra reserva un `@usuario` libre, se muestra ese en el campo.
     *
     * El formulario se llena antes de que la siembra termine, y en ese momento el
     * campo solo tiene la *sugerencia* de [ProfileRepository.sugerirUsername], que
     * no comprobó disponibilidad. La siembra sí la comprobó, así que su valor gana
     * —salvo que el usuario ya haya escrito algo, que es lo que marca
     * [usernameTocado].
     */
    private fun adoptarUsernameReservado(datos: UserProfile) {
        if (isFinishing || isDestroyed) return
        val reservado = datos.profile.username
        if (reservado.isBlank() || usernameTocado) return
        if (etUsername.text.toString().trim() == reservado) return

        llenandoFormulario = true
        etUsername.setText(reservado)
        llenandoFormulario = false

        pintarEstadoUsername(
            getString(R.string.edit_perfil_username_disponible, normalizarUsername(reservado)),
            COLOR_OK
        )
    }

    /**
     * Vuelca el perfil de Firestore en los campos.
     *
     * `llenandoFormulario` apaga los watchers: sin él, poner el texto dispararía
     * una comprobación de `@usuario` por cada campo y un resumen antes de tiempo.
     */
    private fun llenarFormulario(perfil: UserProfile) {
        llenandoFormulario = true

        // --- Paso 1: lo que viene del registro es de solo lectura ---
        modoContratante = perfil.activeRole == com.proyecto.chambaya.data.model.UserRoles.CONTRATANTE
        val identidad = perfil.identity
        val documentoContratante = perfil.employer.documentNumber
        val tipoDocumentoContratante = perfil.employer.documentType
        val esContratanteConIdentidadPropia =
            modoContratante && documentoContratante.isNotBlank() && tipoDocumentoContratante.isNotBlank()

        findViewById<TextView>(R.id.labelDocumento).text =
            if (esContratanteConIdentidadPropia) tipoDocumentoContratante
            else if (identidad.documentType == com.proyecto.chambaya.data.model.IdentityDocumentTypes.RUC) "RUC"
            else "DNI"

        if (esContratanteConIdentidadPropia) {
            tvDni.text = documentoContratante
            iconDniVerificado.isVisible = true
            tvDniNota.isVisible = true
            tvDniNota.text = if (tipoDocumentoContratante == com.proyecto.chambaya.data.model.IdentityDocumentTypes.RUC) {
                "✓ Verificado con SUNAT - No se puede modificar aquí"
            } else {
                "✓ Verificado con RENIEC - No se puede modificar aquí"
            }
        } else if (identidad.identityVerified) {
            tvDni.text = identidad.documentNumber.ifBlank { identidad.documentNumberMasked }
            iconDniVerificado.isVisible = true
            tvDniNota.isVisible = true
            tvDniNota.text = getString(
                if (identidad.verifiedWith == IdentitySources.SUNAT) {
                    R.string.edit_perfil_ruc_nota
                } else {
                    R.string.edit_perfil_dni_nota
                }
            )
        } else {
            tvDni.text = getString(R.string.edit_perfil_dni_no_verificado)
            iconDniVerificado.isVisible = false
            tvDniNota.isVisible = false
        }

        val email = perfil.auth.email.ifBlank { auth.currentUser?.email.orEmpty() }
        etEmail.setText(email)
        iconEmailVerificado.isVisible = perfil.auth.emailVerified && email.isNotBlank()

        // Foto guardada en Cloudinary por el usuario; si no hay, sus iniciales.
        OficioIcons.cargarAvatar(
            ivAvatar,
            perfil.profile.profilePhotoUrl,
            perfil.profile.fullName,
            perfil.profile.username.ifBlank { perfil.uid }
        )
        selectedImageUri = null
        fotoPendiente = false

        // --- Campos editables ---
        etNombre.setText(perfil.profile.fullName)
        etTelefono.setText(perfil.profile.phone)

        // Una cuenta de la FASE 1 nace sin `@usuario`, y las Rules exigen uno válido
        // para cualquier escritura de la FASE 2: sin esto el paso 1 bloquearía el
        // guardado con "El @usuario debe tener entre 3 y 30 caracteres" sin que
        // hubiera ningún motivo visible. Se propone uno derivado del nombre, sin
        // reservar nada todavía: la comprobación de disponibilidad real sale al
        // escribir y también al guardar.
        etUsername.setText(
            if (perfil.profile.username.isNotBlank()) {
                perfil.profile.username
            } else {
                repository.sugerirUsername(perfil)
            }
        )
        usernameTocado = false

        // Las Rules admiten 500 caracteres de descripción: se corta aquí para que
        // el usuario no pueda escribir de más y que el guardado no sea rechazado.
        etBio.filters = arrayOf(InputFilter.LengthFilter(ProfileLimits.BIO_MAX))
        etBio.setText(perfil.profile.bio)

        selectedDate = perfil.profile.birthDate
        pintarFecha()
        selectedGenero = perfil.profile.gender
        when (selectedGenero) {
            Genders.MASCULINO -> rbMasculino.isChecked = true
            Genders.FEMENINO -> rbFemenino.isChecked = true
            Genders.OTRO -> rbOtro.isChecked = true
        }

        selectedDepartamento = perfil.profile.department
        selectedProvincia = perfil.profile.province
        selectedDistrito = perfil.profile.district
        pintarUbicacion()

        etExperiencia.setText(
            if (perfil.worker.experienceYears > 0) perfil.worker.experienceYears.toString() else ""
        )

        // Solo se ofrecen las especialidades que siguen en el catálogo: si el
        // JSON cambia, no se obligatorio elegir un oficio que ya no existe.
        val validas = OficioCatalog.load(this)
        especialidadesElegidas.clear()
        perfil.worker.specialties.forEach { elegida ->
            val oficial = validas.firstOrNull {
                OficioCatalog.normalizar(it.categoria) == OficioCatalog.normalizar(elegida)
            }
            if (oficial != null) especialidadesElegidas += oficial.categoria
        }
        adapterOficios.setSeleccionados(especialidadesElegidas)
        actualizarResumenEspecialidades()
        filtrarOficios(etBuscarOficio.text.toString())

        etHabilidades.setText(perfil.worker.skills.joinToString(", "))

        switchMostrarTelefono.isChecked = perfil.privacy.showPhone
        switchMostrarEmail.isChecked = perfil.privacy.showEmail
        switchMostrarUbicacion.isChecked = perfil.privacy.showExactAddress

        // CONTRATANTE no necesita datos biográficos del trabajador.
        findViewById<View>(R.id.labelFechaNacimiento).isVisible = !modoContratante
        inputFechaNacimiento.isVisible = !modoContratante
        findViewById<View>(R.id.labelGenero).isVisible = !modoContratante
        rgGenero.isVisible = !modoContratante

        // `layoutStep3` es el contenedor del paso entero, a diferencia de los campos
        // de arriba, que son de un paso concreto. Por eso NO basta con mirar el rol:
        // se muestra solo si además es el paso visible. Sin la condición del paso,
        // abrir en el 1 ("Editar perfil") dejaba el paso 3 del trabajador encima del
        // 1, porque esto corre DESPUÉS de `updateStep` y lo pisaba. Con ella, ambas
        // reglas coinciden y el orden deja de importar.
        layoutStep3.isVisible = !modoContratante && currentStep == 3

        llenandoFormulario = false

        // El resumen se calcula con lo que hay, no con lo que se está escribiendo.
        refrescarResumen()
        pintarEstadoUsername(
            getString(R.string.edit_perfil_username_neutro),
            COLOR_NEUTRO
        )
    }

    // ═══════════════════════════════════════════════════════════════
    //  @USUARIO ÚNICO
    // ═══════════════════════════════════════════════════════════════

    private fun programarComprobacionUsername() {
        jobUsername?.cancel()
        jobUsername = lifecycleScope.launch {
            delay(ESPERA_USERNAME_MS)
            comprobarUsername()
        }
    }

    /**
     * Comprueba contra `usernames/{normalizado}` si el `@usuario` escrito está
     * libre. El repositorio trata como disponible el que ya le pertenece a este
     * usuario, así que no hay que comparar con el valor anterior aquí.
     */
    private suspend fun comprobarUsername() {
        val actual = auth.currentUser?.uid ?: return
        val escrito = etUsername.text.toString().trim()
        val normalizado = normalizarUsername(escrito)

        if (escrito.isEmpty()) {
            pintarEstadoUsername(
                getString(R.string.edit_perfil_username_neutro),
                COLOR_NEUTRO
            )
            return
        }

        if (!esUsernameValido(escrito)) {
            pintarEstadoUsername(
                getString(R.string.edit_perfil_username_invalido),
                COLOR_ERROR
            )
            return
        }

        // Si mientras se comprobaba el usuario seguiría escribiendo, el resultado
        // ya no corresponde a lo que hay en el campo: se descarta.
        if (etUsername.text.toString().trim() != escrito) return

        if (repository.isUsernameAvailable(normalizado, actual)) {
            pintarEstadoUsername(
                getString(R.string.edit_perfil_username_disponible, normalizado),
                COLOR_OK
            )
        } else {
            pintarEstadoUsername(
                getString(R.string.edit_perfil_username_tomado),
                COLOR_ERROR
            )
        }
    }

    private fun pintarEstadoUsername(mensaje: String, color: Int) {
        tvUsernameEstado.text = mensaje
        tvUsernameEstado.setTextColor(color)
    }

    // ═══════════════════════════════════════════════════════════════
    //  PASOS 3 Y 4: RESUMEN EN VIVO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Perfil tal como quedaría con lo que hay escrito ahora mismo.
     *
     * Se construye aplicando el formulario sobre el documento de Firestore, que
     * es exactamente lo mismo que hará [ProfileRepository.saveProfile]. Así el
     * porcentaje del resumen y el que se guarda salen del mismo cálculo.
     */
    private fun perfilPrevisualizado(): UserProfile {
        val base = perfilCargado ?: UserProfile(uid = uid.orEmpty())
        return base.copy(
            profile = base.profile.copy(
                fullName = etNombre.text.toString().trim(),
                username = normalizarUsername(etUsername.text.toString()),
                phone = etTelefono.text.toString().trim(),
                bio = etBio.text.toString().trim(),
                birthDate = selectedDate,
                gender = selectedGenero,
                district = selectedDistrito,
                province = selectedProvincia,
                department = selectedDepartamento,
                profilePhotoUrl = if (fotoPendiente) urlFotoPendiente()
                else base.profile.profilePhotoUrl
            ),
            worker = base.worker.copy(
                experienceYears = experienciaActual(),
                specialties = especialidadesElegidas.toList(),
                skills = repository.parseSkills(etHabilidades.text.toString())
            )
        )
    }

    /**
     * Marcador de foto para el resumen mientras la imagen aún no está subida.
     *
     * Solo se usa en memoria, dentro de [perfilPrevisualizado]: nunca se escribe
     * en Firestore. Sirve para que el porcentaje suba en cuanto el usuario elige
     * una foto, sin esperar a que Cloudinary responda.
     */
    private fun urlFotoPendiente(): String = "pendiente://${uid.orEmpty()}"

    /** Pinta el porcentaje y la lista de lo que falta, sin escribir nada. */
    private fun refrescarResumen() {
        val completion: ProfileCompletion = repository.computeCompletion(perfilPrevisualizado())

        progressBarPerfil.progress = completion.percent
        tvPorcentajePerfil.text = getString(R.string.profile_porcentaje, completion.percent)

        tvResumenTitulo.text = when {
            completion.isComplete -> getString(R.string.edit_perfil_resumen_completo)
            completion.percent >= 50 -> getString(R.string.edit_perfil_resumen_casi)
            else -> getString(R.string.edit_perfil_resumen_inicial)
        }

        tvResumenCompletitud.text = if (completion.missing.isEmpty()) {
            getString(R.string.edit_perfil_resumen_todo)
        } else {
            // OJO: la cadena tiene DOS marcadores (`%1$d` y `%2$s`). Pasar solo uno
            // lanzaba `IllegalFormatException` al abrir el paso 4 con algo
            // pendiente, que es justo el caso normal de un perfil a medias.
            getString(
                R.string.edit_perfil_resumen_falta,
                completion.percent,
                completion.missing.joinToString(", ")
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  NAVEGACIÓN POR PASOS
    // ═══════════════════════════════════════════════════════════════

    private fun updateStep(step: Int) {
        currentStep = if (modoContratante && step == 3) 4 else step
        val visibleStep = currentStep

        // Actualizar título e indicador
        tvTitulo.setText(
            when (visibleStep) {
                1 -> R.string.edit_perfil_titulo_paso1
                2 -> R.string.edit_perfil_titulo_paso2
                3 -> R.string.edit_perfil_titulo_paso3
                else -> R.string.edit_perfil_titulo_paso4
            }
        )
        tvStepIndicator.text = getString(R.string.edit_perfil_paso_de, visibleStep, PASOS_TOTAL)

        // Mostrar/ocultar contenedores
        layoutStep1.isVisible = visibleStep == 1
        layoutStep2.isVisible = visibleStep == 2
        layoutStep3.isVisible = visibleStep == 3
        layoutStep4.isVisible = visibleStep == 4

        // Actualizar stepper visual
        updateStepperVisuals()

        // Actualizar botones de navegación
        updateNavigationButtons()

        // El resumen se pinta al llegar al paso 4 con lo último escrito.
        if (step == 4) refrescarResumen()

        // Scroll al inicio
        findViewById<NestedScrollView>(R.id.scrollEditarPerfil).smoothScrollTo(0, 0)
    }

    private fun updateStepperVisuals() {
        val darkColor = Color.parseColor("#111827")
        val inactiveColor = Color.parseColor("#E2E8F0")
        val mutedText = Color.parseColor("#64748B")

        // Paso 1
        updateSingleStep(1, step1Circle, step1Number, step1Check, darkColor, inactiveColor, mutedText)
        line1.setBackgroundColor(if (currentStep > 1) darkColor else inactiveColor)

        // Paso 2
        updateSingleStep(2, step2Circle, step2Number, step2Check, darkColor, inactiveColor, mutedText)
        line2.setBackgroundColor(if (currentStep > 2) darkColor else inactiveColor)

        // Paso 3
        updateSingleStep(3, step3Circle, step3Number, step3Check, darkColor, inactiveColor, mutedText)
        line3.setBackgroundColor(if (currentStep > 3) darkColor else inactiveColor)

        // Paso 4
        updateSingleStep(4, step4Circle, step4Number, step4Check, darkColor, inactiveColor, mutedText)
    }

    private fun updateSingleStep(
        stepNumber: Int,
        circle: View,
        numberTv: TextView,
        checkIv: ImageView,
        darkColor: Int,
        inactiveColor: Int,
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
        // Botón Atrás solo activo después del paso 1
        btnAtras.isEnabled = currentStep > 1
        btnAtras.alpha = if (currentStep > 1) 1f else 0.5f

        // Botón Siguiente cambia en el último paso
        if (currentStep == PASOS_TOTAL) {
            btnSiguiente.setText(R.string.edit_perfil_btn_finalizar)
            btnSiguiente.icon = null
        } else {
            btnSiguiente.setText(R.string.edit_perfil_btn_siguiente)
            btnSiguiente.setIconResource(R.drawable.ic_arrow_forward)
        }
    }

    private fun handleNextStep() {
        // El paso 1 es el único con datos que las Rules exigen: sin nombre,
        // sin @usuario válido o sin teléfono, Firestore rechazaría el guardado.
        if (currentStep == 1 && !validateStep1()) return

        if (currentStep < PASOS_TOTAL) {
            val siguiente = if (modoContratante && currentStep == 2) 4 else currentStep + 1
            updateStep(siguiente)
        } else {
            finalizarActualizacionPerfil()
        }
    }

    /**
     * Solo bloquea lo que las Firestore Security Rules rechazan.
     *
     * El nombre y el `@usuario` sí son obligatorios (la regla exige 3-30
     * caracteres). El teléfono NO: el registro de la FASE 1 no lo pide, así que
     * si se bloqueara aquí nadie podría guardar hasta ponerse uno. Se avisa y se
     * deja pasar; el paso 4 recuerda que falta y el porcentaje lo refleja.
     *
     * Los pasos 2 y 3 tampoco bloquean por la misma razón: sus campos son
     * opcionales y el resumen del paso 4 ya enseña lo que falta.
     */
    private fun validateStep1(): Boolean {
        val nombre = etNombre.text.toString().trim()
        val escrito = etUsername.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()

        if (nombre.length < 3) {
            etNombre.error = getString(R.string.edit_perfil_error_nombre)
            etNombre.requestFocus()
            showToast(getString(R.string.edit_perfil_error_nombre))
            return false
        }

        if (!esUsernameValido(escrito)) {
            etUsername.error = getString(R.string.edit_perfil_username_invalido)
            etUsername.requestFocus()
            showToast(getString(R.string.edit_perfil_username_invalido))
            return false
        }

        // Aviso, no bloqueo: un teléfono mal escrito se avisa, uno vacío no.
        val digitos = telefono.filter { it.isDigit() }
        if (digitos.isNotEmpty() && digitos.length !in ProfileLimits.PHONE_MIN..ProfileLimits.PHONE_MAX) {
            etTelefono.error = getString(R.string.edit_perfil_error_telefono)
            showToast(getString(R.string.edit_perfil_error_telefono))
        } else {
            etTelefono.error = null
        }

        return true
    }

    // ═══════════════════════════════════════════════════════════════
    //  GUARDADO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Sube la foto (si hay una nueva) y guarda el borrador.
     *
     * El orden importa: primero se sube la imagen a Cloudinary y se reciben su
     * `url` y su `publicId`, y solo después se escribe el documento. Las Rules
     * exigen que la foto venga de `chambaya/fotos-perfil/{uid}/` (la misma carpeta
     * que devuelve `CloudinaryUploader.carpetaDePerfil`), así que guardarla
     * antes de tener esos dos datos no sería posible.
     */
    private fun finalizarActualizacionPerfil() {
        val actual = uid
        if (actual == null) {
            showToast(getString(R.string.profile_error_sesion))
            return
        }

        val borrador = construirBorrador()
        val errores = repository.validate(borrador)
        if (errores.isNotEmpty()) {
            showErrores(errores)
            return
        }

        bloquearBotones(true)

        lifecycleScope.launch {
            // 1) Foto, si el usuario eligió una nueva en esta sesión.
            var fotoUrl: String? = null
            var fotoPublicId: String? = null

            if (fotoPendiente) {
                val uri = selectedImageUri
                if (uri == null) {
                    bloquearBotones(false)
                    showToast(getString(R.string.edit_perfil_error_foto))
                    return@launch
                }
                if (!uploader.estaConfigurado) {
                    bloquearBotones(false)
                    showToast(getString(R.string.edit_perfil_foto_sin_configurar))
                    return@launch
                }

                when (val resultado = uploader.uploadProfilePhoto(this@EditarPerfilActivity, actual, uri)) {
                    is PhotoUploadResult.Success -> {
                        fotoUrl = resultado.image.url
                        fotoPublicId = resultado.image.publicId
                    }
                    is PhotoUploadResult.Rejected -> {
                        bloquearBotones(false)
                        showToast(getString(R.string.edit_perfil_foto_error, resultado.message))
                        return@launch
                    }
                    is PhotoUploadResult.NetworkError -> {
                        bloquearBotones(false)
                        showToast(getString(R.string.edit_perfil_foto_sin_conexion))
                        return@launch
                    }
                }
            }

            // 2) Perfil. El repositorio reserva el @usuario y deriva el porcentaje.
            repository.saveProfile(actual, borrador, fotoUrl, fotoPublicId)
                .onSuccess { guardado ->
                    perfilCargado = guardado
                    // El perfil de "Mi Perfil" se repinta al volver, así que esta
                    // caché se actualiza aquí: si el usuario entra a Ajustes antes
                    // de que se relea Firestore, ve lo mismo que acaba de guardar.
                    ProfileCache.perfil = guardado
                    showSuccessDialog(guardado.completion().percent)
                }
                .onFailure { error ->
                    bloquearBotones(false)
                    Log.w(TAG, "No se pudo guardar el perfil: ${motivoFirestore(error)}", error)
                    when (error) {
                        is UsernameYaTomado -> {
                            pintarEstadoUsername(
                                getString(R.string.edit_perfil_username_tomado),
                                COLOR_ERROR
                            )
                            etUsername.requestFocus()
                            updateStep(1)
                        }
                        // `PERMISSION_DENIED` casi siempre es una cosa de despliegue,
                        // no del usuario: las Rules de la FASE 2 sin publicar. El
                        // `Toast` genérico no lo dice y deja sin respuesta a quien
                        // solo quiere rellenar su perfil.
                        else -> {
                            val motivo = motivoFirestore(error)
                            showToast(
                                if (motivo == "PERMISSION_DENIED") {
                                    getString(R.string.edit_perfil_error_guardar_permisos)
                                } else {
                                    getString(R.string.edit_perfil_error_guardar, motivo)
                                }
                            )
                        }
                    }
                }
        }
    }

    /**
     * Lo que el usuario escribió, sin los campos de solo lectura.
     *
     * Lo consume [ProfileRepository.saveProfile], que se encarga de reservar el
     * `@usuario`, filtrar las especialidades contra el catálogo y derivar el
     * porcentaje de completitud.
     */
    private fun construirBorrador(): ProfileDraft {
        val base = perfilCargado
        val habilidades = repository.parseSkills(etHabilidades.text.toString())

        return ProfileDraft(
            fullName = etNombre.text.toString().trim(),
            username = etUsername.text.toString().trim(),
            phone = etTelefono.text.toString().trim(),
            bio = etBio.text.toString().trim(),
            birthDate = selectedDate,
            gender = selectedGenero,
            department = selectedDepartamento,
            province = selectedProvincia,
            district = selectedDistrito,
            experienceYears = experienciaActual(),
            // Se vuelve a pasar por el catálogo: el paso 3 solo ofrece oficios que
            // existen, pero así el documento nunca guarda un oficio inventado.
            specialties = repository.filtrarEspecialidades(this, especialidadesElegidas.toList()),
            skills = habilidades,
            showPhone = switchMostrarTelefono.isChecked,
            showEmail = switchMostrarEmail.isChecked,
            showExactAddress = switchMostrarUbicacion.isChecked,
            workerEnabled = base?.worker?.enabled ?: true
        )
    }

    /** Años escritos, acotados al rango que aceptan las Rules (0..70). */
    private fun experienciaActual(): Int =
        etExperiencia.text.toString().trim().toIntOrNull()?.coerceIn(0, ProfileLimits.EXPERIENCE_MAX)
            ?: 0

    private fun bloquearBotones(bloquear: Boolean) {
        if (bloquear) {
            btnSiguiente.isEnabled = false
            btnAtras.isEnabled = false
            btnSiguiente.setText(R.string.edit_perfil_btn_guardando)
        } else {
            // `updateNavigationButtons` devuelve el botón al texto que le toca.
            updateNavigationButtons()
        }
    }

    private fun showErrores(errores: List<String>) {
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(R.string.edit_perfil_error_titulo)
            .setMessage(errores.joinToString("\n\n") { "• $it" })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showSuccessDialog(completitud: Int) {
        val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
        builder.setTitle(R.string.edit_perfil_exito_titulo)
        builder.setMessage(
            getString(R.string.edit_perfil_exito_mensaje, completitud) +
                "\n\n" +
                getString(R.string.edit_perfil_exito_verificaciones)
        )
        builder.setPositiveButton(R.string.edit_perfil_exito_continuar) { dialog, _ ->
            dialog.dismiss()
            setResult(Activity.RESULT_OK)
            finish()
            applyExitTransition()
        }
        builder.setCancelable(false)
        builder.show()
    }

    // ═══════════════════════════════════════════════════════════════
    //  FOTO
    // ═══════════════════════════════════════════════════════════════

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Si ya hay una fecha guardada se abre en ella, no en hoy.
        val partes = selectedDate.split("/")
        val anio = partes.getOrNull(2)?.toIntOrNull() ?: (calendar.get(Calendar.YEAR) - 25)
        val mes = (partes.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
        val dia = partes.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

        // El día se acota a lo que tiene el mes elegido, no a 28: con 28 quien nació
        // un día 30 veía el selector en una fecha que nunca podría confirmar.
        val diaValido = dia.coerceIn(1, diasDelMes(mes.coerceIn(0, 11), anio))

        val datePickerDialog = DatePickerDialog(
            this,
            R.style.CustomDatePickerTheme,
            { _, year, month, dayOfMonth ->
                selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                tvFecha.text = selectedDate
                tvFecha.setTextColor(COLOR_ENTRADA)
                refrescarResumen()
            },
            anio.coerceAtLeast(ANIO_MINIMO),
            mes.coerceIn(0, 11),
            diaValido
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

    /**
     * Días del mes para el `DatePicker`, respetando años bisiestos.
     *
     * El `DatePicker` no revisa el día contra el mes: si se le pasa el 31 con un mes
     * de 30, salta al mes siguiente. Por eso el día se acota antes de dárselo.
     */
    private fun diasDelMes(mes: Int, anio: Int): Int = when (mes) {
        Calendar.FEBRUARY -> if (esBisiesto(anio)) 29 else 28
        Calendar.APRIL, Calendar.JUNE, Calendar.SEPTEMBER, Calendar.NOVEMBER -> 30
        else -> 31
    }

    private fun esBisiesto(anio: Int): Boolean =
        (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0

    // ═══════════════════════════════════════════════════════════════
    //  UBICACIÓN EN CASCADA
    // ═══════════════════════════════════════════════════════════════

    /**
     * El texto del layout ya decía "Distrito, provincia y departamento", así que
     * el selector es una cascada: primero el departamento, luego su provincia y
     * por último el distrito. Al cambiar un nivel se borra lo que tenía debajo,
     * porque ya no puede ser válido.
     *
     * Cada lista termina en "Otro", que abre un campo para escribir el lugar
     * real: nadie se queda bloqueado por una lista curada.
     */
    private fun showLocationPicker() {
        when {
            selectedDepartamento.isBlank() -> elegirDepartamento()
            selectedProvincia.isBlank() -> elegirProvincia()
            else -> elegirDistrito()
        }
    }

    private fun elegirDepartamento() {
        val opciones = PeruLocations.departamentos + PeruLocations.OTRO
        val titulo = getString(R.string.edit_perfil_ubic_departamento)

        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(titulo)
            .setItems(opciones.toTypedArray()) { dialog, which ->
                val elegido = opciones[which]
                dialog.dismiss()
                if (PeruLocations.esOtro(elegido)) {
                    pedirTextoLibre(titulo) { escrito ->
                        if (escrito.isNotBlank()) {
                            selectedDepartamento = escrito
                            limpiarNivelesInferiores()
                            pintarUbicacion()
                        }
                    }
                } else {
                    selectedDepartamento = elegido
                    limpiarNivelesInferiores()
                    pintarUbicacion()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun elegirProvincia() {
        val opciones = PeruLocations.provincias(selectedDepartamento)
        val titulo = getString(R.string.edit_perfil_ubic_provincia)

        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(titulo)
            .setItems(opciones.toTypedArray()) { dialog, which ->
                val elegido = opciones[which]
                dialog.dismiss()
                if (PeruLocations.esOtro(elegido)) {
                    pedirTextoLibre(titulo) { escrito ->
                        if (escrito.isNotBlank()) {
                            selectedProvincia = escrito
                            selectedDistrito = ""
                            pintarUbicacion()
                        }
                    }
                } else {
                    selectedProvincia = elegido
                    selectedDistrito = ""
                    pintarUbicacion()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun elegirDistrito() {
        val opciones = PeruLocations.distritos(selectedDepartamento, selectedProvincia)
        val titulo = getString(R.string.edit_perfil_ubic_distrito)

        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(titulo)
            .setItems(opciones.toTypedArray()) { dialog, which ->
                val elegido = opciones[which]
                dialog.dismiss()
                if (PeruLocations.esOtro(elegido)) {
                    pedirTextoLibre(titulo) { escrito ->
                        if (escrito.isNotBlank()) {
                            selectedDistrito = escrito
                            pintarUbicacion()
                        }
                    }
                } else {
                    selectedDistrito = elegido
                    pintarUbicacion()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * Diálogo con un único campo de texto, para las opciones "Otro".
     *
     * Se construye la vista a mano en vez de usar `setView` con un `EditText`
     * pelado porque el proyecto no tiene un `TextInputLayout` con el mismo estilo
     * del resto del formulario.
     */
    private fun pedirTextoLibre(titulo: String, alAceptar: (String) -> Unit) {
        val campo = EditText(this).apply {
            hint = getString(R.string.edit_perfil_ubic_otro_hint)
            setTextColor(COLOR_ENTRADA)
            setHintTextColor(COLOR_NEUTRO)
            setSingleLine()
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val margen = (24 * resources.displayMetrics.density).toInt()
            setPadding(margen, margen / 2, margen, 0)
            addView(
                campo,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(titulo)
            .setView(contenedor)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                alAceptar(campo.text.toString().trim())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /** Al cambiar el departamento se invalidan provincia y distrito. */
    private fun limpiarNivelesInferiores() {
        selectedProvincia = ""
        selectedDistrito = ""
    }

    /**
     * Muestra lo elegido. Mientras falte un nivel se dice cuál, para que quede
     * claro que la cascada sigue abierta.
     */
    private fun pintarUbicacion() {
        val etiqueta = PeruLocations.etiqueta(selectedDistrito, selectedProvincia, selectedDepartamento)
        if (etiqueta.isNotBlank()) {
            tvUbicacion.text = etiqueta
            tvUbicacion.setTextColor(COLOR_ENTRADA)
        } else {
            tvUbicacion.setText(R.string.edit_perfil_ubic_vacia)
            tvUbicacion.setTextColor(COLOR_ENTRADA)
        }
        refrescarResumen()
    }

    private fun pintarFecha() {
        // Un `birthDate` con formato raro en el documento (una versión anterior de la
        // app, o un dato escrito a mano) se descarta en vez de intentar pintarlo:
        // el `DatePicker` lo trocearía con `split("/")` y abriría en el día de hoy,
        // lo que perdería la fecha real sin avisar.
        val valida = BirthDates.soloSiValida(selectedDate)
        if (valida.isEmpty()) {
            selectedDate = ""
            tvFecha.setText(R.string.edit_perfil_fecha_vacia)
            tvFecha.setTextColor(COLOR_NEUTRO)
        } else {
            selectedDate = valida
            tvFecha.text = valida
            tvFecha.setTextColor(COLOR_ENTRADA)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ESPECIALIDADES (api_oficios.json)
    // ═══════════════════════════════════════════════════════════════

    private fun setupSelectorOficios() {
        adapterOficios = EspecialidadSelectorAdapter { categoria -> alternarEspecialidad(categoria) }
        rvEspecialidades.layoutManager = LinearLayoutManager(this)
        rvEspecialidades.adapter = adapterOficios
        rvEspecialidades.itemAnimator = null
        filtrarOficios("")
        actualizarResumenEspecialidades()

        // Cabecera tipo acordeón: colapsada no se ve ni el buscador ni la lista,
        // así el catálogo puede tener 6 o 100 oficios sin alargar el paso 3.
        headerEspecialidadesSelector.setOnClickListener { toggleSelectorEspecialidades() }
    }

    /** Muestra u oculta el panel de buscador + lista, rotando el chevron. */
    private fun toggleSelectorEspecialidades() {
        panelEspecialidadesExpandido = !panelEspecialidadesExpandido
        panelEspecialidadesSelector.isVisible = panelEspecialidadesExpandido
        ivChevronEspecialidades.animate()
            .rotation(if (panelEspecialidadesExpandido) 180f else 0f)
            .setDuration(180)
            .start()

        if (panelEspecialidadesExpandido) {
            // Al abrir siempre se ve el catálogo completo (sin el filtro de la
            // última búsqueda), igual que un selector que se acaba de abrir.
            etBuscarOficio.setText("")
            rvEspecialidades.scrollToPosition(0)
        } else {
            ocultarTeclado()
        }
    }

    /**
     * Refleja en la cabecera del acordeón cuántas y cuáles especialidades están
     * elegidas, para que no haga falta desplegar el panel para verlo.
     */
    private fun actualizarResumenEspecialidades() {
        if (especialidadesElegidas.isEmpty()) {
            tvEspecialidadesResumen.text = getString(R.string.edit_perfil_especialidades_placeholder)
            return
        }
        val nombres = especialidadesElegidas.joinToString(", ")
        val contador = getString(
            R.string.edit_perfil_especialidades_contador,
            especialidadesElegidas.size,
            ProfileLimits.SPECIALTY_MAX
        )
        tvEspecialidadesResumen.text = "$nombres · $contador"
    }

    /**
     * Filtra el catálogo por lo que se escribe.
     *
     * Busca en la categoría y en el puesto ("albañil" encuentra Construcción aunque
     * la categoría se llame así). Sin texto se muestra la lista entera: son las
     * pocas categorías que tiene el catálogo, no hace falta paginar.
     */
    private fun filtrarOficios(texto: String) {
        val todos = OficioCatalog.load(this)
        val consulta = OficioCatalog.normalizar(texto)

        val filtrados = if (consulta.isEmpty()) {
            todos
        } else {
            todos.filter {
                OficioCatalog.normalizar(it.categoria).contains(consulta) ||
                    OficioCatalog.normalizar(it.puesto).contains(consulta)
            }
        }

        adapterOficios.submit(filtrados)
        tvSinResultadosOficio.isVisible = filtrados.isEmpty()
        rvEspecialidades.isVisible = filtrados.isNotEmpty()
    }

    /**
     * Marca o desmarca un oficio, respetando el tope de
     * [ProfileLimits.SPECIALTY_MAX]: al llegar al tope se avisa en vez de ignorar
     * el toque sin explicación.
     */
    private fun alternarEspecialidad(categoria: String) {
        ocultarTeclado()
        if (!especialidadesElegidas.remove(categoria)) {
            if (especialidadesElegidas.size >= ProfileLimits.SPECIALTY_MAX) {
                showToast(
                    getString(R.string.edit_perfil_error_especialidades, ProfileLimits.SPECIALTY_MAX)
                )
                return
            }
            especialidadesElegidas += categoria
        }
        adapterOficios.setSeleccionados(especialidadesElegidas)
        actualizarResumenEspecialidades()
        refrescarResumen()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "EditarPerfil"

        /** Paso desde el que se empieza el asistente. */
        const val EXTRA_START_STEP = "com.proyecto.chambaya.extra_start_step"

        const val PASOS_TOTAL = 4

        /** "Editar perfil": se recorre el asistente entero desde el principio. */
        const val PASO_INICIO_EDITAR = 1

        /**
         * "Completar perfil": no se entra por un paso fijo, sino por el primero con
         * datos pendientes (ver [primerPasoIncompleto]). El valor solo sirve para
         * distinguir esta entrada de la de "Editar perfil".
         */
        const val PASO_INICIO_COMPLETAR = 3

        /** Espera antes de preguntar a Firestore si el @usuario está libre. */
        private const val ESPERA_USERNAME_MS = 450L

        /** Suelo del `DatePicker` de nacimiento: nadie que use la app nació antes. */
        private const val ANIO_MINIMO = 1926

        // Qué campos de cada paso se revisan para decidir por dónde empezar. Son
        // las etiquetas de `ProfileCompletion.checksFor`; ver la nota de
        // `primerPasoIncompleto`.
        private val CAMPOS_PASO_1 = setOf(
            "Foto de perfil", "Nombre completo", "Nombre de usuario", "Teléfono"
        )
        private val CAMPOS_PASO_2 = setOf(
            "Descripción", "Distrito", "Provincia", "Departamento",
            "Fecha de nacimiento", "Género"
        )
        private val CAMPOS_PASO_3 = setOf(
            "Años de experiencia", "Especialidades", "Habilidades"
        )

        private val COLOR_ENTRADA = Color.parseColor("#111827")
        private val COLOR_NEUTRO = Color.parseColor("#9CA3AF")
        private val COLOR_OK = Color.parseColor("#00A859")
        private val COLOR_ERROR = Color.parseColor("#DC2626")
    }
}
