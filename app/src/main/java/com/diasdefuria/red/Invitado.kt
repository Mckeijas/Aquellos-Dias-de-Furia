package com.diasdefuria.red

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.view.Surface
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/** Qué le pasa a la conexión del invitado (se notifica en hilos de red). */
sealed interface EstadoInvitado {
    data object Conectando : EstadoInvitado
    data class Dentro(val jugador: Int, val juego: String) : EstadoInvitado
    data class Jugadores(val nombres: List<String>) : EstadoInvitado
    /** Ida y vuelta hasta el anfitrión (ms), se mide cada segundo. */
    data class Ping(val ms: Int) : EstadoInvitado
    /**
     * Retraso medio de la imagen (ms): desde que el anfitrión la copia del juego hasta que
     * el invitado la pone en pantalla (comprimir, enviar y descomprimir). Se mide cada segundo.
     */
    data class Imagen(val ms: Int) : EstadoInvitado
    /** Diagnóstico del vídeo (cada segundo): imágenes recibidas del anfitrión y mostradas en pantalla. */
    data class Video(val recibidas: Int, val mostradas: Int) : EstadoInvitado
    /** El anfitrión ha abierto (true) o cerrado (false) su menú de pausa. */
    data class Pausa(val pausado: Boolean) : EstadoInvitado
    data object Lleno : EstadoInvitado
    data object VersionDistinta : EstadoInvitado
    data object Terminada : EstadoInvitado
    data class Error(val motivo: String) : EstadoInvitado
}

/**
 * Invitado: se conecta al anfitrión, muestra su vídeo en [superficie] (decodificador
 * H.264 por hardware) y le envía los botones.
 */
