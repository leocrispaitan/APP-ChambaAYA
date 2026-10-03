package com.proyecto.chambaya.ui.jobs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.remote.GeoPlace
import com.proyecto.chambaya.data.remote.GeoSearchService
import com.proyecto.chambaya.ui.map.GeoPlaceAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Buscador de lugares del feed — Geocode Earth (Pelias).
 *
 * Autocompleta calles, restaurantes, mercados, barrios y distritos de Perú,
 * sesgado hacia el punto actual (la ubicación elegida o Ayacucho). Al elegir
 * un lugar devuelve el punto con Fragment Result; el feed se recorta a un
 * radio alrededor.
 */
class LocationSearchSheet : BottomSheetDialogFragment() {

    private val geo = GeoSearchService()

    private var focusLat = GeoSearchService.AYACUCHO_LAT
    private var focusLng = GeoSearchService.AYACUCHO_LNG

    private var etSearch: EditText? = null
    private var btnClear: ImageButton? = null
    private var progress: ProgressBar? = null
    private var tvHint: TextView? = null
    private var tvMyLocationHint: TextView? = null
    private var adapter: GeoPlaceAdapter? = null

    private var searchJob: Job? = null
    /** Petición en curso: evita que una respuesta lenta pise a una nueva. */
    private var requestId = 0L

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            pedirUbicacionActual()
        } else {
            tvMyLocationHint?.text = "Permiso de ubicación denegado"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_location_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        focusLat = requireArguments().getDouble(ARG_LAT, GeoSearchService.AYACUCHO_LAT)
        focusLng = requireArguments().getDouble(ARG_LNG, GeoSearchService.AYACUCHO_LNG)

        etSearch = view.findViewById(R.id.etGeoSearch)
        btnClear = view.findViewById(R.id.btnGeoClear)
        progress = view.findViewById(R.id.progressGeo)
        tvHint = view.findViewById(R.id.tvGeoHint)
        tvMyLocationHint = view.findViewById(R.id.tvGeoMyLocationHint)

        val recycler = view.findViewById<RecyclerView>(R.id.rvGeoResults)
        recycler?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        adapter = GeoPlaceAdapter { lugar -> elegir(lugar) }
        recycler?.adapter = adapter

        // Texto: debounce para no gastar las 1000 req/día del plan.
        etSearch?.doAfterTextChanged { texto ->
            val query = texto?.toString().orEmpty()
            btnClear?.visibility = if (query.isBlank()) View.GONE else View.VISIBLE
            searchJob?.cancel()
            if (query.trim().length < GeoSearchService.MIN_CHARS) {
                ++requestId
                progress?.visibility = View.GONE
                adapter?.submitList(emptyList())
                mostrarHint("Escribe al menos 3 letras para buscar.")
                return@doAfterTextChanged
            }
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(DEBOUNCE_MS)
                buscar(query)
            }
        }

        etSearch?.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchJob?.cancel()
                val query = v.text?.toString().orEmpty()
                if (query.trim().isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch { buscar(query) }
                }
                true
            } else {
                false
            }
        }

        btnClear?.setOnClickListener {
            etSearch?.setText("")
            mostrarHint("Escribe al menos 3 letras para buscar.")
        }

        // Atajos de categorías: los lugares donde más se publica.
        val atajos = listOf(
            R.id.chipRestaurantes to "restaurante",
            R.id.chipFarmacias to "farmacia",
            R.id.chipMercados to "mercado",
            R.id.chipColegios to "colegio",
            R.id.chipMunicipalidad to "municipalidad",
            R.id.chipUniversidades to "universidad"
        )
        atajos.forEach { (id, texto) ->
            view.findViewById<Chip>(id)?.setOnClickListener {
                etSearch?.setText(texto)
                etSearch?.setSelection(texto.length)
            }
        }

        view.findViewById<View>(R.id.btnGeoMyLocation)?.setOnClickListener { usarMiUbicacion() }
        view.findViewById<View>(R.id.btnGeoAll)?.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST, bundleOf(EXTRA_CLEAR to true))
            dismiss()
        }
    }

    // ── Búsqueda ─────────────────────────────────────────────

    private suspend fun buscar(query: String) {
        val id = ++requestId
        progress?.visibility = View.VISIBLE
        tvHint?.visibility = View.GONE
        val res = geo.autocomplete(query, focusLat, focusLng)
        if (!isAdded || id != requestId) return   // respuesta obsoleta
        progress?.visibility = View.GONE

        res.onSuccess { lugares ->
            adapter?.submitList(lugares)
            if (lugares.isEmpty()) {
                mostrarHint("Sin resultados para \"$query\".\nPrueba con el nombre de la calle o sin el Jr./Av.")
            }
        }.onFailure { err ->
            adapter?.submitList(emptyList())
            mostrarHint("No se pudo consultar el mapa: ${err.message?.take(60) ?: "sin conexión"}")
        }
    }

    private fun mostrarHint(texto: String) {
        tvHint?.apply {
            this.text = texto
            visibility = View.VISIBLE
        }
    }

    private fun elegir(lugar: GeoPlace) {
        parentFragmentManager.setFragmentResult(
            REQUEST,
            bundleOf(
                EXTRA_CLEAR to false,
                EXTRA_NAME to lugar.name,
                EXTRA_LABEL to lugar.label,
                EXTRA_LAT to lugar.latitude,
                EXTRA_LNG to lugar.longitude,
                EXTRA_LAYER to lugar.layer,
                EXTRA_STREET to lugar.street,
                EXTRA_LOCALITY to lugar.locality,
                EXTRA_COUNTY to lugar.county,
                EXTRA_REGION to lugar.region
            )
        )
        dismiss()
    }

    // ── GPS ──────────────────────────────────────────────────

    private fun usarMiUbicacion() {
        if (hasLocationPermission()) {
            pedirUbicacionActual()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Traduce el GPS a una calle real (reverse geocoding) para que el feed
     * diga "Jr. Tres Mascaras" y no solo coordenadas.
     */
    private fun pedirUbicacionActual() {
        val activity = activity ?: return
        if (!hasLocationPermission()) return
        tvMyLocationHint?.text = "Obteniendo tu ubicación…"
        runCatching {
            LocationServices.getFusedLocationProviderClient(activity)
                .lastLocation
                .addOnSuccessListener { location ->
                    if (location == null || !isAdded) {
                        tvMyLocationHint?.text = "No pudimos obtener el GPS"
                        return@addOnSuccessListener
                    }
                    val id = ++requestId
                    tvMyLocationHint?.text = "Buscando tu calle…"
                    viewLifecycleOwner.lifecycleScope.launch {
                        val lugar = geo.reverse(location.latitude, location.longitude).getOrNull()
                        if (!isAdded || id != requestId) return@launch
                        if (lugar != null) {
                            elegir(lugar)
                        } else {
                            // Sin calle reconocible: al menos el punto exacto.
                            tvMyLocationHint?.text = "Ubicación aproximada"
                            elegir(
                                GeoPlace(
                                    name = "Mi ubicación",
                                    label = "Mi ubicación",
                                    latitude = location.latitude,
                                    longitude = location.longitude
                                )
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    if (isAdded) tvMyLocationHint?.text = "No pudimos obtener el GPS"
                }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        etSearch = null
        btnClear = null
        progress = null
        tvHint = null
        tvMyLocationHint = null
        adapter = null
    }

    companion object {
        const val REQUEST = "location_search"
        const val EXTRA_CLEAR = "clear"
        const val EXTRA_NAME = "name"
        const val EXTRA_LABEL = "label"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_LAYER = "layer"
        const val EXTRA_STREET = "street"
        const val EXTRA_LOCALITY = "locality"
        const val EXTRA_COUNTY = "county"
        const val EXTRA_REGION = "region"

        private const val ARG_LAT = "arg_lat"
        private const val ARG_LNG = "arg_lng"
        private const val DEBOUNCE_MS = 400L

        /** Sesga las sugerencias hacia la ubicación ya elegida. */
        fun newInstance(focusLat: Double, focusLng: Double) = LocationSearchSheet().apply {
            arguments = bundleOf(ARG_LAT to focusLat, ARG_LNG to focusLng)
        }
    }
}