package com.proyecto.chambaya.ui.profile

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.EditarPerfilActivity
import com.proyecto.chambaya.LoginActivity
import com.proyecto.chambaya.MainActivity
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.repository.ProfileRepository
import kotlinx.coroutines.launch

/**
 * Pantalla de Ajustes de Perfil.
 *
 * Flujo:
 *  1. Usuario toca btnSettings en FragmentoMiPerfil
 *  2. Esta pantalla se muestra reemplazando el fragmento de perfil (sin bottom nav)
 *  3. Botón atrás regresa al perfil
 *  4. Fila "Cerrar sesión" cierra la sesión de verdad y vuelve al login
 *
 * FASE 2: la tarjeta de usuario de arriba y el diálogo de "Información de la
 * cuenta" ya no son textos fijos: salen de `users/{uid}`. El resto de filas
 * siguen avisando con un `Toast` porque pertenecen a fases posteriores.
 */
class FragmentoAjustesPerfil : Fragment() {

    private val repository = ProfileRepository()

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    /**
     * Último perfil leído, para pintar sin volver a ir a Firestore.
     *
     * Se inicializa desde [ProfileCache]: esta pantalla se recrea de cero cada
     * vez que se abre (`FragmentoAjustesPerfil()` en el `replace(...)` de
     * `FragmentoMiPerfil`), así que sin esto siempre arrancaba en `null` y
     * dependía de una nueva consulta a Firestore — que es lo que causaba el
     * parpadeo de datos mock en cada entrada.
     */
    private var perfil: UserProfile? = ProfileCache.perfil

    // Launcher para recibir resultado de EditarPerfilActivity
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // El perfil fue actualizado: se relee para que la tarjeta de arriba
            // y el diálogo de cuenta muestren el nombre y la foto nuevos.
            Toast.makeText(requireContext(), R.string.profile_guardado, Toast.LENGTH_SHORT).show()
            cargarPerfil()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragmento_ajustes_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets(view)
        setupTopBar(view)
        setupSettingsRows(view)

