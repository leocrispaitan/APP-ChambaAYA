package com.proyecto.chambaya.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.PublicProfile
import com.proyecto.chambaya.data.model.Rating
import com.proyecto.chambaya.data.model.publicationTimeAgo

/** Calificación con su autor resuelto. */
data class RatingRow(
    val rating: Rating,
    val author: PublicProfile? = null
)

/**
 * FASE 19 — Calificaciones recibidas (tab Reseñas de Mi Perfil).
 */
class RatingsAdapter : ListAdapter<RatingRow, RatingsAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_rating, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivAvatar: ShapeableImageView = view.findViewById(R.id.ivRatingAvatar)
        private val tvAuthor: TextView = view.findViewById(R.id.tvRatingAuthor)
        private val tvStars: TextView = view.findViewById(R.id.tvRatingStars)
        private val tvDate: TextView = view.findViewById(R.id.tvRatingDate)
        private val tvComment: TextView = view.findViewById(R.id.tvRatingComment)
        private val tvJob: TextView = view.findViewById(R.id.tvRatingJob)

        fun bind(item: RatingRow) {
            val r = item.rating
            tvAuthor.text = item.author?.displayName() ?: "Usuario ChambAYA"
            val llenas = r.rating.coerceIn(0, 5)
            tvStars.text = "★".repeat(llenas) + "☆".repeat(5 - llenas) + " ${r.rating}.0"
            tvDate.text = publicationTimeAgo(r.createdAt)
            if (r.comment.isNotBlank()) {
                tvComment.visibility = View.VISIBLE
                tvComment.text = r.comment
            } else {
                tvComment.visibility = View.GONE
            }
            if (r.publicationTitle.isNotBlank()) {
                tvJob.visibility = View.VISIBLE
                tvJob.text = "En: ${r.publicationTitle}"
            } else {
                tvJob.visibility = View.GONE
            }
            val foto = item.author?.photoUrl.orEmpty()
            if (foto.isNotBlank()) {
                ivAvatar.load(foto) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_circle)
                    error(R.drawable.ic_user_circle)
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_circle)
            }
        }
    }

    class Diff : DiffUtil.ItemCallback<RatingRow>() {
        override fun areItemsTheSame(a: RatingRow, b: RatingRow): Boolean =
            a.rating.ratingId == b.rating.ratingId
        override fun areContentsTheSame(a: RatingRow, b: RatingRow): Boolean = a == b
    }
}
