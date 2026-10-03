package com.proyecto.chambaya.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import com.proyecto.chambaya.R

class AdaptadorConversaciones(
    private var listaOriginal: List<ChatConversacion>,
    private val onItemClick: (ChatConversacion) -> Unit
) : RecyclerView.Adapter<AdaptadorConversaciones.ChatViewHolder>() {

    private var listaFiltrada: List<ChatConversacion> = listaOriginal

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ShapeableImageView = itemView.findViewById(R.id.ivAvatar)
        val viewOnlineDot: View = itemView.findViewById(R.id.viewOnlineDot)
        val tvContactName: TextView = itemView.findViewById(R.id.tvContactName)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvUnreadBadge: TextView = itemView.findViewById(R.id.tvUnreadBadge)

        fun bind(item: ChatConversacion) {
            if (item.photoUrl.isNotBlank()) {
                ivAvatar.load(item.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_circle)
                    error(R.drawable.ic_user_circle)
                }
            } else if (item.avatarResId != 0) {
                ivAvatar.setImageResource(item.avatarResId)
            } else {
                ivAvatar.setImageResource(R.drawable.ic_user_circle)
            }
            viewOnlineDot.visibility = if (item.estaEnLinea) View.VISIBLE else View.GONE
            tvContactName.text = item.nombre
            tvLastMessage.text = item.ultimoMensaje
            tvTime.text = item.hora

            if (item.noLeidos > 0) {
                tvUnreadBadge.visibility = View.VISIBLE
                tvUnreadBadge.text = item.noLeidos.toString()
            } else {
                tvUnreadBadge.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_conversacion, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(listaFiltrada[position])
    }

    override fun getItemCount(): Int = listaFiltrada.size

    fun actualizarLista(nuevaLista: List<ChatConversacion>) {
        actualizarYFiltrar(nuevaLista, TipoFiltro.TODOS)
    }

    /**
     * Actualiza datos + filtro de tipo + texto en una sola pasada y solo
     * repinta si lo visible realmente cambió: así volver del detalle no
     * hace parpadear la lista.
     */
    fun actualizarYFiltrar(
        nuevaLista: List<ChatConversacion>,
        tipo: TipoFiltro,
        query: String = ""
    ) {
        listaOriginal = nuevaLista
        var filtrada = when (tipo) {
            TipoFiltro.TODOS -> nuevaLista
            TipoFiltro.NO_LEIDOS -> nuevaLista.filter { it.noLeidos > 0 }
            TipoFiltro.FAVORITOS -> nuevaLista.filter { it.esFavorito }
        }
        if (query.isNotBlank()) {
            filtrada = filtrada.filter {
                it.nombre.contains(query, ignoreCase = true) ||
                    it.ultimoMensaje.contains(query, ignoreCase = true)
            }
        }
        if (filtrada != listaFiltrada) {
            listaFiltrada = filtrada
            notifyDataSetChanged()
        }
    }

    fun filtrarPorTexto(query: String) {
        val nueva = if (query.isBlank()) {
            listaOriginal
        } else {
            listaOriginal.filter {
                it.nombre.contains(query, ignoreCase = true) ||
                        it.ultimoMensaje.contains(query, ignoreCase = true)
            }
        }
        if (nueva != listaFiltrada) {
            listaFiltrada = nueva
            notifyDataSetChanged()
        }
    }

    fun filtrarPorTipo(tipo: TipoFiltro) {
        val nueva = when (tipo) {
            TipoFiltro.TODOS -> listaOriginal
            TipoFiltro.NO_LEIDOS -> listaOriginal.filter { it.noLeidos > 0 }
            TipoFiltro.FAVORITOS -> listaOriginal.filter { it.esFavorito }
        }
        if (nueva != listaFiltrada) {
            listaFiltrada = nueva
            notifyDataSetChanged()
        }
    }

    enum class TipoFiltro {
        TODOS, NO_LEIDOS, FAVORITOS
    }
}
