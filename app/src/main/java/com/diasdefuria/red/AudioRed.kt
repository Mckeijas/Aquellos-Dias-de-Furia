package com.diasdefuria.red

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

/**
 * Anfitrión: copia el sonido del juego (ver cpp/audio_red.c), lo comprime en AAC y
 * se lo pasa al [Anfitrion] para enviarlo a los invitados. El sonido del anfitrión
 * no cambia. Solo trabaja mientras hay invitados conectados.
 */
class AudioRed(
    private val nucleo: String,
    private val calidad: CalidadRed,
    private val anfitrion: () -> Anfitrion?,
) {
    @Volatile private var activo = false
    private var hilo: Thread? = null

    /** Devuelve false si este dispositivo no puede capturar o comprimir el audio. */
    fun iniciar(): Boolean {
        if (activo || !disponible) return activo
        val frecuencia = runCatching { nativoIniciar(nucleo) }.getOrDefault(0)
        if (frecuencia == 0) return false
        val codificador = runCatching { crearCodificador(frecuencia) }.getOrElse {
            Log.w("AudioRed", "Sin codificador AAC", it)
            nativoDetener()
            return false
        }
        activo = true
        hilo = thread(name = "audio-red", isDaemon = true) { bucle(codificador, frecuencia) }
        return true
    }

    private fun crearCodificador(frecuencia: Int): MediaCodec {
        val formato = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, frecuencia, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, if (calidad == CalidadRed.INTERNET) 96_000 else 160_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        return MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(formato, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }
    }

    private fun bucle(c: MediaCodec, frecuencia: Int) {
        val muestras = ShortArray(2048 * 2)
        val info = MediaCodec.BufferInfo()
        var marcosTotales = 0L
        var configEnviada = false
        var ultimoReenvio = System.currentTimeMillis()
        var ultimoSonido = System.currentTimeMillis()
        try {
            while (activo) {
                val a = anfitrion()
                val n = nativoLeer(muestras)
                val ahora = System.currentTimeMillis()
                if (n == 0) {
                    // Más de 1 s sin sonido con invitados: LibretroDroid puede haber quitado el gancho
                    // (o el juego está en pausa: volver a ponerlo no cambia nada)
                    if (a != null && a.hayInvitados && ahora - ultimoSonido > 1000) {
                        runCatching { nativoReenganchar() }; ultimoSonido = ahora
                    }
                    Thread.sleep(5); continue
                }
                ultimoSonido = ahora
                if (a != null && a.hayInvitados && ahora - ultimoReenvio > 3000) {
                    a.reenviarConfigAudio(); ultimoReenvio = ahora
                }
                // Sin invitados no se comprime nada (pero se vacía el búfer)
                if (a == null || !a.hayInvitados) { marcosTotales += n; continue }
                var hecho = 0
                while (hecho < n) {
                    val i = c.dequeueInputBuffer(10_000)
                    if (i < 0) break
                    val buf = c.getInputBuffer(i) ?: break
                    buf.clear()
                    val cabe = minOf(n - hecho, buf.capacity() / 4)
                    val bb = ByteBuffer.allocate(cabe * 4).order(ByteOrder.LITTLE_ENDIAN)
                    bb.asShortBuffer().put(muestras, hecho * 2, cabe * 2)
                    buf.put(bb.array(), 0, cabe * 4)
                    val pts = (marcosTotales + hecho) * 1_000_000L / frecuencia
                    c.queueInputBuffer(i, 0, cabe * 4, pts, 0)
                    hecho += cabe
                }
                marcosTotales += n
                // Lo comprimido sale enseguida hacia los invitados
                while (true) {
                    val o = c.dequeueOutputBuffer(info, 0)
                    if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // Algunos codificadores dan la configuración AAC solo aquí (csd-0) y no como
                        // bloque aparte: sin ella el invitado no puede montar su reproductor y no oye nada
                        if (!configEnviada) runCatching { c.outputFormat.getByteBuffer("csd-0") }.getOrNull()?.let { csd ->
                            val b = ByteArray(csd.remaining()); csd.get(b)
                            if (b.isNotEmpty()) { a.enviarConfigAudio(b, frecuencia, 2); configEnviada = true }
                        }
                        continue
                    }
                    if (o < 0) break
                    val datos = c.getOutputBuffer(o)
                    if (datos != null && info.size > 0) {
                        val b = ByteArray(info.size)
                        datos.position(info.offset); datos.get(b)
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            if (!configEnviada) { a.enviarConfigAudio(b, frecuencia, 2); configEnviada = true }
                        } else a.enviarAudio(info.presentationTimeUs, b)
                    }
                    c.releaseOutputBuffer(o, false)
                }
            }
        } catch (e: Exception) {
            Log.w("AudioRed", "Audio en red detenido", e)
        } finally {
            runCatching { c.stop() }
            runCatching { c.release() }
        }
    }

    /**
     * Devuelve el sonido del núcleo a LibretroDroid. Hay que llamarlo con el núcleo aún
     * cargado (antes de cerrar la partida); si ya no lo está, no toca nada.
     */
    fun detener() {
        if (!activo) return
        activo = false
        runCatching { nativoDetener() }
        runCatching { hilo?.join(300) }
        hilo = null
    }

    /** Tras la pausa o volver de segundo plano: asegura que el sonido sigue llegando a los invitados. */
    fun reenganchar() {
        if (activo) runCatching { nativoReenganchar() }
    }

    companion object {
        val disponible: Boolean = runCatching { System.loadLibrary("logros") }.isSuccess

        @JvmStatic private external fun nativoIniciar(nucleo: String): Int
        @JvmStatic private external fun nativoLeer(destino: ShortArray): Int
        @JvmStatic private external fun nativoDetener()
        @JvmStatic private external fun nativoReenganchar()
    }
}

