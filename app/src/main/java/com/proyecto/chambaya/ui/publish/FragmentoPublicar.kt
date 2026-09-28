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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.ActivarContratanteActivity
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.Workplace
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import com.proyecto.chambaya.data.repository.motivoFirestore
import com.proyecto.chambaya.ui.profile.ProfileCache
import com.proyecto.chambaya.ui.workplace.EditarLugarActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla "Publicar" con tres secciones al estilo Instagram:
 *   0 · Publicar       1 · Publicaciones       2 · Solicitudes
 *
 * Se cambia de sección tocando un icono o deslizando el contenido. El indicador
 * se desplaza bajo la pestaña activa y el contenido entra/sale con un fundido.
 *
 * FASE 4 — la pestaña Publicar muestra el lugar del contratante:
 *  - sin lugar: CTA "Crear lugar" → [EditarLugarActivity];
 *  - con lugar: tarjeta con foto, nombre, tipo, dirección, fecha y
 *    botones Editar / Eliminar.
 * Las pestañas Publicaciones y Solicitudes siguen vacías (Fase 5).
 */

/**
 * Pantalla "Publicar" con tres secciones al estilo Instagram:
 *   0 · Publicar       1 · Publicaciones       2 · Solicitudes
 *
 * Se cambia de sección tocando un icono o deslizando el contenido. El indicador
 * se desplaza bajo la pestaña activa y el contenido entra/sale con un fundido.
 *
 * Por ahora cada sección muestra su estado vacío; el contenido real (formulario,
 * lista de publicaciones y lista de solicitudes) se enchufa en cada panel.
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

    // ── FASE 4 · Lugar del contratante ──────────────────────────────
    private val lugarRepository = WorkplaceRepository()
    private val perfilRepository = ProfileRepository()
    private var lugarActual: Workplace? = null
    private var esContratante = false
    private var cargandoLugar = false

    private lateinit var emptyLugar: View
    private lateinit var cardLugar: View
    private lateinit var ivLugarFoto: ImageView
    private lateinit var tvLugarNombre: TextView
    private lateinit var tvLugarTipo: TextView
    private lateinit var tvLugarDireccion: TextView
    private lateinit var tvLugarFecha: TextView

    private val lugarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) recargarLugar()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmento_publicar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        seccionActual = savedInstanceState?.getInt(KEY_SECCION, SECCION_PUBLICAR) ?: SECCION_PUBLICAR
        colorActivo = requireContext().getColor(R.color.profile_tab_active)
        colorInactivo = requireContext().getColor(R.color.profile_tab_inactive)

        barraTabs = view.findViewById(R.id.barraTabs)
        indicador = view.findViewById(R.id.indicadorTab)
        // Baja la barra de pestañas por debajo de la barra de estado
        // (batería, señal, wifi). Sin esto, con edge-to-edge los iconos
        // quedan montados sobre el sistema, como se veía en el reporte.
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

        enlazarLugar(view)
        configurarPaneles()

        tabs.forEachIndexed { indice, tab ->
            tab.setOnClickListener { seleccionar(indice, animar = true) }
        }
        view.findViewById<ContenedorDeslizable>(R.id.contenedorSecciones).alDeslizar = { dir ->
            seleccionar(seccionActual + dir, animar = true)
        }

        // Estado inicial sin animar. Se reposiciona el indicador si cambia el ancho
        // (rotación, pantalla dividida).
        aplicarEstado(animar = false)
        barraTabs.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val ancho = right - left
            if (ancho != anchoBarraPrevio) {
                anchoBarraPrevio = ancho
                moverIndicador(animar = false)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SECCION, seccionActual)
    }

    override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.white))
        // Al volver del editor, el lugar puede haber cambiado.
        if (::barraTabs.isInitialized) recargarLugar()
    }

    // ── FASE 4 · Lugar del contratante ──────────────────────────────

    private fun enlazarLugar(root: View) {
        emptyLugar = root.findViewById(R.id.emptyLugar)
        cardLugar = root.findViewById(R.id.cardLugar)
        ivLugarFoto = root.findViewById(R.id.ivLugarFoto)
        tvLugarNombre = root.findViewById(R.id.tvLugarNombre)
        tvLugarTipo = root.findViewById(R.id.tvLugarTipo)
        tvLugarDireccion = root.findViewById(R.id.tvLugarDireccion)
        tvLugarFecha = root.findViewById(R.id.tvLugarFecha)
        root.findViewById<View>(R.id.btnLugarEditar).setOnClickListener { abrirEditorLugar() }
        root.findViewById<View>(R.id.btnLugarEliminar).setOnClickListener { confirmarEliminarLugar() }
    }

    /**
     * Pinta la pestaña Publicar según el estado real:
     *  - no contratante → CTA para activar el modo;
     *  - contratante sin lugar → CTA "Crear lugar";
     *  - contratante con lugar → tarjeta con Editar / Eliminar.
     */
    private fun recargarLugar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (cargandoLugar) return
        cargandoLugar = true
        viewLifecycleOwner.lifecycleScope.launch {
            val perfil = perfilRepository.loadProfile(uid).getOrNull()
            if (perfil != null) ProfileCache.perfil = perfil
            esContratante = perfil?.roles?.contains(UserRoles.CONTRATANTE) == true &&
                perfil?.employer?.enabled == true
            if (!isAdded) {
                cargandoLugar = false
                return@launch
            }
            if (!esContratante) {
                lugarActual = null
                pintarSinLugar(
                    titulo = getString(R.string.publicar_nueva_titulo),
                    subtitulo = getString(R.string.lugar_error_solo_contratante),
                    boton = getString(R.string.publicar_nueva_btn)
                )
                cargandoLugar = false
                return@launch
            }
            val lugar = lugarRepository.loadByOwner(uid).getOrNull()
            if (!isAdded) {
                cargandoLugar = false
                return@launch
            }
            lugarActual = lugar
            if (lugar == null) {
                pintarSinLugar(
                    titulo = getString(R.string.lugar_sin_lugar_titulo),
                    subtitulo = getString(R.string.lugar_sin_lugar_sub),
                    boton = getString(R.string.lugar_sin_lugar_btn)
                )
            } else {
                pintarLugar(lugar)
            }
            cargandoLugar = false
        }
    }

    private fun pintarSinLugar(titulo: String, subtitulo: String, boton: String) {
        if (!isAdded) return
        cardLugar.visibility = View.GONE
        emptyLugar.visibility = View.VISIBLE
        emptyLugar.findViewById<TextView>(R.id.tvVacioTitulo).text = titulo
        emptyLugar.findViewById<TextView>(R.id.tvVacioSubtitulo).text = subtitulo
        emptyLugar.findViewById<MaterialButton>(R.id.btnVacioAccion).apply {
            text = boton
            setOnClickListener { abrirEditorLugar() }
        }
    }

    private fun pintarLugar(lugar: Workplace) {
        if (!isAdded) return
        emptyLugar.visibility = View.GONE
        cardLugar.visibility = View.VISIBLE
        tvLugarNombre.text = lugar.name
        val detalleTipo = buildString {
            append(lugar.typeLabel)
            if (lugar.sector.isNotBlank()) append(" • ${lugar.sector}")
        }
        tvLugarTipo.text = detalleTipo
        val direccion = buildString {
            if (lugar.address.isNotBlank()) append(lugar.address)
            if (lugar.locationLabel.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append(lugar.locationLabel)
            }
            if (lugar.location.hasCoords) {
                if (isNotEmpty()) append("\n")
                append(lugar.location.shortLabel)
            }
            if (isEmpty()) append(lugar.address.ifBlank { lugar.locationLabel })
        }
        tvLugarDireccion.text = direccion.toString()
        tvLugarFecha.text = if (lugar.createdAtMillis > 0) {
            val formato = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-PE"))
            getString(R.string.lugar_publicado_el, formato.format(Date(lugar.createdAtMillis)))
        } else ""
        if (lugar.hasPhoto) {
            ivLugarFoto.load(lugar.photoUrl) {
                placeholder(R.drawable.ic_profile_photos)
                error(R.drawable.ic_profile_photos)
                crossfade(true)
            }
        } else {
            ivLugarFoto.setImageResource(R.drawable.ic_profile_photos)
        }
    }

    private fun abrirEditorLugar() {
        if (!esContratante) {
            // Trabajador sin modo contratante: ofrecer activarlo.
            startActivity(Intent(requireContext(), ActivarContratanteActivity::class.java))
            return
        }
        val intent = Intent(requireContext(), EditarLugarActivity::class.java)
        lugarActual?.workplaceId?.let {
            intent.putExtra(EditarLugarActivity.EXTRA_WORKPLACE_ID, it)
        }
        lugarLauncher.launch(intent)
    }

    private fun confirmarEliminarLugar() {
        val lugar = lugarActual ?: return
        AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle(R.string.lugar_eliminar_titulo)
            .setMessage(getString(R.string.lugar_eliminar_mensaje, lugar.name))
            .setPositiveButton(R.string.lugar_btn_eliminar) { _, _ -> eliminarLugar() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun eliminarLugar() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val lugar = lugarActual ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            lugarRepository.delete(uid, lugar.workplaceId)
                .onSuccess {
                    runCatching {
                        perfilRepository.loadProfile(uid).getOrNull()?.let {
                            ProfileCache.perfil = it
                        }
                    }
                    Toast.makeText(requireContext(), R.string.lugar_eliminado, Toast.LENGTH_SHORT).show()
                    recargarLugar()
                }
                .onFailure {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.lugar_error_guardar, motivoFirestore(it)),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    // ── Contenido de cada sección ────────────────────────────────────────────

    private fun configurarPaneles() {
        configurarPanel(
            paneles[SECCION_PUBLICAR],
            R.string.lugar_sin_lugar_titulo,
            R.string.lugar_sin_lugar_sub,
            R.string.lugar_sin_lugar_btn
        ) {
            abrirEditorLugar()
        }
        configurarPanel(
            paneles[SECCION_PUBLICACIONES],
            R.string.publicaciones_vacio_titulo,
            R.string.publicaciones_vacio_sub,
            R.string.publicaciones_vacio_btn
        ) { seleccionar(SECCION_PUBLICAR, animar = true) }
        configurarPanel(
            paneles[SECCION_SOLICITUDES],
            R.string.solicitudes_vacio_titulo,
            R.string.solicitudes_vacio_sub,
            R.string.solicitudes_vacio_btn
        ) { seleccionar(SECCION_PUBLICACIONES, animar = true) }
    }

    private fun configurarPanel(
        panel: View,
        @StringRes titulo: Int,
        @StringRes subtitulo: Int,
        @StringRes boton: Int,
        alPulsar: () -> Unit
    ) {
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
                    // Pequeño "pop" en el icono que se activa.
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
