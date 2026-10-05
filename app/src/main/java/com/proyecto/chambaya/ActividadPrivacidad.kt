package com.proyecto.chambaya

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.data.repository.ProfileRepository
import kotlinx.coroutines.launch

/**
 * Privacidad y Permisos — modo claro.
 * Los interruptores de visibilidad se guardan de verdad en `users/{uid}.privacy`.
 * Los permisos del dispositivo abren los ajustes del sistema.
 */
class ActividadPrivacidad : AppCompatActivity() {

    private val repo = ProfileRepository()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    private var guardando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        IdiomaManager.applySavedLanguage(this)
        super.onCreate(savedInstanceState)
        BarraEstadoUtils.aplicarColor(this, android.graphics.Color.parseColor("#F7F8FA"))
        setContentView(R.layout.actividad_privacidad)

        findViewById<View>(R.id.btnPrivacidadAtras)?.setOnClickListener { finish() }
        findViewById<View>(R.id.filaPermisoCamara)?.setOnClickListener { abrirAjustesApp() }
        findViewById<View>(R.id.filaPermisoUbicacion)?.setOnClickListener { abrirAjustesApp() }

        // Entrada escalonada M3.
        listOfNotNull(
            findViewById(R.id.tarjetaEscudo),
            findViewById(R.id.tarjetaPermisos),
            findViewById(R.id.tarjetaVisibilidad)
        ).forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = 24f
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay((i * 25).toLong()).setDuration(250)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        val swTel = findViewById<SwitchMaterial>(R.id.switchTelefono)
        val swMail = findViewById<SwitchMaterial>(R.id.switchCorreo)
        val swDir = findViewById<SwitchMaterial>(R.id.switchDireccion)
        // Los listeners se enganchan tras la carga inicial para no disparar guardados.
        cargarVisibilidad(swTel, swMail, swDir)
    }

    override fun onResume() {
        super.onResume()
        pintarPermisos()
    }

    private fun cargarVisibilidad(
        swTel: SwitchMaterial, swMail: SwitchMaterial, swDir: SwitchMaterial
    ) {
        val id = uid
        if (id == null) {
            Toast.makeText(this, R.string.priv_sesion, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        lifecycleScope.launch {
            repo.loadProfile(id)
                .onSuccess { p ->
                    swTel.isChecked = p.privacy.showPhone
                    swMail.isChecked = p.privacy.showEmail
                    swDir.isChecked = p.privacy.showExactAddress
                    pintarNivel()
                    val guardar = { guardarVisibilidad() }
                    swTel.setOnCheckedChangeListener { _, _ -> guardar() }
                    swMail.setOnCheckedChangeListener { _, _ -> guardar() }
                    swDir.setOnCheckedChangeListener { _, _ -> guardar() }
                }
                .onFailure {
                    Toast.makeText(this@ActividadPrivacidad, R.string.priv_error_cargar, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun guardarVisibilidad() {
        if (guardando) return
        val id = uid ?: return
        guardando = true
        val tel = findViewById<SwitchMaterial>(R.id.switchTelefono).isChecked
        val mail = findViewById<SwitchMaterial>(R.id.switchCorreo).isChecked
        val dir = findViewById<SwitchMaterial>(R.id.switchDireccion).isChecked
        lifecycleScope.launch {
            val r = repo.updatePrivacy(id, tel, mail, dir)
            guardando = false
            if (r.isFailure) {
                Toast.makeText(this@ActividadPrivacidad, R.string.priv_error_guardar, Toast.LENGTH_SHORT).show()
            } else {
                pintarNivel(animar = true)
            }
        }
    }

    private fun pintarNivel(animar: Boolean = false) {
        val ocultos = listOf(
            findViewById<SwitchMaterial>(R.id.switchTelefono).isChecked,
            findViewById<SwitchMaterial>(R.id.switchCorreo).isChecked,
            findViewById<SwitchMaterial>(R.id.switchDireccion).isChecked
        ).count { !it }
        val pct = ocultos * 100 / 3
        findViewById<TextView>(R.id.tvNivelPrivacidad)?.text = getString(R.string.k_priv_nivel_fmt, ocultos)
        val barra = findViewById<ProgressBar>(R.id.barraNivel) ?: return
        if (animar) {
            ObjectAnimator.ofInt(barra.progress, pct).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                addUpdateListener { barra.progress = it.animatedValue as Int }
                start()
            }
        } else {
            barra.progress = pct
        }
    }

    private fun pintarPermisos() {
        val cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        val ubi = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        findViewById<TextView>(R.id.tvEstadoCamara)?.apply {
            text = if (cam) getString(R.string.priv_perm_ok_camara) else getString(R.string.priv_perm_no_camara)
            setTextColor(ContextCompat.getColor(this@ActividadPrivacidad, if (cam) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        }
        findViewById<TextView>(R.id.tvEstadoUbicacion)?.apply {
            text = if (ubi) getString(R.string.priv_perm_ok_ubi) else getString(R.string.priv_perm_no_ubi)
            setTextColor(ContextCompat.getColor(this@ActividadPrivacidad, if (ubi) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
        }
    }

    private fun abrirAjustesApp() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }
}
