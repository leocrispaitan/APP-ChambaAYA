package com.proyecto.chambaya.data.model

/**
 * FASE 1 — Utilidad para separar nombres y apellidos desde el nombre completo
 * devuelto por RENIEC.
 *
 * RENIEC devuelve el nombre en mayúsculas, por ejemplo:
 *   "MARIA ELENA SANCHEZ QUISPE"
 *
 * Heurística (peruano estándar):
 *   - los dos últimos tokens son apellido paterno y materno;
 *   - el resto son los nombres.
 *
 * No reemplaza al padrón oficial: solo sirve para rellenar
 * `profile.firstName` / `profile.lastName`.
 */
object IdentityNameParser {

    data class SplitName(val firstName: String, val lastName: String)

    fun parseFirstAndLastName(rawName: String?): SplitName {
        val normalized = normalize(rawName)
        if (normalized.isEmpty()) return SplitName("", "")

        val tokens = normalized.split(' ').filter { it.isNotBlank() }
        return when {
            tokens.isEmpty() -> SplitName("", "")
            tokens.size == 1 -> SplitName(tokens[0], "")
            tokens.size == 2 -> SplitName(tokens[0], tokens[1])
            else -> SplitName(
                firstName = tokens.dropLast(2).joinToString(" "),
                lastName = tokens.takeLast(2).joinToString(" ")
            )
        }
    }

    /** "MARIA SANCHEZ" -> "Maria Sanchez" (respeta partículas como DE / DEL / LA). */
    fun toDisplayCase(rawName: String?): String {
        val normalized = normalize(rawName)
        if (normalized.isEmpty()) return ""

        return normalized
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                val lower = token.lowercase()
                val capitalized = lower.replaceFirstChar { it.uppercase() }
                if (lower.length <= 3 && PARTICLES.contains(lower)) lower else capitalized
            }
    }

    private fun normalize(rawName: String?): String {
        if (rawName.isNullOrBlank()) return ""
        return rawName
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private val PARTICLES = setOf("de", "del", "la", "las", "los", "y", "van", "von", "da", "das", "di")
}
