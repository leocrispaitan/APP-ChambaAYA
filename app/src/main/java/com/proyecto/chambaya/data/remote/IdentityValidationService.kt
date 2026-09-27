package com.proyecto.chambaya.data.remote

import com.proyecto.chambaya.data.model.Genders
import com.proyecto.chambaya.data.model.IdentityDocumentTypes
import com.proyecto.chambaya.data.model.IdentityNameParser
import com.proyecto.chambaya.data.model.IdentitySources
import com.proyecto.chambaya.data.model.ValidatedIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Resultado de una consulta de identidad.
 * Se mantienen los mismos casos que manejava `RegistroActivity` para no cambiar
 * el comportamiento actual del registro.
 */
sealed class IdentityValidationResult {
    /** Identidad encontrada y válida. */
    data class Success(val identity: ValidatedIdentity) : IdentityValidationResult()

    /** El padrón respondió correctamente pero el documento no es válido. */
    data class Rejected(val message: String) : IdentityValidationResult()

    /** Error del servicio (código HTTP distinto de 200). */
    data class ServiceError(val httpCode: Int) : IdentityValidationResult()

    /** Sin conexión o excepción de red. */
    data class NetworkError(val cause: String?) : IdentityValidationResult()
}

/**
 * FASE 1 — Validación oficial de identidad.
 *
 *  - Trabajador  : DNI (8 dígitos)  -> RENIEC
 *  - Contratante : DNI (8 dígitos)  -> RENIEC  (PERSONA)
 *  - Contratante : RUC (11 dígitos) -> SUNAT   (EMPRESA / NEGOCIO)
 *
 * Este servicio NO guarda nada en Firestore: solo valida y devuelve el
 * nombre oficial, que luego `RegistrationRepository` persiste dentro de
 * `users/{uid}.identity` y `users/{uid}.profile`.
 */
