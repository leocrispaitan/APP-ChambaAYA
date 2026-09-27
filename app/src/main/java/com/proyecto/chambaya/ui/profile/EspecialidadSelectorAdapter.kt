package com.proyecto.chambaya.ui.profile

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
import com.proyecto.chambaya.data.model.OficioCategoria

/**
 * FASE 2 — Selector múltiple de especialidades.
 *
 * Reemplaza a los seis `CheckBox` fijos del wizard: los oficios salen de
 * `assets/api_oficios.json`, que es el mismo catálogo que usa el grid de
 * categorías de las chambas.
 *
 * El tope de 3 especialidades lo aplica la Activity (que muestra el aviso);
 * este adaptador solo refleja el estado visual de lo que ya está elegido.
 */
class EspecialidadSelectorAdapter(
    private val onToggle: (String) -> Unit
) : RecyclerView.Adapter<EspecialidadSelectorAdapter.OficioViewHolder>() {

    private var items: List<OficioCategoria> = emptyList()
    private var seleccionados: Set<String> = emptySet()

    /**
     * Un solo `ImageLoader` para toda la lista: los iconos del catálogo son SVG
     * y el catálogo ya viene filtrado por la búsqueda, así que no hace falta
     * crear uno por cada fila.
     */
    private var imageLoader: ImageLoader? = null

    /** Carga la lista visible (ya filtrada por la búsqueda). */
    fun submit(lista: List<OficioCategoria>) {
        items = lista
        notifyDataSetChanged()
    }

    /** Marca qué oficios están elegidos. */
    fun setSeleccionados(valores: Set<String>) {
        seleccionados = valores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OficioViewHolder {
        if (imageLoader == null) {
            imageLoader = ImageLoader.Builder(parent.context)
                .components { add(SvgDecoder.Factory()) }
                .build()
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_oficio_selector, parent, false)
        return OficioViewHolder(view)
    }

    override fun onBindViewHolder(holder: OficioViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class OficioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcono: ImageView = itemView.findViewById(R.id.ivOficioIcono)
        private val tvNombre: TextView = itemView.findViewById(R.id.tvOficioNombre)
        private val ivCheck: ImageView = itemView.findViewById(R.id.ivOficioCheck)

        fun bind(oficio: OficioCategoria) {
            val elegido = seleccionados.contains(oficio.categoria)

            tvNombre.text = oficio.categoria
            itemView.isSelected = elegido
            ivCheck.visibility = if (elegido) View.VISIBLE else View.INVISIBLE
            itemView.contentDescription = oficio.categoria

            val request = ImageRequest.Builder(itemView.context)
                .data(oficio.icono)
                .target(ivIcono)
                .placeholder(R.drawable.ic_cat_construccion)
                .error(R.drawable.ic_cat_construccion)
                .build()
            imageLoader?.enqueue(request)

            itemView.setOnClickListener { onToggle(oficio.categoria) }
        }
    }
}
