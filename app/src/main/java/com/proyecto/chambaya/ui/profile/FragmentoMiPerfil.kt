package com.proyecto.chambaya.ui.profile

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.EditarPerfilActivity
import com.proyecto.chambaya.MainActivity
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.StatisticsBlock
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.Workplace
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.RatingRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import com.proyecto.chambaya.data.repository.motivoFirestore
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * FASE 2 — "Mi Perfil".
 *
 * Antes de esta fase la pantalla era una maqueta: el nombre, el `@usuario`, el
 * porcentaje de completitud, el teléfono, el correo, la biografía y las cuatro
 * especialidades estaban escritos en el layout. Aquí todo eso sale de
 * `users/{uid}`.
 *
 * Qué se dibuja con datos reales:
 *  - avatar, nombre y `@usuario`                             -> `profile`
 *  - completitud (badge, anillo del banner y banner)         -> derivado
 *  - trabajos / experiencia / calificación                    -> `worker` + `statistics`
 *  - biografía, especialidades, distrito                      -> `profile` + `worker`
 *  - teléfono y correo, según lo que permita `privacy`       -> `privacy`
 *  - documento de identidad verificado                        -> `identity` (solo lectura)
 *
 * `cardExperience` y `cardReviews` se dejan ocultas: sus datos pertenecen a la
 * FASE 8/9 (trabajos completados y reseñas), no a esta fase.
 */
class FragmentoMiPerfil : Fragment() {

    private val repository = ProfileRepository()

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    /**
     * Último perfil leído de Firestore; sobrevive a los recreados de vista.
     *
     * Se inicializa desde [ProfileCache] para que, si `FragmentoAjustesPerfil`
     * ya lo cargó segundos antes, esta pantalla pinte esos datos de inmediato
     * en vez de esperar una nueva consulta.
     */
    private var perfil: UserProfile? = ProfileCache.perfil

    /**
     * Lugar del establecimiento en vista empresa (concepto Facebook: la
     * "página" del negocio frente al perfil personal).
     *
     * Solo se carga en modo contratante; en modo trabajador siempre es nulo
     * y el perfil pinta datos personales. Hoy el propietario ve ambas vistas
     * cambiando de modo; en la FASE 17 (perfil público) este mismo campo
     * decide qué ve un visitante (datos del negocio, nunca gestión).
     */
    private var lugar: Workplace? = null
    private val lugarRepository = WorkplaceRepository()

    /** Evita dos escrituras de inicialización simultáneas. */
    private var cargando = false

    /** El sembrado de los bloques de la FASE 2 se intenta una sola vez. */
    private var sembradoIntentado = false

    private var iconLoader: ImageLoader? = null

    private val ratingRepository = RatingRepository()
    private var ratingsAdapter: RatingsAdapter? = null
    private var resenasCargadasPara: String? = null

    // Recoge el resultado de EditarPerfilActivity (layout dialog_editar_perfil)
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(requireContext(), R.string.profile_guardado, Toast.LENGTH_SHORT).show()
            // Se relee para que el badge, el banner y las tiles reflejen el cambio.
            recargarPerfil()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragmento_mi_perfil, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iconLoader = OficioIcons.nuevoImageLoader(requireContext())

        setupTopBar(view)
        setupActionButtons(view)
        setupActividad(view)
        setupTabNavigation(view)
        setupEmptyStateButtons(view)
        ocultarSeccionesDeFasesPosteriores(view)

