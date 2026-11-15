package com.diasdefuria.juego

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.diasdefuria.datos.DisposicionTactil
import com.diasdefuria.datos.GrupoTactil
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diasdefuria.R
import com.diasdefuria.ui.tema.Colores

/** Aspecto de los controles táctiles (se elige en «Controles»). */
enum class EstiloTactil { TRANSPARENTE, CLASICO }

/** Colores de los controles según el estilo. */
private data class PaletaTactil(
    val fondo: Color,
    val fondoAtaque: Color,
    val pulsado: Color,
    val borde: Color,
    val grosor: Dp,
    val texto: Color,
    val textoPulsado: Color,
    val cruceta: Color,
    val flecha: Color,
    val flechaActiva: Color,
    val centro: Color,
    val menu: Color,
)

private fun paleta(e: EstiloTactil) = when (e) {
    // Botones de cristal: se ve el juego a través, como en los emuladores de móvil
    EstiloTactil.TRANSPARENTE -> PaletaTactil(
        fondo = Color.White.copy(alpha = 0.10f),
        fondoAtaque = Color(0xFFFF6B5B).copy(alpha = 0.16f),
        pulsado = Color.White.copy(alpha = 0.50f),
        borde = Color.White.copy(alpha = 0.75f),
        grosor = 1.5.dp,
        texto = Color.White.copy(alpha = 0.9f),
        textoPulsado = Color.Black.copy(alpha = 0.85f),
        cruceta = Color.White.copy(alpha = 0.07f),
        flecha = Color.White.copy(alpha = 0.65f),
        flechaActiva = Color.White,
        centro = Color.White.copy(alpha = 0.12f),
        menu = Color.Black.copy(alpha = 0.25f),
    )
    EstiloTactil.CLASICO -> PaletaTactil(
        fondo = Colores.PiedraClara.copy(alpha = 0.85f),
        fondoAtaque = Colores.Carmesi.copy(alpha = 0.85f),
        pulsado = Colores.Oro,
        borde = Colores.Oro,
        grosor = 2.dp,
        texto = Colores.OroClaro,
        textoPulsado = Colores.Piedra,
        cruceta = Colores.PiedraClara.copy(alpha = 0.6f),
        flecha = Colores.Oro.copy(alpha = 0.7f),
        flechaActiva = Colores.OroClaro,
        centro = Colores.Piedra.copy(alpha = 0.6f),
        menu = Colores.Piedra.copy(alpha = 0.7f),
    )
}

private val LocalPaleta = staticCompositionLocalOf { paleta(EstiloTactil.TRANSPARENTE) }

fun leerEstiloTactil(context: android.content.Context): EstiloTactil = runCatching {
    EstiloTactil.valueOf(context.getSharedPreferences("ajustes", android.content.Context.MODE_PRIVATE)
        .getString(com.diasdefuria.datos.ClaveAjuste.ESTILO_TACTIL, null) ?: "")
}.getOrDefault(EstiloTactil.TRANSPARENTE)

/**
 * Controles táctiles para jugar sin mando. Los botones envían directamente el
 * botón RetroPad que FBNeo asigna a cada función de la recreativa:
 * Botón 1 (Atacar) = B, Botón 2 (Saltar) = A, Botón 3 (Seleccionar) = Y, Botón 4 (Usar) = X.
 *
 * La posición y el tamaño de cada grupo se ajustan en «Controles». Con [onMover] los
 * controles no juegan: se arrastran para colocarlos (pantalla de edición).
 */
@Composable
fun BoxScope.ControlesTactiles(
    opacidad: Float,
    vibracion: Boolean,
    botones: Int,
    onBoton: (keyCode: Int, pulsado: Boolean) -> Unit,
    onDireccion: (x: Float, y: Float) -> Unit,
    onMenu: () -> Unit,
    disposicion: DisposicionTactil? = null,
    onMover: ((GrupoTactil, Float, Float) -> Unit)? = null,
    estilo: EstiloTactil? = null,
) {
    val context = LocalContext.current
    val estiloActual = estilo ?: remember { leerEstiloTactil(context) }
    CompositionLocalProvider(LocalPaleta provides paleta(estiloActual)) {
        CapaTactil(opacidad, vibracion, botones, onBoton, onDireccion, onMenu, disposicion, onMover)
    }
}

