package com.proyecto.chambaya.ui.chat

import androidx.annotation.DrawableRes

data class ChatConversacion(
    val id: String,
    val nombre: String,
    val ultimoMensaje: String,
    val hora: String,
    val noLeidos: Int = 0,
    @param:DrawableRes val avatarResId: Int = 0,
    val photoUrl: String = "",
    val estaEnLinea: Boolean = false,
    val esFavorito: Boolean = false,
    val otherUid: String = "",
    val publicationId: String = "",
    val publicationTitle: String = "",
    val lastMessageAtEpochMillis: Long = 0L
)

data class MensajeChat(
    val id: String,
    val texto: String,
    val hora: String,
    val esMio: Boolean,
    val estaLeido: Boolean = true
)
