package com.proyecto.chambaya.ui.publish

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.EditarPerfilActivity
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.ApplicationStatus
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobApplication
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.model.PublicationStatus
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.puedeContratarOPublicar
import com.proyecto.chambaya.data.repository.ApplicationRepository
import com.proyecto.chambaya.data.repository.JobRepository
import com.proyecto.chambaya.data.repository.NotificationRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.data.repository.RatingRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import com.proyecto.chambaya.ui.jobs.ApplySheet
import com.proyecto.chambaya.ui.jobs.JobDetailSheet
import com.proyecto.chambaya.ui.jobs.PublicProfileSheet
import com.proyecto.chambaya.ui.jobs.RateSheet
import com.proyecto.chambaya.ui.profile.ProfileCache
import com.proyecto.chambaya.ui.workplace.EditarLugarActivity
import kotlinx.coroutines.launch

/**
 * Pantalla "Publicar" con tres secciones:
 *   0 · Publicar       1 · Mis publicaciones       2 · Solicitudes
 *
 * FASE 5:
 *  - Publicar → abre CrearPublicacionActivity (contratante listo)
 *  - Mis publicaciones → lista real con pausar/reactivar/finalizar/editar/eliminar
 *  - Solicitudes → estado informativo (las postulaciones llegan en Fase 7)
 */
class FragmentoPublicar : Fragment() {

    private var seccionActual = SECCION_PUBLICAR

    private lateinit var tabs: List<View>
    private lateinit var iconos: List<ImageView>
    private lateinit var paneles: List<View>
    private lateinit var barraTabs: View
    private lateinit var indicador: View

    private var colorActivo = 0
    private var colorInactivo = 0
    private var anchoBarraPrevio = -1

    // ── FASE 4 · Estado de la puerta de publicación ───────────────
    private val lugarRepository = WorkplaceRepository()
    private val perfilRepository = ProfileRepository()
    private val pubRepository = PublicationRepository()
    private val appRepository = ApplicationRepository()
    private val jobRepository = JobRepository()
    private val ratingRepository = RatingRepository()
    private val notificationRepository = NotificationRepository()
    private var esContratante = false
    private var tieneLugar = false
    private var bloqueadoPorCompletitud = false
    private var cargandoEstado = false

    private lateinit var emptyLugar: View
    private var rvMis: RecyclerView? = null
    private var progressMis: ProgressBar? = null
    private var emptyMis: View? = null
    private var misAdapter: MisPublicacionesAdapter? = null
    private var misListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var conteoSincronizado = false

