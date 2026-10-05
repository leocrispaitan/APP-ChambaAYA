package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.repository.BlockRepository
import com.proyecto.chambaya.data.repository.ChatRepository
import com.proyecto.chambaya.data.repository.JobRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.PublicationRepository
import com.proyecto.chambaya.data.repository.RatingRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Perfil público (FASE 6/7, base de FASE 17): contratante Y trabajador.
 *
 * Lee `public_profiles/{uid}` (visible para todos) y calcula los agregados
 * en vivo (publicaciones, trabajos, reputación): nunca muestra DNI, correo,
 * teléfono ni dirección exacta.
 */
class PublicProfileSheet : BottomSheetDialogFragment() {

    private val profileRepo = ProfileRepository()
    private val placeRepo = WorkplaceRepository()
    private val pubRepo = PublicationRepository()
    private val jobRepo = JobRepository()
    private val ratingRepo = RatingRepository()
    private val chatRepo = ChatRepository()
    private val blockRepo = BlockRepository()
    private var loadedProfile: com.proyecto.chambaya.data.model.PublicProfile? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_public_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = requireArguments().getString(ARG_UID).orEmpty()
        if (uid.isBlank()) { dismiss(); return }
        val progress = view.findViewById<ProgressBar>(R.id.progressPublic)
        val error = view.findViewById<TextView>(R.id.tvPublicError)
        progress.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val perfil = profileRepo.loadPublicProfile(uid).getOrNull()
            if (perfil == null || !isAdded) {
                progress.visibility = View.GONE
                if (perfil == null) {
                    error.visibility = View.VISIBLE
                    error.text = getString(R.string.k_pub_no_cargar)
                }
                return@launch
            }
            loadedProfile = perfil
            val esEmpresa = perfil.employer.enabled

            // ── Identidad ──
            view.findViewById<TextView>(R.id.tvPublicName).text = perfil.displayName()
            val rol = if (esEmpresa) {
                perfil.employer.commercialName.ifBlank {
                    perfil.employer.businessName.ifBlank {
                        perfil.employer.employerType.ifBlank { "Contratante" }
                    }
                }
            } else "Trabajador"
            view.findViewById<TextView>(R.id.tvPublicUsername).text =
                "@${perfil.username.ifBlank { "chambaya" }} · $rol"
            view.findViewById<ImageView>(R.id.ivPublicVerified).visibility =
                if (perfil.identityVerified) View.VISIBLE else View.GONE
            val avatar = view.findViewById<ImageView>(R.id.ivPublicAvatar)
            if (perfil.photoUrl.isNotBlank()) {
                avatar.load(perfil.photoUrl) {
                    crossfade(true); placeholder(R.drawable.ic_user_circle); error(R.drawable.ic_user_circle)
                }
            }
            val bio = perfil.bio.trim()
            val tvBio = view.findViewById<TextView>(R.id.tvPublicBio)
            if (bio.isNotBlank()) { tvBio.visibility = View.VISIBLE; tvBio.text = bio }

            // ── FASE 17: distrito + oficios (sin datos privados) ──
            val distrito = listOf(perfil.district, perfil.province)
                .filter { it.isNotBlank() }.joinToString(", ")
            if (distrito.isNotBlank()) {
                view.findViewById<TextView>(R.id.tvPublicDistrict).apply {
                    visibility = View.VISIBLE
                    text = distrito
                }
            }
            // Trabajador: especialidades + habilidades. Contratante: sector.
            val oficios = if (!esEmpresa) {
                (perfil.worker.specialties + perfil.worker.skills).distinct().take(6)
                    .joinToString(" · ")
            } else {
                perfil.employer.sector.trim()
            }
            if (oficios.isNotBlank()) {
                view.findViewById<TextView>(R.id.tvPublicSpecialties).apply {
                    visibility = View.VISIBLE
                    text = oficios
                }
            }

