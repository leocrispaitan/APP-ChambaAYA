package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.model.precioTexto
import com.proyecto.chambaya.data.repository.ApplicationRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.launch

/**
 * FASE 7 — Postularse a una chamba (o ver/retirar la postulación enviada).
 */
class ApplySheet : BottomSheetDialogFragment() {

    private val appRepo = ApplicationRepository()
    private val pubRepo = PublicationRepository()
    private var sending = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_apply, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val publicationId = requireArguments().getString(ARG_ID).orEmpty()
        if (publicationId.isBlank()) { dismiss(); return }
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        view.findViewById<View>(R.id.btnApplyCancel).setOnClickListener { dismiss() }
        view.findViewById<MaterialButton>(R.id.btnApplyWithdraw).setOnClickListener {
            confirmarRetiro(publicationId, uid)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val pub = pubRepo.getById(publicationId).getOrNull()
            if (pub == null || !isAdded) { dismiss(); return@launch }
            view.findViewById<TextView>(R.id.tvApplyJobTitle).text = pub.title
            val entidad = pub.publisher.name.ifBlank { getString(R.string.k_rol_contratante) }
            view.findViewById<TextView>(R.id.tvApplyJobMeta).text =
                "$entidad · ${pub.precioTexto(requireContext())} · ${pub.location.district.ifBlank { "Ayacucho" }}"

            val pendiente = appRepo.pendingFor(publicationId, uid).getOrNull()
            if (!isAdded) return@launch
            if (pendiente != null) {
                view.findViewById<TextView>(R.id.tvApplyTitle).text = getString(R.string.k_apl_enviada_titulo)
                view.findViewById<LinearLayout>(R.id.layoutApplyForm).visibility = View.GONE
                view.findViewById<LinearLayout>(R.id.layoutApplySent).visibility = View.VISIBLE
            } else {
                view.findViewById<View>(R.id.btnApplyConfirm).setOnClickListener {
                    enviar(publicationId, uid)
                }
            }
        }
    }

    private fun enviar(publicationId: String, uid: String) {
        if (sending) return
        if (uid.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.k_apl_login), Toast.LENGTH_SHORT).show()
            return
        }
        val cached = ProfileCache.perfil
        if (cached != null && !cached.roles.contains(UserRoles.TRABAJADOR)) {
            Toast.makeText(requireContext(), getString(R.string.k_apl_modo), Toast.LENGTH_LONG).show()
            return
        }
        sending = true
        val btn = view?.findViewById<MaterialButton>(R.id.btnApplyConfirm)
        btn?.apply { isEnabled = false; text = getString(R.string.k_apl_enviando) }
        val mensaje = view?.findViewById<TextInputEditText>(R.id.etApplyMessage)?.text?.toString().orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            var perfil = ProfileCache.perfil
            if (perfil == null) {
                perfil = com.proyecto.chambaya.data.repository.ProfileRepository()
                    .loadProfile(uid).getOrNull()
                if (perfil != null) ProfileCache.perfil = perfil
            }
            if (perfil == null || !perfil.roles.contains(UserRoles.TRABAJADOR)) {
                sending = false
                if (!isAdded) return@launch
                btn?.apply { isEnabled = true; text = getString(R.string.sheet_postular_enviar) }
                view?.findViewById<TextView>(R.id.tvApplyError)?.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.k_apl_modo)
                }
                return@launch
            }
            val pub = pubRepo.getById(publicationId).getOrNull()
            if (pub == null) {
                sending = false
                btn?.apply { isEnabled = true; text = getString(R.string.sheet_postular_enviar) }
                Toast.makeText(requireContext(), getString(R.string.k_apl_no_disponible), Toast.LENGTH_SHORT).show()
                return@launch
            }
            val r = appRepo.apply(requireContext(), uid, pub, perfil, mensaje)
            sending = false
            if (!isAdded) return@launch
            if (r.isSuccess) {
                Toast.makeText(requireContext(), getString(R.string.k_apl_exito), Toast.LENGTH_SHORT).show()
                parentFragmentManager.setFragmentResult(REQUEST_APPLIED, bundleOf(EXTRA_ID to publicationId))
                dismiss()
            } else {
                view?.findViewById<TextView>(R.id.tvApplyError)?.apply {
                    visibility = View.VISIBLE
                    text = r.exceptionOrNull()?.message ?: getString(R.string.k_apl_no_postular)
                }
                btn?.apply { isEnabled = true; text = getString(R.string.sheet_postular_enviar) }
            }
        }
    }

    private fun confirmarRetiro(publicationId: String, uid: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.sheet_postular_retirar)
            .setMessage(R.string.k_apl_retirar_msg)
            .setPositiveButton(R.string.item_retirar) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val pendiente = appRepo.pendingFor(publicationId, uid).getOrNull()
                    if (pendiente == null) { dismiss(); return@launch }
                    val r = appRepo.withdraw(requireContext(), uid, pendiente.applicationId)
                    if (!isAdded) return@launch
                    if (r.isSuccess) {
                        Toast.makeText(requireContext(), getString(R.string.k_apl_retirada), Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult(REQUEST_APPLIED, bundleOf(EXTRA_ID to publicationId))
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.k_apl_no_retirar), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
            .show()
    }

    companion object {
        const val REQUEST_APPLIED = "application_changed"
        const val EXTRA_ID = "publicationId"
        private const val ARG_ID = "publicationId"
        fun newInstance(publicationId: String) = ApplySheet().apply {
            arguments = bundleOf(ARG_ID to publicationId)
        }
    }
}
