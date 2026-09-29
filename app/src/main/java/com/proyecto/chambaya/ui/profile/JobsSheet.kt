package com.proyecto.chambaya.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.UserRoles
import com.proyecto.chambaya.data.repository.JobRepository
import com.proyecto.chambaya.data.repository.ProfileRepository
import com.proyecto.chambaya.data.repository.RatingRepository
import com.proyecto.chambaya.ui.jobs.RateSheet
import kotlinx.coroutines.launch

/**
 * FASE 19 — Mis trabajos (trabajador) o trabajos contratados (contratante),
 * con su estado, contraparte y calificación cuando corresponde.
 */
class JobsSheet : BottomSheetDialogFragment() {

    private val jobRepo = JobRepository()
    private val profileRepo = ProfileRepository()
    private val ratingRepo = RatingRepository()
    private var adapter: JobHistoryAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottom_sheet_jobs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isBlank()) { dismiss(); return }
        val comoEmpleador = requireArguments().getBoolean(ARG_EMPLOYER)
        view.findViewById<TextView>(R.id.tvJobsTitle).text =
            if (comoEmpleador) "Trabajos contratados" else "Mis trabajos"

        val rv = view.findViewById<RecyclerView>(R.id.rvJobs)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = JobHistoryAdapter(
            onRate = { row ->
                RateSheet.newInstance(row.job.jobId).show(parentFragmentManager, "rate")
            }
        )
        rv.adapter = adapter
        val progress = view.findViewById<ProgressBar>(R.id.progressJobs)
        val empty = view.findViewById<TextView>(R.id.tvJobsEmpty)
        progress.visibility = View.VISIBLE

        parentFragmentManager.setFragmentResultListener(RateSheet.REQUEST_RATED, viewLifecycleOwner) { _, _ ->
            cargar(view, uid, comoEmpleador)
        }
        cargar(view, uid, comoEmpleador)
    }

    private fun cargar(view: View, uid: String, comoEmpleador: Boolean) {
        val progress = view.findViewById<ProgressBar>(R.id.progressJobs)
        val empty = view.findViewById<TextView>(R.id.tvJobsEmpty)
        viewLifecycleOwner.lifecycleScope.launch {
            val jobs = if (comoEmpleador) {
                jobRepo.listByEmployer(uid).getOrNull().orEmpty()
            } else {
                jobRepo.listByWorker(uid).getOrNull().orEmpty()
            }
            val others = jobs.map { if (comoEmpleador) it.workerUid else it.employerUid }.distinct()
            val perfiles = mutableMapOf<String, com.proyecto.chambaya.data.model.PublicProfile>()
            others.forEach { id ->
                profileRepo.loadPublicProfile(id).getOrNull()?.let { perfiles[id] = it }
            }
            val completedIds = jobs.filter { it.status == JobStatus.COMPLETED }.map { it.jobId }
            val rated = if (completedIds.isEmpty()) emptySet()
            else ratingRepo.ratedJobIds(completedIds, uid).getOrNull().orEmpty()
            if (!isAdded) return@launch
            progress.visibility = View.GONE
            if (jobs.isEmpty()) {
                empty.visibility = View.VISIBLE
            } else {
                empty.visibility = View.GONE
                adapter?.submitList(
                    jobs.map { job ->
                        val otherId = if (comoEmpleador) job.workerUid else job.employerUid
                        JobHistoryRow(job, perfiles[otherId], job.jobId in rated)
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_EMPLOYER = "employer"
        fun newInstance(comoEmpleador: Boolean) = JobsSheet().apply {
            arguments = bundleOf(ARG_EMPLOYER to comoEmpleador)
        }
    }
}
