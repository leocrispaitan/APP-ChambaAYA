package com.proyecto.chambaya.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.MainActivity
import com.proyecto.chambaya.R

class FragmentoMapas : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmento_mapas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topBarContainer = view.findViewById<View>(R.id.topBarContainer)
        topBarContainer?.let { topBar ->
            ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
                val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                val baseTopPadding = (10 * resources.displayMetrics.density).toInt()
                v.updatePadding(top = statusBar.top + baseTopPadding)
                insets
            }
        }

        view.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.nav_jobs)
        }

        view.findViewById<View>(R.id.btnMapLayers)?.setOnClickListener {
            Toast.makeText(requireContext(), "Capas del mapa", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.cardActivePlace)?.setOnClickListener {
            Toast.makeText(requireContext(), "Modern Family Villa: $1,250,000", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.cardPlaceGriffith)?.setOnClickListener {
            Toast.makeText(requireContext(), "Griffith Park - 2.3 mi", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.cardPlaceGrove)?.setOnClickListener {
            Toast.makeText(requireContext(), "The Grove - 3.1 mi", Toast.LENGTH_SHORT).show()
        }

        val categoryClickListener = View.OnClickListener { v ->
            val name = when (v.id) {
                R.id.catSchools -> "Schools"
                R.id.catShopping -> "Shopping"
                R.id.catParks -> "Parks"
                R.id.catRestaurants -> "Restaurants"
                else -> ""
            }
            if (name.isNotEmpty()) {
                Toast.makeText(requireContext(), name, Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<View>(R.id.catSchools)?.setOnClickListener(categoryClickListener)
        view.findViewById<View>(R.id.catShopping)?.setOnClickListener(categoryClickListener)
        view.findViewById<View>(R.id.catParks)?.setOnClickListener(categoryClickListener)
        view.findViewById<View>(R.id.catRestaurants)?.setOnClickListener(categoryClickListener)
    }

    override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), Color.parseColor("#EDF8F1"))
    }
}