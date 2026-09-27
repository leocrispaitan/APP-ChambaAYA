package com.proyecto.chambaya.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
 * CLOUDINARY_CLOUD_NAME           -> vtmk2tgh
 * CLOUDINARY_UPLOAD_PRESET        -> chambaya_preset
 * CLOUDINARY_FOLDER_ROOT          -> chambaya
 * CLOUDINARY_FOLDER_PERFILES      -> fotos-perfil
 * CLOUDINARY_FOLDER_LUGARES       -> fotos-lugares
 * CLOUDINARY_FOLDER_PUBLICACIONES -> fotos-publicaciones
 * ```
 *
 * Estructura de modulos dentro de Cloudinary:
 * ```
 * chambaya/
 *   oficios/              <- imagenes de api_oficios.json (se suben fuera de la app)
 *   fotos-perfil/         <- avatares: {uid}/profile
 *   fotos-lugares/
 *   fotos-publicaciones/
 * ```
 *
 * El preset unsigned debe estar restringido en el dashboard de Cloudinary a
 *_folder_ `chambaya/`, imágenes y a un tamaño máximo razonable; si no, cualquiera
 * podría subir archivos con el preset.
 */
class CloudinaryUploader(
    private val cloudName: String = BuildConfig.CLOUDINARY_CLOUD_NAME,
    private val uploadPreset: String = BuildConfig.CLOUDINARY_UPLOAD_PRESET
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
                is HttpCall.HttpError -> PhotoUploadResult.Rejected(explicarError(http.code, cloudName, uploadPreset))
                is HttpCall.Body -> parse(http.json)
            }
        }

    /**
     * Traduce un código HTTP de Cloudinary a algo accionable.
     *
     * El 404 merece párrafo propio porque Cloudinary devuelve una **página HTML**
     * de "Page not found" en vez de su JSON de error habitual, lo que vuelve el
     * fallo casi ilegible. Y hay que saber lo que NO era, porque se perdió mucho
     * tiempo con ello: cuando se usaba la ruta `/v1/` en vez de `/v1_1/`,
     * Cloudinary devolvía ese mismo 404 para CUALQUIER cloud name, válido o no.
     * Parecía que la cuenta estaba caída o mal escrita y se llegaron a crear
     * cuentas nuevas para "arreglarlo"; el arreglo era una sola letra en la URL.
     * Si vuelve a salir un 404, la primera cosa a mirar es la ruta del endpoint
     * en [post], no las credenciales.
     */
    private fun explicarError(code: Int, cloud: String, preset: String): String = when (code) {
        404 ->
            "Cloudinary devolvió 404 al subir. Lo más probable es la ruta del " +
                "endpoint en CloudinaryUploader.post (debe ser /v1_1/, no /v1/); " +
                "si es correcta, revisa el cloud \"$cloud\" en el panel de Cloudinary."
        401, 403 ->
            "Cloudinary rechazó el preset \"$preset\" (código $code). Revisa que el " +
                "preset exista y siga en modo Unsigned."
        413 ->
            "La imagen es demasiado grande para el preset \"$preset\" (413)."
        else -> "Cloudinary respondió $code."
    }

    // ==================== HTTP ====================

    private fun post(boundary: String, cuerpo: ByteArray): HttpCall {
        var conexion: HttpURLConnection? = null
        return try {
            // OJO con la versión de la API: es `v1_1`, NO `v1`.
            //
            // Con `/v1/` Cloudinary no responde con su JSON de error habitual, sino
            // con una página HTML de "Page not found" (404) para CUALQUIER cloud
            // name, válido o no. Por eso parecía un cloud mal escrito cuando en
            // realidad la ruta no existe, y por eso el mismo 404 salía con dos
            // cuentas distintas.
            val url = URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
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
     * El reescalado se hace al decodificar ([ImageDecoder] con `setTargetSize`, o
     * `inSampleSize` + [escalar] como reserva), así que la foto original no se
     * carga nunca entera en memoria.
     */
    private fun leerBitmapComprimido(context: Context, uri: Uri): ByteArray? {
        val original = decodificar(context, uri) ?: return null

        val escalado = escalar(original, MAX_LADO)
        val salida = ByteArrayOutputStream()
        val ok = escalado.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
        if (escalado !== original) escalado.recycle()
        original.recycle()

        if (!ok) {
            Log.w(TAG, "El bitmap no se pudo comprimir a JPEG")
            return null
        }
        return salida.toByteArray()
    }

    /**
     * Decodifica la imagen de [uri], ya escalada a [MAX_LADO] px como máximo.
     *
     * Se prefiere [ImageDecoder] (API 28+) porque entiende formatos que
     * `BitmapFactory` no (HEIF/HEIC) y aplica solo la orientación EXIF, que si no
     * sube la foto de lado. Como devuelve `ALLOCATOR_HARDWARE` por defecto y un
     * bitmap de hardware no se puede comprimir, se fuerza el de software.
     *
     * [BitmapFactory] queda como reserva para API 24-27.
     */
    private fun decodificar(context: Context, uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodificarConImageDecoder(context, uri) ?: decodificarConBitmapFactory(context, uri)
        } else {
            decodificarConBitmapFactory(context, uri)
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodificarConImageDecoder(context: Context, uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val ancho = info.size.width
            val alto = info.size.height
            val mayor = maxOf(ancho, alto)
            if (mayor > MAX_LADO) {
                val factor = MAX_LADO.toFloat() / mayor
                d.setTargetSize(
                    (ancho * factor).toInt().coerceAtLeast(1),
                    (alto * factor).toInt().coerceAtLeast(1)
                )
            }
        }
    }.onFailure { Log.w(TAG, "ImageDecoder no pudo leer la imagen: ${it.message}", it) }.getOrNull()

    /**
     * Reserva de [decodificar] para API 24-27.
     *
     * OJO con el primer `decodeStream`: con `inJustDecodeBounds = true` devuelve
     * SIEMPRE `null` por diseño (solo rellena `outWidth`/`outHeight`). Encadenar su
     * resultado con un `?:` hace que la función devuelva `null` para cualquier
     * imagen, que es exactamente lo que pasaba antes: ninguna foto se podía leer y
     * el error que veía el usuario era "No se pudo leer la imagen seleccionada",
     * sin llegar nunca a hacer la subida.
     */
    private fun decodificarConBitmapFactory(context: Context, uri: Uri): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val flujos = context.contentResolver.openInputStream(uri)
        if (flujos == null) {
            Log.w(TAG, "No se pudo abrir la imagen: $uri")
            return null
        }
        flujos.use { BitmapFactory.decodeStream(it, null, limites) }

        if (limites.outWidth <= 0 || limites.outHeight <= 0) {
            Log.w(TAG, "Formato de imagen no reconocido: $uri")
            return null
        }

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = calcularMuestreo(limites.outWidth, limites.outHeight)
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opciones)
        }
        if (bitmap == null) Log.w(TAG, "BitmapFactory devolvio null para $uri")
        return bitmap
    }

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

    /**
     * Módulo de imágenes dentro de la raíz `chambaya/` de Cloudinary.
     *
     * Cada tipo de imagen tiene el suyo para que el dashboard no mezcle avatares
     * con mapas o con anuncios, y para que el preset pueda restringirse por
     * carpeta.
     */
    enum class ModuloImagen(val subcarpeta: String) {
        /** Avatares. Lleva una carpeta por `uid` para que nadie use la foto de otro. */
        PERFILES(BuildConfig.CLOUDINARY_FOLDER_PERFILES),

        /** Fotos de los lugares del mapa. */
        LUGARES(BuildConfig.CLOUDINARY_FOLDER_LUGARES),

        /** Fotos de anuncios y publicaciones. */
        PUBLICACIONES(BuildConfig.CLOUDINARY_FOLDER_PUBLICACIONES);

        /** Carpeta raíz del módulo, sin barras iniciales ni finales: `chambaya/fotos-perfil`. */
        val raiz: String
            get() = "${BuildConfig.CLOUDINARY_FOLDER_ROOT}/$subcarpeta"
    }

    /** Carpeta de Cloudinary donde se guarda la foto de [uid]. */
    fun carpetaDe(uid: String): String = carpetaDePerfil(uid)

    /** `true` si el preset está configurado (si no, la subida no puede funcionar). */
    val estaConfigurado: Boolean
        get() = cloudName.isNotBlank() && uploadPreset.isNotBlank()

    companion object {
        private const val TAG = "CloudinaryUploader"

        private const val MAX_LADO = 1024
        private const val CALIDAD_JPEG = 85
        private const val TIMEOUT_MS = 30_000

        /**
         * Carpeta de la foto de perfil de [uid]: `chambaya/fotos-perfil/{uid}/profile`.
         *
         * Vive en el companion para que [ProfileRepository.saveProfile] escriba el
         * mismo `profile.profilePhotoPath` que usa la subida, en vez de repetir el
         * literal: si las dos rutas se desincronizan, el campo documenta una
         * carpeta que no existe. Y esa carpeta es la que comprueba
         * `isOwnCloudinaryPhoto` en `firestore.rules`.
         */
        fun carpetaDePerfil(uid: String): String =
            "${ModuloImagen.PERFILES.raiz}/$uid/profile"
    }
}
