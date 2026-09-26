package com.proyecto.chambaya.ui.jobs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.proyecto.chambaya.R

/**
 * Adaptador para mostrar categorías en un RecyclerView horizontal
 * Usa Coil para cargar imágenes SVG con calidad perfecta
 */
class CategoriaAdapter(
    private val categorias: List<Categoria>,
    private val onCategoriaClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)
        return CategoriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        holder.bind(categorias[position])
    }

    override fun getItemCount(): Int = categorias.size

    inner class CategoriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)

        fun bind(categoria: Categoria) {
            tvCategoryName.text = categoria.categoria
            
            // Configurar ImageLoader con soporte SVG
            val imageLoader = ImageLoader.Builder(itemView.context)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()

            // Crear request para cargar SVG
            val request = ImageRequest.Builder(itemView.context)
                .data(categoria.icono)
                .target(ivCategoryIcon)
                .placeholder(R.drawable.ic_cat_construccion)
                .error(R.drawable.ic_cat_construccion)
                .build()

            // Cargar la imagen
            imageLoader.enqueue(request)

            // Manejar click en la categoría
            itemView.setOnClickListener {
                onCategoriaClick(categoria)
            }
        }
    }
}
