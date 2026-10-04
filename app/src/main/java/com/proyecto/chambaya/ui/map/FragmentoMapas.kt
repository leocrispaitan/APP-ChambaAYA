package com.proyecto.chambaya.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.load
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.OficioCatalog
import com.proyecto.chambaya.data.model.Workplace
import com.proyecto.chambaya.data.model.precioTexto
import com.proyecto.chambaya.data.remote.GeoPlace
import com.proyecto.chambaya.data.remote.GeoSearchService
import com.proyecto.chambaya.data.repository.BlockRepository
import com.proyecto.chambaya.data.repository.PublicationInteractionRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import com.proyecto.chambaya.ui.jobs.CategoriasChamba
import com.proyecto.chambaya.ui.jobs.JobDetailSheet
import com.proyecto.chambaya.ui.jobs.categoriaCoincide
import com.proyecto.chambaya.ui.profile.OficioIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

class FragmentoMapas : Fragment() {

    private var mapView: MapView? = null
    private var maplibreMap: MapLibreMap? = null
    private var isSheetExpanded: Boolean = true

    // UI Header elements
    private var tvHeaderTitle: TextView? = null
    private var tvHeaderSubtitle: TextView? = null

    // ── Capa de datos reales (chambas de Firestore) ──
    private val pubRepo = PublicationRepository()
    private val placeRepo = WorkplaceRepository()
    private val interRepo = PublicationInteractionRepository()
    private val blockRepo = BlockRepository()
    private val workplaceCache = mutableMapOf<String, Workplace?>()
    private var allPins: List<MapPin> = emptyList()
    private var selectedCategory = ""
    private var selectedPubId: String? = null
    private var userRefPoint = MapGeo.AYACUCHO
    private var hasGpsFix = false
    /** Punto del mapa donde se buscó ("Buscar en esta zona"); null = GPS/ciudad. */
    private var zonaMapa: LatLng? = null
    /** Etiqueta real del lugar (calle/distrito) mostrada bajo el título. */
    private var zonaLabel: String? = null
    private var userDot: org.maplibre.android.annotations.Marker? = null
    private var placeMarker: org.maplibre.android.annotations.Marker? = null
    private var cameraFitted = false
    private var dataLoading = false
    private var dataLoaded = false
    private var lastShownCount = 0

    private var rvMapPlaces: RecyclerView? = null
    private var placeAdapter: MapPlaceAdapter? = null
    private var progressMapPlaces: ProgressBar? = null
    private var tvMapEmpty: TextView? = null
    private var rowMapCategories: LinearLayout? = null
    private var overlayContainer: View? = null
    private var mapMarkers = mutableListOf<org.maplibre.android.annotations.Marker>()
    private var markerPubIds = HashMap<org.maplibre.android.annotations.Marker, String>()
    private var svgLoader: ImageLoader? = null

    // ── Buscador de calles/lugares (Geocode Earth · Pelias) ──
    private val geoService = GeoSearchService()
    private var geoAdapter: GeoPlaceAdapter? = null
    private var etPlaceSearch: EditText? = null
    private var searchBarCard: View? = null
    private var geoSuggestionsCard: View? = null
    private var rvGeoSuggestions: RecyclerView? = null
    private var progressGeoSearch: ProgressBar? = null
    private var btnClearSearch: View? = null
    private var geoSearchJob: Job? = null

    // MapTiler Cloud Configuration (Satellite Hybrid Vector Tiles)
    private val maptilerApiKey = "1Yoce1uTsAXtugiPDqc5"
    private val maptilerHybridStyle = "https://api.maptiler.com/maps/hybrid-v4/style.json?key=$maptilerApiKey"

    // OpenRouteService (ORS) API Key for Real Turn-by-Turn Routing & Geocoding
    private val orsApiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjVlYzJjYWYxZTE0YjQ5MzE4YTE1ZGI3ZWRiMDBlZTMzIiwiaCI6Im11cm11cjY0In0="
    private var activeRoutePolyline: Polyline? = null
    /** Evita que una ruta vieja (tap anterior) pise a la recién pedida. */
    private var routeRequestId = 0L
    /** Igual para el geocodificador inverso: solo vale la última respuesta. */
    private var reverseSeq = 0L