        // El caché compartido manda sobre la copia local del fragmento: si el
        // perfil se editó desde otra pantalla, ahí está lo más reciente.
        // pintarPerfil trae además el lugar si el modo es contratante.
        val cacheado = ProfileCache.perfil ?: perfil
        perfil = cacheado
        if (cacheado != null) pintarPerfil(view, cacheado) else cargarPerfil()
    }

    override fun onResume() {
        super.onResume()
        // Seamless dark cosmic status bar matching the header gradient
        BarraEstadoUtils.aplicarColor(
            requireActivity(),
            ContextCompat.getColor(requireContext(), R.color.profile_header_dark)
        )
        // Ensure bottom nav is visible when coming back from Settings
        (activity as? MainActivity)?.showBottomNav()
        sincronizarConCacheCompartido()
    }

    /**
     * Repinta si el caché compartido tiene un perfil más nuevo que el pintado.
     *
     * El perfil se puede editar desde "Ajustes", que no es esta pantalla. Al
     * volver, [ProfileCache.perfil] ya trae lo guardado, pero el campo [perfil] de
     * este fragmento seguía con la copia anterior y `onViewCreated` la pintaba
     * sin volver a leer. Peor: como `MainActivity` conserva el fragmento con
     * `by lazy` y la navegación por pestañas usa `hide()`/`show()` en vez de
     * `replace()`, `onViewCreated` tampoco se repite al cambiar de pestaña. Por
     * eso los cambios solo aparecían al reiniciar la app.
     *
     * Se compara contra el caché y no se relee Firestore: el guardado ya publica
     * ahí el documento nuevo, así que el repintado es inmediato y sin red. Tras
     * pintar, [perfil] y el caché vuelven a ser el mismo dato, así que esto no
     * se repite en cada `onResume`.
     */
    private fun sincronizarConCacheCompartido() {
        val root = view ?: return
        val compartido = ProfileCache.perfil ?: return
        if (compartido == perfil) return
        // pintarPerfil (no pintar): el modo pudo cambiar en Ajustes y el lugar
        // del otro modo hay que traerlo de Firestore, no reusar el anterior.
        pintarPerfil(root, compartido)
    }

    // ─────────────────────────────────────────────────────────────
    //  LECTURA
    // ─────────────────────────────────────────────────────────────

    /**
     * Lee `users/{uid}` y pinta la pantalla.
     *
     * El orden importa: primero se LEE y se PINTA, y la escritura va después.
     *
     * Leer `users/{uid}` siempre está autorizado para el propio usuario, así
     * que los datos de la sesión (nombre, correo, foto) aparecen sí o sí. La
     * escritura que crea `worker`, `privacy` y `statistics` es un extra: si esa
     * escritura se rechaza —típico cuando las Rules de la FASE 2 todavía no
     * están publicadas— la pantalla sigue mostrando el perfil en vez de
     * quedarse en blanco con un error.
     */
    private fun cargarPerfil() = leerPerfil(forzar = false)

    /** Vuelve a leer tras guardar: aquí sí se salta el cerrojo de "ya cargando". */
    private fun recargarPerfil() = leerPerfil(forzar = true)

    private fun leerPerfil(forzar: Boolean) {
        val root = view ?: return
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), R.string.profile_error_sesion, Toast.LENGTH_LONG).show()
            return
        }
        if (cargando && !forzar) return
        cargando = true

        viewLifecycleOwner.lifecycleScope.launch {
            val resultado = repository.loadProfile(uid)
            cargando = false
            if (!isAdded) return@launch

            val datos = resultado.getOrElse { error ->
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.profile_error_cargar, motivoCorto(error)),
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            perfil = datos
            ProfileCache.perfil = datos
            pintarPerfil(root, datos)

            // Solo si faltan (las cuentas de la FASE 1 no los tienen) y solo
            // una vez por instancia del fragmento: si las Rules todavía no
            // admiten la FASE 2, reintentar en cada visita solo repetiría el
            // error y el aviso.
            if (!datos.tieneBloquesFase2 && !sembradoIntentado) {
                sembradoIntentado = true
                sembrarBloques(uid)
            }
        }
    }

    /**
     * Crea los bloques de la FASE 2 (`worker`, `privacy`, `statistics`) y el
     * `@usuario` derivado del nombre, la primera vez que se entra al perfil.
     *
     * El perfil ya está pintado cuando esto corre, así que un fallo solo se
     * avisa: no se tira la pantalla. El log deja el motivo real (típicamente
     * `PERMISSION_DENIED` por Rules sin desplegar) para poder diagnosticarlo.
     */
    private fun sembrarBloques(uid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.ensureProfileInitialized(uid)
                .onSuccess { datos ->
                    view?.let { pintarPerfil(it, datos) }
                }
                .onFailure { error ->
                    val motivo = motivoCorto(error)
                    Log.w(TAG, "No se pudieron crear los bloques del perfil: $motivo", error)
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.profile_error_sembrar, motivo),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PINTADO
    // ─────────────────────────────────────────────────────────────

    /**
     * Pinta el perfil y, en vista empresa, reparte el establecimiento en sus
     * secciones (concepto Facebook: perfil personal vs página del negocio).
     *
     * Se pinta dos veces a propósito: primero lo personal (inmediato, también
     * sirve de respaldo si aún no hay lugar) y luego lo del negocio al llegar
     * de Firestore.
     */
    private fun pintarPerfil(root: View, datos: UserProfile) {
        perfil = datos
        ProfileCache.perfil = datos
        lugar = null
        pintar(root, datos)

        if (datos.activeRole == UserRoles.CONTRATANTE) {
            val uid = datos.uid
            viewLifecycleOwner.lifecycleScope.launch {
                val sitio = lugarRepository.loadByOwner(uid).getOrNull()
                if (!isAdded) return@launch
                lugar = sitio
                view?.let { pintar(it, perfil ?: datos) }
            }
        }
    }

    private fun pintar(root: View, datos: UserProfile) {
        val empresa = esVistaEmpresa(datos)
        pintarCabecera(root, datos)
        pintarEstadisticas(root, datos)
        pintarBiografia(root, datos)
        pintarEspecialidades(root, datos.worker.specialties)
        pintarUbicacionYExperiencia(root, datos)
        pintarModoContratante(root, datos)
        pintarFotosTab(root, datos)
        pintarContacto(root, datos)
        pintarIdentidad(root, datos)
        pintarBannerCompletitud(root, datos)
    }

    /** `true` en vista empresa: el contenido se adapta al establecimiento. */
    private fun esVistaEmpresa(datos: UserProfile): Boolean =
        datos.activeRole == UserRoles.CONTRATANTE

    /**
     * Banner de completitud: un SOLO componente con dos estados, no dos pantallas.
     *
     * Decisión de diseño: el banner NO desaparece al llegar al 100%. Ocultarlo
     * borraba justo la prueba del trabajo hecho y dejaba la pantalla con un hueco
     * raro arriba; el anillo a 100% en verde funciona como sello de logro y es lo
     * que el usuario quiere ver después de invertir dos minutos en llenarlo.
     *
     * Lo que sí cambia:
     *  - fondo y borde: violeta (pendiente) → verde (logro);
     *  - título: "Completa tu perfil" → "¡Perfil completado!" con check;
     *  - subtítulo: qué falta → qué ganas por haberlo completado;
     *  - botón "Completar perfil": se oculta, porque sin nada que completar es un
     *    CTA sin destino. "Editar perfil" sigue disponible abajo, así que el
     *    usuario no pierde ninguna acción.
     *
     * El texto del logro es específico del rol: para el trabajador el beneficio es
     * que lo encuentren, y para el contratante es que lo contacten para contratarlo.
     */
    private fun pintarBannerCompletitud(root: View, datos: UserProfile) {
        val card = root.findViewById<View>(R.id.cardCompletaPerfil)
        val check = root.findViewById<View>(R.id.ivBannerCompletado)
        val titulo = root.findViewById<TextView>(R.id.tvBannerTitulo)
        val subtitulo = root.findViewById<TextView>(R.id.tvBannerSubtitulo)
        val boton = root.findViewById<View>(R.id.btnCompletarPerfil)

        val completo = datos.completion().isComplete
        val esContratante = datos.activeRole == UserRoles.CONTRATANTE

        card.setBackgroundResource(
            if (completo) R.drawable.bg_profile_completa_banner_listo
            else R.drawable.bg_profile_completa_banner
        )
        check.isVisible = completo
        boton.isVisible = !completo

        if (completo) {
            titulo.setText(R.string.profile_banner_done_title)
            subtitulo.setText(
                if (esContratante) R.string.profile_banner_done_sub_contratante
                else R.string.profile_banner_done_sub
            )
        } else {
            titulo.setText(R.string.profile_banner_complete_title)
            subtitulo.setText(R.string.profile_banner_complete_sub)
        }
    }

    private fun pintarCabecera(root: View, datos: UserProfile) {
        val perfil = datos.profile
        val completitud = datos.completion()
        val identidadVerificada = datos.identity.identityVerified
        val sitio = lugar.takeIf { esVistaEmpresa(datos) }

        val tvNombre = root.findViewById<TextView>(R.id.tvProfileName)
        // Vista empresa: la identidad principal es el establecimiento
        // ("Restaurante El Pibe", no "Juan Pérez").
        tvNombre.text = sitio?.name?.takeIf { it.isNotBlank() }
            ?: perfil.fullName.ifBlank { getString(R.string.profile_sin_nombre) }
        // El marquee solo se activa si el texto no cabe: con isSelected = true
        // y ellipsize = "marquee", el sistema lo desplaza automáticamente.
        tvNombre.isSelected = true

        root.findViewById<TextView>(R.id.tvProfileHandle).text =
            if (perfil.username.isNotBlank()) {
                "@${perfil.username}"
            } else {
                getString(R.string.profile_sin_username)
            }

        // Avatar: en vista empresa es la foto del establecimiento (y sus
        // iniciales si aún no hay); en personal, la del usuario.
        // La semilla del color es el `@usuario` y, si todavía no hay, el `uid`.
        val fotoUrl = sitio?.photoUrl.orEmpty().ifBlank { perfil.profilePhotoUrl }
        val nombreAvatar = sitio?.name?.takeIf { it.isNotBlank() } ?: perfil.fullName
        OficioIcons.cargarAvatar(
            root.findViewById(R.id.ivProfileAvatar),
            fotoUrl,
            nombreAvatar,
            perfil.username.ifBlank { datos.uid }
        )

        // El sello de verificado depende de la identidad validada en la FASE 1.
        root.findViewById<View>(R.id.ivVerifiedBadge).isVisible = identidadVerificada
        root.findViewById<View>(R.id.cardDniVerificado).isVisible = identidadVerificada

        root.findViewById<TextView>(R.id.tvCompletionBadge).text =
            getString(R.string.profile_completado, completitud.percent)

        actualizarProgresoPerfil(root, completitud.percent)
    }

    /**
     * Actualiza el anillo de progreso circular con color dinámico según el nivel.
     *
     * Colores por rango:
     *  - 0% – 39%   Rojo    (#EF4444)
     *  - 40% – 69%  Ámbar   (#F59E0B)
     *  - 70% – 99%  Azul    (#2563EB)
     *  - 100%       Verde   (#16A34A)
     */
    private fun actualizarProgresoPerfil(root: View, porcentaje: Int) {
        val tvPorcentaje = root.findViewById<TextView>(R.id.tvProgressRing)
        val progressRing = root.findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.progressRing)

        tvPorcentaje.text = getString(R.string.profile_porcentaje, porcentaje)

        val color = when {
            porcentaje >= 100 -> android.graphics.Color.parseColor("#16A34A")
            porcentaje >= 70 -> android.graphics.Color.parseColor("#2563EB")
            porcentaje >= 40 -> android.graphics.Color.parseColor("#F59E0B")
            else -> android.graphics.Color.parseColor("#EF4444")
        }

        progressRing.setIndicatorColor(color)
        tvPorcentaje.setTextColor(color)
        progressRing.setProgress(porcentaje, true)
    }

    private fun pintarEstadisticas(root: View, datos: UserProfile) {
        if (datos.activeRole == com.proyecto.chambaya.data.model.UserRoles.CONTRATANTE) {
            root.findViewById<TextView>(R.id.tvStatsLabel1).text = "Publicaciones"
            root.findViewById<TextView>(R.id.tvStatsLabel2).text = "Contratados"
            root.findViewById<TextView>(R.id.tvStatsLabel3).text = "Calificación"
            root.findViewById<TextView>(R.id.tvPostsCount).text = datos.employer.publishedCount.toString()
            root.findViewById<TextView>(R.id.tvFollowingCount).text = datos.employer.hiredCount.toString()
            root.findViewById<TextView>(R.id.tvFollowersCount).text =
                if (datos.employer.ratingCount > 0) {
                    String.format(Locale.US, "%.1f", datos.employer.ratingAverage)
                } else {
                    getString(R.string.profile_sin_puntuar)
                }
        } else {
            root.findViewById<TextView>(R.id.tvStatsLabel1).text = getString(R.string.profile_posts_label)
            root.findViewById<TextView>(R.id.tvStatsLabel2).text = getString(R.string.profile_following_label)
            root.findViewById<TextView>(R.id.tvStatsLabel3).text = getString(R.string.profile_followers_label)
            root.findViewById<TextView>(R.id.tvPostsCount).text =
                datos.statistics.completedJobsCount.toString()
            root.findViewById<TextView>(R.id.tvFollowingCount).text =
                getString(R.string.profile_anios_experiencia, datos.worker.experienceYears)
            root.findViewById<TextView>(R.id.tvFollowersCount).text =
                if (datos.worker.ratingCount > 0) {
                    String.format(Locale.US, "%.1f", datos.worker.ratingAverage)
                } else {
                    getString(R.string.profile_sin_puntuar)
                }
        }
    }

    private fun pintarModoContratante(root: View, datos: UserProfile) {
        val contratante = datos.activeRole == com.proyecto.chambaya.data.model.UserRoles.CONTRATANTE
        root.findViewById<View>(R.id.cardSkills).isVisible = !contratante
        // cardInfoRow se reutiliza en vista empresa (Distrito + Sector del
        // negocio); por eso ya no se oculta: la repinta pintarUbicacionYExperiencia.
        root.findViewById<View>(R.id.cardInfoRow).isVisible = true
        root.findViewById<View>(R.id.cardExperience).isVisible = !contratante

        root.findViewById<TextView>(R.id.tvHeaderTitle).text =
            if (contratante) "Perfil de contratante" else getString(R.string.profile_title)

        root.findViewById<TextView>(R.id.tvProfileHandle).text =
            if (contratante) {
                datos.employer.commercialName.ifBlank {
                    datos.employer.businessName.ifBlank { "@${datos.profile.username}" }
                }
            } else {
                if (datos.profile.username.isNotBlank()) "@${datos.profile.username}"
                else getString(R.string.profile_sin_username)
            }

        if (contratante && datos.employer.documentNumber.isNotBlank()) {
            root.findViewById<TextView>(R.id.tvDniVerificado).text =
                "${datos.employer.documentType}: ${datos.employer.documentNumber}"
        }
    }

    /**
     * "Sobre mí" en personal, descripción del establecimiento en empresa.
     * Sin lugar (o sin descripción) se respeta la bio personal como respaldo.
     */
    private fun pintarBiografia(root: View, datos: UserProfile) {
        val sitio = lugar.takeIf { esVistaEmpresa(datos) }
        val bio = sitio?.description?.takeIf { it.isNotBlank() } ?: datos.profile.bio
        val tv = root.findViewById<TextView>(R.id.tvBiografia)
        val vacio = root.findViewById<TextView>(R.id.tvSinBiografia)
        val tieneBio = bio.isNotBlank()

        tv.isVisible = tieneBio
        vacio.isVisible = !tieneBio
        tv.text = bio
        vacio.setText(R.string.profile_bio_vacia)
    }

    /**
     * Dibuja hasta [MAX_TILES_VISIBLES] tiles de especialidad.
     *
     * El diseño original era una fila de cuatro tiles con el mismo peso: se
     * conserva esa estructura y se rellena desde `worker.specialties`, con un
     * "+N más" cuando sobran.
     */
    private fun pintarEspecialidades(root: View, especialidades: List<String>) {
        val contenedor = root.findViewById<LinearLayout>(R.id.layoutEspecialidades)
        val aviso = root.findViewById<TextView>(R.id.tvSinEspecialidades)
        val mas = root.findViewById<TextView>(R.id.tvMasEspecialidades)

        contenedor.removeAllViews()

        if (especialidades.isEmpty()) {
            aviso.isVisible = true
            aviso.setText(R.string.profile_especialidades_vacias)
            mas.isVisible = false
            return
        }

        aviso.isVisible = false

        val visibles = especialidades.take(MAX_TILES_VISIBLES)
        val inflater = LayoutInflater.from(root.context)
        val loader = iconLoader

        visibles.forEachIndexed { indice, categoria ->
            val tile = inflater.inflate(
                R.layout.item_especialidad_perfil,
                contenedor,
                false
            )

            // Mismos márgenes que los cuatro tiles fijos del layout original.
            (tile.layoutParams as LinearLayout.LayoutParams).apply {
                when {
                    indice == 0 -> marginEnd = dp(root, 4)
                    indice == visibles.lastIndex -> marginStart = dp(root, 4)
                    else -> {
                        marginStart = dp(root, 2)
                        marginEnd = dp(root, 2)
                    }
                }
                width = 0
                height = dp(root, 86)
                weight = 1f
            }

            tile.findViewById<TextView>(R.id.tvNombre).text = categoria
            OficioIcons.cargar(tile.findViewById<ImageView>(R.id.ivIcono), categoria, loader)
            tile.contentDescription = categoria
            tile.setOnClickListener {
                animateTap(it)
                Toast.makeText(
                    root.context,
                    getString(R.string.profile_especialidad_info, categoria),
                    Toast.LENGTH_SHORT
                ).show()
            }
            contenedor.addView(tile)
        }

        val sobrantes = especialidades.size - visibles.size
        mas.isVisible = sobrantes > 0
        if (sobrantes > 0) mas.text = getString(R.string.profile_mas_especialidades, sobrantes)
    }

    /**
     * Distrito + experiencia en personal; distrito + sector del negocio en
     * empresa (la fila se reutiliza, no se duplica).
     */
    private fun pintarUbicacionYExperiencia(root: View, datos: UserProfile) {
        val perfil = datos.profile
        val worker = datos.worker
        val sitio = lugar.takeIf { esVistaEmpresa(datos) }
        if (sitio != null) {
            root.findViewById<TextView>(R.id.tvDistritoLabel)
                .setText(R.string.profile_district_label)
            root.findViewById<TextView>(R.id.tvDistrito).text =
                sitio.locationLabel.ifBlank { getString(R.string.profile_sin_distrito) }
            root.findViewById<TextView>(R.id.tvExperienciaLabel)
                .setText(R.string.profile_sector_label)
            root.findViewById<TextView>(R.id.tvExperiencia).text =
                sitio.sector.ifBlank {
                    datos.employer.sector.ifBlank { getString(R.string.profile_sin_sector) }
                }
            return
        }
        root.findViewById<TextView>(R.id.tvDistritoLabel)
            .setText(R.string.profile_district_label)
        root.findViewById<TextView>(R.id.tvDistrito).text =
            perfil.locationLabel.ifBlank { getString(R.string.profile_sin_distrito) }
        root.findViewById<TextView>(R.id.tvExperienciaLabel)
            .setText(R.string.profile_experience_label)
        root.findViewById<TextView>(R.id.tvExperiencia).text =
            getString(R.string.profile_anios_experiencia, worker.experienceYears)
    }

    /**
     * Tab Fotos: en vista empresa con foto del establecimiento se muestra en
     * la galería 3x3 (es la "foto de la página"); sin fotos, el vacío actual.
     */
    private fun pintarFotosTab(root: View, datos: UserProfile) {
        val galeria = root.findViewById<RecyclerView>(R.id.rvPhotoGallery)
        val vacio = root.findViewById<View>(R.id.layoutEmptyPhotos)
        val fotoNegocio = lugar.takeIf { esVistaEmpresa(datos) }?.photoUrl.orEmpty()

        if (fotoNegocio.isNotBlank()) {
            vacio.isVisible = false
            galeria.isVisible = true
            if (galeria.layoutManager == null) {
                galeria.layoutManager = GridLayoutManager(requireContext(), 3)
            }
            galeria.adapter = GaleriaFotosAdapter(listOf(fotoNegocio))
        } else {
            galeria.isVisible = false
            vacio.isVisible = true
        }
    }

    /** Galería mínima de una pantalla: N fotos a 3 columnas con Coil. */
    private inner class GaleriaFotosAdapter(
        private val fotos: List<String>
    ) : RecyclerView.Adapter<GaleriaFotosAdapter.Holder>() {

        inner class Holder(val vista: View) : RecyclerView.ViewHolder(vista) {
            val imagen: ImageView = vista.findViewById(R.id.ivPhoto)
        }

        override fun onCreateViewHolder(parent: ViewGroup, tipo: Int): Holder {
            val vista = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_photo_grid, parent, false)
            return Holder(vista)
        }

        override fun getItemCount(): Int = fotos.size

        override fun onBindViewHolder(holder: Holder, posicion: Int) {
            holder.imagen.load(fotos[posicion]) {
                placeholder(R.drawable.ic_profile_photos)
                error(R.drawable.ic_profile_photos)
                crossfade(true)
            }
        }
    }
    /**
     * Teléfono y correo solo se muestran si el usuario lo permitió en el paso 4
     * del wizard. El documento nunca se muestra: es de la FASE 1 y va enmascarado.
     */
    private fun pintarContacto(root: View, datos: UserProfile) {
        val privacidad = datos.privacy
        val perfil = datos.profile

        val hayTelefono = privacidad.showPhone && perfil.phone.isNotBlank()
        val hayCorreo = privacidad.showEmail && datos.auth.email.isNotBlank()

        val layoutTelefono = root.findViewById<View>(R.id.layoutTelefono)
        val layoutEmail = root.findViewById<View>(R.id.layoutEmail)

        layoutTelefono.isVisible = hayTelefono
        layoutEmail.isVisible = hayCorreo

        if (hayTelefono) root.findViewById<TextView>(R.id.tvTelefono).text = perfil.phone
        if (hayCorreo) root.findViewById<TextView>(R.id.tvEmail).text = datos.auth.email

        // El aviso solo aparece cuando no se muestra ningún contacto.
        root.findViewById<View>(R.id.layoutContactoOculto).isVisible = !hayTelefono && !hayCorreo
    }

    private fun pintarIdentidad(root: View, datos: UserProfile) {
        root.findViewById<TextView>(R.id.tvDniVerificado).text =
            datos.identity.verifiedLabel.ifBlank {
                getString(R.string.profile_identidad_pendiente)
            }
    }

    // ─────────────────────────────────────────────────────────────
    //  NAVEGACIÓN
    // ─────────────────────────────────────────────────────────────

    private fun setupTopBar(root: View) {
        // El header de "Mi Perfil" no tiene botón de retroceso: es una pestaña del
        // bottom nav, así que se sale por ahí o con el botón atrás del sistema.
        // Lo único accionable del top bar son los ajustes.

        root.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            abrirAjustes()
        }
    }

    private fun setupActionButtons(root: View) {
        root.findViewById<View>(R.id.btnEditarPerfil)?.setOnClickListener {
            animateTap(it)
            // "Editar perfil" recorre los cuatro pasos desde el principio.
            abrirEdicion(EditarPerfilActivity.PASO_INICIO_EDITAR)
        }

        root.findViewById<View>(R.id.btnCompartirPerfil)?.setOnClickListener {
            animateTap(it)
            compartirPerfil()
        }

        root.findViewById<View>(R.id.btnCompletarPerfil)?.setOnClickListener {
            animateTap(it)
            abrirEdicionEnPasoIncompleto()
        }
    }

    /**
     * Abre el wizard de edición.
     *
     * @param pasoInicial número de paso (1..4) desde el que se empieza.
     */
    private fun abrirEdicion(pasoInicial: Int) {
        editProfileLauncher.launch(
            Intent(requireContext(), EditarPerfilActivity::class.java)
                .putExtra(EditarPerfilActivity.EXTRA_START_STEP, pasoInicial)
        )

        // Transición compatible con todas las versiones de Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requireActivity().overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.dialog_slide_up,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            requireActivity().overridePendingTransition(
                R.anim.dialog_slide_up,
                android.R.anim.fade_out
            )
        }
    }

    /**
     * "Completar perfil": explica exactamente qué falta y lleva al paso que lo resuelve.
     *
     * Antes solo lanzaba un `Toast` con la lista completa de pendientes y abría el
     * primer paso incompleto. Eso tenía dos fallos reales:
     *  - el `Toast` se solapaba con la transición a `EditarPerfilActivity` y a
     *    veces no se leía, así que el usuario no sabía qué corregir;
     *  - en modo CONTRATANTE los campos que faltan (`Tipo de contratante`,
     *    `Identidad del contratante`, `Nombre comercial o negocio`) no están en
     *    ningún paso del asistente, caían en el `else` y el mensaje afirmaba
     *    "Tu perfil está completo" con el perfil incompleto.
     *
     * Ahora el mensaje va en un diálogo (no se pierde con la navegación), solo
     * lista los campos del paso al que se va, y un contratante incompleto se
     * manda a Ajustes, que es donde están esos campos.
     */
    private fun abrirEdicionEnPasoIncompleto() {
        // La copia del fragmento va primero: `sincronizarConCacheCompartido` la
        // mantiene al día, y `ProfileCache` podría venir de otra pantalla.
        val datos = perfil ?: ProfileCache.perfil ?: run {
            abrirEdicion(EditarPerfilActivity.PASO_INICIO_EDITAR)
            return
        }

        val completion = datos.completion()
        if (completion.isComplete) {
            mostrarDialogoPerfilCompleto()
            return
        }

        val faltan = completion.missing
        val esContratante = datos.activeRole == UserRoles.CONTRATANTE
        val mapaPasos = EditarPerfilActivity.camposPorPaso(esContratante)
        val paso = mapaPasos.keys.sorted()
            .firstOrNull { p -> faltan.any { it in mapaPasos.getValue(p) } }
        val tituloPaso = paso?.let { nombreDePaso(it, esContratante) }

        // En CONTRATANTE, lo pendiente del bloque `employer` (tipo, identidad,
        // nombre comercial) no está en este asistente: se dice dónde está en
        // vez de prometer un paso que no contiene esos campos. El lugar SÍ
        // está (paso 3), así que ya no cae aquí.
        if (paso == null && esContratante) {
            mostrarDialogoFaltaContratante(faltan)
            return
        }

        val destino = paso ?: 4
        val camposDelPaso = when (paso) {
            null -> faltan
            else -> faltan.filter { it in mapaPasos.getValue(paso) }
        }
        // Lo que queda para después no se oculta: el mensaje dice dónde empieza el
        // usuario y cuánto le falta en total, no solo lo de esta fase.
        val otros = faltan - camposDelPaso.toSet()

        val mensaje = buildString {
            append("Tu perfil está al ${completion.percent}%.")
            if (camposDelPaso.size == 1) {
                append(" Para llegar al 100% te falta un campo:")
            } else {
                append(" Para llegar al 100% te faltan ${camposDelPaso.size} campos:")
            }
            append("\n\n")
            camposDelPaso.forEach { append("• $it\n") }
            if (otros.isNotEmpty()) {
                append("\nDespués de esta fase, quedará pendiente:\n")
                otros.forEach { append("• $it\n") }
            }
            if (tituloPaso != null) {
                append("\nTe llevamos a: $tituloPaso")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Completa tu perfil")
            .setIcon(R.drawable.ic_home_chevron_down)
            .setMessage(mensaje)
            .setPositiveButton("Completar ahora") { _, _ -> abrirEdicion(destino) }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    /** Nombre legible de cada fase, tal como lo anuncia el asistente. */
    private fun nombreDePaso(paso: Int, esContratante: Boolean): String = when (paso) {
        1 -> "Fase 1 · Información básica"
        2 -> "Fase 2 · Información personal"
        3 -> if (esContratante) "Fase 3 · Mi lugar" else "Fase 3 · Experiencia profesional"
        else -> if (esContratante) {
            "Fase 4 · Datos de contratante"
        } else {
            "Fase 4 · Privacidad y resumen"
        }
    }

    /**
     * Perfil de contratante a medias.
     *
     * Los tres campos que faltan (`employer.*`) los pide `ActivarContratanteActivity`,
     * no el asistente de edición, así que el botón lleva a Ajustes en vez de abrir un
     * paso donde esos campos no existen.
     */
    private fun mostrarDialogoFaltaContratante(faltan: List<String>) {
        val mensaje = buildString {
            append("Tu perfil de contratante todavía está incompleto. Te falta:\n\n")
            faltan.forEach { append("• $it\n") }
            append("\nEstos datos se completan en Ajustes → Datos de contratante.")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Completa tu perfil de contratante")
            .setMessage(mensaje)
            .setPositiveButton("Ir a Ajustes") { _, _ -> abrirAjustes() }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    private fun mostrarDialogoPerfilCompleto() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¡Perfil completo!")
            .setMessage("Ya tienes el 100%. Tu perfil está listo para que otros usuarios lo vean y te contacten.")
            .setPositiveButton("Ver perfil") { _, _ -> abrirEdicion(EditarPerfilActivity.PASO_INICIO_EDITAR) }
            .setNegativeButton("Entendido", null)
            .show()
    }

    private fun abrirAjustes() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.dialog_slide_up,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                R.anim.dialog_slide_down
            )
            .replace(R.id.fragmentContainer, FragmentoAjustesPerfil(), "SETTINGS")
            .addToBackStack("SETTINGS")
            .commit()
    }

    /** Intento de compartir: en empresa se comparte el negocio, no la persona. */
    private fun compartirPerfil() {
        val datos = perfil
        val esEmpresa = datos?.let { esVistaEmpresa(it) } == true
        val usuario = datos?.profile?.username.orEmpty()
        val nombre = if (esEmpresa) {
            lugar?.name?.takeIf { it.isNotBlank() }
                ?: datos?.employer?.businessName
                ?: datos?.profile?.fullName.orEmpty()
        } else {
            datos?.profile?.fullName.orEmpty()
        }

        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.profile_compartir_asunto, usuario))
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.profile_compartir_texto, nombre, usuario)
            )
        }

        if (enviar.resolveActivity(requireContext().packageManager) == null) {
            Toast.makeText(
                requireContext(),
                R.string.profile_compartir_sin_apps,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        startActivity(Intent.createChooser(enviar, getString(R.string.profile_compartir)))
    }

    /**
     * Configura la navegación entre tabs: Sobre Mí, Fotos, Reseñas
     * Muestra/oculta el contenido apropiado y actualiza los estilos de tabs
     */
    private fun setupTabNavigation(root: View) {
        val tabSobreMi = root.findViewById<View>(R.id.tabSobreMi)
        val tabFotos = root.findViewById<View>(R.id.tabFotos)
        val tabResenas = root.findViewById<View>(R.id.tabResenas)

        val contentSobreMi = root.findViewById<View>(R.id.contentTabSobreMi)
        val contentFotos = root.findViewById<View>(R.id.contentTabFotos)
        val contentResenas = root.findViewById<View>(R.id.contentTabResenas)

        tabSobreMi?.setOnClickListener {
            activateTab(
                root,
                it,
                contentSobreMi,
                listOf(tabSobreMi, tabFotos, tabResenas),
                listOf(contentSobreMi, contentFotos, contentResenas)
            )
        }

        tabFotos?.setOnClickListener {
            activateTab(
                root,
                it,
                contentFotos,
                listOf(tabSobreMi, tabFotos, tabResenas),
                listOf(contentSobreMi, contentFotos, contentResenas)
            )
        }

        tabResenas?.setOnClickListener {
            activateTab(
                root,
                it,
                contentResenas,
                listOf(tabSobreMi, tabFotos, tabResenas),
                listOf(contentSobreMi, contentFotos, contentResenas)
            )
            cargarResenas(root)
        }
    }

    /**
     * FASE 17/19 — Accesos de actividad: postulaciones, trabajos y guardados.
     */
    private fun setupActividad(root: View) {
        root.findViewById<View>(R.id.btnActPostulaciones)?.setOnClickListener {
            (activity as? MainActivity)?.irAPublicar(
                com.proyecto.chambaya.ui.publish.FragmentoPublicar.SECCION_SOLICITUDES
            )
        }
        root.findViewById<View>(R.id.btnActTrabajos)?.setOnClickListener {
            val comoEmpleador = (perfil ?: ProfileCache.perfil)?.activeRole == UserRoles.CONTRATANTE
            JobsSheet.newInstance(comoEmpleador).show(parentFragmentManager, "jobs")
        }
        root.findViewById<View>(R.id.btnActGuardados)?.setOnClickListener {
            SavedSheet().show(parentFragmentManager, "saved")
        }
    }

    /**
     * FASE 19 — Reseñas recibidas en el tab Reseñas (antes vacío).
     */
    private fun cargarResenas(root: View) {
        val uid = auth.currentUser?.uid ?: return
        if (resenasCargadasPara == uid && ratingsAdapter?.itemCount != 0) return
        val rv = root.findViewById<RecyclerView>(R.id.rvResenas) ?: return
        val empty = root.findViewById<View>(R.id.layoutEmptyReviews) ?: return
        if (rv.adapter == null) {
            rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            rv.isNestedScrollingEnabled = false
            ratingsAdapter = RatingsAdapter()
            rv.adapter = ratingsAdapter
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val lista = ratingRepository.receivedBy(uid, 30).getOrNull().orEmpty()
            if (!isAdded) return@launch
            resenasCargadasPara = uid
            if (lista.isEmpty()) {
                rv.visibility = View.GONE
                empty.visibility = View.VISIBLE
            } else {
                empty.visibility = View.GONE
                rv.visibility = View.VISIBLE
                // Nombres y fotos de quienes calificaron.
                val autores = mutableMapOf<String, com.proyecto.chambaya.data.model.PublicProfile>()
                lista.map { it.fromUid }.distinct().forEach { id ->
                    repository.loadPublicProfile(id).getOrNull()?.let { autores[id] = it }
                }
                if (!isAdded) return@launch
                ratingsAdapter?.submitList(lista.map { RatingRow(it, autores[it.fromUid]) })
            }
        }
    }

    /**
     * Activa un tab específico, actualiza su estilo y muestra el contenido correspondiente
     */
    private fun activateTab(
        root: View,
        selectedTab: View,
        contentToShow: View?,
        allTabs: List<View?>,
        allContents: List<View?>
    ) {
        allContents.forEach { it?.visibility = View.GONE }
        contentToShow?.visibility = View.VISIBLE

        allTabs.forEachIndexed { indice, tab ->
            if (tab == null) return@forEachIndexed

            val activo = tab == selectedTab
            tab.setBackgroundResource(
                if (activo) R.drawable.bg_profile_tab_active
                else R.drawable.bg_profile_tab_inactive
            )

            val color = ContextCompat.getColor(
                requireContext(),
                if (activo) R.color.brand_color else R.color.profile_text_stat_label
            )
            // `color` ya es el VALOR resuelto (p. ej. 0xFF2E6FF3), no un id de
            // recurso. Pasarlo a `getColorStateList` —que espera un `@ColorRes`—
            // lanzaba `Resources.NotFoundException` y cerraba la app al tocar
            // cualquier tab. Con `valueOf` se arma el ColorStateList del color.
            val iconTint = ColorStateList.valueOf(color)

            when (indice) {
                0 -> {
                    root.findViewById<ImageView>(R.id.iconTabSobreMi)?.imageTintList = iconTint
                    root.findViewById<TextView>(R.id.tvTabSobreMi)?.setTextColor(color)
                }
                1 -> {
                    root.findViewById<ImageView>(R.id.iconTabFotos)?.imageTintList = iconTint
                    root.findViewById<TextView>(R.id.tvTabFotos)?.setTextColor(color)
                }
                2 -> {
                    root.findViewById<ImageView>(R.id.iconTabResenas)?.imageTintList = iconTint
                    root.findViewById<TextView>(R.id.tvTabResenas)?.setTextColor(color)
                }
            }
        }

        animateTap(selectedTab)
    }

    /** Botones de los empty states que aún no tienen datos detrás. */
    private fun setupEmptyStateButtons(root: View) {
        root.findViewById<View>(R.id.btnSubirFoto)?.setOnClickListener {
            animateTap(it)
            // La galería de trabajos es de la FASE 8; por ahora solo se explica.
            Toast.makeText(
                requireContext(),
                R.string.profile_fotos_proximamente,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * `cardExperience` y `cardReviews` muestran datos de la FASE 8/9. Se dejan
     * siempre ocultas para no pintar tarjetas con contenido de maqueta.
     */
    private fun ocultarSeccionesDeFasesPosteriores(root: View) {
        root.findViewById<View>(R.id.cardExperience)?.visibility = View.GONE
        root.findViewById<View>(R.id.cardReviews)?.visibility = View.GONE
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    /**
     * Motivo legible de un fallo de Firestore, en una línea.
     *
     * `Tasks.await` envuelve el error real en un `ExecutionException`, así que
     * sin desenrollar la cadena el `message` sale como
     * "com.google.firebase.firestore.FirebaseFirestoreException: ..." y no
     * dice nada. Con el código (`PERMISSION_DENIED`, `UNAVAILABLE`, ...) sí se
     * puede saber si es un problema de Rules o de red.
     */
    private fun motivoCorto(error: Throwable): String = motivoFirestore(error)

    private fun animateTap(view: View) {
        view.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(70)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun dp(root: View, valor: Int): Int =
        (valor * root.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG = "MiPerfil"

        /** Los tiles no caben todos: se muestran 4 y el resto como "+N más". */
        const val MAX_TILES_VISIBLES = 4
    }
}
