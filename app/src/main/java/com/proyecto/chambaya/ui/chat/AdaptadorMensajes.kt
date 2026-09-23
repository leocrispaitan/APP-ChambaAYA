package com.proyecto.chambaya.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.proyecto.chambaya.R

class AdaptadorMensajes(
    private val listaMensajes: MutableList<MensajeChat>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RECEIVED = 1
        private const val TYPE_SENT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (listaMensajes[position].esMio) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_bubble_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_bubble_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = listaMensajes[position]
        if (holder is SentViewHolder) {
            holder.bind(mensaje)
        } else if (holder is ReceivedViewHolder) {
            holder.bind(mensaje)
        }
    }

    override fun getItemCount(): Int = listaMensajes.size

    fun agregarMensaje(mensaje: MensajeChat) {
        listaMensajes.add(mensaje)
        notifyItemInserted(listaMensajes.size - 1)
    }

    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvReceivedMessage: TextView = itemView.findViewById(R.id.tvReceivedMessage)
        private val tvReceivedTime: TextView = itemView.findViewById(R.id.tvReceivedTime)

        fun bind(mensaje: MensajeChat) {
            tvReceivedMessage.text = mensaje.texto
            tvReceivedTime.text = mensaje.hora
        }
    }

    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSentMessage: TextView = itemView.findViewById(R.id.tvSentMessage)
        private val tvSentTime: TextView = itemView.findViewById(R.id.tvSentTime)
        private val ivSentStatus: ImageView = itemView.findViewById(R.id.ivSentStatus)

        fun bind(mensaje: MensajeChat) {
            tvSentMessage.text = mensaje.texto
            tvSentTime.text = mensaje.hora
            ivSentStatus.setImageResource(R.drawable.ic_chat_double_check)
        }
    }
}
