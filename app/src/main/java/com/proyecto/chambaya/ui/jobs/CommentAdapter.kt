package com.proyecto.chambaya.ui.jobs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.PublicationComment
import com.proyecto.chambaya.data.model.publicationTimeAgo

/**
 * FASE 10 — Lista de comentarios con menú propio/ajeno.
 */
class CommentAdapter(
    private val onMenu: (PublicationComment, View) -> Unit = { _, _ -> }
) : ListAdapter<PublicationComment, CommentAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return VH(v, onMenu)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(view: View, private val onMenu: (PublicationComment, View) -> Unit) :
        RecyclerView.ViewHolder(view) {
        private val ivAvatar: ShapeableImageView = view.findViewById(R.id.ivCommentAvatar)
        private val tvAuthor: TextView = view.findViewById(R.id.tvCommentAuthor)
        private val tvTime: TextView = view.findViewById(R.id.tvCommentTime)
        private val tvText: TextView = view.findViewById(R.id.tvCommentText)
        private val btnMenu: ImageButton = view.findViewById(R.id.btnCommentMenu)

        fun bind(c: PublicationComment) {
            tvAuthor.text = c.authorName.ifBlank { "@${c.authorUsername}".ifBlank { itemView.context.getString(R.string.k_com_usuario) } }
            tvTime.text = publicationTimeAgo(c.createdAt)
            tvText.text = c.text
            if (c.authorPhotoUrl.isNotBlank()) {
                ivAvatar.load(c.authorPhotoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_circle)
                    error(R.drawable.ic_user_circle)
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_circle)
            }
            btnMenu.setOnClickListener { onMenu(c, it) }
        }
    }

    class Diff : DiffUtil.ItemCallback<PublicationComment>() {
        override fun areItemsTheSame(a: PublicationComment, b: PublicationComment): Boolean =
            a.commentId == b.commentId
        override fun areContentsTheSame(a: PublicationComment, b: PublicationComment): Boolean =
            a == b
    }
}