@Composable
private fun BoxScope.CapaTactil(
    opacidad: Float,
    vibracion: Boolean,
    botones: Int,
    onBoton: (keyCode: Int, pulsado: Boolean) -> Unit,
    onDireccion: (x: Float, y: Float) -> Unit,
    onMenu: () -> Unit,
    disposicion: DisposicionTactil?,
    onMover: ((GrupoTactil, Float, Float) -> Unit)?,
) {
    val context = LocalContext.current
    val pal = LocalPaleta.current
    val disp = disposicion ?: remember { DisposicionTactil.cargar(context) }
    val e = disp.escala
    val haptica = LocalHapticFeedback.current
    val vibrar = { if (vibracion) haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    val editando = onMover != null
    val boton: (Int, Boolean) -> Unit = if (editando) { _, _ -> } else onBoton
    val direccion: (Float, Float) -> Unit = if (editando) { _, _ -> } else onDireccion

    BoxWithConstraints(Modifier.fillMaxSize().alpha(if (editando) 1f else opacidad)) {
        val densidad = LocalDensity.current
        val anchoPx = constraints.maxWidth.toFloat()
        val altoPx = constraints.maxHeight.toFloat()
        fun px(d: Dp) = with(densidad) { d.toPx() }

        // Tamaños y posiciones de fábrica (centro de cada grupo, en píxeles)
        val tCruceta = 160.dp * e
        val tAcciones = 190.dp * e
        val anchoCentro = (84.dp * 2 + 16.dp) * e
        val altoCentro = 36.dp * e
        val fabrica = mapOf(
            GrupoTactil.CRUCETA to Offset(px(24.dp) + px(tCruceta) / 2, altoPx - px(24.dp) - px(tCruceta) / 2),
            GrupoTactil.ACCIONES to Offset(anchoPx - px(20.dp) - px(tAcciones) / 2, altoPx - px(18.dp) - px(tAcciones) / 2),
            GrupoTactil.CENTRO to Offset(anchoPx / 2, altoPx - px(14.dp) - px(altoCentro) / 2),
        )

        @Composable
        fun Grupo(grupo: GrupoTactil, ancho: Dp, alto: Dp, contenido: @Composable BoxScope.() -> Unit) {
            val guardado = disp.centros[grupo]
            val w = px(ancho); val h = px(alto)
            val centro = guardado?.let { Offset(it.first * anchoPx, it.second * altoPx) } ?: fabrica.getValue(grupo)
            val x = (centro.x - w / 2).coerceIn(0f, (anchoPx - w).coerceAtLeast(0f))
            val y = (centro.y - h / 2).coerceIn(0f, (altoPx - h).coerceAtLeast(0f))
            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .size(ancho, alto),
            ) {
                contenido()
                if (onMover != null) {
                    // Capa de edición: arrastrar mueve todo el grupo
                    val centroActual by rememberUpdatedState(Offset(x + w / 2, y + h / 2))
                    var actual by remember(grupo) { mutableStateOf(centroActual) }
                    Box(
                        Modifier
                            .matchParentSize()
                            .border(2.dp, Colores.OroClaro.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .pointerInput(grupo, anchoPx, altoPx) {
                                detectDragGestures(onDragStart = { actual = centroActual }) { cambio, arrastre ->
                                    cambio.consume()
                                    actual = Offset(
                                        (actual.x + arrastre.x).coerceIn(w / 2, anchoPx - w / 2),
                                        (actual.y + arrastre.y).coerceIn(h / 2, altoPx - h / 2),
                                    )
                                    onMover(grupo, actual.x / anchoPx, actual.y / altoPx)
                                }
                            },
                    )
                }
            }
        }

        Grupo(GrupoTactil.CRUCETA, tCruceta, tCruceta) {
            Cruceta(tamano = tCruceta, onDireccion = direccion, onCambio = vibrar)
        }

        // Hasta cuatro botones en rombo, como un mando: abajo Golpe, derecha Salto,
        // izquierda Botón 3, arriba Botón 4
        Grupo(GrupoTactil.ACCIONES, tAcciones, tAcciones) {
            val b = 66.dp * e
            BotonTactil(stringResource(R.string.tactil_atacar), KeyEvent.KEYCODE_BUTTON_B, b, boton, vibrar,
                Modifier.align(Alignment.BottomCenter), ataque = true, icono = Icons.Filled.Gavel)
            BotonTactil(stringResource(R.string.tactil_saltar), KeyEvent.KEYCODE_BUTTON_A, b, boton, vibrar,
                Modifier.align(Alignment.CenterEnd), icono = Icons.Filled.KeyboardDoubleArrowUp)
            // Botón 3 (izquierda) y botón 4 (arriba) solo en los juegos que los usan
            if (botones >= 3) {
                BotonTactil(stringResource(R.string.tactil_seleccionar), KeyEvent.KEYCODE_BUTTON_Y, b, boton, vibrar,
                    Modifier.align(Alignment.CenterStart), icono = Icons.Filled.AutoAwesome)
            }
            if (botones >= 4) {
                BotonTactil(stringResource(R.string.tactil_usar), KeyEvent.KEYCODE_BUTTON_X, b, boton, vibrar,
                    Modifier.align(Alignment.TopCenter), icono = Icons.Filled.SwapHoriz)
            }
        }

        // Moneda y Start
        Grupo(GrupoTactil.CENTRO, anchoCentro, altoCentro) {
            Row {
                BotonTactil(stringResource(R.string.tactil_moneda), KeyEvent.KEYCODE_BUTTON_SELECT, 0.dp, boton, vibrar,
                    Modifier.size(width = 84.dp * e, height = altoCentro), forma = RoundedCornerShape(18.dp))
                Spacer(Modifier.width(16.dp * e))
                BotonTactil(stringResource(R.string.tactil_start), KeyEvent.KEYCODE_BUTTON_START, 0.dp, boton, vibrar,
                    Modifier.size(width = 84.dp * e, height = altoCentro), forma = RoundedCornerShape(18.dp))
            }
        }

        // Menú de pausa (fijo arriba a la derecha)
        if (!editando) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .background(pal.menu, CircleShape)
                    .border(pal.grosor, pal.borde, CircleShape)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            if (waitForUpOrCancellation() != null) onMenu()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.pausa_titulo), tint = pal.texto)
            }
        }
    }
}

