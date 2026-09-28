package com.proyecto.chambaya.ui.workplace

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.PeruLocations
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.Workplace
import com.proyecto.chambaya.data.model.WorkplaceDraft
import com.proyecto.chambaya.data.model.WorkplaceLimits
import com.proyecto.chambaya.data.model.WorkplaceTypes
import com.proyecto.chambaya.data.remote.CloudinaryUploader
import com.proyecto.chambaya.data.remote.PhotoUploadResult
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import com.proyecto.chambaya.data.repository.motivoFirestore
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.launch

/**
 * FASE 4 — Crear / editar el lugar del contratante.
 *
 * Un contratante tiene UN lugar principal (`users/{uid}.employer.workplaceId`).
 * La foto es opcional y sube a `chambaya/fotos-lugares/{uid}/{workplaceId}`.
 * La fecha/hora la sella Firestore (`createdAt/updatedAt`), nunca se escribe a mano.
 *
 * A diferencia del avatar (circular + recorte), la foto del local es apaisada y
 * va sin recorte: se usa tal cual y [CloudinaryUploader] la reescala a 1024 px.
 */
class EditarLugarActivity : AppCompatActivity() {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val repository = WorkplaceRepository()
    private val profileRepository = ProfileRepository()
    private val uploader = CloudinaryUploader()

    private var uid: String? = null
    private var lugarExistente: Workplace? = null
    private var cargando = true

    private lateinit var tvTitulo: TextView
    private lateinit var ivFoto: ShapeableImageView
    private lateinit var btnCambiarFoto: MaterialButton
    private lateinit var btnQuitarFoto: MaterialButton
    private lateinit var etNombre: EditText
    private lateinit var inputTipo: FrameLayout
    private lateinit var tvTipo: TextView
    private lateinit var etSector: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etDireccion: EditText
    private lateinit var inputUbicacion: FrameLayout
    private lateinit var tvUbicacion: TextView
    private lateinit var tvCoords: TextView
    private lateinit var btnGps: MaterialButton
    private lateinit var btnGuardar: MaterialButton
    private lateinit var btnEliminar: MaterialButton

    private var tipoElegido: String = WorkplaceTypes.OTRO
    private var depElegido = ""
    private var provElegida = ""
    private var distElegido = ""
    private var latitud: Double? = null
    private var longitud: Double? = null

    /** Foto elegida en esta sesión, aún no subida. */
    private var fotoUriPendiente: Uri? = null

