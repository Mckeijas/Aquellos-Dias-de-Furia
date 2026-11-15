package com.diasdefuria.ui.componentes

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.diasdefuria.datos.Juego
import kotlinx.coroutines.delay

/** Lo que se espera con el juego quieto en la ruleta antes de que arranque su vídeo. */
private const val ESPERA_MS = 1200L

/** ¿Tiene vídeo de demostración? (assets/videos/<id>.mp4) */
fun tieneVideo(context: android.content.Context, juego: Juego): Boolean =
    runCatching { context.assets.openFd("videos/${juego.id}.mp4").close(); true }.getOrDefault(false)

/**
 * Vídeo de demostración del juego, como el «attract mode» de las recreativas: en silencio y en
 * bucle, encima de la portada de la ficha. Arranca cuando el juego lleva un momento elegido
 * (no mientras la ruleta gira) y aparece con un fundido cuando ya tiene la primera imagen.
 * Los vídeos van en assets/videos/<id del juego>.mp4 (ver herramientas/videos.py); si un
 * juego no tiene, se queda la portada.
 */
@Composable
fun VideoDemo(juego: Juego, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hay = remember(juego) { tieneVideo(context, juego) }
    if (!hay) return
    var activo by remember(juego) { mutableStateOf(false) }
    LaunchedEffect(juego) {
        delay(ESPERA_MS)
        activo = true
    }
    if (!activo) return

    var proporcion by remember(juego) { mutableFloatStateOf(4f / 3f) }
    var visible by remember(juego) { mutableStateOf(false) }
    val reproductor = remember(juego) { MediaPlayer() }
    DisposableEffect(juego) {
        onDispose { runCatching { reproductor.release() } }
    }

    // Fondo negro translúcido: la portada se intuye detrás de las bandas negras
    Box(modifier.alpha(if (visible) 1f else 0f).background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            runCatching {
                                ctx.assets.openFd("videos/${juego.id}.mp4").use { fd ->
                                    reproductor.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                                }
                                reproductor.setSurface(Surface(st))
                                reproductor.isLooping = true
                                reproductor.setVolume(0f, 0f)
                                reproductor.setOnVideoSizeChangedListener { _, vw, vh ->
                                    if (vw > 0 && vh > 0) proporcion = vw.toFloat() / vh
                                }
                                reproductor.setOnInfoListener { _, que, _ ->
                                    if (que == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) visible = true
                                    false
                                }
                                reproductor.setOnPreparedListener { it.start() }
                                reproductor.prepareAsync()
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit
                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            runCatching { reproductor.stop() }
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                }
            },
            // El vídeo (horizontal) se ve entero, centrado sobre negro
            modifier = Modifier.fillMaxWidth().aspectRatio(proporcion),
        )
    }
}
