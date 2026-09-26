package com.proyecto.chambaya.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import kotlinx.coroutines.Dispatchers
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

    // MapTiler Cloud Configuration (Satellite Hybrid Vector Tiles)
    private val maptilerApiKey = "1Yoce1uTsAXtugiPDqc5"
    private val maptilerHybridStyle = "https://api.maptiler.com/maps/hybrid-v4/style.json?key=$maptilerApiKey"

    // OpenRouteService (ORS) API Key for Real Turn-by-Turn Routing & Geocoding
    private val orsApiKey = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjVlYzJjYWYxZTE0YjQ5MzE4YTE1ZGI3ZWRiMDBlZTMzIiwiaCI6Im11cm11cjY0In0="
    private var activeRoutePolyline: Polyline? = null

    // Key points of interest in Ayacucho, Perú
    private val ayacuchoCenterLatLng = LatLng(-13.1631, -74.2236) // Plaza Mayor de Ayacucho
    private val carmenAltoLatLng = LatLng(-13.1764, -74.2185)     // Carmen Alto
    private val sanJuanBautistaLatLng = LatLng(-13.1690, -74.2150) // San Juan Bautista
    private val miradorAcuchimayLatLng = LatLng(-13.1712, -74.2205)// Mirador Acuchimay
    private val terminalTerrestreLatLng = LatLng(-13.1550, -74.2140)// Terminal Terrestre

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
                val baseTopPadding = (10 * resources.displayMetrics.density).toInt()
                v.updatePadding(top = statusBar.top + baseTopPadding)
                insets
            }
        }

        // Initialize MapView
        mapView = view.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)

        mapView?.getMapAsync { map ->
            maplibreMap = map

            // Initial camera position centered on Ayacucho, Perú
            map.cameraPosition = CameraPosition.Builder()
                .target(ayacuchoCenterLatLng)
                .zoom(14.5)
                .build()

            // Load MapTiler Satellite Hybrid Vector Style
            loadMapStyle(map)

            // Add Place Markers on the map
            addAyacuchoMarkers(map)

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

        // Place and Property interactions: Tapping calculates & draws real OpenRouteService routes!
        view.findViewById<View>(R.id.cardActivePlace)?.setOnClickListener {
            val userPos = getUserCurrentLatLng() ?: ayacuchoCenterLatLng
            calculateAndDrawRoute(userPos, ayacuchoCenterLatLng, "Plaza Mayor de Ayacucho")
        }

        view.findViewById<View>(R.id.cardPlaceGriffith)?.setOnClickListener {
            val userPos = getUserCurrentLatLng() ?: ayacuchoCenterLatLng
            calculateAndDrawRoute(userPos, carmenAltoLatLng, "Carmen Alto (Chamba activa)")
        }

        view.findViewById<View>(R.id.cardPlaceGrove)?.setOnClickListener {
            val userPos = getUserCurrentLatLng() ?: ayacuchoCenterLatLng
            calculateAndDrawRoute(userPos, miradorAcuchimayLatLng, "Mirador Acuchimay")
        }

        // Category clicks calculate routes to zones
        val categoryClickListener = View.OnClickListener { v ->
            val userPos = getUserCurrentLatLng() ?: ayacuchoCenterLatLng
            val (name, target) = when (v.id) {
                R.id.catSchools -> Pair("Colegios y Zona Educativa", ayacuchoCenterLatLng)
                R.id.catShopping -> Pair("Mercados y Comercios", sanJuanBautistaLatLng)
                R.id.catParks -> Pair("Mirador y Parques", miradorAcuchimayLatLng)
                R.id.catRestaurants -> Pair("Restaurantes y Servicios", carmenAltoLatLng)
                else -> Pair("", null)
            }
            if (target != null) {
                calculateAndDrawRoute(userPos, target, name)
            }
        }

        view.findViewById<View>(R.id.catSchools)?.setOnClickListener(categoryClickListener)
        view.findViewById<View>(R.id.catShopping)?.setOnClickListener(categoryClickListener)
        view.findViewById<View>(R.id.catParks)?.setOnClickListener(categoryClickListener)
        view.findViewById<View>(R.id.catRestaurants)?.setOnClickListener(categoryClickListener)

        // Request location permission silently if not already granted
        requestLocationPermissionSilently()
    }

    private fun loadMapStyle(map: MapLibreMap) {
        val styleBuilder = Style.Builder().fromUri(maptilerHybridStyle)
        map.setStyle(styleBuilder) { style ->
            if (hasLocationPermission()) {
                enableLocationComponent(map, style)
            }
        }
    }

    private fun addAyacuchoMarkers(map: MapLibreMap) {
        try {
            map.addMarker(
                MarkerOptions()
                    .position(ayacuchoCenterLatLng)
                    .title("Plaza Mayor de Ayacucho")
                    .snippet("Centro Histórico • Huamanga")
            )
            map.addMarker(
                MarkerOptions()
                    .position(carmenAltoLatLng)
                    .title("Carmen Alto")
                    .snippet("Zona de Trabajos Activos")
            )
            map.addMarker(
                MarkerOptions()
                    .position(miradorAcuchimayLatLng)
                    .title("Mirador Acuchimay")
                    .snippet("Zona Turística")
            )
            map.addMarker(
                MarkerOptions()
                    .position(sanJuanBautistaLatLng)
                    .title("San Juan Bautista")
                    .snippet("Comercio y Talleres")
            )
            map.addMarker(
                MarkerOptions()
                    .position(terminalTerrestreLatLng)
                    .title("Terminal Terrestre")
                    .snippet("Transporte y Envíos")
            )

            // When user taps a marker, calculate route to it
            map.setOnMarkerClickListener { marker ->
                val userPos = getUserCurrentLatLng() ?: ayacuchoCenterLatLng
                calculateAndDrawRoute(userPos, marker.position, marker.title ?: "Punto")
                false // Return false so the title bubble still shows
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Calculates real driving route using OpenRouteService API
     * Draws glowing polyline on map with distance and estimated time
     */
    private fun calculateAndDrawRoute(start: LatLng, destination: LatLng, destinationName: String) {
        Toast.makeText(requireContext(), "Trazando ruta a $destinationName...", Toast.LENGTH_SHORT).show()

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
     * Reverse Geocoding with OpenRouteService: Resolves real street & place names
     */
    private fun fetchAddressForLocation(latLng: LatLng) {
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
                        tvHeaderSubtitle?.text = placeName
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
                        maplibreMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, 16.0),
                            1200
                        )
                        // Reverse geocode real address with OpenRouteService
                        fetchAddressForLocation(userLatLng)
                        Toast.makeText(context, "Ubicación GPS centrada", Toast.LENGTH_SHORT).show()
                    } else {
                        val lastKnown = maplibreMap?.locationComponent?.lastKnownLocation
                        if (lastKnown != null) {
                            val userLatLng = LatLng(lastKnown.latitude, lastKnown.longitude)
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
        BarraEstadoUtils.aplicarColor(requireActivity(), Color.parseColor("#EDF8F1"))
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
        mapView?.onDestroy()
        mapView = null
        maplibreMap = null
    }
}