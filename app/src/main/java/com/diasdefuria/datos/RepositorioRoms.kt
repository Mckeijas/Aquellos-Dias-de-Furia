package com.diasdefuria.datos

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Qué ha pasado con cada archivo elegido por el jugador. */
sealed interface ResultadoArchivo {
    val archivo: String

    /** ZIP reconocido y guardado. [faltan] son los archivos que no trae él mismo. */
    data class ZipReconocido(
        override val archivo: String,
        val romset: RomsetConocido,
        val faltan: List<ArchivoRom>,
        /** Archivos que venían «sobredumpeados» (el doble de tamaño, repetidos) y se han recortado. */
        val reparados: List<String> = emptyList(),
        /** BIOS que venía dentro del ZIP y se ha guardado aparte (p. ej. «neogeo»). */
        val bios: String? = null,
    ) : ResultadoArchivo

    /** BIOS de placa (p. ej. neogeo.zip) guardada o completada con los archivos que traía este ZIP. */
    data class BiosAnadida(override val archivo: String, val bios: String) : ResultadoArchivo

    /** Archivo .key suelto añadido a los ZIP que lo necesitaban. */
    data class ClaveAnadida(override val archivo: String, val clave: String, val romsets: List<String>) : ResultadoArchivo

    /** Clave válida, pero todavía no hay ningún ZIP importado que la use. */
    data class ClaveSinDestino(override val archivo: String, val clave: String) : ResultadoArchivo

    /** No es ninguna versión conocida. Con [esperado], no es ninguna versión de ese juego. */
    data class NoReconocido(override val archivo: String, val esperado: Juego? = null) : ResultadoArchivo

    /** Es una versión de [encontrado], pero se estaba importando para [esperado]: no se instala. */
    data class OtroJuego(override val archivo: String, val encontrado: Juego, val esperado: Juego) : ResultadoArchivo
    data class Fallo(override val archivo: String, val motivo: String) : ResultadoArchivo
}

/** Situación de un juego según los archivos importados. */
data class EstadoJuego(
    val juego: Juego,
    /** Versión Hispanic lista para jugar, si la hay. */
    val jugable: RomsetConocido?,
    /** Versión Hispanic importada pero incompleta, y lo que le falta. */
    val incompleto: RomsetConocido?,
    val faltan: List<ArchivoRom>,
    /** Versión Europa completa, para jugar en inglés si la Hispanic no está lista. */
    val alternativa: RomsetConocido?,
    val algoImportado: Boolean,
    /** Todas las versiones completas, para elegir con cuál jugar (p. ej. Hispanic o Europa con logros). */
    val versiones: List<RomsetConocido> = emptyList(),
)

/**
 * Importa los ZIP reconociéndolos por su contenido (CRC de cada archivo), no por
 * su nombre, y los guarda con el nombre de romset que espera FBNeo. Igual que
 * FBNeo, una versión se considera completa si entre su ZIP y el de su juego padre
 * están todos los archivos. Los archivos originales del jugador no se modifican.
 */
class RepositorioRoms(private val context: Context) {

    private val carpeta: File get() = File(context.filesDir, "roms").apply { mkdirs() }
    private val elecciones = context.getSharedPreferences("versiones", Context.MODE_PRIVATE)

    /** Versión con la que se jugará a un juego cuando hay varias completas. */
    fun elegirVersion(romset: RomsetConocido) {
        elecciones.edit().putString(romset.juego.id, romset.nombre).apply()
    }

    fun archivo(romset: RomsetConocido) = File(carpeta, romset.archivoZip)

    private fun crcsDe(f: File): Set<Long> =
        if (!f.isFile) emptySet()
        else runCatching { ZipFile(f).use { z -> z.entries().toList().filterNot { it.isDirectory }.map { it.crc }.toSet() } }
            .getOrDefault(emptySet())

