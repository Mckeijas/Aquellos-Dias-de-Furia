package com.diasdefuria.ui.tema

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.diasdefuria.R

/**
 * Paleta callejera ochentera, sacada del rótulo de la portada: amarillo, naranja y rojo
 * de spray, contorno azul eléctrico, asfalto casi negro y blanco tiza.
 * (Los nombres se conservan del armazón: «Oro» es ahora el amarillo spray, etc.)
 */
object Colores {
    val OroClaro = Color(0xFFFFE57A)      // amarillo claro (foco, texto destacado)
    val Oro = Color(0xFFFFC21F)           // amarillo spray
    val OroOscuro = Color(0xFFFF6A13)     // naranja
    val Carmesi = Color(0xFFE3262E)       // rojo spray
    val CarmesiOscuro = Color(0xFF6B0B12)
    val Piedra = Color(0xFF0E0D12)        // asfalto de noche
    val PiedraClara = Color(0xFF1F1C26)
    val Pergamino = Color(0xFFF2EFE8)     // blanco tiza
    val TextoSuave = Color(0xFFA7A2B2)
    val Azul = Color(0xFF1E69FF)          // halo azul del grafiti
    val Neon = Color(0xFFFF3CC8)          // rosa neón
}

object Pinceles {
    /** Bordes y detalles: degradado del rótulo, de amarillo a rojo. */
    val oro = Brush.verticalGradient(listOf(Colores.OroClaro, Colores.Oro, Colores.OroOscuro, Colores.Carmesi))
    val carmesi = Brush.verticalGradient(listOf(Color(0xFFF0383E), Color(0xFF9E0F18)))
    val piedra = Brush.verticalGradient(listOf(Colores.PiedraClara, Colores.Piedra))
}

/** Títulos y botones: Bangers, rotulación de cómic y recreativa de los 80 (OFL). */
val FuenteTitulo = FontFamily(Font(R.font.bangers_titulo, FontWeight.Normal))

/** Textos: Barlow Condensed, condensada y muy legible, como los rótulos de la calle (OFL). */
val FuenteTexto = FontFamily(
    Font(R.font.barlow_texto, FontWeight.Normal),
    Font(R.font.barlow_negrita, FontWeight.SemiBold),
    Font(R.font.barlow_negrita, FontWeight.Bold),
)

private val tipografia = Typography(
    headlineMedium = TextStyle(fontFamily = FuenteTitulo, fontSize = 32.sp, letterSpacing = 1.2.sp),
    titleLarge = TextStyle(fontFamily = FuenteTitulo, fontSize = 24.sp, letterSpacing = 0.8.sp),
    titleMedium = TextStyle(fontFamily = FuenteTitulo, fontSize = 20.sp, letterSpacing = 0.8.sp),
    bodyLarge = TextStyle(fontFamily = FuenteTexto, fontSize = 18.sp),
    bodyMedium = TextStyle(fontFamily = FuenteTexto, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = FuenteTitulo, fontSize = 18.sp, letterSpacing = 1.0.sp),
    labelMedium = TextStyle(fontFamily = FuenteTexto, fontSize = 14.sp),
)

private val esquema = darkColorScheme(
    primary = Colores.Oro,
    onPrimary = Colores.Piedra,
    secondary = Colores.Carmesi,
    onSecondary = Colores.OroClaro,
    background = Colores.Piedra,
    onBackground = Colores.Pergamino,
    surface = Colores.PiedraClara,
    onSurface = Colores.Pergamino,
    surfaceVariant = Colores.PiedraClara,
    onSurfaceVariant = Colores.TextoSuave,
    outline = Colores.OroOscuro,
)

@Composable
fun TemaMystara(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipografia) {
        // Sin esto, los textos que no están dentro de una Surface salen en negro
        CompositionLocalProvider(LocalContentColor provides Colores.Pergamino, content = contenido)
    }
}
