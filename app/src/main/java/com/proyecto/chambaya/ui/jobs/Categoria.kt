package com.proyecto.chambaya.ui.jobs

/**
 * Modelo de datos para una categoría de oficios
 */
data class Categoria(
    val id: Int,
    val categoria: String,
    val puesto: String,
    val icono: String,
    val descripcion: String,
    val habilidades_requeridas: List<String>
)