    /** ZIP de una BIOS de placa, con el nombre que busca FBNeo (neogeo.zip). */
    fun archivoBios(bios: String) = File(carpeta, "$bios.zip")

    suspend fun estado(juego: Juego): EstadoJuego = withContext(Dispatchers.IO) {
        val romsets = CatalogoRomsets.todos.filter { it.juego == juego }
        val crcs = romsets.associate { it.nombre to crcsDe(archivo(it)) }
        val crcsBios = romsets.mapNotNull { it.bios }.distinct().associateWith { crcsDe(archivoBios(it)) }
        fun faltan(rs: RomsetConocido): List<ArchivoRom> {
            val disponibles = crcs[rs.nombre].orEmpty() + (rs.padre?.let { crcs[it] }.orEmpty())
            val propios = rs.archivos.filter { !it.opcional && it.crc !in disponibles }
            // La BIOS de la placa (neogeo.zip) también tiene que estar
            val bios = rs.bios?.let { b ->
                CatalogoRomsets.bios[b].orEmpty().filter { !it.opcional && it.crc !in crcsBios[b].orEmpty() }
            }.orEmpty()
            return propios + bios
        }
        val instalados = romsets.filter { crcs[it.nombre].orEmpty().isNotEmpty() }
        // Una Hispanic cuenta si tiene su ZIP o si sus programas vienen dentro del
        // ZIP del padre (romsets «merged», que agrupan todas las versiones en uno)
        val hispanicas = romsets.filter { rs ->
            rs.hispanic && (rs in instalados ||
                rs.archivos.filter { it.programa }.all { it.crc in rs.padre?.let { p -> crcs[p] }.orEmpty() })
        }
        // Juegos sin versión en español: vale cualquier versión importada
        val preferidas = if (juego.tieneHispanic) hispanicas else instalados
        val completas = (hispanicas + instalados).distinct().filter { faltan(it).isEmpty() }
        // La versión elegida por el jugador manda; si no, la Hispanic (o la primera completa)
        val elegida = elecciones.getString(juego.id, null)?.let { n -> completas.firstOrNull { it.nombre == n } }
        val jugable = elegida ?: preferidas.firstOrNull { faltan(it).isEmpty() }
            // Sin Hispanic importada: se juega con la versión completa que haya (p. ej. la europea)
            ?: if (hispanicas.isEmpty()) completas.firstOrNull() else null
        val incompleto = if (jugable == null) preferidas.minByOrNull { faltan(it).size } else null
        val alternativa = if (jugable == null && juego.tieneHispanic)
            instalados.firstOrNull { !it.hispanic && faltan(it).isEmpty() } else null
        EstadoJuego(
            juego = juego,
            jugable = jugable,
            incompleto = incompleto,
            faltan = incompleto?.let(::faltan).orEmpty(),
            alternativa = alternativa,
            algoImportado = instalados.isNotEmpty(),
            versiones = completas,
        )
    }

    /**
     * Con [juego], solo se aceptan versiones de ese juego (Hispanic, Europa, Japón...):
     * un ZIP o una clave de otro juego da error y no se instala nada.
     */
    /**
     * Importa los archivos elegidos. Con [conservarVersiones] (importación desde la carpeta de
     * «Refresca») no se borran las demás versiones del juego ni se cambia la que se juega.
     */
    suspend fun importar(uris: List<Uri>, juego: Juego? = null, conservarVersiones: Boolean = false): List<ResultadoArchivo> = withContext(Dispatchers.IO) {
        // Primero los ZIP y después las claves, para que una clave elegida junto a su ZIP encuentre destino
        val copias = uris.map { uri -> uri to (nombreVisible(uri) ?: uri.lastPathSegment ?: "archivo") }
        val (claves, resto) = copias.partition { it.second.endsWith(".key", ignoreCase = true) }
        (resto + claves).map { (uri, nombre) ->
            try {
                if (nombre.endsWith(".key", ignoreCase = true)) importarClave(uri, nombre, juego)
                else importarZip(uri, nombre, juego, conservarVersiones)
            } catch (e: Exception) {
                ResultadoArchivo.Fallo(nombre, e.localizedMessage ?: e.javaClass.simpleName)
            }
        }
    }

