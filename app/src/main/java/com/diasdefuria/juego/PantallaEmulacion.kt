package com.diasdefuria.juego

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.diasdefuria.R
import com.diasdefuria.datos.Aspecto
import com.diasdefuria.datos.Partida
import com.diasdefuria.red.Invitacion
import com.diasdefuria.red.CodigoInvitacion
import com.diasdefuria.red.Descubrimiento
import com.diasdefuria.red.PartidaEnRed
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.automirrored.filled.Login
import com.diasdefuria.logros.Logro
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import com.diasdefuria.datos.Bezels
import com.diasdefuria.datos.Juego
import com.diasdefuria.ui.pantallas.MiniaturaBezel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.sp
import com.diasdefuria.datos.AccionMando
import com.diasdefuria.datos.BotonesFisicos
import com.diasdefuria.datos.MapaBotones
import androidx.compose.ui.platform.LocalContext
import com.diasdefuria.datos.RomsetConocido
import com.diasdefuria.datos.Truco
import com.diasdefuria.ui.pantallas.SeccionTrucos
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.pantallas.FondoBezel
import com.diasdefuria.ui.tema.Colores
import com.swordfish.libretrodroid.GLRetroView
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/** Alto de la imagen de la CPS-2 en píxeles. */
private const val ALTO_CPS2 = 224