    // ── FASE 7/8/9 · Solicitudes ──────────────────────────────────
    private var rvSolicitudes: RecyclerView? = null
    private var progressSolicitudes: ProgressBar? = null
    private var emptySolicitudes: View? = null
    private var applicantsAdapter: ApplicantsAdapter? = null
    private var reqFilter = 0 // 0 todas · 1 pendientes · 2 decididas
    private var employerApps: List<JobApplication> = emptyList()
    private var jobsCache: Map<String, Job> = emptyMap() // applicationId -> Job
    private var ratedCache: Set<String> = emptySet() // jobIds ya calificados por mí
    private var solicitudesLoading = false
    private val lugarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) recargarEstado()
    }

    private val publicarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            recargarEstado()
            cargarMisPublicaciones()
            seleccionar(SECCION_PUBLICACIONES, animar = true)
            Toast.makeText(requireContext(), getString(R.string.k_pub_exito), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragmento_publicar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seccionActual = savedInstanceState?.getInt(KEY_SECCION, SECCION_PUBLICAR) ?: SECCION_PUBLICAR
        colorActivo = requireContext().getColor(R.color.profile_tab_active)
        colorInactivo = requireContext().getColor(R.color.profile_tab_inactive)

        barraTabs = view.findViewById(R.id.barraTabs)
        indicador = view.findViewById(R.id.indicadorTab)
        ViewCompat.setOnApplyWindowInsetsListener(barraTabs) { v, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = statusBar.top)
            insets
        }
        ViewCompat.requestApplyInsets(barraTabs)
        tabs = listOf(
            view.findViewById(R.id.tabPublicar),
            view.findViewById(R.id.tabPublicaciones),
            view.findViewById(R.id.tabSolicitudes)
        )
        iconos = listOf(
            view.findViewById(R.id.iconoPublicar),
            view.findViewById(R.id.iconoPublicaciones),
            view.findViewById(R.id.iconoSolicitudes)
        )
        paneles = listOf(
            view.findViewById(R.id.panelPublicar),
            view.findViewById(R.id.panelPublicaciones),
            view.findViewById(R.id.panelSolicitudes)
        )

        enlazarVacio(view)
        configurarPaneles()
        configurarMis(view)
        configurarSolicitudes(view)

        tabs.forEachIndexed { indice, tab ->
            tab.setOnClickListener { seleccionar(indice, animar = true) }
        }
        view.findViewById<ContenedorDeslizable>(R.id.contenedorSecciones).alDeslizar = { dir ->
            seleccionar(seccionActual + dir, animar = true)
        }

        aplicarEstado(animar = false)
        barraTabs.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val ancho = right - left
            if (ancho != anchoBarraPrevio) {
                anchoBarraPrevio = ancho
                moverIndicador(animar = false)
            }
        }

        parentFragmentManager.setFragmentResultListener(JobDetailSheet.REQUEST_CHANGED, viewLifecycleOwner) { _, _ ->
            cargarMisPublicaciones()
        }
        parentFragmentManager.setFragmentResultListener(RateSheet.REQUEST_RATED, viewLifecycleOwner) { _, _ ->
            cargarSolicitudes()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SECCION, seccionActual)
    }

    override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.white))
        // Solo recarga el estado: al terminar, él mismo engancha
        // Mis publicaciones (ver recargarEstado).
        if (::barraTabs.isInitialized) {
            recargarEstado()
        }
    }

    override fun onPause() {
        detachMis()
        super.onPause()
    }

    override fun onDestroyView() {
        detachMis()
        conteoSincronizado = false
        rvMis = null
        progressMis = null
        emptyMis = null
        misAdapter = null
        rvSolicitudes = null
        progressSolicitudes = null
        emptySolicitudes = null
        applicantsAdapter = null
        super.onDestroyView()
    }

    // ── Puerta de publicación ────────────────────────────────────

    private fun enlazarVacio(root: View) {
        emptyLugar = root.findViewById(R.id.panelPublicar)
    }

    private fun recargarEstado() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (cargandoEstado) return
        cargandoEstado = true
        viewLifecycleOwner.lifecycleScope.launch {
            val perfil = perfilRepository.loadProfile(uid).getOrNull()
            if (perfil != null) ProfileCache.perfil = perfil
            esContratante = perfil?.roles?.contains(UserRoles.CONTRATANTE) == true &&
                perfil?.employer?.enabled == true
            bloqueadoPorCompletitud = esContratante &&
                perfil?.puedeContratarOPublicar() != true
            val porcentaje = perfil?.completion()?.percent ?: 0
            tieneLugar = if (esContratante && !bloqueadoPorCompletitud) {
                lugarRepository.loadByOwner(uid)
                    .onFailure { android.util.Log.w("Publicar", "No se pudo leer el lugar del contratante", it) }
                    .getOrNull() != null
            } else false
            if (!isAdded) {
                cargandoEstado = false
                return@launch
            }
            when {
                bloqueadoPorCompletitud -> pintarEstadoVacio(
                    titulo = getString(R.string.publicar_bloqueado_titulo),
                    subtitulo = getString(R.string.publicar_bloqueado_sub, porcentaje),
                    boton = getString(R.string.publicar_bloqueado_btn)
                )
                esContratante && !tieneLugar -> pintarEstadoVacio(
                    titulo = getString(R.string.lugar_sin_lugar_titulo),
                    subtitulo = getString(R.string.lugar_sin_lugar_sub),
                    boton = getString(R.string.lugar_sin_lugar_btn)
                )
                esContratante -> pintarEstadoVacio(
                    titulo = "Publica una nueva chamba",
                    subtitulo = "Describe el trabajo, indica el pago y recibe postulaciones.",
                    boton = "Crear publicación"
                )
                else -> pintarEstadoVacio(
                    titulo = getString(R.string.publicar_nueva_titulo),
                    subtitulo = getString(R.string.publicar_nueva_sub),
                    boton = getString(R.string.publicar_nueva_btn)
                )
            }
            // Solicitudes: texto honesto según rol.
            pintarSolicitudes()
            cargandoEstado = false
            // Mis publicaciones se engancha AQUÍ (con los flags ya resueltos),
            // no en onResume: en arranque en frío onResume corre antes de que
            // termine esta carga y `esContratante` aún es false, lo que dejaba
            // la tab en blanco hasta navegar y volver.
            cargarMisPublicaciones()
        }
    }

    private fun pintarEstadoVacio(titulo: String, subtitulo: String, boton: String) {
        if (!isAdded) return
        emptyLugar.findViewById<TextView>(R.id.tvVacioTitulo).text = titulo
        emptyLugar.findViewById<TextView>(R.id.tvVacioSubtitulo).text = subtitulo
        emptyLugar.findViewById<MaterialButton>(R.id.btnVacioAccion).apply {
            text = boton
            setOnClickListener { abrirAccionPrincipal() }
        }
    }

    /** Un solo CTA que se adapta al estado: completar, registrar o publicar. */
    private fun abrirAccionPrincipal() {
        if (bloqueadoPorCompletitud) {
            startActivity(
                Intent(requireContext(), EditarPerfilActivity::class.java)
                    .putExtra(EditarPerfilActivity.EXTRA_START_STEP, EditarPerfilActivity.PASO_INICIO_COMPLETAR)
            )
            return
        }
        if (esContratante && !tieneLugar) {
            lugarLauncher.launch(Intent(requireContext(), EditarLugarActivity::class.java))
            return
        }
        if (esContratante) {
            publicarLauncher.launch(Intent(requireContext(), CrearPublicacionActivity::class.java))
            return
        }
        // Rol fijo desde el registro: un trabajador no puede volverse
        // contratante (esta pantalla ni siquiera aparece en su menú).
        Toast.makeText(
            requireContext(),
            "Solo las cuentas contratante pueden publicar chambas.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // ── FASE 5 · Mis publicaciones ────────────────────────────────

    private fun configurarMis(view: View) {
        rvMis = view.findViewById(R.id.rvMisPublicaciones)
        progressMis = view.findViewById(R.id.progressMis)
        emptyMis = view.findViewById(R.id.emptyMis)
        misAdapter = MisPublicacionesAdapter(
            onVer = { pub ->
                JobDetailSheet.newInstance(pub.publicationId).show(parentFragmentManager, "detail")
            },
            onToggle = { pub -> alternarEstado(pub) },
            onMenu = { pub, anchor -> mostrarMenu(pub, anchor) }
        )
        rvMis?.adapter = misAdapter
        // Oculto hasta el primer snapshot: evita el flash de lista vacía en
        // arranque en frío.
        rvMis?.visibility = View.GONE
        emptyMis?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
            text = getString(R.string.publicaciones_vacio_btn)
            setOnClickListener { abrirAccionPrincipal() }
        }
    }

    private fun cargarMisPublicaciones() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!esContratante) {
            detachMis()
            rvMis?.visibility = View.GONE
            progressMis?.visibility = View.GONE
            emptyMis?.visibility = View.GONE
            return
        }
        // Ya enganchado: el snapshot en vivo trae los cambios solo.
        if (misListener != null) return
        progressMis?.visibility = View.VISIBLE
        rvMis?.visibility = View.GONE
        emptyMis?.visibility = View.GONE
        misListener = pubRepository.listenByOwner(
            uid, 50,
            onUpdate = { lista ->
                if (!isAdded) return@listenByOwner
                pintarMis(lista)
            },
            onError = {
                if (!isAdded) return@listenByOwner
                progressMis?.visibility = View.GONE
                if (misAdapter?.itemCount == 0) emptyMis?.visibility = View.VISIBLE
            }
        )
    }

    private fun pintarMis(lista: List<Publication>) {
        progressMis?.visibility = View.GONE
        if (lista.isEmpty()) {
            rvMis?.visibility = View.GONE
            emptyMis?.visibility = View.VISIBLE
            emptyMis?.findViewById<TextView>(R.id.tvVacioTitulo)?.setText(R.string.publicaciones_vacio_titulo)
            emptyMis?.findViewById<TextView>(R.id.tvVacioSubtitulo)?.setText(R.string.publicaciones_vacio_sub)
        } else {
            emptyMis?.visibility = View.GONE
            rvMis?.visibility = View.VISIBLE
            misAdapter?.submitList(lista)
        }
        // Repara una sola vez por vista los contadores denormalizados
        // (quedaron en 0 cuando las reglas aún no permitían el ±1).
        if (!conteoSincronizado && esContratante) {
            conteoSincronizado = true
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val total = lista.size
            viewLifecycleOwner.lifecycleScope.launch {
                pubRepository.syncPublishedCount(uid, total)
                ProfileCache.perfil?.let { p ->
                    if (p.employer.publishedCount != total) {
                        ProfileCache.perfil = p.copy(
                            employer = p.employer.copy(publishedCount = total),
                            statistics = p.statistics.copy(publicationsCount = total)
                        )
                    }
                }
            }
        }
    }

    private fun detachMis() {
        misListener?.remove()
        misListener = null
    }

    private fun alternarEstado(pub: Publication) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val nuevo = when (pub.status) {
            PublicationStatus.ACTIVE -> PublicationStatus.PAUSED
            PublicationStatus.PAUSED -> PublicationStatus.ACTIVE
            PublicationStatus.FINISHED -> PublicationStatus.ACTIVE
            else -> PublicationStatus.ACTIVE
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val r = pubRepository.changeStatus(requireContext(), uid, pub.publicationId, nuevo)
            if (r.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    when (nuevo) {
                        PublicationStatus.PAUSED -> getString(R.string.k_detalle_pausada)
                        PublicationStatus.ACTIVE -> getString(R.string.k_pub_activa_nueva)
                        else -> getString(R.string.k_pub_estado_ok)
                    },
                    Toast.LENGTH_SHORT
                ).show()
                cargarMisPublicaciones()
            } else {
                Toast.makeText(requireContext(), r.exceptionOrNull()?.message ?: getString(R.string.k_comun_no_actualizar), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarMenu(pub: Publication, anchor: View) {
        val menu = PopupMenu(requireContext(), anchor)
        menu.menu.add(0, 1, 0, getString(R.string.k_comun_editar))
        if (pub.status == PublicationStatus.ACTIVE) menu.menu.add(0, 2, 0, getString(R.string.item_pausar))
        if (pub.status == PublicationStatus.PAUSED) menu.menu.add(0, 3, 0, getString(R.string.k_detalle_reactivar))
        if (pub.status == PublicationStatus.FINISHED || pub.status == PublicationStatus.ARCHIVED) {
            menu.menu.add(0, 3, 0, getString(R.string.k_pub_republicar))
        }
        if (pub.status != PublicationStatus.FINISHED) menu.menu.add(0, 4, 0, getString(R.string.sheet_detalle_finalizar))
        if (pub.status != PublicationStatus.ARCHIVED) menu.menu.add(0, 6, 0, getString(R.string.k_pub_archivar))
        menu.menu.add(0, 5, 0, getString(R.string.k_comun_eliminar))
        menu.setOnMenuItemClickListener { item ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnMenuItemClickListener false
            when (item.itemId) {
                1 -> {
                    if (pub.status !in listOf(PublicationStatus.ACTIVE, PublicationStatus.PAUSED)) {
                        Toast.makeText(requireContext(), getString(R.string.k_pub_solo_edit), Toast.LENGTH_SHORT).show()
                    } else {
                        publicarLauncher.launch(CrearPublicacionActivity.editarIntent(requireActivity(), pub.publicationId))
                    }
                    true
                }
                2, 3 -> { alternarEstado(pub); true }
                4 -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val r = pubRepository.changeStatus(requireContext(), uid, pub.publicationId, PublicationStatus.FINISHED)
                        if (r.isSuccess) {
                            Toast.makeText(requireContext(), getString(R.string.k_detalle_finalizada), Toast.LENGTH_SHORT).show()
                            cargarMisPublicaciones()
                        }
                    }
                    true
                }
                5 -> { confirmarEliminar(pub); true }
                6 -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val r = pubRepository.changeStatus(requireContext(), uid, pub.publicationId, PublicationStatus.ARCHIVED)
                        if (r.isSuccess) {
                            Toast.makeText(requireContext(), getString(R.string.k_pub_archivada), Toast.LENGTH_SHORT).show()
                            cargarMisPublicaciones()
                        }
                    }
                    true
                }
                else -> false
            }
        }
        menu.show()
    }

    private fun confirmarEliminar(pub: Publication) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.k_pub_eliminar_titulo)
            .setMessage(R.string.k_pub_eliminar_msg)
            .setPositiveButton(R.string.k_comun_eliminar) { _, _ ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = pubRepository.delete(requireContext(), uid, pub.publicationId)
                    if (r.isSuccess) {
                        // Caché en caliente: el servidor ya restó 1.
                        ProfileCache.perfil?.let { p ->
                            ProfileCache.perfil = p.copy(
                                employer = p.employer.copy(
                                    publishedCount = (p.employer.publishedCount - 1).coerceAtLeast(0)
                                ),
                                statistics = p.statistics.copy(
                                    publicationsCount = (p.statistics.publicationsCount - 1).coerceAtLeast(0)
                                )
                            )
                        }
                        Toast.makeText(requireContext(), getString(R.string.k_pub_eliminada), Toast.LENGTH_SHORT).show()
                        cargarMisPublicaciones()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.k_comun_no_eliminar), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
            .show()
    }

    // ── Solicitudes (Fase 7: placeholder honesto) ─────────────────

    private fun configurarSolicitudes(view: View) {
        rvSolicitudes = view.findViewById(R.id.rvSolicitudes)
        progressSolicitudes = view.findViewById(R.id.progressSolicitudes)
        emptySolicitudes = view.findViewById(R.id.emptySolicitudes)

        applicantsAdapter = ApplicantsAdapter(
            onAccept = { row -> confirmarDecision(row, accept = true) },
            onReject = { row -> confirmarDecision(row, accept = false) },
            onJobAction = { row -> avanzarTrabajo(row) },
            onRate = { row ->
                row.job?.let { RateSheet.newInstance(it.jobId).show(parentFragmentManager, "rate") }
            },
            onOpenDetail = { row ->
                JobDetailSheet.newInstance(row.app.publicationId).show(parentFragmentManager, "detail")
            },
            onOpenProfile = { row ->
                PublicProfileSheet.newInstance(row.app.workerUid).show(parentFragmentManager, "profile")
            },
            onChat = { row ->
                abrirChat(row.app.workerUid, row.app.publicationId, row.app.publicationTitle)
            }
        )

        val chipAll = view.findViewById<TextView>(R.id.chipReqAll)
        val chipPending = view.findViewById<TextView>(R.id.chipReqPending)
        val chipDecided = view.findViewById<TextView>(R.id.chipReqDecided)
        fun activar(cual: Int) {
            reqFilter = cual
            listOf(chipAll, chipPending, chipDecided).forEachIndexed { i, chip ->
                val on = i == cual
                chip.setBackgroundResource(if (on) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
                chip.setTextColor(requireContext().getColor(if (on) R.color.white else R.color.text_primary))
            }
            pintarListaSolicitudes()
        }
        chipAll.setOnClickListener { activar(0) }
        chipPending.setOnClickListener { activar(1) }
        chipDecided.setOnClickListener { activar(2) }

        emptySolicitudes?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.setOnClickListener {
            seleccionar(SECCION_PUBLICAR, animar = true)
        }
    }

    /** Solicitudes recibidas (esta pantalla es solo del contratante). */
    private fun pintarSolicitudes() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank() || !isAdded) return
        rvSolicitudes?.adapter = applicantsAdapter
        if (esContratante) cargarSolicitudes() else mostrarAvisoSoloContratante()
    }

    /** Estado defensivo: sin rol contratante no hay nada que gestionar aquí. */
    private fun mostrarAvisoSoloContratante() {
        progressSolicitudes?.visibility = View.GONE
        rvSolicitudes?.visibility = View.GONE
        emptySolicitudes?.visibility = View.VISIBLE
        emptySolicitudes?.findViewById<TextView>(R.id.tvVacioTitulo)?.text = getString(R.string.k_pub_solo_contratantes)
        emptySolicitudes?.findViewById<TextView>(R.id.tvVacioSubtitulo)?.text =
            "Esta sección es para gestionar tus publicaciones y postulantes."
        emptySolicitudes?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.text = getString(R.string.k_pub_ver_pubs)
    }

    private fun filtrarApps(apps: List<JobApplication>): List<JobApplication> = when (reqFilter) {
        1 -> apps.filter { it.status == ApplicationStatus.PENDING }
        2 -> apps.filter { it.status != ApplicationStatus.PENDING }
        else -> apps
    }

    private fun cargarSolicitudes() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (solicitudesLoading || !isAdded || !esContratante) return
        solicitudesLoading = true
        progressSolicitudes?.visibility = View.VISIBLE
        rvSolicitudes?.visibility = View.GONE
        emptySolicitudes?.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            employerApps = appRepository.listByEmployer(uid).getOrNull().orEmpty()
            val acceptedIds = employerApps
                .filter { it.status == ApplicationStatus.ACCEPTED }
                .map { it.applicationId }
            jobsCache = if (acceptedIds.isEmpty()) emptyMap()
            else jobRepository.findByApplicationIds(acceptedIds).getOrNull().orEmpty()
            val completedIds = jobsCache.values
                .filter { it.status == JobStatus.COMPLETED }
                .map { it.jobId }
            ratedCache = if (completedIds.isEmpty()) emptySet()
            else ratingRepository.ratedJobIds(completedIds, uid).getOrNull().orEmpty()
            if (!isAdded) {
                solicitudesLoading = false
                return@launch
            }
            solicitudesLoading = false
            pintarListaSolicitudes()
        }
    }

    private fun pintarListaSolicitudes() {
        if (!isAdded) return
        progressSolicitudes?.visibility = View.GONE
        val filtradas = filtrarApps(employerApps)
        if (filtradas.isEmpty()) {
            rvSolicitudes?.visibility = View.GONE
            emptySolicitudes?.visibility = View.VISIBLE
            emptySolicitudes?.findViewById<TextView>(R.id.tvVacioTitulo)?.text = getString(R.string.k_pub_solicitudes)
            emptySolicitudes?.findViewById<TextView>(R.id.tvVacioSubtitulo)?.text =
                "Cuando un especialista se postule, verás aquí su perfil y podrás aceptar o rechazar."
            emptySolicitudes?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.text = getString(R.string.k_pub_cta)
        } else {
            emptySolicitudes?.visibility = View.GONE
            rvSolicitudes?.visibility = View.VISIBLE
            val items = mutableListOf<RequestItem>()
            filtradas.groupBy { it.publicationTitle.ifBlank { "Chamba" } }
                .forEach { (titulo, apps) ->
                    items += RequestItem.Header(
                        titulo,
                        apps.count { it.status == ApplicationStatus.PENDING },
                        apps.size
                    )
                    apps.forEach { app ->
                        val job = jobsCache[app.applicationId]
                        items += RequestItem.Row(
                            ApplicantRow(
                                app = app,
                                job = job,
                                ratedByMe = job?.jobId in ratedCache
                            )
                        )
                    }
                }
            applicantsAdapter?.submitList(items)
        }
    }

    /** Abre (o crea) el chat 1:1 con el otro participante. */
    private fun abrirChat(otherUid: String, publicationId: String, publicationTitle: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (me.isBlank() || otherUid.isBlank() || me == otherUid) return
        viewLifecycleOwner.lifecycleScope.launch {
            val conv = com.proyecto.chambaya.data.repository.ChatRepository()
                .ensureConversation(
                    me,
                    otherUid,
                    publicationId,
                    publicationTitle,
                    com.proyecto.chambaya.data.repository.ChatRepository.hiddenConversationIds(requireContext(), me)
                ).getOrNull()
            if (!isAdded) return@launch
            if (conv == null) {
                Toast.makeText(requireContext(), getString(R.string.k_chat_no_abrir), Toast.LENGTH_LONG).show()
                return@launch
            }
            var nombre = "Chat"
            var foto = ""
            profileCachePublic(otherUid)?.let {
                nombre = it.displayName()
                foto = it.photoUrl
            }
            val intent = android.content.Intent(
                requireContext(),
                com.proyecto.chambaya.ui.chat.ActividadChatDetalle::class.java
            ).apply {
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_CONV_ID, conv.conversationId)
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_OTHER_UID, otherUid)
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_NOMBRE, nombre)
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_FOTO, foto)
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_PUB_TITULO, publicationTitle)
            }
            startActivity(intent)
        }
    }

    private suspend fun profileCachePublic(uid: String): com.proyecto.chambaya.data.model.PublicProfile? {
        return perfilRepository.loadPublicProfile(uid).getOrNull()
    }

    private fun confirmarDecision(row: ApplicantRow, accept: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val nombre = row.app.worker.name.ifBlank { getString(R.string.k_pub_especialista) }
        AlertDialog.Builder(requireContext())
            .setTitle(if (accept) getString(R.string.k_pub_aceptar_titulo) else getString(R.string.k_pub_rechazar_titulo))
            .setMessage(
                if (accept) getString(R.string.k_pub_aceptar_msg_fmt, nombre)
                else getString(R.string.k_pub_rechazar_msg_fmt, nombre)
            )
            .setPositiveButton(if (accept) getString(R.string.item_aceptar) else getString(R.string.item_rechazar)) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = appRepository.decide(requireContext(), uid, row.app.applicationId, accept)
                    if (!isAdded) return@launch
                    if (r.isSuccess) {
                        val msg = if (accept) {
                            if (r.getOrNull()?.publicationFilled == true)
                                getString(R.string.k_pub_contratado_lleno)
                            else getString(R.string.k_pub_contratado_ok)
                        } else getString(R.string.k_pub_rechazada)
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        cargarSolicitudes()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            r.exceptionOrNull()?.message ?: getString(R.string.k_pub_no_decidir),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
            .show()
    }

    private fun avanzarTrabajo(row: ApplicantRow) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val job = row.job ?: return
        val next = JobStatus.nextFrom(job.status).firstOrNull { it != JobStatus.CANCELLED } ?: return
        val verbo = if (next == JobStatus.IN_PROGRESS) getString(R.string.k_pub_verbo_iniciar) else getString(R.string.k_pub_verbo_completar)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.k_pub_trabajo_titulo)
            .setMessage(getString(R.string.k_pub_verbo_fmt, verbo, row.app.worker.name.ifBlank { getString(R.string.k_pub_especialista) }))
            .setPositiveButton(R.string.k_comun_si) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = jobRepository.transition(requireContext(), uid, job.jobId, next)
                    if (!isAdded) return@launch
                    if (r.isSuccess) {
                        if (next == JobStatus.COMPLETED) {
                            notificationRepository.push(
                                recipientUid = job.workerUid,
                                type = com.proyecto.chambaya.data.model.NotificationType.JOB_COMPLETED,
                                title = getString(R.string.k_push_job_fin),
                                message = getString(R.string.k_pub_job_fin_msg, job.publicationTitle.take(60)),
                                senderUid = uid,
                                publicationId = job.publicationId
                            )
                            Toast.makeText(requireContext(), getString(R.string.k_pub_completado), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.k_pub_en_curso), Toast.LENGTH_SHORT).show()
                        }
                        cargarSolicitudes()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            r.exceptionOrNull()?.message ?: getString(R.string.k_comun_no_actualizar),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
            .show()
    }

    // ── Contenido de cada sección ────────────────────────────────────────────

    private fun configurarPaneles() {
        configurarPanel(
            paneles[SECCION_PUBLICAR],
            R.string.publicar_nueva_titulo,
            R.string.publicar_nueva_sub,
            R.string.publicar_nueva_btn
        ) {
            abrirAccionPrincipal()
        }
    }

    private fun configurarPanel(panel: View, @StringRes titulo: Int, @StringRes subtitulo: Int, @StringRes boton: Int, alPulsar: () -> Unit) {
        panel.findViewById<TextView>(R.id.tvVacioTitulo).setText(titulo)
        panel.findViewById<TextView>(R.id.tvVacioSubtitulo).setText(subtitulo)
        panel.findViewById<MaterialButton>(R.id.btnVacioAccion).apply {
            setText(boton)
            setOnClickListener { alPulsar() }
        }
    }

    // ── Cambio de sección ────────────────────────────────────────────────────

    private fun seleccionar(nueva: Int, animar: Boolean) {
        if (nueva !in paneles.indices || nueva == seccionActual) return
        val anterior = seccionActual
        seccionActual = nueva
        aplicarEstado(animar, anterior)
        if (nueva == SECCION_SOLICITUDES) cargarSolicitudes()
    }

    private fun aplicarEstado(animar: Boolean, anterior: Int = seccionActual) {
        actualizarIconos(animar)
        moverIndicador(animar)
        mostrarPanel(anterior, seccionActual, animar)
    }

    private fun actualizarIconos(animar: Boolean) {
        iconos.forEachIndexed { indice, icono ->
            val activo = indice == seccionActual
            val destino = if (activo) colorActivo else colorInactivo
            val origen = (icono.tag as? Int) ?: colorInactivo
            tabs[indice].isSelected = activo

            if (animar && origen != destino) {
                ValueAnimator.ofObject(ArgbEvaluator(), origen, destino).apply {
                    duration = 200
                    addUpdateListener {
                        icono.imageTintList = ColorStateList.valueOf(it.animatedValue as Int)
                    }
                    start()
                }
                if (activo) {
                    icono.scaleX = 0.82f
                    icono.scaleY = 0.82f
                    icono.animate().scaleX(1f).scaleY(1f).setDuration(280)
                        .setInterpolator(OvershootInterpolator(2.2f)).start()
                }
            } else {
                icono.imageTintList = ColorStateList.valueOf(destino)
            }
            icono.tag = destino
        }
    }

    private fun moverIndicador(animar: Boolean) {
        val anchoTab = barraTabs.width / tabs.size
        if (anchoTab == 0) return
        val destino = seccionActual * anchoTab + (anchoTab - indicador.layoutParams.width) / 2f
        indicador.animate().cancel()
        if (animar) {
            indicador.animate().translationX(destino).setDuration(260)
                .setInterpolator(DecelerateInterpolator(1.6f)).start()
        } else {
            indicador.translationX = destino
        }
    }

    private fun mostrarPanel(anterior: Int, nueva: Int, animar: Boolean) {
        val desplazamiento = resources.displayMetrics.density * 28f
        val direccion = if (nueva >= anterior) 1 else -1

        paneles.forEachIndexed { indice, panel ->
            panel.animate().cancel()
            if (!animar || (indice != anterior && indice != nueva)) {
                panel.visibility = if (indice == nueva) View.VISIBLE else View.GONE
                panel.alpha = 1f
                panel.translationX = 0f
            }
        }
        if (!animar || anterior == nueva) return

        val sale = paneles[anterior]
        val entra = paneles[nueva]

        if (entra.visibility != View.VISIBLE) {
            entra.alpha = 0f
            entra.translationX = direccion * desplazamiento
            entra.visibility = View.VISIBLE
        }
        entra.animate().alpha(1f).translationX(0f).setDuration(260)
            .setInterpolator(DecelerateInterpolator()).start()

        sale.animate().alpha(0f).translationX(-direccion * desplazamiento).setDuration(160)
            .withEndAction {
                sale.visibility = View.GONE
                sale.alpha = 1f
                sale.translationX = 0f
            }.start()
    }

    /** Salta a una sección (para deep-links como Mis postulaciones). */
    fun mostrarSeccion(seccion: Int) {
        if (seccion in paneles.indices) seleccionar(seccion, animar = false)
    }

    companion object {
        const val KEY_SECCION = "publicar_seccion"
        const val SECCION_PUBLICAR = 0
        const val SECCION_PUBLICACIONES = 1
        const val SECCION_SOLICITUDES = 2
    }
}
