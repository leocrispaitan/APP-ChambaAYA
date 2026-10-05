package com.proyecto.chambaya.ui.trabajos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobStatus
import java.util.Date

/** Trabajo con entidad y calificación para la línea de tiempo. */
data class TimelineRow(
    val job: Job,
    val entityName: String = "",
    val ratedByMe: Boolean = false
)

fun Timestamp?.fechaCorta(): String {
    if (this == null) return "—"
    return android.text.format.DateFormat.format("dd/MM/yyyy", Date(seconds * 1000)).toString()
}

/**
 * Historial del trabajador como línea de tiempo vertical: contratado,
 * inicio, fin, pago y estado de cada trabajo.
 */
class TimelineAdapter(
    private val onRate: (TimelineRow) -> Unit = {}
) : ListAdapter<TimelineRow, TimelineAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline_job, parent, false)
        return VH(v, onRate)
    }

    override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))

    class VH(view: View, private val onRate: (TimelineRow) -> Unit) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTimelineTitle)
        private val tvStatus: TextView = view.findViewById(R.id.tvTimelineStatus)
        private val tvEntity: TextView = view.findViewById(R.id.tvTimelineEntity)
        private val tvHired: TextView = view.findViewById(R.id.tvTimelineHired)
        private val tvStart: TextView = view.findViewById(R.id.tvTimelineStart)
        private val tvEnd: TextView = view.findViewById(R.id.tvTimelineEnd)
        private val tvPay: TextView = view.findViewById(R.id.tvTimelinePay)
        private val btnRate: MaterialButton = view.findViewById(R.id.btnTimelineRate)

        fun bind(item: TimelineRow) {
            val job = item.job
            tvTitle.text = job.publicationTitle.ifBlank { itemView.context.getString(R.string.k_hist_trabajo) }
            tvEntity.text = item.entityName.ifBlank { itemView.context.getString(R.string.k_time_contratante) }
            tvStatus.text = JobStatus.label(itemView.context, job.status)
            tvHired.text = job.createdAt.fechaCorta()
            tvStart.text = if (job.startedAt != null) job.startedAt.fechaCorta() else itemView.context.getString(R.string.k_time_por_iniciar)
            tvEnd.text = when {
                job.completedAt != null -> job.completedAt.fechaCorta()
                job.status == JobStatus.CANCELLED -> itemView.context.getString(R.string.k_est_job_cancel)
                else -> itemView.context.getString(R.string.k_est_job_curso)
            }
            val monto = job.agreedPayment.amount.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.2f", it)
            }
            tvPay.text = itemView.context.getString(R.string.k_time_monto_fmt, monto)
            val puede = job.status == JobStatus.COMPLETED && !item.ratedByMe
            btnRate.visibility = if (puede) View.VISIBLE else View.GONE
            if (puede) btnRate.setOnClickListener { onRate(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<TimelineRow>() {
        override fun areItemsTheSame(a: TimelineRow, b: TimelineRow): Boolean =
            a.job.jobId == b.job.jobId
        override fun areContentsTheSame(a: TimelineRow, b: TimelineRow): Boolean = a == b
    }
}
