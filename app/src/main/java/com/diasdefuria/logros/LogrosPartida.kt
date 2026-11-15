package com.diasdefuria.logros

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.diasdefuria.R
import com.diasdefuria.datos.RomsetConocido

/**
 * Logros durante una partida: inicia sesión con el token guardado, identifica el
 * juego, comprueba los logros en cada fotograma y avisa cuando se consigue uno.
 *
 * [jugando] lo controla la pantalla de juego: solo se comprueban logros con el
 * emulador en marcha (no en la pausa ni con la app en segundo plano).
 */
class LogrosPartida(
    context: Context,
    private val romset: RomsetConocido,
    private val nucleo: String,
    /** Mensajes cortos para el aviso de la pantalla de juego (hilo principal). */
    private val alMensaje: (String) -> Unit,
    /** Logro conseguido (hilo principal). */
    private val alLogro: (Logro) -> Unit,
) {
    private val app = context.applicationContext
    private val cuenta = CuentaLogros(app)
    private val principal = Handler(Looper.getMainLooper())
    private var cliente: ClienteLogros? = null
    private var hilo: Thread? = null
    @Volatile private var activo = false
    @Volatile private var juegoCargado = false
    @Volatile var jugando = false
    /** Con trucos activos no se comprueban logros (no sería justo conseguirlos así). */
    @Volatile var trucosActivos = false
        set(v) {
            if (v && !field && juegoCargado) principal.post { alMensaje(app.getString(R.string.ra_pausa_trucos)) }
            field = v
        }

    private var sonidos: SoundPool? = null
    private var sonidoTrofeo = 0

    val habilitado: Boolean get() = ClienteLogros.disponible && cuenta.conectada && cuenta.activados

    /** Se llama cuando el juego ya está dibujando (el núcleo tiene la memoria lista). */
    fun iniciar() {
        if (!habilitado || activo) return
        if (!ClienteLogros.nativoEnlazarNucleo(nucleo)) {
            alMensaje(app.getString(R.string.ra_sin_memoria))
            return
        }
        activo = true
        if (cuenta.sonido) {
            sonidos = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            ).build().also { sonidoTrofeo = it.load(app, R.raw.trofeo, 1) }
        }
        val c = ClienteLogros(object : ClienteLogros.Eventos {
            override fun sesion(ok: Boolean, error: String, usuario: String, nombre: String, token: String, puntos: Int) {
                if (ok) {
                    cuenta.actualizarPuntos(puntos)
                    cliente?.cargarJuego(CuentaLogros.hash(romset))
                } else principal.post { alMensaje(app.getString(R.string.ra_error_sesion, error)) }
            }

            override fun juego(ok: Boolean, error: String, id: Int, titulo: String, conseguidos: Int, total: Int, puntos: Int, puntosTotal: Int) {
                principal.post {
                    when {
                        !ok && id == 0 -> alMensaje(app.getString(R.string.ra_version_no_reconocida, romset.nombre))
                        !ok -> alMensaje(app.getString(R.string.ra_error_juego, error))
                        total == 0 -> alMensaje(app.getString(R.string.ra_sin_logros))
                        else -> {
                            juegoCargado = true
                            alMensaje(app.getString(R.string.ra_juego_cargado, conseguidos, total))
                            if (trucosActivos) alMensaje(app.getString(R.string.ra_pausa_trucos))
                        }
                    }
                }
            }

            override fun logro(l: Logro) {
                principal.post {
                    if (cuenta.sonido) sonidos?.play(sonidoTrofeo, 1f, 1f, 1, 0, 1f)
                    alLogro(l)
                }
            }

            override fun aviso(tipo: Int, mensaje: String) {
                val texto = when (tipo) {
                    ClienteLogros.AVISO_JUEGO_COMPLETADO -> app.getString(R.string.ra_completado)
                    ClienteLogros.AVISO_SIN_CONEXION -> app.getString(R.string.ra_sin_conexion)
                    ClienteLogros.AVISO_RECONECTADO -> app.getString(R.string.ra_reconectado)
                    ClienteLogros.AVISO_ERROR_SERVIDOR -> app.getString(R.string.ra_error_servidor, mensaje)
                    else -> return
                }
                principal.post { alMensaje(texto) }
            }
        })
        cliente = c
        c.iniciarSesionConToken(cuenta.usuario.orEmpty(), cuenta.token.orEmpty())

        // Comprobación de logros a 60 fotogramas por segundo, como la recreativa
        hilo = Thread({
            val periodo = 1_000_000_000L / 60
            var siguiente = System.nanoTime()
            var ultimaEspera = 0L
            while (activo) {
                val ahora = System.nanoTime()
                if (jugando && juegoCargado && !trucosActivos) {
                    c.fotograma()
                } else if (ahora - ultimaEspera > 250_000_000L) {
                    // En pausa: se siguen enviando al servidor los logros pendientes
                    ultimaEspera = ahora
                    c.enEspera()
                }
                siguiente += periodo
                val espera = siguiente - System.nanoTime()
                if (espera > 0) Thread.sleep(espera / 1_000_000, (espera % 1_000_000).toInt())
                else siguiente = System.nanoTime()
            }
        }, "logros-fotogramas").apply { isDaemon = true; start() }
    }

    /** Al salir del juego: se deja de leer la memoria antes de que el emulador se cierre. */
    fun detener() {
        jugando = false
        if (!activo) return
        activo = false
        runCatching { hilo?.join(500) }
        hilo = null
        ClienteLogros.nativoSoltarNucleo()
        cliente?.cerrar()
        cliente = null
        principal.postDelayed({ sonidos?.release(); sonidos = null }, 3000)
    }
}