class ConexionInvitado(
    /** IP o nombre del anfitrión (en la wifi, por internet o por Tailscale/ZeroTier). */
    private val host: String,
    private val puerto: Int,
    private val superficie: Surface,
    private val alCambiar: (EstadoInvitado) -> Unit,
) {
    private var socket: Socket? = null
    private var salida: DataOutputStream? = null
    // Un solo hilo para enviar: mantiene el orden de los botones y no bloquea la interfaz
    private val envio = Executors.newSingleThreadExecutor()
    @Volatile private var activa = true
    @Volatile private var decodificador: MediaCodec? = null
    private var audio: ReproductorAudioRed? = null
    /** Configuración del sonido con la que se creó [audio]. */
    private var configAudio: Triple<Int, Int, List<Byte>>? = null

    /** Un trozo de vídeo recibido, a la espera del decodificador. */
    private class Trozo(val datos: ByteArray, val tiempo: Long, val flags: Int)
    /**
     * El hilo de la red solo recibe y deja aquí la imagen; otro hilo la decodifica. Antes la
     * red esperaba al decodificador y, mientras, no leía nada (ni el sonido): tirones.
     */
    private val colaVideo = java.util.concurrent.LinkedBlockingQueue<Trozo>()
    @Volatile private var esperandoClave = false
    private var ultimaPeticion = 0L
    private var hiloEntrada: Thread? = null
    private var hiloSalida: Thread? = null
    /**
     * Diferencia entre el reloj del anfitrión y el de este dispositivo (ns), calculada con el
     * ping como hace NTP: se queda la del ping más rápido de los últimos, que es la más fiable.
     */
    @Volatile private var desfase: Long? = null
    private val muestrasReloj = ArrayDeque<Pair<Long, Long>>() // (ida y vuelta, desfase)

    // Vídeo: tamaño, última configuración (SPS/PPS) y contadores para el diagnóstico
    private var anchoVideo = 0
    private var altoVideo = 0
    @Volatile private var ultimaConfig: ByteArray? = null
    @Volatile private var recibidas = 0
    @Volatile private var mostradas = 0
    /** Última vez (ms) que se puso una imagen en pantalla (o que se creó el decodificador). */
    @Volatile private var ultimaMostrada = 0L
    @Volatile private var anfitrionPausado = false
    private var ultimoReinicio = 0L

    init {
        thread(name = "invitado", isDaemon = true) { ejecutar() }
    }

    private fun ejecutar() {
        alCambiar(EstadoInvitado.Conectando)
        try {
            val direccion: InetAddress = resolver(host)
            val s = Socket().apply {
                tcpNoDelay = true
                runCatching { trafficClass = Protocolo.CLASE_TRAFICO }
                runCatching { receiveBufferSize = 256 * 1024 }
                connect(InetSocketAddress(direccion, puerto), 8000)
            }
            socket = s
            val ent = DataInputStream(BufferedInputStream(s.getInputStream(), 128 * 1024))
            val sal = DataOutputStream(BufferedOutputStream(s.getOutputStream()))
            salida = sal
            sal.writeByte(Protocolo.HOLA.toInt()); sal.writeInt(Protocolo.VERSION); sal.writeUTF(nombreDispositivo()); sal.flush()
            // Medición del retraso: un ping por segundo
            thread(name = "invitado-ping", isDaemon = true) {
                while (activa) {
                    Thread.sleep(1000)
                    enviar { writeByte(Protocolo.PING.toInt()); writeLong(System.nanoTime()) }
                    if (decodificador != null || anchoVideo > 0) {
                        alCambiar(EstadoInvitado.Video(recibidas, mostradas))
                        // No se pudo rehacer el decodificador: se vuelve a intentar
                        if (decodificador == null) reiniciarDecodificador()
                        // Vigilante: 3 s sin poner ninguna imagen (y el anfitrión no está en pausa)
                        // = la imagen se ha atascado: se pide al anfitrión una completa
                        if (!anfitrionPausado && System.currentTimeMillis() - ultimaMostrada > 3000) atascado()
                    }
                }
            }

            var buffer = ByteArray(256 * 1024)
            while (activa) {
                when (ent.readByte()) {
                    Protocolo.BIENVENIDO -> {
                        val jugador = ent.readInt(); val juego = ent.readUTF()
                        val w = ent.readInt(); val h = ent.readInt()
                        crearDecodificador(w, h)
                        alCambiar(EstadoInvitado.Dentro(jugador, juego))
                    }
                    Protocolo.LLENO -> { alCambiar(EstadoInvitado.Lleno); break }
                    Protocolo.VERSION_DISTINTA -> { alCambiar(EstadoInvitado.VersionDistinta); break }
                    Protocolo.VIDEO_CFG -> {
                        val n = ent.readInt(); if (buffer.size < n) buffer = ByteArray(n * 2)
                        ent.readFully(buffer, 0, n)
                        val cfg = buffer.copyOf(n)
                        ultimaConfig = cfg
                        colaVideo.offer(Trozo(cfg, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG))
                    }
                    Protocolo.VIDEO -> {
                        val t = ent.readLong(); val clave = ent.readBoolean(); val n = ent.readInt()
                        if (buffer.size < n) buffer = ByteArray(n * 2)
                        ent.readFully(buffer, 0, n)
                        recibidas++
                        recibirImagen(buffer, n, t, clave)
                    }
                    Protocolo.JUGADORES -> {
                        val k = ent.readInt(); alCambiar(EstadoInvitado.Jugadores(List(k) { ent.readUTF() }))
                    }
                    Protocolo.AUDIO_CFG -> {
                        val frecuencia = ent.readInt(); val canales = ent.readInt(); val n = ent.readInt()
                        val cfg = ByteArray(n); ent.readFully(cfg)
                        // El anfitrión la repite cada pocos segundos: si es la misma y ya suena, nada que hacer
                        val clave = Triple(frecuencia, canales, cfg.toList())
                        if (audio == null || clave != configAudio) {
                            audio?.cerrar()
                            audio = runCatching { ReproductorAudioRed(frecuencia, canales, cfg) }.getOrNull()
                            configAudio = if (audio != null) clave else null
                        }
                    }
                    Protocolo.AUDIO -> {
                        val t = ent.readLong(); val n = ent.readInt()
                        if (buffer.size < n) buffer = ByteArray(n * 2)
                        ent.readFully(buffer, 0, n)
                        audio?.decodificar(buffer, n, t)
                    }
                    Protocolo.PONG -> {
                        val ahora = System.nanoTime()
                        val marca = ent.readLong(); val horaAnfitrion = ent.readLong()
                        val idaVuelta = ahora - marca
                        muestrasReloj.addLast(idaVuelta to horaAnfitrion - (marca + ahora) / 2)
                        if (muestrasReloj.size > 10) muestrasReloj.removeFirst()
                        desfase = muestrasReloj.minByOrNull { it.first }?.second
                        alCambiar(EstadoInvitado.Ping((idaVuelta / 1_000_000).toInt()))
                    }
                    Protocolo.PAUSA -> {
                        val si = ent.readBoolean()
                        anfitrionPausado = si
                        // Al volver de la pausa el vigilante no debe saltar por la espera
                        if (!si) ultimaMostrada = System.currentTimeMillis()
                        alCambiar(EstadoInvitado.Pausa(si))
                    }
                    Protocolo.FIN -> { alCambiar(EstadoInvitado.Terminada); break }
                    else -> break
                }
            }
        } catch (e: Exception) {
            if (activa) alCambiar(EstadoInvitado.Error(e.localizedMessage ?: e.javaClass.simpleName))
        }
        cerrarRecursos()
    }

    private fun crearDecodificador(w: Int, h: Int) {
        if (decodificador != null) return
        anchoVideo = w; altoVideo = h
        decodificador = nuevoDecodificador()
        ultimaMostrada = System.currentTimeMillis()
        if (hiloEntrada == null) arrancarHilos()
    }

    /** Un decodificador H.264 por hardware que pinta en [superficie]. */
    private fun nuevoDecodificador(): MediaCodec {
        val w = anchoVideo; val h = altoVideo
        fun formato(extra: Boolean) = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
            if (Build.VERSION.SDK_INT >= 30) setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            setInteger(MediaFormat.KEY_PRIORITY, 0)
            if (extra) {
                // Modo baja latencia de los decodificadores Qualcomm (Odin, la mayoría de móviles)
                setInteger("vendor.qti-ext-dec-low-latency.enable", 1)
                setInteger("vendor.qti-ext-dec-picture-order.enable", 0)
            }
        }
        val d = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val ok = runCatching { d.configure(formato(true), superficie, null, 0) }.isSuccess ||
            runCatching { d.reset(); d.configure(formato(false), superficie, null, 0) }.isSuccess
        if (!ok) { d.release(); error("decodificador H.264 no disponible") }
        d.start()
        return d
    }

    /**
     * El decodificador ha dado un error. Antes se seguía usando y ya no pintaba nada en toda la
     * partida (conectado pero sin imagen). Ahora se hace uno nuevo, se le vuelve a dar la
     * configuración y se pide al anfitrión una imagen completa. Como mucho uno por segundo.
     */
    @Synchronized
    private fun reiniciarDecodificador() {
        val ahora = System.currentTimeMillis()
        if (!activa || ahora - ultimoReinicio < 1000) return
        ultimoReinicio = ahora
        val viejo = decodificador
        decodificador = null
        runCatching { viejo?.stop() }
        runCatching { viejo?.release() }
        decodificador = runCatching { nuevoDecodificador() }.getOrNull()
        ultimaMostrada = ahora
        colaVideo.clear()
        ultimaConfig?.let { colaVideo.offer(Trozo(it, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG)) }
        atascado()
    }

    private fun arrancarHilos() {
        hiloEntrada = thread(name = "decodificador-entrada", isDaemon = true) {
            while (activa) {
                // Mientras se rehace el decodificador no se saca nada de la cola (ni la configuración)
                if (decodificador == null) { dormir(10); continue }
                val t: Trozo? = try {
                    colaVideo.poll(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    null
                }
                if (t == null) continue
                if (!decodificar(t.datos, t.datos.size, t.tiempo, t.flags) && t.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                    // Sin hueco en el decodificador: perder una imagen estropea las siguientes,
                    // así que se espera a una completa
                    atascado()
                }
            }
        }
        // Las imágenes ya decodificadas se muestran en cuanto salen (sin cola de espera)
        hiloSalida = thread(name = "decodificador-salida", isDaemon = true) {
            val info = MediaCodec.BufferInfo()
            var sumaRetraso = 0L; var medidas = 0; var ultimoAviso = System.nanoTime()
            while (activa) {
                // Sin decodificador (se está rehaciendo) o con uno que falla: se espera y se sigue.
                // Antes este hilo terminaba y la imagen no volvía en toda la partida.
                val dec = decodificador
                if (dec == null) { dormir(10); continue }
                var i = try { dec.dequeueOutputBuffer(info, 20_000) } catch (_: Exception) { dormir(10); continue }
                if (i < 0) continue
                var pts = info.presentationTimeUs
                // Si la red ha entregado varias imágenes de golpe, solo se muestra la más reciente:
                // las anteriores llegarían tarde y alargarían el retraso
                while (true) {
                    val siguiente = try { dec.dequeueOutputBuffer(info, 0) } catch (_: Exception) { -1 }
                    if (siguiente < 0) break
                    runCatching { dec.releaseOutputBuffer(i, false) }
                    i = siguiente
                    pts = info.presentationTimeUs
                }
                runCatching { dec.releaseOutputBuffer(i, true) }
                mostradas++
                ultimaMostrada = System.currentTimeMillis()
                // Retraso de esta imagen: el anfitrión le pone la hora de su reloj al copiarla
                // (la marca de tiempo del codificador); se pasa a este reloj con el desfase del ping
                val ahora = System.nanoTime()
                desfase?.let { d ->
                    val ms = (ahora + d - pts * 1000) / 1_000_000
                    if (ms in 0..3000) { sumaRetraso += ms; medidas++ }
                }
                if (ahora - ultimoAviso > 1_000_000_000L) {
                    if (medidas > 0) alCambiar(EstadoInvitado.Imagen((sumaRetraso / medidas).toInt()))
                    sumaRetraso = 0; medidas = 0; ultimoAviso = ahora
                }
            }
        }
    }

    /** Una imagen llega de la red: a la cola del decodificador (o fuera, si va atrasado). */
    private fun recibirImagen(datos: ByteArray, n: Int, tiempo: Long, clave: Boolean) {
        if (esperandoClave) {
            if (!clave) return
            esperandoClave = false
        }
        // Más de unas 8 imágenes esperando = el decodificador no da abasto: se tira lo atrasado
        if (colaVideo.size > 8) { atascado(); if (!clave) return; esperandoClave = false }
        colaVideo.offer(Trozo(datos.copyOf(n), tiempo, if (clave) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0))
    }

    /** Se descarta la imagen pendiente y se pide al anfitrión una completa (como mucho dos veces por segundo). */
    private fun atascado() {
        // La configuración (SPS/PPS) no se puede perder
        val cfg = colaVideo.filter { it.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG }
        colaVideo.clear(); cfg.forEach { colaVideo.offer(it) }
        esperandoClave = true
        val ahora = System.currentTimeMillis()
        if (ahora - ultimaPeticion > 500) {
            ultimaPeticion = ahora
            enviar { writeByte(Protocolo.PEDIR_CLAVE.toInt()) }
        }
    }

    /** Devuelve false si el decodificador no tenía hueco (la imagen se pierde). */
    private fun decodificar(datos: ByteArray, n: Int, tiempo: Long, flags: Int): Boolean {
        val d = decodificador ?: return true
        return try {
            val i = d.dequeueInputBuffer(100_000)
            if (i < 0) return false
            d.getInputBuffer(i)?.apply { clear(); put(datos, 0, n) }
            d.queueInputBuffer(i, 0, n, tiempo, flags)
            true
        } catch (_: Exception) {
            // El decodificador ha fallado (p. ej. con una imagen dañada): se rehace
            reiniciarDecodificador()
            true
        }
    }

    // ── Envío de controles ───────────────────────────────────────────────────

    fun boton(tecla: Int, pulsado: Boolean) = enviar {
        writeByte(Protocolo.BOTON.toInt()); writeInt(tecla); writeBoolean(pulsado)
    }

    fun direccion(x: Float, y: Float) = enviar {
        writeByte(Protocolo.DIRECCION.toInt()); writeFloat(x); writeFloat(y)
    }

    private fun enviar(escribir: DataOutputStream.() -> Unit) {
        if (!activa) return
        runCatching {
            envio.execute {
                val s = salida ?: return@execute
                runCatching { s.escribir(); s.flush() }
            }
        }
    }

    fun cerrar() {
        if (!activa) return
        enviar { writeByte(Protocolo.ADIOS.toInt()) }
        runCatching {
            envio.execute {
                activa = false
                runCatching { socket?.close() }
            }
        }
        envio.shutdown()
    }

    /** Espera corta que no tumba la app si el hilo se interrumpe al cerrar la partida. */
    private fun dormir(ms: Long) {
        try { Thread.sleep(ms) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
    }

    private fun cerrarRecursos() {
        activa = false
        audio?.cerrar(); audio = null
        runCatching { socket?.close() }
        // Los hilos del decodificador terminan antes de liberarlo
        runCatching { hiloEntrada?.interrupt(); hiloEntrada?.join(300) }
        runCatching { hiloSalida?.join(300) }
        colaVideo.clear()
        runCatching { decodificador?.stop() }
        runCatching { decodificador?.release() }
        decodificador = null
    }
}
