package com.proyecto.chambaya.ui.foto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.proyecto.chambaya.BarraEstadoUtils
import com.proyecto.chambaya.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Pantalla de recorte de la foto de perfil.
 *
 * Se abre **después** de que el usuario elija la imagen y antes de que se
 * guarde el perfil. El recorte ocurre aquí y no en Cloudinary a propósito: la
 * subida es diferida al pulsar "Finalizar" (para no dejar imágenes huérfanas si
 * el usuario cancela), así que esta pantalla solo produce un JPEG cuadrado en
 * el caché y lo devuelve por `result`.
 *
 * Lo que se sube es exactamente el círculo que se ve aquí, sin que nada lo
 * recorte por sorpresa después: [com.proyecto.chambaya.data.remote.CloudinaryUploader]
 * solo reescala, y al recibir siempre un cuadrado no toca el encuadre.
 *
 * Cancelar **no** tira la foto elegida: quien la llama conserva la original y
 * la usa tal cual, que es lo que pasaba antes de que existiera esta pantalla.
 */
class RecortarFotoActivity : AppCompatActivity() {

    private lateinit var vista: VistaRecorte
    private lateinit var progreso: ProgressBar

    /** Bitmap de trabajo, ya enderezado por EXIF. */
    private var origen: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarraEstadoUtils.aplicarColor(this, COLOR_FONDO)

        setContentView(R.layout.activity_recortar_foto)

        vista = findViewById(R.id.vistaRecorte)
        progreso = findViewById(R.id.progressRecorte)
        val btnCerrar = findViewById<View>(R.id.btnCerrarRecorte)
        val btnRotar = findViewById<MaterialButton>(R.id.btnRotarRecorte)
        val btnRestablecer = findViewById<MaterialButton>(R.id.btnRestablecerRecorte)
        val btnAplicar = findViewById<MaterialButton>(R.id.btnAplicarRecorte)

        btnCerrar.setOnClickListener { finish() }
        btnRotar.setOnClickListener { rotar() }
        btnRestablecer.setOnClickListener { vista.reiniciar() }
        btnAplicar.setOnClickListener { aplicar() }

