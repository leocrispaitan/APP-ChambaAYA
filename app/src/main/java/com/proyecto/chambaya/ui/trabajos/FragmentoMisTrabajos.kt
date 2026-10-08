package com.proyecto.chambaya.ui.trabajos

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.ApplicationStatus
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobApplication
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.model.PublicationStatus
import com.proyecto.chambaya.data.model.PublicationType
import com.proyecto.chambaya.data.repository.ApplicationRepository
import com.proyecto.chambaya.data.repository.ChatRepository
import com.proyecto.chambaya.data.repository.JobRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.data.repository.RatingRepository
import com.proyecto.chambaya.ui.jobs.JobDetailSheet
import com.proyecto.chambaya.ui.jobs.RateSheet
import com.proyecto.chambaya.ui.publish.CrearPublicacionActivity
import com.proyecto.chambaya.ui.publish.MyApplicationsAdapter
import com.proyecto.chambaya.ui.publish.MyAppRow
import com.proyecto.chambaya.ui.publish.MisPublicacionesAdapter
import kotlinx.coroutines.launch

/**
 * Solo rol Trabajador: disponibilidad, historial, solicitudes y gestión de
 * sus publicaciones.
 *
 * Replica el lenguaje visual de Publicar (barra de pestañas con indicador
 * deslizante y cambio por toque o deslizamiento).
 */
class FragmentoMisTrabajos : Fragment() {

    private var seccionActual = SECCION_PUBLICAR_TIEMPO

    private lateinit var tabs: List<View>
    private lateinit var iconos: List<ImageView>
    private lateinit var paneles: List<View>
    private lateinit var barraTabs: View
    private lateinit var indicador: View

    private var colorActivo = 0
    private var colorInactivo = 0
    private var anchoBarraPrevio = -1

    private val appRepository = ApplicationRepository()
    private val jobRepository = JobRepository()
    private val ratingRepository = RatingRepository()
    private val profileRepository = ProfileRepository()
    private val publicationRepository = PublicationRepository()

    // Solicitudes
    private var rvSolicitudes: RecyclerView? = null
    private var progressSol: ProgressBar? = null
    private var emptySol: View? = null
    private var solAdapter: MyApplicationsAdapter? = null
    private var solFilter = 0
    private var workerApps: List<JobApplication> = emptyList()
    private var jobsCache: Map<String, Job> = emptyMap()
    private var ratedCache: Set<String> = emptySet()
    private var solLoading = false

    // Historial
    private var rvHistorial: RecyclerView? = null
    private var progressHist: ProgressBar? = null
    private var emptyHist: View? = null
    private var histAdapter: TimelineAdapter? = null
    private var histFilter = 0 // 0 todos · 1 en curso · 2 completados
    private var historyJobs: List<Job> = emptyList()
    private var entityCache: Map<String, String> = emptyMap()
    private var histLoading = false