    // Punto de referencia de la ciudad (los distritos viven en MapGeo).
    private val ayacuchoCenterLatLng = LatLng(-13.1631, -74.2236) // Plaza Mayor de Ayacucho

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            maplibreMap?.let { map ->
                map.style?.let { style ->
                    enableLocationComponent(map, style)
                }
            }
            moveToCurrentLocation()
        } else {
            Toast.makeText(
                requireContext(),
                "Ubicación por GPS desactivada. Mostrando Ayacucho, Perú.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize MapLibre before layout inflation
        MapLibre.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmento_mapas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyWhiteStatusBar()

        // Header Views
        tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle)
        tvHeaderSubtitle = view.findViewById(R.id.tvHeaderSubtitle)
        tvHeaderTitle?.text = "Ayacucho, Perú"
        tvHeaderSubtitle?.text = "Plaza Mayor • Huamanga"

        // Adjust topBar padding for status bar window insets
        val topBarContainer = view.findViewById<View>(R.id.topBarContainer)
        topBarContainer?.let { topBar ->
            ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
                val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                val baseTopPadding = (8 * resources.displayMetrics.density).toInt()
                val bottomPadding = (12 * resources.displayMetrics.density).toInt()
                v.updatePadding(
                    top = statusBar.top + baseTopPadding,
                    bottom = bottomPadding
                )
                insets
            }
        }

        // Initialize MapView
        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)

        mapView?.getMapAsync { map ->
            maplibreMap = map

            // Disable default compass needle to prevent overlapping status bar or custom action buttons
            map.uiSettings.apply {
                isCompassEnabled = false
                isLogoEnabled = false
                isAttributionEnabled = false
            }

            // Initial camera position centered on Ayacucho, Perú
            map.cameraPosition = CameraPosition.Builder()
                .target(ayacuchoCenterLatLng)
                .zoom(14.5)
                .build()

            // Load MapTiler Satellite Hybrid Vector Style
            loadMapStyle(map)

            // Taps en pines: selecciona la chamba (overlay + ruta).
            // Tap en fondo: muestra la más cercana o esconde el overlay.
            map.setOnMarkerClickListener { marker ->
                val pid = markerPubIds[marker]
                if (pid != null) {
                    // Segundo tap sobre lo ya marcado → detalle.
                    if (selectedPubId == pid) abrirDetalle(pid)
                    else seleccionarPin(pid, withRoute = true)
                }
                false
            }
            map.addOnMapClickListener { point ->
                etPlaceSearch?.clearFocus()
                hideKeyboard()
                onMapBackgroundTap(point)
                true
            }
            map.addOnCameraIdleListener { onCameraIdle() }

            // Center to live GPS if permission granted
            if (hasLocationPermission()) {
                moveToCurrentLocation()
            }
        }

        // Bottom Sheet and Toggle Button Setup
        val cardBottomSheet = view.findViewById<View>(R.id.cardBottomSheet)
        val btnToggleBottomSheet = view.findViewById<View>(R.id.btnToggleBottomSheet)
        val ivToggleSheetIcon = view.findViewById<ImageView>(R.id.ivToggleSheetIcon)
        val viewDragHandle = view.findViewById<View>(R.id.viewDragHandle)
        val btnMyLocation = view.findViewById<View>(R.id.btnMyLocation)

        fun toggleSheet() {
            if (cardBottomSheet == null) return
            isSheetExpanded = !isSheetExpanded
            val targetY = if (isSheetExpanded) 0f else cardBottomSheet.height.toFloat()
            cardBottomSheet.animate()
                .translationY(targetY)
                .setDuration(320)
                .setInterpolator(DecelerateInterpolator())
                .start()

            ivToggleSheetIcon?.setImageResource(
                if (isSheetExpanded) R.drawable.ic_collapse_sheet else R.drawable.ic_expand_sheet
            )

            val msg = if (isSheetExpanded) "Información visible" else "Mapa completo (Calles y Satélite)"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        btnToggleBottomSheet?.setOnClickListener {
            toggleSheet()
        }

        viewDragHandle?.setOnClickListener {
            toggleSheet()
        }

        btnMyLocation?.setOnClickListener {
            checkLocationAndNavigate()
        }

        // Lista de chambas cercanas (diseño de tarjetas del mapa).
        rvMapPlaces = view.findViewById(R.id.rvMapPlaces)
        progressMapPlaces = view.findViewById(R.id.progressMapPlaces)
        tvMapEmpty = view.findViewById(R.id.tvMapEmpty)
        rowMapCategories = view.findViewById(R.id.rowMapCategories)
        overlayContainer = view.findViewById(R.id.propertyOverlayContainer)
        rvMapPlaces?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
        placeAdapter = MapPlaceAdapter(
            onLocate = { pin -> seleccionarPin(pin.publication.publicationId, withRoute = true) }
        )
        rvMapPlaces?.adapter = placeAdapter

        // Overlay de selección: tap → detalle; "Cómo llegar" → redibuja la ruta.
        view.findViewById<View>(R.id.cardActivePlace)?.setOnClickListener {
            selectedPubId?.let { abrirDetalle(it) }
        }
        view.findViewById<View>(R.id.btnOverlayRoute)?.setOnClickListener {
            val pin = allPins.firstOrNull { it.publication.publicationId == selectedPubId }
            if (pin != null) {
                val userPos = getUserCurrentLatLng() ?: userRefPoint
                calculateAndDrawRoute(userPos, pin.position, pin.publication.title)
            }
        }

        // Pill "Buscar en esta zona".
        view.findViewById<View>(R.id.btnSearchArea)?.setOnClickListener {
            buscarEnZonaVisible()
        }

        // Buscador de calles, lugares y restaurantes (Geocode Earth · Pelias).
        setupPlaceSearch()

        // Request location permission silently if not already granted
        requestLocationPermissionSilently()

        // El pin falso centrado en pantalla se apaga: la ubicación real la
        // marca el pin verde anclado al mapa; la tarjeta es solo informativa.
        // (Antes el pin falso hacía creer que la chamba estaba en el centro
        // de la pantalla aunque el marcador real estuviera en otro lado.)
        view.findViewById<View>(R.id.ivMainHousePin)?.visibility = View.GONE
        view.findViewById<View>(R.id.radarDot)?.visibility = View.GONE

        // Carga las chambas reales (pines + categorías + lista).
        cargarDatos()

        // Desde el detalle abierto aquí: ver perfil del publicador.
        parentFragmentManager.setFragmentResultListener(
            JobDetailSheet.REQUEST_OPEN_PROFILE, viewLifecycleOwner
        ) { _, bundle ->
            val uid = bundle.getString(JobDetailSheet.EXTRA_UID).orEmpty()
            if (uid.isNotBlank()) {
                com.proyecto.chambaya.ui.jobs.PublicProfileSheet.newInstance(uid)
                    .show(parentFragmentManager, "profile")
            }
        }
    }

    private fun abrirDetalle(publicationId: String) {
        if (publicationId.isBlank() || !isAdded) return
        JobDetailSheet.newInstance(publicationId).show(parentFragmentManager, "detail")
    }

    // ── Buscador de lugares: Geocode Earth (Pelias) ───────────
    //
    // `autocomplete` mientras se escribe (calles con typos, restaurantes,
    // direcciones) y `search` al pulsar Enter. Al elegir un resultado se
    // centra el mapa, se marca el lugar y la lista de chambas se reordena
    // por cercanía a ese punto.

    private fun setupPlaceSearch() {
        val root = view ?: return
        searchBarCard = root.findViewById(R.id.searchBarCard)
        geoSuggestionsCard = root.findViewById(R.id.geoSuggestionsCard)
        rvGeoSuggestions = root.findViewById(R.id.rvGeoSuggestions)
        progressGeoSearch = root.findViewById(R.id.progressGeoSearch)
        btnClearSearch = root.findViewById(R.id.btnClearSearch)

        val et = root.findViewById<EditText>(R.id.etPlaceSearch) ?: return
        etPlaceSearch = et

        geoAdapter = GeoPlaceAdapter { place -> onPlacePicked(place) }
        rvGeoSuggestions?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = geoAdapter
            itemAnimator = null
        }

        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val q = s?.toString().orEmpty()
                btnClearSearch?.visibility = if (q.isBlank()) View.GONE else View.VISIBLE
                scheduleAutocomplete(q)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        et.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchPlacesNow(et.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }

        btnClearSearch?.setOnClickListener { limpiarBuscador() }
    }

    /** Autocompletado con debounce: no quema cuota al teclear rápido. */
    private fun scheduleAutocomplete(query: String) {
        geoSearchJob?.cancel()
        val q = query.trim()
        if (q.length < MIN_GEO_CHARS) {
            hideSuggestions()
            progressGeoSearch?.visibility = View.GONE
            return
        }
        geoSearchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(GEO_DEBOUNCE_MS)
            progressGeoSearch?.visibility = View.VISIBLE
            val results = geoService.autocomplete(q, userRefPoint.latitude, userRefPoint.longitude)
            progressGeoSearch?.visibility = View.GONE
            if (!isAdded) return@launch
            showGeoResults(results.getOrNull().orEmpty())
        }
    }

    /** Enter / botón del teclado: búsqueda completa. */
    private fun searchPlacesNow(query: String) {
        geoSearchJob?.cancel()
        val q = query.trim()
        if (q.isEmpty()) {
            hideSuggestions()
            return
        }
        geoSearchJob = viewLifecycleOwner.lifecycleScope.launch {
            progressGeoSearch?.visibility = View.VISIBLE
            val results = geoService.search(q, userRefPoint.latitude, userRefPoint.longitude)
            progressGeoSearch?.visibility = View.GONE
            if (!isAdded) return@launch
            val places = results.getOrNull().orEmpty()
            if (places.isEmpty()) {
                hideSuggestions()
                Toast.makeText(
                    requireContext(),
                    "No encontramos ese lugar. Prueba con otra calle.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showGeoResults(places)
            }
        }
    }

    private fun showGeoResults(places: List<GeoPlace>) {
        if (!isAdded) return
        if (places.isEmpty()) {
            hideSuggestions()
            return
        }
        geoAdapter?.submitList(places)
        geoSuggestionsCard?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(140).start()
        }
    }

    private fun hideSuggestions() {
        geoSuggestionsCard?.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val et = etPlaceSearch ?: return
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(et.windowToken, 0)
    }

    /** Lugar elegido: centra el mapa, lo marca y reordena las chambas. */
    private fun onPlacePicked(place: GeoPlace) {
        if (!isAdded) return
        hideSuggestions()
        hideKeyboard()
        etPlaceSearch?.clearFocus()

        val pos = LatLng(place.latitude, place.longitude)
        zonaMapa = pos
        userRefPoint = pos
        zonaLabel = place.label.ifBlank { place.name }
        view?.findViewById<View>(R.id.btnSearchArea)?.visibility = View.GONE

        tvHeaderTitle?.text = place.name.ifBlank { place.label }
        tvHeaderSubtitle?.text = zonaLabel.orEmpty()

        marcarLugarBuscado(pos)
        maplibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, GEO_ZOOM), 900)
        if (dataLoaded) actualizarDistanciasYLista()
    }

    /** Pin del lugar buscado (para no perderlo al mover la cámara). */
    private fun marcarLugarBuscado(pos: LatLng) {
        val map = maplibreMap ?: return
        try {
            placeMarker?.let { runCatching { map.removeMarker(it) } }
            val bmp = vectorToBitmap(R.drawable.ic_home_marker, 48) ?: return
            val icon = org.maplibre.android.annotations.IconFactory.getInstance(requireContext())
                .fromBitmap(bmp)
            placeMarker = map.addMarker(MarkerOptions().position(pos).icon(icon))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun limpiarBuscador() {
        etPlaceSearch?.setText("")
        geoSearchJob?.cancel()
        progressGeoSearch?.visibility = View.GONE
        hideSuggestions()
        hideKeyboard()
        etPlaceSearch?.clearFocus()
        // El lugar buscado deja de ser el centro de la búsqueda.
        zonaMapa = null
        zonaLabel = null
        placeMarker?.let { m -> maplibreMap?.let { runCatching { it.removeMarker(m) } } }
        placeMarker = null
        activeRoutePolyline?.let { p -> maplibreMap?.let { runCatching { it.removePolyline(p) } } }
        activeRoutePolyline = null
        tvHeaderTitle?.text = "Ayacucho, Perú"
        refreshPinsAndList()
    }

    private fun loadMapStyle(map: MapLibreMap) {
        val styleBuilder = Style.Builder().fromUri(maptilerHybridStyle)
        map.setStyle(styleBuilder) { style ->
            if (hasLocationPermission()) {
                enableLocationComponent(map, style)
            }
        }
    }

    // ── Pines dinámicos (uno por chamba activa) ────────────────

    private fun dibujarPines(pins: List<MapPin>) {
        val map = maplibreMap ?: return
        try {
            mapMarkers.forEach { runCatching { map.removeMarker(it) } }
            mapMarkers.clear()
            markerPubIds.clear()
            val icon = pinIcon() ?: return
            pins.forEach { pin ->
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(pin.position)
                        .title(pin.publication.title)
                        .snippet("${pin.publication.category} · ${pin.publication.precioTexto()}")
                        .icon(icon)
                )
                mapMarkers += marker
                markerPubIds[marker] = pin.publication.publicationId
            }
            // Primera carga: encuadra todos los pines.
            if (!cameraFitted) {
                cameraFitted = true
                if (pins.isNotEmpty()) {
                    val bounds = LatLngBounds.Builder()
                    pins.forEach { bounds.include(it.position) }
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds.build(), 140, 180, 140, 420),
                        1200
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var cachedPinIcon: org.maplibre.android.annotations.Icon? = null

    private fun pinIcon(): org.maplibre.android.annotations.Icon? {
        cachedPinIcon?.let { return it }
        return try {
            val bmp = BitmapFactory.decodeResource(resources, R.drawable.ic_map_main_pin) ?: return null
            // Escala del pin al ~60% para no tapar el mapa.
            val w = (bmp.width * 0.62).toInt().coerceAtLeast(1)
            val h = (bmp.height * 0.62).toInt().coerceAtLeast(1)
            val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, w, h, true)
            org.maplibre.android.annotations.IconFactory.getInstance(requireContext())
                .fromBitmap(scaled).also { cachedPinIcon = it }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addAyacuchoMarkers(map: MapLibreMap) {
        // Compatibilidad: los pines ahora salen de Firestore (dibujarPines).
        // Se conserva el método por si se necesita una capa base de la ciudad.
    }

    // ── Datos reales: chambas → pines + categorías + lista ─────

    private fun cargarDatos() {
        if (dataLoading || !isAdded) return
        if (view == null) return
        dataLoading = true
        progressMapPlaces?.visibility = View.VISIBLE
        tvMapEmpty?.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val pubs = pubRepo.feed(60).getOrNull().orEmpty()
            val ids = pubs.map { it.publicationId }
            val hidden = interRepo.hiddenIds(ids, uid)
            val blocked = blockRepo.myBlocks(uid).getOrNull().orEmpty()
            val valid = pubs.filter { p ->
                if (p.publicationId in hidden) return@filter false
                val owner = p.publisher.uid.ifBlank { p.ownerUid }
                owner.isBlank() || owner !in blocked
            }
            // Coordenadas del lugar cuando la chamba no trae propias.
            valid.mapNotNull { it.workplaceId.ifBlank { null } }.distinct().forEach { wid ->
                if (!workplaceCache.containsKey(wid)) {
                    workplaceCache[wid] = placeRepo.loadById(wid).getOrNull()
                }
            }
            val pins = valid.map { pub ->
                val w = workplaceCache[pub.workplaceId]
                val wpos = w?.let {
                    val la = it.location.latitude
                    val lo = it.location.longitude
                    if (la != null && lo != null) LatLng(la, lo) else null
                }
                MapGeo.resolve(pub, wpos).apply {
                    distanceKm = MapGeo.distanceKm(userRefPoint, position)
                }
            }
            withContext(Dispatchers.Main) {
                if (!isAdded) {
                    dataLoading = false
                    return@withContext
                }
                dataLoading = false
                allPins = pins
                dataLoaded = true
                progressMapPlaces?.visibility = View.GONE
                pintarCategorias()
                refreshPinsAndList()
            }
        }
    }

    private fun svgImageLoader(): ImageLoader {
        return svgLoader ?: ImageLoader.Builder(requireContext())
            .components { add(SvgDecoder.Factory()) }
            .build().also { svgLoader = it }
    }

    private fun pintarCategorias() {
        val row = rowMapCategories ?: return
        if (!isAdded) return
        row.removeAllViews()
        fun norm(s: String) = OficioCatalog.normalizar(s)

        tarjetaCategoria(
            nombre = "Todas",
            categoria = "Todas",
            iconoUrl = "",
            conteo = allPins.size,
            seleccionada = selectedCategory.isBlank()
        ) {
            selectedCategory = ""
            pintarCategorias()
            refreshPinsAndList()
        }?.let { row.addView(it) }

        CategoriasChamba.disponibles.forEach { categoria ->
            val key = norm(categoria)
            val conteo = allPins.count { categoriaCoincide(categoria, it.publication.category) }
            tarjetaCategoria(
                nombre = categoria,
                categoria = categoria,
                iconoUrl = OficioCatalog.load(requireContext())
                    .firstOrNull { categoriaCoincide(categoria, it.categoria) }
                    ?.icono.orEmpty(),
                conteo = conteo,
                seleccionada = key == selectedCategory
            ) {
                selectedCategory = if (selectedCategory == key) "" else key
                pintarCategorias()
                refreshPinsAndList()
            }?.let { row.addView(it) }
        }
    }

    private fun tarjetaCategoria(
        nombre: String,
        categoria: String,
        iconoUrl: String,
        conteo: Int,
        seleccionada: Boolean,
        alPulsar: () -> Unit
    ): View? {
        if (!isAdded) return null
        val card = layoutInflater.inflate(R.layout.item_map_category, rowMapCategories, false)
            as? com.google.android.material.card.MaterialCardView ?: return null
        val iv = card.findViewById<ImageView>(R.id.ivMapCatIcon)
        val tvName = card.findViewById<TextView>(R.id.tvMapCatName)
        val tvCount = card.findViewById<TextView>(R.id.tvMapCatCount)
        val categoriaCatalogo = OficioCatalog.load(requireContext())
            .firstOrNull { categoriaCoincide(categoria, it.categoria) }
        val iconoLocal = categoriaCatalogo?.let { OficioIcons.local(it.categoria) }
            ?: OficioIcons.local(categoria)
        tvName.text = nombre
        tvCount.text = if (conteo == 1) "1 chamba" else "$conteo chambas"
        if (iconoUrl.isNotBlank()) {
            iv.setImageResource(iconoLocal)
            val req = ImageRequest.Builder(requireContext())
                .data(iconoUrl)
                .target(iv)
                .placeholder(iconoLocal)
                .error(iconoLocal)
                .build()
            svgImageLoader().enqueue(req)
        } else {
            iv.setImageResource(if (categoria == "Todas") R.drawable.ic_home_search else iconoLocal)
        }
        if (seleccionada) {
            card.strokeColor = ContextCompat.getColor(requireContext(), R.color.brand_color)
            card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brand_container))
        }
        card.setOnClickListener { alPulsar() }
        return card
    }

    private fun pinsFiltrados(): List<MapPin> {
        return allPins
            .filter {
                selectedCategory.isBlank() ||
                    categoriaCoincide(selectedCategory, it.publication.category)
            }
            .sortedBy { it.distanceKm }
    }

    private fun refreshPinsAndList() {
        if (!isAdded) return
        val filtered = pinsFiltrados()
        lastShownCount = filtered.size
        dibujarPines(filtered)
        // Si lo seleccionado quedó fuera del filtro, esconde su tarjeta.
        if (selectedPubId != null && filtered.none { it.publication.publicationId == selectedPubId }) {
            selectedPubId = null
            overlayContainer?.visibility = View.GONE
        }
        if (filtered.isEmpty()) {
            rvMapPlaces?.visibility = View.GONE
            tvMapEmpty?.visibility = View.VISIBLE
            tvMapEmpty?.text = if (selectedCategory.isBlank()) {
                "Aún no hay chambas publicadas.\nVuelve pronto."
            } else {
                "No hay chambas de esta categoría por aquí.\nPrueba con “Todas”."
            }
        } else {
            tvMapEmpty?.visibility = View.GONE
            rvMapPlaces?.visibility = View.VISIBLE
            placeAdapter?.submitList(filtered)
        }
        tvHeaderSubtitle?.text = when {
            zonaMapa != null && !zonaLabel.isNullOrBlank() ->
                "$zonaLabel • $lastShownCount chambas"
            zonaMapa != null -> "$lastShownCount chambas en esta zona"
            hasGpsFix && !zonaLabel.isNullOrBlank() ->
                "$zonaLabel • $lastShownCount cerca"
            hasGpsFix -> "$lastShownCount chambas cerca de ti"
            else -> "$lastShownCount chambas en Ayacucho"
        }
    }

    /** Reordena por distancia sin recargar (al obtener GPS). */
    private fun actualizarDistanciasYLista() {
        allPins.forEach { it.distanceKm = MapGeo.distanceKm(userRefPoint, it.position) }
        refreshPinsAndList()
    }

    private fun seleccionarPin(publicationId: String, withRoute: Boolean) {
        val pin = allPins.firstOrNull { it.publication.publicationId == publicationId } ?: return
        selectedPubId = publicationId
        val pub = pin.publication
        overlayContainer?.visibility = View.VISIBLE
        view?.findViewById<TextView>(R.id.tvOverlayTitle)?.text = pub.title
        view?.findViewById<TextView>(R.id.tvOverlayMeta)?.text =
            "${pub.precioTexto()} · ${MapGeo.formatDistance(pin.distanceKm)}"
        val thumb = view?.findViewById<ShapeableImageView>(R.id.ivVillaThumb)
        val photo = pub.images.firstOrNull()?.url.orEmpty()
        if (thumb != null) {
            if (photo.isNotBlank()) {
                thumb.load(photo) {
                    crossfade(true)
                    placeholder(R.drawable.bg_job_image_placeholder)
                    error(R.drawable.bg_job_image_placeholder)
                }
            } else {
                thumb.setImageResource(R.drawable.bg_job_image_placeholder)
            }
        }
        maplibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pin.position, 15.0), 900)
        if (withRoute) {
            val userPos = getUserCurrentLatLng() ?: userRefPoint
            calculateAndDrawRoute(userPos, pin.position, pub.title)
        }
    }

    private fun onMapBackgroundTap(point: LatLng) {
        val nearest = allPins.minByOrNull { MapGeo.distanceKm(point, it.position) }
        if (nearest != null && MapGeo.distanceKm(point, nearest.position) <= 1.5) {
            seleccionarPin(nearest.publication.publicationId, withRoute = false)
        } else {
            selectedPubId = null
            overlayContainer?.visibility = View.GONE
        }
    }

    /**
     * Calculates real driving route using OpenRouteService API
     * Draws glowing polyline on map with distance and estimated time
     */
    private fun calculateAndDrawRoute(start: LatLng, destination: LatLng, destinationName: String) {
        Toast.makeText(requireContext(), "Trazando ruta a $destinationName...", Toast.LENGTH_SHORT).show()
        val myId = ++routeRequestId

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ORS uses start=lon,lat & end=lon,lat
                val urlString = "https://api.openrouteservice.org/v2/directions/driving-car?api_key=$orsApiKey&start=${start.longitude},${start.latitude}&end=${destination.longitude},${destination.latitude}"
                val responseBody = URL(urlString).readText()

                val json = JSONObject(responseBody)
                val features = json.optJSONArray("features")
                if (features != null && features.length() > 0) {
                    val feature = features.optJSONObject(0) ?: return@launch
                    val geometry = feature.optJSONObject("geometry") ?: return@launch
                    val coords = geometry.optJSONArray("coordinates") ?: return@launch
                    val routePoints = mutableListOf<LatLng>()

                    for (i in 0 until coords.length()) {
                        val point = coords.optJSONArray(i) ?: continue
                        val lon = point.optDouble(0)
                        val lat = point.optDouble(1)
                        routePoints.add(LatLng(lat, lon))
                    }

                    val properties = feature.optJSONObject("properties")
                    val summary = properties?.optJSONObject("summary")
                    val distanceMeters = summary?.optDouble("distance", 0.0) ?: 0.0
                    val durationSeconds = summary?.optDouble("duration", 0.0) ?: 0.0

                    val km = String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
                    val minutes = "${(durationSeconds / 60.0).roundToInt()} min"
                    val summaryText = "$destinationName • $km ($minutes)"

                    withContext(Dispatchers.Main) {
                        if (!isAdded || myId != routeRequestId) return@withContext
                        drawRouteOnMap(routePoints, summaryText)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // Fallback to camera animation if network error
                    maplibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(destination, 15.5), 1000)
                }
            }
        }
    }

    private fun drawRouteOnMap(points: List<LatLng>, summaryText: String? = null) {
        if (!isAdded || points.isEmpty()) return
        activity?.runOnUiThread {
            activeRoutePolyline?.let { maplibreMap?.removePolyline(it) }

            val polylineOptions = PolylineOptions()
                .addAll(points)
                .color(Color.parseColor("#10B981")) // ChambAYA Vibrant Emerald Green
                .width(6f)
            activeRoutePolyline = maplibreMap?.addPolyline(polylineOptions)

            if (points.size > 1) {
                val boundsBuilder = LatLngBounds.Builder()
                points.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                maplibreMap?.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 120, 160, 120, 260),
                    1200
                )
            }

            summaryText?.let {
                tvHeaderSubtitle?.text = it
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Ubica el lugar de unas coordenadas: calle/barrio real (no "Ayacucho").
     * Usa Pelias (Geocode Earth) y, si falla, cae al geocodificador de ORS.
     */
    private fun fetchAddressForLocation(latLng: LatLng) {
        val seq = ++reverseSeq
        viewLifecycleOwner.lifecycleScope.launch {
            val place = geoService.reverse(latLng.latitude, latLng.longitude).getOrNull()
            if (place == null) {
                fetchAddressWithOrs(latLng)
                return@launch
            }
            withContext(Dispatchers.Main) {
                if (!isAdded || seq != reverseSeq) return@withContext
                val titulo = place.name.ifBlank { place.label }
                zonaLabel = titulo
                tvHeaderTitle?.text = titulo
                tvHeaderSubtitle?.text = if (lastShownCount > 0) {
                    "$titulo • $lastShownCount cerca"
                } else {
                    titulo
                }
            }
        }
    }

    /**
     * Respaldo: Reverse Geocoding con OpenRouteService.
     */
    private fun fetchAddressWithOrs(latLng: LatLng) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://api.openrouteservice.org/geocode/reverse?api_key=$orsApiKey&point.lon=${latLng.longitude}&point.lat=${latLng.latitude}&size=1"
                val responseBody = URL(urlString).readText()

                val json = JSONObject(responseBody)
                val features = json.optJSONArray("features")
                if (features != null && features.length() > 0) {
                    val feature = features.optJSONObject(0) ?: return@launch
                    val props = feature.optJSONObject("properties")
                    val name = props?.optString("name", "") ?: ""
                    val label = props?.optString("label", "") ?: ""
                    val placeName = if (name.isNotBlank()) name else if (label.isNotBlank()) label else "Ayacucho, Perú"

                    withContext(Dispatchers.Main) {
                        if (!isAdded) return@withContext
                        tvHeaderSubtitle?.text = if (lastShownCount > 0) {
                            "$placeName • $lastShownCount cerca"
                        } else {
                            placeName
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** GPS fijado: reordena todo por distancia real ("cerca de mí"). */
    private fun onUserPosition(userLatLng: LatLng) {
        userRefPoint = userLatLng
        zonaMapa = null
        hasGpsFix = true
        view?.findViewById<View>(R.id.btnSearchArea)?.visibility = View.GONE
        mostrarPuntoUsuario(userLatLng)
        if (dataLoaded) actualizarDistanciasYLista()
    }

    /** Punto azul "estás aquí" (el componente de ubicación no siempre lo pinta). */
    private fun mostrarPuntoUsuario(pos: LatLng) {
        val map = maplibreMap ?: return
        try {
            val icon = vectorToBitmap(R.drawable.map_user_dot, 44) ?: return
            val mapIcon = org.maplibre.android.annotations.IconFactory.getInstance(requireContext())
                .fromBitmap(icon)
            if (userDot == null) {
                userDot = map.addMarker(
                    MarkerOptions().position(pos).icon(mapIcon)
                )
            } else {
                val existing = userDot
                if (existing != null) {
                    existing.position = pos
                    map.updateMarker(existing)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vectorToBitmap(drawableRes: Int, sizePx: Int): android.graphics.Bitmap? {
        return try {
            val d = ContextCompat.getDrawable(requireContext(), drawableRes) ?: return null
            val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            d.setBounds(0, 0, sizePx, sizePx)
            d.draw(canvas)
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Al mover el mapa lejos del punto de búsqueda, ofrece buscar en la zona visible. */
    private fun onCameraIdle() {
        if (!isAdded) return
        val target = try {
            maplibreMap?.cameraPosition?.target
        } catch (e: Exception) {
            null
        } ?: return
        val pill = view?.findViewById<View>(R.id.btnSearchArea) ?: return
        val lejos = MapGeo.distanceKm(target, userRefPoint) > 1.2
        pill.visibility = if (lejos && allPins.isNotEmpty()) View.VISIBLE else View.GONE
    }

    /** Re-centra la búsqueda donde mira el mapa y reordena por cercanía. */
    private fun buscarEnZonaVisible() {
        val target = try {
            maplibreMap?.cameraPosition?.target
        } catch (e: Exception) {
            null
        } ?: return
        userRefPoint = target
        zonaMapa = target
        zonaLabel = null
        view?.findViewById<View>(R.id.btnSearchArea)?.visibility = View.GONE
        Toast.makeText(requireContext(), "Buscando chambas en esta zona…", Toast.LENGTH_SHORT).show()
        actualizarDistanciasYLista()
    }

    private fun getUserCurrentLatLng(): LatLng? {
        val lastKnown = maplibreMap?.locationComponent?.lastKnownLocation
        return if (lastKnown != null) {
            LatLng(lastKnown.latitude, lastKnown.longitude)
        } else null
    }

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun requestLocationPermissionSilently() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun enableLocationComponent(map: MapLibreMap, style: Style) {
        try {
            val context = context ?: return
            if (!hasLocationPermission()) return

            val locationComponentOptions = LocationComponentActivationOptions.builder(context, style)
                .useDefaultLocationEngine(true)
                .build()

            map.locationComponent.apply {
                activateLocationComponent(locationComponentOptions)
                isLocationComponentEnabled = true
                cameraMode = CameraMode.NONE
                renderMode = RenderMode.COMPASS
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkLocationAndNavigate() {
        if (hasLocationPermission()) {
            moveToCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun moveToCurrentLocation() {
        val context = context ?: return
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        onUserPosition(userLatLng)
                        maplibreMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, 16.0),
                            1200
                        )
                        // Calle real del punto: Geocode Earth (Pelias), con ORS de respaldo
                        fetchAddressForLocation(userLatLng)
                        Toast.makeText(context, "Ubicación GPS centrada", Toast.LENGTH_SHORT).show()
                    } else {
                        val lastKnown = maplibreMap?.locationComponent?.lastKnownLocation
                        if (lastKnown != null) {
                            val userLatLng = LatLng(lastKnown.latitude, lastKnown.longitude)
                            onUserPosition(userLatLng)
                            maplibreMap?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(userLatLng, 16.0),
                                1200
                            )
                            fetchAddressForLocation(userLatLng)
                        } else {
                            Toast.makeText(context, "Buscando señal GPS...", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
        applyWhiteStatusBar()
        // Refresca chambas al volver (nuevas publicaciones aparecen solas).
        if (view != null) cargarDatos()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            applyWhiteStatusBar()
        }
    }

    private fun applyWhiteStatusBar() {
        val act = activity ?: return
        BarraEstadoUtils.aplicarColor(act, Color.WHITE)
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        geoSearchJob?.cancel()
        mapView?.onDestroy()
        mapView = null
        maplibreMap = null
        geoAdapter = null
        etPlaceSearch = null
        searchBarCard = null
        geoSuggestionsCard = null
        rvGeoSuggestions = null
        progressGeoSearch = null
        btnClearSearch = null
        placeMarker = null
        reverseSeq++
        rvMapPlaces = null
        placeAdapter = null
        progressMapPlaces = null
        tvMapEmpty = null
        rowMapCategories = null
        overlayContainer = null
        mapMarkers.clear()
        markerPubIds.clear()
        tvHeaderTitle = null
        tvHeaderSubtitle = null
    }

    private companion object {
        /** Mínimo de letras para preguntar al autocompletado de Pelias. */
        const val MIN_GEO_CHARS = 3
        /** Espera tras la última tecla antes de pedir sugerencias. */
        const val GEO_DEBOUNCE_MS = 320L
        /** Zoom al elegir un lugar (algo más cerrado que el de GPS). */
        const val GEO_ZOOM = 16.2
    }
}
