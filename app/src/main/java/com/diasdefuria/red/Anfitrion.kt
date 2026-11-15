package com.diasdefuria.red

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Servidor de la partida. Acepta hasta 3 invitados, les asigna los puestos de
 * jugador 2, 3 y 4, les reenvía el vídeo y entrega sus botones al emulador.
 *
 * [alPulsar] y [alMover] llegan desde los hilos de red; GLRetroView ya encola los
 * eventos en su propio hilo, así que se pueden pasar directamente.
 */
class Anfitrion(
    private val juego: String,
    private val calidad: CalidadRed,
    private val alPulsar: (puerto: Int, tecla: Int, pulsado: Boolean) -> Unit,
    private val alMover: (puerto: Int, x: Float, y: Float) -> Unit,
    private val alCambiarJugadores: (List<String>) -> Unit,
    private val pedirFotogramaClave: () -> Unit,
) {
    /** Puerto fijo si está libre (así se puede abrir en el router); si no, cualquiera. */
    private val servidor = runCatching {
        ServerSocket().apply { reuseAddress = true; bind(java.net.InetSocketAddress(Protocolo.PUERTO)) }
    }.getOrElse { ServerSocket(0) }
    val puerto: Int get() = servidor.localPort

    @Volatile private var activo = true
    private val invitados = arrayOfNulls<Invitado>(Protocolo.MAX_JUGADORES - 1) // puestos 2..4
    @Volatile private var configVideo: ByteArray? = null
    @Volatile private var configAudio: ByteArray? = null
    /** El anfitrión tiene la partida en pausa (se avisa también a quien entra en ese momento). */
    @Volatile private var pausado = false
    /** Fotogramas que no cupieron en la red (la retransmisión baja la calidad si crece). */
    val descartes = java.util.concurrent.atomic.AtomicInteger(0)

    /**
     * Un invitado conectado, con tres colas de salida por orden de prioridad:
     * 1. control (bienvenida, configuración, jugadores, pong): nunca se descarta;
     * 2. sonido: si se atasca se tira el más viejo (unos 0,4 s como mucho);
     * 3. vídeo: si la wifi va lenta se descartan fotogramas en lugar de acumular retraso.
     * Antes todo iba en una sola cola corta: el sonido le quitaba sitio al vídeo, y al
     * vaciarla por un atasco se podía perder la configuración del sonido (el invitado se
     * quedaba sin audio toda la partida).
     */
    private inner class Invitado(val socket: Socket, val jugador: Int, val nombre: String, val salida: DataOutputStream) {
        val control = java.util.concurrent.LinkedBlockingQueue<ByteArray>()
        val sonido = ArrayBlockingQueue<ByteArray>(20)
        val video = ArrayBlockingQueue<ByteArray>(calidad.cola)
        private val aviso = java.util.concurrent.Semaphore(0)
        @Volatile var esperandoClave = true
        val emisor = thread(name = "envio-j$jugador", isDaemon = true) {
            try {
                while (activo && !socket.isClosed) {
                    aviso.tryAcquire(500, TimeUnit.MILLISECONDS)
                    aviso.drainPermits()
                    var algo = false
                    // Todo lo urgente y el sonido pendiente; después un fotograma, y vuelta a empezar
                    while (true) {
                        val p = control.poll() ?: sonido.poll() ?: break
                        salida.write(p); algo = true
                    }
                    val f = video.poll()
                    if (f != null) { salida.write(f); algo = true }
                    if (algo) salida.flush()
                    if (f != null && (video.isNotEmpty() || control.isNotEmpty() || sonido.isNotEmpty())) aviso.release()
                }
            } catch (_: Exception) {
            }
            desconectar(this)
        }

        fun enviarControl(paquete: ByteArray) { control.offer(paquete); aviso.release() }

        fun enviarSonido(paquete: ByteArray) {
            if (!sonido.offer(paquete)) { sonido.poll(); sonido.offer(paquete) }
            aviso.release()
        }

        fun enviarVideo(paquete: ByteArray, esClave: Boolean) {
            if (esperandoClave && !esClave) return
            if (!video.offer(paquete)) {
                // Cola llena: se vacía y se espera al siguiente fotograma clave
                video.clear(); esperandoClave = true; descartes.incrementAndGet(); pedirFotogramaClave()
                return
            }
            if (esClave) esperandoClave = false
            aviso.release()
        }
    }

    init {
        thread(name = "anfitrion-aceptar", isDaemon = true) {
            while (activo) {
                val s = try { servidor.accept() } catch (_: SocketException) { break } catch (_: Exception) { continue }
                thread(name = "anfitrion-cliente", isDaemon = true) { atender(s) }
            }
        }
    }

    private fun atender(s: Socket) {
        var yo: Invitado? = null
        try {
            s.tcpNoDelay = true
            // Poco búfer de envío: mejor descartar un fotograma que acumular retraso en el sistema
            // (lo que espera en el sistema es retraso que ya no se puede descartar)
            runCatching { s.sendBufferSize = if (calidad == CalidadRed.INTERNET) 48 * 1024 else 64 * 1024 }
            runCatching { s.trafficClass = Protocolo.CLASE_TRAFICO }
            val entrada = DataInputStream(BufferedInputStream(s.getInputStream()))
            val salida = DataOutputStream(BufferedOutputStream(s.getOutputStream(), 64 * 1024))
            if (entrada.readByte() != Protocolo.HOLA) { s.close(); return }
            val version = entrada.readInt()
            val nombre = entrada.readUTF().take(24)
            if (version != Protocolo.VERSION) {
                salida.writeByte(Protocolo.VERSION_DISTINTA.toInt()); salida.flush(); s.close(); return
            }
            val hueco = synchronized(invitados) {
                val i = invitados.indexOfFirst { it == null }
                if (i < 0) null else Invitado(s, i + 2, nombre, salida).also { invitados[i] = it }
            }
            if (hueco == null) {
                salida.writeByte(Protocolo.LLENO.toInt()); salida.flush(); s.close(); return
            }
            yo = hueco
            hueco.enviarControl(paquete {
                writeByte(Protocolo.BIENVENIDO.toInt()); writeInt(hueco.jugador); writeUTF(juego)
                writeInt(calidad.ancho); writeInt(calidad.alto)
            })
            configVideo?.let { hueco.enviarControl(paqueteConfig(it)) }
            configAudio?.let { hueco.enviarControl(it) }
            if (pausado) hueco.enviarControl(paquetePausa(true))
            pedirFotogramaClave()
            avisarJugadores()

            // Bucle de entrada: botones del invitado → su puerto del emulador (0 = anfitrión)
            val puertoEmu = hueco.jugador - 1
            while (activo) {
                when (entrada.readByte()) {
                    Protocolo.BOTON -> { val t = entrada.readInt(); val p = entrada.readBoolean(); alPulsar(puertoEmu, t, p) }
                    Protocolo.DIRECCION -> { val x = entrada.readFloat(); val y = entrada.readFloat(); alMover(puertoEmu, x, y) }
                    Protocolo.PING -> {
                        val marca = entrada.readLong()
                        // Sin esperar: el bucle de botones no se puede parar nunca
                        hueco.enviarControl(paquete { writeByte(Protocolo.PONG.toInt()); writeLong(marca); writeLong(System.nanoTime()) })
                    }
                    Protocolo.PEDIR_CLAVE -> {
                        // Al invitado se le ha atascado la imagen: necesita un fotograma completo
                        // (y la configuración, por si su decodificador se ha rehecho)
                        hueco.esperandoClave = true; hueco.video.clear()
                        configVideo?.let { hueco.enviarControl(paqueteConfig(it)) }
                        descartes.incrementAndGet(); pedirFotogramaClave()
                    }
                    Protocolo.ADIOS -> break
                    else -> break
                }
            }
        } catch (_: Exception) {
        }
        yo?.let { desconectar(it) } ?: runCatching { s.close() }
    }

    private fun desconectar(i: Invitado) {
        val quitado = synchronized(invitados) {
            val idx = invitados.indexOf(i)
            if (idx >= 0) { invitados[idx] = null; true } else false
        }
        runCatching { i.socket.close() }
        if (quitado) {
            // Suelta todo lo que tuviera pulsado para que su personaje no siga andando
            alMover(i.jugador - 1, 0f, 0f)
            TECLAS_RETROPAD.forEach { alPulsar(i.jugador - 1, it, false) }
            avisarJugadores()
        }
    }

    private fun avisarJugadores() {
        val nombres = synchronized(invitados) { invitados.mapNotNull { it?.let { "J${it.jugador} · ${it.nombre}" } } }
        alCambiarJugadores(nombres)
        val p = paquete {
            writeByte(Protocolo.JUGADORES.toInt()); writeInt(nombres.size); nombres.forEach { writeUTF(it) }
        }
        lista().forEach { it.enviarControl(p) }
    }

    private fun lista() = synchronized(invitados) { invitados.filterNotNull() }

    val hayInvitados: Boolean get() = lista().isNotEmpty()

    /** Algún invitado no puede mostrar nada hasta recibir un fotograma completo. */
    val alguienEsperaClave: Boolean get() = lista().any { it.esperandoClave }

    // ── Vídeo (desde el codificador) ─────────────────────────────────────────

    fun enviarConfig(datos: ByteArray) {
        configVideo = datos
        val p = paqueteConfig(datos)
        lista().forEach { it.enviarControl(p) }
    }

    fun enviarFotograma(tiempoUs: Long, clave: Boolean, datos: ByteArray, longitud: Int) {
        val invitados = lista()
        if (invitados.isEmpty()) return
        val p = paquete {
            writeByte(Protocolo.VIDEO.toInt()); writeLong(tiempoUs); writeBoolean(clave)
            writeInt(longitud); write(datos, 0, longitud)
        }
        invitados.forEach { it.enviarVideo(p, clave) }
    }

    // ── Audio (desde AudioRed) ───────────────────────────────────────────────

    fun enviarConfigAudio(datos: ByteArray, frecuencia: Int, canales: Int) {
        val p = paquete {
            writeByte(Protocolo.AUDIO_CFG.toInt()); writeInt(frecuencia); writeInt(canales)
            writeInt(datos.size); write(datos)
        }
        configAudio = p
        lista().forEach { it.enviarControl(p) }
    }

    /**
     * Vuelve a mandar la configuración del sonido a todos (cada pocos segundos). El invitado
     * la ignora si ya la tiene; si se le perdió, así recupera el sonido sin reconectar.
     */
    fun reenviarConfigAudio() {
        val p = configAudio ?: return
        lista().forEach { it.enviarControl(p) }
    }

    fun enviarAudio(tiempoUs: Long, datos: ByteArray) {
        val invitados = lista()
        if (invitados.isEmpty()) return
        val p = paquete {
            writeByte(Protocolo.AUDIO.toInt()); writeLong(tiempoUs); writeInt(datos.size); write(datos)
        }
        // Si la red va justa, el audio se pierde antes que acumular retraso
        invitados.forEach { it.enviarSonido(p) }
    }

    /** Avisa a los invitados de que el anfitrión ha pausado o ha vuelto a la partida. */
    fun avisarPausa(si: Boolean) {
        if (pausado == si) return
        pausado = si
        val p = paquetePausa(si)
        lista().forEach { it.enviarControl(p) }
    }

    private fun paquetePausa(si: Boolean) = paquete { writeByte(Protocolo.PAUSA.toInt()); writeBoolean(si) }

    private fun paqueteConfig(d: ByteArray) = paquete {
        writeByte(Protocolo.VIDEO_CFG.toInt()); writeInt(d.size); write(d)
    }

    fun cerrar() {
        if (!activo) return
        val p = paquete { writeByte(Protocolo.FIN.toInt()) }
        lista().forEach { it.video.clear(); it.sonido.clear(); it.enviarControl(p) }
        Thread.sleep(150) // da tiempo a que salga el aviso de fin
        activo = false
        runCatching { servidor.close() }
        lista().forEach { runCatching { it.socket.close() } }
    }
}

/** Botones RetroPad que puede enviar un invitado (códigos de Android, como espera LibretroDroid). */
internal val TECLAS_RETROPAD = listOf(
    android.view.KeyEvent.KEYCODE_BUTTON_A, android.view.KeyEvent.KEYCODE_BUTTON_B,
    android.view.KeyEvent.KEYCODE_BUTTON_X, android.view.KeyEvent.KEYCODE_BUTTON_Y,
    android.view.KeyEvent.KEYCODE_BUTTON_L1, android.view.KeyEvent.KEYCODE_BUTTON_R1,
    android.view.KeyEvent.KEYCODE_BUTTON_L2, android.view.KeyEvent.KEYCODE_BUTTON_R2,
    android.view.KeyEvent.KEYCODE_BUTTON_SELECT, android.view.KeyEvent.KEYCODE_BUTTON_START,
)

/** Construye un paquete en memoria para enviarlo de una vez. */
internal inline fun paquete(escribir: DataOutputStream.() -> Unit): ByteArray {
    val b = java.io.ByteArrayOutputStream(64)
    DataOutputStream(b).apply(escribir).flush()
    return b.toByteArray()
}