    // Publicaciones propias del trabajador.
    private var rvMisPublicaciones: RecyclerView? = null
    private var progressMisPublicaciones: ProgressBar? = null
    private var emptyMisPublicaciones: View? = null
    private var misPublicacionesAdapter: MisPublicacionesAdapter? = null
    private var publicacionesListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val editarPublicacionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            seleccionar(SECCION_MIS_PUBLICACIONES, animar = false)
            cargarMisPublicaciones()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmento_mis_trabajos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seccionActual = when {
            savedInstanceState?.containsKey(KEY_SECCION_V3) == true ->
                savedInstanceState.getInt(KEY_SECCION_V3)
            savedInstanceState?.containsKey(KEY_SECCION_V2) == true ->
                when (savedInstanceState.getInt(KEY_SECCION_V2)) {
                    0 -> SECCION_PUBLICAR_TIEMPO
                    1 -> SECCION_HISTORIAL
                    2 -> SECCION_SOLICITUDES
                    else -> SECCION_MIS_PUBLICACIONES
                }
            savedInstanceState?.containsKey(KEY_SECCION_V1) == true ->
                when (savedInstanceState.getInt(KEY_SECCION_V1, SECCION_PUBLICAR_TIEMPO)) {
                    0 -> SECCION_PUBLICAR_TIEMPO
                    1 -> SECCION_SOLICITUDES
                    2 -> SECCION_HISTORIAL
                    else -> SECCION_PUBLICAR_TIEMPO
                }
            else -> SECCION_PUBLICAR_TIEMPO
        }
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
            view.findViewById(R.id.tabPublicarTiempo),
            view.findViewById(R.id.tabMisPublicaciones),
            view.findViewById(R.id.tabHistorial),
            view.findViewById(R.id.tabSolicitudes),
        )
        // En el rol trabajador, ofrecer disponibilidad ocupa la primera pestaña.
        (barraTabs as? android.widget.LinearLayout)?.let { barra ->
            tabs.forEach { barra.removeView(it) }
            tabs.forEach { barra.addView(it) }
        }
        iconos = listOf(
            view.findViewById(R.id.iconoPublicarTiempo),
            view.findViewById(R.id.iconoMisPublicaciones),
            view.findViewById(R.id.iconoHistorial),
            view.findViewById(R.id.iconoSolicitudes),
        )
        paneles = listOf(
            view.findViewById(R.id.panelPublicarTiempo),
            view.findViewById(R.id.panelMisPublicaciones),
            view.findViewById(R.id.panelHistorial),
            view.findViewById(R.id.panelSolicitudes),
        )

        val publicarTiempoPanel = view.findViewById<View>(R.id.emptyPublicarTiempo)
        publicarTiempoPanel.findViewById<TextView>(R.id.tvVacioTitulo)?.apply {
            text = getString(R.string.pub_tiempo_libre_titulo)
        }
        publicarTiempoPanel.findViewById<TextView>(R.id.tvVacioSubtitulo)?.apply {
            text = getString(R.string.pub_tiempo_libre_sub)
        }
        publicarTiempoPanel.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
            text = getString(R.string.pub_disponibilidad_btn)
            setOnClickListener {
                startActivity(
                    Intent(requireContext(), CrearPublicacionActivity::class.java)
                        .putExtra(CrearPublicacionActivity.EXTRA_PUBLICATION_TYPE, PublicationType.WORKER_AVAILABILITY)
                )
            }
        }

        configurarSolicitudes(view)
        configurarMisPublicaciones(view)
        configurarHistorial(view)

        tabs.forEachIndexed { indice, tab ->
            tab.setOnClickListener { seleccionar(indice, animar = true) }
        }
        view.findViewById<com.proyecto.chambaya.ui.publish.ContenedorDeslizable>(R.id.contenedorSecciones).alDeslizar = { dir ->
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

        parentFragmentManager.setFragmentResultListener(RateSheet.REQUEST_RATED, viewLifecycleOwner) { _, _ ->
            cargarSolicitudes()
            cargarHistorial()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SECCION_V3, seccionActual)
    }

    override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.white))
        if (::barraTabs.isInitialized) {
            seleccionar(SECCION_PUBLICAR_TIEMPO, animar = false)
            cargarSolicitudes()
            cargarHistorial()
        }
    }

    override fun onPause() {
        detachPublicaciones()
        super.onPause()
    }

    override fun onDestroyView() {
        rvSolicitudes = null
        progressSol = null
        emptySol = null
        solAdapter = null
        rvHistorial = null
        progressHist = null
        emptyHist = null
        histAdapter = null
        detachPublicaciones()
        rvMisPublicaciones = null
        progressMisPublicaciones = null
        emptyMisPublicaciones = null
        misPublicacionesAdapter = null
        super.onDestroyView()
    }

    private fun configurarMisPublicaciones(view: View) {
        rvMisPublicaciones = view.findViewById(R.id.rvMisPublicaciones)
        progressMisPublicaciones = view.findViewById(R.id.progressMisPublicaciones)
        emptyMisPublicaciones = view.findViewById(R.id.emptyMisPublicaciones)
        misPublicacionesAdapter = MisPublicacionesAdapter(
            onVer = { pub ->
                JobDetailSheet.newInstance(pub.publicationId)
                    .show(parentFragmentManager, "worker_publication_detail")
            },
            onToggle = { pub -> alternarPublicacion(pub) },
            onMenu = { pub, anchor -> mostrarMenuPublicacion(pub, anchor) }
        )
        rvMisPublicaciones?.adapter = misPublicacionesAdapter
        rvMisPublicaciones?.visibility = View.GONE
        emptyMisPublicaciones?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
            text = getString(R.string.pub_disponibilidad_btn)
            setOnClickListener {
                startActivity(
                    Intent(requireContext(), CrearPublicacionActivity::class.java)
                        .putExtra(
                            CrearPublicacionActivity.EXTRA_PUBLICATION_TYPE,
                            PublicationType.WORKER_AVAILABILITY
                        )
                )
            }
        }
    }

    private fun cargarMisPublicaciones() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!isAdded || publicacionesListener != null) return
        progressMisPublicaciones?.visibility = View.VISIBLE
        rvMisPublicaciones?.visibility = View.GONE
        emptyMisPublicaciones?.visibility = View.GONE
        publicacionesListener = publicationRepository.listenByOwner(
            uid = uid,
            limit = 50,
            onUpdate = { publicaciones ->
                if (isAdded) pintarMisPublicaciones(publicaciones)
            },
            onError = { error ->
                if (!isAdded) return@listenByOwner
                progressMisPublicaciones?.visibility = View.GONE
                if (misPublicacionesAdapter?.itemCount == 0) {
                    emptyMisPublicaciones?.visibility = View.VISIBLE
                }
                Toast.makeText(
                    requireContext(),
                    error.message ?: getString(R.string.k_comun_no_actualizar),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun pintarMisPublicaciones(publicaciones: List<Publication>) {
        progressMisPublicaciones?.visibility = View.GONE
        if (publicaciones.isEmpty()) {
            rvMisPublicaciones?.visibility = View.GONE
            emptyMisPublicaciones?.visibility = View.VISIBLE
            emptyMisPublicaciones?.findViewById<TextView>(R.id.tvVacioTitulo)
                ?.setText(R.string.publicaciones_vacio_titulo)
            emptyMisPublicaciones?.findViewById<TextView>(R.id.tvVacioSubtitulo)
                ?.setText(R.string.publicaciones_vacio_sub)
        } else {
            emptyMisPublicaciones?.visibility = View.GONE
            rvMisPublicaciones?.visibility = View.VISIBLE
            misPublicacionesAdapter?.submitList(publicaciones)
        }
    }

    private fun detachPublicaciones() {
        publicacionesListener?.remove()
        publicacionesListener = null
    }

    private fun alternarPublicacion(publicacion: Publication) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val nuevoEstado = when (publicacion.status) {
            PublicationStatus.ACTIVE -> PublicationStatus.PAUSED
            PublicationStatus.PAUSED, PublicationStatus.FINISHED -> PublicationStatus.ACTIVE
            else -> return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = publicationRepository.changeStatus(
                requireContext(), uid, publicacion.publicationId, nuevoEstado
            )
            if (resultado.isSuccess) {
                val mensaje = if (nuevoEstado == PublicationStatus.PAUSED) {
                    getString(R.string.k_detalle_pausada)
                } else getString(R.string.k_pub_activa_nueva)
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    resultado.exceptionOrNull()?.message ?: getString(R.string.k_comun_no_actualizar),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarMenuPublicacion(publicacion: Publication, anchor: View) {
        val menu = PopupMenu(requireContext(), anchor)
        menu.menu.add(0, 1, 0, getString(R.string.k_comun_editar))
        if (publicacion.status == PublicationStatus.ACTIVE) {
            menu.menu.add(0, 2, 0, getString(R.string.item_pausar))
        }
        if (publicacion.status == PublicationStatus.PAUSED) {
            menu.menu.add(0, 3, 0, getString(R.string.k_detalle_reactivar))
        }
        if (publicacion.status == PublicationStatus.FINISHED ||
            publicacion.status == PublicationStatus.ARCHIVED
        ) {
            menu.menu.add(0, 3, 0, getString(R.string.k_pub_republicar))
        }
        if (publicacion.status != PublicationStatus.FINISHED) {
            menu.menu.add(0, 4, 0, getString(R.string.sheet_detalle_finalizar))
        }
        if (publicacion.status != PublicationStatus.ARCHIVED) {
            menu.menu.add(0, 6, 0, getString(R.string.k_pub_archivar))
        }
        menu.menu.add(0, 5, 0, getString(R.string.k_comun_eliminar))
        menu.setOnMenuItemClickListener { item ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
                ?: return@setOnMenuItemClickListener false
            when (item.itemId) {
                1 -> {
                    if (publicacion.status !in listOf(PublicationStatus.ACTIVE, PublicationStatus.PAUSED)) {
                        Toast.makeText(requireContext(), getString(R.string.k_pub_solo_edit), Toast.LENGTH_SHORT).show()
                    } else {
                        editarPublicacionLauncher.launch(
                            CrearPublicacionActivity.editarIntent(
                                requireActivity(), publicacion.publicationId, publicacion.type
                            )
                        )
                    }
                    true
                }
                2, 3 -> { alternarPublicacion(publicacion); true }
                4 -> {
                    cambiarEstadoPublicacion(publicacion, uid, PublicationStatus.FINISHED, R.string.k_detalle_finalizada)
                    true
                }
                5 -> { confirmarEliminarPublicacion(publicacion, uid); true }
                6 -> {
                    cambiarEstadoPublicacion(publicacion, uid, PublicationStatus.ARCHIVED, R.string.k_pub_archivada)
                    true
                }
                else -> false
            }
        }
        menu.show()
    }

    private fun cambiarEstadoPublicacion(
        publicacion: Publication,
        uid: String,
        estado: String,
        mensajeRes: Int
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = publicationRepository.changeStatus(
                requireContext(), uid, publicacion.publicationId, estado
            )
            if (resultado.isSuccess) {
                Toast.makeText(requireContext(), getString(mensajeRes), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    resultado.exceptionOrNull()?.message ?: getString(R.string.k_comun_no_actualizar),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmarEliminarPublicacion(publicacion: Publication, uid: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.k_pub_eliminar_titulo)
            .setMessage(R.string.k_pub_eliminar_msg)
            .setPositiveButton(R.string.k_comun_eliminar) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val resultado = publicationRepository.delete(
                        requireContext(), uid, publicacion.publicationId
                    )
                    if (resultado.isSuccess) {
                        Toast.makeText(
                            requireContext(), getString(R.string.k_pub_eliminada), Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(), getString(R.string.k_comun_no_eliminar), Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
            .show()
    }

    // ── Solicitudes ───────────────────────────────────────────────

    private fun configurarSolicitudes(view: View) {
        rvSolicitudes = view.findViewById(R.id.rvSolicitudes)
        progressSol = view.findViewById(R.id.progressSolicitudes)
        emptySol = view.findViewById(R.id.emptySolicitudes)
        solAdapter = MyApplicationsAdapter(
            onPrimary = { row -> accionMiPostulacion(row) },
            onContact = { row -> abrirChat(row.app.employerUid, row.app.publicationId, row.app.publicationTitle) },
            onOpenDetail = { row ->
                JobDetailSheet.newInstance(row.app.publicationId).show(parentFragmentManager, "detail")
            }
        )
        rvSolicitudes?.adapter = solAdapter
        rvSolicitudes?.visibility = View.GONE

        val chips = listOf(
            view.findViewById<TextView>(R.id.chipSolAll),
            view.findViewById<TextView>(R.id.chipSolPending),
            view.findViewById<TextView>(R.id.chipSolDecided)
        )
        chips.forEachIndexed { i, chip ->
            chip.setOnClickListener {
                solFilter = i
                chips.forEachIndexed { j, c -> pintarChip(c, j == i) }
                pintarSolicitudes()
            }
        }
        emptySol?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
            text = "Explorar chambas"
            setOnClickListener {
                (activity as? com.proyecto.chambaya.MainActivity)
                    ?.navigateToTab(R.id.nav_jobs)
            }
        }
    }

    private fun pintarChip(chip: TextView, on: Boolean) {
        chip.setBackgroundResource(if (on) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
        chip.setTextColor(requireContext().getColor(if (on) R.color.white else R.color.text_primary))
    }

    private fun cargarSolicitudes() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (solLoading || !isAdded) return
        solLoading = true
        progressSol?.visibility = View.VISIBLE
        rvSolicitudes?.visibility = View.GONE
        emptySol?.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            workerApps = appRepository.listByWorker(uid).getOrNull().orEmpty()
            val acceptedIds = workerApps
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
                solLoading = false
                return@launch
            }
            solLoading = false
            pintarSolicitudes()
        }
    }

    private fun pintarSolicitudes() {
        if (!isAdded) return
        progressSol?.visibility = View.GONE
        val filtradas = when (solFilter) {
            1 -> workerApps.filter { it.status == ApplicationStatus.PENDING }
            2 -> workerApps.filter { it.status != ApplicationStatus.PENDING }
            else -> workerApps
        }
        if (filtradas.isEmpty()) {
            rvSolicitudes?.visibility = View.GONE
            emptySol?.visibility = View.VISIBLE
            emptySol?.findViewById<TextView>(R.id.tvVacioTitulo)?.text = getString(R.string.k_mistrab_titulo)
            emptySol?.findViewById<TextView>(R.id.tvVacioSubtitulo)?.text =
                getString(R.string.k_mistrab_sub)
        } else {
            emptySol?.visibility = View.GONE
            rvSolicitudes?.visibility = View.VISIBLE
            solAdapter?.submitList(
                filtradas.map { app ->
                    val job = jobsCache[app.applicationId]
                    MyAppRow(app, job, job?.jobId in ratedCache)
                }
            )
        }
    }

    private fun accionMiPostulacion(row: MyAppRow) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val app = row.app
        val job = row.job
        when {
            app.status == ApplicationStatus.PENDING -> {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.sheet_postular_retirar)
                    .setMessage(getString(R.string.k_mistrab_retirar_fmt, app.publicationTitle.take(50)))
                    .setPositiveButton(R.string.item_retirar) { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val r = appRepository.withdraw(requireContext(), uid, app.applicationId)
                            if (!isAdded) return@launch
                            Toast.makeText(
                                requireContext(),
                                if (r.isSuccess) getString(R.string.k_apl_retirada) else getString(R.string.k_apl_no_retirar),
                                Toast.LENGTH_SHORT
                            ).show()
                            cargarSolicitudes()
                            cargarHistorial()
                        }
                    }
                    .setNegativeButton(R.string.k_comun_cancelar, null)
                    .show()
            }
            job?.status == JobStatus.COMPLETED && !row.ratedByMe -> {
                RateSheet.newInstance(job.jobId).show(parentFragmentManager, "rate")
            }
            else -> {
                JobDetailSheet.newInstance(app.publicationId).show(parentFragmentManager, "detail")
            }
        }
    }

    private fun abrirChat(otherUid: String, publicationId: String, publicationTitle: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (me.isBlank() || otherUid.isBlank() || me == otherUid) return
        viewLifecycleOwner.lifecycleScope.launch {
            val conv = ChatRepository()
                .ensureConversation(
                    me,
                    otherUid,
                    publicationId,
                    publicationTitle,
                    ChatRepository.hiddenConversationIds(requireContext(), me)
                ).getOrNull()
            val perfil = profileRepository.loadPublicProfile(otherUid).getOrNull()
            if (!isAdded) return@launch
            if (conv == null) {
                Toast.makeText(requireContext(), getString(R.string.k_chat_no_abrir), Toast.LENGTH_LONG).show()
                return@launch
            }
            val intent = Intent(
                requireContext(),
                com.proyecto.chambaya.ui.chat.ActividadChatDetalle::class.java
            ).apply {
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_CONV_ID, conv.conversationId)
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_OTHER_UID, otherUid)
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_NOMBRE, perfil?.displayName() ?: getString(R.string.k_chat_default))
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_FOTO, perfil?.photoUrl.orEmpty())
                putExtra(com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_PUB_TITULO, publicationTitle)
            }
            startActivity(intent)
        }
    }

    // ── Historial ─────────────────────────────────────────────────

    private fun configurarHistorial(view: View) {
        rvHistorial = view.findViewById(R.id.rvHistorial)
        progressHist = view.findViewById(R.id.progressHistorial)
        emptyHist = view.findViewById(R.id.emptyHistorial)
        histAdapter = TimelineAdapter(
            onRate = { row ->
                RateSheet.newInstance(row.job.jobId).show(parentFragmentManager, "rate")
            }
        )
        rvHistorial?.adapter = histAdapter
        rvHistorial?.visibility = View.GONE

        val chips = listOf(
            view.findViewById<TextView>(R.id.chipHistAll),
            view.findViewById<TextView>(R.id.chipHistActive),
            view.findViewById<TextView>(R.id.chipHistDone)
        )
        chips.forEachIndexed { i, chip ->
            chip.setOnClickListener {
                histFilter = i
                chips.forEachIndexed { j, c -> pintarChip(c, j == i) }
                pintarHistorial()
            }
        }
        emptyHist?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
            text = "Explorar chambas"
            setOnClickListener {
                (activity as? com.proyecto.chambaya.MainActivity)
                    ?.navigateToTab(R.id.nav_jobs)
            }
        }
    }

    private fun cargarHistorial() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (histLoading || !isAdded) return
        histLoading = true
        progressHist?.visibility = View.VISIBLE
        rvHistorial?.visibility = View.GONE
        emptyHist?.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            val jobs = jobRepository.listByWorker(uid).getOrNull().orEmpty()
            // Entidades (contratantes) de cada trabajo.
            val entityIds = jobs.map { it.employerUid }.distinct().filter { it.isNotBlank() }
            val nombres = mutableMapOf<String, String>()
            entityIds.forEach { id ->
                profileRepository.loadPublicProfile(id).getOrNull()?.let {
                    nombres[id] = it.displayName()
                }
            }
            val completedIds = jobs.filter { it.status == JobStatus.COMPLETED }.map { it.jobId }
            val rated = if (completedIds.isEmpty()) emptySet()
            else ratingRepository.ratedJobIds(completedIds, uid).getOrNull().orEmpty()
            if (!isAdded) {
                histLoading = false
                return@launch
            }
            histLoading = false
            historyJobs = jobs
            entityCache = nombres
            // Reutiliza el caché de calificados si ya se cargó en solicitudes.
            if (rated.isNotEmpty()) ratedCache = ratedCache + rated
            pintarHistorial()
        }
    }

    private fun pintarHistorial() {
        if (!isAdded) return
        progressHist?.visibility = View.GONE
        val filtrados = when (histFilter) {
            1 -> historyJobs.filter { it.status == JobStatus.ACCEPTED || it.status == JobStatus.IN_PROGRESS }
            2 -> historyJobs.filter { it.status == JobStatus.COMPLETED }
            else -> historyJobs
        }
        if (filtrados.isEmpty()) {
            rvHistorial?.visibility = View.GONE
            emptyHist?.visibility = View.VISIBLE
            emptyHist?.findViewById<TextView>(R.id.tvVacioTitulo)?.text = getString(R.string.k_mistrab_recorrido)
            emptyHist?.findViewById<TextView>(R.id.tvVacioSubtitulo)?.text =
                getString(R.string.k_mistrab_hist_sub)
        } else {
            emptyHist?.visibility = View.GONE
            rvHistorial?.visibility = View.VISIBLE
            histAdapter?.submitList(
                filtrados.map { job ->
                    TimelineRow(job, entityCache[job.employerUid].orEmpty(), job.jobId in ratedCache)
                }
            )
        }
    }

    // ── Cambio de sección (misma mecánica que Publicar) ───────────

    private fun seleccionar(nueva: Int, animar: Boolean) {
        if (nueva !in paneles.indices || nueva == seccionActual) return
        val anterior = seccionActual
        seccionActual = nueva
        aplicarEstado(animar, anterior)
        if (nueva == SECCION_SOLICITUDES) cargarSolicitudes()
        if (nueva == SECCION_MIS_PUBLICACIONES) cargarMisPublicaciones()
        else detachPublicaciones()
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

    private companion object {
        const val KEY_SECCION_V1 = "mistrabajos_seccion"
        const val KEY_SECCION_V2 = "mistrabajos_seccion_v2"
        const val KEY_SECCION_V3 = "mistrabajos_seccion_v3"
        const val SECCION_PUBLICAR_TIEMPO = 0
        const val SECCION_MIS_PUBLICACIONES = 1
        const val SECCION_HISTORIAL = 2
        const val SECCION_SOLICITUDES = 3
    }
}
