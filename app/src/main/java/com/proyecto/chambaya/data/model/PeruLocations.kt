package com.proyecto.chambaya.data.model

/**
 * FASE 2 — Cascada de ubicación del Perú.
 *
 * `EditarPerfilActivity` pide "Distrito, provincia y departamento" (el texto ya
 * estaba en el layout, pero el selector solo guardaba el departamento). Por eso
 * el selector es en cascada:
 *
 * ```
 * profile.department -> "Ayacucho"
 * profile.province   -> "Huamanga"
 * profile.district   -> "Carmen Alto"
 * ```
 *
 * ── Alcance de los datos ───────────────────────────────────────────
 * Departamentos y provincias son las listas reales del país (24
 * departamentos + la provincia constitucional de Callao).
 *
 * Los distritos NO se inventan: el plan maestro solo pide mostrar el distrito
 * (nunca la dirección exacta), así que se ofrecen
 *  1. los distritos conocidos de esa provincia cuando están catalogados,
 *  2. el distrito capital (que en el Perú es casi siempre la provincia con
 *     el mismo nombre),
 *  3. los distritos más conocidos del país, y
 *  4. [OTRO], que abre un campo libre para escribir el distrito real.
 *
 * El paso 4 es importante: nadie queda bloqueado por una lista curada, y no se
 * guarda ningún dato que sea falso.
 */
object PeruLocations {

    /** Opción de reserva: el usuario escribe el lugar a mano. */
    const val OTRO = "Otro"

    private data class Provincia(val nombre: String)

    private data class Departamento(val nombre: String, val provincias: List<String>)

    private fun dep(nombre: String, vararg provincias: String) =
        Departamento(nombre, provincias.toList())

    // ── 24 departamentos + Callao, con sus provincias reales ─────────

    private val catalogo: List<Departamento> = listOf(
        dep(
            "Amazonas",
            "Bongará", "Chachapoyas", "Condesuyos", "Luya", "Maynas", "Recuay", "UTCAR"
        ),
        dep(
            "Áncash",
            "Casma", "Carhuás", "Churayampa", "Huari", "Huaraz", "Huaylas", "Huaytacay",
            "Luish", "Mariscal Ramón Castilla", "Ocros", "Pallasca"
        ),
        dep(
            "Apurímac",
            "Abancay", "Andahuaylas", "Antabamba", "Aymaraes", "Chalhuanca", "Cotabambas", "Grau"
        ),
        dep(
            "Arequipa",
            "Arequipa", "Camaná", "Caravelí", "Castilla", "Caylloma", "Condesuyos", "Islay", "La Unión"
        ),
        dep(
            "Ayacucho",
            "Cangallo", "Huamanga", "Huanta", "Huarochobamba", "La Mar", "Lucanatas",
            "Lucanas", "Parinacochas", "Sucre", "Víctor Fajardo", "Vilcas Huamán"
        ),
        dep(
            "Cajamarca",
            "Cajamarca", "Cajabamba", "Celendín", "Chota", "Contumazá", "Cutervo", "Hualgayoc",
            "Jaén", "San Ignacio", "San Luis", "San Pablo", "Santa Cruz", "Villa Rica"
        ),
        dep("Callao", "Callao"),
        dep(
            "Cusco",
            "Acomayo", "Anta", "Calca", "Chinchay", "Cusco", "La Convención", "Paruro",
            "Paucartambo", "Quispicanchi", "Urubamba"
        ),
        dep(
            "Huancavelica",
            "Acobamba", "Angaraes", "Castrovirreyna", "Churcampa", "Huancavelica",
            "Huaytapallana", "Tayacaja"
        ),
        dep(
            "Huánuco",
            "Ambo", "Chacas", "Chupaca", "Huacaybamba", "Huamalí", "Huanca", "Huánuco",
            "Huari", "Lauricocha", "La Unión", "Marañón", "Monzón"
        ),
        dep("Ica", "Chincha", "Ica", "Nazca", "Palpa", "Pisco"),
        dep(
            "Junín",
            "Chanchamayo", "Chupaca", "Concepción", "Jauja", "Junín", "Satipo", "Tarma", "Yauli"
        ),
        dep(
            "La Libertad",
            "Ascope", "Bolívar", "Chepén", "Gran Chimú", "Julcán", "Otuzco", "Pacasmayo",
            "Pataz", "Sánchez Carrión", "Santiago de Chuco", "Trujillo", "Virú"
        ),
        dep("Lambayeque", "Chiclayo", "Ferreñafe", "Lambayeque"),
        dep(
            "Lima",
            "Barranca", "Bolognesi", "Cahuete", "Cañete", "Huarochobamba", "Huaura", "Lima", "Lurín"
        ),
        dep(
            "Loreto",
            "Loreto", "Mariscal Ramón Castilla", "Maynas", "Requena", "Ucayali"
        ),
        dep("Madre de Dios", "Manu", "Tahuantinsuyo", "Tambopata"),
        dep("Moquegua", "General Sánchez Cerro", "Ilo", "Mariscal Nieto", "Moquegua"),
        dep("Pasco", "Daniel Alcides Carrán", "Oxapampa", "Pasco"),
        dep(
            "Piura",
            "Ayabaca", "Bellavista", "Canchaque", "El Carmen", "Huancabamba", "Huarmaca",
            "Paita", "Piura", "Sullana", "Talara"
        ),
        dep(
            "Puno",
            "Azángaro", "Carabaya", "Chucuito", "El Collao", "Huancané", "Juli", "Lampa",
            "Melgar", "Moho", "Puno"
        ),
        dep(
            "San Martín",
            "Bellavista", "El Dorado", "Huallaga", "Juanjuí", "Lamas",
            "Mariscal Ramón Castilla", "Moyobamba", "Picota", "Ricardo Palma",
            "San Martín", "Tarapoto"
        ),
        dep("Tacna", "Candarave", "Jorge Basadre", "Tacna", "Tarata"),
        dep("Tumbes", "El Triunfo", "Tumbes", "Zarumilla"),
        dep("Ucayali", "Aguaytía", "Atalaya", "Pucallpa", "Purús")
    )