    private fun importarZip(uri: Uri, nombre: String, juego: Juego?, conservarVersiones: Boolean = false): ResultadoArchivo {
        val temporal = File(carpeta, "importando.tmp")
        try {
            copiar(uri, temporal)
            var crcs = crcsDe(temporal)
            if (crcs.isEmpty()) return ResultadoArchivo.NoReconocido(nombre, juego)

            val romset = identificar(crcs)
            if (romset == null) {
                // ¿Es una BIOS suelta (neogeo.zip)? Se guarda aunque no haya juego que la use todavía
                val bios = extraerBios(temporal)
                return if (bios != null) ResultadoArchivo.BiosAnadida(nombre, bios)
                else ResultadoArchivo.NoReconocido(nombre, juego)
            }
            // Una versión de otro juego no se instala (ni se toca lo que ya hay)
            if (juego != null && romset.juego != juego) return ResultadoArchivo.OtroJuego(nombre, romset.juego, juego)

            // ROMs antiguas con archivos del doble de tamaño (repetidos): se recortan a lo que espera FBNeo
            val reparados = repararSobredumps(temporal, romset, crcs)
            if (reparados.isNotEmpty()) crcs = crcsDe(temporal)
            // Si trae la BIOS de su placa dentro (p. ej. Neo Geo), se guarda aparte como neogeo.zip
            val bios = romset.bios?.let { extraerBios(temporal) }

            val destino = archivo(romset)
            destino.delete()
            if (!temporal.renameTo(destino)) temporal.copyTo(destino, overwrite = true)
            if (!conservarVersiones) reemplazarVersiones(romset)
            return ResultadoArchivo.ZipReconocido(nombre, romset,
                romset.archivos.filter { !it.opcional && it.crc !in crcs }, reparados, bios)
        } finally {
            temporal.delete()
        }
    }

    /**
     * Qué versión es un ZIP. Cuenta lo que la distingue: los programas que no comparte con
     * su versión padre (un ZIP «split» de un clon solo trae eso) o, para el padre, todos sus
     * programas. Si encajan varias (ZIP «merged» con todas), gana la completa, luego el padre
     * y luego la que tiene más archivos dentro.
     */
    private fun identificar(crcs: Set<Long>): RomsetConocido? {
        var mejor: RomsetConocido? = null
        var mejorPuntos = Triple(false, false, -1)
        for (rs in CatalogoRomsets.todos) {
            val padre = rs.padre?.let { CatalogoRomsets.porNombre(it) }
            val prueba = if (padre == null) {
                rs.archivos.filter { it.programa }.ifEmpty { rs.archivos.filterNot { it.opcional } }
            } else {
                val delPadre = padre.archivos.map { it.crc }.toSet()
                val propios = rs.archivos.filter { it.crc !in delPadre && !it.opcional }
                propios.filter { it.programa }.ifEmpty { propios }
            }
            if (prueba.isEmpty() || !prueba.all { it.crc in crcs }) continue
            val puntos = Triple(
                rs.archivos.all { it.opcional || it.crc in crcs },
                rs.padre == null,
                rs.archivos.count { it.crc in crcs },
            )
            if (mejor == null || compararPuntos(puntos, mejorPuntos) > 0) {
                mejor = rs
                mejorPuntos = puntos
            }
        }
        return mejor
    }

    private fun compararPuntos(a: Triple<Boolean, Boolean, Int>, b: Triple<Boolean, Boolean, Int>): Int =
        compareValuesBy(a, b, { it.first }, { it.second }, { it.third })

