package com.diasdefuria.datos

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Una partida guardada (save state). Se guarda por versión del juego (romset),
 * porque el estado de una revisión no sirve para otra.
 *
 * Archivos: auto.state (al salir del juego), ranura1..6.state (guardadas a mano)
 * y, de versiones anteriores, aaaammdd-hhmmss.state. Cada una puede llevar al lado
 * su miniatura .png con la imagen del juego en ese momento.
 */
data class Partida(
    val juego: Juego,
    val romset: RomsetConocido,
    val archivo: File,
) {
    val automatica: Boolean get() = archivo.name == ARCHIVO_AUTO
    /** Número de ranura (1..6) o null si es la automática o una antigua. */
    val ranura: Int? get() = archivo.nameWithoutExtension.removePrefix(PREFIJO_RANURA)
        .takeIf { archivo.name.startsWith(PREFIJO_RANURA) }?.toIntOrNull()
    val fecha: Long get() = archivo.lastModified()
    val miniatura: File get() = File(archivo.parentFile, archivo.nameWithoutExtension + ".png")

    companion object {
        const val ARCHIVO_AUTO = "auto.state"
        const val PREFIJO_RANURA = "ranura"
        const val RANURAS = 6
    }
}

class RepositorioPartidas(private val context: Context) {

    private fun carpeta(romset: RomsetConocido) =
        File(context.filesDir, "estados/${romset.nombre}").apply { mkdirs() }

    private fun validos(romset: RomsetConocido) =
        (carpeta(romset).listFiles() ?: emptyArray())
            .filter { it.isFile && it.extension == "state" && it.length() > 0 }

    fun listar(juego: Juego): List<Partida> =
        CatalogoRomsets.todos.filter { it.juego == juego }.flatMap { rs -> listar(rs) }
            .sortedByDescending { it.fecha }

    /** Partidas de una versión concreta: primero la automática, luego las ranuras y las antiguas. */
    fun listar(romset: RomsetConocido): List<Partida> =
        validos(romset).map { Partida(romset.juego, romset, it) }
            .sortedWith(compareBy<Partida>({ !it.automatica }, { it.ranura ?: 99 }, { -it.fecha }))

    /** La partida de cada ranura (null = vacía). */
    fun ranuras(romset: RomsetConocido): List<Partida?> = (1..Partida.RANURAS).map { n ->
        File(carpeta(romset), "${Partida.PREFIJO_RANURA}$n.state").takeIf { it.isFile && it.length() > 0 }
            ?.let { Partida(romset.juego, romset, it) }
    }

    fun automatica(romset: RomsetConocido): File? =
        File(carpeta(romset), Partida.ARCHIVO_AUTO).takeIf { it.isFile && it.length() > 0 }

    /** Guarda en una ranura (1..6) o, con [ranura] = null, la automática. */
    fun guardar(romset: RomsetConocido, datos: ByteArray, ranura: Int?, miniatura: Bitmap? = null): File {
        val nombre = if (ranura == null) Partida.ARCHIVO_AUTO else "${Partida.PREFIJO_RANURA}$ranura.state"
        val destino = File(carpeta(romset), nombre)
        escribirSeguro(destino) { it.writeBytes(datos) }
        val png = File(destino.parentFile, destino.nameWithoutExtension + ".png")
        if (miniatura != null) {
            runCatching { escribirSeguro(png) { f -> f.outputStream().use { miniatura.compress(Bitmap.CompressFormat.PNG, 100, it) } } }
        } else {
            png.delete()
        }
        return destino
    }

    /** Compatibilidad con la llamada antigua (partida con fecha). */
    fun guardar(romset: RomsetConocido, datos: ByteArray, automatica: Boolean): File =
        if (automatica) guardar(romset, datos, null)
        else {
            val destino = File(carpeta(romset), SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date()) + ".state")
            escribirSeguro(destino) { it.writeBytes(datos) }
            destino
        }

    private fun escribirSeguro(destino: File, escribir: (File) -> Unit) {
        val temporal = File(destino.parentFile, destino.name + ".tmp")
        escribir(temporal)
        destino.delete()
        temporal.renameTo(destino)
    }

    fun ultima(romset: RomsetConocido): File? = validos(romset).maxByOrNull { it.lastModified() }

    fun borrar(partida: Partida) {
        partida.archivo.delete()
        partida.miniatura.delete()
    }
}
