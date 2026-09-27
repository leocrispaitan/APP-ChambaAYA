package com.proyecto.chambaya.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.proyecto.chambaya.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Foto ya subida a Cloudinary.
 *
 * En Firestore solo se guardan estos dos datos (más el origen en
 * `profile.profilePhotoSource`); la imagen vive en Cloudinary, nunca en la
 * base de datos.
 */
data class UploadedImage(
    val url: String,
    val publicId: String
)

/** Resultado de una subida de foto, en los mismos casos que el resto del proyecto. */
sealed class PhotoUploadResult {
    data class Success(val image: UploadedImage) : PhotoUploadResult()

    /** Cloudinary respondió con un error (preset mal configurado, formato, tamaño...). */
    data class Rejected(val message: String) : PhotoUploadResult()

    /** Sin conexión o fallo de red. */
    data class NetworkError(val cause: String?) : PhotoUploadResult()
}

/**
 * Respuesta cruda de la llamada HTTP, antes de interpretarla.
 *
 * Se separa del [PhotoUploadResult] para no mezclar "lo que dijo el servidor"
 * con "lo que le pasó al usuario": [parse] traduce [Body] a un resultado final.
 */
private sealed class HttpCall {
    /** 2xx con el cuerpo JSON de Cloudinary. */
    data class Body(val json: String) : HttpCall()

    /** Respuesta con código de error: el cuerpo puede no ser JSON. */
    data class HttpError(val code: Int) : HttpCall()

    /** No hubo respuesta: sin cobertura, WiFi, DNS, tiempo de espera... */
    data class Failure(val message: String) : HttpCall()
}

/**
 * FASE 2 — Subida de la foto de perfil a Cloudinary.
 *
 * Usa **unsigned upload preset**: el móvil hace un `POST` directo al endpoint de
 * Cloudinary sin ninguna firma, así que el "API Secret" NO viaja en la app
 * (quedaría expuesto en cualquier APK).
 *
 * Credenciales públicas (definidas en `app/build.gradle.kts`):
 * ```
 * CLOUDINARY_CLOUD_NAME     -> vtmk2tgh
 * CLOUDINARY_UPLOAD_PRESET  -> chambaya_unsigned
 * CLOUDINARY_PROFILE_FOLDER -> chambaya/perfiles
 * ```
 *
 * Carpeta final de la foto, según la estructura del proyecto en Cloudinary:
 * ```
 * chambaya/perfiles/{uid}/profile
 * ```
 *
 * El preset unsigned debe estar restringido en el dashboard de Cloudinary a
 *_folder_ `chambaya/`, imágenes y a un tamaño máximo razonable; si no, cualquiera
 * podría subir archivos con el preset.
 */
