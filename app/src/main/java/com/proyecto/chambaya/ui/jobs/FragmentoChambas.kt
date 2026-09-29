package com.proyecto.chambaya.ui.jobs

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.repository.PublicationInteractionRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStreamReader

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

    // ── Tiempo real: el listener se engancha en onResume y se suelta en
    // onPause. Los flags like/save se cachean por id para no releerlos en
    // cada snapshot; solo se consultan los ids nuevos.
    private var feedListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val stateCache = mutableMapOf<String, Pair<Boolean, Boolean>>()
    private val hiddenCache = mutableSetOf<String>()
    private var statesLoaded = false

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
        setupHeaderActions(view)

        view.findViewById<Button>(R.id.btnFeedRetry)?.setOnClickListener { recargarFeed() }
        view.findViewById<Button>(R.id.btnFeedEmptyAction)?.setOnClickListener {
            if (filters.isEmpty() && filters.query.isBlank() && selectedCategory.isBlank()) recargarFeed()
            else { limpiarFiltros(view); }
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

        loadCategoriasFromJson()
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
            onReport = { }
        )
        recyclerView?.apply {
            adapter = this@FragmentoChambas.adapter
            isNestedScrollingEnabled = false
            setHasFixedSize(true)
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
        if (feedListener != null) return
        if (allItems.isEmpty()) pintarEstado(Estado.CARGANDO)
        feedListener = pubRepo.listenFeed(
            40,
            onUpdate = { pubs -> integrarSnapshot(pubs) },
            onError = { e ->
                if (isAdded) pintarEstado(Estado.ERROR, e.message)
            }
        )
    }

    private fun detachFeed() {
        feedListener?.remove()
        feedListener = null
    }

    /** Fusiona el snapshot con los flags cacheados y repinta. */
    private fun integrarSnapshot(pubs: List<com.proyecto.chambaya.data.model.Publication>) {
        viewLifecycleOwner.lifecycleScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            if (!statesLoaded) {
                val ids = pubs.map { it.publicationId }
                val liked = interRepo.likedIds(ids, uid)
                val saved = interRepo.savedIds(ids, uid)
                hiddenCache.addAll(interRepo.hiddenIds(ids, uid))
                pubs.forEach { stateCache[it.publicationId] = (it.publicationId in liked) to (it.publicationId in saved) }
                statesLoaded = true
            } else if (uid.isNotBlank()) {
                val unknown = pubs.map { it.publicationId }
                    .filter { it !in stateCache && it !in hiddenCache }
                if (unknown.isNotEmpty()) {
                    val liked = interRepo.likedIds(unknown, uid)
                    val saved = interRepo.savedIds(unknown, uid)
                    hiddenCache.addAll(interRepo.hiddenIds(unknown, uid))
                    unknown.forEach { stateCache[it] = (it in liked) to (it in saved) }
                }
            }
            if (!isAdded) return@launch
            allItems = pubs
                .filter { it.publicationId !in hiddenCache }
                .map { p ->
                    val (l, s) = stateCache[p.publicationId] ?: (false to false)
                    PublicationFeedItem(p, l, s, p.statistics.likes, p.statistics.saves)
                }
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        val efectivo = filters.copy(
            category = filters.category.ifBlank { selectedCategory }
        )
        val filtrada = allItems.applyFilters(efectivo)
        adapter?.submitList(filtrada)
        if (filtrada.isEmpty()) {
            val buscando = efectivo.query.isNotBlank() || !efectivo.isEmpty() || selectedCategory.isNotBlank()
            pintarEstado(
                Estado.VACIO,
                if (buscando) "sin_resultados" else null
            )
        } else {
            pintarEstado(Estado.LISTA)
        }
    }

    private enum class Estado { CARGANDO, LISTA, VACIO, ERROR }

    private fun pintarEstado(estado: Estado, detalle: String? = null) {
        recyclerView?.visibility = if (estado == Estado.LISTA) View.VISIBLE else View.GONE
        layoutLoading?.visibility = if (estado == Estado.CARGANDO) View.VISIBLE else View.GONE
        layoutEmpty?.visibility = if (estado == Estado.VACIO) View.VISIBLE else View.GONE
        layoutError?.visibility = if (estado == Estado.ERROR) View.VISIBLE else View.GONE
        if (estado == Estado.VACIO) {
            if (detalle == "sin_resultados") {
                tvEmptyTitle?.text = "Sin resultados"
                tvEmptySub?.text = "Prueba con otra palabra o ajusta los filtros."
                view?.findViewById<Button>(R.id.btnFeedEmptyAction)?.text = "Limpiar filtros"
            } else {
                tvEmptyTitle?.text = "Aún no hay chambas aquí"
                tvEmptySub?.text = "Cuando un contratante publique una chamba, la verás en este feed."
                view?.findViewById<Button>(R.id.btnFeedEmptyAction)?.text = "Recargar"
            }
        }
        if (estado == Estado.ERROR) {
            tvErrorDetail?.text = detalle?.take(120) ?: "Revisa tu conexión e inténtalo de nuevo."
        }
    }

    // ── Búsqueda ────────────────────────────────────────────

    private fun setupSearch(view: View) {
        val et = view.findViewById<EditText>(R.id.etSearch) ?: return
        et.doAfterTextChanged { texto ->
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(350)
                filters = filters.copy(query = texto?.toString().orEmpty())
                aplicarFiltros()
            }
        }
        et.setOnEditorActionListener { v, _, _ ->
            filters = filters.copy(query = v.text?.toString().orEmpty())
            aplicarFiltros()
            true
        }
        view.findViewById<View>(R.id.btnFilter)?.setOnClickListener {
            val cats = allItems.map { it.publication.category }.filter { it.isNotBlank() }.distinct().sorted()
            val dis = allItems.map { it.publication.location.district }.filter { it.isNotBlank() }.distinct().sorted()
            FilterBottomSheet.newInstance(cats, dis, filters).show(parentFragmentManager, "filters")
        }
    }

    private fun setupChips(view: View) {
        val chipAll = view.findViewById<TextView>(R.id.chipAll)
        val chipNewest = view.findViewById<TextView>(R.id.chipNewest)
        val chipPopular = view.findViewById<TextView>(R.id.chipPopular)
        val chipConstruction = view.findViewById<TextView>(R.id.chipConstruction)
        val chips = listOfNotNull(chipAll, chipNewest, chipPopular, chipConstruction)

        fun activar(activo: TextView?) {
            chips.forEach {
                val on = it == activo
                it.setBackgroundResource(if (on) R.drawable.bg_chip_active else R.drawable.bg_chip_inactive)
                it.setTextColor(
                    requireContext().getColor(if (on) R.color.white else R.color.text_primary)
                )
            }
        }
        chipAll?.setOnClickListener {
            selectedCategory = ""
            filters = filters.copy(sortNewestFirst = true)
            activar(chipAll); aplicarFiltros()
        }
        chipNewest?.setOnClickListener {
            selectedCategory = ""
            filters = filters.copy(sortNewestFirst = true)
            activar(chipNewest); aplicarFiltros()
        }
        chipPopular?.setOnClickListener {
            filters = filters.copy(sortNewestFirst = false)
            activar(chipPopular); aplicarFiltros()
        }
        chipConstruction?.setOnClickListener {
            selectedCategory = if (selectedCategory == "Construcción") "" else "Construcción"
            activar(if (selectedCategory.isBlank()) chipAll else chipConstruction)
            aplicarFiltros()
        }
        view.findViewById<View>(R.id.tvSpecialSeeAll)?.setOnClickListener {
            selectedCategory = ""; filters = PublicationFilters()
            view.findViewById<EditText>(R.id.etSearch)?.setText("")
            activar(chipAll); aplicarFiltros()
        }
        view.findViewById<View>(R.id.btnBannerCta)?.setOnClickListener {
            scrollView?.smoothScrollTo(0, recyclerView?.top ?: 0)
        }
    }

    private fun limpiarFiltros(view: View) {
        filters = PublicationFilters()
        selectedCategory = ""
        view.findViewById<EditText>(R.id.etSearch)?.setText("")
        aplicarFiltros()
    }

    private fun setupHeaderActions(view: View) {
        view.findViewById<View>(R.id.btnNotifications)?.setOnClickListener {
            if (FirebaseAuth.getInstance().currentUser?.uid.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Inicia sesión para ver avisos.", Toast.LENGTH_SHORT).show()
            } else {
                NotificationsSheet().show(parentFragmentManager, "notif")
            }
        }
        view.findViewById<View>(R.id.tvCategorySeeAll)?.setOnClickListener {
            Toast.makeText(requireContext(), "Explora las categorías tocando cada tarjeta.", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Categorías (se mantiene la fuente api_oficios.json) ──

    private fun setupCategorias(view: View) {
        rvCategories = view.findViewById(R.id.rvCategories)
        rvCategories?.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }
    }

    private fun loadCategoriasFromJson() {
        try {
            val inputStream = requireContext().assets.open("api_oficios.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val type = object : TypeToken<List<Categoria>>() {}.type
            val categorias: List<Categoria> = gson.fromJson(reader, type)
            reader.close()
            categoriaAdapter = CategoriaAdapter(categorias) { categoria ->
                // Toca la categoría → filtra el feed real por esa categoría.
                selectedCategory = if (selectedCategory == categoria.categoria) "" else categoria.categoria
                aplicarFiltros()
                if (selectedCategory.isNotBlank()) {
                    Toast.makeText(requireContext(), "Filtrando: $selectedCategory", Toast.LENGTH_SHORT).show()
                }
            }
            rvCategories?.adapter = categoriaAdapter
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Interacciones ───────────────────────────────────────

    private fun toggleLike(item: PublicationFeedItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Inicia sesión para dar me gusta.", Toast.LENGTH_SHORT).show()
            return
        }
        // Optimista.
        item.liked = !item.liked
        item.likesCount += if (item.liked) 1 else -1
        stateCache[item.publication.publicationId] = item.liked to item.saved
        adapter?.notifyDataSetChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            val r = interRepo.toggleLike(item.publication.publicationId, uid)
            if (r.isFailure) {
                item.liked = !item.liked
                item.likesCount += if (item.liked) 1 else -1
                stateCache[item.publication.publicationId] = item.liked to item.saved
                adapter?.notifyDataSetChanged()
                Toast.makeText(requireContext(), "No se pudo registrar el me gusta.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleSave(item: PublicationFeedItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Inicia sesión para guardar.", Toast.LENGTH_SHORT).show()
            return
        }
        item.saved = !item.saved
        item.savesCount += if (item.saved) 1 else -1
        stateCache[item.publication.publicationId] = item.liked to item.saved
        adapter?.notifyDataSetChanged()
        viewLifecycleOwner.lifecycleScope.launch {
            val r = interRepo.toggleSave(item.publication.publicationId, uid)
            if (r.isSuccess) {
                Toast.makeText(
                    requireContext(),
                    if (r.getOrDefault(false)) "Guardado en tu lista." else "Quitado de guardados.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                item.saved = !item.saved
                item.savesCount += if (item.saved) 1 else -1
                stateCache[item.publication.publicationId] = item.liked to item.saved
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun compartir(item: PublicationFeedItem) {
        val p = item.publication
        val texto = "📢 ${p.title}\n\n💰 S/ ${p.payment.amount} / ${p.payment.period}\n📍 ${p.location.district}\n\n${p.description.take(280)}\n\n🔗 Compartido desde ChambAYA"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chamba: ${p.title}")
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, "Compartir chamba"))
        viewLifecycleOwner.lifecycleScope.launch { pubRepo.registerShare(p.publicationId) }
    }

    private fun manejarOpcion(action: String, publicationId: String) {
        val item = allItems.firstOrNull { it.publication.publicationId == publicationId } ?: return
        when (action) {
            PublicationOptionsSheet.ACTION_SAVE -> toggleSave(item)
            PublicationOptionsSheet.ACTION_SHARE -> compartir(item)
            PublicationOptionsSheet.ACTION_WHY -> Toast.makeText(
                requireContext(), "Ves esta chamba por tu ubicación (Ayacucho) y tus intereses.",
                Toast.LENGTH_LONG
            ).show()
            PublicationOptionsSheet.ACTION_RATE -> Toast.makeText(
                requireContext(), "Podrás calificar cuando completes un trabajo (Fase 9).",
                Toast.LENGTH_SHORT
            ).show()
            PublicationOptionsSheet.ACTION_HIDE -> ocultar(item)
            PublicationOptionsSheet.ACTION_REPORT -> mostrarDenuncia(item)
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
            Toast.makeText(requireContext(), "Verás menos chambas como esta.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDenuncia(item: PublicationFeedItem) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) {
            Toast.makeText(requireContext(), "Inicia sesión para denunciar.", Toast.LENGTH_SHORT).show()
            return
        }
        val motivos = arrayOf("Fraude o estafa", "Contenido inapropiado", "Información falsa", "Spam", "Otro")
        AlertDialog.Builder(requireContext())
            .setTitle("Denunciar publicación")
            .setItems(motivos) { _, cual ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = interRepo.report(item.publication.publicationId, uid, motivos[cual])
                    Toast.makeText(
                        requireContext(),
                        if (r.isSuccess) "Denuncia enviada. La revisaremos." else "No se pudo enviar la denuncia.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        try {
            BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.brand_color))
        } catch (_: Exception) { }
        // Re-engancha el feed: lo publicado desde Publicar aparece sin
        // cerrar la app.
        if (view != null) attachFeed()
    }

    override fun onPause() {
        detachFeed()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detachFeed()
        stateCache.clear()
        hiddenCache.clear()
        statesLoaded = false
        searchJob?.cancel()
        scrollView = null
        recyclerView = null
        adapter = null
        rvCategories = null
        categoriaAdapter = null
    }
}
