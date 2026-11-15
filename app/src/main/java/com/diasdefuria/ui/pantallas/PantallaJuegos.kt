package com.diasdefuria.ui.pantallas

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.diasdefuria.R
import com.diasdefuria.datos.CatalogoRomsets
import com.diasdefuria.datos.EstadoJuego
import com.diasdefuria.datos.Juego
import com.diasdefuria.datos.Previews
import com.diasdefuria.datos.RomsetConocido
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.componentes.PantallaSeccion
import com.diasdefuria.ui.tema.Colores
import com.diasdefuria.ui.tema.FuenteTitulo
import com.diasdefuria.ui.tema.Pinceles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import com.diasdefuria.ui.componentes.VideoDemo
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.DisposableEffect
import java.util.Date
import java.text.SimpleDateFormat
import com.diasdefuria.datos.Puntuaciones
import com.diasdefuria.datos.Estadisticas
import kotlin.random.Random
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Casino

/** Alto de cada fila de la ruleta. */
private val ALTO_FILA = 54.dp

/** Copias virtuales de la lista para que la ruleta dé la vuelta sin llegar nunca al final. */
private const val VUELTAS = 2000

/** Con menos juegos la lista no da la vuelta: se verían repetidos en pantalla a la vez. */
private const val MINIMO_PARA_GIRAR = 8

/** Saltos de L1/R1 cuando la lista no está en orden alfabético. */
private const val SALTO = 10

enum class FiltroRuleta(@StringRes val texto: Int) {
    TODOS(R.string.ruleta_todos),
    LISTOS(R.string.ruleta_listos),
    FAVORITOS(R.string.ruleta_favoritos);

    fun siguiente() = entries[(ordinal + 1) % entries.size]
}

enum class OrdenRuleta(@StringRes val texto: Int) {
    LISTA(R.string.ruleta_orden_lista),
    NOMBRE(R.string.ruleta_orden_nombre),
    ANIO(R.string.ruleta_orden_anio);

    fun siguiente() = entries[(ordinal + 1) % entries.size]
}

/** Último juego elegido, filtro y orden: se recuerdan entre sesiones. */
private class PreferenciasRuleta(context: Context) {
    private val p = context.getSharedPreferences("ruleta", Context.MODE_PRIVATE)

    var ultimo: String?
        get() = p.getString("ultimo", null)
        set(v) = p.edit().putString("ultimo", v).apply()

    var filtro: FiltroRuleta
        get() = FiltroRuleta.entries.firstOrNull { it.name == p.getString("filtro", null) } ?: FiltroRuleta.TODOS
        set(v) = p.edit().putString("filtro", v.name).apply()

    var orden: OrdenRuleta
        get() = OrdenRuleta.entries.firstOrNull { it.name == p.getString("orden", null) } ?: OrdenRuleta.LISTA
        set(v) = p.edit().putString("orden", v.name).apply()

    /** Ids de los juegos marcados como favoritos (★). */
    var favoritos: Set<String>
        get() = p.getStringSet("favoritos", emptySet()).orEmpty().toSet()
        set(v) = p.edit().putStringSet("favoritos", v).apply()
}

/**
 * Selección de juego: a la izquierda una ruleta con todos los juegos, que gira con la
 * cruceta (L1/R1 saltan de letra o de 10 en 10) o arrastrando con el dedo; a la derecha,
 * la ficha del juego que queda en el centro: imagen, datos, ROM importada, versiones
 * (regiones) que se aceptan y botones de Jugar / Importar.
 */