@Composable
fun PantallaEmulacion(
    vista: GLRetroView?,
    romset: RomsetConocido,
    aspecto: Aspecto,
    bezel: String,
    escaladoEntero: Boolean,
    mostrarTactiles: Boolean,
    opacidadTactiles: Float,
    vibracion: Boolean,
    menuAbierto: Boolean,
    hayUltima: Boolean,
    error: String?,
    aviso: String?,
    /** Menú de servicio de la placa abierto: X = TEST, Y = SERVICE (se recuerda arriba). */
    modoServicio: Boolean = false,
    onBoton: (Int, Boolean) -> Unit,
    onDireccion: (Float, Float) -> Unit,
    onAbrirMenu: () -> Unit,
    onContinuar: () -> Unit,
    onReiniciar: () -> Unit,
    onSalir: () -> Unit,
    onAvisoVisto: () -> Unit,
    onMenuServicio: () -> Unit = {},
    onElegirBezel: (String) -> Unit = {},
    panel: PanelPausa,
    onPanel: (PanelPausa) -> Unit,
    ranuras: List<Partida?>,
    cargables: List<Partida>,
    onGuardarEn: (Int) -> Unit,
    onCargar: (Partida) -> Unit,
    onCambiarTruco: (Truco, Int) -> Unit,
    jugadoresRed: List<String>? = null,
    invitacion: Invitacion? = null,
    onCompartir: () -> Unit = {},
    onCopiar: () -> Unit = {},
    tactilesVisibles: Boolean = false,
    onAlternarTactiles: () -> Unit = {},
    logro: Logro? = null,
    onAbrirSala: (internet: Boolean) -> Unit = {},
    /** Unirse a la partida de otro dispositivo (se deja esta, guardada). juego = null si se entra con código. */
    onUnirse: (host: String, puerto: Int, juego: String?) -> Unit = { _, _, _ -> },
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        // Bezel: rellena lo que no ocupa la imagen del juego
        FondoBezel(bezel, Modifier.fillMaxSize(), juego = romset.juego)

        if (vista != null && error == null) {
            val densidad = LocalDensity.current
            val anchoPx = constraints.maxWidth
            val altoPx = constraints.maxHeight

            // Tamaño de la imagen en pantalla, sin deformar
            var alto = if (escaladoEntero) max(1, altoPx / ALTO_CPS2) * ALTO_CPS2 else altoPx
            var ancho = (alto * aspecto.proporcion).roundToInt()
            if (ancho > anchoPx) {
                ancho = anchoPx
                alto = (ancho / aspecto.proporcion).roundToInt()
            }
            // FBNeo siempre dibuja en 4:3. El búfer se fija en 4:3 y Android lo escala al
            // tamaño de la vista: en 4:3 queda igual y en 16:9 los píxeles salen cuadrados.
            val bufferAlto = alto
            val bufferAncho = (alto * 4f / 3f).roundToInt()

            AndroidView(
                factory = { vista },
                update = { it.holder.setFixedSize(bufferAncho, bufferAlto) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(with(densidad) { ancho.toDp() }, with(densidad) { alto.toDp() }),
            )
        }

        if (mostrarTactiles) {
            ControlesTactiles(
                opacidad = opacidadTactiles,
                vibracion = vibracion,
                botones = romset.juego.botones,
                onBoton = onBoton,
                onDireccion = onDireccion,
                onMenu = onAbrirMenu,
            )
        }

        // Menú de servicio: recordatorio fijo de los botones del panel de la placa
        if (modoServicio) {
            Text(
                stringResource(R.string.servicio_botones),
                style = MaterialTheme.typography.labelMedium,
                color = Colores.OroClaro,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
                    .background(Colores.Piedra.copy(alpha = 0.8f), FormaPlaca)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // Anfitrión de una partida en red: solo un icono con cuántos jugadores hay
        // (el detalle está en la pausa, «Quién está conectado»)
        if (jugadoresRed != null && !menuAbierto) {
            com.diasdefuria.ui.componentes.IndicadorRed(
                jugadores = jugadoresRed.size + 1,
                esperando = jugadoresRed.isEmpty(),
                ping = null,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            )
        }

        if (menuAbierto) {
            when (panel) {
                PanelPausa.MENU -> MenuPausa(romset, hayUltima, invitacion != null, tactilesVisibles, onAlternarTactiles,
                    onContinuar, onPanel, onReiniciar, onMenuServicio, onSalir)
                PanelPausa.INVITAR -> if (invitacion != null) {
                    PanelInvitar(invitacion, onCompartir, onCopiar) { onPanel(PanelPausa.MENU) }
                } else {
                    // Se está abriendo la partida en la red
                    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Colores.Oro)
                    }
                }
                PanelPausa.JUGADORES -> PanelJugadores(jugadoresRed.orEmpty(), { onPanel(PanelPausa.INVITAR) }) {
                    onPanel(PanelPausa.MENU)
                }
                PanelPausa.ABRIR_SALA -> PanelAbrirSala(onAbrirSala, onUnirse) { onPanel(PanelPausa.MENU) }
                PanelPausa.MARCO -> PanelMarco(bezel, romset.juego, onElegirBezel) { onPanel(PanelPausa.MENU) }
                PanelPausa.TRUCOS -> PanelTrucos(romset, onCambiarTruco) { onPanel(PanelPausa.MENU) }
                PanelPausa.GUARDAR -> PanelPartidas(
                    titulo = stringResource(R.string.pausa_guardar),
                    ayuda = stringResource(R.string.pausa_guardar_ayuda),
                    elementos = ranuras.mapIndexed { i, p -> ElementoPartida(i + 1, p) },
                    onElegir = { e -> onGuardarEn(e.ranura!!) },
                    onCerrar = { onPanel(PanelPausa.MENU) },
                )
                PanelPausa.CARGAR -> PanelPartidas(
                    titulo = stringResource(R.string.pausa_cargar),
                    ayuda = stringResource(R.string.pausa_cargar_ayuda),
                    elementos = cargables.map { ElementoPartida(it.ranura, it) },
                    onElegir = { e -> e.partida?.let(onCargar) },
                    onCerrar = { onPanel(PanelPausa.MENU) },
                )
            }
        }

        if (error != null) {
            PanelError(error, onSalir)
        }

        // Logro de RetroAchievements conseguido
        AvisoLogro(logro, Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 70.dp))

        if (aviso != null) {
            LaunchedEffect(aviso) {
                delay(2200)
                onAvisoVisto()
            }
            Text(
                aviso,
                style = MaterialTheme.typography.titleMedium,
                color = Colores.OroClaro,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Colores.Piedra.copy(alpha = 0.85f), FormaPlaca)
                    .border(1.dp, Colores.Oro, FormaPlaca)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** Qué se ve con el juego en pausa. */
enum class PanelPausa { MENU, TRUCOS, GUARDAR, CARGAR, INVITAR, ABRIR_SALA, MARCO, JUGADORES }

@Composable
private fun MenuPausa(
    romset: RomsetConocido,
    hayPartidas: Boolean,
    enRed: Boolean,
    tactilesVisibles: Boolean,
    onAlternarTactiles: () -> Unit,
    onContinuar: () -> Unit,
    onPanel: (PanelPausa) -> Unit,
    onReiniciar: () -> Unit,
    onMenuServicio: () -> Unit,
    onSalir: () -> Unit,
) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(360.dp)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.pausa_titulo), style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
            Text(
                "${romset.juego.nombre} · ${romset.region}",
                style = MaterialTheme.typography.labelMedium,
                color = Colores.TextoSuave,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            val ancho = Modifier.fillMaxWidth()
            BotonMystara(stringResource(R.string.pausa_continuar), onContinuar, ancho.focusRequester(foco),
                icono = Icons.Filled.PlayArrow, estilo = EstiloBoton.PRINCIPAL)
            if (enRed) {
                BotonMystara(stringResource(R.string.multi_conectados), { onPanel(PanelPausa.JUGADORES) }, ancho,
                    icono = Icons.Filled.Groups)
                BotonMystara(stringResource(R.string.inet_invitar), { onPanel(PanelPausa.INVITAR) }, ancho,
                    icono = Icons.Filled.Share)
            } else {
                BotonMystara(stringResource(R.string.pausa_multijugador), { onPanel(PanelPausa.ABRIR_SALA) }, ancho,
                    icono = Icons.Filled.Groups)
            }
            BotonMystara(stringResource(R.string.pausa_guardar), { onPanel(PanelPausa.GUARDAR) }, ancho, icono = Icons.Filled.Save)
            BotonMystara(stringResource(R.string.pausa_cargar), { onPanel(PanelPausa.CARGAR) }, ancho, icono = Icons.Filled.Restore,
                habilitado = hayPartidas)
            BotonMystara(stringResource(R.string.pausa_trucos), { onPanel(PanelPausa.TRUCOS) }, ancho, icono = Icons.Filled.AutoFixHigh)
            BotonMystara(
                stringResource(if (tactilesVisibles) R.string.pausa_ocultar_tactiles else R.string.pausa_mostrar_tactiles),
                onAlternarTactiles, ancho, icono = Icons.Filled.TouchApp,
            )
            BotonMystara(stringResource(R.string.pausa_marco), { onPanel(PanelPausa.MARCO) }, ancho, icono = Icons.Filled.Wallpaper)
            BotonMystara(stringResource(R.string.pausa_servicio), onMenuServicio, ancho, icono = Icons.Filled.Tune)
            BotonMystara(stringResource(R.string.pausa_reiniciar), onReiniciar, ancho, icono = Icons.Filled.RestartAlt)
            BotonMystara(stringResource(R.string.pausa_salir), onSalir, ancho,
                icono = Icons.AutoMirrored.Filled.ExitToApp, estilo = EstiloBoton.DISCRETO)
            val context = LocalContext.current
            val teclaPausa = remember { BotonesFisicos.nombre(MapaBotones(context).tecla(AccionMando.PAUSA)) }
            Text(stringResource(R.string.pausa_ayuda, teclaPausa), style = MaterialTheme.typography.labelMedium,
                color = Colores.TextoSuave, textAlign = TextAlign.Center)
        }
    }
}

