package com.diasdefuria.logros

import android.os.Build
import android.util.Log
import com.diasdefuria.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** Un logro conseguido, para el aviso en pantalla. */
data class Logro(val titulo: String, val descripcion: String, val puntos: Int, val imagen: String)

/**
 * Cliente de RetroAchievements (rcheevos en C, ver app/src/main/cpp/logros.c).
 * Los métodos públicos sin «nativo» los llama el código C: no cambiar sus firmas.
 *
 * Todas las llamadas a rcheevos pasan por [cerrojo]: el hilo de fotogramas, las
 * respuestas del servidor y el cierre no se pisan nunca.
 */
class ClienteLogros(private val eventos: Eventos) {

    interface Eventos {
        fun sesion(ok: Boolean, error: String, usuario: String, nombre: String, token: String, puntos: Int) {}
        fun juego(ok: Boolean, error: String, id: Int, titulo: String, conseguidos: Int, total: Int, puntos: Int, puntosTotal: Int) {}
        fun logro(l: Logro) {}
        fun aviso(tipo: Int, mensaje: String) {}
    }

    private val cerrojo = Any()
    private var ptr: Long = nativoCrear(this)
    private val pendientes = AtomicInteger(0)
    private val agente: String by lazy {
        "DiasDeFuria/${BuildConfig.VERSION_NAME} (Android ${Build.VERSION.RELEASE}) " +
            synchronized(cerrojo) { if (ptr != 0L) nativoAgente(ptr) else "" }
    }

    val valido: Boolean get() = ptr != 0L

    fun iniciarSesion(usuario: String, clave: String) = conCliente { nativoSesionClave(it, usuario, clave) }
    fun iniciarSesionConToken(usuario: String, token: String) = conCliente { nativoSesionToken(it, usuario, token) }
    fun cargarJuego(hash: String) = conCliente { nativoCargarJuego(it, hash) }
    fun fotograma() = conCliente { nativoFotograma(it) }
    fun enEspera() = conCliente { nativoEnEspera(it) }

    private inline fun conCliente(f: (Long) -> Unit) {
        synchronized(cerrojo) { if (ptr != 0L) f(ptr) }
    }

    /**
     * Cierra el cliente. Antes espera (en segundo plano) a que terminen las peticiones
     * pendientes, para no perder un logro conseguido justo antes de salir.
     */
    fun cerrar() {
        Thread({
            val limite = System.currentTimeMillis() + 8000
            while (pendientes.get() > 0 && System.currentTimeMillis() < limite) Thread.sleep(100)
            synchronized(cerrojo) {
                if (ptr != 0L) {
                    runCatching { nativoDescargarJuego(ptr) }
                    nativoDestruir(ptr)
                    ptr = 0L
                }
            }
        }, "logros-cierre").start()
    }

    // ── Llamadas desde C ─────────────────────────────────────────────────────

    /** rcheevos pide una petición al servidor: se hace aquí con HTTPS y se le devuelve la respuesta. */
    fun peticion(url: String, post: String?, tipo: String?, pendiente: Long) {
        pendientes.incrementAndGet()
        red.execute {
            val (cuerpo, estado) = http(url, post, tipo)
            synchronized(cerrojo) { if (ptr != 0L) nativoRespuesta(pendiente, cuerpo, estado) }
            pendientes.decrementAndGet()
        }
    }

    fun alIniciarSesion(resultado: Int, error: String, usuario: String, nombre: String, token: String, puntos: Int) =
        seguro { eventos.sesion(resultado == 0, error, usuario, nombre, token, puntos) }

    fun alCargarJuego(resultado: Int, error: String, id: Int, titulo: String, conseguidos: Int, total: Int, puntos: Int, puntosTotal: Int) =
        seguro { eventos.juego(resultado == 0, error, id, titulo, conseguidos, total, puntos, puntosTotal) }

    fun alConseguirLogro(titulo: String, descripcion: String, puntos: Int, imagen: String) =
        seguro { eventos.logro(Logro(titulo, descripcion, puntos, imagen)) }

    fun alAviso(tipo: Int, mensaje: String) = seguro { eventos.aviso(tipo, mensaje) }

    /** Una excepción no puede volver al código C: se registra y se sigue. */
    private inline fun seguro(f: () -> Unit) {
        try { f() } catch (e: Throwable) { Log.e("Logros", "Error en un aviso", e) }
    }

    private fun http(url: String, post: String?, tipo: String?): Pair<ByteArray, Int> = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", agente)
        if (post != null) {
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Content-Type", tipo ?: "application/x-www-form-urlencoded")
            c.outputStream.use { it.write(post.toByteArray()) }
        }
        val estado = c.responseCode
        val flujo = if (estado in 200..399) c.inputStream else c.errorStream
        val cuerpo = flujo?.use { it.readBytes() } ?: ByteArray(0)
        c.disconnect()
        cuerpo to estado
    } catch (e: IOException) {
        Log.w("Logros", "Sin conexión con RetroAchievements: ${e.message}")
        ByteArray(0) to ERROR_REINTENTABLE
    }

    companion object {
        /** Código de rcheevos para «fallo de red, reintentar más tarde». */
        private const val ERROR_REINTENTABLE = -2

        const val AVISO_JUEGO_COMPLETADO = 15
        const val AVISO_ERROR_SERVIDOR = 16
        const val AVISO_SIN_CONEXION = 17
        const val AVISO_RECONECTADO = 18

        private val red = Executors.newCachedThreadPool { r -> Thread(r, "logros-red").apply { isDaemon = true } }

        /** false si la biblioteca nativa no está en el APK (no debería pasar). */
        val disponible: Boolean = runCatching { System.loadLibrary("logros") }.isSuccess

        @JvmStatic private external fun nativoCrear(oyente: Any): Long
        @JvmStatic private external fun nativoDestruir(ptr: Long)
        @JvmStatic private external fun nativoAgente(ptr: Long): String
        @JvmStatic private external fun nativoSesionClave(ptr: Long, usuario: String, clave: String)
        @JvmStatic private external fun nativoSesionToken(ptr: Long, usuario: String, token: String)
        @JvmStatic private external fun nativoCargarJuego(ptr: Long, hash: String)
        @JvmStatic private external fun nativoDescargarJuego(ptr: Long)
        @JvmStatic private external fun nativoFotograma(ptr: Long)
        @JvmStatic private external fun nativoEnEspera(ptr: Long)
        @JvmStatic private external fun nativoRespuesta(pendiente: Long, cuerpo: ByteArray, estado: Int)
        @JvmStatic external fun nativoEnlazarNucleo(nombre: String): Boolean
        @JvmStatic external fun nativoSoltarNucleo()
    }
}
