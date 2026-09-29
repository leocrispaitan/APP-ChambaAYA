package com.proyecto.chambaya.ui.publish

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.ApplicationStatus
import com.proyecto.chambaya.data.model.Job
import com.proyecto.chambaya.data.model.JobApplication
import com.proyecto.chambaya.data.model.JobStatus
import com.proyecto.chambaya.data.model.publicationTimeAgo

/** Mi postulación con su trabajo y si ya califiqué. */
data class MyAppRow(
    val app: JobApplication,
    val job: Job? = null,
    val ratedByMe: Boolean = false
)

/**
 * FASE 7/8/9 — Mis postulaciones (vista trabajador).
 */
class MyApplicationsAdapter(
    private val onPrimary: (MyAppRow) -> Unit = {},
    private val onContact: (MyAppRow) -> Unit = {},
    private val onOpenDetail: (MyAppRow) -> Unit = {}
) : ListAdapter<MyAppRow, MyApplicationsAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_my_application, parent, false)
        return VH(v, onPrimary, onContact, onOpenDetail)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(
        view: View,
        private val onPrimary: (MyAppRow) -> Unit,
        private val onContact: (MyAppRow) -> Unit,
        private val onOpenDetail: (MyAppRow) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val tvStatus: TextView = view.findViewById(R.id.tvMyAppStatus)
        private val tvDate: TextView = view.findViewById(R.id.tvMyAppDate)
        private val tvTitle: TextView = view.findViewById(R.id.tvMyAppTitle)
        private val tvJob: TextView = view.findViewById(R.id.tvMyAppJob)
        private val btnPrimary: MaterialButton = view.findViewById(R.id.btnMyAppPrimary)
        private val btnSecondary: MaterialButton = view.findViewById(R.id.btnMyAppSecondary)

        fun bind(item: MyAppRow) {
            val app = item.app
            val job = item.job
            tvStatus.text = ApplicationStatus.label(app.status)
            tvDate.text = publicationTimeAgo(app.createdAt)
            tvTitle.text = app.publicationTitle.ifBlank { "Chamba" }

            if (job != null) {
                tvJob.visibility = View.VISIBLE
                val monto = job.agreedPayment.amount.let {
                    if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.2f", it)
                }
                tvJob.text = when (job.status) {
                    JobStatus.ACCEPTED -> "Seleccionado · S/ $monto por cobrar"
                    JobStatus.IN_PROGRESS -> "Trabajo en curso · S/ $monto"
                    JobStatus.COMPLETED -> if (item.ratedByMe) "Completado · Calificado ✓ · S/ $monto"
                    else "Completado · S/ $monto"
                    else -> JobStatus.label(job.status)
                }
            } else {
                tvJob.visibility = View.GONE
            }

            // Acción principal según estado.
            val primary: Pair<String, Boolean>? = when {
                app.status == ApplicationStatus.PENDING -> "Retirar" to true
                job?.status == JobStatus.COMPLETED && !item.ratedByMe -> "Calificar" to true
                else -> null
            }
            if (primary != null) {
                btnPrimary.visibility = View.VISIBLE
                btnPrimary.text = primary.first
                btnPrimary.setOnClickListener { onPrimary(item) }
            } else {
                btnPrimary.visibility = View.GONE
            }
            // Con trabajo activo, el secundario contacta al contratante.
            val chateable = job != null &&
                (job.status == JobStatus.ACCEPTED || job.status == JobStatus.IN_PROGRESS)
            btnSecondary.text = if (chateable) "Contactar" else "Ver chamba"
            btnSecondary.setOnClickListener {
                if (chateable) onContact(item) else onOpenDetail(item)
            }
            itemView.setOnClickListener { onOpenDetail(item) }
        }
    }

    class Diff : DiffUtil.ItemCallback<MyAppRow>() {
        override fun areItemsTheSame(a: MyAppRow, b: MyAppRow): Boolean =
            a.app.applicationId == b.app.applicationId
        override fun areContentsTheSame(a: MyAppRow, b: MyAppRow): Boolean = a == b
    }
}