/** Una casilla del panel de guardar/cargar: [ranura] = 1..6 (o null), [partida] = null si está vacía. */
private data class ElementoPartida(val ranura: Int?, val partida: Partida?)

@Composable
private fun PanelPartidas(
    titulo: String,
    ayuda: String,
    elementos: List<ElementoPartida>,
    onElegir: (ElementoPartida) -> Unit,
    onCerrar: () -> Unit,
) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth(0.86f)
                .fillMaxHeight(0.94f)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(titulo, style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
                    Text(ayuda, style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
                }
                BotonMystara(stringResource(R.string.volver), onCerrar, estilo = EstiloBoton.DISCRETO)
            }
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(elementos) { i, e ->
                    CasillaPartida(e, formato, if (i == 0) Modifier.focusRequester(foco) else Modifier) { onElegir(e) }
                }
            }
        }
    }
}

@Composable
private fun CasillaPartida(e: ElementoPartida, formato: SimpleDateFormat, modifier: Modifier, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocada by interaccion.collectIsFocusedAsState()
    val p = e.partida
    val imagen = remember(p?.archivo?.path, p?.fecha) {
        p?.miniatura?.takeIf { it.isFile }?.let { runCatching { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }.getOrNull() }
    }
    Column(
        modifier
            .background(Colores.PiedraClara.copy(alpha = 0.5f), FormaPlaca)
            .border(if (enfocada) 3.dp else 1.5.dp, if (enfocada) Colores.OroClaro else Colores.OroOscuro, FormaPlaca)
            .clickable(interactionSource = interaccion, indication = null, onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(Color.Black, FormaPlaca),
            contentAlignment = Alignment.Center,
        ) {
            if (imagen != null) {
                Image(imagen, contentDescription = null, contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    stringResource(if (p == null) R.string.ranura_vacia else R.string.ranura_sin_imagen),
                    style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                p?.automatica == true -> stringResource(R.string.partida_auto)
                e.ranura != null -> stringResource(R.string.ranura_n, e.ranura)
                else -> stringResource(R.string.partida_manual, formato.format(Date(p?.fecha ?: 0)))
            },
            style = MaterialTheme.typography.titleSmall,
            color = if (enfocada) Colores.OroClaro else Colores.Pergamino,
        )
        Text(
            if (p != null) formato.format(Date(p.fecha)) else "—",
            style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave,
        )
    }
}

/** Marco de los laterales desde la pausa: se ve el cambio detrás del menú al momento. */
@Composable
private fun PanelMarco(actual: String, juego: Juego, onElegir: (String) -> Unit, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val opciones = remember { Bezels.disponibles(context) }
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.55f)), contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Colores.Piedra.copy(alpha = 0.95f))
                .border(1.5.dp, Colores.Oro)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pausa_marco), style = MaterialTheme.typography.titleLarge,
                    color = Colores.Oro, modifier = Modifier.weight(1f))
                BotonMystara(stringResource(R.string.volver), onCerrar, Modifier.focusRequester(foco), estilo = EstiloBoton.DISCRETO)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                (opciones + listOfNotNull(actual.takeIf { Bezels.esDeGaleria(it) })).forEach { b ->
                    MiniaturaBezel(b, seleccionado = b == actual, juego = juego) { onElegir(b) }
                }
            }
        }
    }
}

