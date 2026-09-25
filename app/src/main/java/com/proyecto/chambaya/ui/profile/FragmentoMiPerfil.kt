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

        root.findViewById<View>(R.id.btnPrivacy)?.setOnClickListener {
            Toast.makeText(requireContext(), "Modo privacidad", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupActionButtons(root: View) {
        val btnEdit = root.findViewById<View>(R.id.btnFollow)
        val btnViewPublic = root.findViewById<View>(R.id.btnMessage)
        val btnComplete = root.findViewById<View>(R.id.btnEmail)

        btnEdit?.setOnClickListener {
            showCardFeedback(it, "Editar perfil de Carlos Quispe")
        }

        btnViewPublic?.setOnClickListener {
            showCardFeedback(it, "Vista previa de tu perfil público")
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
}