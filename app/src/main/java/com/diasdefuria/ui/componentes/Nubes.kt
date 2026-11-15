package com.diasdefuria.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.diasdefuria.R
import kotlin.math.roundToInt

// Zona de cielo animada, en píxeles de la portada (ANCHO_PORTADA × ALTO_PORTADA, ver Portada.kt);
// debe coincidir con herramientas/portada/nubes.py.
private const val CIELO_X0 = 1100f
private const val CIELO_Y0 = 0f
private const val CIELO_X1 = 1460f
private const val CIELO_Y1 = 300f
/** Lo que tardan las nubes en recorrer una vuelta completa de su textura (720 px de portada ≈ 24 px/s). */
private const val VUELTA_NUBES_MS = 30_000L

/**
 * Nubes que cruzan el cielo del fondo de la portada de izquierda a derecha.
 *
 * Solo usa la operación de dibujo más básica (imágenes con transparencia, una encima de otra):
 * 1. nubes_cielo.png: jirones semitransparentes, repetibles en horizontal, que avanzan.
 * 2. nubes_delante.png: la propia portada recortada a la zona del cielo, opaca donde hay
 *    farola, aguja, escalera de incendios o edificios y transparente en el cielo libre.
 *    Se pinta encima de las nubes, así quedan por detrás de todo.
 * Va encima de la imagen de fondo y con su mismo encuadre (encuadrePortada). [sesgoX] es la
 * alineación horizontal de esa imagen: 0 = centrada (intro), -1 = pegada a la izquierda (menú).
 * El tiempo lo lleva un reloj por fotogramas propio, así se mueven aunque las animaciones
 * del sistema estén desactivadas.
 */
@Composable
fun NubesEnMovimiento(modifier: Modifier = Modifier, sesgoX: Float = 0f, visibleMin: Float = 0f) {
    val nubes = ImageBitmap.imageResource(R.drawable.nubes_cielo)
    val delante = ImageBitmap.imageResource(R.drawable.nubes_delante)
    val avance by produceState(0f) {
        val inicio = withFrameNanos { it }
        while (true) {
            withFrameNanos { ahora ->
                value = ((ahora - inicio) / 1_000_000L % VUELTA_NUBES_MS).toFloat() / VUELTA_NUBES_MS
            }
        }
    }
    Canvas(modifier) {
        // Mismo encuadre que la portada que tiene debajo (Portada.kt)
        val e = encuadrePortada(size.width, size.height, sesgoX, visibleMin)
        val escala = e.escala
        val origenX = e.x
        val origenY = e.y
        val izq = (origenX + CIELO_X0 * escala).roundToInt()
        val arriba = (origenY + CIELO_Y0 * escala).roundToInt()
        val ancho = ((CIELO_X1 - CIELO_X0) * escala).roundToInt()
        val alto = ((CIELO_Y1 - CIELO_Y0) * escala).roundToInt()
        val anchoTextura = (nubes.width * escala).roundToInt()
        val desplazado = (avance * anchoTextura).roundToInt()
        clipRect(izq.toFloat(), arriba.toFloat(), (izq + ancho).toFloat(), (arriba + alto).toFloat()) {
            // Dos copias seguidas de la textura: siempre cubren la zona mientras avanza
            for (k in -1..0) {
                drawImage(
                    nubes,
                    dstOffset = IntOffset(izq + desplazado + k * anchoTextura, arriba),
                    dstSize = IntSize(anchoTextura + 1, alto),
                    filterQuality = FilterQuality.Low,
                )
            }
        }
        // Lo que está delante del cielo (farola, aguja, edificios…), recortado de la propia portada
        drawImage(
            delante,
            dstOffset = IntOffset(izq, arriba),
            dstSize = IntSize(ancho, alto),
            filterQuality = FilterQuality.Low,
        )
    }
}
