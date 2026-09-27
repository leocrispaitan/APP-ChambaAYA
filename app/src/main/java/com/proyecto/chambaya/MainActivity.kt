package com.proyecto.chambaya

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.ui.chat.FragmentoMensajes
import com.proyecto.chambaya.ui.jobs.FragmentoChambas
import com.proyecto.chambaya.ui.map.FragmentoMapas
import com.proyecto.chambaya.ui.profile.FragmentoMiPerfil
import com.proyecto.chambaya.ui.profile.ProfileCache
import com.proyecto.chambaya.ui.publish.FragmentoPublicar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fragmentContainer: View

    private val jobsFragment by lazy { FragmentoChambas() }
    private val mapFragment by lazy { FragmentoMapas() }
    private val publishFragment by lazy { FragmentoPublicar() }
    private val chatFragment by lazy { FragmentoMensajes() }
    private val profileFragment by lazy { FragmentoMiPerfil() }
    private val bottomNavPopInterpolator = OvershootInterpolator(1.12f)
    private val bottomNavSettleInterpolator = DecelerateInterpolator()
    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.actividad_principal)

        fragmentContainer = findViewById(R.id.fragmentContainer)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // No pad top: each view draws its own background behind the status bar
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Push bottom nav items up while keeping the background stretched to the bottom edge
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom)
            insets
        }

        setupBottomNavigation()
        precargarPerfil()

        if (savedInstanceState == null) {
            switchToTab(R.id.nav_jobs)
        } else {
            activeFragment = supportFragmentManager.fragments.firstOrNull {
                it.id == R.id.fragmentContainer && !it.isHidden
            }
        }
    }

    /**
     * Adelanta la lectura de `users/{uid}` apenas se entra a `MainActivity`,
     * sin esperar a que el usuario toque la pestaña "Perfil".
     *
     * Antes, `FragmentoMiPerfil` solo empezaba a pedir el perfil cuando su
     * `onViewCreated` corría — es decir, la primera vez que el usuario tocaba
     * esa pestaña. Si el usuario navegaba antes por otras pestañas (Chambas,
     * Mapa, ...), ese tiempo no servía de nada: la consulta a Firestore ni
     * siquiera había empezado. Entonces, al final, tanto "Mi Perfil" como
     * "Ajustes" mostraban el mismo parpadeo aunque el usuario llevara rato
     * usando la app.
     *
     * `MainActivity` es el único punto de entrada real al shell de la app
     * (login, acceso rápido y registro navegan todos aquí), así que es el
     * lugar correcto para arrancar esta lectura una sola vez por sesión de
     * proceso: cuando el usuario finalmente entra a "Mi Perfil" o a
     * "Ajustes", lo más probable es que [ProfileCache] ya tenga el dato listo
     * y no haya nada que esperar.
     *
     * Es "fire and forget": no toca ninguna vista de `MainActivity`, no
     * muestra ningún Toast y no bloquea `switchToTab`. Si falla (sin red, por
     * ejemplo), no pasa nada aquí — `FragmentoMiPerfil` reintenta solo cuando
     * el usuario entra de verdad, y ahí sí avisa si algo salió mal.
     */
    private fun precargarPerfil() {
        if (ProfileCache.perfil != null) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        lifecycleScope.launch {
            ProfileRepository().loadProfile(uid)
                .onSuccess { ProfileCache.perfil = it }
                // Fallo silencioso a propósito: ver el comentario de la función.
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            animateBottomNavSelection(item.itemId)

            when (item.itemId) {
                R.id.nav_jobs -> {
                    switchToTab(R.id.nav_jobs)
                    true
                }
                R.id.nav_map -> {
                    switchToTab(R.id.nav_map)
                    true
                }
                R.id.nav_publish -> {
                    switchToTab(R.id.nav_publish)
                    true
                }
                R.id.nav_chat -> {
                    switchToTab(R.id.nav_chat)
                    true
                }
                R.id.nav_profile -> {
                    switchToTab(R.id.nav_profile)
                    true
                }
                else -> false
            }
        }

        bottomNavigation.setOnItemReselectedListener { item ->
            animateBottomNavSelection(item.itemId)
        }

        bottomNavigation.post {
            val selectedItemId = bottomNavigation.selectedItemId.takeIf { it != View.NO_ID }
                ?: R.id.nav_jobs
            animateBottomNavSelection(selectedItemId, animate = false)
        }
    }

    private fun animateBottomNavSelection(selectedItemId: Int, animate: Boolean = true) {
        val menuView = bottomNavigation.getChildAt(0) as? ViewGroup ?: return

        for (index in 0 until menuView.childCount) {
            val itemView = menuView.getChildAt(index)
            val isSelected = itemView.id == selectedItemId
            val targetScale = if (isSelected) 1.06f else 1f
            val targetAlpha = if (isSelected) 1f else 0.78f

            itemView.animate().cancel()

            if (!animate) {
                itemView.scaleX = targetScale
                itemView.scaleY = targetScale
                itemView.alpha = targetAlpha
                continue
            }

            itemView.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .alpha(targetAlpha)
                .setDuration(if (isSelected) 230L else 170L)
                .setInterpolator(if (isSelected) bottomNavPopInterpolator else bottomNavSettleInterpolator)
                .start()
        }
    }

    fun navigateToTab(tabId: Int) {
        bottomNavigation.selectedItemId = tabId
    }

    /** Oculta el bottom nav con animación suave (para pantallas de ajustes, etc.). */
    fun hideBottomNav() {
        if (bottomNavigation.visibility == View.GONE) return
        bottomNavigation.animate()
            .translationY(bottomNavigation.height.toFloat())
            .alpha(0f)
            .setDuration(220)
            .withEndAction { bottomNavigation.visibility = View.GONE }
            .start()
    }

    /** Restaura el bottom nav con animación suave. */
    fun showBottomNav() {
        if (bottomNavigation.visibility == View.VISIBLE) return
        bottomNavigation.visibility = View.VISIBLE
        bottomNavigation.translationY = bottomNavigation.height.toFloat()
        bottomNavigation.alpha = 0f
        bottomNavigation.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(240)
            .start()
    }

    private fun switchToTab(tabId: Int) {
        val tag = tabTag(tabId)
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: createFragmentForTab(tabId)
        if (fragment == activeFragment) return

        val transaction = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)

        activeFragment?.let { current ->
            transaction
                .hide(current)
                .setMaxLifecycle(current, Lifecycle.State.STARTED)
        }

        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(fragmentContainer.id, fragment, tag)
        }

        transaction
            .setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
            .commit()

        activeFragment = fragment
    }

    private fun createFragmentForTab(tabId: Int): Fragment {
        return when (tabId) {
            R.id.nav_jobs -> jobsFragment
            R.id.nav_map -> mapFragment
            R.id.nav_publish -> publishFragment
            R.id.nav_chat -> chatFragment
            R.id.nav_profile -> profileFragment
            else -> jobsFragment
        }
    }

    private fun tabTag(tabId: Int): String {
        return when (tabId) {
            R.id.nav_jobs -> TAG_JOBS
            R.id.nav_map -> TAG_MAP
            R.id.nav_publish -> TAG_PUBLISH
            R.id.nav_chat -> TAG_CHAT
            R.id.nav_profile -> TAG_PROFILE
            else -> TAG_JOBS
        }
    }

    companion object {
        private const val TAG_JOBS = "tab_jobs"
        private const val TAG_MAP = "tab_map"
        private const val TAG_PUBLISH = "tab_publish"
        private const val TAG_CHAT = "tab_chat"
        private const val TAG_PROFILE = "tab_profile"
    }
}