            // ── Lugar (solo empresa con lugar) ──
            val workplaceId = perfil.employer.workplaceId.orEmpty()
            if (esEmpresa && workplaceId.isNotBlank()) {
                val lugar = placeRepo.loadById(workplaceId).getOrNull()
                if (lugar != null && isAdded) {
                    view.findViewById<View>(R.id.rowPublicPlace).visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.tvPublicPlaceName).text = lugar.name
                    view.findViewById<TextView>(R.id.tvPublicPlaceDistrict).text =
                        listOf(lugar.district, lugar.province).filter { it.isNotBlank() }.joinToString(", ")
                }
            }

            // ── Agregados en vivo ──
            val pubsD = async { pubRepo.byOwner(uid, 100).getOrNull().orEmpty() }
            val jobsEmpD = async { if (esEmpresa) jobRepo.listByEmployer(uid, 100).getOrNull().orEmpty() else emptyList() }
            val jobsWorkD = async { if (!esEmpresa) jobRepo.listByWorker(uid, 100).getOrNull().orEmpty() else emptyList() }
            val ratingsD = async { ratingRepo.receivedBy(uid, 100).getOrNull().orEmpty() }
            val pubs = pubsD.await()
            val jobsEmp = jobsEmpD.await()
            val jobsWork = jobsWorkD.await()
            val ratings = ratingsD.await()
            if (!isAdded) return@launch
            progress.visibility = View.GONE

            if (esEmpresa) {
                view.findViewById<TextView>(R.id.tvStatLabel1).text = getString(R.string.sheet_perfpublic_publicadas)
                view.findViewById<TextView>(R.id.tvStatLabel2).text = getString(R.string.sheet_perfpublic_contrataciones)
                view.findViewById<TextView>(R.id.tvPublicPublished).text = pubs.size.toString()
                view.findViewById<TextView>(R.id.tvPublicHired).text =
                    jobsEmp.count { it.status != JobStatus.CANCELLED }.toString()
            } else {
                view.findViewById<TextView>(R.id.tvStatLabel1).text = getString(R.string.k_pub_trabajos)
                view.findViewById<TextView>(R.id.tvStatLabel2).text = getString(R.string.profile_experience_label)
                view.findViewById<TextView>(R.id.tvPublicPublished).text =
                    jobsWork.count { it.status == JobStatus.COMPLETED }.toString()
                view.findViewById<TextView>(R.id.tvPublicHired).text =
                    if (perfil.worker.experienceYears > 0) getString(R.string.profile_anios_experiencia, perfil.worker.experienceYears) else "—"
            }
            // FASE 17/19: promedio + nº de calificaciones (reputación visible).
            view.findViewById<TextView>(R.id.tvPublicRating).text =
                if (ratings.isNotEmpty()) {
                    String.format("%.1f (%d)", ratings.map { it.rating }.average(), ratings.size)
                } else "—"

            configurarAcciones(view, uid)
        }
    }

    /** Mensaje + bloquear/denunciar (FASE 12/13). Se oculta viéndose a sí mismo. */
    private fun configurarAcciones(view: View, uid: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val btnMsg = view.findViewById<View>(R.id.btnPublicMessage)
        val btnMore = view.findViewById<View>(R.id.btnPublicMore)
        if (me.isBlank() || me == uid) {
            btnMsg.visibility = View.GONE
            btnMore.visibility = View.GONE
            return
        }
        val publicationId = requireArguments().getString(ARG_PUB).orEmpty()
        val publicationTitle = requireArguments().getString(ARG_PUB_TITLE).orEmpty()
        btnMsg.setOnClickListener {
            it.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val r = chatRepo.ensureConversation(me, uid, publicationId, publicationTitle)
                if (!isAdded) return@launch
                it.isEnabled = true
                if (r.isSuccess) {
                    val conv = r.getOrThrow()
                    val intent = android.content.Intent(
                        requireContext(),
                        com.proyecto.chambaya.ui.chat.ActividadChatDetalle::class.java
                    ).apply {
                        putExtra(
                            com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_CONV_ID,
                            conv.conversationId
                        )
                        putExtra(
                            com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_OTHER_UID,
                            uid
                        )
                        putExtra(
                            com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_NOMBRE,
                            loadedProfile?.displayName()
                        )
                        putExtra(
                            com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_FOTO,
                            loadedProfile?.photoUrl.orEmpty()
                        )
                        putExtra(
                            com.proyecto.chambaya.ui.chat.ActividadChatDetalle.EXTRA_PUB_TITULO,
                            publicationTitle
                        )
                    }
                    // La foto viaja por el perfil público en la apertura.
                    dismiss()
                    startActivity(intent)
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        r.exceptionOrNull()?.message ?: getString(R.string.k_chat_no_abrir),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        btnMore.setOnClickListener { anchor ->
            val menu = androidx.appcompat.widget.PopupMenu(requireContext(), anchor)
            menu.menu.add(0, 1, 0, getString(R.string.k_comun_bloquear))
            menu.menu.add(0, 2, 0, getString(R.string.k_comun_denunciar))
            menu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> confirmarBloqueo(uid)
                    2 -> denunciar(uid)
                }
                true
            }
            menu.show()
        }
    }

    private fun confirmarBloqueo(uid: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.k_pub_bloquear_titulo)
            .setMessage(R.string.k_pub_bloquear_msg)
            .setPositiveButton(R.string.k_comun_bloquear) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val ya = blockRepo.isBlocked(me, uid)
                    val r = if (ya) blockRepo.unblock(me, uid) else blockRepo.block(me, uid)
                    if (!isAdded) return@launch
                    android.widget.Toast.makeText(
                        requireContext(),
                        when {
                            r.isFailure -> getString(R.string.k_pub_no_completar)
                            ya -> getString(R.string.k_pub_desbloqueado_ok)
                            else -> getString(R.string.k_pub_bloqueado_ok)
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                    if (r.isSuccess && !ya) dismiss()
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
            .show()
    }

    private fun denunciar(uid: String) {
        val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (me.isBlank()) return
        val motivos = arrayOf(getString(R.string.k_razon_spam), getString(R.string.k_razon_acoso), getString(R.string.k_razon_fraude_estafa), getString(R.string.k_razon_inapropiado), getString(R.string.k_razon_otro))
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.k_pub_denunciar_titulo)
            .setItems(motivos) { _, cual ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val r = blockRepo.reportUser(me, uid, motivos[cual])
                    if (isAdded) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            if (r.isSuccess) getString(R.string.k_com_denunciar_enviar) else getString(R.string.k_comun_no_enviar),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(R.string.k_comun_cancelar, null)
            .show()
    }

    companion object {
        private const val ARG_UID = "uid"
        private const val ARG_PUB = "publicationId"
        private const val ARG_PUB_TITLE = "publicationTitle"
        fun newInstance(uid: String, publicationId: String = "", publicationTitle: String = "") =
            PublicProfileSheet().apply {
                arguments = androidx.core.os.bundleOf(
                    ARG_UID to uid,
                    ARG_PUB to publicationId,
                    ARG_PUB_TITLE to publicationTitle
                )
            }
    }
}
