package com.proyecto.chambaya.ui.publish

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.OficioCatalog
import com.proyecto.chambaya.data.model.PaymentPeriod
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.model.PublicationDraft
import com.proyecto.chambaya.data.model.PublicationImage
import com.proyecto.chambaya.data.model.validatePublicationDraft
import com.proyecto.chambaya.data.remote.CloudinaryUploader
import com.proyecto.chambaya.data.remote.PhotoUploadResult
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.launch

/**
 * FASE 5 — Crear (y editar) publicación.
 *
 * El publicador (nombre, @usuario, foto, verificación, sector) se carga
 * automáticamente desde users/{uid}: el formulario no lo pide (FASE 5.2).
 * Fotos (máx. 3) a Cloudinary fotos-publicaciones/{uid}/{publicationId}.
 */
class CrearPublicacionActivity : AppCompatActivity() {

    private val pubRepo = PublicationRepository()
    private val profileRepo = ProfileRepository()
    private val placeRepo = WorkplaceRepository()
    private val uploader = CloudinaryUploader()

    private val fotosUris = mutableListOf<Uri>()
    private var editando: Publication? = null
    private var categoriaElegida = ""
    private var publicando = false

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@registerForActivityResult
        if (fotosUris.size >= 3) {
            Toast.makeText(this, "Máximo 3 fotos.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        fotosUris += uri
        pintarFotos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_publicacion)

        val editId = intent.getStringExtra(EXTRA_EDIT_ID).orEmpty()
        findViewById<View>(R.id.btnCerrar).setOnClickListener { finish() }
        configurarCampos()

        if (editId.isNotBlank()) {
            findViewById<TextView>(R.id.tvWorkplaceChip)?.text = "Editando"
            (findViewById<View>(R.id.btnPublicar) as? MaterialButton)?.text = "Guardar cambios"
            findViewById<TextView>(android.R.id.title)?.text = "Editar chamba"
            cargarParaEditar(editId)
        } else {
            cargarContexto()
        }

        findViewById<View>(R.id.cardAddPhoto).setOnClickListener {
            if (fotosUris.size >= 3) Toast.makeText(this, "Máximo 3 fotos.", Toast.LENGTH_SHORT).show()
            else pickPhoto.launch("image/*")
        }
        findViewById<View>(R.id.btnPublicar).setOnClickListener { publicar() }
    }

