package com.proyecto.chambaya.ui.jobs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.proyecto.chambaya.R

/**
 * Mismo diseño que ChambAYA-APP-main (AdaptadorChipCategoria):
 * tarjeta 112x104 con foto + etiqueta abajo.
 */
class CategoriaAdapter(
    private val categorias: List<String>,
    private var seleccionada: String = "Todas",
    private val onCategoriaClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = categorias[position].hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(categorias[position])
    }

    override fun getItemCount(): Int = categorias.size

    fun setSeleccionada(nombre: String) {
        // El bottom sheet maneja categorías de `allItems` (taxonomía vieja)
        // que pueden no estar en el grid: en ese caso se vuelve a "Todas"
        // para no dejar el adapter en un estado imposible.
        val destino = if (categorias.any { it.equals(nombre, ignoreCase = true) }) nombre else "Todas"
        if (destino.equals(seleccionada, ignoreCase = true)) return
        val anterior = categorias.indexOfFirst { it.equals(seleccionada, ignoreCase = true) }
        seleccionada = destino
        if (anterior >= 0) notifyItemChanged(anterior)
        val nueva = categorias.indexOfFirst { it.equals(seleccionada, ignoreCase = true) }
        if (nueva >= 0) notifyItemChanged(nueva)
    }

    inner class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        private val imagen: ImageView = itemView.findViewById(R.id.ivCategoryImage)
        private val etiqueta: TextView = itemView.findViewById(R.id.tvChipLabel)

        fun bind(categoria: String) {
            etiqueta.text = categoria
            imagen.setImageResource(ImagenCategoriaChamba.obtener(categoria))
            val esSeleccionada = categoria.equals(seleccionada, ignoreCase = true)
            val ctx = itemView.context
            card.strokeColor = ContextCompat.getColor(
                ctx,
                if (esSeleccionada) R.color.brand_color else R.color.divider
            )
            card.strokeWidth = ((if (esSeleccionada) 2 else 1) * ctx.resources.displayMetrics.density).toInt()

            card.setOnClickListener {
                if (categoria == seleccionada) return@setOnClickListener
                val anterior = categorias.indexOf(seleccionada)
                val nueva = bindingAdapterPosition
                if (nueva == RecyclerView.NO_POSITION) return@setOnClickListener
                seleccionada = categoria
                if (anterior >= 0) notifyItemChanged(anterior)
                notifyItemChanged(nueva)
                onCategoriaClick(categoria)
            }
        }
    }
}