@Composable
fun PantallaJuegos(
    estados: Map<Juego, EstadoJuego>,
    importando: Juego?,
    /** Nombre de la carpeta elegida con «Ruta» (null si aún no hay). */
    carpeta: String?,
    refrescando: Boolean,
    onRuta: () -> Unit,
    onRefresca: () -> Unit,
    onVolver: () -> Unit,
    onElegirZip: (Juego) -> Unit,
    onJugar: (RomsetConocido) -> Unit,
    onVerEstado: (Juego) -> Unit,
    onElegirVersion: (RomsetConocido) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { PreferenciasRuleta(context) }
    var filtro by remember { mutableStateOf(prefs.filtro) }
    var orden by remember { mutableStateOf(prefs.orden) }
    var seleccionado by remember { mutableStateOf(Juego.porId(prefs.ultimo) ?: Juego.primero) }
    var favoritos by remember { mutableStateOf(prefs.favoritos) }
    /** Cada «¡Al azar!» sube este número: la ruleta lo ve y gira hasta un juego al azar. */
    var tiradas by remember { mutableIntStateOf(0) }
    val focoRuleta = remember { FocusRequester() }
    val focoFicha = remember { FocusRequester() }

    val lista = remember(estados, filtro, orden, favoritos) {
        val ordenados = when (orden) {
            OrdenRuleta.LISTA -> Juego.entries.toList()
            OrdenRuleta.NOMBRE -> Juego.entries.sortedBy { claveOrden(it) }
            OrdenRuleta.ANIO -> Juego.entries.sortedWith(compareBy({ it.anio }, { claveOrden(it) }))
        }
        when (filtro) {
            FiltroRuleta.TODOS -> ordenados
            FiltroRuleta.LISTOS -> ordenados.filter { estados[it]?.jugable != null }
            FiltroRuleta.FAVORITOS -> ordenados.filter { it.id in favoritos }
        }
    }

    // Se guarda el juego elegido cuando la ruleta se para (no en cada fila que pasa)
    LaunchedEffect(seleccionado) {
        delay(400)
        prefs.ultimo = seleccionado.id
    }

    fun accionPrincipal(juego: Juego) {
        val estado = estados[juego]
        val jugable = estado?.jugable
        when {
            importando != null || refrescando -> Unit
            jugable != null -> onJugar(jugable)
            estado?.algoImportado == true -> onVerEstado(juego)
            else -> onElegirZip(juego)
        }
    }
    val cambiarFiltro = { filtro = filtro.siguiente(); prefs.filtro = filtro }
    // Chips táctiles: «Todos / Listos» alterna entre esos dos y «★ Favoritos» entra y sale de favoritos
    val cambiarTodosListos = {
        filtro = if (filtro == FiltroRuleta.TODOS) FiltroRuleta.LISTOS else FiltroRuleta.TODOS
        prefs.filtro = filtro
    }
    val alternarSoloFavoritos = {
        filtro = if (filtro == FiltroRuleta.FAVORITOS) FiltroRuleta.TODOS else FiltroRuleta.FAVORITOS
        prefs.filtro = filtro
    }
    val alternarFavorito = { j: Juego ->
        favoritos = if (j.id in favoritos) favoritos - j.id else favoritos + j.id
        prefs.favoritos = favoritos
    }
    val alAzar = { if (lista.size > 1 && importando == null && !refrescando) tiradas++ }
    val cambiarOrden = { orden = orden.siguiente(); prefs.orden = orden }

    PantallaSeccion(
        stringResource(R.string.menu_leyendas),
        onVolver,
        desplazable = false,
        // Arriba a la derecha, en la línea del título: carpeta de ROMs e importar todo lo que haya
        acciones = {
            BotonEspecial(
                texto = stringResource(R.string.ruleta_ruta),
                icono = if (carpeta == null) Icons.Filled.CreateNewFolder else Icons.Filled.FolderOpen,
                descripcion = carpeta?.let { stringResource(R.string.ruleta_ruta_actual, it) } ?: stringResource(R.string.ruleta_ruta_ninguna),
                onClick = onRuta,
            )
            BotonEspecial(
                texto = stringResource(if (refrescando) R.string.ruleta_refrescando else R.string.ruleta_refresca),
                icono = Icons.Filled.Refresh,
                descripcion = stringResource(R.string.ruleta_refresca_ayuda),
                girando = refrescando,
                onClick = onRefresca,
            )
        },
    ) {
        Row(
            Modifier.fillMaxSize().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Izquierda: la ruleta ──
            Column(Modifier.weight(0.42f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val posicion = lista.indexOf(seleccionado)
                CabeceraRuleta(
                    filtro = filtro,
                    orden = orden,
                    posicion = posicion,
                    total = lista.size,
                    onFiltro = cambiarTodosListos,
                    onFavoritos = alternarSoloFavoritos,
                    onOrden = cambiarOrden,
                )
                if (lista.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(if (filtro == FiltroRuleta.FAVORITOS) R.string.ruleta_vacia_favoritos else R.string.ruleta_vacia),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Colores.TextoSuave,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .focusRequester(focoRuleta)
                                .onPreviewKeyEvent { e ->
                                    if (e.type == KeyEventType.KeyDown && e.key == Key.ButtonY) { cambiarFiltro(); true } else false
                                }
                                .focusable()
                                .padding(16.dp),
                        )
                    }
                } else {
                    Ruleta(
                        lista = lista,
                        estados = estados,
                        seleccionado = seleccionado,
                        alfabetico = orden == OrdenRuleta.NOMBRE,
                        foco = focoRuleta,
                        onCambio = { seleccionado = it },
                        onAccion = { accionPrincipal(seleccionado) },
                        onDerecha = { runCatching { focoFicha.requestFocus() } },
                        onFiltro = cambiarFiltro,
                        onOrden = cambiarOrden,
                        onRefresca = onRefresca,
                        favoritos = favoritos,
                        tiradas = tiradas,
                        onFavorito = { alternarFavorito(seleccionado) },
                        onAzar = alAzar,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // «¡Al azar!»: la ruleta gira unas vueltas y se para en un juego cualquiera (R2 con el mando)
                    BotonEspecial(
                        texto = stringResource(R.string.ruleta_azar),
                        icono = Icons.Filled.Casino,
                        descripcion = stringResource(R.string.ruleta_azar_ayuda),
                        onClick = alAzar,
                    )
                    Text(
                        stringResource(R.string.ruleta_ayuda),
                        style = MaterialTheme.typography.labelSmall,
                        color = Colores.TextoSuave,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Derecha: ficha del juego elegido ──
            val visible = seleccionado.takeIf { it in lista }
            if (visible == null) {
                Box(Modifier.weight(0.58f).fillMaxHeight())
            } else {
                FichaSeleccion(
                    juego = visible,
                    estado = estados[visible],
                    favorito = visible.id in favoritos,
                    onFavorito = { alternarFavorito(visible) },
                    importando = importando == visible,
                    foco = focoFicha,
                    onElegirZip = { onElegirZip(visible) },
                    onJugar = onJugar,
                    onVerEstado = { onVerEstado(visible) },
                    onElegirVersion = onElegirVersion,
                    modifier = Modifier.weight(0.58f).fillMaxHeight(),
                )
            }
        }
    }

    LaunchedEffect(Unit) { runCatching { focoRuleta.requestFocus() } }
}

/** Orden alfabético sin el artículo inicial: «The Punisher» va en la P. */
private fun claveOrden(j: Juego): String = j.nombre.lowercase(Locale.ROOT).removePrefix("the ").trim()

private fun inicial(j: Juego): Char =
    claveOrden(j).firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'

// ── Cabecera: filtro, orden y contador ───────────────────────────────────────

@Composable
private fun CabeceraRuleta(
    filtro: FiltroRuleta,
    orden: OrdenRuleta,
    posicion: Int,
    total: Int,
    onFiltro: () -> Unit,
    onFavoritos: () -> Unit,
    onOrden: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Con mando se cambian con Y (Todos › Listos › Favoritos) y X; los chips son para el
        // táctil y no roban el foco. Favoritos tiene su propio chip, siempre a la vista.
        // Si no caben (pantallas pequeñas), los chips se desplazan de lado y el contador no se mueve
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val listos = filtro == FiltroRuleta.LISTOS
            ChipRuleta(stringResource(if (listos) R.string.ruleta_listos else R.string.ruleta_todos),
                activo = listos, enfocable = false, onClick = onFiltro)
            ChipRuleta(stringResource(R.string.ruleta_favoritos), activo = filtro == FiltroRuleta.FAVORITOS,
                enfocable = false, onClick = onFavoritos)
            ChipRuleta(stringResource(orden.texto), activo = orden != OrdenRuleta.LISTA, enfocable = false, onClick = onOrden)
        }
        Text(
            if (total == 0) "0/0" else "${posicion.coerceAtLeast(0) + 1}/$total",
            // Más pequeño y en una sola línea: «13/21» no se parte aunque la cabecera vaya justa
            style = MaterialTheme.typography.labelSmall,
            color = Colores.Oro,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ── La ruleta ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Ruleta(
    lista: List<Juego>,
    estados: Map<Juego, EstadoJuego>,
    seleccionado: Juego,
    alfabetico: Boolean,
    foco: FocusRequester,
    onCambio: (Juego) -> Unit,
    onAccion: () -> Unit,
    onDerecha: () -> Unit,
    onFiltro: () -> Unit,
    onOrden: () -> Unit,
    onRefresca: () -> Unit,
    favoritos: Set<String>,
    tiradas: Int,
    onFavorito: () -> Unit,
    onAzar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = lista.size
    val gira = n >= MINIMO_PARA_GIRAR
    val total = if (gira) n * VUELTAS else n
    // Al cambiar la lista (filtro u orden) la ruleta se recoloca en el juego que estaba elegido
    val estado = remember(lista) {
        val real = lista.indexOf(seleccionado).coerceAtLeast(0)
        LazyListState(firstVisibleItemIndex = if (gira) n * (VUELTAS / 2) + real else real)
    }
    var objetivo by remember(lista) { mutableIntStateOf(estado.firstVisibleItemIndex) }
    val scope = rememberCoroutineScope()
    var enfocada by remember { mutableStateOf(false) }
    val alCambiar by rememberUpdatedState(onCambio)

    // La fila elegida es la que queda más cerca del centro de la ruleta
    val centrada by remember(estado) {
        derivedStateOf {
            val info = estado.layoutInfo
            val centro = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - centro) }?.index
                ?: estado.firstVisibleItemIndex
        }
    }
    LaunchedEffect(centrada, lista) { lista.getOrNull(centrada % n)?.let(alCambiar) }

    fun irA(indice: Int) {
        objetivo = indice.coerceIn(0, total - 1)
        scope.launch { estado.animateScrollToItem(objetivo) }
    }

    // Si la ruleta ya está girando, los pulsos se acumulan sobre el destino, no sobre lo que se ve
    fun mover(pasos: Int) = irA((if (estado.isScrollInProgress) objetivo else centrada) + pasos)

    // «¡Al azar!»: unas vueltas de ruleta que frenan poco a poco hasta un juego al azar
    val densidad = LocalDensity.current
    // (solo las tiradas nuevas: al volver a esta pantalla o cambiar de filtro no se repite)
    var atendidas by remember { mutableIntStateOf(tiradas) }
    LaunchedEffect(tiradas) {
        if (tiradas == atendidas || n < 2) return@LaunchedEffect
        atendidas = tiradas
        val base = centrada
        val actual = base % n
        val elegido = (actual + 1 + Random.nextInt(n - 1)) % n            // nunca el mismo
        val pasos = ((elegido - actual) % n + n) % n
        if (!gira) { irA(elegido); return@LaunchedEffect }
        val destino = (base + n * 3 + pasos).coerceAtMost(total - 1)
        val altoFila = with(densidad) { ALTO_FILA.toPx() }
        val distancia = (destino - estado.firstVisibleItemIndex) * altoFila - estado.firstVisibleItemScrollOffset
        objetivo = destino
        estado.animateScrollBy(distancia, tween(3200, easing = CubicBezierEasing(0.15f, 0.6f, 0.2f, 1f)))
        estado.animateScrollToItem(destino)   // encaja la fila justo en el centro
    }

    /** L1/R1 en orden alfabético: primer juego de la letra anterior / siguiente. */
    fun saltarLetra(adelante: Boolean) {
        if (!alfabetico) { mover(if (adelante) SALTO else -SALTO); return }
        val base = if (estado.isScrollInProgress) objetivo else centrada
        val actual = base % n
        fun en(k: Int) = lista[((actual + k) % n + n) % n]
        if (adelante) {
            val letra = inicial(en(0))
            val pasos = (1 until n).firstOrNull { inicial(en(it)) != letra } ?: return
            irA(base + pasos)
        } else {
            if (n < 2) return
            val letra = inicial(en(-1))
            var pasos = 1
            while (pasos < n - 1 && inicial(en(-pasos - 1)) == letra) pasos++
            irA(base - pasos)
        }
    }

    BoxWithConstraints(
        modifier
            .clip(FormaPlaca)
            .background(Colores.Piedra.copy(alpha = 0.55f), FormaPlaca)
            .border(1.5.dp, if (enfocada) Colores.Oro else Colores.OroOscuro, FormaPlaca)
            .focusRequester(foco)
            .onFocusChanged { enfocada = it.isFocused }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val primera = e.nativeKeyEvent.repeatCount == 0
                when (e.key) {
                    Key.DirectionUp -> { mover(-1); true }
                    Key.DirectionDown -> { mover(1); true }
                    Key.ButtonL1, Key.PageUp -> { saltarLetra(false); true }
                    Key.ButtonR1, Key.PageDown -> { saltarLetra(true); true }
                    Key.DirectionRight -> { onDerecha(); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.ButtonStart -> { if (primera) onAccion(); true }
                    Key.ButtonY -> { if (primera) onFiltro(); true }
                    Key.ButtonX -> { if (primera) onOrden(); true }
                    Key.ButtonSelect -> { if (primera) onRefresca(); true }
                    Key.ButtonL2 -> { if (primera) onFavorito(); true }
                    Key.ButtonR2 -> { if (primera) onAzar(); true }
                    else -> false
                }
            }
            .focusable(),
    ) {
        // Relleno arriba y abajo: así la primera y la última fila también pueden quedar en el centro
        val relleno = ((maxHeight - ALTO_FILA) / 2).coerceAtLeast(0.dp)

        // Banda fija del centro: marca el juego elegido
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .height(ALTO_FILA)
                .background(Pinceles.carmesi, FormaPlaca)
                .border(if (enfocada) 2.5.dp else 1.5.dp, if (enfocada) Colores.OroClaro else Colores.Oro, FormaPlaca),
        )

        LazyColumn(
            state = estado,
            flingBehavior = rememberSnapFlingBehavior(estado),
            contentPadding = PaddingValues(vertical = relleno),
            modifier = Modifier.fillMaxSize().focusProperties { canFocus = false },
        ) {
            items(total) { i ->
                val juego = lista[i % n]
                FilaRuleta(
                    juego = juego,
                    estado = estados[juego],
                    favorito = juego.id in favoritos,
                    centrada = i == centrada,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ALTO_FILA)
                        // Efecto ruleta: las filas se encogen y se apagan al alejarse del centro
                        .graphicsLayer {
                            val info = estado.layoutInfo
                            val fila = info.visibleItemsInfo.firstOrNull { it.index == i } ?: return@graphicsLayer
                            val centro = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val distancia = abs(fila.offset + fila.size / 2f - centro) / fila.size.coerceAtLeast(1)
                            val escala = (1f - distancia * 0.07f).coerceIn(0.72f, 1f)
                            scaleX = escala
                            scaleY = escala
                            alpha = (1f - distancia * 0.2f).coerceIn(0.2f, 1f)
                        }
                        .pointerInput(estado, i) {
                            // Tocar una fila la lleva al centro
                            detectTapGestures { irA(i) }
                        },
                )
            }
        }

        // Degradados arriba y abajo: las filas «entran» y «salen» de la ruleta
        val difuminado = relleno * 0.7f
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(difuminado)
                .background(Brush.verticalGradient(listOf(Colores.Piedra, Color.Transparent))),
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(difuminado)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Colores.Piedra))),
        )
    }
}

