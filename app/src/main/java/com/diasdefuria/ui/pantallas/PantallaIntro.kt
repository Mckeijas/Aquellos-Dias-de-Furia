package com.diasdefuria.ui.pantallas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diasdefuria.R
import com.diasdefuria.audio.Efectos
import com.diasdefuria.ui.componentes.NubesEnMovimiento
import com.diasdefuria.ui.componentes.CapaPortada
import com.diasdefuria.ui.componentes.FondoDifuminado
import com.diasdefuria.ui.componentes.VISIBLE_INTRO
import com.diasdefuria.ui.tema.Colores
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.BotonMystara
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.activity.compose.BackHandler
import kotlin.random.Random
import com.diasdefuria.audio.AmbienteIntro
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.DisposableEffect

/**
 * Intro antes del menú.
 *
 * ESPACIO RESERVADO: de momento muestra la portada con un fundido. Cuando llegue el
 * material definitivo (vídeo, animación o secuencia de imágenes) se sustituye el
 * contenido de esta pantalla; el resto de la app no cambia.
 * Sin música: de fondo suena bajito el tráfico de la ciudad y el neón «ARCADE / VIDEO GAMES»
 * parpadea de forma irregular (cada tubo por su cuenta). Espera a que se pulse START (no pasa
 * sola al menú). Al tocar la pantalla o pulsar cualquier botón del mando se apaga el ambiente,
 * suena un golpe seco en un cubo de basura, la imagen da un golpe (destello y temblor) y se pasa al menú, donde
 * empieza la música.
 *
 * Las nubes del cielo del fondo se desplazan despacio (ui/componentes/Nubes.kt).
 *
 * El neón apagado son dos capas PNG del mismo tamaño que la portada (neon_arcade_apagado.png
 * y neon_video_apagado.png, transparentes salvo los tubos y su reflejo): se dibujan encima con
 * el mismo encuadre y su opacidad hace de interruptor. Se generan con herramientas/portada/neon.py.
 */
