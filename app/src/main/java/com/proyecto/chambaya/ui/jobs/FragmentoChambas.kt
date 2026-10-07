package com.proyecto.chambaya.ui.jobs

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.button.MaterialButton
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.publicationTimeAgo
import com.proyecto.chambaya.data.repository.BlockRepository
import com.proyecto.chambaya.data.repository.JobRepository
import com.proyecto.chambaya.data.repository.NotificationRepository
import com.proyecto.chambaya.data.repository.PublicationInteractionRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.data.repository.RatingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * FASE 6 — Feed de chambas con datos reales de Firestore.
 *
 * Se mantiene el layout existente (header, buscador, banner, categorías,
 * chips): solo se conecta la lógica:
 *  - feed ACTIVE+PUBLIC desde publications
 *  - búsqueda por texto en vivo
 *  - filtros (categoría, distrito, pago, orden) en bottom sheet
 *  - chips Todos / Recientes / Populares (+ categoría rápida)
 *  - like / guardar / compartir / no me interesa / denunciar reales
 *  - tap en tarjeta → detalle; tap en perfil → perfil público
 *  - estados carga / vacío / error
 */
class FragmentoChambas : Fragment() {

    private var scrollView: NestedScrollView? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: PublicationAdapter? = null
    private var rvCategories: RecyclerView? = null
    private var categoriaAdapter: CategoriaAdapter? = null

    private var layoutLoading: View? = null
    private var layoutEmpty: View? = null
    private var layoutError: View? = null
    private var tvErrorDetail: TextView? = null
    private var tvEmptyTitle: TextView? = null
    private var tvEmptySub: TextView? = null

    private val pubRepo = PublicationRepository()
    private val interRepo = PublicationInteractionRepository()

    private var allItems: List<PublicationFeedItem> = emptyList()
    private var filters = PublicationFilters()
    private var searchJob: Job? = null
    private var selectedCategory: String = ""
    private var showingFreeTime: Boolean = false

    // ── Tiempo real: el listener se engancha en onResume y se suelta en
    // onPause. Los flags like/save se cachean por id para no releerlos en
    // cada snapshot; solo se consultan los ids nuevos.
    private val feedListeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
    private val feedByType = mutableMapOf<String, List<com.proyecto.chambaya.data.model.Publication>>()
    private var badgeListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var badgeUid: String? = null
    private val stateCache = mutableMapOf<String, Pair<Boolean, Boolean>>()
    private val hiddenCache = mutableSetOf<String>()
    private val blockedCache = mutableSetOf<String>()
    private var statesLoaded = false
    // Toques rápidos: la UI cambia al instante en cada tap y el servidor se
    // sincroniza en serie por publicación. [likeWant] guarda el estado deseado
    // más reciente; el loop repite el toggle hasta alcanzarlo. Nunca se ignora
    // un tap (eso obligaba a "esperar y reintentar").
    private val likeSync = mutableSetOf<String>()
    private val saveSync = mutableSetOf<String>()
    private val likeWant = mutableMapOf<String, Boolean>()
    private val saveWant = mutableMapOf<String, Boolean>()

    // ── Carrusel de banners (Empleos + Tiempo libre, fijo: no depende del toggle)
    private var vpBanner: ViewPager2? = null
    private var bannerAdapter: BannerPromoAdapter? = null
    private var llBannerDots: LinearLayout? = null
    private var btnTabEmpleos: MaterialButton? = null
    private var btnTabTiempo: MaterialButton? = null
    private val bannerHandler = Handler(Looper.getMainLooper())
    private var bannerPageCallback: ViewPager2.OnPageChangeCallback? = null
    private var bannerAutoScroll = false
    private val bannerAvanzar = object : Runnable {
        override fun run() {
            val vp = vpBanner ?: return
            val total = bannerAdapter?.itemCount ?: 0
            if (bannerAutoScroll && total > 1 && vp.isAttachedToWindow) {
                vp.setCurrentItem((vp.currentItem + 1) % total, true)
            }
            if (bannerAutoScroll) bannerHandler.postDelayed(this, BANNER_INTERVALO_MS)
        }
    }

