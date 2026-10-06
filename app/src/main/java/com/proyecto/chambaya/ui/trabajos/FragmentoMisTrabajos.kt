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
import com.proyecto.chambaya.data.model.PublicationType
import com.proyecto.chambaya.data.repository.ApplicationRepository
import com.proyecto.chambaya.data.repository.ChatRepository
import com.proyecto.chambaya.data.repository.JobRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.RatingRepository
import com.proyecto.chambaya.ui.jobs.JobDetailSheet
import com.proyecto.chambaya.ui.jobs.RateSheet
import com.proyecto.chambaya.ui.publish.CrearPublicacionActivity
import com.proyecto.chambaya.ui.publish.MyApplicationsAdapter
import com.proyecto.chambaya.ui.publish.MyAppRow
import kotlinx.coroutines.launch

/**
 * Solo rol Trabajador: Solicitudes (mis postulaciones) + Historial
 * (línea de tiempo de trabajos con fechas de contratación, inicio y fin).
 *
 * Replica el lenguaje visual de Publicar (barra de pestañas con indicador
 * deslizante y cambio por toque o deslizamiento).
 */
class FragmentoMisTrabajos : Fragment() {

    private var seccionActual = SECCION_SOLICITUDES

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmento_mis_trabajos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seccionActual = savedInstanceState?.getInt(KEY_SECCION, SECCION_SOLICITUDES) ?: SECCION_SOLICITUDES
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
            view.findViewById(R.id.tabSolicitudes),
            view.findViewById(R.id.tabHistorial),
            view.findViewById(R.id.tabPublicarTiempo)
        )
        iconos = listOf(
            view.findViewById(R.id.iconoSolicitudes),
            view.findViewById(R.id.iconoHistorial),
            view.findViewById(R.id.iconoPublicarTiempo)
        )
        paneles = listOf(
            view.findViewById(R.id.panelSolicitudes),
            view.findViewById(R.id.panelHistorial),
            view.findViewById(R.id.panelPublicarTiempo)
        )

        val publicarTiempoPanel = view.findViewById<View>(R.id.emptyPublicarTiempo)
        publicarTiempoPanel.findViewById<TextView>(R.id.tvVacioTitulo)?.apply {
            text = "Ofrece tu tiempo libre"
        }
        publicarTiempoPanel.findViewById<TextView>(R.id.tvVacioSubtitulo)?.apply {
            text = "Publica tu experiencia y disponibilidad para que puedan contratarte."
        }
        publicarTiempoPanel.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
            text = "Publicar disponibilidad"
            setOnClickListener {
                startActivity(
                    Intent(requireContext(), CrearPublicacionActivity::class.java)
                        .putExtra(CrearPublicacionActivity.EXTRA_PUBLICATION_TYPE, PublicationType.WORKER_AVAILABILITY)
                )
            }
        }

        configurarSolicitudes(view)
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
        outState.putInt(KEY_SECCION, seccionActual)
    }

    override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.white))
        if (::barraTabs.isInitialized) {
            cargarSolicitudes()
            cargarHistorial()
        }
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
        super.onDestroyView()
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
        const val KEY_SECCION = "mistrabajos_seccion"
        const val SECCION_SOLICITUDES = 0
        const val SECCION_HISTORIAL = 1
    }
}
