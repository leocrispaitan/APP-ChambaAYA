package com.proyecto.chambaya.ui.jobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.RatingLimits
import com.proyecto.chambaya.data.repository.JobRepository
import com.proyecto.chambaya.data.repository.RatingRepository
import com.proyecto.chambaya.ui.profile.ProfileCache
import kotlinx.coroutines.launch

/**
 * FASE 9 — Calificar un trabajo completado (1-5 estrellas + comentario).
 */
class RateSheet : BottomSheetDialogFragment() {

    private val jobRepo = JobRepository()
    private val ratingRepo = RatingRepository()
    private var stars = 0
    private var sending = false
    private lateinit var starButtons: List<ImageButton>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_rate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val jobId = requireArguments().getString(ARG_ID).orEmpty()
        if (jobId.isBlank()) { dismiss(); return }

        starButtons = listOf(
            view.findViewById(R.id.star1), view.findViewById(R.id.star2),
            view.findViewById(R.id.star3), view.findViewById(R.id.star4),
            view.findViewById(R.id.star5)
        )
        starButtons.forEachIndexed { i, btn -> btn.setOnClickListener { fijarEstrellas(i + 1) } }

        viewLifecycleOwner.lifecycleScope.launch {
            val job = jobRepo.getById(jobId).getOrNull()
            if (job == null || !isAdded) { dismiss(); return@launch }
            val soyWorker = ProfileCache.perfil?.uid == job.workerUid
            view.findViewById<TextView>(R.id.tvRateTarget).text =
                if (soyWorker) getString(R.string.k_rate_target_worker, job.publicationTitle.take(50))
                else getString(R.string.k_rate_target_dueno, job.publicationTitle.take(50))
            view.findViewById<View>(R.id.btnRateSend).setOnClickListener {
                enviar(jobId)
            }
        }
    }

    private fun fijarEstrellas(n: Int) {
        stars = n
        starButtons.forEachIndexed { i, btn ->
            btn.setImageResource(if (i < n) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        }
        view?.findViewById<TextView>(R.id.tvRateLabel)?.text = when (n) {
            1 -> getString(R.string.k_rate_malo)
            2 -> getString(R.string.k_rate_regular)
            3 -> getString(R.string.k_rate_bueno)
            4 -> getString(R.string.k_rate_muy_bueno)
            else -> getString(R.string.k_rate_excelente)
        }
    }

    private fun enviar(jobId: String) {
        if (sending) return
        if (stars !in RatingLimits.MIN..RatingLimits.MAX) {
            view?.findViewById<TextView>(R.id.tvRateError)?.apply {
                visibility = View.VISIBLE
                text = getString(R.string.k_rate_elegir)
            }
            return
        }
        val perfil = ProfileCache.perfil
        if (perfil == null) {
            Toast.makeText(requireContext(), getString(R.string.k_rate_sesion), Toast.LENGTH_SHORT).show()
            return
        }
        sending = true
        val comentario = view?.findViewById<TextInputEditText>(R.id.etRateComment)?.text?.toString().orEmpty()
        viewLifecycleOwner.lifecycleScope.launch {
            val job = jobRepo.getById(jobId).getOrNull()
            if (job == null) { sending = false; dismiss(); return@launch }
            val r = ratingRepo.rate(requireContext(), perfil, job, stars, comentario)
            sending = false
            if (!isAdded) return@launch
            if (r.isSuccess) {
                Toast.makeText(requireContext(), getString(R.string.k_rate_gracias), Toast.LENGTH_SHORT).show()
                parentFragmentManager.setFragmentResult(REQUEST_RATED, bundleOf(EXTRA_JOB to jobId))
                dismiss()
            } else {
                view?.findViewById<TextView>(R.id.tvRateError)?.apply {
                    visibility = View.VISIBLE
                    text = r.exceptionOrNull()?.message ?: getString(R.string.k_rate_no_calificar)
                }
            }
        }
    }

    companion object {
        const val REQUEST_RATED = "rating_changed"
        const val EXTRA_JOB = "jobId"
        private const val ARG_ID = "jobId"
        fun newInstance(jobId: String) = RateSheet().apply {
            arguments = bundleOf(ARG_ID to jobId)
        }
    }
}
