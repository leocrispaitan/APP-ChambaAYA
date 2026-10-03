package com.proyecto.chambaya.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.proyecto.chambaya.R
import com.proyecto.chambaya.data.remote.GeoLayers
import com.proyecto.chambaya.data.remote.GeoPlace

/**
 * Sugerencias de lugares compartido por el buscador del feed y el
 * registro del local.
 */
class GeoPlaceAdapter(
    private val onPick: (GeoPlace) -> Unit = {}
) : ListAdapter<GeoPlace, GeoPlaceAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_geo_place, parent, false)
        return VH(v, onPick)
    }

    override fun onBindViewHolder(h: VH, p: Int) = h.bind(getItem(p))

    class VH(view: View, private val onPick: (GeoPlace) -> Unit) : RecyclerView.ViewHolder(view) {
        private val iv: ImageView = view.findViewById(R.id.ivGeoIcon)
        private val tvName: TextView = view.findViewById(R.id.tvGeoName)
        private val tvLabel: TextView = view.findViewById(R.id.tvGeoLabel)
        private val tvType: TextView = view.findViewById(R.id.tvGeoType)

        fun bind(place: GeoPlace) {
            iv.setImageResource(GeoLayers.iconFor(place.layer))
            tvName.text = place.name.ifBlank { place.label }
            tvLabel.text = place.subtitle
            tvLabel.visibility = if (tvLabel.text.isBlank()) View.GONE else View.VISIBLE
            tvType.text = GeoLayers.humanFor(place.layer)
            itemView.setOnClickListener { onPick(place) }
        }
    }

    class Diff : DiffUtil.ItemCallback<GeoPlace>() {
        override fun areItemsTheSame(a: GeoPlace, b: GeoPlace): Boolean =
            a.latitude == b.latitude && a.longitude == b.longitude && a.label == b.label
        override fun areContentsTheSame(a: GeoPlace, b: GeoPlace): Boolean = a == b
    }
}