@Composable
fun PantallaIntro(onTerminar: (pulsado: Boolean) -> Unit, onSalir: () -> Unit = {}) {
    val context = LocalContext.current
    val opacidad = remember { Animatable(0f) }
    val destello = remember { Animatable(0f) }
    val temblor = remember { Animatable(0f) }
    val foco = remember { FocusRequester() }
    var pulsado by remember { mutableStateOf(false) }
    // Diálogo de salir: 0 = Sí, 1 = No (elegido de entrada). Lo lleva la portada y no el foco de
    // Compose: si el foco no llegaba a los botones, con mando o teclado no se podía elegir nada
    var eleccionSalida by remember { mutableIntStateOf(1) }
    val focoSi = remember { FocusRequester() }
    val focoNo = remember { FocusRequester() }
    // B / Atrás en la intro: «¿Realmente quieres salir de la aplicación?»
    var preguntarSalida by remember { mutableStateOf(false) }
    BackHandler(enabled = !preguntarSalida && !pulsado) { preguntarSalida = true; eleccionSalida = 1 }
    // 0 = tubo encendido, 1 = apagado (opacidad de la capa «apagado» encima de la portada)
    var arcadeApagado by remember { mutableFloatStateOf(0f) }
    var videoApagado by remember { mutableFloatStateOf(0f) }

    // Ambiente de tráfico: suena mientras se ve la intro (se pausa si la app pasa a segundo plano)
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> if (!pulsado) AmbienteIntro.reproducir(context)
                Lifecycle.Event.ON_STOP -> AmbienteIntro.pausar()
                else -> Unit
            }
        }
        if (ciclo.currentState.isAtLeast(Lifecycle.State.STARTED)) AmbienteIntro.reproducir(context)
        ciclo.addObserver(observador)
        onDispose {
            ciclo.removeObserver(observador)
            AmbienteIntro.parar()
        }
    }

    // Neón con fallos: cada tubo parpadea por su cuenta y a su ritmo
    LaunchedEffect(Unit) { neonIrregular(Random(7), nervio = 1f) { arcadeApagado = it } }
    LaunchedEffect(Unit) { neonIrregular(Random(31), nervio = 0.6f) { videoApagado = it } }
    // Farola: luz cálida que respira y, de vez en cuando, un leve tintineo
    var farolaLuz by remember { mutableFloatStateOf(0.5f) }
    var farolaApagada by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { farolaTitilante(Random(11), { farolaLuz = it }, { farolaApagada = it }) }

    LaunchedEffect(Unit) {
        runCatching { foco.requestFocus() }
        opacidad.animateTo(1f, tween(1400))
        // Se queda aquí hasta que se pulse START: no pasa sola al menú
    }

    // Golpe al pulsar START: puñetazo + destello + temblor, y luego al menú
    LaunchedEffect(pulsado) {
        if (!pulsado) return@LaunchedEffect
        AmbienteIntro.parar()
        Efectos.punetazo(context)
        destello.snapTo(0.85f)
        temblor.snapTo(1f)
        coroutineScope {
            launch { destello.animateTo(0f, tween(260)) }
            launch { temblor.animateTo(0f, tween(320)) }
        }
        delay(120)
        onTerminar(true)
    }
    val pulsar = { pulsado = true }

    val parpadeo by rememberInfiniteTransition(label = "parpadeo").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alfa",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Colores.Piedra)
            // Con el diálogo de salir abierto, la cruceta, A y Enter van al diálogo estén donde estén
            .onPreviewKeyEvent { e ->
                if (!preguntarSalida) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionLeft, Key.DirectionUp -> {
                        if (e.type == KeyEventType.KeyDown) { eleccionSalida = 0; runCatching { focoSi.requestFocus() } }
                        true
                    }
                    Key.DirectionRight, Key.DirectionDown -> {
                        if (e.type == KeyEventType.KeyDown) { eleccionSalida = 1; runCatching { focoNo.requestFocus() } }
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.ButtonA, Key.Spacebar -> {
                        if (e.type == KeyEventType.KeyUp) {
                            if (eleccionSalida == 0) onSalir()
                            else { preguntarSalida = false; runCatching { foco.requestFocus() } }
                        }
                        true
                    }
                    else -> false // Atrás / B: lo atiende el BackHandler del diálogo (= No)
                }
            }
            .focusRequester(foco)
            .focusable()
            .onKeyEvent { e ->
                when {
                    preguntarSalida -> false   // el diálogo atiende sus teclas
                    // B (llega como Atrás) o Escape: preguntar si se sale de la app
                    e.key == Key.Back || e.key == Key.Escape -> {
                        if (e.type == KeyEventType.KeyUp) { preguntarSalida = true; eleccionSalida = 1 }
                        true
                    }
                    e.type == KeyEventType.KeyUp -> { pulsar(); true }
                    else -> false
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = pulsar,
            )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .alpha(opacidad.value)
                .graphicsLayer {
                    // temblor seco: oscila y se apaga
                    val t = temblor.value
                    translationX = 18f * t * kotlin.math.sin(t * 40f)
                    translationY = 10f * t * kotlin.math.cos(t * 33f)
                    scaleX = 1f + 0.04f * t
                    scaleY = 1f + 0.04f * t
                },
        ) {
            // Encuadre adaptable (Portada.kt): en pantallas casi cuadradas se ve casi toda la
            // ciudad, algo desplazada a la derecha para no perder el neón, y arriba y abajo el
            // fondo difuminado
            FondoDifuminado(R.drawable.portada_leyendas, Modifier.fillMaxSize())
            CapaPortada(R.drawable.portada_leyendas, Modifier.fillMaxSize(), sesgoX = SESGO_INTRO, visibleMin = VISIBLE_INTRO)
            NubesEnMovimiento(Modifier.fillMaxSize(), sesgoX = SESGO_INTRO, visibleMin = VISIBLE_INTRO)  // ui/componentes/Nubes.kt
            // Mismo encuadre que la portada: las capas caen justo encima de los tubos y de la farola
            CapaPortada(R.drawable.neon_arcade_apagado, Modifier.fillMaxSize(), arcadeApagado, SESGO_INTRO, VISIBLE_INTRO)
            CapaPortada(R.drawable.neon_video_apagado, Modifier.fillMaxSize(), videoApagado, SESGO_INTRO, VISIBLE_INTRO)
            // Farola (herramientas/portada/farola.py): luz extra en el foco y en la acera mojada,
            // y encima la farola apagada para los bajones de tensión
            CapaPortada(R.drawable.farola_luz, Modifier.fillMaxSize(), farolaLuz, SESGO_INTRO, VISIBLE_INTRO)
            CapaPortada(R.drawable.farola_apagada, Modifier.fillMaxSize(), farolaApagada, SESGO_INTRO, VISIBLE_INTRO)
        }
        Box(Modifier.fillMaxSize().alpha(destello.value).background(Color.White))
        Text(
            text = stringResource(R.string.intro_continuar),
            style = MaterialTheme.typography.titleMedium,
            color = Colores.OroClaro,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .alpha(parpadeo * opacidad.value),
        )
        if (preguntarSalida) {
            DialogoSalir(
                eleccion = eleccionSalida,
                focoSi = focoSi,
                focoNo = focoNo,
                onSi = onSalir,
                onNo = { preguntarSalida = false; runCatching { foco.requestFocus() } },
            )
        }
    }
}

