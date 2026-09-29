package com.proyecto.chambaya.ui.jobs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.precioTexto
import com.proyecto.chambaya.data.model.publicationTimeAgo
import com.proyecto.chambaya.data.repository.PublicationInteractionRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.launch

/**
 * FASE 6 — Vista detallada de la chamba (BottomSheet).
 *
 * Muestra la publicación completa con jerarquía profesional y acciones según
 * el rol:
 *  - trabajador / visitante → Postularme (Fase 7: placeholder informativo),
 *    Guardar, Compartir
 *  - dueño contratante → Pausar / Reactivar / Finalizar
 */
class JobDetailSheet : BottomSheetDialogFragment() {

    private val pubRepo = PublicationRepository()
    private val interRepo = PublicationInteractionRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_job_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val publicationId = requireArguments().getString(ARG_ID).orEmpty()
        if (publicationId.isBlank()) { dismiss(); return }

        viewLifecycleOwner.lifecycleScope.launch {
            val pub = pubRepo.getById(publicationId).getOrNull()
            if (pub == null) {
                Toast.makeText(requireContext(), "La publicación ya no está disponible.", Toast.LENGTH_SHORT).show()
                dismiss()
                return@launch
            }
            pintar(view, publicationId)
            pubRepo.registerView(publicationId)
        }
    }

    private suspend fun pintar(view: View, publicationId: String) {
        val pub = pubRepo.getById(publicationId).getOrNull() ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val isOwner = uid.isNotBlank() && pub.ownerUid == uid
        var saved = if (uid.isBlank()) false else interRepo.isSaved(publicationId, uid)

        // ── Publicador ──
        view.findViewById<TextView>(R.id.tvDetailName).text =
            pub.publisher.name.ifBlank { "Contratante ChambAYA" }
        val username = pub.publisher.username.ifBlank { "chambaya" }
        view.findViewById<TextView>(R.id.tvDetailMeta).text =
            "@$username · ${publicationTimeAgo(pub.createdAt)}"
        view.findViewById<ImageView>(R.id.ivDetailVerified).visibility =
            if (pub.publisher.verified) View.VISIBLE else View.GONE
        val ivAvatar = view.findViewById<ImageView>(R.id.ivDetailAvatar)
        if (pub.publisher.photoUrl.isNotBlank()) {
            ivAvatar.load(pub.publisher.photoUrl) {
                crossfade(true); placeholder(R.drawable.ic_user_circle); error(R.drawable.ic_user_circle)
            }
        }
        view.findViewById<View>(R.id.rowPublisher).setOnClickListener {
            dismiss()
            parentFragmentManager.setFragmentResult(
                REQUEST_OPEN_PROFILE,
                bundleOf(EXTRA_UID to pub.publisher.uid.ifBlank { pub.ownerUid })
            )
        }

        // ── Contenido ──
        view.findViewById<TextView>(R.id.tvDetailTitle).text = pub.title
        view.findViewById<TextView>(R.id.tvDetailCategory).text = pub.category.ifBlank { "Chamba" }
        val libres = (pub.workersNeeded - pub.workersHired).coerceAtLeast(0)
        view.findViewById<TextView>(R.id.tvDetailStatus).text =
            if (libres <= 0) "Vacantes cubiertas" else "$libres vacante${if (libres == 1) "" else "s"}"

        val cardPhoto = view.findViewById<MaterialCardView>(R.id.cardDetailPhoto)
        val firstImage = pub.images.firstOrNull()?.url.orEmpty()
        if (firstImage.isNotBlank()) {
            cardPhoto.visibility = View.VISIBLE
            view.findViewById<ImageView>(R.id.ivDetailPhoto).load(firstImage) { crossfade(true) }
        } else cardPhoto.visibility = View.GONE

        view.findViewById<TextView>(R.id.tvDetailPrice).text = pub.precioTexto()
        view.findViewById<TextView>(R.id.tvDetailNegotiable).visibility =
            if (pub.payment.negotiable) View.VISIBLE else View.GONE
        view.findViewById<TextView>(R.id.tvDetailDistrict).text =
            pub.location.district.ifBlank { "Ayacucho" }

        val horario = listOf(pub.schedule.startTime, pub.schedule.endTime)
            .filter { it.isNotBlank() }.joinToString(" – ")
        view.findViewById<TextView>(R.id.tvDetailSchedule).text = horario.ifBlank { "A convenir" }
        view.findViewById<TextView>(R.id.tvDetailWorkers).text =
            "${pub.workersNeeded} persona${if (pub.workersNeeded == 1) "" else "s"}"
        view.findViewById<TextView>(R.id.tvDetailDate).text = publicationTimeAgo(pub.createdAt)
        val wp = pub.workplaceName.ifBlank { "Por definir" }
        view.findViewById<TextView>(R.id.tvDetailWorkplace).text = wp

        view.findViewById<TextView>(R.id.tvDetailDescription).text = pub.description

        val chips = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipsSkills)
        chips.removeAllViews()
        pub.skillsRequired.take(8).forEach { skill ->
            val chip = Chip(requireContext()).apply { text = skill; isClickable = false }
            chips.addView(chip)
        }
        chips.visibility = if (pub.skillsRequired.isEmpty()) View.GONE else View.VISIBLE

        fun statsText(likes: Long, saves: Long) =
            "${formatCount(likes)} me gusta · ${formatCount(saves)} guardados · ${formatCount(pub.statistics.views + 1)} vistas"
        view.findViewById<TextView>(R.id.tvDetailStats).text = statsText(pub.statistics.likes, pub.statistics.saves)

        // ── Acciones por rol ──
        val workerActions = view.findViewById<LinearLayout>(R.id.layoutWorkerActions)
        val ownerActions = view.findViewById<LinearLayout>(R.id.layoutOwnerActions)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnDetailSave)
        val btnApply = view.findViewById<MaterialButton>(R.id.btnDetailApply)

        if (isOwner) {
            workerActions.visibility = View.GONE
            ownerActions.visibility = View.VISIBLE
            val btnPause = view.findViewById<MaterialButton>(R.id.btnOwnerPause)
            btnPause.text = if (pub.status == "PAUSED") "Reactivar" else "Pausar"
            btnPause.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val nuevo = if (pub.status == "PAUSED") "ACTIVE" else "PAUSED"
                    val r = pubRepo.changeStatus(uid, publicationId, nuevo)
                    if (r.isSuccess) {
                        Toast.makeText(requireContext(), if (nuevo == "PAUSED") "Publicación pausada." else "Publicación reactivada.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult(REQUEST_CHANGED, bundleOf())
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), r.exceptionOrNull()?.message ?: "No se pudo actualizar.", Toast.LENGTH_LONG).show()
                    }
                }
            }
            view.findViewById<MaterialButton>(R.id.btnOwnerFinish).setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = pubRepo.changeStatus(uid, publicationId, "FINISHED")
                    if (r.isSuccess) {
                        Toast.makeText(requireContext(), "Publicación finalizada.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult(REQUEST_CHANGED, bundleOf())
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), r.exceptionOrNull()?.message ?: "No se pudo finalizar.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            workerActions.visibility = View.VISIBLE
            ownerActions.visibility = View.GONE

            fun paintSave() {
                btnSave.icon = androidx.core.content.ContextCompat.getDrawable(
                    requireContext(),
                    if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
                )
            }
            paintSave()
            btnSave.setOnClickListener {
                if (uid.isBlank()) {
                    Toast.makeText(requireContext(), "Inicia sesión para guardar.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = interRepo.toggleSave(publicationId, uid)
                    if (r.isSuccess) {
                        saved = r.getOrDefault(false)
                        paintSave()
                        Toast.makeText(requireContext(), if (saved) "Guardado en tu lista." else "Quitado de guardados.", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult(REQUEST_CHANGED, bundleOf())
                    }
                }
            }
            view.findViewById<MaterialButton>(R.id.btnDetailShare).setOnClickListener {
                compartir(pub.title, pub.description, pub.precioTexto(), pub.location.district)
                viewLifecycleOwner.lifecycleScope.launch { pubRepo.registerShare(publicationId) }
            }
            val esTrabajador = ProfileCache.perfil?.roles?.contains("TRABAJADOR") != false
            btnApply.isEnabled = esTrabajador
            btnApply.alpha = if (esTrabajador) 1f else 0.5f
            btnApply.setOnClickListener {
                // Fase 7: las postulaciones aún no existen; placeholder honesto.
                Toast.makeText(
                    requireContext(),
                    "Las postulaciones llegan en la Fase 7. Guarda la chamba para no perderla.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun compartir(titulo: String, desc: String, precio: String, distrito: String) {
        val texto = "📢 $titulo\n\n💰 $precio\n📍 $distrito\n\n${desc.take(300)}\n\n🔗 Compartido desde ChambAYA"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Chamba: $titulo")
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, "Compartir chamba"))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {
        const val REQUEST_CHANGED = "job_detail_changed"
        const val REQUEST_OPEN_PROFILE = "job_detail_open_profile"
        const val EXTRA_UID = "uid"
        private const val ARG_ID = "publicationId"
        fun newInstance(publicationId: String) = JobDetailSheet().apply {
            arguments = bundleOf(ARG_ID to publicationId)
        }
    }
}