    companion object {
        private const val BANNER_INTERVALO_MS = 5000L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragmento_chambas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scrollView = view.findViewById(R.id.scrollMain)
        scrollView?.apply {
            setLayerType(View.LAYER_TYPE_NONE, null)
            isNestedScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
            isSmoothScrollingEnabled = true
            isVerticalFadingEdgeEnabled = false
            isHorizontalFadingEdgeEnabled = false
        }

        layoutLoading = view.findViewById(R.id.layoutFeedLoading)
        layoutEmpty = view.findViewById(R.id.layoutFeedEmpty)
        layoutError = view.findViewById(R.id.layoutFeedError)
        tvErrorDetail = view.findViewById(R.id.tvFeedErrorDetail)
        tvEmptyTitle = view.findViewById(R.id.tvFeedEmptyTitle)
        tvEmptySub = view.findViewById(R.id.tvFeedEmptySub)

        setupFeed(view)
        setupCategorias(view)
        setupSearch(view)
        setupChips(view)
        configurarModoFeed(view)
        setupBannerCarousel(view)
        setupHeaderActions(view)

        view.findViewById<Button>(R.id.btnFeedRetry)?.setOnClickListener { recargarFeed() }
        view.findViewById<Button>(R.id.btnFeedEmptyAction)?.setOnClickListener {
            if (filters.isEmpty() && filters.query.isBlank() && selectedCategory.isBlank()) recargarFeed()
            else limpiarFiltros(view)
        }

        // Resultados de sheets hijos.
        parentFragmentManager.setFragmentResultListener(PublicationOptionsSheet.REQUEST, viewLifecycleOwner) { _, bundle ->
            val action = bundle.getString(PublicationOptionsSheet.EXTRA_ACTION).orEmpty()
            val id = bundle.getString(PublicationOptionsSheet.EXTRA_ID).orEmpty()
            manejarOpcion(action, id)
        }
        parentFragmentManager.setFragmentResultListener(FilterBottomSheet.REQUEST, viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean(FilterBottomSheet.EXTRA_CLEAR)) {
                limpiarFiltros(view)
            } else {
                filters = filters.copy(
                    category = bundle.getString(FilterBottomSheet.EXTRA_CAT).orEmpty(),
                    district = bundle.getString(FilterBottomSheet.EXTRA_DIS).orEmpty(),
                    minAmount = bundle.getDouble(FilterBottomSheet.EXTRA_MIN),
                    sortNewestFirst = bundle.getBoolean(FilterBottomSheet.EXTRA_SORT, true)
                )
                // Una sola fuente de verdad para categoría: si el sheet trae
                // categoría, el grid se sincroniza (o vuelve a "Todas").
                selectedCategory = ""
                categoriaAdapter?.setSeleccionada(
                    if (filters.category.isBlank()) "Todas" else filters.category
                )
                aplicarFiltros()
            }
        }
        parentFragmentManager.setFragmentResultListener(NotificationsSheet.REQUEST_OPEN_PUB, viewLifecycleOwner) { _, bundle ->
            val pid = bundle.getString(NotificationsSheet.EXTRA_PUB).orEmpty()
            if (pid.isNotBlank()) {
                JobDetailSheet.newInstance(pid).show(parentFragmentManager, "detail")
            }
        }
        parentFragmentManager.setFragmentResultListener(JobDetailSheet.REQUEST_OPEN_PROFILE, viewLifecycleOwner) { _, bundle ->
            val uid = bundle.getString(JobDetailSheet.EXTRA_UID).orEmpty()
            if (uid.isNotBlank()) PublicProfileSheet.newInstance(uid).show(parentFragmentManager, "profile")
        }

