package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.WorkplaceRepository
import kotlinx.coroutines.launch

/**
 * FASE 6 / FASE 17 (adelanto parcial) — Perfil público del contratante.
 *
 * Solo datos públicos: foto, nombre, @usuario, verificación, sector,
 * publicaciones, contrataciones, calificación y lugar. Nunca DNI, correo,
 * teléfono ni dirección exacta.
 */
class PublicProfileSheet : BottomSheetDialogFragment() {

    private val profileRepo = ProfileRepository()
    private val placeRepo = WorkplaceRepository()

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
            val perfil = profileRepo.loadProfile(uid).getOrNull()
            progress.visibility = View.GONE
            if (perfil == null) {
                error.visibility = View.VISIBLE
                error.text = "No se pudo cargar el perfil."
                return@launch
            }
            val nombre = perfil.employer.businessName.ifBlank { perfil.profile.fullName.ifBlank { "Contratante ChambAYA" } }
            view.findViewById<TextView>(R.id.tvPublicName).text = nombre
            val tipo = perfil.employer.employerType.ifBlank { "Contratante" }
            view.findViewById<TextView>(R.id.tvPublicUsername).text =
                "@${perfil.profile.username.ifBlank { "chambaya" }} · $tipo"
            view.findViewById<ImageView>(R.id.ivPublicVerified).visibility =
                if (perfil.identity.identityVerified) View.VISIBLE else View.GONE
            view.findViewById<TextView>(R.id.tvPublicPublished).text = perfil.employer.publishedCount.toString()
            view.findViewById<TextView>(R.id.tvPublicHired).text = perfil.employer.hiredCount.toString()
            view.findViewById<TextView>(R.id.tvPublicRating).text =
                if (perfil.employer.ratingCount > 0) String.format("%.1f", perfil.employer.ratingAverage) else "—"

            val bio = perfil.profile.bio.trim()
            val tvBio = view.findViewById<TextView>(R.id.tvPublicBio)
            if (bio.isNotBlank()) { tvBio.visibility = View.VISIBLE; tvBio.text = bio }

            val avatar = view.findViewById<ImageView>(R.id.ivPublicAvatar)
            if (perfil.profile.profilePhotoUrl.isNotBlank()) {
                avatar.load(perfil.profile.profilePhotoUrl) {
                    crossfade(true); placeholder(R.drawable.ic_user_circle); error(R.drawable.ic_user_circle)
                }
            }

            val workplaceId = perfil.employer.workplaceId.orEmpty()
            if (workplaceId.isNotBlank()) {
                val lugar = placeRepo.loadById(workplaceId).getOrNull()
                if (lugar != null) {
                    view.findViewById<View>(R.id.rowPublicPlace).visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.tvPublicPlaceName).text = lugar.name
                    view.findViewById<TextView>(R.id.tvPublicPlaceDistrict).text =
                        listOf(lugar.district, lugar.province).filter { it.isNotBlank() }.joinToString(", ")
                }
            }
        }
    }

    companion object {
        private const val ARG_UID = "uid"
        fun newInstance(uid: String) = PublicProfileSheet().apply {
            arguments = bundleOf(ARG_UID to uid)
        }
    }
}
