package com.proyecto.chambaya.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.MainActivity
import com.proyecto.chambaya.R

class FragmentoMiPerfil : Fragment() {

    private var isFollowing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmento_mi_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopBar(view)
        setupActionButtons(view)
        setupSkillsAndExperience(view)
        setupTabNavigation(view)
        setupEmptyStateButtons(view)
    }

    override fun onResume() {
        super.onResume()
        // Seamless dark cosmic status bar matching the header gradient
        BarraEstadoUtils.aplicarColor(
            requireActivity(),
            ContextCompat.getColor(requireContext(), R.color.profile_header_dark)
        )
        // Ensure bottom nav is visible when coming back from Settings
        (activity as? MainActivity)?.showBottomNav()
    }

    private fun setupTopBar(root: View) {
        root.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            // Return to primary jobs tab if hosted in MainActivity or handle back
            val main = activity as? MainActivity
            if (main != null) {
                main.navigateToTab(R.id.nav_jobs)
            } else {
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }

        root.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            // Navegar al fragmento de ajustes (sin bottom nav)
            val settingsFragment = FragmentoAjustesPerfil()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.dialog_slide_up,   // enter
                    android.R.anim.fade_out,  // exit
                    android.R.anim.fade_in,   // popEnter
                    R.anim.dialog_slide_down  // popExit
                )
                .replace(requireView().parent?.parent.let {
                    // Usar el contenedor de MainActivity
                    com.proyecto.chambaya.R.id.fragmentContainer
                }, settingsFragment, "SETTINGS")
                .addToBackStack("SETTINGS")
                .commit()
        }
        
    }

    private fun setupActionButtons(root: View) {
        val btnEditarPerfil = root.findViewById<View>(R.id.btnEditarPerfil)
        val btnCompartirPerfil = root.findViewById<View>(R.id.btnCompartirPerfil)
        val btnComplete = root.findViewById<View>(R.id.btnEmail)

        btnEditarPerfil?.setOnClickListener {
            showCardFeedback(it, "Editar perfil de Katty Huaman")
        }

        btnCompartirPerfil?.setOnClickListener {
            showCardFeedback(it, "Compartir tu perfil")
        }

        btnComplete?.setOnClickListener {
            showCardFeedback(it, "Completando información restante del perfil...")
        }
    }

    private fun setupSkillsAndExperience(root: View) {
        root.findViewById<View>(R.id.skillFigma)?.setOnClickListener {
            showCardFeedback(it, "Especialidad: Albañilería")
        }
        root.findViewById<View>(R.id.skillAdobeXd)?.setOnClickListener {
            showCardFeedback(it, "Especialidad: Pintura")
        }
        root.findViewById<View>(R.id.skillSketch)?.setOnClickListener {
            showCardFeedback(it, "Especialidad: Jardinería")
        }
        root.findViewById<View>(R.id.skillInVision)?.setOnClickListener {
            showCardFeedback(it, "Especialidad: Ayudante general")
        }

        root.findViewById<View>(R.id.expItem1)?.setOnClickListener {
            showCardFeedback(it, "Ayudante de construcción • Carmen Alto (5 días)")
        }
        root.findViewById<View>(R.id.expItem2)?.setOnClickListener {
            showCardFeedback(it, "Pintura de vivienda • Ayacucho Centro (3 días)")
        }
    }

    private fun showCardFeedback(view: View, message: String) {
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(90)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .start()
            }
            .start()

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Configura la navegación entre tabs: Sobre Mí, Fotos, Reseñas
     * Muestra/oculta el contenido apropiado y actualiza los estilos de tabs
     */
    private fun setupTabNavigation(root: View) {
        val tabSobreMi = root.findViewById<View>(R.id.tabSobreMi)
        val tabFotos = root.findViewById<View>(R.id.tabFotos)
        val tabResenas = root.findViewById<View>(R.id.tabResenas)

        val contentSobreMi = root.findViewById<View>(R.id.contentTabSobreMi)
        val contentFotos = root.findViewById<View>(R.id.contentTabFotos)
        val contentResenas = root.findViewById<View>(R.id.contentTabResenas)

        // Tab 1: Sobre Mí (por defecto activo)
        tabSobreMi?.setOnClickListener {
            activateTab(
                root,
                selectedTab = it,
                contentToShow = contentSobreMi,
                allTabs = listOf(tabSobreMi, tabFotos, tabResenas),
                allContents = listOf(contentSobreMi, contentFotos, contentResenas)
            )
        }

        // Tab 2: Fotos
        tabFotos?.setOnClickListener {
            activateTab(
                root,
                selectedTab = it,
                contentToShow = contentFotos,
                allTabs = listOf(tabSobreMi, tabFotos, tabResenas),
                allContents = listOf(contentSobreMi, contentFotos, contentResenas)
            )
        }

        // Tab 3: Reseñas
        tabResenas?.setOnClickListener {
            activateTab(
                root,
                selectedTab = it,
                contentToShow = contentResenas,
                allTabs = listOf(tabSobreMi, tabFotos, tabResenas),
                allContents = listOf(contentSobreMi, contentFotos, contentResenas)
            )
        }
    }

    /**
     * Activa un tab específico, actualiza su estilo y muestra el contenido correspondiente
     */
    private fun activateTab(
        root: View,
        selectedTab: View,
        contentToShow: View?,
        allTabs: List<View?>,
        allContents: List<View?>
    ) {
        // Ocultar todos los contenidos
        allContents.forEach { it?.visibility = View.GONE }
        // Mostrar el contenido seleccionado
        contentToShow?.visibility = View.VISIBLE

        // Actualizar estilos de todos los tabs
        allTabs.forEachIndexed { index, tab ->
            tab?.let {
                val isActive = tab == selectedTab
                
                // Actualizar background
                it.setBackgroundResource(
                    if (isActive) R.drawable.bg_profile_tab_active 
                    else R.drawable.bg_profile_tab_inactive
                )

                // Actualizar color de icono y texto según estado
                val iconId = when (index) {
                    0 -> R.id.iconTabSobreMi
                    1 -> R.id.iconTabFotos
                    2 -> R.id.iconTabResenas
                    else -> null
                }
                val textId = when (index) {
                    0 -> R.id.tvTabSobreMi
                    1 -> R.id.tvTabFotos
                    2 -> R.id.tvTabResenas
                    else -> null
                }

                iconId?.let { id ->
                    root.findViewById<android.widget.ImageView>(id)?.imageTintList = 
                        ContextCompat.getColorStateList(
                            requireContext(),
                            if (isActive) R.color.brand_color else R.color.profile_text_stat_label
                        )
                }
                
                textId?.let { id ->
                    root.findViewById<TextView>(id)?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isActive) R.color.brand_color else R.color.profile_text_stat_label
                        )
                    )
                }
            }
        }

        // Feedback visual de tap
        selectedTab.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(70)
            .withEndAction {
                selectedTab.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    /**
     * Configura los botones de los empty states (Subir foto, etc.)
     */
    private fun setupEmptyStateButtons(root: View) {
        // Botón: Subir foto (en tab de Fotos)
        root.findViewById<View>(R.id.btnSubirFoto)?.setOnClickListener {
            showCardFeedback(it, "Próximamente: Subir fotos de tus trabajos")
        }
    }
}