    /**
     * Algunos ZIP antiguos traen archivos «sobredumpeados»: el doble (o más) de su tamaño,
     * con el contenido repetido (p. ej. avp.14m de 2 MB en Alien vs. Predator). Si el nombre
     * coincide y la primera copia tiene el CRC correcto, se recorta. Devuelve los reparados.
     */
    private fun repararSobredumps(zip: File, romset: RomsetConocido, crcs: Set<Long>): List<String> {
        val faltan = romset.archivos.filter { it.crc !in crcs && it.tamano > 0 }
        if (faltan.isEmpty()) return emptyList()
        val arreglos = HashMap<String, ByteArray>()
        ZipFile(zip).use { z ->
            val entradas = z.entries().toList().filterNot { it.isDirectory }
            for (a in faltan) {
                val e = entradas.firstOrNull { it.name.substringAfterLast('/').equals(a.nombre, ignoreCase = true) } ?: continue
                if (e.size <= a.tamano || e.size % a.tamano != 0L || e.size > 64L * 1024 * 1024) continue
                val datos = z.getInputStream(e).use { it.readBytes() }
                val n = a.tamano.toInt()
                val primera = datos.copyOfRange(0, n)
                if (CRC32().apply { update(primera) }.value != a.crc) continue
                val repetido = (1 until datos.size / n).all { k ->
                    (0 until n).all { i -> datos[k * n + i] == primera[i] }
                }
                if (repetido) arreglos[e.name] = primera
            }
        }
        if (arreglos.isEmpty()) return emptyList()
        reescribirZip(zip) { nombre, original -> arreglos[nombre] ?: original() }
        return arreglos.keys.map { it.substringAfterLast('/') }
    }

    /**
     * Copia a <bios>.zip (p. ej. neogeo.zip) los archivos de BIOS que traiga [zip], con el nombre
     * que espera FBNeo. Devuelve el nombre de la BIOS si ha añadido algo o ya estaba completa.
     */
    private fun extraerBios(zip: File): String? {
        for ((bios, lista) in CatalogoRomsets.bios) {
            val porCrc = lista.associateBy { it.crc }
            val encontrados = LinkedHashMap<String, ByteArray>()
            ZipFile(zip).use { z ->
                z.entries().toList().filterNot { it.isDirectory }.forEach { e ->
                    val a = porCrc[e.crc] ?: return@forEach
                    if (a.nombre !in encontrados) encontrados[a.nombre] = z.getInputStream(e).use { it.readBytes() }
                }
            }
            // Sin los archivos obligatorios de la BIOS no cuenta (un juego puede compartir algún CRC)
            val obligatorios = lista.filterNot { it.opcional }
            if (encontrados.isEmpty() || obligatorios.none { it.nombre in encontrados }) continue
            val destino = archivoBios(bios)
            val yaEstan = crcsDe(destino)
            val nuevos = encontrados.filter { (n, _) -> lista.first { it.nombre == n }.crc !in yaEstan }
            if (nuevos.isNotEmpty()) anadirAlZip(destino, nuevos)
            return bios
        }
        return null
    }

    /** Reescribe un ZIP cambiando el contenido de algunas entradas. */
    private fun reescribirZip(zip: File, contenido: (nombre: String, original: () -> ByteArray) -> ByteArray) {
        val nuevo = File(zip.parentFile, zip.name + ".nuevo")
        ZipFile(zip).use { origen ->
            ZipOutputStream(nuevo.outputStream().buffered()).use { salida ->
                origen.entries().toList().filterNot { it.isDirectory }.forEach { e ->
                    salida.putNextEntry(ZipEntry(e.name))
                    salida.write(contenido(e.name) { origen.getInputStream(e).use { it.readBytes() } })
                    salida.closeEntry()
                }
            }
        }
        zip.delete()
        if (!nuevo.renameTo(zip)) {
            nuevo.copyTo(zip, overwrite = true)
            nuevo.delete()
        }
    }

