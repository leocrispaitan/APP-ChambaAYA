package com.proyecto.chambaya.ui.jobs

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.proyecto.chambaya.R

/**
 * Adapter optimizado para el RecyclerView de Job Cards (Estilo Instagram)
 * Usa ListAdapter con DiffUtil para máximo rendimiento
 */
class JobCardAdapter(
    private val onJobClick: (JobCard) -> Unit = {},
    private val onFavoriteClick: (JobCard) -> Unit = {}
) : ListAdapter<JobCard, JobCardAdapter.JobCardViewHolder>(JobCardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_card, parent, false)
        return JobCardViewHolder(view, onJobClick, onFavoriteClick)
    }

    override fun onBindViewHolder(holder: JobCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder rediseñado al estilo publicación de Instagram
     */
    class JobCardViewHolder(
        itemView: View,
        private val onJobClick: (JobCard) -> Unit,
        private val onFavoriteClick: (JobCard) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        // Vistas de cabecera (header)
        private val cardJob: MaterialCardView = itemView.findViewById(R.id.cardJob)
        private val tvProfileName: TextView = itemView.findViewById(R.id.tvProfileName)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val btnPostular: TextView = itemView.findViewById(R.id.btnPostular)

        // Descripción del trabajo
        private val tvJobDescription: TextView = itemView.findViewById(R.id.tvJobDescription)

        // Área de imagen / info card
        private val frameJobImage: FrameLayout = itemView.findViewById(R.id.frameJobImage)
        private val ivJobImage: ImageView = itemView.findViewById(R.id.ivJobImage)
        private val layoutJobInfoCard: View = itemView.findViewById(R.id.layoutJobInfoCard)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val tvJobTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        private val tvJobCategory: TextView = itemView.findViewById(R.id.tvJobCategory)
        private val tvJobPrice: TextView = itemView.findViewById(R.id.tvJobPrice)

        // Barra de interacciones
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        private val btnComment: ImageButton = itemView.findViewById(R.id.btnComment)
        private val btnShare: ImageButton = itemView.findViewById(R.id.btnShare)
        private val btnBookmark: ImageButton = itemView.findViewById(R.id.btnBookmark)

        // Pie de tarjeta
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tvLikesCount)
        private val tvJobRating: TextView = itemView.findViewById(R.id.tvJobRating)

        fun bind(jobCard: JobCard) {
            // ── Cabecera ──────────────────────────────────
            // Nombre de perfil (usamos la categoría como nombre del publicador si no hay campo)
            tvProfileName.text = jobCard.empleador.ifEmpty { "Empleador ChambAYA" }
            tvLocation.text = jobCard.distrito.ifEmpty { "Ayacucho" }
            tvTimeAgo.text = jobCard.tiempoPublicado.ifEmpty { "Hace 2h" }

            // ── Descripción del trabajo ────────────────────
            tvJobDescription.text = jobCard.descripcion.ifEmpty {
                "${jobCard.empleador.ifEmpty { "Se busca" }} para ${jobCard.titulo}"
            }

            // ── Imagen / Info Card ─────────────────────────
            // Si no hay imagen, mostrar la tarjeta de info con gradiente
            ivJobImage.visibility = View.GONE
            layoutJobInfoCard.visibility = View.VISIBLE

            // Color de fondo del área de imagen
            try {
                frameJobImage.setBackgroundColor(Color.parseColor(jobCard.colorFondo))
            } catch (e: IllegalArgumentException) {
                frameJobImage.setBackgroundColor(Color.parseColor("#1E3A5F"))
            }

            // Ícono de categoría
            ivCategoryIcon.setImageResource(jobCard.iconoCategoria)

            // Título del puesto
            tvJobTitle.text = jobCard.titulo

            // Categoría como badge
            tvJobCategory.text = jobCard.categoria

            // Precio/sueldo destacado
            tvJobPrice.text = jobCard.precio

            // ── Interacciones ─────────────────────────────
            // Ícono de favorito / corazón
            val iconoFavorito = if (jobCard.isFavorito) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart_outline
            }
            btnFavorite.setImageResource(iconoFavorito)

            // Contador de likes (simulado)
            val likesSimulados = (jobCard.rating * 720).toInt()
            tvLikesCount.text = "${formatLikes(likesSimulados)} Me gusta"

            // Rating + tiempo
            tvJobRating.text = String.format("%.1f · %s · Ver más",
                jobCard.rating,
                jobCard.tiempoPublicado.ifEmpty { "Hace 2h" })

            // ── Clicks ────────────────────────────────────
            cardJob.setOnClickListener { onJobClick(jobCard) }

            btnPostular.setOnClickListener { onJobClick(jobCard) }

            btnFavorite.setOnClickListener {
                jobCard.isFavorito = !jobCard.isFavorito
                val nuevoIcono = if (jobCard.isFavorito) R.drawable.ic_heart_filled
                                 else R.drawable.ic_heart_outline
                btnFavorite.setImageResource(nuevoIcono)
                onFavoriteClick(jobCard)
            }

            btnBookmark.setOnClickListener { onJobClick(jobCard) }
        }

        private fun formatLikes(count: Int): String {
            return when {
                count >= 1000 -> String.format("%.1f K", count / 1000.0)
                else -> count.toString()
            }
        }
    }

    /**
     * DiffUtil callback para optimizar las actualizaciones del RecyclerView
     */
    class JobCardDiffCallback : DiffUtil.ItemCallback<JobCard>() {
        override fun areItemsTheSame(oldItem: JobCard, newItem: JobCard): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JobCard, newItem: JobCard): Boolean {
            return oldItem == newItem
        }
    }
}