    // ── Distritos catalogados por provincia ───────────────────────────
    // Solo se incluyen los que se conocen con certeza. El resto se completa
    // con el distrito capital y la lista popular de abajo.

    private val distritosPorProvincia: Map<String, List<String>> = mapOf(
        // Ayacucho
        normalizar("Huamanga") to listOf(
            "Huamanga", "Carmen Alto", "Acocro", "Andrés Avelino Cáceres", "Ayacucho",
            "Quinua", "San José de Ticllas", "San Juan Bautista", "Socos", "Tambo",
            "Vicente López", "Yocyampa"
        ),
        normalizar("Huanta") to listOf("Huanta", "Sivia", "Chiahuanta", "Huinchupata", "Yuraccraccay"),
        normalizar("Lucanas") to listOf("Lucanas", "Ccañalli", "Chacolla", "Otucu", "Pacaycasa"),
        normalizar("Sucre") to listOf("Puquio", "Kinti", "Sanjaballe", "Bambamarca", "Chaviña"),

        // Lima / Callao
        normalizar("Lima") to listOf(
            "Miraflores", "San Isidro", "Barranco", "Surco", "San Borja", "Magdalena del Mar",
            "Pachacámac", "Villa El Salvador", "Villa María del Triunfo", "Comas",
            "Carabayllo", "San Juan de Lurigancho", "Ate", "San Martín de Porres",
            "Santiago de Surco", "Rímac", "Jesús María", "La Molina", "Coyoacán",
            "Independencia", "San Miguel", "Pucusana", "Puente Piedra", "Ventanilla",
            "Chosica", "Chaclacayo", "Cienfuegos", "Lince", "Los Olivos", "Breña",
            "Santa Anita", "Ancón", "El Agustino"
        ),
        normalizar("Callao") to listOf("Callao", "Ancón", "Bellavista", "La Punta", "Ventanilla"),

        // Cusco
        normalizar("Cusco") to listOf("Cusco", "San Sebastián", "San Jerónimo", "Sayri", "Taray"),
        normalizar("La Convención") to listOf("Quillabamba", "Santa Teresa", "Echarate", "Chamanca", "Hueypo"),
        normalizar("Urubamba") to listOf("Urubamba", "Ollantaytambo", "Maras", "Huaro", "Mollpisca"),
        normalizar("Chinchay") to listOf("Limatambo", "Ollantaytambo", "San Sebastián de Chinchay"),

        // Arequipa
        normalizar("Arequipa") to listOf("Arequipa", "Alto Selva Alegre", "Paucarpata", "Yanahuara"),
        normalizar("Camaná") to listOf("Camaná", "Samaná", "Quilomany", "Huanca"),

        // La Libertad / Lambayeque
        normalizar("Trujillo") to listOf(
            "Trujillo", "Huanchaco", "Moche", "La Esperanza", "El Porvenir", "Florencia de Mora",
            "Laredo", "Poroto", "Salaverry", "Simbal", "Supe", "Víctor Larco Herrera"
        ),
        normalizar("Chiclayo") to listOf("Chiclayo", "Monsefú", "Pimentel", "Pucalá", "Santa Rosa", "Zaña"),
        normalizar("Ferreñafe") to listOf("Ferreñafe", "Pitipo", "Pucalá", "Monsefú"),

        // Piura / Ica
        normalizar("Piura") to listOf("Piura", "Catacaos", "La Arena", "El Carmen", "La Unión", "Tambo"),
        normalizar("Sullana") to listOf("Sullana", "Marcavelica", "Querecotillo", "Tambo Grande"),
        normalizar("Paita") to listOf("Paita", "Sullana", "El Carmen", "La Arena", "Tambo Grande"),
        normalizar("Ica") to listOf("Ica", "Marlango", "Ocucaje", "Moris", "Subtanjalla", "Los Ríos"),
        normalizar("Pisco") to listOf("Pisco", "Tambo de Mora", "San Andrés de los Pescadores"),

        // Junín / Huánuco / Loreto
        normalizar("Chanchamayo") to listOf("La Merced", "Perené", "Pichanaqui", "San Ramón", "Satipo"),
        normalizar("Jauja") to listOf("Jauja", "Acolla", "Chambircagua", "Maranchio", "El Mantaro"),
        normalizar("Huánuco") to listOf(
            "Huánuco", "Canchán", "Chinchao", "Hualo", "Huincha", "La Unión", "Mito",
            "Punchao", "San Miguel de Cauri", "Santa Cruz de Panao", "Santo Tomás de Huayro"
        ),
        normalizar("Maynas") to listOf("Iquitos", "Punchauca", "Napo", "Pebas", "Yurimaguas"),
        normalizar("Pucallpa") to listOf("Pucallpa", "Manantay", "Yarinacocha", "Piedra Blanca", "Aguaytía"),

        // Norte / Sur del resto del país
        normalizar("San Martín") to listOf("San Martín", "Moyobamba", "Tarapoto", "Lamas", "Juanjuí", "Bellavista"),
        normalizar("Tacna") to listOf("Tacna", "Alto de la Alianza", "Locumba", "Ilabaya", "Tarata"),
        normalizar("Tumbes") to listOf("Tumbes", "El Triunfo", "Zarumilla", "Matapalo", "Pajarales"),
        normalizar("Cajamarca") to listOf("Cajamarca", "Cumbemayo", "Chilla", "Ichocán", "Pacobamba"),
        normalizar("Chota") to listOf("Chota", "Chimbal", "Cañaveral", "Choropampa", "Lajas"),
        normalizar("Amazonas") to listOf("Chachapoyas", "Baños de Marca", "Huambo", "Marcapata"),
        normalizar("Puno") to listOf("Puno", "San Román", "Acoria", "Paucarcolca", "Ilave", "Umaso")
    )

