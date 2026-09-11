package com.proyecto.chambaya.ui.jobs

import androidx.annotation.DrawableRes

/**
 * Modelo de datos para representar una Card de Chamba/Trabajo
 */
data class JobCard(
    val id: String,
    val titulo: String,
    val categoria: String,
    val rating: Float,
    val precio: String,
    @DrawableRes val iconoCategoria: Int,
    val colorFondo: String,
    var isFavorito: Boolean = false
)