        cargar()
    }

    // ═══════════════════════════════════════════════════════════════
    //  CARGA
    // ═══════════════════════════════════════════════════════════════

    private fun cargar() {
        // `IntentCompat` en vez de `intent.getParcelableExtra`: el método de
        // `Intent` quedó obsoleto en API 33 y en pantallas grandes se puede
        //_classCastException_ al leer un `Uri` si el extra se serializó mal.
        val uri = IntentCompat.getParcelableExtra(intent, EXTRA_ORIGEN, Uri::class.java)
        if (uri == null) {
            avisarYCerrar(R.string.recorte_foto_error_lectura)
            return
        }

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { leer(uri) }
            if (bitmap == null) {
                avisarYCerrar(R.string.recorte_foto_error_lectura)
                return@launch
            }
            origen = bitmap
            progreso.visibility = View.GONE
            vista.establecerImagen(bitmap)
        }
    }

    /**
     * Lee la imagen ya enderezada y acotada a [LADO_ORIGEN_MAXIMO].
     *
     * Se decodea escalado de entrada (`setTargetSize` / `inSampleSize`) porque
     * una foto de móvil de 12 MP son ~48 MB en ARGB_8888, y para recortar solo
     * hace falta el lado menor: al zoom mínimo el recorte mide justamente el
     * lado **menor** de la imagen, así que con 2048 de lado mayor casi todas las
     * fotos dan un recorte de sobra para los 1024 px de salida.
     *
     * La orientación EXIF se aplica siempre. Sin esto, una foto tomada en
     * horizontal se recorta de lado, que es justo el tipo de ERROR que el
     * usuario no puede corregir a mano. [ImageDecoder] (API 28+) la aplica solo;
     * por debajo hay que leerla y girar a mano.
     */
    private fun leer(uri: Uri): Bitmap? {
        val crudo = try {
            decodificar(uri)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "No hay memoria para decodificar la foto", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo decodificar la foto: ${e.message}", e)
            null
        } ?: return null

        val acotada = escalar(crudo, LADO_ORIGEN_MAXIMO)
        if (acotada !== crudo) crudo.recycle()
        return enderezar(acotada, uri)
    }

    private fun decodificar(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodificarConImageDecoder(uri)
        } else {
            decodificarConBitmapFactory(uri)
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodificarConImageDecoder(uri: Uri): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { d, info, _ ->
            // Software obligatorio: el bitmap final se dibuja en un `Canvas` de
            // software y un bitmap de hardware no se puede pintar ahí.
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val ancho = info.size.width
            val alto = info.size.height
            val mayor = maxOf(ancho, alto)
            if (mayor > LADO_ORIGEN_MAXIMO) {
                val factor = LADO_ORIGEN_MAXIMO.toFloat() / mayor
                d.setTargetSize(
                    (ancho * factor).toInt().coerceAtLeast(1),
                    (alto * factor).toInt().coerceAtLeast(1)
                )
            }
        }
    }.onFailure { Log.e(TAG, "ImageDecoder falló: ${it.message}", it) }.getOrNull()

    private fun decodificarConBitmapFactory(uri: Uri): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, limites)
        }
        if (limites.outWidth <= 0 || limites.outHeight <= 0) return null

        val opciones = BitmapFactory.Options().apply {
            inSampleSize = muestreo(limites.outWidth, limites.outHeight)
        }
        return contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opciones)
        }
    }

    /** Mayor potencia de dos que deja el lado mayor por debajo del tope. */
    private fun muestreo(ancho: Int, alto: Int): Int {
        var muestreo = 1
        var w = ancho
        var h = alto
        while (w / 2 >= LADO_ORIGEN_MAXIMO && h / 2 >= LADO_ORIGEN_MAXIMO) {
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
        return Bitmap.createScaledBitmap(
            origen,
            (origen.width * factor).toInt().coerceAtLeast(1),
            (origen.height * factor).toInt().coerceAtLeast(1),
            true
        )
    }

    /**
     * Gira la imagen según su EXIF. En API 28+ ya viene enderezada.
     *
     * Usa `android.media.ExifInterface` y no el de AndroidX a propósito: este
     * camino solo corre en API 24-27, donde el de la plataforma lee bien el
     * tag de orientación, y añadir una dependencia solo para ese caso
     *_oldeno兼容_ no compensa. Lint lo avisa por estar obsoleto; el
     * comportamiento es el que hace falta.
     */
    @Suppress("DEPRECATION")
    private fun enderezar(origen: Bitmap, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return origen

        val grados = try {
            contentResolver.openInputStream(uri)?.use { flujo ->
                when (ExifInterface(flujo).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo leer la orientación EXIF: ${e.message}")
            0f
        }

        if (grados == 0f) return origen
        val girada = Bitmap.createBitmap(origen, 0, 0, origen.width, origen.height, Matrix().apply {
            postRotate(grados)
        }, true)
        if (girada !== origen) origen.recycle()
        return girada
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACCIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Gira 90° a la derecha.
     *
     * El giro se aplica al **bitmap de origen**, no a la vista: así la
     * transformación de [VistaRecorte] sigue siendo escala + traslación, que es
     * lo que hace que su [VistaRecorte.exportar] sea una fórmula cerrada y no
     * una inversión de matriz. Girar por pasos de 90° tampoco pierde calidad:
     * es una transposición, no un reescalado.
     */
    private fun rotar() {
        val actual = origen ?: return
        try {
            val girada = Bitmap.createBitmap(
                actual, 0, 0, actual.width, actual.height,
                Matrix().apply { postRotate(90f) }, true
            )
            if (girada === actual) return
            origen = girada
            vista.establecerImagen(girada)
            actual.recycle()
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "No hay memoria para girar la foto", e)
            Toast.makeText(this, R.string.recorte_foto_sin_memoria, Toast.LENGTH_SHORT).show()
        }
    }

    private fun aplicar() {
        if (origen == null) return

        // Nunca más píxeles de los que la imagen puede dar: subir 1024 px a
        // partir de un recorte de 400 solo agranda el JPEG, no el detalle.
        val lado = min(LADO_SALIDA, vista.ladoUtilDeRecorte().coerceAtLeast(1))

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.Default) { vista.exportar(lado) }
            if (bitmap == null) {
                avisarYCerrar(R.string.recorte_foto_error_lectura)
                return@launch
            }

            val ruta = withContext(Dispatchers.IO) { escribir(bitmap) }
            bitmap.recycle()

            if (ruta == null) {
                avisarYCerrar(R.string.recorte_foto_error_escritura)
                return@launch
            }

            setResult(RESULT_OK, intent.putExtra(EXTRA_RUTA_RECORTE, ruta))
            finish()
        }
    }

    /**
     * Escribe el recorte como JPEG en el caché y devuelve su ruta.
     *
     * Va al caché y no a la galería: es un archivo intermedio de esta pantalla,
     * y el sistema lo borra solo cuando le hace falta espacio. Los recortes
     * anteriores se limpian aquí para que no se acumulen en una sesión larga.
     */
    private fun escribir(bitmap: Bitmap): String? = try {
        limpiarRecortesPrevios()
        val archivo = File(cacheDir, "recorte_avatar_${System.currentTimeMillis()}.jpg")
        val ok = FileOutputStream(archivo).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, it)
        }
        if (ok) archivo.absolutePath else null
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo escribir el recorte: ${e.message}", e)
        null
    }

    private fun limpiarRecortesPrevios() {
        cacheDir.listFiles { f -> f.name.startsWith(PREFIJO_RECORTE) }?.forEach {
            it.delete()
        }
    }

    private fun avisarYCerrar(mensaje: Int) {
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        origen?.recycle()
        origen = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RecortarFoto"

        private const val COLOR_FONDO = 0xFF0B0B0F.toInt()

        /** Lado mayor al que se decodea la foto para recortar. */
        private const val LADO_ORIGEN_MAXIMO = 2048

        /**
         * Lado del JPEG de salida.
         *
         * Es el mismo tope que usa [com.proyecto.chambaya.data.remote.CloudinaryUploader],
         * así que la subida no vuelve a tocar la imagen: entra cuadrada de 1024
         * y sale igual.
         */
        private const val LADO_SALIDA = 1024

        private const val CALIDAD_JPEG = 92
        private const val PREFIJO_RECORTE = "recorte_avatar_"

        /** `Uri` de la imagen elegida, sin recortar. */
        const val EXTRA_ORIGEN = "com.proyecto.chambaya.ui.foto.EXTRA_ORIGEN"

        /** Ruta absoluta del JPEG recortado, en el `result`. */
        const val EXTRA_RUTA_RECORTE = "com.proyecto.chambaya.ui.foto.EXTRA_RUTA_RECORTE"
    }
}