/**
 * Invitado: descomprime el audio AAC que envía el anfitrión y lo reproduce con el
 * mínimo retraso posible. Si se acumula retraso (la red se atasca), se descarta audio
 * para volver a ir a la par con la imagen.
 */
class ReproductorAudioRed(frecuencia: Int, private val canales: Int, configuracion: ByteArray) {
    private val decodificador: MediaCodec
    private val pista: AudioTrack
    private val frecuencia = frecuencia
    @Volatile private var activo = true
    private var escritos = 0L

    init {
        val formato = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, frecuencia, canales).apply {
            setByteBuffer("csd-0", ByteBuffer.wrap(configuracion))
            setInteger(MediaFormat.KEY_IS_ADTS, 0)
        }
        decodificador = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(formato, null, null, 0)
            start()
        }
        val canal = if (canales == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
        val minimo = AudioTrack.getMinBufferSize(frecuencia, canal, AudioFormat.ENCODING_PCM_16BIT)
        fun crearPista(bajaLatencia: Boolean) = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(frecuencia).setChannelMask(canal)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(minimo, frecuencia * canales * 2 / 10))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .apply { if (bajaLatencia && Build.VERSION.SDK_INT >= 26) setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY) }
            .build()
        // Algunos dispositivos no admiten el modo de baja latencia con esta frecuencia: entonces, el normal
        pista = runCatching { crearPista(true) }.getOrNull()?.takeIf { it.state == AudioTrack.STATE_INITIALIZED }
            ?: crearPista(false)
        pista.play()
    }

    private val hiloSalida = thread(name = "audio-invitado", isDaemon = true) { salida() }

    /** Un bloque AAC recibido del anfitrión. */
    fun decodificar(datos: ByteArray, n: Int, tiempo: Long) {
        if (!activo) return
        runCatching {
            // Espera corta: esto corre en el hilo que lee la red y no debe frenarlo
            val i = decodificador.dequeueInputBuffer(5_000)
            if (i < 0) return
            decodificador.getInputBuffer(i)?.apply { clear(); put(datos, 0, n) }
            decodificador.queueInputBuffer(i, 0, n, tiempo, 0)
        }
    }

    private fun salida() {
        val info = MediaCodec.BufferInfo()
        // Más de 100 ms en cola: se recorta (antes 150; así el sonido no se queda por detrás de la imagen)
        val limite = frecuencia * 10 / 100
        while (activo) {
            val o = try { decodificador.dequeueOutputBuffer(info, 20_000) } catch (_: Exception) { break }
            if (o < 0) continue
            val datos = runCatching { decodificador.getOutputBuffer(o) }.getOrNull()
            if (datos != null && info.size > 0) {
                val enCola = escritos - (pista.playbackHeadPosition.toLong() and 0xFFFFFFFFL)
                if (enCola < limite) {
                    datos.position(info.offset)
                    datos.limit(info.offset + info.size)
                    val n = pista.write(datos, info.size, AudioTrack.WRITE_NON_BLOCKING)
                    if (n > 0) escritos += n / (2 * canales)
                }
            }
            runCatching { decodificador.releaseOutputBuffer(o, false) }
        }
    }

    /** Primero se para el hilo que escribe en la pista; liberarla con él escribiendo cerraba la app. */
    fun cerrar() {
        if (!activo) return
        activo = false
        runCatching { hiloSalida.join(500) }
        runCatching { pista.pause(); pista.flush(); pista.release() }
        runCatching { decodificador.stop() }
        runCatching { decodificador.release() }
    }
}
