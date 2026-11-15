package com.diasdefuria.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Ancho (en dp) para el que están pensadas las pantallas: un móvil apaisado normal. */
private const val ANCHO_DISENO = 680f
/** Lo más que se encoge todo en pantallas pequeñas (por debajo, el texto ya no se lee). */
private const val ESCALA_MINIMA = 0.72f

/**
 * Pantallas pequeñas o casi cuadradas (Retroid Pocket Classic, móviles pequeños): si la
 * pantalla mide menos de [ANCHO_DISENO] dp de ancho, todo (textos, botones, márgenes) se
 * dibuja proporcionalmente más pequeño, como si la pantalla fuera de ese ancho. Así las
 * columnas no se estrechan hasta partir el texto letra a letra. En móviles y tabletas
 * normales no cambia nada.
 */
@Composable
fun AjustePantalla(contenido: @Composable () -> Unit) {
    val configuracion = LocalConfiguration.current
    val densidad = LocalDensity.current
    val factor = (configuracion.screenWidthDp / ANCHO_DISENO).coerceIn(ESCALA_MINIMA, 1f)
    if (factor >= 0.999f) {
        contenido()
    } else {
        CompositionLocalProvider(
            LocalDensity provides Density(densidad.density * factor, densidad.fontScale),
            content = contenido,
        )
    }
}