    /**
     * «Cambiar ZIP» cambia de versión, sin perder partidas ni récords:
     * - Al importar la europea (u otra versión no Hispanic) se quita la Hispanic y
     *   las demás versiones: se juega con la nueva (la de los logros).
     * - Al importar una Hispanic se quitan las otras Hispanic; el ZIP padre (la
     *   europea) se conserva porque la Hispanic lo necesita para funcionar.
     * La versión recién importada pasa a ser la que se juega.
     */
    private fun reemplazarVersiones(nueva: RomsetConocido) {
        CatalogoRomsets.todos
            .filter { it.juego == nueva.juego && it != nueva && it.nombre != nueva.padre }
            .filter { viejo -> !nueva.hispanic || viejo.hispanic }
            .forEach { archivo(it).delete() }
        elegirVersion(nueva)
    }

    private fun importarClave(uri: Uri, nombre: String, juego: Juego?): ResultadoArchivo {
        val datos = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return ResultadoArchivo.Fallo(nombre, "no se pudo abrir")
        val crc = CRC32().apply { update(datos) }.value
        val clave = CatalogoRomsets.todos.flatMap { it.archivos }.firstOrNull { it.clave && it.crc == crc }
            ?: return ResultadoArchivo.NoReconocido(nombre, juego)
        val delJuego = CatalogoRomsets.todos.filter { rs -> rs.archivos.any { it.clave && it.crc == crc } }.map { it.juego }.toSet()
        if (juego != null && juego !in delJuego) return ResultadoArchivo.OtroJuego(nombre, delJuego.first(), juego)

        val destinos = CatalogoRomsets.todos.filter { rs -> (juego == null || rs.juego == juego) &&
            rs.archivos.any { it.clave && it.crc == crc } && archivo(rs).isFile && crc !in crcsDe(archivo(rs))
        }
        if (destinos.isEmpty()) {
            val yaEsta = CatalogoRomsets.todos.any { rs -> crc in crcsDe(archivo(rs)) }
            return if (yaEsta) ResultadoArchivo.ClaveAnadida(nombre, clave.nombre, emptyList())
            else ResultadoArchivo.ClaveSinDestino(nombre, clave.nombre)
        }
        destinos.forEach { anadirAlZip(archivo(it), mapOf(clave.nombre to datos)) }
        return ResultadoArchivo.ClaveAnadida(nombre, clave.nombre, destinos.map { it.nombre })
    }

    /**
     * Reescribe el ZIP con archivos de más (los ZIP de ROMs no se pueden ampliar en el sitio).
     * Si el ZIP no existe, se crea.
     */
    private fun anadirAlZip(zip: File, archivos: Map<String, ByteArray>) {
        val nuevo = File(zip.parentFile, zip.name + ".nuevo")
        ZipOutputStream(nuevo.outputStream().buffered()).use { salida ->
            if (zip.isFile) ZipFile(zip).use { origen ->
                origen.entries().toList()
                    .filter { e -> !e.isDirectory && archivos.keys.none { it.equals(e.name, ignoreCase = true) } }
                    .forEach { e ->
                        salida.putNextEntry(ZipEntry(e.name))
                        origen.getInputStream(e).use { it.copyTo(salida) }
                        salida.closeEntry()
                    }
            }
            archivos.forEach { (nombre, datos) ->
                salida.putNextEntry(ZipEntry(nombre))
                salida.write(datos)
                salida.closeEntry()
            }
        }
        zip.delete()
        if (!nuevo.renameTo(zip)) {
            nuevo.copyTo(zip, overwrite = true)
            nuevo.delete()
        }
    }

    private fun copiar(uri: Uri, destino: File) {
        val entrada = context.contentResolver.openInputStream(uri) ?: error("no se pudo abrir")
        entrada.use { o -> destino.outputStream().use { d -> o.copyTo(d) } }
    }

    private fun nombreVisible(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
}
