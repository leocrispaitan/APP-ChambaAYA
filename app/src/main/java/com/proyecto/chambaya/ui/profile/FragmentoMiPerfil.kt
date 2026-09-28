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
import coil.ImageLoader
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.EditarPerfilActivity
import com.proyecto.chambaya.MainActivity
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.ProfileBlock
import com.proyecto.chambaya.data.model.StatisticsBlock
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.WorkerBlock
import com.proyecto.chambaya.data.repository.ProfileRepository
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

    /** Evita dos escrituras de inicialización simultáneas. */
    private var cargando = false

    /** El sembrado de los bloques de la FASE 2 se intenta una sola vez. */
    private var sembradoIntentado = false

    private var iconLoader: ImageLoader? = null

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
        setupTabNavigation(view)
        setupEmptyStateButtons(view)
        ocultarSeccionesDeFasesPosteriores(view)

        // El caché compartido manda sobre la copia local del fragmento: si el
        // perfil se editó desde otra pantalla, ahí está lo más reciente.
        val cacheado = ProfileCache.perfil ?: perfil
        perfil = cacheado
        if (cacheado != null) pintar(view, cacheado) else cargarPerfil()
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
        perfil = compartido
        pintar(root, compartido)
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
            pintar(root, datos)

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
                    perfil = datos
                    ProfileCache.perfil = datos
                    view?.let { pintar(it, datos) }
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

    private fun pintar(root: View, datos: UserProfile) {
        pintarCabecera(root, datos)
        pintarEstadisticas(root, datos)
        pintarBiografia(root, datos.profile.bio)
        pintarEspecialidades(root, datos.worker.specialties)
        pintarUbicacionYExperiencia(root, datos.profile, datos.worker)
        pintarModoContratante(root, datos)
        pintarContacto(root, datos)
        pintarIdentidad(root, datos)
        root.findViewById<View>(R.id.cardCompletaPerfil).isVisible = !datos.completion().isComplete
    }

    private fun pintarCabecera(root: View, datos: UserProfile) {
        val perfil = datos.profile
        val completitud = datos.completion()
        val identidadVerificada = datos.identity.identityVerified

        val tvNombre = root.findViewById<TextView>(R.id.tvProfileName)
        tvNombre.text = perfil.fullName.ifBlank { getString(R.string.profile_sin_nombre) }
        // El marquee solo se activa si el texto no cabe: con isSelected = true
        // y ellipsize = "marquee", el sistema lo desplaza automáticamente.
        tvNombre.isSelected = true

        root.findViewById<TextView>(R.id.tvProfileHandle).text =
            if (perfil.username.isNotBlank()) {
                "@${perfil.username}"
            } else {
                getString(R.string.profile_sin_username)
            }

        // Sin foto (cuenta creada con correo y contraseña) se dibujan las
        // iniciales del nombre. La semilla del color es el `@usuario` y, si
        // todavía no hay, el `uid`: así el color no cambia al pedir el nombre.
        OficioIcons.cargarAvatar(
            root.findViewById(R.id.ivProfileAvatar),
            perfil.profilePhotoUrl,
            perfil.fullName,
            perfil.username.ifBlank { datos.uid }
        )

        // El sello de verificado depende de la identidad validada en la FASE 1.
        root.findViewById<View>(R.id.ivVerifiedBadge).isVisible = identidadVerificada
        root.findViewById<View>(R.id.cardDniVerificado).isVisible = identidadVerificada

        root.findViewById<TextView>(R.id.tvCompletionBadge).text =
            getString(R.string.profile_completado, completitud.percent)

        root.findViewById<TextView>(R.id.tvProgressRing).text =
            getString(R.string.profile_porcentaje, completitud.percent)
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
        root.findViewById<View>(R.id.cardInfoRow).isVisible = !contratante
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

    private fun pintarBiografia(root: View, bio: String) {
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

    private fun pintarUbicacionYExperiencia(
        root: View,
        perfil: ProfileBlock,
        worker: WorkerBlock
    ) {
        root.findViewById<TextView>(R.id.tvDistrito).text =
            perfil.locationLabel.ifBlank { getString(R.string.profile_sin_distrito) }

        root.findViewById<TextView>(R.id.tvExperiencia).text =
            getString(R.string.profile_anios_experiencia, worker.experienceYears)
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
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.dialog_slide_up,   // enter
                    android.R.anim.fade_out,  // exit
                    android.R.anim.fade_in,   // popEnter
                    R.anim.dialog_slide_down  // popExit
                )
                .replace(R.id.fragmentContainer, FragmentoAjustesPerfil(), "SETTINGS")
                .addToBackStack("SETTINGS")
                .commit()
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
            // "Completar perfil" entra por el paso 3, donde están las
            // especialidades: es lo que más pesa en el porcentaje.
            abrirEdicion(EditarPerfilActivity.PASO_INICIO_COMPLETAR)
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

    /** Intento de compartir: el `@usuario` es el identificador público del perfil. */
    private fun compartirPerfil() {
        val usuario = perfil?.profile?.username.orEmpty()
        val nombre = perfil?.profile?.fullName.orEmpty()

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
