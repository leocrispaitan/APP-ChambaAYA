package com.proyecto.chambaya.ui.jobs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.proyecto.chambaya.R

/** Páginas del carrusel de fotos del detalle (ViewPager2). */
class DetailPhotoAdapter(
    private val urls: List<String>
) : RecyclerView.Adapter<DetailPhotoAdapter.PhotoVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detail_photo, parent, false)
        return PhotoVH(v)
    }

    override fun onBindViewHolder(holder: PhotoVH, position: Int) {
        holder.bind(urls[position])
    }

    override fun getItemCount(): Int = urls.size

    inner class PhotoVH(view: View) : RecyclerView.ViewHolder(view) {
        private val imagen: ImageView = view.findViewById(R.id.ivDetailPhotoPage)

        fun bind(url: String) {
            imagen.load(url) {
                crossfade(true)
                placeholder(R.drawable.bg_job_image_placeholder)
                error(R.drawable.bg_job_image_placeholder)
            }
        }
    }
}
