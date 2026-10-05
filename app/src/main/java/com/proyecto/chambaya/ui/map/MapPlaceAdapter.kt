package com.proyecto.chambaya.ui.map

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.model.precioTexto

/**
 * Chambas cercanas ordenadas por distancia. Tap → fijar en el mapa + ruta
 * (el detalle completo solo se abre desde lo marcado en el mapa).
 */
class MapPlaceAdapter(
    private val onLocate: (MapPin) -> Unit = {}
) : ListAdapter<MapPin, MapPlaceAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_map_place, parent, false)
        return VH(v, onLocate)
    }

    override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))

    class VH(view: View, private val onLocate: (MapPin) -> Unit) : RecyclerView.ViewHolder(view) {
        private val ivPhoto: ShapeableImageView = view.findViewById(R.id.ivMapPlacePhoto)
        private val ivIcon: ImageView = view.findViewById(R.id.ivMapPlaceIcon)
        private val tvTitle: TextView = view.findViewById(R.id.tvMapPlaceTitle)
        private val tvMeta: TextView = view.findViewById(R.id.tvMapPlaceMeta)
        private val tvDist: TextView = view.findViewById(R.id.tvMapPlaceDist)

        fun bind(pin: MapPin) {
            val pub = pin.publication
            tvTitle.text = pub.title
            tvMeta.text = "${pub.category.ifBlank { itemView.context.getString(R.string.k_detalle_chamba) }} · ${pub.precioTexto(itemView.context)}"
            tvDist.text = MapGeo.formatDistance(pin.distanceKm)

            val photo = pub.images.firstOrNull()?.url.orEmpty()
            if (photo.isNotBlank()) {
                ivIcon.visibility = View.GONE
                ivPhoto.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                ivPhoto.load(photo) {
                    crossfade(true)
                    placeholder(R.drawable.bg_job_image_placeholder)
                    error(R.drawable.bg_job_image_placeholder)
                }
            } else {
                // Bloque de color con ícono (mismo lenguaje del feed sin foto).
                ivPhoto.load(null) // cancela cualquier carga en curso de la vista reciclada
                ivPhoto.setBackgroundColor(Color.parseColor("#1E293B"))
                ivIcon.visibility = View.VISIBLE
                ivIcon.setImageResource(R.drawable.ic_cat_construccion)
            }
            itemView.setOnClickListener { onLocate(pin) }
        }
    }

    class Diff : DiffUtil.ItemCallback<MapPin>() {
        override fun areItemsTheSame(a: MapPin, b: MapPin): Boolean =
            a.publication.publicationId == b.publication.publicationId
        override fun areContentsTheSame(a: MapPin, b: MapPin): Boolean = a == b
    }
}