    /** `true` si había foto guardada y el usuario la quitó. */
    private var fotoQuitada = false

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                fotoUriPendiente = uri
                fotoQuitada = false
                ivFoto.load(uri) { crossfade(true) }
                toast(getString(R.string.lugar_foto_elegida))
            }
        }
    }

    private val permisoGpsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val ok = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) fijarGps() else toast(getString(R.string.lugar_gps_sin_permiso))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarraEstadoUtils.aplicarColor(this, Color.parseColor("#FFFFFF"))
        setContentView(R.layout.activity_editar_lugar)
        enlazar()
        escuchar()
        cargar()
    }

    private fun enlazar() {
        tvTitulo = findViewById(R.id.tvTituloLugar)
        ivFoto = findViewById(R.id.ivFotoLugar)
        btnCambiarFoto = findViewById(R.id.btnCambiarFotoLugar)
        btnQuitarFoto = findViewById(R.id.btnQuitarFotoLugar)
        etNombre = findViewById(R.id.etNombreLugar)
        inputTipo = findViewById(R.id.inputTipoLugar)
        tvTipo = findViewById(R.id.tvTipoLugar)
        etSector = findViewById(R.id.etSectorLugar)
        etDescripcion = findViewById(R.id.etDescripcionLugar)
        etDireccion = findViewById(R.id.etDireccionLugar)
        inputUbicacion = findViewById(R.id.inputUbicacionLugar)
        tvUbicacion = findViewById(R.id.tvUbicacionLugar)
        tvCoords = findViewById(R.id.tvCoordsLugar)
        btnGps = findViewById(R.id.btnGpsLugar)
        btnGuardar = findViewById(R.id.btnGuardarLugar)
        btnEliminar = findViewById(R.id.btnEliminarLugar)
    }

    private fun escuchar() {
        findViewById<android.view.View>(R.id.btnCerrarLugar).setOnClickListener { finish() }
        btnCambiarFoto.setOnClickListener { abrirGaleria() }
        ivFoto.setOnClickListener { abrirGaleria() }
        btnQuitarFoto.setOnClickListener {
            fotoUriPendiente = null
            fotoQuitada = true
            ivFoto.setImageResource(R.drawable.ic_profile_photos)
            toast(getString(R.string.lugar_foto_quitada))
        }
        inputTipo.setOnClickListener { elegirTipo() }
        inputUbicacion.setOnClickListener { elegirUbicacion() }
        btnGps.setOnClickListener { pedirGps() }
        btnGuardar.setOnClickListener { guardar() }
        btnEliminar.setOnClickListener { confirmarEliminar() }
    }

    private fun cargar() {
        val actual = auth.currentUser?.uid
        if (actual == null) {
            toast(getString(R.string.lugar_error_sesion))
            finish()
            return
        }
        uid = actual
        lifecycleScope.launch {
            // Verificar rol contratante con dato fresco (el caché puede estar viejo).
            val perfil = profileRepository.loadProfile(actual).getOrNull()
            if (perfil != null) ProfileCache.perfil = perfil
            val esContratante = perfil?.roles?.contains(UserRoles.CONTRATANTE) == true &&
                perfil.employer.enabled
            if (!esContratante) {
                toast(getString(R.string.lugar_error_solo_contratante))
                finish()
                return@launch
            }
            // Prefill del sector con el del perfil employer (no se vuelve a pedir).
            val pedidoId = intent.getStringExtra(EXTRA_WORKPLACE_ID).orEmpty()
                .ifBlank { perfil.employer.workplaceId.orEmpty() }
            val lugar = if (pedidoId.isNotBlank()) {
                repository.loadById(pedidoId).getOrNull()
            } else {
                repository.loadByOwner(actual).getOrNull()
            }
            if (isFinishing || isDestroyed) return@launch
            if (lugar != null) {
                lugarExistente = lugar
                pintarExistente(lugar, perfil.employer.sector)
            } else if (perfil.employer.sector.isNotBlank()) {
                etSector.setText(perfil.employer.sector)
            }
            cargando = false
        }
    }

    private fun pintarExistente(lugar: Workplace, sectorSugerido: String) {
        tvTitulo.setText(R.string.lugar_titulo_editar)
        etNombre.setText(lugar.name)
        tipoElegido = lugar.type
        pintarTipo()
        etSector.setText(lugar.sector.ifBlank { sectorSugerido })
        etDescripcion.setText(lugar.description)
        etDireccion.setText(lugar.address)
        depElegido = lugar.department
        provElegida = lugar.province
        distElegido = lugar.district
        pintarUbicacion()
        latitud = lugar.location.latitude
        longitud = lugar.location.longitude
        pintarCoords()
        if (lugar.hasPhoto) {
            ivFoto.load(lugar.photoUrl) {
                placeholder(R.drawable.ic_profile_photos)
                error(R.drawable.ic_profile_photos)
                crossfade(true)
            }
        }
        btnEliminar.visibility = android.view.View.VISIBLE
    }

    // ── Selectores ────────────────────────────────────────────────

    private fun elegirTipo() {
        val opciones = WorkplaceTypes.ALL
        val etiquetas = opciones.map { WorkplaceTypes.label(it) }.toTypedArray()
        val actual = opciones.indexOf(tipoElegido).coerceAtLeast(0)
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(getString(R.string.lugar_label_tipo))
            .setSingleChoiceItems(etiquetas, actual) { dialog, which ->
                tipoElegido = opciones[which]
                pintarTipo()
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun pintarTipo() {
        tvTipo.text = WorkplaceTypes.label(tipoElegido)
        tvTipo.setTextColor(Color.parseColor("#111827"))
    }

    private fun elegirUbicacion() {
        when {
            depElegido.isBlank() -> elegirDepartamento()
            provElegida.isBlank() -> elegirProvincia()
            else -> elegirDistrito()
        }
    }

    private fun elegirDepartamento() {
        val opciones = PeruLocations.departamentos + PeruLocations.OTRO
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(R.string.lugar_label_ubicacion)
            .setItems(opciones.toTypedArray()) { dialog, which ->
                val elegido = opciones[which]
                dialog.dismiss()
                if (PeruLocations.esOtro(elegido)) {
                    pedirTexto(getString(R.string.lugar_label_ubicacion)) { escrito ->
                        if (escrito.isNotBlank()) {
                            depElegido = escrito
                            provElegida = ""
                            distElegido = ""
                            pintarUbicacion()
                        }
                    }
                } else {
                    depElegido = elegido
                    provElegida = ""
                    distElegido = ""
                    pintarUbicacion()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun elegirProvincia() {
        val opciones = PeruLocations.provincias(depElegido)
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(R.string.lugar_label_ubicacion)
            .setItems(opciones.toTypedArray()) { dialog, which ->
                val elegido = opciones[which]
                dialog.dismiss()
                if (PeruLocations.esOtro(elegido)) {
                    pedirTexto(getString(R.string.lugar_label_ubicacion)) { escrito ->
                        if (escrito.isNotBlank()) {
                            provElegida = escrito
                            distElegido = ""
                            pintarUbicacion()
                        }
                    }
                } else {
                    provElegida = elegido
                    distElegido = ""
                    pintarUbicacion()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun elegirDistrito() {
        val opciones = PeruLocations.distritos(depElegido, provElegida)
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(R.string.lugar_label_ubicacion)
            .setItems(opciones.toTypedArray()) { dialog, which ->
                val elegido = opciones[which]
                dialog.dismiss()
                if (PeruLocations.esOtro(elegido)) {
                    pedirTexto(getString(R.string.lugar_label_ubicacion)) { escrito ->
                        if (escrito.isNotBlank()) {
                            distElegido = escrito
                            pintarUbicacion()
                        }
                    }
                } else {
                    distElegido = elegido
                    pintarUbicacion()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun pedirTexto(titulo: String, alAceptar: (String) -> Unit) {
        val campo = EditText(this).apply {
            hint = titulo
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

    private fun pintarUbicacion() {
        val etiqueta = PeruLocations.etiqueta(distElegido, provElegida, depElegido)
        if (etiqueta.isNotBlank()) {
            tvUbicacion.text = etiqueta
            tvUbicacion.setTextColor(Color.parseColor("#111827"))
        } else {
            tvUbicacion.setText(R.string.lugar_hint_ubicacion)
            tvUbicacion.setTextColor(Color.parseColor("#9CA3AF"))
        }
    }

    private fun pintarCoords() {
        val lat = latitud
        val lng = longitud
        tvCoords.text = if (lat != null && lng != null) {
            String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)
        } else {
            getString(R.string.lugar_gps_sin_fijar)
        }
    }

    // ── Foto y GPS ────────────────────────────────────────────────

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickerLauncher.launch(intent)
    }

    private fun pedirGps() {
        val fino = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val grueso = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fino || grueso) {
            fijarGps()
        } else {
            permisoGpsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fijarGps() {
        try {
            val cliente = LocationServices.getFusedLocationProviderClient(this)
            val fino = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val grueso = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!fino && !grueso) {
                toast(getString(R.string.lugar_gps_sin_permiso))
                return
            }
            cliente.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        latitud = loc.latitude
                        longitud = loc.longitude
                        pintarCoords()
                        toast(getString(R.string.lugar_gps_ok))
                    } else {
                        toast(getString(R.string.lugar_gps_no_disponible))
                    }
                }
                .addOnFailureListener {
                    toast(getString(R.string.lugar_gps_no_disponible))
                }
        } catch (e: SecurityException) {
            toast(getString(R.string.lugar_gps_sin_permiso))
        }
    }

    // ── Guardado ──────────────────────────────────────────────────

    private fun construirBorrador(): WorkplaceDraft = WorkplaceDraft(
        name = etNombre.text.toString().trim(),
        type = tipoElegido,
        sector = etSector.text.toString().trim(),
        description = etDescripcion.text.toString().trim(),
        address = etDireccion.text.toString().trim(),
        district = distElegido.trim(),
        province = provElegida.trim(),
        department = depElegido.trim(),
        latitude = latitud,
        longitude = longitud
    )

    private fun guardar() {
        val actual = uid ?: run {
            toast(getString(R.string.lugar_error_sesion))
            return
        }
        if (cargando) return
        val borrador = construirBorrador()
        val errores = repository.validate(borrador)
        if (errores.isNotEmpty()) {
            if (borrador.name.trim().length !in WorkplaceLimits.NAME_MIN..WorkplaceLimits.NAME_MAX) {
                etNombre.error = errores.first()
                etNombre.requestFocus()
            }
            toast(errores.first())
            return
        }
        bloquear(true)
        lifecycleScope.launch {
            // 1) Foto nueva, si el usuario eligió una en esta sesión.
            var fotoUrl = ""
            var fotoPublicId = ""
            var subirFoto = false
            val existente = lugarExistente
            val idDestino = existente?.workplaceId?.ifBlank { null }
                ?: repository.newWorkplaceId()

            val pendiente = fotoUriPendiente
            if (pendiente != null) {
                if (!uploader.estaConfigurado) {
                    bloquear(false)
                    toast(getString(R.string.edit_perfil_foto_sin_configurar))
                    return@launch
                }
                when (val r = uploader.uploadWorkplacePhoto(this@EditarLugarActivity, actual, idDestino, pendiente)) {
                    is PhotoUploadResult.Success -> {
                        fotoUrl = r.image.url
                        fotoPublicId = r.image.publicId
                        subirFoto = true
                    }
                    is PhotoUploadResult.Rejected -> {
                        bloquear(false)
                        toast(getString(R.string.lugar_error_foto, r.message))
                        return@launch
                    }
                    is PhotoUploadResult.NetworkError -> {
                        bloquear(false)
                        toast(getString(R.string.lugar_error_foto_conexion))
                        return@launch
                    }
                }
            } else if (fotoQuitada) {
                // Quitar la foto = guardar cadenas vacías.
                subirFoto = true
            }

            // 2) Crear o actualizar.
            val resultado = if (existente == null) {
                repository.create(actual, idDestino, borrador, fotoUrl, fotoPublicId)
            } else if (subirFoto) {
                repository.update(actual, existente.workplaceId, borrador, fotoUrl, fotoPublicId)
            } else {
                repository.update(actual, existente.workplaceId, borrador, null, null)
            }
            resultado
                .onSuccess { guardado ->
                    // Refrescar el enlace en caché para que Publicar lo vea al volver.
                    runCatching {
                        val perfil = profileRepository.loadProfile(actual).getOrNull()
                        if (perfil != null) ProfileCache.perfil = perfil
                    }
                    toast(getString(R.string.lugar_guardado))
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .onFailure { e ->
                    bloquear(false)
                    toast(getString(R.string.lugar_error_guardar, motivoFirestore(e)))
                }
        }
    }

    private fun confirmarEliminar() {
        val existente = lugarExistente ?: return
        AlertDialog.Builder(this, R.style.CustomAlertDialog)
            .setTitle(R.string.lugar_eliminar_titulo)
            .setMessage(getString(R.string.lugar_eliminar_mensaje, existente.name))
            .setPositiveButton(R.string.lugar_btn_eliminar) { _, _ -> eliminar() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun eliminar() {
        val actual = uid ?: return
        val existente = lugarExistente ?: return
        bloquear(true)
        lifecycleScope.launch {
            repository.delete(actual, existente.workplaceId)
                .onSuccess {
                    runCatching {
                        val perfil = profileRepository.loadProfile(actual).getOrNull()
                        if (perfil != null) ProfileCache.perfil = perfil
                    }
                    toast(getString(R.string.lugar_eliminado))
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                .onFailure { e ->
                    bloquear(false)
                    toast(getString(R.string.lugar_error_guardar, motivoFirestore(e)))
                }
        }
    }

    private fun bloquear(bloquear: Boolean) {
        btnGuardar.isEnabled = !bloquear
        btnGuardar.setText(
            if (bloquear) R.string.lugar_btn_guardando else R.string.lugar_btn_guardar
        )
        btnEliminar.isEnabled = !bloquear
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        /** Si se pasa, se edita ese lugar; si no, se usa el enlazado al perfil. */
        const val EXTRA_WORKPLACE_ID = "com.proyecto.chambaya.extra_workplace_id"

        /** Para `startActivityForResult`: recargar el lugar al volver. */
        const val REQUEST_LUGAR = 7401
    }
}
