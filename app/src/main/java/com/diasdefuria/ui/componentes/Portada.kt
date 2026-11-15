package com.diasdefuria.ui.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Tamaño en píxeles de la portada (portada_leyendas.jpg / fondo_menu.jpg) y de sus capas. */
const val ANCHO_PORTADA = 1672f
const val ALTO_PORTADA = 941f

/** Dónde cae la portada en pantalla: escala y esquina superior izquierda, en píxeles. */
data class Encuadre(val escala: Float, val x: Float, val y: Float)

/**
 * Encuadre de la portada en una pantalla de [ancho]×[alto] píxeles.
 *
 * Normalmente cubre toda la pantalla y recorta lo que sobra (como ContentScale.Crop), con
 * [sesgoX] para elegir qué lado se ve: -1 izquierda, 0 centro, 1 derecha. En pantallas casi
 * cuadradas (consolas portátiles como la Retroid Pocket Classic) recortar así se comería
 * media imagen por los lados; con [visibleMin] se garantiza que se vea al menos esa parte
 * del ancho de la imagen, y lo que falte arriba y abajo se rellena con el fondo difuminado.
 */
fun encuadrePortada(ancho: Float, alto: Float, sesgoX: Float = 0f, visibleMin: Float = 0f): Encuadre {
    val cubrir = max(ancho / ANCHO_PORTADA, alto / ALTO_PORTADA)
    val aLoAncho = ancho / ANCHO_PORTADA
    val escala = if (visibleMin <= 0f) cubrir
    else min(cubrir, max(aLoAncho, ancho / (ANCHO_PORTADA * visibleMin)))
    val sobraX = ancho - ANCHO_PORTADA * escala
    return Encuadre(escala, sobraX * (1f + sesgoX) / 2f, (alto - ALTO_PORTADA * escala) / 2f)
}

/** Parte de la imagen que se ve como mínimo en la intro, por ancho, en pantallas estrechas. */
const val VISIBLE_INTRO = 0.92f

/**
 * Una capa del tamaño de la portada (la propia portada, el neón apagado, la farola…) pintada
 * con [encuadrePortada]. Todas las capas con los mismos parámetros caen exactamente encima.
 */
@Composable
fun CapaPortada(
    @DrawableRes imagen: Int,
    modifier: Modifier = Modifier,
    opacidad: Float = 1f,
    sesgoX: Float = 0f,
    visibleMin: Float = 0f,
) {
    val bitmap: ImageBitmap = ImageBitmap.imageResource(imagen)
    Canvas(modifier) {
        if (opacidad <= 0.001f) return@Canvas
        val e = encuadrePortada(size.width, size.height, sesgoX, visibleMin)
        drawImage(
            bitmap,
            dstOffset = IntOffset(e.x.roundToInt(), e.y.roundToInt()),
            dstSize = IntSize((ANCHO_PORTADA * e.escala).roundToInt(), (ALTO_PORTADA * e.escala).roundToInt()),
            alpha = opacidad.coerceIn(0f, 1f),
            filterQuality = FilterQuality.Medium,
        )
    }
}

/**
 * Relleno para las franjas de arriba y abajo cuando la portada no cubre la pantalla:
 * la misma imagen ampliada, difuminada (Android 12 o superior) y oscurecida.
 */
@Composable
fun FondoDifuminado(@DrawableRes imagen: Int, modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(imagen),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(28.dp),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
    }
}