@Composable
private fun FilaRuleta(juego: Juego, estado: EstadoJuego?, favorito: Boolean, centrada: Boolean, modifier: Modifier) {
    Row(
        modifier.padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconoEstado(estado)
        Spacer(Modifier.width(12.dp))
        Text(
            juego.nombre,
            style = if (centrada) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = if (centrada) Colores.OroClaro else Colores.Pergamino,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (favorito) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Colores.Oro, modifier = Modifier.padding(start = 6.dp).size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            juego.anio.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (centrada) Colores.Oro else Colores.TextoSuave,
        )
    }
}

/** ✔ listo para jugar · ⚠ importado pero incompleto · punto apagado: sin importar. */
@Composable
private fun IconoEstado(estado: EstadoJuego?) {
    val (icono, color) = when {
        estado?.jugable != null -> Icons.Filled.CheckCircle to Colores.Oro
        estado?.algoImportado == true -> Icons.Filled.WarningAmber to Colores.OroOscuro
        else -> Icons.Filled.FiberManualRecord to Colores.TextoSuave.copy(alpha = 0.35f)
    }
    Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(if (estado?.jugable != null) 18.dp else 14.dp))
}

// ── Ficha del juego elegido ──────────────────────────────────────────────────

@Composable
private fun FichaSeleccion(
    juego: Juego,
    estado: EstadoJuego?,
    favorito: Boolean,
    onFavorito: () -> Unit,
    importando: Boolean,
    foco: FocusRequester,
    onElegirZip: () -> Unit,
    onJugar: (RomsetConocido) -> Unit,
    onVerEstado: () -> Unit,
    onElegirVersion: (RomsetConocido) -> Unit,
    modifier: Modifier = Modifier,
) {
    val jugable = estado?.jugable
    val version = jugable ?: estado?.incompleto
    val versiones = remember(juego) { CatalogoRomsets.todos.filter { it.juego == juego } }

    Column(
        modifier
            .background(Pinceles.piedra, FormaPlaca)
            .border(1.5.dp, Colores.OroOscuro, FormaPlaca)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
        // Portada en vertical (las de las recreativas son flyers 2:3). Su ancho sale del alto, pero
        // nunca pasa del 40 % de la ficha: en pantallas casi cuadradas (Retroid Pocket Classic)
        // se comería el sitio de los datos. Si la ficha es muy estrecha, no se pone.
        val anchoPortada = minOf(maxHeight * 0.68f, maxWidth * 0.4f)
        val conPortada = maxWidth >= 300.dp
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(if (conPortada) 14.dp else 0.dp)) {
            if (conPortada) {
                Box(Modifier.width(anchoPortada).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Crossfade(
                        targetState = juego,
                        label = "preview",
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.68f),
                    ) { j -> VistaPrevia(j) }
                }
            }

            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Datos arriba (se desplazan si no caben); las versiones van fijas abajo y siempre se ven,
                // aunque el nombre del juego ocupe dos líneas
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            juego.nombre,
                            // Los nombres largos («Teenage Mutant Ninja Turtles: Turtles in Time») van algo más pequeños
                            style = if (juego.nombre.length > 24) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            color = Colores.Oro,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        // Favorito: se marca tocando la estrella (o con L2 en la ruleta)
                        Icon(
                            if (favorito) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = stringResource(R.string.ficha_favorito),
                            tint = if (favorito) Colores.Oro else Colores.TextoSuave,
                            modifier = Modifier
                                .size(28.dp)
                                .focusProperties { canFocus = false }
                                .clickable(onClick = onFavorito)
                                .padding(2.dp),
                        )
                    }
                    EtiquetaEstado(estado)
                    Spacer(Modifier.height(4.dp))
                    // Ficha técnica
                    DatoFicha(stringResource(R.string.ficha_fabricante), juego.fabricante)
                    DatoFicha(stringResource(R.string.ficha_anio), juego.anio.toString())
                    DatoFicha(stringResource(R.string.ficha_placa), juego.placa)
                    DatoFicha(stringResource(R.string.ficha_genero), juego.genero)
                    DatoFicha(
                        stringResource(R.string.ficha_jugadores_titulo),
                        if (juego.jugadores == 1) stringResource(R.string.ficha_un_jugador)
                        else stringResource(R.string.ficha_jugadores, juego.jugadores),
                    )
                    DatoFicha(stringResource(R.string.ficha_botones), juego.botones.toString())
                    Spacer(Modifier.height(4.dp))
                    // Estadísticas: se releen al volver de jugar
                    val vueltas = contadorDeVueltas()
                    val context = LocalContext.current
                    val stats = remember(juego, vueltas) { Estadisticas.de(context, juego) }
                    val record = remember(juego, vueltas) { Puntuaciones.record(context, juego) }
                    DatoFicha(
                        stringResource(R.string.ficha_jugado),
                        if (stats.partidas == 0) stringResource(R.string.ficha_nunca)
                        else context.resources.getQuantityString(R.plurals.ficha_partidas, stats.partidas, stats.partidas) +
                            " · " + Estadisticas.textoTiempo(stats.tiempoMs),
                    )
                    if (stats.ultima > 0) {
                        DatoFicha(stringResource(R.string.ficha_ultima),
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(stats.ultima)))
                    }
                    if (record != null) {
                        DatoFicha(stringResource(R.string.ficha_record), String.format(Locale.getDefault(), "%,d", record),
                            destacado = true)
                    }
                    Spacer(Modifier.height(4.dp))
                    // ROM importada: archivo, región y tamaño (o cuántas versiones se aceptan)
                    if (version != null) {
                        DatoFicha(stringResource(R.string.rom_archivo), version.archivoZip, destacado = jugable != null)
                        DatoFicha(stringResource(R.string.rom_region), version.region, destacado = jugable != null)
                        val faltan = estado?.faltan.orEmpty()
                        DatoFicha(
                            stringResource(R.string.rom_tamano),
                            formatoTamano(version.tamanoTotal) +
                                if (jugable == null && faltan.isNotEmpty()) " · " + stringResource(R.string.rom_faltan, faltan.size) else "",
                            destacado = jugable != null,
                        )
                    } else {
                        Text(
                            stringResource(R.string.rom_sin_importar, versiones.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = Colores.TextoSuave,
                        )
                    }
                }
                // Versiones (regiones) que se aceptan: las importadas se eligen, las demás abren el importador
                Text(stringResource(R.string.rom_versiones), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
                Row(
                    Modifier.fillMaxWidth().height(30.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Primero las importadas, luego el resto en el orden del catálogo
                    val completas = estado?.versiones.orEmpty()
                    versiones.sortedByDescending { it in completas }.forEach { rs ->
                        val completa = rs in completas
                        ChipRuleta(
                            texto = etiquetaVersion(rs),
                            activo = rs == jugable,
                            apagado = !completa,
                            onClick = { if (completa) onElegirVersion(rs) else onElegirZip() },
                        )
                    }
                }
            }
        }
        }
        BotonesFicha(estado, importando, foco, onElegirZip, onJugar, onVerEstado)
    }
}

