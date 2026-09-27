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
        private val ivProfilePhoto: com.google.android.material.imageview.ShapeableImageView = itemView.findViewById(R.id.ivProfilePhoto)
        private val tvProfileName: TextView = itemView.findViewById(R.id.tvProfileName)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
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

        // Botón de favorito superpuesto (nuevo diseño)
        private val btnFavoriteOverlay: ImageButton = itemView.findViewById(R.id.btnFavoriteOverlay)
        
        // Barra de interacciones (simplificada)
        private val btnComment: ImageButton = itemView.findViewById(R.id.btnComment)
        private val tvCommentsCount: TextView = itemView.findViewById(R.id.tvCommentsCount)
        private val btnVerTrabajo: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnVerTrabajo)

        // Pie de tarjeta
        private val tvJobRating: TextView = itemView.findViewById(R.id.tvJobRating)

        fun bind(jobCard: JobCard) {
            // ── Cabecera ──────────────────────────────────
            // Foto de perfil del empleador
            val avatarResource = if (jobCard.avatarEmpleador != 0) {
                jobCard.avatarEmpleador
            } else {
                R.drawable.ic_user_circle  // Placeholder por defecto
            }
            ivProfilePhoto.setImageResource(avatarResource)
            
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

            // ── Interacciones (Solo comentarios) ──
            // Actualizar botón de favorito superpuesto
            val iconoFavorito = if (jobCard.isFavorito) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart_outline
            }
            btnFavoriteOverlay.setImageResource(iconoFavorito)

            // Contadores de Comentarios
            var likesSimulados = (jobCard.rating * 720).toInt() + if (jobCard.isFavorito) 1 else 0
            val comentariosSimulados = (jobCard.rating * 28).toInt()

            tvCommentsCount.text = formatLikes(comentariosSimulados)

            // Rating + tiempo (Sin "Ver más", dejando la estrella intacta)
            tvJobRating.text = String.format(
                "%.1f · %s",
                jobCard.rating,
                jobCard.tiempoPublicado.ifEmpty { "Hace 2h" }
            )

            // ── Clicks ────────────────────────────────────
            cardJob.setOnClickListener { onJobClick(jobCard) }
            btnVerTrabajo.setOnClickListener { onJobClick(jobCard) }

            btnMoreOptions.setOnClickListener {
                showOptionsBottomSheet(itemView.context, jobCard)
            }

            btnFavoriteOverlay.setOnClickListener {
                jobCard.isFavorito = !jobCard.isFavorito
                val nuevoIcono = if (jobCard.isFavorito) R.drawable.ic_heart_filled
                                 else R.drawable.ic_heart_outline
                btnFavoriteOverlay.setImageResource(nuevoIcono)
                
                onFavoriteClick(jobCard)
            }
        }

        private fun showOptionsBottomSheet(context: android.content.Context, jobCard: JobCard) {
            val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_job_options, null)
            bottomSheetDialog.setContentView(dialogView)

            // 1. Guardar publicación (NUEVA)
            dialogView.findViewById<View>(R.id.optionSave)?.setOnClickListener {
                // Cambiar el estado de guardado
                jobCard.isFavorito = !jobCard.isFavorito
                val mensaje = if (jobCard.isFavorito) "Publicación guardada" else "Guardado eliminado"
                android.widget.Toast.makeText(context, mensaje, android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            // 2. Compartir publicación
            dialogView.findViewById<View>(R.id.optionShare)?.setOnClickListener {
                shareJobCard(context, jobCard)
                bottomSheetDialog.dismiss()
            }

            // 3. Por qué ves esto
            dialogView.findViewById<View>(R.id.optionWhy)?.setOnClickListener {
                android.widget.Toast.makeText(context, "Por qué ves esta publicación de ${jobCard.empleador}", android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            // 4. Calificar publicación
            dialogView.findViewById<View>(R.id.optionRate)?.setOnClickListener {
                android.widget.Toast.makeText(context, "Calificando publicación: ${jobCard.titulo}", android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            // 5. No me interesa
            dialogView.findViewById<View>(R.id.optionNotInterested)?.setOnClickListener {
                android.widget.Toast.makeText(context, "Marcar 'No me interesa'", android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            // 6. Denunciar publicación
            dialogView.findViewById<View>(R.id.optionReport)?.setOnClickListener {
                android.widget.Toast.makeText(context, "Denunciar publicación", android.widget.Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        }

        /**
         * Función para compartir la publicación usando Android Share Sheet
         */
        private fun shareJobCard(context: android.content.Context, jobCard: JobCard) {
            val shareText = buildString {
                append("📢 ${jobCard.titulo}\n\n")
                append("💼 ${jobCard.categoria}\n")
                append("💰 ${jobCard.precio}\n")
                append("📍 ${jobCard.distrito}\n\n")
                if (jobCard.descripcion.isNotEmpty()) {
                    append("${jobCard.descripcion}\n\n")
                }
                append("🔗 Compartido desde ChambAYA")
            }

            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Trabajo: ${jobCard.titulo}")
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }

            val chooserIntent = android.content.Intent.createChooser(shareIntent, "Compartir publicación")
            context.startActivity(chooserIntent)
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
