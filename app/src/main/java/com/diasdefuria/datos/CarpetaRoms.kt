package com.diasdefuria.datos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Qué pasó al refrescar la carpeta de ROMs. */
data class ResultadoCarpeta(
    val carpeta: String,
    /** ZIP y claves .key encontrados en la carpeta (y sus subcarpetas). */
    val encontrados: Int,
    /** Los que ya se habían importado antes y no han cambiado: no se vuelven a copiar. */
    val yaImportados: Int,
    val resultados: List<ResultadoArchivo>,
    /** No se pudo leer la carpeta (permiso perdido, tarjeta SD quitada…). */
    val sinAcceso: Boolean = false,
)

/**
 * Carpeta del móvil de donde «Refresca» importa todas las ROMs de golpe.
 *
 * - «Ruta» la elige con el selector de carpetas de Android; el permiso de lectura se
 *   guarda para siempre (sobrevive a reinicios), así que solo se elige una vez.
 * - «Refresca» busca los .zip y .key de esa carpeta y de sus subcarpetas (hasta 3 niveles)
 *   y los importa como si se eligieran a mano, pero sin borrar otras versiones del mismo
 *   juego: si en la carpeta hay varias regiones, se quedan todas.
 * - Recuerda lo ya importado (por archivo, tamaño y fecha): al refrescar otra vez solo
 *   se copian los ZIP nuevos o cambiados, y los que se borraron de la app.
 */
class CarpetaRoms(private val context: Context, private val repo: RepositorioRoms) {
    private val prefs = context.getSharedPreferences("carpeta_roms", Context.MODE_PRIVATE)
    private val hechos = context.getSharedPreferences("carpeta_roms_hechos", Context.MODE_PRIVATE)

    var ruta: Uri?
        get() = prefs.getString("ruta", null)?.let(Uri::parse)
        private set(v) = prefs.edit().putString("ruta", v?.toString()).apply()

    /** Guarda la carpeta elegida con «Ruta» y suelta el permiso de la anterior. */
    fun elegir(arbol: Uri) {
        val cr = context.contentResolver
        val anterior = ruta
        cr.takePersistableUriPermission(arbol, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (anterior != null && anterior != arbol) {
            runCatching { cr.releasePersistableUriPermission(anterior, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            hechos.edit().clear().apply()
        }
        ruta = arbol
    }

    /** Nombre de la carpeta para mostrarlo («roms»), o null si no hay ruta o ya no se puede leer. */
    fun nombre(arbol: Uri? = ruta): String? = arbol?.let {
        runCatching {
            val doc = DocumentsContract.buildDocumentUriUsingTree(it, DocumentsContract.getTreeDocumentId(it))
            context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
    }

    suspend fun refrescar(): ResultadoCarpeta = withContext(Dispatchers.IO) {
        val arbol = ruta ?: return@withContext ResultadoCarpeta("", 0, 0, emptyList(), sinAcceso = true)
        val carpeta = nombre(arbol) ?: return@withContext ResultadoCarpeta(arbol.lastPathSegment ?: "", 0, 0, emptyList(), sinAcceso = true)
        val archivos = try {
            buscar(arbol, DocumentsContract.getTreeDocumentId(arbol), 0)
        } catch (e: SecurityException) {
            return@withContext ResultadoCarpeta(carpeta, 0, 0, emptyList(), sinAcceso = true)
        }
        val nuevos = archivos.filterNot(::yaHecho)
        val resultados = if (nuevos.isEmpty()) emptyList()
            else repo.importar(nuevos.map { it.uri }, juego = null, conservarVersiones = true)
        // importar() devuelve los resultados en otro orden (primero los ZIP, luego las claves): se casan por nombre
        val porNombre = nuevos.associateBy { it.nombre }
        resultados.forEach { r ->
            val a = porNombre[r.archivo] ?: return@forEach
            val valor = when (r) {
                is ResultadoArchivo.ZipReconocido -> r.romset.nombre
                is ResultadoArchivo.Fallo, is ResultadoArchivo.ClaveSinDestino -> null // se reintenta la próxima vez
                else -> "-"
            }
            if (valor != null) hechos.edit().putString(a.clave, valor).apply()
        }
        ResultadoCarpeta(carpeta, archivos.size, archivos.size - nuevos.size, resultados)
    }

    private data class Encontrado(val uri: Uri, val nombre: String, val clave: String)

    /** Ya importado y sin cambios (y, si era un juego, su ZIP sigue en la app). */
    private fun yaHecho(a: Encontrado): Boolean {
        val valor = hechos.getString(a.clave, null) ?: return false
        if (valor == "-") return true
        val rs = CatalogoRomsets.porNombre(valor) ?: return false
        return repo.archivo(rs).isFile
    }

    private fun buscar(arbol: Uri, docId: String, nivel: Int): List<Encontrado> {
        val hijos = DocumentsContract.buildChildDocumentsUriUsingTree(arbol, docId)
        val columnas = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val out = mutableListOf<Encontrado>()
        val carpetas = mutableListOf<String>()
        context.contentResolver.query(hijos, columnas, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val nombre = c.getString(1) ?: continue
                val tipo = c.getString(2)
                if (tipo == DocumentsContract.Document.MIME_TYPE_DIR) {
                    carpetas += id
                } else if (nombre.endsWith(".zip", true) || nombre.endsWith(".key", true)) {
                    val tam = if (c.isNull(3)) -1L else c.getLong(3)
                    val fecha = if (c.isNull(4)) -1L else c.getLong(4)
                    out += Encontrado(DocumentsContract.buildDocumentUriUsingTree(arbol, id), nombre, "$id|$tam|$fecha")
                }
            }
        }
        if (nivel < MAX_NIVEL) carpetas.forEach { out += buscar(arbol, it, nivel + 1) }
        return out
    }

    private companion object {
        const val MAX_NIVEL = 3
    }
}