class IdentityValidationService(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val apiToken: String = DEFAULT_API_TOKEN
) {

    suspend fun validateDni(dni: String): IdentityValidationResult = withContext(Dispatchers.IO) {
        val cleanDni = dni.filter { it.isDigit() }
        if (cleanDni.length != DNI_LENGTH) {
            return@withContext IdentityValidationResult.Rejected("El DNI debe tener 8 dígitos.")
        }

        when (val http = get("$baseUrl/dni/$cleanDni")) {
            is HttpCall.Failure -> IdentityValidationResult.NetworkError(http.message)
            is HttpCall.HttpError -> IdentityValidationResult.ServiceError(http.code)
            is HttpCall.Body -> parseDniResponse(http.json, cleanDni)
        }
    }

    suspend fun validateRuc(ruc: String): IdentityValidationResult = withContext(Dispatchers.IO) {
        val cleanRuc = ruc.filter { it.isDigit() }
        if (cleanRuc.length != RUC_LENGTH) {
            return@withContext IdentityValidationResult.Rejected("El RUC debe tener 11 dígitos.")
        }

        when (val http = get("$baseUrl/ruc/$cleanRuc")) {
            is HttpCall.Failure -> IdentityValidationResult.NetworkError(http.message)
            is HttpCall.HttpError -> IdentityValidationResult.ServiceError(http.code)
            is HttpCall.Body -> parseRucResponse(http.json, cleanRuc)
        }
    }

    /**
     * Vuelve a consultar el padrón de un documento **ya validado** para completar
     * los campos que el registro no se quedó: cumpleaños, género y ubicación
     * oficial.
     *
     * No es una validación nueva (para eso están [validateDni] y [validateRuc]):
     * el documento ya pasó por RENIEC/SUNAT en el registro, así que aquí solo se
     * recupera lo que la API devolvió y se dejó en el camino. Si el documento ya
     * no existe en el padrón o el servicio no responde, se devuelve el fallo tal
     * cual y quien llama lo trata como información: el campo queda vacío y el
     * usuario lo rellena a mano.
     */
    suspend fun consultarPadron(
        documentType: String,
        documentNumber: String
    ): IdentityValidationResult = when (documentType) {
        IdentityDocumentTypes.DNI -> validateDni(documentNumber)
        IdentityDocumentTypes.RUC -> validateRuc(documentNumber)
        else -> IdentityValidationResult.Rejected("El tipo de documento no se puede consultar.")
    }

    // ==================== PARSEO ====================

    private fun parseDniResponse(root: JSONObject, dni: String): IdentityValidationResult {
        if (!root.optBoolean("success", false)) {
            return IdentityValidationResult.Rejected(
                root.optString("message", "DNI no encontrado en RENIEC")
            )
        }

        val data = root.optJSONObject("data") ?: JSONObject()
        val rawName = data.optString("nombre_completo", data.optString("name", "")).trim()
        if (rawName.isEmpty()) {
            return IdentityValidationResult.Rejected("El DNI no tiene un nombre registrado.")
        }

        val departamento = data.optString("departamento", "").trim()
        val provincia = data.optString("provincia", "").trim()
        val location = when {
            departamento.isNotEmpty() && provincia.isNotEmpty() -> "$departamento, $provincia"
            departamento.isNotEmpty() -> departamento
            else -> "Perú"
        }

        val split = IdentityNameParser.parseFirstAndLastName(rawName)

        return IdentityValidationResult.Success(
            ValidatedIdentity(
                documentType = IdentityDocumentTypes.DNI,
                documentNumber = dni,
                fullName = IdentityNameParser.toDisplayCase(rawName),
                firstName = IdentityNameParser.toDisplayCase(split.firstName),
                lastName = IdentityNameParser.toDisplayCase(split.lastName),
                source = IdentitySources.RENIEC,
                locationLabel = location,
                // La API de RENIEC ya devuelve el cumpleaños y el sexo: antes se
                // ignoraban y el usuario tenía que escribirlos a mano en
                // "Editar perfil".
                birthDate = normalizarFecha(
                    data.optString("fecha_nacimiento", data.optString("fechaNacimiento", ""))
                ),
                gender = normalizarSexo(data.optString("sexo", data.optString("gender", ""))),
                department = departamento,
                province = provincia,
                district = data.optString("distrito", "").trim(),
                address = data.optString(
                    "direccion_completa",
                    data.optString("direccion", "")
                ).trim()
            )
        )
    }

    private fun parseRucResponse(root: JSONObject, ruc: String): IdentityValidationResult {
        if (!root.optBoolean("success", false)) {
            return IdentityValidationResult.Rejected(
                root.optString("message", "RUC no encontrado en SUNAT")
            )
        }

        val data = root.optJSONObject("data") ?: JSONObject()
        val razonSocial = data.optString(
            "nombre_o_razon_social",
            data.optString("name", "Razón social no disponible")
        ).trim()
        val estado = data.optString("estado", "").trim().uppercase()
        val condicion = data.optString("condicion", "").trim().uppercase()

        if (estado != "ACTIVO" || condicion != "HABIDO") {
            return IdentityValidationResult.Rejected(
                "RUC no válido: Estado=$estado, Condición=$condicion. " +
                    "Solo se aceptan empresas ACTIVAS y HABIDAS."
            )
        }

        val departamento = data.optString("departamento", "").trim()
        val provincia = data.optString("provincia", "").trim()
        val distrito = data.optString("distrito", "").trim()

        return IdentityValidationResult.Success(
            ValidatedIdentity(
                documentType = IdentityDocumentTypes.RUC,
                documentNumber = ruc,
                fullName = IdentityNameParser.toDisplayCase(razonSocial),
                firstName = "",
                lastName = "",
                legalName = razonSocial,
                source = IdentitySources.SUNAT,
                statusLabel = "$condicion / $estado",
                locationLabel = listOf(distrito, provincia, departamento)
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")
                    .ifEmpty { "Perú" },
                // SUNAT no devuelve cumpleaños ni género: son datos de persona, y una
                // razón social no los tiene.
                department = departamento,
                province = provincia,
                district = distrito,
                address = data.optString(
                    "direccion_completa",
                    data.optString("address", data.optString("direccion", ""))
                ).trim()
            )
        )
    }

    // ==================== NORMALIZACIÓN ====================

    /**
     * Deja la fecha del padrón en el único formato que usa la app: `dd/MM/aaaa`.
     *
     * RENIEC devuelve `30/11/2000`, pero no se fía uno de un servicio externo: si
     * llegara en ISO (`2000-11-30`) o con guiones, se reordena igual. Un valor
     * que no se puede entender se devuelve vacío, nunca una fecha inventada.
     */
    private fun normalizarFecha(valor: String): String {
        val limpio = valor.trim()
        if (limpio.isEmpty()) return ""

        val partes = limpio.split('/', '-', '.').map { it.trim() }
        return when {
            // dd/MM/aaaa
            partes.size == 3 && partes[0].length <= 2 -> {
                val dia = partes[0].toIntOrNull()
                val mes = partes[1].toIntOrNull()
                val anio = partes[2].toIntOrNull()
                if (dia == null || mes == null || anio == null) ""
                else String.format(Locale.US, "%02d/%02d/%04d", dia, mes, anio)
            }
            // aaaa-MM-dd
            partes.size == 3 && partes[0].length == 4 -> {
                val anio = partes[0].toIntOrNull()
                val mes = partes[1].toIntOrNull()
                val dia = partes[2].toIntOrNull()
                if (dia == null || mes == null || anio == null) ""
                else String.format(Locale.US, "%02d/%02d/%04d", dia, mes, anio)
            }
            else -> ""
        }
    }

    /**
     * Traduce el sexo del padrón a los valores de `profile.gender`.
     *
     * RENIEC devuelve `VARON` / `MUJER`. Cualquier otra cosa (o vacío) se devuelve
     * como cadena vacía: es un campo opcional y prefiero que quede pendiente a que
     * el usuario elija, antes que adivinar.
     */
    private fun normalizarSexo(valor: String): String =
        when (valor.trim().uppercase(Locale.US)) {
            "VARON", "MASCULINO", "M" -> Genders.MASCULINO
            "MUJER", "FEMENINO", "F" -> Genders.FEMENINO
            else -> ""
        }

    // ==================== HTTP ====================

    private sealed class HttpCall {
        data class Body(val json: JSONObject) : HttpCall()
        data class HttpError(val code: Int) : HttpCall()
        data class Failure(val message: String?) : HttpCall()
    }

    private fun get(urlString: String): HttpCall {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $apiToken")
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                HttpCall.HttpError(code)
            } else {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                HttpCall.Body(JSONObject(body))
            }
        } catch (e: Exception) {
            HttpCall.Failure(e.message)
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        private const val DEFAULT_BASE_URL = "https://apis.aqpfact.pe/api"
        private const val DEFAULT_API_TOKEN = "8204|89676i7wDZfoYBDJ70rZAQOLx9YgbDObuz0ui3Rw"
        private const val TIMEOUT_MS = 10_000
        private const val DNI_LENGTH = 8
        private const val RUC_LENGTH = 11
    }
}
