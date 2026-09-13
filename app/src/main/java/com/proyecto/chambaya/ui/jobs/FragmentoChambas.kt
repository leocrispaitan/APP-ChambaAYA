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
        // Datos de ejemplo con campos de estilo Instagram
        val sampleJobs = listOf(
            JobCard(
                id = "1",
                titulo = "Maestro Albañil",
                categoria = "Construcción",
                rating = 4.8f,
                precio = "S/ 80 / día",
                iconoCategoria = R.drawable.ic_cat_construccion,
                colorFondo = "#1E3A5F",
                empleador = "Carlos Quispe",
                distrito = "Ayacucho Centro",
                tiempoPublicado = "Hace 2h",
                descripcion = "Se necesita maestro albañil con experiencia en construcción de viviendas. Obra en el centro de Ayacucho, trabajo inmediato."
            ),
            JobCard(
                id = "2",
                titulo = "Limpieza de Hogar",
                categoria = "Limpieza",
                rating = 4.6f,
                precio = "S/ 50 / día",
                iconoCategoria = R.drawable.ic_cat_limpieza,
                colorFondo = "#2D5A8E",
                empleador = "María Flores",
                distrito = "Carmen Alto",
                tiempoPublicado = "Hace 5h",
                descripcion = "Necesito persona responsable para limpieza profunda de departamento. Se paga al finalizar el día. Llevar implementos propios."
            ),
            JobCard(
                id = "3",
                titulo = "Delivery Express",
                categoria = "Delivery",
                rating = 4.9f,
                precio = "S/ 40 / día",
                iconoCategoria = R.drawable.ic_cat_delivery,
                colorFondo = "#1A6B4A",
                empleador = "Restaurante El Inca",
                distrito = "Jesús Nazareno",
                tiempoPublicado = "Hace 1h",
                descripcion = "Buscamos repartidores con moto propia para delivery de comida. Horario flexible de lunes a domingo. Pago diario."
            ),
            JobCard(
                id = "4",
                titulo = "Técnico Electricista",
                categoria = "Técnico",
                rating = 4.7f,
                precio = "S/ 100 / día",
                iconoCategoria = R.drawable.ic_cat_tecnico,
                colorFondo = "#5B3D8F",
                empleador = "Juan Mendoza",
                distrito = "San Juan Bautista",
                tiempoPublicado = "Hace 3h",
                descripcion = "Instalación eléctrica residencial. Trabajo de 2 días. Requiere certificación y herramientas propias. Pago adelantado el 50%."
            ),
            JobCard(
                id = "5",
                titulo = "Pintor Profesional",
                categoria = "Construcción",
                rating = 4.5f,
                precio = "S/ 70 / día",
                iconoCategoria = R.drawable.ic_cat_construccion,
                colorFondo = "#7B3D2A",
                empleador = "Constructora Andina",
                distrito = "Ayacucho Centro",
                tiempoPublicado = "Hace 8h",
                descripcion = "Se requiere pintor con experiencia en pintura de interiores y exteriores. Proyecto de 1 semana con posibilidad de renovación."
            ),
            JobCard(
                id = "6",
                titulo = "Jardinería y Mantenimiento",
                categoria = "Limpieza",
                rating = 4.4f,
                precio = "S/ 60 / día",
                iconoCategoria = R.drawable.ic_cat_limpieza,
                colorFondo = "#2D6B3A",
                empleador = "Club Ayacucho",
                distrito = "Magdalena",
                tiempoPublicado = "Hace 6h",
                descripcion = "Mantenimiento de jardines y áreas verdes. Trabajo fijo los fines de semana. Incluye almuerzo y materiales."
            ),
            JobCard(
                id = "7",
                titulo = "Mensajería Rápida",
                categoria = "Delivery",
                rating = 4.8f,
                precio = "S/ 35 / día",
                iconoCategoria = R.drawable.ic_cat_delivery,
                colorFondo = "#1E5A7A",
                empleador = "Farmacias Unidas",
                distrito = "Ayacucho Centro",
                tiempoPublicado = "Hace 30min",
                descripcion = "Mensajero para entrega de medicamentos a domicilio. Zona urbana solamente. Bicicleta o moto. Turno mañana o tarde."
            ),
            JobCard(
                id = "8",
                titulo = "Gasfitero / Plomero",
                categoria = "Técnico",
                rating = 4.6f,
                precio = "S/ 90 / día",
                iconoCategoria = R.drawable.ic_cat_tecnico,
                colorFondo = "#3D2D6B",
                empleador = "Roberto Huamán",
                distrito = "Andrés Avelino Cáceres",
                tiempoPublicado = "Hace 4h",
                descripcion = "Instalación y reparación de tuberías en edificio nuevo. 3 días de trabajo. Herramientas a cargo del contratante. Pago diario."
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