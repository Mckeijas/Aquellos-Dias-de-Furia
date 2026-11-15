package com.diasdefuria.ui.pantallas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import com.diasdefuria.R
import kotlinx.coroutines.delay

private const val NEGRO_INICIO_MS = 400L
private const val FUNDIDO_ENTRADA_MS = 1100L
private const val LOGO_QUIETO_MS = 1800L
private const val FUNDIDO_SALIDA_MS = 1000L
private const val NEGRO_FINAL_MS = 250L

/**
 * Logo de Patrulla Retro al abrir la app, antes de la intro: negro → fundido al logo →
 * el logo unos segundos → fundido a negro → intro (o menú, si la intro está desactivada).
 * Tocar la pantalla o pulsar un botón lo acorta: hace ya el fundido a negro.
 * Los fundidos van por fotogramas (no con animaciones de Compose), así se ven igual aunque
 * las animaciones del sistema estén desactivadas.
 */
@Composable
fun PantallaLogo(onTerminar: () -> Unit) {
    var opacidad by remember { mutableFloatStateOf(0f) }
    var saltar by remember { mutableStateOf(false) }
    val foco = remember { FocusRequester() }

    suspend fun fundir(hasta: Float, ms: Long) {
        val desde = opacidad
        val inicio = withFrameNanos { it }
        while (true) {
            val t = withFrameNanos { (it - inicio) / 1_000_000f / ms }.coerceIn(0f, 1f)
            opacidad = desde + (hasta - desde) * t
            if (t >= 1f || (saltar && hasta > desde)) break   // al saltar, no termina de aparecer
        }
    }

    LaunchedEffect(Unit) {
        runCatching { foco.requestFocus() }
        delay(NEGRO_INICIO_MS)
        if (!saltar) fundir(1f, FUNDIDO_ENTRADA_MS)
        // Espera con el logo quieto, pero atenta a si se pide saltar
        var esperado = 0L
        while (!saltar && esperado < LOGO_QUIETO_MS) { delay(50); esperado += 50 }
        fundir(0f, if (saltar) 350L else FUNDIDO_SALIDA_MS)
        delay(NEGRO_FINAL_MS)
        onTerminar()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(foco)
            .focusable()
            .onKeyEvent { if (it.type == KeyEventType.KeyUp) { saltar = true; true } else false }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { saltar = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = ImageBitmap.imageResource(R.drawable.logo_patrulla_retro),
            contentDescription = "Patrulla Retro",
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            modifier = Modifier.fillMaxWidth(0.62f).alpha(opacidad),
        )
    }
}
