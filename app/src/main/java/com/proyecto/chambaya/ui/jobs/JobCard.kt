package com.proyecto.chambaya.ui.jobs

import androidx.annotation.DrawableRes

/**
 * Modelo de datos para representar una Card de Chamba/Trabajo
 * Rediseñado con campos adicionales para el estilo publicación de Instagram
 */
data class JobCard(
    val id: String,
    val titulo: String,
    val categoria: String,
    val rating: Float,
    val precio: String,
    @DrawableRes val iconoCategoria: Int,
    val colorFondo: String,
    var isFavorito: Boolean = false,

    // Campos nuevos para estilo Instagram
    val empleador: String = "",
    val distrito: String = "",
    val tiempoPublicado: String = "",
    val descripcion: String = "",
    val imagenUrl: String = ""
)
