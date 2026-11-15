package com.diasdefuria.datos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.diasdefuria.R
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Un pantallazo guardado al conseguir un logro. */
data class Pantallazo(
    val archivo: File,
    val juego: Juego?,
    val logro: String,
    val puntos: Int,
    val fecha: Long,
) {
    /** Ruta que entiende Coil. */
    val uri: String get() = "file://${archivo.absolutePath}"
}

/**
 * Pantallazos de los logros (Extras › Pantallazos).
 *
 * Al saltar un logro se captura la imagen del juego y se le pinta encima el aviso con el
 * icono del logro, su nombre, la descripción y los puntos. Se guardan en la memoria de la
 * app (files/pantallazos/), un JPG por logro y al lado un .json con sus datos.
 */
object Pantallazos {
    private const val CARPETA = "pantallazos"

    private fun carpeta(context: Context) = File(context.filesDir, CARPETA).apply { mkdirs() }

    /** Todos, del más reciente al más antiguo. */
    fun listar(context: Context): List<Pantallazo> =
        (carpeta(context).listFiles { f -> f.extension == "jpg" } ?: emptyArray()).mapNotNull { jpg ->
            val datos = runCatching { JSONObject(File(jpg.path.removeSuffix(".jpg") + ".json").readText()) }.getOrNull()
            Pantallazo(
                archivo = jpg,
                juego = Juego.porId(datos?.optString("juego")),
                logro = datos?.optString("logro").orEmpty(),
                puntos = datos?.optInt("puntos") ?: 0,
                fecha = datos?.optLong("fecha")?.takeIf { it > 0 } ?: jpg.lastModified(),
            )
        }.sortedByDescending { it.fecha }

    fun borrar(p: Pantallazo) {
        p.archivo.delete()
        File(p.archivo.path.removeSuffix(".jpg") + ".json").delete()
    }

    /**
     * Compone y guarda el pantallazo. [captura] es la imagen del juego; [icono], el del
     * logro (puede faltar si no se pudo descargar). Se llama fuera del hilo principal.
     */
    fun guardar(
        context: Context,
        captura: Bitmap,
        romset: RomsetConocido,
        titulo: String,
        descripcion: String,
        puntos: Int,
        icono: Bitmap?,
    ): File? = runCatching {
        val fecha = System.currentTimeMillis()
        val imagen = componer(context, captura, titulo, descripcion, puntos, icono)
        val nombre = "${romset.nombre}_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(fecha))
        val jpg = File(carpeta(context), "$nombre.jpg")
        jpg.outputStream().use { imagen.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        File(carpeta(context), "$nombre.json").writeText(
            JSONObject()
                .put("juego", romset.juego.id)
                .put("romset", romset.nombre)
                .put("logro", titulo)
                .put("descripcion", descripcion)
                .put("puntos", puntos)
                .put("fecha", fecha)
                .toString()
        )
        imagen.recycle()
        jpg
    }.getOrNull()

    /** La captura con el aviso del logro abajo en el centro, como sale en la partida. */
    private fun componer(
        context: Context,
        captura: Bitmap,
        titulo: String,
        descripcion: String,
        puntos: Int,
        icono: Bitmap?,
    ): Bitmap {
        val salida = captura.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(salida)
        val w = salida.width.toFloat()
        val h = salida.height.toFloat()
        val u = h / 100f // unidad: 1 % del alto

        val negrita = runCatching { ResourcesCompat.getFont(context, R.font.barlow_negrita) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        val normal = runCatching { ResourcesCompat.getFont(context, R.font.barlow_texto) }.getOrNull() ?: Typeface.DEFAULT
        val titular = runCatching { ResourcesCompat.getFont(context, R.font.bangers_titulo) }.getOrNull() ?: negrita

        // Caja del aviso
        val anchoCaja = (w * 0.62f).coerceAtLeast(70 * u)
        val altoCaja = 21 * u
        val caja = RectF((w - anchoCaja) / 2, h - altoCaja - 4 * u, (w + anchoCaja) / 2, h - 4 * u)
        val radio = 2.2f * u
        c.drawRoundRect(caja, radio, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(caja.left, 0f, caja.right, 0f, Color.argb(235, 26, 26, 34), Color.argb(235, 42, 42, 54), Shader.TileMode.CLAMP)
        })
        c.drawRoundRect(caja, radio, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 0.35f * u; color = Color.argb(150, 255, 194, 31)
        })

        // Icono del logro
        val lado = altoCaja - 5 * u
        val cajaIcono = RectF(caja.left + 2.5f * u, caja.top + 2.5f * u, caja.left + 2.5f * u + lado, caja.top + 2.5f * u + lado)
        if (icono != null) {
            c.drawBitmap(icono, null, cajaIcono, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            c.drawRoundRect(cajaIcono, u, u, Paint().apply { color = Color.BLACK })
        }

        // Textos
        val x = cajaIcono.right + 3 * u
        val anchoTexto = caja.right - 2.5f * u - x
        val etiqueta = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = titular; textSize = 3.6f * u; color = Color.rgb(255, 194, 31); letterSpacing = 0.06f }
        val nombre = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = negrita; textSize = 5.2f * u; color = Color.WHITE }
        val detalle = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = normal; textSize = 3.6f * u; color = Color.argb(190, 255, 255, 255) }
        var y = caja.top + 6.2f * u
        c.drawText(context.getString(R.string.ra_logro_desbloqueado).uppercase(), x, y, etiqueta)
        y += 6.2f * u
        c.drawText(TextUtils.ellipsize(titulo, nombre, anchoTexto, TextUtils.TruncateAt.END).toString(), x, y, nombre)
        y += 5.2f * u
        val linea = context.getString(R.string.ra_puntos, puntos) + if (descripcion.isNotBlank()) " · $descripcion" else ""
        c.drawText(TextUtils.ellipsize(linea, detalle, anchoTexto, TextUtils.TruncateAt.END).toString(), x, y, detalle)

        // Marca discreta arriba a la derecha: juego y fecha
        val marca = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = titular; textSize = 3.2f * u; color = Color.argb(140, 255, 255, 255); textAlign = Paint.Align.RIGHT
        }
        c.drawText(
            context.getString(R.string.app_name) + " · " +
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
            w - 2.5f * u, 5 * u, marca,
        )
        return salida
    }
}