/** Abrir la partida en curso a otros jugadores, sin salir ni reiniciar. */
@Composable
private fun PanelAbrirSala(
    onAbrir: (internet: Boolean) -> Unit,
    onUnirse: (host: String, puerto: Int, juego: String?) -> Unit,
    onCerrar: () -> Unit,
) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    // Partidas de otros dispositivos en la misma wifi (se buscan mientras el panel está abierto)
    val context = LocalContext.current
    var partidas by remember { mutableStateOf<List<PartidaEnRed>>(emptyList()) }
    DisposableEffect(Unit) {
        val d = Descubrimiento(context)
        d.buscar { partidas = it }
        onDispose { d.cerrar() }
    }
    var codigo by rememberSaveable { mutableStateOf("") }
    var errorCodigo by remember { mutableStateOf(false) }
    fun unirseConCodigo() {
        val destino = CodigoInvitacion.leer(codigo)
        if (destino == null) errorCodigo = true else onUnirse(destino.first, destino.second, null)
    }
    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(440.dp)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.pausa_multijugador), style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
            Text(stringResource(R.string.pausa_multi_ayuda), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            BotonMystara(stringResource(R.string.pausa_multi_casa), { onAbrir(false) }, Modifier.fillMaxWidth().focusRequester(foco),
                icono = Icons.Filled.WifiTethering, estilo = EstiloBoton.PRINCIPAL)
            BotonMystara(stringResource(R.string.pausa_multi_internet), { onAbrir(true) }, Modifier.fillMaxWidth(),
                icono = Icons.Filled.Public, estilo = EstiloBoton.PRINCIPAL)

            // Unirse a la partida de otro: se deja esta (queda guardada, como al salir)
            Text(stringResource(R.string.pausa_unirse_titulo), style = MaterialTheme.typography.titleLarge, color = Colores.Oro,
                modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.pausa_unirse_ayuda), style = MaterialTheme.typography.labelMedium,
                color = Colores.TextoSuave, textAlign = TextAlign.Center)
            if (partidas.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), color = Colores.Oro, strokeWidth = 2.dp)
                    Text("  " + stringResource(R.string.multi_buscando), style = MaterialTheme.typography.bodyMedium,
                        color = Colores.TextoSuave)
                }
            }
            partidas.forEach { p ->
                val nombreJuego = Juego.porId(p.juego)?.nombre ?: p.juego
                BotonMystara(
                    "$nombreJuego · ${p.anfitrion}",
                    { onUnirse(p.direccion.hostAddress ?: "", p.puerto, p.juego) },
                    Modifier.fillMaxWidth(), icono = Icons.AutoMirrored.Filled.Login,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it.take(64); errorCodigo = false },
                    singleLine = true,
                    isError = errorCodigo,
                    placeholder = { Text(stringResource(R.string.inet_codigo_ejemplo)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { unirseConCodigo() }),
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = Colores.OroClaro),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Colores.OroClaro, unfocusedBorderColor = Colores.Oro,
                        cursorColor = Colores.OroClaro, errorBorderColor = Colores.Carmesi,
                    ),
                    modifier = Modifier.weight(1f),
                )
                BotonMystara(stringResource(R.string.multi_unirse_boton), ::unirseConCodigo,
                    icono = Icons.AutoMirrored.Filled.Login, habilitado = codigo.isNotBlank())
            }
            if (errorCodigo) {
                Text(stringResource(R.string.inet_codigo_malo), style = MaterialTheme.typography.labelMedium, color = Colores.OroClaro)
            }
            BotonMystara(stringResource(R.string.volver), onCerrar, Modifier.fillMaxWidth(), estilo = EstiloBoton.DISCRETO)
        }
    }
}

