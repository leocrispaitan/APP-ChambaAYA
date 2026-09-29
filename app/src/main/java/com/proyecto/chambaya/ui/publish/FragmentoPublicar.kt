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
import com.proyecto.chambaya.ActivarContratanteActivity
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.EditarPerfilActivity
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.model.PublicationStatus
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.puedeContratarOPublicar
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import com.proyecto.chambaya.ui.jobs.JobDetailSheet
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
    private var esContratante = false
    private var tieneLugar = false
    private var bloqueadoPorCompletitud = false
    private var cargandoEstado = false

    private lateinit var emptyLugar: View
    private var rvMis: RecyclerView? = null
    private var progressMis: ProgressBar? = null
    private var emptyMis: View? = null
    private var misAdapter: MisPublicacionesAdapter? = null

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
            Toast.makeText(requireContext(), "¡Chamba publicada!", Toast.LENGTH_SHORT).show()
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SECCION, seccionActual)
    }

    override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.white))
        if (::barraTabs.isInitialized) {
            recargarEstado()
            cargarMisPublicaciones()
        }
    }

    // ── FASE 4 · Puerta de publicación ────────────────────────────

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
        startActivity(Intent(requireContext(), ActivarContratanteActivity::class.java))
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
        emptyMis?.findViewById<MaterialButton>(R.id.btnVacioAccion)?.apply {
            text = getString(R.string.publicaciones_vacio_btn)
            setOnClickListener { abrirAccionPrincipal() }
        }
    }

    private fun cargarMisPublicaciones() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!esContratante) {
            rvMis?.visibility = View.GONE
            progressMis?.visibility = View.GONE
            emptyMis?.visibility = View.GONE
            return
        }
        progressMis?.visibility = View.VISIBLE
        rvMis?.visibility = View.GONE
        emptyMis?.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            val lista = pubRepository.byOwner(uid, 50).getOrNull().orEmpty()
            if (!isAdded) return@launch
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
        }
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
            val r = pubRepository.changeStatus(uid, pub.publicationId, nuevo)
            if (r.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    when (nuevo) {
                        PublicationStatus.PAUSED -> "Publicación pausada."
                        PublicationStatus.ACTIVE -> "Publicación activa de nuevo."
                        else -> "Estado actualizado."
                    },
                    Toast.LENGTH_SHORT
                ).show()
                cargarMisPublicaciones()
            } else {
                Toast.makeText(requireContext(), r.exceptionOrNull()?.message ?: "No se pudo actualizar.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarMenu(pub: Publication, anchor: View) {
        val menu = PopupMenu(requireContext(), anchor)
        menu.menu.add(0, 1, 0, "Editar")
        if (pub.status == PublicationStatus.ACTIVE) menu.menu.add(0, 2, 0, "Pausar")
        if (pub.status == PublicationStatus.PAUSED) menu.menu.add(0, 3, 0, "Reactivar")
        if (pub.status == PublicationStatus.FINISHED || pub.status == PublicationStatus.ARCHIVED) {
            menu.menu.add(0, 3, 0, "Republicar")
        }
        if (pub.status != PublicationStatus.FINISHED) menu.menu.add(0, 4, 0, "Finalizar")
        if (pub.status != PublicationStatus.ARCHIVED) menu.menu.add(0, 6, 0, "Archivar")
        menu.menu.add(0, 5, 0, "Eliminar")
        menu.setOnMenuItemClickListener { item ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnMenuItemClickListener false
            when (item.itemId) {
                1 -> {
                    if (pub.status !in listOf(PublicationStatus.ACTIVE, PublicationStatus.PAUSED)) {
                        Toast.makeText(requireContext(), "Solo puedes editar activas o pausadas.", Toast.LENGTH_SHORT).show()
                    } else {
                        publicarLauncher.launch(CrearPublicacionActivity.editarIntent(requireActivity(), pub.publicationId))
                    }
                    true
                }
                2, 3 -> { alternarEstado(pub); true }
                4 -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val r = pubRepository.changeStatus(uid, pub.publicationId, PublicationStatus.FINISHED)
                        if (r.isSuccess) {
                            Toast.makeText(requireContext(), "Publicación finalizada.", Toast.LENGTH_SHORT).show()
                            cargarMisPublicaciones()
                        }
                    }
                    true
                }
                5 -> { confirmarEliminar(pub); true }
                6 -> {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val r = pubRepository.changeStatus(uid, pub.publicationId, PublicationStatus.ARCHIVED)
                        if (r.isSuccess) {
                            Toast.makeText(requireContext(), "Publicación archivada.", Toast.LENGTH_SHORT).show()
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
            .setTitle("Eliminar publicación")
            .setMessage("Se quitará del feed para todos. Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = pubRepository.delete(uid, pub.publicationId)
                    if (r.isSuccess) {
                        Toast.makeText(requireContext(), "Publicación eliminada.", Toast.LENGTH_SHORT).show()
                        cargarMisPublicaciones()
                    } else {
                        Toast.makeText(requireContext(), "No se pudo eliminar.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── Solicitudes (Fase 7: placeholder honesto) ─────────────────

    private fun configurarSolicitudes(view: View) {
        configurarPanel(
            view.findViewById(R.id.panelSolicitudes),
            R.string.solicitudes_vacio_titulo,
            R.string.solicitudes_vacio_sub,
            R.string.solicitudes_vacio_btn
        ) { seleccionar(SECCION_PUBLICACIONES, animar = true) }
    }

    private fun pintarSolicitudes() {
        val panel: View = try { requireView().findViewById(R.id.panelSolicitudes) } catch (_: Exception) { return }
        if (esContratante) {
            panel.findViewById<TextView>(R.id.tvVacioTitulo).text = "Solicitudes de tus chambas"
            panel.findViewById<TextView>(R.id.tvVacioSubtitulo).text =
                "Cuando un especialista se postule, verás aquí su perfil y podrás aceptar o rechazar. Disponible en la Fase 7."
            panel.findViewById<MaterialButton>(R.id.btnVacioAccion).apply {
                text = "Ver mis publicaciones"
                setOnClickListener { seleccionar(SECCION_PUBLICACIONES, animar = true) }
            }
        } else {
            configurarPanel(
                panel,
                R.string.solicitudes_vacio_titulo,
                R.string.solicitudes_vacio_sub,
                R.string.solicitudes_vacio_btn
            ) { seleccionar(SECCION_PUBLICACIONES, animar = true) }
        }
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
        const val KEY_SECCION = "publicar_seccion"
        const val SECCION_PUBLICAR = 0
        const val SECCION_PUBLICACIONES = 1
        const val SECCION_SOLICITUDES = 2
    }
}