        // Con datos ya cargados (vuelta desde atrás) se repinta sin ir a Firestore.
        val cacheado = perfil
        if (cacheado != null) pintarUsuario(view, cacheado) else cargarPerfil()
    }

    override fun onResume() {
        super.onResume()
        // Status bar blanca con iconos oscuros para coincidir con el top bar claro
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
    //  WINDOW INSETS (STATUS BAR PADDING)
    // ─────────────────────────────────────────────────────────────

    private fun setupWindowInsets(root: View) {
        val topBar = root.findViewById<View>(R.id.topBarSettings) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, windowInsets ->
            val statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = statusBarHeight)
            windowInsets
        }
        ViewCompat.requestApplyInsets(topBar)
    }

    // ─────────────────────────────────────────────────────────────
    //  TOP BAR
    // ─────────────────────────────────────────────────────────────

    private fun setupTopBar(root: View) {
        root.findViewById<View>(R.id.btnSettingsBack)?.setOnClickListener {
            navigateBack()
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ROWS
    // ─────────────────────────────────────────────────────────────

    private fun setupSettingsRows(root: View) {

        // Edit Profile
        root.findViewById<View>(R.id.btnSettingsEditProfile)?.setOnClickListener {
            animateTap(it)
            openEditProfileScreen()
        }

        // Rol fijo: se define en el registro y ya no se puede cambiar.
        // La fila solo informa el rol actual de la cuenta.
        root.findViewById<View>(R.id.btnCambiarModo)?.setOnClickListener {
            animateTap(it)
            val rol = if (perfil?.activeRole == UserRoles.CONTRATANTE) "Contratante" else "Trabajador"
            Toast.makeText(
                requireContext(),
                "Tu rol es $rol y se definió al registrarte.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Section: Cuenta
        root.findViewById<View>(R.id.rowAccountInfo)?.setOnClickListener {
            animateTap(it)
            showAccountInfoDialog()
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
    //  DATOS REALES DEL USUARIO
    // ─────────────────────────────────────────────────────────────

    private fun cargarPerfil() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), R.string.profile_error_sesion, Toast.LENGTH_LONG)
                .show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repository.loadProfile(uid)
                .onSuccess { datos ->
                    perfil = datos
                    ProfileCache.perfil = datos
                    view?.let { pintarUsuario(it, datos) }
                }
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.profile_error_cargar, error.message.orEmpty()),
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    /** Rellena la tarjeta de usuario de la cabecera con datos de Firestore. */
    private fun pintarUsuario(root: View, datos: UserProfile) {
        root.findViewById<TextView>(R.id.tvSettingsUserName)?.text =
            datos.profile.fullName.ifBlank { getString(R.string.profile_sin_nombre) }

        // El correo es de la FASE 1 y además lo tiene Firebase Auth.
        val email = datos.auth.email.ifBlank { auth.currentUser?.email.orEmpty() }
        root.findViewById<TextView>(R.id.tvSettingsUserEmail)?.text =
            email.ifBlank { getString(R.string.settings_sin_correo) }

        // Sin foto se dibujan las iniciales: las cuentas de correo y contraseña
        // nunca traen imagen de Google.
        OficioIcons.cargarAvatar(
            root.findViewById<ShapeableImageView>(R.id.ivSettingsAvatar),
            datos.profile.profilePhotoUrl,
            datos.profile.fullName,
            datos.profile.username.ifBlank { datos.uid }
        )

        val roleTitle = root.findViewById<TextView>(R.id.tvRoleModeTitle)
        val roleSubtitle = root.findViewById<TextView>(R.id.tvRoleModeSubtitle)
        if (datos.activeRole == UserRoles.CONTRATANTE) {
            roleTitle?.setText(R.string.modo_contratante_titulo)
            roleSubtitle?.text = getString(R.string.modo_rol_fijo_contratante)
        } else {
            roleTitle?.setText(R.string.modo_trabajador_titulo)
            roleSubtitle?.text = getString(R.string.modo_rol_fijo_trabajador)
        }
    }

    /**
     * Diálogo de "Información de la cuenta".
     *
     * Muestra el `@usuario`, el teléfono, la ubicación y el documento de
     * identidad. Todo es de solo lectura: lo único editable es el perfil, y
     * para eso está el botón de arriba.
     */
    private fun showAccountInfoDialog() {
        val datos = perfil
        if (datos == null) {
            Toast.makeText(requireContext(), R.string.profile_error_cargar, Toast.LENGTH_SHORT)
                .show()
            return
        }

        val lineas = buildList {
            add(
                getString(
                    R.string.settings_campo_usuario,
                    if (datos.profile.username.isBlank()) {
                        getString(R.string.settings_sin_dato)
                    } else {
                        "@${datos.profile.username}"
                    }
                )
            )
            add(
                getString(
                    R.string.settings_campo_telefono,
                    datos.profile.phone.ifBlank { getString(R.string.settings_sin_dato) }
                )
            )
            add(
                getString(
                    R.string.settings_campo_ubicacion,
                    datos.profile.locationLabel.ifBlank { getString(R.string.settings_sin_dato) }
                )
            )
            add(
                getString(
                    R.string.settings_campo_documento,
                    datos.identity.verifiedLabel.ifBlank {
                        getString(R.string.profile_identidad_pendiente)
                    }
                )
            )
            add(
                getString(
                    R.string.settings_campo_registro,
                    datos.profile.fullName.ifBlank { getString(R.string.settings_sin_dato) }
                )
            )
        }

        AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setTitle(R.string.settings_section_account)
            .setMessage(lineas.joinToString("\n\n"))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // ─────────────────────────────────────────────────────────────
    //  LOGOUT DIALOG (BOTTOM SHEET)
    // ─────────────────────────────────────────────────────────────

    private fun showLogoutDialog() {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout_confirmacion, null)
        bottomSheet.setContentView(dialogView)

        // Fondo transparente en el contenedor del BottomSheet para permitir bordes superiores curvos
        (dialogView.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)

        dialogView.findViewById<View>(R.id.btnCloseLogoutDialog)?.setOnClickListener {
            bottomSheet.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnLogoutCancel)?.setOnClickListener {
            animateTap(it)
            bottomSheet.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnLogoutConfirm)?.setOnClickListener {
            animateTap(it)
            bottomSheet.dismiss()
            performLogout()
        }

        bottomSheet.show()
    }

    /**
     * Cierra la sesión de verdad.
     *
     * `signOut()` es la pieza que faltaba: mientras no se llame, la sesión sigue
     * viva y "volver a entrar" entra directo. Se vacía la pila de Activities
     * (`CLEAR_TASK | NEW_TASK`) para que el botón atrás no devuelva a la pantalla
     * de ajustes ya cerrada.
     */
    private fun performLogout() {
        auth.signOut()
        ProfileCache.limpiar()

        Toast.makeText(requireContext(), R.string.settings_sesion_cerrada, Toast.LENGTH_SHORT)
            .show()

        startActivity(
            Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        requireActivity().finish()
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun openEditProfileScreen() {
        val intent = Intent(requireContext(), EditarPerfilActivity::class.java)
        editProfileLauncher.launch(intent)
        
        // Aplicar transición compatible con todas las versiones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requireActivity().overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.dialog_slide_up,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            requireActivity().overridePendingTransition(R.anim.dialog_slide_up, android.R.anim.fade_out)
        }
    }

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
