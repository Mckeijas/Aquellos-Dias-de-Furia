package com.diasdefuria.red

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceView
import kotlin.concurrent.thread

/**
 * Anfitrión: copia la imagen del emulador (PixelCopy sobre la SurfaceView del juego),
 * la comprime en H.264 con el codificador por hardware y entrega cada fotograma
 * al [Anfitrion]. Solo trabaja mientras hay invitados conectados.
 */
class Retransmision(
    private val vista: SurfaceView,
    private val calidad: CalidadRed,
    private val anfitrion: () -> Anfitrion?,
) {
    private val ancho = calidad.ancho
    private val alto = calidad.alto
    @Volatile private var bitrate = calidad.bitrate
    private var ultimoAjuste = 0L
    private var ultimoDescarte = 0L
    private val hilo = HandlerThread("retransmision").apply { start() }
    private val manejador = Handler(hilo.looper)
    private val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    private val destino = Rect(0, 0, ancho, alto)

    private var codificador: MediaCodec? = null
    private var entrada: Surface? = null
    @Volatile private var activa = false
    @Volatile private var copiando = false
    @Volatile private var pausada = false
    private var hiloSalida: Thread? = null
    private var ultimaPeticionClave = 0L
    /** Última vez que el juego avisó de una imagen nueva ([nuevoFotograma]). */
    @Volatile private var ultimoAviso = 0L
    private val capturaUna = Runnable { capturar() }

    fun iniciar() {
        if (activa) return
        val c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        // Primero la configuración de mínima latencia; si el codificador no la acepta, una más sencilla
        val configurado = runCatching { c.configure(formato(c, completo = true), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }.isSuccess ||
            runCatching { c.reset(); c.configure(formato(c, completo = false), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE) }.isSuccess
        if (!configurado) { c.release(); error("codificador H.264 no disponible") }
        entrada = c.createInputSurface()
        c.start()
        codificador = c
        activa = true
        hiloSalida = thread(name = "codificador-salida", isDaemon = true) { leerSalida(c) }
        manejador.post(captura)
    }

    private fun formato(c: MediaCodec, completo: Boolean) =
        MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, ancho, alto).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, Protocolo.FPS_VIDEO)
            // Por TCP no se pierde nada: los fotogramas clave (los más pesados) solo hacen falta al
            // entrar alguien o tras descartar imagen, y entonces se piden. Con uno cada 3 s la wifi
            // recibía un pico cada 3 s y el invitado notaba un tirón periódico.
            // 0.22.1: de 10 s a 5 s, por si el codificador no hace caso cuando se le pide uno
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
            // Prioridad a la latencia: sin fotogramas B y en tiempo real
            setInteger(MediaFormat.KEY_PRIORITY, 0)
            setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
            if (Build.VERSION.SDK_INT >= 30) setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
            if (completo) {
                setInteger("latency", 1) // el codificador entrega cada fotograma sin esperar a los siguientes
                // (0.22.1: fuera la clave «vendor.qti-ext-enc-low-latency.enable» de la 0.21, que no
                // se llegó a probar y es sospechosa de dejar a los invitados sin imagen)
                // Perfil Baseline: el decodificador puede mostrar cada imagen nada más recibirla
                setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline)
                setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel32)
                // Tasa constante: paquetes de tamaño parecido, sin picos que atasquen la red
                val cbr = runCatching {
                    c.codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).encoderCapabilities
                        .isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
                }.getOrDefault(false)
                if (cbr) setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            }
        }

    /**
     * Calidad automática: si la red no da abasto (se descartan fotogramas) se baja el
     * bitrate enseguida; si va holgada, se sube poco a poco.
     */
    private fun ajustarBitrate() {
        val ahora = System.currentTimeMillis()
        if (ahora - ultimoAjuste < 2000) return
        ultimoAjuste = ahora
        val a = anfitrion() ?: return
        val descartes = a.descartes.getAndSet(0)
        val nuevo = when {
            descartes > 0 -> { ultimoDescarte = ahora; (bitrate * 0.7f).toInt() }
            ahora - ultimoDescarte > 10_000 -> (bitrate * 1.1f).toInt()
            else -> bitrate
        }.coerceIn(calidad.bitrateMin, calidad.bitrateMax)
        if (nuevo != bitrate) {
            bitrate = nuevo
            runCatching { codificador?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, nuevo) }) }
        }
    }

    /** Durante el menú de pausa no se envía nada nuevo (los invitados ven la imagen congelada). */
    fun pausar(si: Boolean) { pausada = si }

    fun pedirFotogramaClave() {
        runCatching {
            codificador?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) })
        }
    }

    /**
     * El juego acaba de dibujar una imagen (GLRetroView avisa de cada una): se copia al momento.
     * Antes se copiaba con un reloj propio de 62,5 por segundo, desacompasado con los 60 del
     * juego: de vez en cuando se enviaba una imagen repetida o se saltaba una (tirón), y cada
     * imagen esperaba de media unos 8 ms a que tocase copiarla.
     */
    fun nuevoFotograma() {
        if (!activa) return
        ultimoAviso = SystemClock.uptimeMillis()
        manejador.post(capturaUna)
    }

    private fun capturar() {
        val a = anfitrion()
        if (!activa || pausada || copiando || a?.hayInvitados != true || !vista.holder.surface.isValid) return
        copiando = true
        runCatching {
            PixelCopy.request(vista, bitmap, { res ->
                if (res == PixelCopy.SUCCESS) dibujar()
                copiando = false
            }, manejador)
        }.onFailure { copiando = false }
    }

    /** Reloj de respaldo: ajusta la calidad y, si el juego no avisa de imágenes, copia él. */
    private val captura = object : Runnable {
        override fun run() {
            if (!activa) return
            val ahora = SystemClock.uptimeMillis()
            if (ahora - ultimoAviso > 100) capturar()
            // Mientras algún invitado espera un fotograma completo, se insiste cada medio segundo:
            // hay codificadores que no hacen caso a la primera
            if (ahora - ultimaPeticionClave > 500 && anfitrion()?.alguienEsperaClave == true) {
                ultimaPeticionClave = ahora
                pedirFotogramaClave()
            }
            ajustarBitrate()
            manejador.postDelayed(this, 1000L / Protocolo.FPS_VIDEO)
        }
    }

    private fun dibujar() {
        val s = entrada ?: return
        runCatching {
            val lienzo = if (Build.VERSION.SDK_INT >= 23) s.lockHardwareCanvas() else s.lockCanvas(null)
            lienzo.drawBitmap(bitmap, null, destino, null)
            s.unlockCanvasAndPost(lienzo)
        }
    }

    private fun leerSalida(c: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        var buffer = ByteArray(256 * 1024)
        while (activa) {
            val i = try { c.dequeueOutputBuffer(info, 20_000) } catch (_: Exception) { break }
            if (i < 0) continue
            // Si el codificador se está cerrando, esto lanza una excepción: sin capturarla cerraba la app
            val datos = runCatching { c.getOutputBuffer(i) }.getOrNull() ?: break
            if (datos != null && info.size > 0) {
                if (buffer.size < info.size) buffer = ByteArray(info.size * 2)
                datos.position(info.offset)
                datos.get(buffer, 0, info.size)
                val a = anfitrion()
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    a?.enviarConfig(buffer.copyOf(info.size))
                } else {
                    val clave = info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0
                    a?.enviarFotograma(info.presentationTimeUs, clave, buffer, info.size)
                }
            }
            runCatching { c.releaseOutputBuffer(i, false) }
        }
    }

    fun detener() {
        activa = false
        manejador.removeCallbacksAndMessages(null)
        // Se espera a que el hilo de salida suelte el codificador antes de liberarlo
        runCatching { hiloSalida?.join(300) }
        hiloSalida = null
        runCatching { codificador?.stop() }
        runCatching { codificador?.release() }
        runCatching { entrada?.release() }
        codificador = null
        hilo.quitSafely()
    }
}
