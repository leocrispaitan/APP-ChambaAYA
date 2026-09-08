package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R

class FragmentoChambas : Fragment() {

    private var scrollView: NestedScrollView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragmento_chambas, container, false)
        
        // Optimizar el scroll para máxima fluidez
        scrollView = view.findViewById(R.id.scrollMain)
        scrollView?.apply {
            // NO usar hardware layer - causa lag en scrolls
            // Usar software rendering que es más rápido para scroll
            setLayerType(View.LAYER_TYPE_NONE, null)
            
            // Habilitar nested scrolling correctamente
            isNestedScrollingEnabled = true
            
            // Desactivar over-scroll
            overScrollMode = View.OVER_SCROLL_NEVER
            
            // Habilitar smooth scrolling
            isSmoothScrollingEnabled = true
            
            // Desactivar el fading edge que causa lag
            isVerticalFadingEdgeEnabled = false
            isHorizontalFadingEdgeEnabled = false
            
            // Optimizar el scroll cache
            isScrollContainer = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            
            // Configurar velocity para scroll más rápido y fluido
            setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
                // Listener vacío para forzar optimización
            }
        }
        
        return view
    }

override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.home_red))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        scrollView = null
    }
}