/**
 * Botón táctil con volumen: sombra, cuerpo con degradado, aro y brillo de cristal.
 * Al pulsarlo se hunde un poco y se ilumina.
 */
@Composable
private fun BotonTactil(
    texto: String,
    keyCode: Int,
    tamano: Dp,
    onBoton: (Int, Boolean) -> Unit,
    vibrar: () -> Unit,
    modifier: Modifier = Modifier,
    ataque: Boolean = false,
    forma: Shape = CircleShape,
    icono: ImageVector? = null,
) {
    val pal = LocalPaleta.current
    var pulsado by remember { mutableStateOf(false) }
    val escala by animateFloatAsState(if (pulsado) 0.92f else 1f, label = "pulsado")
    val base = if (tamano > 0.dp) modifier.size(tamano) else modifier
    val color = if (ataque) pal.fondoAtaque else pal.fondo
    Box(
        base
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .drawBehind {
                val contorno = forma.createOutline(size, layoutDirection, this)
                // Sombra
                translate(top = 3.dp.toPx()) { drawOutline(contorno, Color.Black.copy(alpha = 0.35f)) }
                // Cuerpo: más claro arriba a la izquierda, más oscuro abajo
                val cuerpo = if (pulsado) pal.pulsado else color
                drawOutline(contorno, Brush.radialGradient(
                    listOf(cuerpo.aclarar(0.25f), cuerpo, cuerpo.oscurecer(0.35f)),
                    center = Offset(size.width * 0.35f, size.height * 0.3f),
                    radius = size.maxDimension * 0.85f,
                ))
                // Aro exterior
                drawOutline(contorno, Brush.verticalGradient(listOf(pal.borde, pal.borde.copy(alpha = pal.borde.alpha * 0.4f))),
                    style = Stroke(pal.grosor.toPx() * 1.4f))
                // Brillo de cristal en la mitad de arriba
                drawOval(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = if (pulsado) 0.1f else 0.28f), Color.Transparent),
                        startY = size.height * 0.06f, endY = size.height * 0.5f),
                    topLeft = Offset(size.width * 0.16f, size.height * 0.06f),
                    size = Size(size.width * 0.68f, size.height * 0.42f),
                )
                // Halo al pulsar
                if (pulsado) drawOutline(contorno, pal.flechaActiva.copy(alpha = 0.6f), style = Stroke(3.dp.toPx()))
            }
            .pointerInput(keyCode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pulsado = true
                    vibrar()
                    onBoton(keyCode, true)
                    waitForUpOrCancellation()
                    pulsado = false
                    onBoton(keyCode, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = if (pulsado) pal.textoPulsado else pal.texto,
                    modifier = Modifier.size(if (tamano > 0.dp) tamano * 0.36f else 16.dp))
            }
            Text(
                texto,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.sp,
                    shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1.5f), 2f),
                ),
                color = if (pulsado) pal.textoPulsado else pal.texto,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun Color.aclarar(f: Float) = Color(red + (1 - red) * f, green + (1 - green) * f, blue + (1 - blue) * f, alpha)
private fun Color.oscurecer(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), (alpha + 0.1f).coerceAtMost(1f))

/**
 * Cruceta en cruz, como la de un mando, con relieve. Funciona en 8 direcciones:
 * arrastrando el dedo se cambia de dirección sin levantarlo.
 */
