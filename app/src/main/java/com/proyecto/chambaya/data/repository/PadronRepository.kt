package com.proyecto.chambaya.data.repository

import com.proyecto.chambaya.data.model.BirthDates
import com.proyecto.chambaya.data.model.Genders
import com.proyecto.chambaya.data.model.PeruLocations
import com.proyecto.chambaya.data.model.UserProfile
import com.proyecto.chambaya.data.model.ValidatedIdentity
import com.proyecto.chambaya.data.remote.IdentityValidationResult
import com.proyecto.chambaya.data.remote.IdentityValidationService
import java.util.concurrent.ConcurrentHashMap

/**
 * Datos que el padrón oficial (RENIEC / SUNAT) devuelve y que `users/{uid}` todavía
 * no tiene guardados.
 *
 * Se modelan aparte de [UserProfile] porque NO se escriben en Firestore: sirven
 * para rellenar el formulario de "Editar perfil" antes de que el usuario pulse
 * "Finalizar". Si luego los cambia, gana lo que él escriba; si los deja tal cual,
 * se guardan junto con el resto del perfil.
 */
data class DatosPadron(
    val documentType: String = "",
    val documentNumber: String = "",
    val fullName: String = "",
    /** `dd/MM/aaaa`. Vacío si el padrón no lo devuelve. */
    val birthDate: String = "",
    /** Uno de [Genders]. Vacío si el padrón no lo devuelve. */
    val gender: String = "",
    val department: String = "",
    val province: String = "",
    val district: String = ""
) {
    /** `true` si el padrón trajo al menos un dato aprovechable. */
    val tieneAlgo: Boolean
        get() = birthDate.isNotEmpty() || gender.isNotEmpty() ||
            department.isNotEmpty() || province.isNotEmpty() || district.isNotEmpty()

    companion object {
        val VACIO = DatosPadron()
    }
}

/**
 * FASE 2 — Rellena los huecos del perfil con lo que ya sabe el padrón oficial.
 *
 * Por qué existe: la API de RENIEC/SUNAT que valida el documento en el registro
 * devuelve mucho más que el nombre (cumpleaños, sexo, departamento, provincia,
 * distrito y dirección), pero antes solo se leía el nombre. El usuario tenía que
 * teclear a mano en "Editar perfil" datos que el servidor ya le había dado.
 *
 * Reglas de diseño:
 *  - **No escribe nada en Firestore.** Solo devuelve los datos para pintar el
 *    formulario: que el guardado sea una decisión del usuario evita pisar
 *    información cuando solo estaba de paso mirando.
 *  - **Nunca pisa lo que el usuario ya escribió.** [datosQueFaltan] compara contra
 *    el documento y se queda solo con los huecos reales.
 *  - **Es un extra, nunca un bloqueo.** Si no hay red, el servicio cae o el
 *    documento ya no existe en el padrón, se avisa con un log y el formulario se
 *    queda como está: los campos simplemente se ven pendientes.
 *  - **Una consulta por sesión**, cacheada por `uid`, para no pegarle a la API
 *    cada vez que se abre el asistente.
 */
class PadronRepository(
    private val service: IdentityValidationService = IdentityValidationService()
) {

    private val cache = ConcurrentHashMap<String, DatosPadron>()

    /**
     * Datos del padrón de [perfil], reducidos a lo que el documento aún no tiene.
     *
     * Recibe el perfil ya leído en lugar de leerlo otra vez: quien lo llama (el
     * asistente de edición) lo tiene a mano y así se ahorra una ida a Firestore.
     *
     * @param forzar vuelve a consultar la API aunque haya resultado en caché.
     * @return `Result` con [DatosPadron.VACIO] —que no es un error— cuando el
     *   padrón no aporta nada nuevo o cuando no se pudo consultar.
     */
    suspend fun datosQueFaltan(
        perfil: UserProfile,
        forzar: Boolean = false
    ): Result<DatosPadron> {
        val uid = perfil.uid
        if (uid.isBlank()) return Result.success(DatosPadron.VACIO)
        if (!forzar) cache[uid]?.let { return Result.success(it) }

        val identidad = perfil.identity
        val completo = when (
            val resultado = service.consultarPadron(identidad.documentType, identidad.documentNumber)
        ) {
            is IdentityValidationResult.Success -> resultado.identity.aDatosPadron()
            // El tipo de documento no es DNI/RUC, o el servicio no responde: no hay
            // nada que traer y el formulario se queda como está.
            is IdentityValidationResult.Rejected,
            is IdentityValidationResult.ServiceError,
            is IdentityValidationResult.NetworkError -> DatosPadron.VACIO
        }

        val faltan = soloHuecos(perfil, completo)
        cache[uid] = faltan
        return Result.success(faltan)
    }

    /** Lo que ya está en caché para [uid], sin tocar la red. */
    fun enCache(uid: String): DatosPadron? = cache[uid]

    /** Olvida la caché de [uid] (o toda la caché si se pasa `null`). */
    fun limpiar(uid: String? = null) {
        if (uid == null) cache.clear() else cache.remove(uid)
    }

    /**
     * Se queda solo con los campos que el documento tiene vacíos.
     *
     * El nombre oficial nunca se propaga: `profile.fullName` lo escribió el
     * registro desde el padrón y el usuario puede haberlo cambiado; aquí solo
     * importan los campos que el registro dejó sin rellenar.
     */
    private fun soloHuecos(perfil: UserProfile, padron: DatosPadron): DatosPadron {
        val bloque = perfil.profile
        return padron.copy(
            birthDate = padron.birthDate.takeIf { bloque.birthDate.isBlank() }.orEmpty(),
            gender = padron.gender.takeIf { bloque.gender.isBlank() }.orEmpty(),
            department = padron.department.takeIf { bloque.department.isBlank() }.orEmpty(),
            province = padron.province.takeIf { bloque.province.isBlank() }.orEmpty(),
            district = padron.district.takeIf { bloque.district.isBlank() }.orEmpty()
        )
    }
}

/**
 * Traduce la respuesta del padrón al modelo del wizard, limpiando lo que no sirve.
 *
 * - La fecha pasa por [BirthDates.soloSiValida]: RENIEC a veces devuelve vacío o un
 *   formato raro, y un `birthDate` ilegible rompería el `DatePicker` del paso 2.
 * - El género solo se acepta si es uno de [Genders]; si el padrón dice algo que no
 *   se reconoce, el campo se deja pendiente en vez de adivinar.
 * - La ubicación se pasa por el catálogo de [PeruLocations] porque el padrón la
 *   devuelve en mayúsculas ("LIMA") y la app la muestra con su propia grafía.
 */
private fun ValidatedIdentity.aDatosPadron(): DatosPadron = DatosPadron(
    documentType = documentType,
    documentNumber = documentNumber,
    fullName = fullName,
    birthDate = BirthDates.soloSiValida(birthDate),
    gender = gender.takeIf { it in Genders.ALL }.orEmpty(),
    department = PeruLocations.nombreCanonico(department),
    province = PeruLocations.provinciaCanonica(department, province),
    district = district.trim()
)