/** Una línea «Etiqueta   valor» de la ficha. */
@Composable
private fun DatoFicha(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = Colores.TextoSuave,
            modifier = Modifier.width(78.dp),
        )
        Text(
            valor,
            style = MaterialTheme.typography.labelMedium,
            color = if (destacado) Colores.OroClaro else Colores.Pergamino,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VistaPrevia(juego: Juego) {
    val context = LocalContext.current
    val uri = remember(juego) { Previews.uri(context, juego) }
    Box(
        Modifier
            .fillMaxSize()
            .clip(FormaPlaca)
            .background(Color.Black)
            .border(1.dp, Colores.Oro, FormaPlaca),
        contentAlignment = Alignment.Center,
    ) {
        if (uri != null) {
            // Detrás, la misma imagen ampliada y oscurecida: rellena el hueco si no es 2:3
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.3f },
            )
            AsyncImage(
                model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                contentDescription = juego.nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Sin imagen: placa con el nombre, para que la app funcione sin arte
            Column(
                Modifier.fillMaxSize().background(Pinceles.carmesi).padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(juego.nombre, style = MaterialTheme.typography.headlineMedium, color = Colores.OroClaro, textAlign = TextAlign.Center)
                Text(juego.ficha, style = MaterialTheme.typography.titleMedium, color = Colores.Oro)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.ruleta_sin_imagen, juego.id),
                    style = MaterialTheme.typography.labelMedium,
                    color = Colores.TextoSuave,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // Vídeo de demostración (si el juego tiene, assets/videos/<id>.mp4): en lugar de la foto fija
        VideoDemo(juego, Modifier.fillMaxSize())
        // Placa en la esquina, como en los frontends de recreativas
        Text(
            juego.placa,
            style = MaterialTheme.typography.labelMedium,
            color = Colores.OroClaro,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Colores.Piedra.copy(alpha = 0.8f), FormaPlaca)
                .border(1.dp, Colores.OroOscuro, FormaPlaca)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EtiquetaEstado(estado: EstadoJuego?) {
    val (texto, color) = when {
        estado?.jugable != null -> stringResource(R.string.rom_estado_listo) to Colores.Oro
        estado?.algoImportado == true -> stringResource(R.string.rom_estado_incompleto) to Colores.OroOscuro
        else -> stringResource(R.string.rom_estado_sin) to Colores.TextoSuave
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconoEstado(estado)
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/**
 * - sin archivos: «Importar ROM»
 * - listo: «Jugar» y «Cambiar ZIP»
 * - con archivos pero incompleto: «Completar» (explica qué falta) e «Importar ROM»
 */
@Composable
private fun BotonesFicha(
    estado: EstadoJuego?,
    importando: Boolean,
    foco: FocusRequester,
    onElegirZip: () -> Unit,
    onJugar: (RomsetConocido) -> Unit,
    onVerEstado: () -> Unit,
) {
    val jugable = estado?.jugable
    Row(Modifier.fillMaxWidth().height(46.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val primero = Modifier.weight(1.4f).fillMaxHeight().focusRequester(foco)
        val segundo = Modifier.weight(1f).fillMaxHeight()
        when {
            importando -> BotonMystara(stringResource(R.string.importando), {}, primero, cargando = true)
            jugable != null -> {
                BotonMystara(stringResource(R.string.ficha_jugar), { onJugar(jugable) }, primero,
                    icono = Icons.Filled.PlayArrow, estilo = EstiloBoton.PRINCIPAL)
                BotonMystara(stringResource(R.string.menu_cambiar_zip), onElegirZip, segundo,
                    icono = Icons.Filled.SwapHoriz, estilo = EstiloBoton.DISCRETO)
            }
            estado?.algoImportado == true -> {
                BotonMystara(stringResource(R.string.ficha_completar), onVerEstado, primero, icono = Icons.Filled.WarningAmber)
                BotonMystara(stringResource(R.string.rom_importar), onElegirZip, segundo,
                    icono = Icons.Filled.FileDownload, estilo = EstiloBoton.DISCRETO)
            }
            else -> BotonMystara(stringResource(R.string.rom_importar), onElegirZip, primero,
                icono = Icons.Filled.FileDownload, estilo = EstiloBoton.PRINCIPAL)
        }
    }
}

/** «Español» para las Hispanic; para el resto, su región (y 🏆 en la versión principal, la de los logros). */
@Composable
private fun etiquetaVersion(rs: RomsetConocido): String =
    if (rs.hispanic) stringResource(R.string.version_espanol) + " " + rs.region.substringAfter(' ')
    else if (rs.padre == null) rs.region + " 🏆"
    else rs.region

/** Chip pequeño: [activo] relleno en oro, [apagado] (versión sin importar) atenuado. */
@Composable
private fun ChipRuleta(
    texto: String,
    activo: Boolean,
    onClick: () -> Unit,
    apagado: Boolean = false,
    enfocable: Boolean = true,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = when {
            activo -> Colores.Piedra
            apagado -> Colores.TextoSuave
            else -> Colores.OroClaro
        },
        maxLines = 1,
        modifier = Modifier
            .background(if (activo) Colores.Oro else Colores.Piedra.copy(alpha = 0.6f), FormaPlaca)
            .border(
                if (enfocado) 2.5.dp else 1.dp,
                when {
                    enfocado -> Colores.OroClaro
                    apagado -> Colores.OroOscuro
                    else -> Colores.Oro
                },
                FormaPlaca,
            )
            .focusProperties { canFocus = enfocable }
            .clickable(interactionSource = interaccion, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/**
 * Botón especial de la cabecera («Ruta», «Refresca»): pegatina roja de spray con contorno
 * amarillo, icono y letras Bangers, para que se distinga de los chips de filtro y orden.
 * Los dos comparten este mismo formato.
 */
@Composable
private fun BotonEspecial(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    girando: Boolean = false,
) {
    val giro by androidx.compose.animation.core.rememberInfiniteTransition(label = "giro").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing)),
        label = "grados",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .semantics { contentDescription = descripcion }
            .background(
                Brush.verticalGradient(listOf(Colores.Carmesi, Colores.CarmesiOscuro)),
                FormaPlaca,
            )
            .border(1.5.dp, Colores.Oro, FormaPlaca)
            .focusProperties { canFocus = false }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(start = 7.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = Colores.OroClaro,
            modifier = Modifier.size(15.dp).graphicsLayer { rotationZ = if (girando) giro else 0f },
        )
        Text(
            texto.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FuenteTitulo),
            color = Colores.Pergamino,
            maxLines = 1,
        )
    }
}

private fun formatoTamano(bytes: Long): String =
    if (bytes >= 1024 * 1024) String.format(Locale.getDefault(), "%.1f MB", bytes / 1048576.0)
    else "${bytes / 1024} KB"

/** Cuenta las veces que la pantalla vuelve al primer plano (p. ej. al volver de jugar). */
@Composable
private fun contadorDeVueltas(): Int {
    var vueltas by remember { mutableIntStateOf(0) }
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vueltas++ }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }
    return vueltas
}
