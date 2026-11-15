package com.diasdefuria.datos

import android.content.Context
import androidx.annotation.StringRes
import com.diasdefuria.R

/**
 * Galería de arte, una carpeta por juego: app/src/main/assets/arte/<id del juego>/
 * (01.jpg, 02.jpg…). Aparecen solas en Extras, ordenadas por nombre de archivo.
 */
data class ImagenArte(val juego: Juego, val archivo: String) {
    /** Carpeta dentro de assets/arte/ (la usan también los marcos laterales). */
    val carpeta: String get() = juego.id

    /** Ruta que entiende Coil para leer desde assets. */
    val uri: String get() = "file:///android_asset/arte/$carpeta/$archivo"
}

private val EXTENSIONES = setOf("jpg", "jpeg", "png", "webp")

/** Imágenes de [juego], o de todos los juegos (en el orden de la ruleta) si es null. */
fun cargarArte(context: Context, juego: Juego? = null): List<ImagenArte> =
    (juego?.let { listOf(it) } ?: Juego.entries).flatMap { j ->
        (runCatching { context.assets.list("arte/${j.id}") }.getOrNull() ?: emptyArray())
            .filter { it.substringAfterLast('.').lowercase() in EXTENSIONES }
            .sorted()
            .map { ImagenArte(j, it) }
    }

/** Un texto de assets/historia/ (se lee en «Extras»). */
data class Guia(val archivo: String, val carpeta: String = "historia") {
    val esPdf: Boolean get() = archivo.endsWith(".pdf", ignoreCase = true)

    /** Juego al que pertenece: el nombre del archivo empieza por su id («ffight.pdf», «ffight_guia.txt»). */
    val juego: Juego? get() = archivo.substringBeforeLast('.').lowercase().let { base ->
        Juego.entries.firstOrNull { base == it.id || base.startsWith(it.id + "_") }
    }

    /** «ffight.pdf» → «Final Fight»; «ffight_guia_haggar.txt» → «Final Fight · guia haggar». */
    val titulo: String get() {
        val base = archivo.substringBeforeLast('.')
        val j = juego ?: return base.replace('_', ' ').replaceFirstChar { it.uppercase() }
        val resto = base.drop(j.id.length).trimStart('_').replace('_', ' ')
        return if (resto.isBlank()) j.nombre else "${j.nombre} · $resto"
    }

    /** Mismo orden que la lista de juegos. */
    val orden: Int get() = juego?.ordinal ?: Int.MAX_VALUE

    fun leer(context: Context): String = runCatching {
        context.assets.open("$carpeta/$archivo").bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrDefault("")
}

fun cargarGuias(context: Context, carpeta: String = "historia"): List<Guia> =
    (context.assets.list(carpeta) ?: emptyArray())
        .filter { it.substringAfterLast('.').lowercase() in setOf("txt", "md", "pdf") }
        .map { Guia(it, carpeta) }
        .sortedWith(compareBy({ it.orden }, { it.archivo }))

/**
 * Un PDF de las guías abierto con el lector de Android. PdfRenderer necesita un
 * archivo de verdad, así que el PDF se copia de los assets a la caché una vez.
 * Solo se puede dibujar una página a la vez: por eso [pagina] va sincronizado.
 */
class DocumentoPdf private constructor(
    private val descriptor: android.os.ParcelFileDescriptor,
    private val lector: android.graphics.pdf.PdfRenderer,
) {
    val paginas: Int get() = lector.pageCount

    /** Dibuja la página [i] con [anchoPx] de ancho (fondo blanco, como el papel). */
    @Synchronized
    fun pagina(i: Int, anchoPx: Int): android.graphics.Bitmap? = runCatching {
        lector.openPage(i).use { p ->
            val ancho = anchoPx.coerceIn(200, 2400)
            val alto = (ancho.toLong() * p.height / p.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
            val bmp = android.graphics.Bitmap.createBitmap(ancho, alto, android.graphics.Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            p.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        }
    }.getOrNull()

    /** Alto/ancho de la página [i], para reservar su hueco antes de dibujarla. */
    @Synchronized
    fun proporcion(i: Int): Float = runCatching {
        lector.openPage(i).use { it.height.toFloat() / it.width.coerceAtLeast(1) }
    }.getOrDefault(1.414f)

    @Synchronized
    fun cerrar() {
        runCatching { lector.close() }
        runCatching { descriptor.close() }
    }

    companion object {
        fun abrir(context: Context, guia: Guia): DocumentoPdf? = runCatching {
            val copia = java.io.File(context.cacheDir, "${guia.carpeta}/${guia.archivo}")
            if (!copia.isFile || copia.length() == 0L) {
                copia.parentFile?.mkdirs()
                context.assets.open("${guia.carpeta}/${guia.archivo}").use { e -> copia.outputStream().use { e.copyTo(it) } }
            }
            val d = android.os.ParcelFileDescriptor.open(copia, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            DocumentoPdf(d, android.graphics.pdf.PdfRenderer(d))
        }.getOrNull()
    }
}