class CloudinaryUploader(
    private val cloudName: String = BuildConfig.CLOUDINARY_CLOUD_NAME,
    private val uploadPreset: String = BuildConfig.CLOUDINARY_UPLOAD_PRESET,
    private val profileFolder: String = BuildConfig.CLOUDINARY_PROFILE_FOLDER
) {

    /**
     * Sube [uri] como foto de perfil del usuario [uid].
     *
     * La imagen se reescala antes de enviarse: una foto de móvil de 4-8 MB se
     * convierte en un JPEG de como mucho 1024 px y calidad 85, que es de sobra
     * para un avatar y evita subir archivos de varios megas con datos del móvil.
     */
    suspend fun uploadProfilePhoto(context: Context, uid: String, uri: Uri): PhotoUploadResult =
        withContext(Dispatchers.IO) {
            if (uid.isBlank()) {
                return@withContext PhotoUploadResult.Rejected("No se pudo identificar al usuario.")
            }

            val bytes = leerBitmapComprimido(context, uri)
                ?: return@withContext PhotoUploadResult.Rejected("No se pudo leer la imagen seleccionada.")

            val folder = carpetaDe(uid)
            val boundary = "ChambAYA${UUID.randomUUID().toString().replace("-", "")}"
            val cuerpo = multipart(boundary, uploadPreset, folder, bytes)

            when (val http = post(boundary, cuerpo)) {
                is HttpCall.Failure -> PhotoUploadResult.NetworkError(http.message)
                is HttpCall.HttpError -> PhotoUploadResult.Rejected("Cloudinary respondió ${http.code}.")
                is HttpCall.Body -> parse(http.json)
            }
        }

    // ==================== HTTP ====================

    private fun post(boundary: String, cuerpo: ByteArray): HttpCall {
        var conexion: HttpURLConnection? = null
        return try {
            val url = URL("https://api.cloudinary.com/v1/$cloudName/image/upload")
            conexion = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "ChambAYA-Android")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            DataOutputStream(conexion.outputStream).use { it.write(cuerpo) }

            val codigo = conexion.responseCode
            val flujo: InputStream? =
                if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (codigo in 200..299) HttpCall.Body(texto) else HttpCall.HttpError(codigo)
        } catch (e: Exception) {
            HttpCall.Failure(e.message ?: e.javaClass.simpleName)
        } finally {
            conexion?.disconnect()
        }
    }

    private fun multipart(
        boundary: String,
        preset: String,
        folder: String,
        bytes: ByteArray
    ): ByteArray {
        val salida = ByteArrayOutputStream()
        DataOutputStream(salida).use { out ->
            // `upload_preset` debe ir primero: es el que habilita la subida sin firma.
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
            out.writeBytes("$preset\r\n")

            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"folder\"\r\n\r\n")
            out.writeBytes("$folder\r\n")

            out.writeBytes("--$boundary\r\n")
            out.writeBytes(
                "Content-Disposition: form-data; name=\"file\"; filename=\"profile.jpg\"\r\n"
            )
            out.writeBytes("Content-Type: image/jpeg\r\n\r\n")
            out.write(bytes)
            out.writeBytes("\r\n")

            out.writeBytes("--$boundary--\r\n")
        }
        return salida.toByteArray()
    }

    private fun parse(json: String): PhotoUploadResult {
        val root = runCatching { JSONObject(json) }.getOrNull()
            ?: return PhotoUploadResult.Rejected("Respuesta inválida de Cloudinary.")

        root.optJSONObject("error")?.let { error ->
            val message = error.optString("message").ifBlank { "Cloudinary rechazó la imagen." }
            return PhotoUploadResult.Rejected(message)
        }

        // Solo `secure_url` (https). Antes se caía a `url` (http) si faltaba, y esa
        // URL la rechaza `isOwnCloudinaryPhoto` en `firestore.rules` al guardar: el
        // usuario habría visto un `PERMISSION_DENIED` sin motivo aparente. Es mejor
        // un aviso claro aquí.
        val url = root.optString("secure_url")
        val publicId = root.optString("public_id")
        if (url.isBlank() || publicId.isBlank()) {
            return PhotoUploadResult.Rejected("Cloudinary no devolvió una URL segura de la imagen.")
        }
        return PhotoUploadResult.Success(UploadedImage(url, publicId))
    }

    // ==================== IMAGEN ====================

    /**
     * Lee [uri] y devuelve un JPEG reescalado a [MAX_LADO] px como máximo.
     *
     * `inJustDecodeBounds` + `inSampleSize` primero (para no decodificar la foto
     * entera a memoria) y el escalado real después.
     */
    private fun leerBitmapComprimido(context: Context, uri: Uri): ByteArray? = runCatching {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, limites)
        } ?: return null

        val muestreo = calcularMuestreo(limites.outWidth, limites.outHeight)
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
        val original = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opciones)
        } ?: return null

        val escalado = escalar(original, MAX_LADO)
        val salida = ByteArrayOutputStream()
        escalado.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
        if (escalado !== original) escalado.recycle()
        original.recycle()
        salida.toByteArray()
    }.getOrNull()

    /** Mayor potencia de dos que keep la imagen por debajo de [MAX_LADO]. */
    private fun calcularMuestreo(ancho: Int, alto: Int): Int {
        var muestreo = 1
        var w = ancho
        var h = alto
        while (w / 2 >= MAX_LADO && h / 2 >= MAX_LADO) {
            w /= 2
            h /= 2
            muestreo *= 2
        }
        return muestreo
    }

    private fun escalar(origen: Bitmap, ladoMaximo: Int): Bitmap {
        val mayor = maxOf(origen.width, origen.height)
        if (mayor <= ladoMaximo) return origen
        val factor = ladoMaximo.toFloat() / mayor
        val ancho = (origen.width * factor).toInt().coerceAtLeast(1)
        val alto = (origen.height * factor).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(origen, ancho, alto, true)
    }

    /** Carpeta de Cloudinary donde se guarda la foto de [uid]. */
    fun carpetaDe(uid: String): String = "$profileFolder/$uid/profile"

    /** `true` si el preset está configurado (si no, la subida no puede funcionar). */
    val estaConfigurado: Boolean
        get() = cloudName.isNotBlank() && uploadPreset.isNotBlank()

    companion object {
        private const val MAX_LADO = 1024
        private const val CALIDAD_JPEG = 85
        private const val TIMEOUT_MS = 30_000
    }
}
