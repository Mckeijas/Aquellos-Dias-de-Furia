package com.diasdefuria.audio

import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.diasdefuria.R

/**
 * Música de fondo del menú, en bucle.
 *
 * El archivo es res/raw/musica_menu.m4a. Se busca por nombre (no con R.raw) para
 * que el proyecto compile aunque no esté: no se sube a GitHub porque la canción
 * tiene derechos de autor.
 */
object MusicaMenu {
    private var reproductor: MediaPlayer? = null
    private var fundido: ValueAnimator? = null
    private var volumen = 0.7f

    fun disponible(context: Context): Boolean = idRecurso(context) != 0

    private fun idRecurso(context: Context) =
        context.resources.getIdentifier("musica_menu", "raw", context.packageName)

    /** Empieza (o continúa) la música con un fundido de entrada. */
    fun reproducir(context: Context, volumen: Float, retrasoMs: Long = 0) {
        this.volumen = volumen
        val r = reproductor ?: crear(context.applicationContext) ?: return
        if (r.isPlaying) { r.setVolume(volumen, volumen); return }
        r.setVolume(0f, 0f)
        r.start()
        fundir(0f, volumen, 1800, retrasoMs)
    }

    fun pausar() {
        fundido?.cancel()
        reproductor?.takeIf { it.isPlaying }?.pause()
    }

    fun cambiarVolumen(v: Float) {
        volumen = v
        fundido?.cancel()
        reproductor?.setVolume(v, v)
    }

    fun liberar() {
        fundido?.cancel()
        reproductor?.release()
        reproductor = null
    }

    private fun crear(context: Context): MediaPlayer? {
        val id = idRecurso(context)
        if (id == 0) return null
        return runCatching {
            MediaPlayer.create(
                context, id,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
                0,
            ).apply { isLooping = true }
        }.getOrNull()?.also { reproductor = it }
    }

    private fun fundir(desde: Float, hasta: Float, duracionMs: Long, retrasoMs: Long) {
        fundido?.cancel()
        fundido = ValueAnimator.ofFloat(desde, hasta).apply {
            duration = duracionMs
            startDelay = retrasoMs
            addUpdateListener { a ->
                val v = a.animatedValue as Float
                reproductor?.setVolume(v, v)
            }
            start()
        }
    }
}

/** Efectos de sonido cortos. */
object Efectos {
    /** Golpe al pulsar START en la intro: golpe seco en un cubo de basura metálico, con eco (res/raw/punetazo.ogg). */
    fun punetazo(context: Context) {
        runCatching {
            MediaPlayer.create(context.applicationContext, R.raw.punetazo)?.apply {
                setOnCompletionListener { it.release() }
                start()
            }
        }
    }
}

/**
 * Ambiente de la intro: ruido de ciudad (res/raw/ambiente_trafico.ogg, «City noise» de
 * freesound_community en Pixabay, montado en bucle sin corte), a volumen moderado y en bucle. Se apaga
 * al pulsar START.
 */
object AmbienteIntro {
    private const val VOLUMEN = 0.45f
    private var reproductor: MediaPlayer? = null

    fun reproducir(context: Context) {
        val r = reproductor ?: runCatching {
            MediaPlayer.create(
                context.applicationContext, R.raw.ambiente_trafico,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
                0,
            )?.apply { isLooping = true; setVolume(VOLUMEN, VOLUMEN) }
        }.getOrNull()?.also { reproductor = it } ?: return
        if (!r.isPlaying) r.start()
    }

    fun pausar() {
        reproductor?.takeIf { it.isPlaying }?.pause()
    }

    fun parar() {
        reproductor?.release()
        reproductor = null
    }
}
