package com.proyecto.chambaya.ui.workplace

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.proyecto.chambaya.R
import com.proyecto.chambaya.ui.map.MapaEstilo
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.Locale

/**
 * Selector de punto GPS en bottom sheet.
 *
 * Handle superior de arrastre (se cierra deslizando hacia abajo, comportamiento
 * nativo del BottomSheet), tarjeta con mapa satelital interactivo —tocar el
 * mapa mueve el pin y actualiza las coordenadas—, botón "Mi ubicación" (GPS) y
 * "Confirmar" que devuelve el punto a [EditarLugarActivity].
 */
class MapaUbicacionSheet : BottomSheetDialogFragment() {

    private var mapView: MapView? = null
    private var mapa: MapLibreMap? = null
    private var pin: Marker? = null
    private var punto: LatLng? = null

    private lateinit var tvCoords: TextView

    private val permisoLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val ok = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) irAMiUbicacion() else toast(getString(R.string.lugar_gps_sin_permiso))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_mapa_ubicacion, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Fondo transparente para que se vean las esquinas redondeadas del sheet.
        (view.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        tvCoords = view.findViewById(R.id.tvMapaCoords)
        mapView = view.findViewById(R.id.mapVistaMapa)
        mapView?.onCreate(savedInstanceState)

        val inicio = LatLng(
            arguments?.getDouble(ARG_LAT, MapaEstilo.DEF_LAT) ?: MapaEstilo.DEF_LAT,
            arguments?.getDouble(ARG_LNG, MapaEstilo.DEF_LNG) ?: MapaEstilo.DEF_LNG
        )
        mapView?.getMapAsync { map ->
            mapa = map
            map.uiSettings.apply {
                isCompassEnabled = false
                isLogoEnabled = false
                isAttributionEnabled = false
            }
            map.cameraPosition = CameraPosition.Builder()
                .target(inicio)
                .zoom(ZOOM_INICIAL)
                .build()
            map.setStyle(Style.Builder().fromUri(MapaEstilo.HIBRIDO_URL)) {
                colocarPin(inicio, animar = false)
            }
            // Tocar el mapa = mover el pin y actualizar coordenadas.
            map.addOnMapClickListener { puntoTocado ->
                colocarPin(puntoTocado, animar = false)
                true
            }
        }

        view.findViewById<View>(R.id.btnCerrarMapa).setOnClickListener { dismiss() }
        view.findViewById<View>(R.id.btnMapaMiUbicacion).setOnClickListener { pedirGps() }
        view.findViewById<View>(R.id.btnMapaConfirmar).setOnClickListener { confirmar() }
    }

    private fun colocarPin(latlng: LatLng, animar: Boolean) {
        punto = latlng
        pin?.remove()
        pin = mapa?.addMarker(MarkerOptions().position(latlng))
        if (animar) {
            mapa?.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, ZOOM_INICIAL), 800)
        }
        tvCoords.text = String.format(Locale.US, "%.5f, %.5f", latlng.latitude, latlng.longitude)
    }

    private fun pedirGps() {
        val ctx = context ?: return
        val fino = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val grueso = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (fino || grueso) irAMiUbicacion()
        else permisoLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun irAMiUbicacion() {
        val activity = activity ?: return
        try {
            LocationServices.getFusedLocationProviderClient(activity).lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) colocarPin(LatLng(loc.latitude, loc.longitude), animar = true)
                    else toast(getString(R.string.lugar_gps_no_disponible))
                }
                .addOnFailureListener { toast(getString(R.string.lugar_gps_no_disponible)) }
        } catch (e: SecurityException) {
            toast(getString(R.string.lugar_gps_sin_permiso))
        }
    }

    private fun confirmar() {
        val p = punto
        if (p == null) {
            toast(getString(R.string.lugar_mapa_sin_punto))
            return
        }
        setFragmentResultCompat(bundleOf(EXTRA_LAT to p.latitude, EXTRA_LNG to p.longitude))
        dismiss()
    }

    /** `parentFragmentManager` directo: evita añadir `fragment-ktx` al proyecto. */
    private fun setFragmentResultCompat(datos: android.os.Bundle) {
        parentFragmentManager.setFragmentResult(RESULTADO_KEY, datos)
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() { super.onStart(); mapView?.onStart() }
    override fun onResume() { super.onResume(); mapView?.onResume() }
    override fun onPause() { mapView?.onPause(); super.onPause() }
    override fun onStop() { mapView?.onStop(); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }
    override fun onLowMemory() { super.onLowMemory(); mapView?.onLowMemory() }
    override fun onDestroyView() { mapView?.onDestroy(); mapView = null; super.onDestroyView() }

    companion object {
        const val RESULTADO_KEY = "mapa_ubicacion_resultado"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"

        private const val ARG_LAT = "arg_lat"
        private const val ARG_LNG = "arg_lng"
        private const val ZOOM_INICIAL = 15.0

        fun nueva(lat: Double?, lng: Double?): MapaUbicacionSheet =
            MapaUbicacionSheet().apply {
                arguments = bundleOf(
                    ARG_LAT to (lat ?: MapaEstilo.DEF_LAT),
                    ARG_LNG to (lng ?: MapaEstilo.DEF_LNG)
                )
            }
    }
}
