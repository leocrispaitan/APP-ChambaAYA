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
 * Adapter optimizado para el RecyclerView de Job Cards
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
     * ViewHolder optimizado con ViewBinding manual para mejor rendimiento
     */
    class JobCardViewHolder(
        itemView: View,
        private val onJobClick: (JobCard) -> Unit,
        private val onFavoriteClick: (JobCard) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardJob: MaterialCardView = itemView.findViewById(R.id.cardJob)
        private val frameJobImage: FrameLayout = itemView.findViewById(R.id.frameJobImage)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val btnFavorite: ImageButton = itemView.findViewById(R.id.btnFavorite)
        private val tvJobTitle: TextView = itemView.findViewById(R.id.tvJobTitle)
        private val tvJobRating: TextView = itemView.findViewById(R.id.tvJobRating)
        private val tvJobPrice: TextView = itemView.findViewById(R.id.tvJobPrice)

        fun bind(jobCard: JobCard) {
            // Configurar título
            tvJobTitle.text = jobCard.titulo

            // Configurar rating
            tvJobRating.text = String.format("%.1f", jobCard.rating)

            // Configurar precio
            tvJobPrice.text = jobCard.precio

            // Configurar icono de categoría
            ivCategoryIcon.setImageResource(jobCard.iconoCategoria)

            // Configurar color de fondo del frame
            try {
                frameJobImage.setBackgroundColor(Color.parseColor(jobCard.colorFondo))
            } catch (e: IllegalArgumentException) {
                // Color por defecto si el parsing falla
                frameJobImage.setBackgroundColor(Color.parseColor("#E3F2FD"))
            }

            // Configurar botón de favorito
            val iconoFavorito = if (jobCard.isFavorito) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart_outline
            }
            btnFavorite.setImageResource(iconoFavorito)

            // Click en la card completa
            cardJob.setOnClickListener {
                onJobClick(jobCard)
            }

            // Click en el botón de favorito
            btnFavorite.setOnClickListener {
                jobCard.isFavorito = !jobCard.isFavorito
                val nuevoIcono = if (jobCard.isFavorito) {
                    R.drawable.ic_heart_filled
                } else {
                    R.drawable.ic_heart_outline
                }
                btnFavorite.setImageResource(nuevoIcono)
                onFavoriteClick(jobCard)
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
