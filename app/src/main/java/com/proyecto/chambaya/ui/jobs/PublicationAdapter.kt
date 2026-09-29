package com.proyecto.chambaya.ui.jobs

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
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.precioTexto
import com.proyecto.chambaya.data.model.publicationTimeAgo

/**
 * FASE 6 — Adapter del feed de publicaciones reales.
 *
 * Reutiliza item_job_card.xml (no se reemplaza el layout del feed) y lo vuelve
 * realmente interactivo:
 *  - tap en la tarjeta o "Ver chamba" → detalle completo
 *  - tap en avatar/nombre → perfil público del contratante
 *  - corazón → me gusta (Firestore publication_likes)
 *  - bookmark superpuesto → guardar (publication_saves)
 *  - ⋮ → Guardar / Compartir / No me interesa / Denunciar
 *  - badge de verificación solo cuando publisher.verified == true
 */
class PublicationAdapter(
    private val onOpenDetail: (PublicationFeedItem) -> Unit = {},
    private val onOpenProfile: (PublicationFeedItem) -> Unit = {},
    private val onToggleLike: (PublicationFeedItem) -> Unit = {},
    private val onToggleSave: (PublicationFeedItem) -> Unit = {},
    private val onShare: (PublicationFeedItem) -> Unit = {},
    private val onHide: (PublicationFeedItem) -> Unit = {},
    private val onReport: (PublicationFeedItem) -> Unit = {}
) : ListAdapter<PublicationFeedItem, PublicationAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_job_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val card: MaterialCardView = view.findViewById(R.id.cardJob)
        private val ivAvatar: ShapeableImageView = view.findViewById(R.id.ivProfilePhoto)
        private val tvName: TextView = view.findViewById(R.id.tvProfileName)
        private val ivVerified: ImageView = view.findViewById(R.id.ivVerified)
        private val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        private val tvTime: TextView = view.findViewById(R.id.tvTimeAgo)
        private val btnMore: ImageButton = view.findViewById(R.id.btnMoreOptions)
        private val tvDesc: TextView = view.findViewById(R.id.tvJobDescription)
        private val btnVerMas: TextView = view.findViewById(R.id.btnVerMas)
        private val frameImage: FrameLayout = view.findViewById(R.id.frameJobImage)
        private val ivPhoto: ImageView = view.findViewById(R.id.ivJobImage)
        private val infoCard: View = view.findViewById(R.id.layoutJobInfoCard)
        private val ivCatIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvJobTitle)
        private val tvCategory: TextView = view.findViewById(R.id.tvJobCategory)
        private val tvPrice: TextView = view.findViewById(R.id.tvJobPrice)
        private val btnFav: ImageButton = view.findViewById(R.id.btnFavoriteOverlay)
        private val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        private val tvLikes: TextView = view.findViewById(R.id.tvLikesCount)
        private val btnComment: ImageButton = view.findViewById(R.id.btnComment)
        private val tvComments: TextView = view.findViewById(R.id.tvCommentsCount)
        private val btnVer: MaterialButton = view.findViewById(R.id.btnVerTrabajo)
        private val tvRating: TextView = view.findViewById(R.id.tvJobRating)

        fun bind(item: PublicationFeedItem) {
            val p = item.publication
            val ctx = itemView.context

            // ── Cabecera ──
            val displayName = p.publisher.name.ifBlank { "Contratante ChambAYA" }
            tvName.text = displayName
            ivVerified.visibility = if (p.publisher.verified) View.VISIBLE else View.GONE
            val distrito = p.location.district.ifBlank { "Ayacucho" }
            tvLocation.text = distrito
            tvTime.text = publicationTimeAgo(p.createdAt)

            if (p.publisher.photoUrl.isNotBlank()) {
                ivAvatar.load(p.publisher.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_circle)
                    error(R.drawable.ic_user_circle)
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_circle)
            }

            // Perfil clicable (avatar + nombre).
            val openProfile = View.OnClickListener { onOpenProfile(item) }
            ivAvatar.setOnClickListener(openProfile)
            tvName.setOnClickListener(openProfile)

            // ── Descripción expandible ──
            tvDesc.text = p.description.ifBlank { p.title }
            tvDesc.maxLines = 2
            var expanded = false
            btnVerMas.text = "Ver más"
            tvDesc.post {
                btnVerMas.visibility =
                    if (tvDesc.lineCount > 2 || tvDesc.text.length > 90) View.VISIBLE else View.GONE
            }
            btnVerMas.setOnClickListener {
                expanded = !expanded
                tvDesc.maxLines = if (expanded) Int.MAX_VALUE else 2
                btnVerMas.text = if (expanded) "Ver menos" else "Ver más"
            }

            // ── Foto o tarjeta de info ──
            val firstImage = p.images.firstOrNull()?.url.orEmpty()
            if (firstImage.isNotBlank()) {
                ivPhoto.visibility = View.VISIBLE
                infoCard.visibility = View.GONE
                frameImage.setBackgroundColor(0xFF1E293B.toInt())
                ivPhoto.load(firstImage) {
                    crossfade(true)
                    placeholder(R.drawable.bg_job_image_placeholder)
                    error(R.drawable.bg_job_image_placeholder)
                }
            } else {
                ivPhoto.visibility = View.GONE
                infoCard.visibility = View.VISIBLE
                frameImage.setBackgroundColor(0xFF1E293B.toInt())
                ivCatIcon.setImageResource(R.drawable.ic_cat_construccion)
                tvTitle.text = p.title
                tvCategory.text = p.category.ifBlank { "Chamba" }
                tvPrice.text = p.precioTexto()
            }

            // ── Guardar (bookmark superpuesto) ──
            btnFav.setImageResource(
                if (item.saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
            )
            btnFav.contentDescription = if (item.saved) "Guardado" else "Guardar"
            btnFav.setOnClickListener { onToggleSave(item) }

            // ── Barra inferior: Me gusta + comentarios + ver ──
            btnLike.setImageResource(
                if (item.liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
            tvLikes.text = formatCount(item.likesCount)
            btnLike.setOnClickListener { onToggleLike(item) }
            tvComments.text = formatCount(p.statistics.comments)
            btnComment.setOnClickListener { onOpenDetail(item) }
            tvRating.text = "★ ${formatCount(item.savesCount)} guardados · ${publicationTimeAgo(p.createdAt)}"

            // ── Apertura del detalle ──
            card.setOnClickListener { onOpenDetail(item) }
            btnVer.text = "Ver chamba"
            btnVer.setOnClickListener { onOpenDetail(item) }

            btnMore.setOnClickListener {
                PublicationOptionsSheet.newInstance(
                    publicationId = p.publicationId,
                    title = p.title,
                    saved = item.saved
                ).show(
                    (ctx as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
                        ?: return@setOnClickListener,
                    "options"
                )
            }
            // Los callbacks del sheet se resuelven en el Fragment vía
            // parentFragmentManager listeners (ver FragmentoChambas).
        }
    }

    class Diff : DiffUtil.ItemCallback<PublicationFeedItem>() {
        override fun areItemsTheSame(a: PublicationFeedItem, b: PublicationFeedItem): Boolean =
            a.publication.publicationId == b.publication.publicationId

        override fun areContentsTheSame(a: PublicationFeedItem, b: PublicationFeedItem): Boolean =
            a.publication == b.publication && a.liked == b.liked && a.saved == b.saved &&
                a.likesCount == b.likesCount && a.savesCount == b.savesCount
    }
}