        loadCategorias()
        // Primera escucha inmediata (onResume la re-engancha al volver de Publicar).
        attachFeed()
        // Con caché pinta al instante; sin datos muestra "cargando" (la misma
        // animación de Recargar) hasta el primer snapshot, nunca "vacío".
        if (allItems.isNotEmpty()) aplicarFiltros() else pintarEstado(Estado.CARGANDO)
    }

    // ── Feed ────────────────────────────────────────────────

    private fun setupFeed(view: View) {
        recyclerView = view.findViewById(R.id.rvJobs)
        adapter = PublicationAdapter(
            onOpenDetail = { item ->
                JobDetailSheet.newInstance(item.publication.publicationId)
                    .show(parentFragmentManager, "detail")
            },
            onOpenProfile = { item ->
                val uid = item.publication.publisher.uid.ifBlank { item.publication.ownerUid }
                if (uid.isNotBlank()) PublicProfileSheet.newInstance(uid).show(parentFragmentManager, "profile")
            },
            onToggleLike = { item -> toggleLike(item) },
            onToggleSave = { item -> toggleSave(item) },
            onShare = { item -> compartir(item) },
            onHide = { },
            onReport = { },
            onOpenComments = { item ->
                CommentsSheet.newInstance(item.publication.publicationId)
                    .show(parentFragmentManager, "comments")
            }
        )
        recyclerView?.apply {
            adapter = this@FragmentoChambas.adapter
            isNestedScrollingEnabled = false
            // wrap_content dentro del NestedScrollView: con true se queda en
            // altura 0 al pasar de vacío → con datos (por eso "Todos" quedaba
            // en blanco hasta reiniciar).
            setHasFixedSize(false)
            setItemViewCacheSize(10)
            itemAnimator = null
        }
    }

    private fun cargarFeed(refreshInteractionsOnly: Boolean = false) {
        if (refreshInteractionsOnly) {
            statesLoaded = false
        } else {
            recargarFeed()
            return
        }
        // Refresca solo los flags like/save/ocultos y repinta.
        viewLifecycleOwner.lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val ids = allItems.map { it.publication.publicationId }
            val liked = interRepo.likedIds(ids, uid)
            val saved = interRepo.savedIds(ids, uid)
            hiddenCache.addAll(interRepo.hiddenIds(ids, uid))
            ids.forEach { stateCache[it] = (it in liked) to (it in saved) }
            statesLoaded = true
            if (!isAdded) return@launch
            allItems = allItems
                .filter { it.publication.publicationId !in hiddenCache }
                .map {
                    val (l, s) = stateCache[it.publication.publicationId] ?: (false to false)
                    it.copy(liked = l, saved = s)
                }
            aplicarFiltros()
        }
    }

    /** Re-engancha el listener desde cero (reintentos y primera carga). */
    private fun recargarFeed() {
        detachFeed()
        statesLoaded = false
        attachFeed()
    }

    private fun attachFeed() {
        if (feedListeners.isNotEmpty()) return
        if (allItems.isEmpty()) pintarEstado(Estado.CARGANDO)
        listOf("JOB_OFFER", "WORKER_AVAILABILITY").forEach { type ->
            val listener = pubRepo.listenFeed(
                limit = 40,
                type = type,
                onUpdate = { pubs ->
                    feedByType[type] = pubs
                    integrarSnapshot(feedByType.values.flatten().distinctBy { it.publicationId })
                },
                onError = { e ->
                    android.util.Log.e("FragmentoChambas", "No se pudo cargar el feed $type", e)
                    if (isAdded) pintarEstado(Estado.ERROR)
                }
            )
            feedListeners += listener
        }
    }

    private fun detachFeed() {
        feedListeners.forEach { it.remove() }
        feedListeners.clear()
    }

    /** Fusiona el snapshot con los flags cacheados y repinta. */
    private fun integrarSnapshot(pubs: List<com.proyecto.chambaya.data.model.Publication>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                if (!statesLoaded) {
                    val ids = pubs.map { it.publicationId }
                    // Cada lookup es tolerante a fallos: si Firestore/permiso
                    // falla, se sigue con sets vacíos en vez de dejar el feed
                    // colgado en "cargando".
                    val liked = runCatching { interRepo.likedIds(ids, uid) }.getOrDefault(emptySet())
                    val saved = runCatching { interRepo.savedIds(ids, uid) }.getOrDefault(emptySet())
                    hiddenCache.addAll(runCatching { interRepo.hiddenIds(ids, uid) }.getOrDefault(emptySet()))
                    // FASE 12 · excluye a bloqueados.
                    blockedCache.addAll(
                        runCatching { BlockRepository().myBlocks(uid).getOrNull().orEmpty() }
                            .getOrDefault(emptySet())
                    )
                    pubs.forEach { stateCache[it.publicationId] = (it.publicationId in liked) to (it.publicationId in saved) }
                    statesLoaded = true
                } else if (uid.isNotBlank()) {
                    val unknown = pubs.map { it.publicationId }
                        .filter { it !in stateCache && it !in hiddenCache }
                    if (unknown.isNotEmpty()) {
                        val liked = runCatching { interRepo.likedIds(unknown, uid) }.getOrDefault(emptySet())
                        val saved = runCatching { interRepo.savedIds(unknown, uid) }.getOrDefault(emptySet())
                        hiddenCache.addAll(runCatching { interRepo.hiddenIds(unknown, uid) }.getOrDefault(emptySet()))
                        unknown.forEach { stateCache[it] = (it in liked) to (it in saved) }
                    }
                }
                if (!isAdded) return@launch
                val previos = allItems.associateBy { it.publication.publicationId }
                allItems = pubs
                    .filter { it.publicationId !in hiddenCache }
                    .filter { p ->
                        val owner = p.publisher.uid.ifBlank { p.ownerUid }
                        owner.isBlank() || owner !in blockedCache
                    }
                    .map { p ->
                        val pid = p.publicationId
                        val anterior = previos[pid]
                        // Si hay un toggle en vuelo, el snapshot aún trae el
                        // contador viejo: se conserva el estado optimista para
                        // no parpadear ni revertir el corazón.
                        if (anterior != null && (likeWant.containsKey(pid) || pid in likeSync || saveWant.containsKey(pid) || pid in saveSync)) {
                            PublicationFeedItem(
                                publication = p,
                                liked = anterior.liked,
                                saved = anterior.saved,
                                likesCount = anterior.likesCount.coerceAtLeast(0L),
                                savesCount = anterior.savesCount.coerceAtLeast(0L)
                            )
                        } else {
                            val (l, s) = stateCache[pid] ?: (false to false)
                            PublicationFeedItem(
                                p, l, s,
                                p.statistics.likes.coerceAtLeast(0L),
                                p.statistics.saves.coerceAtLeast(0L)
                            )
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("FragmentoChambas", "integrarSnapshot falló", e)
            } finally {
                if (!isAdded) return@launch
                android.util.Log.d(
                    "FragmentoChambas",
                    "snapshot pubs=${pubs.size} all=${allItems.size} " +
                        "q='${filters.query}' cat='${filters.category}' sel='$selectedCategory'"
                )
                aplicarFiltros()
            }
        }
    }

    private fun aplicarFiltros() {
        val tipoEsperado = if (showingFreeTime) "WORKER_AVAILABILITY" else "JOB_OFFER"
        val efectivo = filters.copy(
            category = filters.category.ifBlank { selectedCategory }
        )
        // Búsqueda simple solo por texto/categoría.
        val filtrada = allItems.filter { it.publication.type == tipoEsperado }.applyFilters(efectivo)
        android.util.Log.d(
            "FragmentoChambas",
            "aplicarFiltros all=${allItems.size} filtrada=${filtrada.size} " +
                "q='${efectivo.query}' cat='${efectivo.category}'"
        )
        adapter?.submitList(filtrada) {
            // Con wrap_content el RecyclerView debe remedirse tras el Diff.
            recyclerView?.requestLayout()
        }
        if (filtrada.isEmpty()) {
            val buscando = efectivo.query.isNotBlank() || !efectivo.isEmpty() || selectedCategory.isNotBlank()
            pintarEstado(
                Estado.VACIO,
                if (buscando) "sin_resultados" else null
            )
        } else {
            pintarEstado(Estado.LISTA)
        }
        pintarBanner()
    }

    /**
     * Carrusel fijo con las 2 tarjetas (Empleos + Tiempo libre).
     *
     * Ya NO cambia con el toggle de feedModeTabs: ambas tarjetas viven en el
     * carrusel y rotan solas cada 5 s (más swipe manual). El toggle solo filtra
     * el feed. Los conteos se actualizan con datos reales vía [pintarBanner].
     */
    private fun setupBannerCarousel(view: View) {
        vpBanner = view.findViewById(R.id.vpBanner)
        llBannerDots = view.findViewById(R.id.llBannerDots)
        val vp = vpBanner ?: return
        bannerAdapter = BannerPromoAdapter(onCtaClick = { esTiempo -> irAModoBanner(esTiempo) }).also {
            it.submitList(
                listOf(
                    BannerPromo(
                        esTiempoLibre = false,
                        badge = getString(R.string.home_banner_badge),
                        titulo = getString(R.string.feed_banner_chambas_titulo),
                        subtitulo = getString(R.string.pub_encuentra_zona),
                        textoCta = getString(R.string.home_banner_cta),
                        legal = getString(R.string.home_banner_legal)
                    ),
                    BannerPromo(
                        esTiempoLibre = true,
                        badge = getString(R.string.home_banner_badge),
                        titulo = getString(R.string.feed_banner_tiempo_titulo),
                        subtitulo = getString(R.string.feed_banner_tiempo_sub),
                        textoCta = getString(R.string.home_banner_cta),
                        legal = getString(R.string.home_banner_legal)
                    )
                )
            )
            vp.adapter = it
        }
        vp.offscreenPageLimit = 1
        runCatching { (vp.getChildAt(0) as? RecyclerView)?.overScrollMode = View.OVER_SCROLL_NEVER }
        // Efecto carrusel: tarjetas separadas + escala sutil al desplazar.
        vp.setPageTransformer(
            BannerCarouselTransformer((14 * resources.displayMetrics.density).toInt())
        )
        pintarDotsBanner(0)
        bannerPageCallback?.let { vp.unregisterOnPageChangeCallback(it) }
        bannerPageCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pintarDotsBanner(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                // Pausa el avance mientras el usuario arrastra; al soltar se retoma.
                bannerHandler.removeCallbacks(bannerAvanzar)
                if (bannerAutoScroll && state == ViewPager2.SCROLL_STATE_IDLE) {
                    bannerHandler.postDelayed(bannerAvanzar, BANNER_INTERVALO_MS)
                }
            }
        }.also(vp::registerOnPageChangeCallback)
        // Pinta los conteos si el feed ya tenía datos.
        pintarBanner()
        iniciarBannerAutoScroll()
    }

    private fun pintarDotsBanner(activo: Int) {
        val dots = llBannerDots ?: return
        val total = bannerAdapter?.itemCount ?: 0
        if (total <= 1) {
            dots.visibility = View.GONE
            return
        }
        dots.visibility = View.VISIBLE
        dots.removeAllViews()
        val density = dots.resources.displayMetrics.density
        repeat(total) { i ->
            val esActivo = i == (activo % total)
            dots.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ((if (esActivo) 10 else 8) * density).toInt(),
                    (8 * density).toInt()
                ).apply {
                    if (i > 0) marginStart = (5 * density).toInt()
                }
                setBackgroundResource(
                    if (esActivo) R.drawable.bg_carousel_dot_active
                    else R.drawable.bg_carousel_dot_inactive
                )
            })
        }
    }

    private fun iniciarBannerAutoScroll() {
        bannerHandler.removeCallbacks(bannerAvanzar)
        if (isAdded && view != null && (bannerAdapter?.itemCount ?: 0) > 1) {
            bannerAutoScroll = true
            bannerHandler.postDelayed(bannerAvanzar, BANNER_INTERVALO_MS)
        }
    }

    private fun detenerBannerAutoScroll() {
        bannerAutoScroll = false
        bannerHandler.removeCallbacks(bannerAvanzar)
    }

    /** CTA del banner: activa el modo de su tarjeta y baja al feed. */
    private fun irAModoBanner(esTiempo: Boolean) {
        if (showingFreeTime != esTiempo) {
            cambiarModo(esTiempo)
        }
        scrollView?.smoothScrollTo(0, recyclerView?.top ?: 0)
    }

    /**
     * Actualiza el carrusel con datos reales del feed (conteos por tipo) y la
     * hora de la publicación más reciente. Ya NO cambia textos con el toggle:
     * las 2 tarjetas son fijas en el carrusel.
     */
    private fun pintarBanner() {
        val v = view ?: return
        val empleos = allItems.count { it.publication.type == "JOB_OFFER" }
        val libres = allItems.count { it.publication.type == "WORKER_AVAILABILITY" }
        bannerAdapter?.actualizarConteos(empleos, libres)
        val newest = allItems.mapNotNull { it.publication.createdAt }.maxOrNull()
        v.findViewById<TextView>(R.id.tvFlashTimer)?.text =
            if (allItems.isEmpty()) "—" else publicationTimeAgo(newest)
    }

    /** Cambia el modo del feed (toggle Empleos / Tiempo libre). */
    private fun cambiarModo(esTiempo: Boolean) {
        if (showingFreeTime == esTiempo) return
        showingFreeTime = esTiempo
        selectedCategory = ""
        filters = filters.copy(category = "")
        categoriaAdapter?.setSeleccionada("Todas")
        pintarTabsModo()
        aplicarFiltros()
    }

    private fun pintarTabsModo() {
        val empleos = btnTabEmpleos ?: return
        val tiempo = btnTabTiempo ?: return
        val azul = requireContext().getColor(R.color.brand_color)
        val gris = requireContext().getColor(R.color.text_secondary)
        empleos.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (showingFreeTime) android.graphics.Color.parseColor("#EEF2F8") else azul
        )
        empleos.setTextColor(if (showingFreeTime) gris else android.graphics.Color.WHITE)
        empleos.iconTint = android.content.res.ColorStateList.valueOf(if (showingFreeTime) gris else android.graphics.Color.WHITE)
        tiempo.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (showingFreeTime) azul else android.graphics.Color.parseColor("#EEF2F8")
        )
        tiempo.setTextColor(if (showingFreeTime) android.graphics.Color.WHITE else gris)
        tiempo.iconTint = android.content.res.ColorStateList.valueOf(if (showingFreeTime) android.graphics.Color.WHITE else gris)
    }

    private fun configurarModoFeed(view: View) {
        btnTabEmpleos = view.findViewById(R.id.tabBusinessJobs)
        btnTabTiempo = view.findViewById(R.id.tabFreeTime)
        btnTabEmpleos?.setOnClickListener { cambiarModo(false) }
        btnTabTiempo?.setOnClickListener { cambiarModo(true) }
        pintarTabsModo()
    }

    private enum class Estado { CARGANDO, LISTA, VACIO, ERROR }

    private fun pintarEstado(estado: Estado, detalle: String? = null) {
        recyclerView?.visibility = if (estado == Estado.LISTA) View.VISIBLE else View.GONE
        layoutLoading?.visibility = if (estado == Estado.CARGANDO) View.VISIBLE else View.GONE
        layoutEmpty?.visibility = if (estado == Estado.VACIO) View.VISIBLE else View.GONE
        layoutError?.visibility = if (estado == Estado.ERROR) View.VISIBLE else View.GONE
        if (estado == Estado.VACIO) {
            when (detalle) {
                "sin_resultados" -> {
                    tvEmptyTitle?.text = getString(R.string.k_feed_sin)
                    tvEmptySub?.text = getString(R.string.k_feed_sin_sub)
                    view?.findViewById<Button>(R.id.btnFeedEmptyAction)?.text = getString(R.string.k_feed_limpiar)
                }
                else -> {
                    tvEmptyTitle?.text = getString(R.string.home_feed_vacio_titulo)
                    tvEmptySub?.text = getString(R.string.home_feed_vacio_sub)
                    view?.findViewById<Button>(R.id.btnFeedEmptyAction)?.text = getString(R.string.home_feed_recargar)
                }
            }
        }
        if (estado == Estado.ERROR) {
            tvErrorDetail?.text = detalle?.take(120) ?: getString(R.string.home_feed_error_sub)
        }
    }

    // ── Búsqueda simple por texto ───────────────────────────

    private fun setupSearch(view: View) {
        val et = view.findViewById<EditText>(R.id.etSearch) ?: return

        et.doAfterTextChanged { texto ->
            val query = texto?.toString().orEmpty()
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(350)
                filters = filters.copy(query = query)
                aplicarFiltros()
            }
        }
        et.setOnEditorActionListener { v, _, _ ->
            filters = filters.copy(query = v.text?.toString().orEmpty())
            aplicarFiltros()
            true
        }
        view.findViewById<View>(R.id.btnFilter)?.setOnClickListener {
            val cats = CategoriasChamba.disponibles
            val dis = (
                com.proyecto.chambaya.data.model.PeruLocations.distritos("Ayacucho", "Huamanga").take(12) +
                    allItems.map { it.publication.location.district }.filter { it.isNotBlank() }
                ).distinct().sorted()
            FilterBottomSheet.newInstance(cats, dis, filters).show(parentFragmentManager, "filters")
        }
    }

    private fun setupChips(view: View) {
        // Los chips horizontales se eliminaron del layout: el filtro por
        // categoría ahora es solo el grid con imágenes. El CTA del banner vive
        // en cada página del carrusel (BannerPromoAdapter), no aquí.
        view.findViewById<View>(R.id.tvSpecialSeeAll)?.setOnClickListener {
            limpiarFiltros(view)
        }
    }

    private fun limpiarFiltros(view: View) {
        filters = PublicationFilters()
        selectedCategory = ""
        categoriaAdapter?.setSeleccionada("Todas")
        view.findViewById<EditText>(R.id.etSearch)?.setText("")
        aplicarFiltros()
    }

    private fun setupHeaderActions(view: View) {
        view.findViewById<View>(R.id.btnNotifications)?.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser?.uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.k_avisos_login), Toast.LENGTH_SHORT).show()
            } else {
                NotificationsSheet().show(parentFragmentManager, "notif")
            }
        }
        actualizarBadge()
    }

    /** FASE 14 · Badge con no leídos (se refresca al volver a la tab). */
    private fun actualizarBadge() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val badge = view?.findViewById<TextView>(R.id.tvNotifBadge) ?: return
        if (uid.isBlank()) {
            badgeListener?.remove()
            badgeListener = null
            badgeUid = null
            badge.visibility = View.GONE
            return
        }
        if (badgeUid == uid && badgeListener != null) return
        badgeListener?.remove()
        badgeUid = uid
        badgeListener = NotificationRepository().listenUnreadCount(
            uid = uid,
            onUpdate = { n ->
                if (!isAdded || view == null) return@listenUnreadCount
            if (n > 0) {
                badge.visibility = View.VISIBLE
                badge.text = if (n > 99) "99+" else n.toString()
            } else {
                badge.visibility = View.GONE
            }
            },
            onError = { error ->
                android.util.Log.w("FragmentoChambas", "No se pudo actualizar el badge de notificaciones", error)
            }
        )
    }

    // ── Categorías (mismas que ChambAYA-APP-main, con fotos locales) ──

    private fun setupCategorias(view: View) {
        rvCategories = view.findViewById(R.id.rvCategories)
        rvCategories?.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }
    }

    private fun loadCategorias() {
        val lista = listOf("Todas") + CategoriasChamba.disponibles
        categoriaAdapter = CategoriaAdapter(lista, "Todas") { elegida ->
            // "Todas" = sin filtro; el resto filtra por nombre. Se limpia
            // filters.category para que no quede un filtro viejo del sheet.
            filters = filters.copy(category = "")
            selectedCategory = if (elegida.equals("Todas", ignoreCase = true)) "" else elegida
            aplicarFiltros()
            if (selectedCategory.isNotBlank()) {
                Toast.makeText(requireContext(), getString(R.string.k_filtrando_fmt, selectedCategory), Toast.LENGTH_SHORT).show()
            }
        }
        rvCategories?.adapter = categoriaAdapter
    }

    // ── Interacciones ───────────────────────────────────────

    private fun buscarItem(pid: String): PublicationFeedItem? =
        allItems.firstOrNull { it.publication.publicationId == pid }

    private fun toggleLike(item: PublicationFeedItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.k_like_login), Toast.LENGTH_SHORT).show()
            return
        }
        val pid = item.publication.publicationId
        // Cambio visual INSTANTÁNEO en cada tap (nunca se bloquea ni se ignora).
        val actual = buscarItem(pid) ?: item
        actual.liked = !actual.liked
        actual.likesCount = (actual.likesCount.coerceAtLeast(0L) + if (actual.liked) 1 else -1).coerceAtLeast(0L)
        stateCache[pid] = actual.liked to actual.saved
        likeWant[pid] = actual.liked
        refrescarFila(pid)
        if (pid !in likeSync) {
            viewLifecycleOwner.lifecycleScope.launch { sincronizarLike(pid, uid) }
        }
    }

    /** Lleva el servidor al estado deseado, un toggle atómico a la vez. */
    private suspend fun sincronizarLike(pid: String, uid: String) {
        likeSync += pid
        try {
            while (likeWant.containsKey(pid)) {
                val deseado = likeWant[pid] ?: break
                val r = interRepo.toggleLike(requireContext(), pid, uid)
                if (!isAdded) return
                if (r.isSuccess) {
                    if (r.getOrDefault(deseado) == likeWant[pid]) {
                        // Servidor ya coincide con lo deseado: listo.
                        likeWant.remove(pid)
                        buscarItem(pid)?.let { stateCache[pid] = it.liked to it.saved }
                    }
                    // Si difiere, el usuario tocó durante la llamada: el while
                    // repite un toggle más sin tocar la UI (ya está al día).
                } else {
                    // Fallo de red/permiso: se anula lo pendiente y se revierte
                    // la UI a la verdad del servidor (mejor esfuerzo).
                    likeWant.remove(pid)
                    val real = runCatching { interRepo.isLiked(pid, uid) }.getOrNull()
                    buscarItem(pid)?.let {
                        if (real != null) it.liked = real
                        else it.liked = !deseado
                        stateCache[pid] = it.liked to it.saved
                    }
                    refrescarFila(pid)
                    Toast.makeText(requireContext(), getString(R.string.k_like_error), Toast.LENGTH_SHORT).show()
                    return
                }
            }
        } finally {
            likeSync -= pid
        }
        // El conteo exacto lo confirma el snapshot en vivo; se repinta por si acaso.
        refrescarFila(pid)
    }

    private fun toggleSave(item: PublicationFeedItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), getString(R.string.k_guardar_login), Toast.LENGTH_SHORT).show()
            return
        }
        val pid = item.publication.publicationId
        val actual = buscarItem(pid) ?: item
        actual.saved = !actual.saved
        actual.savesCount = (actual.savesCount.coerceAtLeast(0L) + if (actual.saved) 1 else -1).coerceAtLeast(0L)
        stateCache[pid] = actual.liked to actual.saved
        saveWant[pid] = actual.saved
        refrescarFila(pid)
        if (pid !in saveSync) {
            viewLifecycleOwner.lifecycleScope.launch { sincronizarSave(pid, uid) }
        }
    }

    private suspend fun sincronizarSave(pid: String, uid: String) {
        saveSync += pid
        try {
            while (saveWant.containsKey(pid)) {
                val deseado = saveWant[pid] ?: break
                val r = interRepo.toggleSave(pid, uid)
                if (!isAdded) return
                if (r.isSuccess) {
                    if (r.getOrDefault(deseado) == saveWant[pid]) {
                        saveWant.remove(pid)
                        buscarItem(pid)?.let { stateCache[pid] = it.liked to it.saved }
                    }
                } else {
                    saveWant.remove(pid)
                    val real = runCatching { interRepo.isSaved(pid, uid) }.getOrNull()
                    buscarItem(pid)?.let {
                        if (real != null) it.saved = real
                        else it.saved = !deseado
                        stateCache[pid] = it.liked to it.saved
                    }
                    refrescarFila(pid)
                    Toast.makeText(requireContext(), getString(R.string.k_like_error), Toast.LENGTH_SHORT).show()
                    return
                }
            }
        } finally {
            saveSync -= pid
        }
        // Un solo aviso con el estado final (no un toast por cada tap).
        buscarItem(pid)?.let {
            refrescarFila(pid)
            Toast.makeText(
                requireContext(),
                if (it.saved) "Guardado en tu lista." else "Quitado de guardados.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** Repinta solo la fila afectada (sin notifyDataSetChanged global). */
    private fun refrescarFila(publicationId: String) {
        val list = adapter?.currentList ?: return
        val idx = list.indexOfFirst { it.publication.publicationId == publicationId }
        if (idx >= 0) adapter?.notifyItemChanged(idx)
    }

    private fun compartir(item: PublicationFeedItem) {
        val p = item.publication
        val texto = getString(R.string.k_compartir_texto, p.title, p.payment.amount.toString(), p.payment.period, p.location.district, p.description.take(280))
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.k_compartir_asunto, p.title))
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.k_compartir_titulo)))
        viewLifecycleOwner.lifecycleScope.launch { pubRepo.registerShare(p.publicationId) }
    }

    private fun manejarOpcion(action: String, publicationId: String) {
        val item = allItems.firstOrNull { it.publication.publicationId == publicationId } ?: return
        when (action) {
            PublicationOptionsSheet.ACTION_SAVE -> toggleSave(item)
            PublicationOptionsSheet.ACTION_SHARE -> compartir(item)
            PublicationOptionsSheet.ACTION_WHY -> AlertDialog.Builder(requireContext())
                .setTitle(R.string.sheet_opciones_porque)
                .setMessage(
                    getString(
                        R.string.k_porque_msg,
                        item.publication.publisher.name.ifBlank { getString(R.string.k_porque_contratante) },
                        item.publication.location.district.ifBlank { "Ayacucho" }
                    )
                )
                .setPositiveButton(R.string.k_comun_entendido, null)
                .show()
            PublicationOptionsSheet.ACTION_RATE -> calificarPublicacion(item)
            PublicationOptionsSheet.ACTION_HIDE -> ocultar(item)
            PublicationOptionsSheet.ACTION_REPORT -> mostrarDenuncia(item)
        }
    }

    /**
     * FASE 11 — "Calificar publicación": si ya completé un trabajo con este
     * contratante y falta mi calificación, la abre; si no, lo explica.
     */
    private fun calificarPublicacion(item: PublicationFeedItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.k_calificar_login), Toast.LENGTH_SHORT).show()
            return
        }
        val ownerUid = item.publication.publisher.uid.ifBlank { item.publication.ownerUid }
        if (ownerUid.isBlank() || ownerUid == uid) {
            Toast.makeText(requireContext(), getString(R.string.k_calificar_propias), Toast.LENGTH_SHORT).show()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val jobs = JobRepository().listByWorker(uid).getOrNull().orEmpty()
                .filter { it.employerUid == ownerUid && it.status == JobStatus.COMPLETED }
            val job = jobs.firstOrNull()
            if (!isAdded) return@launch
            if (job == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.k_calificar_pendiente),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val ya = RatingRepository().existingFor(job.jobId, uid).getOrNull() != null
            if (ya) {
                Toast.makeText(requireContext(), getString(R.string.k_ya_calificaste), Toast.LENGTH_SHORT).show()
            } else {
                RateSheet.newInstance(job.jobId).show(parentFragmentManager, "rate")
            }
        }
    }

    private fun ocultar(item: PublicationFeedItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) return
        viewLifecycleOwner.lifecycleScope.launch {
            interRepo.hide(item.publication.publicationId, uid)
            hiddenCache += item.publication.publicationId
            stateCache.remove(item.publication.publicationId)
            allItems = allItems.filter { it.publication.publicationId != item.publication.publicationId }
            aplicarFiltros()
            Toast.makeText(requireContext(), getString(R.string.k_menos_chambas), Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDenuncia(item: PublicationFeedItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.k_com_denunciar_login), Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_report_publication, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        val reasons = dialogView.findViewById<android.widget.RadioGroup>(R.id.reportReasons)
        val details = dialogView.findViewById<android.widget.EditText>(R.id.reportDetails)

        dialogView.findViewById<View>(R.id.reportCancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.reportSubmit).setOnClickListener {
            val selected = reasons.checkedRadioButtonId
            if (selected == -1) {
                Toast.makeText(requireContext(), getString(R.string.k_reportar_elige_motivo), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val reason = dialogView.findViewById<android.widget.RadioButton>(selected).text.toString()
            val description = details.text?.toString().orEmpty()
            dialogView.findViewById<View>(R.id.reportSubmit).isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = interRepo.report(item.publication.publicationId, uid, reason, description)
                if (!isAdded) return@launch
                Toast.makeText(
                    requireContext(),
                    if (result.isSuccess) getString(R.string.k_com_denunciar_enviar) else getString(R.string.k_denuncia_pub_no),
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
        }
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.94f).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94f).toInt(),
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onResume() {
        super.onResume()
        try {
            BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.brand_color))
        } catch (_: Exception) { }
        // Re-engancha el feed: lo publicado desde Publicar aparece sin
        // cerrar la app.
        if (view != null) attachFeed()
        actualizarBadge()
        iniciarBannerAutoScroll()
    }

    override fun onPause() {
        detenerBannerAutoScroll()
        detachFeed()
        badgeListener?.remove()
        badgeListener = null
        badgeUid = null
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detenerBannerAutoScroll()
        bannerPageCallback?.let { cb -> vpBanner?.unregisterOnPageChangeCallback(cb) }
        bannerPageCallback = null
        vpBanner?.adapter = null
        vpBanner = null
        bannerAdapter = null
        llBannerDots = null
        btnTabEmpleos = null
        btnTabTiempo = null
        detachFeed()
        feedByType.clear()
        badgeListener?.remove()
        badgeListener = null
        badgeUid = null
        stateCache.clear()
        hiddenCache.clear()
        blockedCache.clear()
        likeSync.clear()
        saveSync.clear()
        likeWant.clear()
        saveWant.clear()
        statesLoaded = false
        searchJob?.cancel()
        scrollView = null
        recyclerView = null
        adapter = null
        rvCategories = null
        categoriaAdapter = null
    }
}