/** Partida en red (anfitrión): quién está jugando y con qué número de jugador. */
@Composable
private fun PanelJugadores(invitados: List<String>, onInvitar: () -> Unit, onCerrar: () -> Unit) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(420.dp)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.multi_conectados), style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
            Text(stringResource(R.string.multi_anfitrion_yo, com.diasdefuria.red.nombreDispositivo()),
                style = MaterialTheme.typography.titleMedium, color = Colores.OroClaro, textAlign = TextAlign.Center)
            invitados.forEach {
                Text(it, style = MaterialTheme.typography.titleMedium, color = Colores.OroClaro, textAlign = TextAlign.Center)
            }
            if (invitados.isEmpty()) {
                Text(stringResource(R.string.multi_nadie), style = MaterialTheme.typography.bodyMedium,
                    color = Colores.TextoSuave, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(4.dp))
            BotonMystara(stringResource(R.string.inet_invitar), onInvitar, Modifier.fillMaxWidth().focusRequester(foco),
                icono = Icons.Filled.Share)
            BotonMystara(stringResource(R.string.volver), onCerrar, Modifier.fillMaxWidth(), estilo = EstiloBoton.DISCRETO)
        }
    }
}

/** Cómo pueden unirse los demás: código para internet y para la misma wifi. */
@Composable
private fun PanelInvitar(inv: Invitacion, onCompartir: () -> Unit, onCopiar: () -> Unit, onCerrar: () -> Unit) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.94f)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.inet_invitar), style = MaterialTheme.typography.headlineMedium,
                    color = Colores.Oro, modifier = Modifier.weight(1f))
                BotonMystara(stringResource(R.string.volver), onCerrar, estilo = EstiloBoton.DISCRETO)
            }
            if (inv.internet) {
                Text(stringResource(R.string.inet_por_internet), style = MaterialTheme.typography.titleLarge, color = Colores.Oro)
                when {
                    inv.preparando -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Colores.Oro, strokeWidth = 2.dp)
                        Text("  " + stringResource(R.string.inet_preparando), style = MaterialTheme.typography.bodyMedium)
                    }
                    inv.codigoInternet != null -> CodigoGrande(inv.codigoInternet)
                }
                if (!inv.preparando) {
                    Text(
                        stringResource(when (inv.apertura) {
                            Invitacion.Apertura.ABIERTO -> R.string.inet_estado_abierto
                            Invitacion.Apertura.MANUAL -> R.string.inet_estado_manual
                            Invitacion.Apertura.CGNAT -> R.string.inet_estado_cgnat
                            else -> R.string.inet_estado_sin
                        }, inv.puerto, inv.ipLocal ?: "—"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (inv.apertura == Invitacion.Apertura.ABIERTO) Colores.Pergamino else Colores.OroClaro,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonMystara(stringResource(R.string.inet_compartir), onCompartir, Modifier.focusRequester(foco),
                        icono = Icons.Filled.Share, estilo = EstiloBoton.PRINCIPAL, habilitado = inv.codigoInternet != null)
                    BotonMystara(stringResource(R.string.inet_copiar), onCopiar, icono = Icons.Filled.ContentCopy,
                        habilitado = inv.codigoInternet != null)
                }
            }
            Text(stringResource(R.string.inet_en_casa), style = MaterialTheme.typography.titleLarge, color = Colores.Oro)
            Text(stringResource(R.string.inet_en_casa_ayuda), style = MaterialTheme.typography.bodyMedium)
            if (inv.codigoCasa != null) {
                CodigoGrande(inv.codigoCasa)
                Text("${inv.ipLocal}:${inv.puerto}", style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
            }
            if (!inv.internet) {
                BotonMystara(stringResource(R.string.volver), onCerrar, Modifier.focusRequester(foco), estilo = EstiloBoton.PRINCIPAL)
            }
            Text(stringResource(R.string.inet_tailscale), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
        }
    }
}

