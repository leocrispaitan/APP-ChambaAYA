package com.proyecto.chambaya.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.PublicProfile
import com.proyecto.chambaya.data.model.publicationTimeAgo

/** Trabajo con contraparte resuelta y si ya lo califiqué. */
data class JobHistoryRow(
    val job: Job,
    val other: PublicProfile? = null,
    val ratedByMe: Boolean = false
)

/**
 * FASE 19 — Historial de trabajos.
 */
class JobHistoryAdapter(
    private val onRate: (JobHistoryRow) -> Unit = {}
) : ListAdapter<JobHistoryRow, JobHistoryAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_job_history, parent, false)
        return VH(v, onRate)
    }

    override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))

    class VH(view: View, private val onRate: (JobHistoryRow) -> Unit) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ShapeableImageView = view.findViewById(R.id.ivJobAvatar)
        private val tvTitle: TextView = view.findViewById(R.id.tvJobTitle)
        private val tvOther: TextView = view.findViewById(R.id.tvJobOther)
        private val tvStatus: TextView = view.findViewById(R.id.tvJobStatus)
        private val tvMeta: TextView = view.findViewById(R.id.tvJobMeta)
        private val btnRate: MaterialButton = view.findViewById(R.id.btnJobRate)

        fun bind(item: JobHistoryRow) {
            val job = item.job
            tvTitle.text = job.publicationTitle.ifBlank { itemView.context.getString(R.string.k_hist_trabajo) }
            val nombre = item.other?.displayName() ?: itemView.context.getString(R.string.k_rat_usuario)
            tvOther.text = itemView.context.getString(R.string.k_hist_con_fmt, nombre)
            tvStatus.text = JobStatus.label(itemView.context, job.status)
            val monto = job.agreedPayment.amount.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.2f", it)
            }
            tvMeta.text = itemView.context.getString(R.string.k_hist_monto_fmt, monto, publicationTimeAgo(job.createdAt))
            val foto = item.other?.photoUrl.orEmpty()
            if (foto.isNotBlank()) {
                ivAvatar.load(foto) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_circle)
                    error(R.drawable.ic_user_circle)
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_circle)
            }
            val puede = job.status == JobStatus.COMPLETED && !item.ratedByMe
            btnRate.visibility = if (puede) View.VISIBLE else View.GONE
            if (puede) btnRate.setOnClickListener { onRate(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<JobHistoryRow>() {
        override fun areItemsTheSame(a: JobHistoryRow, b: JobHistoryRow): Boolean =
            a.job.jobId == b.job.jobId
        override fun areContentsTheSame(a: JobHistoryRow, b: JobHistoryRow): Boolean = a == b
    }
}
