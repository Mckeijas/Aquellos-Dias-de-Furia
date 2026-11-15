package com.diasdefuria.datos

import android.content.Context
import java.io.File

/**
 * Tablas de récords de las recreativas (hiscore.dat de FBNeo).
 *
 * FBNeo guarda la tabla de puntuaciones de cada versión en saves/fbneo/<romset>.hi
 * al salir del juego, y la vuelve a cargar al arrancarlo, también tras reiniciar.
 * Las versiones de un mismo juego que usan la misma tabla (según hiscore.dat) la
 * comparten: al empezar se copia la más reciente, así que cambiar de ZIP o de
 * versión no hace perder los récords.
 */
object Puntuaciones {
    private const val DAT = "hiscore/hiscore.dat"

    /** Instala hiscore.dat para FBNeo y trae los récords más recientes del grupo del romset. */
    fun preparar(context: Context, sistema: File, guardados: File, romset: RomsetConocido) {
        runCatching {
            val texto = context.assets.open(DAT).bufferedReader().use { it.readText() }
            val destino = File(sistema, "fbneo/hiscore.dat")
            if (!destino.isFile || destino.readText() != texto) {
                destino.parentFile?.mkdirs()
                destino.writeText(texto)
            }
            val grupo = grupoDe(texto, romset.nombre)
            val carpeta = File(guardados, "fbneo").apply { mkdirs() }
            val mio = File(carpeta, "${romset.nombre}.hi")
            val reciente = grupo.map { File(carpeta, "$it.hi") }
                .filter { it.isFile && it.length() > 0 }
                .maxByOrNull { it.lastModified() }
            if (reciente != null && reciente != mio && (!mio.isFile || reciente.lastModified() > mio.lastModified())) {
                reciente.copyTo(mio, overwrite = true)
                mio.setLastModified(reciente.lastModified())
            }
        }
    }

    /** Romsets que comparten la tabla de récords con [romset] (un bloque de hiscore.dat). */
    private fun grupoDe(texto: String, romset: String): Set<String> =
        texto.replace("\r", "").split(Regex("\n\\s*\n"))
            .map { bloque -> bloque.lines().filter { it.endsWith(":") && !it.startsWith(";") && !it.startsWith("@") }.map { it.dropLast(1) }.toSet() }
            .firstOrNull { romset in it } ?: setOf(romset)

    /**
     * Récord de la máquina: la mejor puntuación de la tabla de récords guardada (la que la
     * placa enseña como «TOP»). Solo en los juegos cuya entrada de hiscore.dat tiene esa
     * puntuación en una zona propia, en BCD: [ZONA_RECORD] dice cuál es (contando desde 0).
     * Al principio es el récord que trae la máquina de fábrica, hasta que alguien lo supera.
     */
    fun record(context: Context, juego: Juego): Long? = runCatching {
        val zona = ZONA_RECORD[juego.id] ?: return null
        val texto = context.assets.open(DAT).bufferedReader().use { it.readText() }
        val carpeta = File(File(context.filesDir, "saves"), "fbneo")
        // La tabla más reciente de cualquier versión del juego
        val hi = CatalogoRomsets.todos.filter { it.juego == juego }
            .map { File(carpeta, "${it.nombre}.hi") }
            .filter { it.isFile && it.length() > 0 }
            .maxByOrNull { it.lastModified() } ?: return null
        val regiones = regionesDe(texto, hi.nameWithoutExtension)
        if (zona >= regiones.size) return null
        val desde = regiones.take(zona).sum()
        val bytes = hi.readBytes()
        if (bytes.size < desde + regiones[zona]) return null
        var valor = 0L
        for (i in 0 until regiones[zona]) {
            val b = bytes[desde + i].toInt() and 0xFF
            val alto = b shr 4
            val bajo = b and 0x0F
            if (alto > 9 || bajo > 9) return null   // no es BCD: mejor no enseñar nada
            valor = valor * 100 + alto * 10 + bajo
        }
        valor.takeIf { it > 0 }
    }.getOrNull()

    /** Tamaño en bytes de cada zona de memoria del bloque de hiscore.dat del romset. */
    private fun regionesDe(texto: String, romset: String): List<Int> =
        texto.replace("\r", "").split(Regex("\n\\s*\n"))
            .firstOrNull { bloque -> bloque.lines().any { it == "$romset:" } }
            ?.lines()?.filter { it.startsWith("@") }
            ?.mapNotNull { it.split(',').getOrNull(3)?.trim()?.toIntOrNull(16) }
            .orEmpty()

    /** Zona de hiscore.dat con la puntuación «TOP», por juego (comprobada en el hiscore.dat). */
    private val ZONA_RECORD = mapOf(
        "ffight" to 1, "captcomm" to 2, "knights" to 3, "wof" to 2, "punisher" to 1, "ddragon" to 1, "ddragon2" to 1,
    )
}