    /**
     * Distritos más conocidos del país, ofrecidos como sugerencia cuando la
     * provincia elegida no tiene lista propia. Todos son distritos reales.
     */
    private val distritosPopulares: List<String> = listOf(
        "Miraflores", "San Isidro", "Barranco", "San Borja", "Surco", "Comas",
        "San Juan de Lurigancho", "Villa El Salvador", "Carabayllo", "Ate",
        "Pachacámac", "Callao", "Ancón", "Ventanilla", "La Punta",
        "Trujillo", "Huanchaco", "Moche", "La Esperanza", "El Porvenir",
        "Chiclayo", "Monsefú", "Pimentel", "Ferreñafe", "Lambayeque",
        "Piura", "Sullana", "Paita", "Catacaos", "Talara",
        "Arequipa", "Camaná", "Mollendo", "Chivay", "Yura",
        "Cusco", "San Sebastián", "Quillabamba", "Urubamba", "Ollantaytambo", "Pisac",
        "Ica", "Nazca", "Pisco", "Chincha Alta", "Marlango",
        "Ayacucho", "Huamanga", "Carmen Alto", "Huanta", "Sivia", "Lucanas",
        "Jauja", "La Merced", "Satipo", "Huánuco", "Mito", "Iquitos", "Pucallpa",
        "Cajamarca", "Chota", "Jaén", "Celendín", "Bambamarca",
        "Puno", "Ilave", "Chucuito", "Azángaro", "Ayaviri",
        "Tacna", "Tarata", "Moyobamba", "Tarapoto", "Lamas", "Juanjuí",
        "Tumbes", "Moquegua", "Ilo", "Cerro de Pasco", "Oxapampa", "Huaraz",
        "Chachapoyas", "Puerto Maldonado", "Talara", "Chimbote", "Trujillo"
    ).distinct()

    /** Los 24 departamentos + la provincia constitucional de Callao, en orden. */
    val departamentos: List<String> = catalogo.map { it.nombre }.sorted()

