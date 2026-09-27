package com.proyecto.chambaya.data.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

/**
 * FASE 2 — Catálogo de oficios.
 *
 * Las especialidades del perfil NO se escriben a mano en el formulario: salen de
 * `assets/api_oficios.json`, el mismo archivo que ya alimenta el grid de
 * categorías de `fragmento_chambas.xml`. Así el trabajador solo puede elegir
 * oficios que existen en ChambAYA y las specialties de su perfil casan con las
 * categorías de las publicaciones.
 *
 * ```
 * {
 *   "id": 3,
 *   "categoria": "Construcción",           // -> se guarda en worker.specialties
 *   "puesto": "Ayudante de albañilería / Peón",
 *   "icono": "https://res.cloudinary.com/vtmk2tgh/image/upload/construccion.svg",
 *   "descripcion": "...",
 *   "habilidades_requeridas": ["Preparación de mezcla", ...]  // -> sugerencia de skills
 * }
 * ```
 */
data class OficioCategoria(
    val id: Int,
    val categoria: String,
    val puesto: String,
    val icono: String,
    val descripcion: String,
    val habilidades_requeridas: List<String>
)

/**
 * Lector del catálogo de oficios. Se carga una vez y se cachea en memoria.
 *
 * El proyecto ya parsea este mismo JSON en `FragmentoChambas`; aquí se reutiliza
 * el formato en lugar de inventar otra lista de oficios.
 */
object OficioCatalog {

    private const val ASSET_NAME = "api_oficios.json"

    @Volatile
    private var cache: List<OficioCategoria>? = null

    /** Categorías en el orden del JSON. Nunca lanza: si falla, devuelve vacío. */
    fun load(context: Context): List<OficioCategoria> {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: leer(context).also { cache = it }
        }
    }

    private fun leer(context: Context): List<OficioCategoria> = runCatching {
        val json = context.applicationContext.assets.open(ASSET_NAME)
            .bufferedReader()
            .use { it.readText() }
        val tipo = object : TypeToken<List<OficioCategoria>>() {}.type
        Gson().fromJson<List<OficioCategoria>>(json, tipo)
            ?.filter { it.categoria.isNotBlank() }
            .orEmpty()
    }.getOrDefault(emptyList())

    /** Nombres de categoría, en el orden del JSON y sin repetir. */
    fun nombres(context: Context): List<String> =
        load(context).map { it.categoria }.distinct()

    /**
     * Busca una categoría ignorando mayúsculas, acentos y espacios extra.
     *
     * Necesario porque `worker.specialties` guarda el texto tal como se eligió y
     * ese texto vuelve a compararse contra el catálogo al pintar el perfil.
     */
    fun buscar(context: Context, nombre: String): OficioCategoria? {
        val objetivo = normalizar(nombre)
        if (objetivo.isEmpty()) return null
        return load(context).firstOrNull { normalizar(it.categoria) == objetivo }
    }

    /** URL del icono SVG de un oficio, o `null` si no está en el catálogo. */
    fun iconoDe(context: Context, nombre: String): String? =
        buscar(context, nombre)?.icono?.takeIf { it.isNotBlank() }

    /**
     * Normaliza texto para comparar: minúsculas, sin tildes y sin espacios
     * sobrantes. Mismo criterio que [normalizarUsername] para los `@usuario`.
     *
     * Se usa `Normalizer` en vez de reemplazar letra por letra para que cualquier
     * vocal acentuada (incluida la "ü" o la "ñ") quede plana sin casos sueltos.
     */
    fun normalizar(texto: String?): String {
        val plano = java.text.Normalizer
            .normalize(texto ?: "", java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .filter { it.isLetterOrDigit() || it == ' ' }
        return plano.replace(Regex("\\s+"), " ").trim()
    }
}

/**
 * Normalización de `@nombreDeUsuario`.
 *
 * El `usernameNormalized` es la clave real de unicidad: se guarda siempre en
 * minúsculas, sin tildes y sin espacios, de modo que "Maria.Chambaya",
 * "maria chambaya" y "mariachambaya" colisionen en el mismo documento
 * `usernames/{normalized}` y no se puedan registrar dos veces.
 *
 * Solo deja `a-z`, `0-9`, `_` y `.`, que es exactamente lo que accepts las
 * Rules (`^[a-z0-9._]+$`). Si la app admitiera un carácter que las Rules no,
 * el guardado se rechazaría con `PERMISSION_DENIED`: es mejor que el usuario
 * vea el `Toast` de validación que un error de servidor.
 */
fun normalizarUsername(raw: String?): String {
    val base = OficioCatalog.normalizar(raw)
    return base
        .lowercase(Locale.ROOT)
        .replace(" ", "_")
        .replace(Regex("_{2,}"), "_")
        .filter { esCaracterUsername(it) }
        .trim('_', '.')
}

/** `true` para lo que las Rules aceptan en `profile.username`. */
private fun esCaracterUsername(c: Char): Boolean =
    c in 'a'..'z' || c in '0'..'9' || c == '_' || c == '.'

/**
 * ¿El texto escrito puede guardarse como `profile.username`?
 *
 * Acepta mayúsculas y espacios (se normalizan al guardar) pero rechaza cualquier
 * otro símbolo, para que el `@usuario` de la URL siempre sea limpio.
 */
fun esUsernameValido(username: String): Boolean {
    val limpio = username.trim()
    if (limpio.isEmpty()) return false
    if (limpio.any { !it.isLetterOrDigit() && it != '_' && it != '.' && it != ' ' }) return false
    val normalizado = normalizarUsername(limpio)
    return normalizado.length in ProfileLimits.USERNAME_MIN..ProfileLimits.USERNAME_MAX
}
