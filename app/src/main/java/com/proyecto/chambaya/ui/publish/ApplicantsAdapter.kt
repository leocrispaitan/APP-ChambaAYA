package com.proyecto.chambaya.ui.publish

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.ApplicationStatus
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobApplication
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.publicationTimeAgo

/** Fila de postulante con su trabajo y estado de calificación. */
data class ApplicantRow(
    val app: JobApplication,
    val job: Job? = null,
    val ratedByMe: Boolean = false
)

sealed interface RequestItem {
    data class Header(val title: String, val pending: Int, val total: Int) : RequestItem
    data class Row(val row: ApplicantRow) : RequestItem
}

/**
 * FASE 7/8/9 — Solicitudes recibidas, agrupadas por chamba.
 */
class ApplicantsAdapter(
    private val onAccept: (ApplicantRow) -> Unit = {},
    private val onReject: (ApplicantRow) -> Unit = {},
    private val onJobAction: (ApplicantRow) -> Unit = {},
    private val onRate: (ApplicantRow) -> Unit = {},
    private val onOpenDetail: (ApplicantRow) -> Unit = {},
    private val onOpenProfile: (ApplicantRow) -> Unit = {}
) : ListAdapter<RequestItem, RecyclerView.ViewHolder>(Diff()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is RequestItem.Header -> TYPE_HEADER
        is RequestItem.Row -> TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
            HeaderVH(v)
        } else {
            ApplicantVH(inflater.inflate(R.layout.item_applicant, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is RequestItem.Header -> (holder as HeaderVH).bind(item)
            is RequestItem.Row -> (holder as ApplicantVH).bind(item.row)
        }
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(android.R.id.text1)
        fun bind(h: RequestItem.Header) {
            tv.text = "${h.title} · ${h.pending} pendientes de ${h.total}"
            tv.setTextColor(itemView.context.getColor(R.color.text_secondary))
            tv.textSize = 13f
            tv.setPadding(32, 16, 32, 4)
        }
    }

    inner class ApplicantVH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ShapeableImageView = view.findViewById(R.id.ivApplicantAvatar)
        private val tvName: TextView = view.findViewById(R.id.tvApplicantName)
        private val tvMeta: TextView = view.findViewById(R.id.tvApplicantMeta)
        private val tvStatus: TextView = view.findViewById(R.id.tvApplicantStatus)
        private val tvMessage: TextView = view.findViewById(R.id.tvApplicantMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvApplicantTime)
        private val layoutActions: View = view.findViewById(R.id.layoutApplicantActions)
        private val btnAccept: MaterialButton = view.findViewById(R.id.btnApplicantAccept)
        private val btnReject: MaterialButton = view.findViewById(R.id.btnApplicantReject)
        private val layoutJob: View = view.findViewById(R.id.layoutApplicantJob)
        private val tvJobStatus: TextView = view.findViewById(R.id.tvApplicantJobStatus)
        private val btnJobAction: MaterialButton = view.findViewById(R.id.btnApplicantJobAction)
        private val btnRate: MaterialButton = view.findViewById(R.id.btnApplicantRate)

        fun bind(item: ApplicantRow) {
            val app = item.app
            val w = app.worker
            tvName.text = w.name.ifBlank { "@${w.username}".ifBlank { "Trabajador" } }
            val ratingTxt = if (w.ratingCount > 0) "★ ${String.format("%.1f", w.ratingAverage)}" else "Sin calificar"
            val expTxt = if (w.experienceYears > 0) "${w.experienceYears} años exp." else "Empezando"
            tvMeta.text = "@${w.username.ifBlank { "chambaya" }} · $ratingTxt · $expTxt"
            tvStatus.text = ApplicationStatus.label(app.status)
            if (app.message.isNotBlank()) {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = "“${app.message}”"
            } else {
                tvMessage.visibility = View.GONE
            }
            tvTime.text = publicationTimeAgo(app.createdAt)
            if (w.photoUrl.isNotBlank()) {
                ivAvatar.load(w.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_circle)
                    error(R.drawable.ic_user_circle)
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_circle)
            }
            ivAvatar.setOnClickListener { onOpenProfile(item) }

            val pending = app.status == ApplicationStatus.PENDING
            layoutActions.visibility = if (pending) View.VISIBLE else View.GONE
            if (pending) {
                btnAccept.setOnClickListener { onAccept(item) }
                btnReject.setOnClickListener { onReject(item) }
            }

            val job = item.job
            if (app.status == ApplicationStatus.ACCEPTED && job != null) {
                layoutJob.visibility = View.VISIBLE
                tvJobStatus.text = when (job.status) {
                    JobStatus.ACCEPTED -> "Seleccionado · pendiente de inicio"
                    JobStatus.IN_PROGRESS -> "Trabajo en curso · S/ ${monto(job)}"
                    JobStatus.COMPLETED -> "Trabajo completado · S/ ${monto(job)}"
                    else -> JobStatus.label(job.status)
                }
                val next = JobStatus.nextFrom(job.status).firstOrNull { it != JobStatus.CANCELLED }
                if (next != null) {
                    btnJobAction.visibility = View.VISIBLE
                    btnJobAction.text = if (next == JobStatus.IN_PROGRESS) "Iniciar" else "Completar"
                    btnJobAction.setOnClickListener { onJobAction(item) }
                } else {
                    btnJobAction.visibility = View.GONE
                }
                val puedeCalificar = job.status == JobStatus.COMPLETED && !item.ratedByMe
                btnRate.visibility = if (puedeCalificar) View.VISIBLE else View.GONE
                if (puedeCalificar) btnRate.setOnClickListener { onRate(item) }
            } else {
                layoutJob.visibility = View.GONE
            }
            itemView.setOnClickListener { onOpenDetail(item) }
        }

        private fun monto(job: Job): String {
            val a = job.agreedPayment.amount
            return if (a % 1.0 == 0.0) a.toInt().toString() else String.format("%.2f", a)
        }
    }

    class Diff : DiffUtil.ItemCallback<RequestItem>() {
        override fun areItemsTheSame(a: RequestItem, b: RequestItem): Boolean = when {
            a is RequestItem.Header && b is RequestItem.Header -> a.title == b.title
            a is RequestItem.Row && b is RequestItem.Row -> a.row.app.applicationId == b.row.app.applicationId
            else -> false
        }
        override fun areContentsTheSame(a: RequestItem, b: RequestItem): Boolean = a == b
    }
}