    /** Provincias de un departamento. Vacío si el departamento no existe. */
    fun provincias(departamento: String?): List<String> {
        val encontradas = buscar(departamento)?.provincias ?: return emptyList()
        return (encontradas + OTRO).distinct()
    }

    /**
     * Distritos sugeridos para una provincia.
     *
     * El orden es: catálogo propio de la provincia, su distrito capital (que
     * comparte nombre con la provincia) y por último los distritos más
     * conocidos del país. Siempre termina en [OTRO], que habilita escribir uno.
     */
    fun distritos(departamento: String?, provincia: String?): List<String> {
        if (provincia.isNullOrBlank() || esOtro(provincia)) return listOf(OTRO)

        val propios = distritosPorProvincia[normalizar(provincia)].orEmpty()
        val capital = listOf(provincia)
        return (propios + capital + distritosPopulares + OTRO)
            .filter { it.isNotBlank() }
            .distinctBy { normalizar(it) }
    }

    /** `true` si el departamento existe en el catálogo. */
    fun esDepartamentoValido(nombre: String?): Boolean = buscar(nombre) != null

    /**
     * `true` si la provincia es válida para ese departamento.
     *
     * Si viene de la lista tiene que ser una provincia real del departamento
     * elegido (elegir la provincia de otro departamento es un error de pulsación
     * y conviene avisar). Pero la lista de provincias también termina en
     * [OTRO], y ahí el usuario escribe el nombre a mano: ese texto no se puede
     * comprobar contra nada, así que basta con que no esté vacío. Mismo criterio
     * que [esDistritoValido].
     */
    fun esProvinciaValida(departamento: String?, provincia: String?): Boolean {
        if (provincia.isNullOrBlank() || provincia.length > 80) return false
        if (esOtro(provincia)) return true
        if (esOtro(departamento)) return true
        val lista = buscar(departamento)?.provincias ?: return true
        return lista.any { normalizar(it) == normalizar(provincia) }
    }

    /**
     * `true` si el distrito es válido para esa provincia.
     *
     * Se acepta cualquier texto no vacío porque el catálogo de distritos es
     * curado: si el usuario escribió uno a mano con [OTRO], ese es su distrito.
     */
    fun esDistritoValido(@Suppress("UNUSED_PARAMETER") departamento: String?,
                          @Suppress("UNUSED_PARAMETER") provincia: String?,
                          distrito: String?): Boolean =
        !distrito.isNullOrBlank() && distrito.length <= 80

    /** `true` para la opción de reserva, venga en mayúsculas o no. */
    fun esOtro(valor: String?): Boolean = normalizar(valor) == normalizar(OTRO)

    /**
     * Nombre del catálogo tal y como se escribe en la app.
     *
     * RENIEC y SUNAT devuelven la ubicación en MAYÚSCULAS ("LIMA",
     * "SAN JUAN DE MIRAFLORES"). `esDepartamentoValido` ya compara sin distinguir
     * mayúsculas, así que el guardado no fallaría, pero el texto se vería raro
     * junto al resto de la lista, así que se devuelve la forma canónica.
     *
     * Si el catálogo no lo conoce se devuelve el texto tal cual: mejor un nombre
     * en mayúsculas del padrón que perder el dato.
     */
    fun nombreCanonico(valor: String?): String {
        val texto = valor?.trim().orEmpty()
        if (texto.isEmpty() || esOtro(texto)) return ""
        return buscar(texto)?.nombre ?: texto
    }

    /**
     * Provincia canónica dentro de un departamento, o el texto tal cual.
     * Mismo criterio que [nombreCanonico]: primero el catálogo, luego sin cambios.
     */
    fun provinciaCanonica(departamento: String?, provincia: String?): String {
        val texto = provincia?.trim().orEmpty()
        if (texto.isEmpty() || esOtro(texto)) return ""
        val lista = buscar(departamento)?.provincias
            ?: buscar(nombreCanonico(departamento))?.provincias
            ?: return texto
        return lista.firstOrNull { normalizar(it) == normalizar(texto) } ?: texto
    }

    /** "Carmen Alto, Huamanga, Ayacucho": lo que se guarda y lo que se muestra. */
    fun etiqueta(distrito: String?, provincia: String?, departamento: String?): String =
        listOf(distrito, provincia, departamento)
            .filter { !it.isNullOrBlank() && !esOtro(it) }
            .joinToString(", ")

    private fun buscar(nombre: String?): Departamento? {
        if (nombre.isNullOrBlank()) return null
        return catalogo.firstOrNull { normalizar(it.nombre) == normalizar(nombre) }
    }

    private fun normalizar(texto: String?) = OficioCatalog.normalizar(texto)
}
