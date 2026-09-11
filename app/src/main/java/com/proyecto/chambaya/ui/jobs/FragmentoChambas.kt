package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R

class FragmentoChambas : Fragment() {

    private var scrollView: NestedScrollView? = null
    private var recyclerView: RecyclerView? = null
    private var jobCardAdapter: JobCardAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragmento_chambas, container, false)
        
        // Optimizar el NestedScrollView para máxima fluidez
        scrollView = view.findViewById(R.id.scrollMain)
        scrollView?.apply {
            // LAYER_TYPE_NONE es el más eficiente para scroll
            setLayerType(View.LAYER_TYPE_NONE, null)
            
            // Habilitar nested scrolling
            isNestedScrollingEnabled = true
            
            // Desactivar over-scroll para eliminar efecto de rebote
            overScrollMode = View.OVER_SCROLL_NEVER
            
            // Habilitar smooth scrolling
            isSmoothScrollingEnabled = true
            
            // Desactivar fading edges que causan overhead
            isVerticalFadingEdgeEnabled = false
            isHorizontalFadingEdgeEnabled = false
            
            // Configurar scroll container
            isScrollContainer = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        }
        
        // Configurar RecyclerView con Adapter
        setupRecyclerView(view)
        
        // Cargar datos de ejemplo
        loadSampleData()
        
        return view
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.rvJobs)
        
        // Crear adapter con callbacks
        jobCardAdapter = JobCardAdapter(
            onJobClick = { jobCard ->
                // Manejar click en la card
                Toast.makeText(
                    requireContext(),
                    "Seleccionado: ${jobCard.titulo}",
                    Toast.LENGTH_SHORT
                ).show()
                // Aquí puedes navegar a los detalles del trabajo
            },
            onFavoriteClick = { jobCard ->
                // Manejar click en favorito
                val mensaje = if (jobCard.isFavorito) {
                    "Agregado a favoritos"
                } else {
                    "Removido de favoritos"
                }
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
            }
        )
        
        recyclerView?.apply {
            adapter = jobCardAdapter
            
            // Deshabilitar nested scrolling en el RecyclerView para que el NestedScrollView controle todo
            isNestedScrollingEnabled = false
            
            // Optimizaciones de rendimiento
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            
            // Hardware acceleration para el RecyclerView
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // Desactivar animaciones que causan lag
            itemAnimator = null
        }
    }

    private fun loadSampleData() {
        // Datos de ejemplo basados en el diseño original
        val sampleJobs = listOf(
            JobCard(
                id = "1",
                titulo = "Maestro Albañil",
                categoria = "Construcción",
                rating = 4.8f,
                precio = "S/ 80/día",
                iconoCategoria = R.drawable.ic_cat_construccion,
                colorFondo = "#FFF3E0"
            ),
            JobCard(
                id = "2",
                titulo = "Limpieza de hogar",
                categoria = "Limpieza",
                rating = 4.6f,
                precio = "S/ 50/día",
                iconoCategoria = R.drawable.ic_cat_limpieza,
                colorFondo = "#E3F2FD"
            ),
            JobCard(
                id = "3",
                titulo = "Delivery Express",
                categoria = "Delivery",
                rating = 4.9f,
                precio = "S/ 40/día",
                iconoCategoria = R.drawable.ic_cat_delivery,
                colorFondo = "#E8F5E9"
            ),
            JobCard(
                id = "4",
                titulo = "Técnico Electricista",
                categoria = "Técnico",
                rating = 4.7f,
                precio = "S/ 100/día",
                iconoCategoria = R.drawable.ic_cat_tecnico,
                colorFondo = "#FFF9C4"
            ),
            JobCard(
                id = "5",
                titulo = "Pintor Profesional",
                categoria = "Construcción",
                rating = 4.5f,
                precio = "S/ 70/día",
                iconoCategoria = R.drawable.ic_cat_construccion,
                colorFondo = "#FFF3E0"
            ),
            JobCard(
                id = "6",
                titulo = "Jardinería",
                categoria = "Limpieza",
                rating = 4.4f,
                precio = "S/ 60/día",
                iconoCategoria = R.drawable.ic_cat_limpieza,
                colorFondo = "#E3F2FD"
            ),
            JobCard(
                id = "7",
                titulo = "Mensajería Rápida",
                categoria = "Delivery",
                rating = 4.8f,
                precio = "S/ 35/día",
                iconoCategoria = R.drawable.ic_cat_delivery,
                colorFondo = "#E8F5E9"
            ),
            JobCard(
                id = "8",
                titulo = "Gasfitero",
                categoria = "Técnico",
                rating = 4.6f,
                precio = "S/ 90/día",
                iconoCategoria = R.drawable.ic_cat_tecnico,
                colorFondo = "#FFF9C4"
            )
        )
        
        // Enviar datos al adapter
        jobCardAdapter?.submitList(sampleJobs)
    }

    override fun onResume() {
        super.onResume()
        BarraEstadoUtils.aplicarColor(requireActivity(), requireContext().getColor(R.color.brand_color))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        scrollView = null
        recyclerView = null
        jobCardAdapter = null
    }
}