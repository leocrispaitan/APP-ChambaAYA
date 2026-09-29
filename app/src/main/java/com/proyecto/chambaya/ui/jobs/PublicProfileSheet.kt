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
import com.proyecto.chambaya.data.model.JobStatus
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
                    error.text = "No se pudo cargar el perfil."
                }
                return@launch
            }
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
                view.findViewById<TextView>(R.id.tvStatLabel1).text = "Publicadas"
                view.findViewById<TextView>(R.id.tvStatLabel2).text = "Contrataciones"
                view.findViewById<TextView>(R.id.tvPublicPublished).text = pubs.size.toString()
                view.findViewById<TextView>(R.id.tvPublicHired).text =
                    jobsEmp.count { it.status != JobStatus.CANCELLED }.toString()
            } else {
                view.findViewById<TextView>(R.id.tvStatLabel1).text = "Trabajos"
                view.findViewById<TextView>(R.id.tvStatLabel2).text = "Experiencia"
                view.findViewById<TextView>(R.id.tvPublicPublished).text =
                    jobsWork.count { it.status == JobStatus.COMPLETED }.toString()
                view.findViewById<TextView>(R.id.tvPublicHired).text =
                    if (perfil.worker.experienceYears > 0) "${perfil.worker.experienceYears} años" else "—"
            }
            view.findViewById<TextView>(R.id.tvPublicRating).text =
                if (ratings.isNotEmpty()) {
                    String.format("%.1f", ratings.map { it.rating }.average())
                } else "—"
        }
    }

    companion object {
        private const val ARG_UID = "uid"
        fun newInstance(uid: String) = PublicProfileSheet().apply {
            arguments = bundleOf(ARG_UID to uid)
        }
    }
}
