package com.proyecto.chambaya.ui.profile

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.MainActivity
import com.proyecto.chambaya.R

/**
 * Pantalla de Ajustes de Perfil.
 *
 * Flujo:
 *  1. Usuario toca btnSettings en FragmentoMiPerfil
 *  2. Esta pantalla se muestra reemplazando el fragmento de perfil (sin bottom nav)
 *  3. Botón atrás regresa al perfil
 *  4. Fila "Cerrar sesión" muestra el dialog de confirmación
 */
class FragmentoAjustesPerfil : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmento_ajustes_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopBar(view)
        setupSettingsRows(view)
    }

    override fun onResume() {
        super.onResume()
        // Status bar blanca para coincidir con el top bar claro
        BarraEstadoUtils.aplicarColor(
            requireActivity(),
            ContextCompat.getColor(requireContext(), R.color.white)
        )
        // Ocultar bottom navigation
        (activity as? MainActivity)?.hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        // Restaurar bottom navigation al salir
        (activity as? MainActivity)?.showBottomNav()
    }

    // ─────────────────────────────────────────────────────────────
    //  TOP BAR
    // ─────────────────────────────────────────────────────────────

    private fun setupTopBar(root: View) {
        root.findViewById<View>(R.id.btnSettingsBack)?.setOnClickListener {
            navigateBack()
        }

        root.findViewById<View>(R.id.btnSettingsMore)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Más opciones", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ROWS
    // ─────────────────────────────────────────────────────────────

    private fun setupSettingsRows(root: View) {

        // Edit Profile
        root.findViewById<View>(R.id.btnSettingsEditProfile)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Editar perfil", Toast.LENGTH_SHORT).show()
        }

        // Section: Cuenta
        root.findViewById<View>(R.id.rowAccountInfo)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Información de la cuenta", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.rowMyOrders)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Mis pedidos y chambas", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.rowAddressManagement)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Gestión de direcciones", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.rowPasswordManager)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Seguridad y contraseñas", Toast.LENGTH_SHORT).show()
        }

        // Section: Ajustes de la Aplicación
        val switchNotif = root.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchNotifications)
        root.findViewById<View>(R.id.rowNotifications)?.setOnClickListener {
            switchNotif?.let { s ->
                s.isChecked = !s.isChecked
                val msg = if (s.isChecked) "Notificaciones activadas" else "Notificaciones desactivadas"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }
        switchNotif?.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) "Notificaciones activadas" else "Notificaciones desactivadas"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.rowLanguage)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Idioma: Español (Perú)", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.rowAppearance)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Tema: Modo Claro", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.rowPrivacy)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Privacidad y permisos", Toast.LENGTH_SHORT).show()
        }

        // Section: Soporte y Legal
        root.findViewById<View>(R.id.rowHelpCenter)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Centro de ayuda y soporte ChambAYA", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.rowTerms)?.setOnClickListener {
            animateTap(it)
            Toast.makeText(requireContext(), "Términos y condiciones", Toast.LENGTH_SHORT).show()
        }

        root.findViewById<View>(R.id.rowLogout)?.setOnClickListener {
            animateTap(it)
            showLogoutDialog()
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  LOGOUT DIALOG
    // ─────────────────────────────────────────────────────────────

    private fun showLogoutDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout_confirmacion)

        // Fondo transparente para que se vea el shape redondeado
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        // Animación de entrada suave
        dialog.window?.attributes?.windowAnimations = R.style.DialogSlideUpAnimation

        dialog.findViewById<View>(R.id.btnCloseLogoutDialog)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.btnLogoutCancel)?.setOnClickListener {
            animateTap(it)
            dialog.dismiss()
        }

        dialog.findViewById<View>(R.id.btnLogoutConfirm)?.setOnClickListener {
            animateTap(it)
            dialog.dismiss()
            performLogout()
        }

        dialog.show()
    }

    private fun performLogout() {
        // TODO: limpiar sesión Firebase / SharedPrefs y navegar a Login
        Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun navigateBack() {
        parentFragmentManager.popBackStack()
    }

    /** Micro-animación de tap (scale bounce). */
    private fun animateTap(view: View) {
        view.animate()
            .scaleX(0.95f).scaleY(0.95f)
            .setDuration(80)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }.start()
    }
}
