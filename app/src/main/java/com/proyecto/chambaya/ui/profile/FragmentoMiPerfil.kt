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
        setupTabs(view)
        setupPerkCards(view)
    }

    override fun onResume() {
        super.onResume()
        // Seamless dark cosmic status bar matching the header gradient
        BarraEstadoUtils.aplicarColor(
            requireActivity(),
            ContextCompat.getColor(requireContext(), R.color.profile_header_dark)
        )
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
            Toast.makeText(requireContext(), "Ajustes de perfil", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.btnPrivacy)?.setOnClickListener {
            Toast.makeText(requireContext(), "Modo privacidad", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupActionButtons(root: View) {
        val btnFollow = root.findViewById<AppCompatButton>(R.id.btnFollow)
        val btnMessage = root.findViewById<AppCompatButton>(R.id.btnMessage)
        val btnEmail = root.findViewById<AppCompatButton>(R.id.btnEmail)

        btnFollow?.setOnClickListener {
            isFollowing = !isFollowing
            if (isFollowing) {
                btnFollow.text = "Following"
                btnFollow.alpha = 0.88f
                Toast.makeText(requireContext(), "¡Siguiendo a Katty Abrahams!", Toast.LENGTH_SHORT).show()
            } else {
                btnFollow.text = getString(R.string.profile_btn_follow)
                btnFollow.alpha = 1f
            }
        }

        btnMessage?.setOnClickListener {
            Toast.makeText(requireContext(), "Mensaje a Katty Abrahams", Toast.LENGTH_SHORT).show()
        }

        btnEmail?.setOnClickListener {
            Toast.makeText(requireContext(), "Enviar correo a Katty Abrahams", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs(root: View) {
        val tabPerks = root.findViewById<View>(R.id.tabPerks)
        val tabAchievements = root.findViewById<View>(R.id.tabAchievements)
        val tvTabPerks = root.findViewById<TextView>(R.id.tvTabPerks)
        val tvTabAchievements = root.findViewById<TextView>(R.id.tvTabAchievements)
        val viewPerksIndicator = root.findViewById<View>(R.id.viewPerksIndicator)
        val viewAchievementsIndicator = root.findViewById<View>(R.id.viewAchievementsIndicator)

        val activeColor = ContextCompat.getColor(requireContext(), R.color.profile_tab_active)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.profile_tab_inactive)

        tabPerks?.setOnClickListener {
            tvTabPerks?.setTextColor(activeColor)
            tvTabAchievements?.setTextColor(inactiveColor)
            viewPerksIndicator?.visibility = View.VISIBLE
            viewAchievementsIndicator?.visibility = View.INVISIBLE
        }

        tabAchievements?.setOnClickListener {
            tvTabPerks?.setTextColor(inactiveColor)
            tvTabAchievements?.setTextColor(activeColor)
            viewPerksIndicator?.visibility = View.INVISIBLE
            viewAchievementsIndicator?.visibility = View.VISIBLE
        }
    }

    private fun setupPerkCards(root: View) {
        root.findViewById<View>(R.id.cardTurbos)?.setOnClickListener {
            showCardFeedback(it, "Turbos: 15% de aceleración activado")
        }

        root.findViewById<View>(R.id.cardFastAdds)?.setOnClickListener {
            showCardFeedback(it, "Fast Adds: Publicación prioritaria lista")
        }

        root.findViewById<View>(R.id.cardSpotlights)?.setOnClickListener {
            showCardFeedback(it, "Spotlights: Mayor visibilidad en resultados")
        }

        root.findViewById<View>(R.id.cardCommunity)?.setOnClickListener {
            showCardFeedback(it, "Community: Conectado con la red de creadores")
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