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
        private val btnMoreOptions: ImageButton = itemView.findViewById(R.id.btnMoreOptions)

        // Descripción del trabajo
        private val tvJobDescription: TextView = itemView.findViewById(R.id.tvJobDescription)
        private val btnVerMas: TextView = itemView.findViewById(R.id.btnVerMas)

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
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tvLikesCount)
        private val tvCommentsCount: TextView = itemView.findViewById(R.id.tvCommentsCount)
        private val tvSharesCount: TextView = itemView.findViewById(R.id.tvSharesCount)

        // Pie de tarjeta
        private val tvJobRating: TextView = itemView.findViewById(R.id.tvJobRating)

        fun bind(jobCard: JobCard) {
            // ── Cabecera ──────────────────────────────────
            tvProfileName.text = jobCard.empleador.ifEmpty { "Empleador ChambAYA" }
            tvLocation.text = jobCard.distrito.ifEmpty { "Ayacucho" }
            tvTimeAgo.text = jobCard.tiempoPublicado.ifEmpty { "Hace 2h" }

            // ── Descripción del trabajo + "Ver más" ───────
            val fullDescription = jobCard.descripcion.ifEmpty {
                "${jobCard.empleador.ifEmpty { "Se busca" }} para ${jobCard.titulo}"
            }
            tvJobDescription.text = fullDescription
            tvJobDescription.maxLines = 2
            var isExpanded = false
            btnVerMas.text = "Ver más"

            // Mostrar "Ver más" si la descripción es extensa (> 75 caracteres o > 2 líneas)
            tvJobDescription.post {
                if (tvJobDescription.lineCount > 2 || fullDescription.length > 75) {
                    btnVerMas.visibility = View.VISIBLE
                } else {
                    btnVerMas.visibility = View.GONE
                }
            }

            val toggleExpand = View.OnClickListener {
                isExpanded = !isExpanded
                if (isExpanded) {
                    tvJobDescription.maxLines = Int.MAX_VALUE
                    btnVerMas.text = "Ver menos"
                } else {
                    tvJobDescription.maxLines = 2
                    btnVerMas.text = "Ver más"
                }
            }
            btnVerMas.setOnClickListener(toggleExpand)

            // ── Imagen / Info Card ─────────────────────────
            ivJobImage.visibility = View.GONE
            layoutJobInfoCard.visibility = View.VISIBLE

            try {
                frameJobImage.setBackgroundColor(Color.parseColor(jobCard.colorFondo))
            } catch (e: IllegalArgumentException) {
                frameJobImage.setBackgroundColor(Color.parseColor("#1E3A5F"))
            }

            ivCategoryIcon.setImageResource(jobCard.iconoCategoria)
            tvJobTitle.text = jobCard.titulo
            tvJobCategory.text = jobCard.categoria
            tvJobPrice.text = jobCard.precio

            // ── Interacciones (Contadores al lado derecho) ──
            val iconoFavorito = if (jobCard.isFavorito) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart_outline
            }
            btnFavorite.setImageResource(iconoFavorito)

            // Contadores de Me gusta, Comentarios y Compartidos
            var likesSimulados = (jobCard.rating * 720).toInt() + if (jobCard.isFavorito) 1 else 0
            val comentariosSimulados = (jobCard.rating * 28).toInt()
            val compartidosSimulados = (jobCard.rating * 15).toInt()

            tvLikesCount.text = formatLikes(likesSimulados)
            tvCommentsCount.text = formatLikes(comentariosSimulados)
            tvSharesCount.text = formatLikes(compartidosSimulados)

            // Rating + tiempo (Sin "Ver más", dejando la estrella intacta)
            tvJobRating.text = String.format(
                "%.1f · %s",
                jobCard.rating,
                jobCard.tiempoPublicado.ifEmpty { "Hace 2h" }
            )

            // ── Clicks ────────────────────────────────────
            cardJob.setOnClickListener { onJobClick(jobCard) }
            btnPostular.setOnClickListener { onJobClick(jobCard) }

            btnMoreOptions.setOnClickListener {
                showOptionsBottomSheet(itemView.context, jobCard)
            }

            btnFavorite.setOnClickListener {
                jobCard.isFavorito = !jobCard.isFavorito
                val nuevoIcono = if (jobCard.isFavorito) R.drawable.ic_heart_filled
                                 else R.drawable.ic_heart_outline
                btnFavorite.setImageResource(nuevoIcono)
                
                if (jobCard.isFavorito) likesSimulados++ else likesSimulados--
                tvLikesCount.text = formatLikes(likesSimulados)
                
                onFavoriteClick(jobCard)
            }

            btnBookmark.setOnClickListener { onJobClick(jobCard) }
        }

        private fun showOptionsBottomSheet(context: android.content.Context, jobCard: JobCard) {
            val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_job_options, null)
            bottomSheetDialog.setContentView(dialogView)

            dialogView.findViewById<View>(R.id.optionWhy)?.setOnClickListener {
                android.widget.Toast.makeText(context, "Por qué ves esta publicación de ${jobCard.empleador}", android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            dialogView.findViewById<View>(R.id.optionRate)?.setOnClickListener {
                android.widget.Toast.makeText(context, "Calificando publicación: ${jobCard.titulo}", android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            dialogView.findViewById<View>(R.id.optionNotInterested)?.setOnClickListener {
                android.widget.Toast.makeText(context, "Marcar 'No me interesa'", android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            dialogView.findViewById<View>(R.id.optionReport)?.setOnClickListener {
                android.widget.Toast.makeText(context, "Denunciar publicación", android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        }

        private fun formatLikes(count: Int): String {
            return when {
                count >= 1000 -> String.format("%.1fk", count / 1000.0)
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