@Composable
private fun CodigoGrande(codigo: String) {
    Text(
        codigo,
        style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 4.sp),
        color = Colores.OroClaro,
        modifier = Modifier
            .border(2.dp, Colores.Oro, FormaPlaca)
            .background(Colores.PiedraClara.copy(alpha = 0.4f), FormaPlaca)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

/** Aviso de logro: entra deslizándose desde arriba, con la insignia del logro. */
@Composable
private fun AvisoLogro(logro: Logro?, modifier: Modifier) {
    // Se recuerda el último para poder animar la salida
    var ultimo by remember { mutableStateOf<Logro?>(null) }
    if (logro != null) ultimo = logro
    AnimatedVisibility(
        visible = logro != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        val l = ultimo ?: return@AnimatedVisibility
        Row(
            Modifier
                .widthIn(min = 260.dp, max = 380.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color(0xF01A1A22), Color(0xF02A2A36))),
                    RoundedCornerShape(10.dp),
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = l.imagen,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f, fill = false)) {
                Text(stringResource(R.string.ra_logro_desbloqueado), style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFB8C7FF))
                Text(l.titulo, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 2)
                Text(
                    stringResource(R.string.ra_puntos, l.puntos) + if (l.descripcion.isNotBlank()) " · ${l.descripcion}" else "",
                    style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 2,
                )
            }
        }
    }
}

/** Trucos en mitad de la partida: se aplican en cuanto se cambian. */
@Composable
private fun PanelTrucos(romset: RomsetConocido, onCambiar: (Truco, Int) -> Unit, onCerrar: () -> Unit) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.92f)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pausa_trucos), style = MaterialTheme.typography.headlineMedium,
                    color = Colores.Oro, modifier = Modifier.weight(1f))
                BotonMystara(stringResource(R.string.volver), onCerrar, Modifier.focusRequester(foco),
                    estilo = EstiloBoton.DISCRETO)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SeccionTrucos(romset.juego, onCambio = onCambiar)
            }
        }
    }
}

@Composable
private fun PanelError(texto: String, onSalir: () -> Unit) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(460.dp)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Carmesi, FormaPlaca)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.error_titulo), style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
            Text(texto, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            BotonMystara(stringResource(R.string.volver), onSalir, Modifier.focusRequester(foco), estilo = EstiloBoton.PRINCIPAL)
        }
    }
}