    private fun configurarCampos() {
        findViewById<TextInputEditText>(R.id.etDescripcion)?.doAfterTextChanged {
            findViewById<TextView>(R.id.tvDescCount)?.text = "${it?.length ?: 0} / 2000"
        }
        val periodos = mapOf("Hora" to PaymentPeriod.HOUR, "Día" to PaymentPeriod.DAY, "Semana" to PaymentPeriod.WEEK, "Mes" to PaymentPeriod.MONTH, "Por trabajo" to PaymentPeriod.JOB)
        findViewById<MaterialAutoCompleteTextView>(R.id.actvPeriodo)?.apply {
            setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, periodos.keys.toList()))
            setOnClickListener { showDropDown() }
            tag = periodos
        }
        // Categorías desde el catálogo de oficios.
        val cats = OficioCatalog.load(this).map { it.categoria }.distinct().take(14)
        val group = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCategoria)
        cats.forEach { cat ->
            val chip = Chip(this).apply { text = cat; isCheckable = true; tag = cat }
            chip.setOnCheckedChangeListener { _, checked -> if (checked) categoriaElegida = cat }
            group?.addView(chip)
        }
        // Distritos frecuentes de Ayacucho.
        val distritos = listOf("Ayacucho", "Carmen Alto", "San Juan Bautista", "Jesús Nazareno", "Andrés Avelino Cáceres", "Magdalena", "Huanta", "Tambo")
        findViewById<MaterialAutoCompleteTextView>(R.id.actvDistrito)?.apply {
            setAdapter(ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, distritos))
            setOnClickListener { showDropDown() }
        }
    }

    private fun periodoElegido(): String {
        val label = findViewById<MaterialAutoCompleteTextView>(R.id.actvPeriodo)?.text?.toString().orEmpty()
        return when (label) {
            "Hora" -> PaymentPeriod.HOUR
            "Semana" -> PaymentPeriod.WEEK
            "Mes" -> PaymentPeriod.MONTH
            "Por trabajo" -> PaymentPeriod.JOB
            else -> PaymentPeriod.DAY
        }
    }

    private fun cargarContexto() {
        lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val lugar = placeRepo.loadByOwner(uid).getOrNull()
            if (!isFinishing) {
                findViewById<TextView>(R.id.tvWorkplaceChip)?.text =
                    lugar?.name?.take(20) ?: "Sin lugar"
            }
        }
    }

    private fun cargarParaEditar(publicationId: String) {
        lifecycleScope.launch {
            val pub = pubRepo.getById(publicationId).getOrNull()
            if (pub == null) {
                Toast.makeText(this@CrearPublicacionActivity, "La publicación ya no existe.", Toast.LENGTH_SHORT).show()
                finish(); return@launch
            }
            editando = pub
            categoriaElegida = pub.category
            findViewById<TextInputEditText>(R.id.etTitulo)?.setText(pub.title)
            findViewById<TextInputEditText>(R.id.etDescripcion)?.setText(pub.description)
            findViewById<TextInputEditText>(R.id.etMonto)?.setText(if (pub.payment.amount % 1.0 == 0.0) pub.payment.amount.toInt().toString() else pub.payment.amount.toString())
            findViewById<MaterialAutoCompleteTextView>(R.id.actvPeriodo)?.setText(
                when (pub.payment.period) {
                    PaymentPeriod.HOUR -> "Hora"; PaymentPeriod.WEEK -> "Semana"
                    PaymentPeriod.MONTH -> "Mes"; PaymentPeriod.JOB -> "Por trabajo"
                    else -> "Día"
                }, false
            )
            findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNegociable)?.isChecked = pub.payment.negotiable
            findViewById<TextInputEditText>(R.id.etVacantes)?.setText(pub.workersNeeded.toString())
            findViewById<MaterialAutoCompleteTextView>(R.id.actvDistrito)?.setText(pub.location.district, false)
            findViewById<TextInputEditText>(R.id.etHoraInicio)?.setText(pub.schedule.startTime)
            findViewById<TextInputEditText>(R.id.etHoraFin)?.setText(pub.schedule.endTime)
            findViewById<TextInputEditText>(R.id.etHabilidades)?.setText(pub.skillsRequired.joinToString(", "))
            // Marca la categoría.
            val group = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCategoria)
            for (i in 0 until (group?.childCount ?: 0)) {
                val chip = group?.getChildAt(i) as? Chip ?: continue
                if ((chip.tag as? String) == pub.category) { chip.isChecked = true; break }
            }
        }
    }

    private fun armarDraft(): PublicationDraft {
        val habilidades = findViewById<TextInputEditText>(R.id.etHabilidades)?.text?.toString().orEmpty()
            .split(',').map { it.trim() }.filter { it.isNotEmpty() }.take(8)
        return PublicationDraft(
            title = findViewById<TextInputEditText>(R.id.etTitulo)?.text?.toString().orEmpty(),
            description = findViewById<TextInputEditText>(R.id.etDescripcion)?.text?.toString().orEmpty(),
            category = categoriaElegida,
            skillsRequired = habilidades,
            amount = findViewById<TextInputEditText>(R.id.etMonto)?.text?.toString()?.toDoubleOrNull() ?: 0.0,
            period = periodoElegido(),
            negotiable = findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNegociable)?.isChecked == true,
            workersNeeded = findViewById<TextInputEditText>(R.id.etVacantes)?.text?.toString()?.toIntOrNull() ?: 1,
            district = findViewById<MaterialAutoCompleteTextView>(R.id.actvDistrito)?.text?.toString().orEmpty(),
            startTime = findViewById<TextInputEditText>(R.id.etHoraInicio)?.text?.toString().orEmpty().trim(),
            endTime = findViewById<TextInputEditText>(R.id.etHoraFin)?.text?.toString().orEmpty().trim()
        )
    }

    private fun publicar() {
        if (publicando) return
        val draft = armarDraft()
        val errores = validatePublicationDraft(draft)
        val tvError = findViewById<TextView>(R.id.tvFormError)
        if (errores.isNotEmpty()) {
            tvError.visibility = View.VISIBLE
            tvError.text = errores.first()
            return
        }
        tvError.visibility = View.GONE
        publicando = true
        (findViewById<View>(R.id.btnPublicar) as? MaterialButton)?.apply { isEnabled = false; text = "Publicando…" }

        lifecycleScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                require(uid.isNotBlank()) { "Sesión no válida." }
                var perfil = ProfileCache.perfil
                if (perfil == null) {
                    perfil = profileRepo.loadProfile(uid).getOrNull()
                    if (perfil != null) ProfileCache.perfil = perfil
                }
                require(perfil != null) { "No se pudo cargar tu perfil." }
                val lugar = placeRepo.loadByOwner(uid).getOrNull()

                val existente = editando
                if (existente != null) {
                    // Edición: conserva imágenes salvo que se agreguen nuevas.
                    val nuevas = subirFotos(uid, existente.publicationId)
                    val imagenes = if (nuevas.isEmpty()) null else {
                        val actuales = existente.images.take(3 - nuevas.size)
                        actuales + nuevas
                    }
                    val r = pubRepo.update(uid, existente.publicationId, draft, imagenes)
                    if (r.isFailure) throw r.exceptionOrNull() ?: Exception("No se pudo guardar.")
                    Toast.makeText(this@CrearPublicacionActivity, "Cambios guardados.", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                    return@launch
                }

                val publicationId = pubRepo.newPublicationId()
                val imagenes = subirFotos(uid, publicationId)
                val r = pubRepo.create(
                    uid = uid,
                    publicationId = publicationId,
                    draft = draft,
                    perfil = perfil,
                    workplaceId = lugar?.workplaceId.orEmpty(),
                    workplaceName = lugar?.name.orEmpty(),
                    workplacePhotoUrl = lugar?.photoUrl.orEmpty(),
                    images = imagenes
                )
                if (r.isFailure) throw r.exceptionOrNull() ?: Exception("No se pudo publicar.")
                // Caché en caliente: el servidor ya sumó +1 a los contadores.
                ProfileCache.perfil = perfil.copy(
                    employer = perfil.employer.copy(
                        publishedCount = perfil.employer.publishedCount + 1
                    ),
                    statistics = perfil.statistics.copy(
                        publicationsCount = perfil.statistics.publicationsCount + 1
                    )
                )
                Toast.makeText(this@CrearPublicacionActivity, "¡Chamba publicada!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: Exception) {
                tvError.visibility = View.VISIBLE
                tvError.text = e.message ?: "No se pudo publicar. Inténtalo de nuevo."
            } finally {
                publicando = false
                (findViewById<View>(R.id.btnPublicar) as? MaterialButton)?.apply { isEnabled = true; text = if (editando != null) "Guardar cambios" else "Publicar chamba" }
            }
        }
    }

    private suspend fun subirFotos(uid: String, publicationId: String): List<PublicationImage> {
        val resultado = mutableListOf<PublicationImage>()
        for (uri in fotosUris.toList()) {
            when (val r = uploader.uploadPublicationPhoto(this, uid, publicationId, uri)) {
                is PhotoUploadResult.Success -> resultado += PublicationImage(r.image.url, r.image.publicId)
                is PhotoUploadResult.Rejected -> throw IllegalArgumentException(r.message)
                is PhotoUploadResult.NetworkError -> throw IllegalArgumentException("Sin conexión para subir fotos.")
            }
        }
        return resultado
    }

    private fun pintarFotos() {
        val cont = findViewById<LinearLayout>(R.id.containerFotos) ?: return
        cont.removeAllViews()
        fotosUris.forEachIndexed { index, uri ->
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(288, 288).apply { marginEnd = 24 }
                radius = 42f
            }
            val img = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            img.load(uri)
            img.setOnClickListener {
                fotosUris.removeAt(index)
                pintarFotos()
                Toast.makeText(this, "Foto quitada.", Toast.LENGTH_SHORT).show()
            }
            card.addView(img)
            cont.addView(card)
        }
    }

    companion object {
        const val EXTRA_EDIT_ID = "edit_publication_id"
        fun editarIntent(activity: Activity, publicationId: String): Intent {
            return Intent(activity, CrearPublicacionActivity::class.java)
                .putExtra(EXTRA_EDIT_ID, publicationId)
        }
    }
}
