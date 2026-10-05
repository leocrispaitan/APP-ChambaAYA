package com.proyecto.chambaya.ui.publish

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.Publication
import com.proyecto.chambaya.data.model.PublicationStatus
import com.proyecto.chambaya.data.model.precioTexto
import com.proyecto.chambaya.data.model.publicationTimeAgo
import com.proyecto.chambaya.ui.jobs.formatCount

/**
 * FASE 5 — Lista de "Mis publicaciones" con estado y acciones del dueño.
 */
class MisPublicacionesAdapter(
    private val onVer: (Publication) -> Unit = {},
    private val onToggle: (Publication) -> Unit = {},
    private val onMenu: (Publication, View) -> Unit = { _, _ -> }
) : ListAdapter<Publication, MisPublicacionesAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_my_publication, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvStatus: TextView = view.findViewById(R.id.tvMyStatus)
        private val tvDate: TextView = view.findViewById(R.id.tvMyDate)
        private val tvTitle: TextView = view.findViewById(R.id.tvMyTitle)
        private val tvMeta: TextView = view.findViewById(R.id.tvMyMeta)
        private val tvStats: TextView = view.findViewById(R.id.tvMyStats)
        private val btnToggle: MaterialButton = view.findViewById(R.id.btnMyToggle)
        private val btnView: MaterialButton = view.findViewById(R.id.btnMyView)
        private val btnMenu: ImageButton = view.findViewById(R.id.btnMyMenu)

        fun bind(p: Publication) {
            tvStatus.text = when (p.status) {
                PublicationStatus.ACTIVE -> itemView.context.getString(R.string.k_mispub_activa)
                PublicationStatus.PAUSED -> itemView.context.getString(R.string.k_mispub_pausada)
                PublicationStatus.FINISHED -> itemView.context.getString(R.string.k_mispub_fin)
                else -> itemView.context.getString(R.string.k_mispub_archivada)
            }
            tvDate.text = publicationTimeAgo(p.createdAt)
            tvTitle.text = p.title
            val vacantesDisponibles = (p.workersNeeded - p.workersHired).coerceAtLeast(0)
            tvMeta.text = itemView.context.getString(R.string.k_mispub_meta_fmt, p.precioTexto(itemView.context), p.location.district.ifBlank { "Ayacucho" }, vacantesDisponibles)
            tvStats.text = itemView.context.getString(R.string.k_mispub_stats_fmt, formatCount(p.statistics.views), formatCount(p.statistics.likes), formatCount(p.statistics.applications))

            btnToggle.text = when (p.status) {
                PublicationStatus.ACTIVE -> itemView.context.getString(R.string.item_pausar)
                PublicationStatus.PAUSED -> itemView.context.getString(R.string.k_detalle_reactivar)
                PublicationStatus.FINISHED -> itemView.context.getString(R.string.k_pub_republicar)
                else -> itemView.context.getString(R.string.k_detalle_reactivar)
            }
            btnToggle.visibility =
                if (p.status == PublicationStatus.ARCHIVED) View.GONE else View.VISIBLE

            btnView.setOnClickListener { onVer(p) }
            itemView.setOnClickListener { onVer(p) }
            btnToggle.setOnClickListener { onToggle(p) }
            btnMenu.setOnClickListener { onMenu(p, it) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Publication>() {
        override fun areItemsTheSame(a: Publication, b: Publication): Boolean =
            a.publicationId == b.publicationId
        override fun areContentsTheSame(a: Publication, b: Publication): Boolean = a == b
    }
}