/** «¿Realmente quieres salir de la aplicación?» con Sí / No (No elegido de entrada). */
@Composable
private fun DialogoSalir(eleccion: Int, focoSi: FocusRequester, focoNo: FocusRequester, onSi: () -> Unit, onNo: () -> Unit) {
    LaunchedEffect(Unit) { runCatching { focoNo.requestFocus() } }
    BackHandler(onBack = onNo)
    Box(
        Modifier
            .fillMaxSize()
            .background(Colores.Piedra.copy(alpha = 0.6f))
            // Tocar fuera no hace nada (ni pasa al menú)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .background(Colores.Piedra.copy(alpha = 0.95f), FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                stringResource(R.string.salir_pregunta),
                style = MaterialTheme.typography.titleLarge,
                color = Colores.OroClaro,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // El marco claro señala la opción elegida aunque el foco no esté en el botón
                val marco = { i: Int ->
                    Modifier.width(130.dp)
                        .border(3.dp, if (eleccion == i) Colores.OroClaro else Color.Transparent, FormaPlaca)
                }
                BotonMystara(stringResource(R.string.salir_si), onSi, marco(0).focusRequester(focoSi), estilo = EstiloBoton.DISCRETO)
                BotonMystara(stringResource(R.string.salir_no), onNo, marco(1).focusRequester(focoNo),
                    estilo = EstiloBoton.PRINCIPAL)
            }
        }
    }
}


/**
 * Parpadeo de un tubo de neón viejo: arranca a trompicones en cuanto se abre la app y luego
 * está casi siempre encendido, pero cada pocos segundos
 * falla de una de tres maneras, al azar: una ráfaga de chispazos, un apagón que vuelve
 * con tartamudeo o un bajón de intensidad. [nervio] < 1 = falla menos a menudo.
 */
private suspend fun neonIrregular(rnd: Random, nervio: Float, poner: (Float) -> Unit) {
    fun entre(a: Int, b: Int) = rnd.nextInt(a, b).toLong()
    // Arranque nada más abrirse la app: el tubo empieza apagado y se enciende a trompicones
    poner(1f)
    delay(entre(0, 150))
    repeat(rnd.nextInt(3, 7)) {
        poner(if (rnd.nextFloat() < 0.6f) 0f else 0.5f); delay(entre(30, 120))
        poner(1f); delay(entre(40, 220))
    }
    var primera = true
    while (true) {
        poner(0f)
        // Tras el arranque vuelve a fallar enseguida; luego, cada pocos segundos
        delay(if (primera) entre(300, 1200) else (entre(1200, 6500) / nervio).toLong())
        primera = false
        val tipo = rnd.nextFloat()
        when {
            tipo < 0.55f -> repeat(rnd.nextInt(2, 8)) { // ráfaga de chispazos
                poner(if (rnd.nextFloat() < 0.7f) 1f else 0.6f)
                delay(entre(25, 110))
                poner(0f)
                delay(entre(30, 170))
            }
            tipo < 0.8f -> { // apagón y vuelve tartamudeando
                poner(1f)
                delay(entre(250, 1400))
                repeat(rnd.nextInt(1, 4)) {
                    poner(0f); delay(entre(35, 90))
                    poner(1f); delay(entre(40, 130))
                }
            }
            else -> { // bajón de tensión
                poner(0.45f)
                delay(entre(80, 320))
            }
        }
    }
}

/** En pantallas estrechas la intro se desplaza hacia la derecha: el neón queda a la vista. */
private const val SESGO_INTRO = 0.8f

/**
 * Farola de sodio vieja: su luz (y el reflejo en la acera) respira suavemente sin parar y,
 * cada varios segundos, tintinea: unos parpadeos rápidos y flojos o, más rara vez, un bajón
 * de tensión casi hasta apagarse. Todo leve, para que no compita con el neón.
 */
private suspend fun farolaTitilante(rnd: Random, luz: (Float) -> Unit, apagada: (Float) -> Unit) {
    fun entre(a: Int, b: Int) = rnd.nextInt(a, b).toLong()
    var nivel = 0.5f
    while (true) {
        // Respiración: va hacia un nivel nuevo poco a poco durante unos segundos
        val pasos = rnd.nextInt(30, 90)
        repeat(pasos) {
            val objetivo = 0.42f + rnd.nextFloat() * 0.18f
            nivel += (objetivo - nivel) * 0.08f
            luz(nivel)
            delay(40)
        }
        val tipo = rnd.nextFloat()
        when {
            tipo < 0.6f -> repeat(rnd.nextInt(2, 6)) { // tintineo flojo
                luz(nivel * 0.4f); apagada(0.15f + rnd.nextFloat() * 0.2f)
                delay(entre(30, 90))
                luz(nivel); apagada(0f)
                delay(entre(40, 160))
            }
            tipo < 0.8f -> { // bajón de tensión que vuelve titubeando
                luz(0f); apagada(0.55f + rnd.nextFloat() * 0.3f)
                delay(entre(120, 450))
                repeat(rnd.nextInt(1, 4)) {
                    luz(nivel); apagada(0f); delay(entre(40, 110))
                    luz(0.1f); apagada(0.4f); delay(entre(30, 90))
                }
                luz(nivel); apagada(0f)
            }
            else -> Unit // esta vez solo respira
        }
    }
}