@Composable
private fun Cruceta(
    tamano: Dp,
    onDireccion: (Float, Float) -> Unit,
    onCambio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalPaleta.current
    var dir by remember { mutableStateOf(0 to 0) }
    Box(
        modifier
            .size(tamano)
            .drawBehind {
                val w = size.width
                val brazo = w * 0.34f
                val r = brazo * 0.22f
                val c = w / 2
                // Base circular discreta
                drawCircle(pal.cruceta, radius = w / 2)
                drawCircle(pal.borde.copy(alpha = pal.borde.alpha * 0.35f), radius = w / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                val cruz = Path().apply {
                    addRoundRect(RoundRect(c - brazo / 2, w * 0.06f, c + brazo / 2, w * 0.94f, CornerRadius(r, r)))
                    val h = Path().apply { addRoundRect(RoundRect(w * 0.06f, c - brazo / 2, w * 0.94f, c + brazo / 2, CornerRadius(r, r))) }
                    op(this, h, PathOperation.Union)
                }
                translate(top = 3.dp.toPx()) { drawPath(cruz, Color.Black.copy(alpha = 0.35f)) }
                drawPath(cruz, Brush.verticalGradient(listOf(pal.fondo.aclarar(0.2f), pal.fondo, pal.fondo.oscurecer(0.35f))))
                drawPath(cruz, pal.borde, style = Stroke(pal.grosor.toPx() * 1.3f))
                // Brazo pulsado iluminado y flechas
                val brazos = listOf(
                    (0 to -1) to Rect(c - brazo / 2, w * 0.06f, c + brazo / 2, c - brazo / 2),
                    (0 to 1) to Rect(c - brazo / 2, c + brazo / 2, c + brazo / 2, w * 0.94f),
                    (-1 to 0) to Rect(w * 0.06f, c - brazo / 2, c - brazo / 2, c + brazo / 2),
                    (1 to 0) to Rect(c + brazo / 2, c - brazo / 2, w * 0.94f, c + brazo / 2),
                )
                brazos.forEach { (d, rect) ->
                    val activa = (d.first != 0 && d.first == dir.first) || (d.second != 0 && d.second == dir.second)
                    if (activa) drawRoundRect(pal.pulsado, rect.topLeft, rect.size, CornerRadius(r, r))
                    val centro = rect.center
                    val t = brazo * 0.2f
                    val flecha = Path().apply {
                        when (d) {
                            0 to -1 -> { moveTo(centro.x, centro.y - t); lineTo(centro.x - t, centro.y + t * 0.6f); lineTo(centro.x + t, centro.y + t * 0.6f) }
                            0 to 1 -> { moveTo(centro.x, centro.y + t); lineTo(centro.x - t, centro.y - t * 0.6f); lineTo(centro.x + t, centro.y - t * 0.6f) }
                            -1 to 0 -> { moveTo(centro.x - t, centro.y); lineTo(centro.x + t * 0.6f, centro.y - t); lineTo(centro.x + t * 0.6f, centro.y + t) }
                            else -> { moveTo(centro.x + t, centro.y); lineTo(centro.x - t * 0.6f, centro.y - t); lineTo(centro.x - t * 0.6f, centro.y + t) }
                        }
                        close()
                    }
                    drawPath(flecha, if (activa) pal.textoPulsado else pal.flecha)
                }
                // Hueco central
                drawCircle(Brush.radialGradient(listOf(pal.centro.oscurecer(0.4f), pal.centro), center = Offset(c, c), radius = brazo * 0.32f),
                    radius = brazo * 0.3f, center = Offset(c, c))
                // Brillo
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), startY = w * 0.06f, endY = c),
                    topLeft = Offset(c - brazo / 2 + 3.dp.toPx(), w * 0.07f), size = Size(brazo - 6.dp.toPx(), c - w * 0.07f - brazo / 2),
                    cornerRadius = CornerRadius(r, r),
                )
            }
            .pointerInput(Unit) {
                val radio = size.width / 2f
                fun aplicar(p: Offset) {
                    val dx = p.x - radio
                    val dy = p.y - radio
                    val umbral = radio * 0.25f
                    val nueva = (if (dx > umbral) 1 else if (dx < -umbral) -1 else 0) to
                        (if (dy > umbral) 1 else if (dy < -umbral) -1 else 0)
                    if (nueva != dir) {
                        dir = nueva
                        onCambio()
                        onDireccion(nueva.first.toFloat(), nueva.second.toFloat())
                    }
                }
                awaitEachGesture {
                    val inicio = awaitFirstDown(requireUnconsumed = false)
                    aplicar(inicio.position)
                    while (true) {
                        val evento = awaitPointerEvent()
                        val cambio = evento.changes.firstOrNull { it.id == inicio.id } ?: break
                        if (!cambio.pressed) break
                        aplicar(cambio.position)
                        cambio.consume()
                    }
                    dir = 0 to 0
                    onDireccion(0f, 0f)
                }
            },
    )
}
