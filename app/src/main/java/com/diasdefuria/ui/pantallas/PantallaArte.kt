package com.diasdefuria.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.diasdefuria.R
import com.diasdefuria.datos.Juego
import androidx.compose.material.icons.filled.PhotoLibrary
import com.diasdefuria.datos.ImagenArte
import com.diasdefuria.datos.cargarArte
import com.diasdefuria.datos.Guia
import com.diasdefuria.datos.DocumentoPdf
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.diasdefuria.datos.cargarGuias
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import androidx.compose.ui.text.style.TextOverflow
import com.diasdefuria.datos.Pantallazos
import com.diasdefuria.datos.Pantallazo
import androidx.compose.material.icons.filled.PhotoCamera
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.componentes.PantallaSeccion
import com.diasdefuria.ui.tema.Colores
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/** Pestañas de Extras: la galería de cada juego, la historia y los pantallazos de logros. */
private enum class PestanaExtras(@androidx.annotation.StringRes val nombre: Int) {
    GALERIA(R.string.extras_galeria),
    HISTORIA(R.string.extras_historia),
    PANTALLAZOS(R.string.extras_pantallazos),
}

/** Lo que enseña el visor a pantalla completa: la imagen y, si hay, un pie con su descripción. */
private data class ImagenVisor(val uri: String, val descripcion: String, val pie: String? = null)

/**
 * Extras: logros de RetroAchievements, galería de cada juego con visor a pantalla
 * completa y la historia de cada juego (PDF o texto).
 */
@Composable
fun PantallaArte(onVolver: () -> Unit) {
    val context = LocalContext.current
    val todas = remember { cargarArte(context) }
    val historias = remember { cargarGuias(context, "historia") }
    // Solo los juegos que tienen imágenes, en el orden de la ruleta
    val conImagenes = remember(todas) { todas.map { it.juego }.distinct() }
    var juegoId by rememberSaveable { mutableStateOf(conImagenes.firstOrNull()?.id) }
    val juego = Juego.porId(juegoId)
    var pestana by rememberSaveable { mutableStateOf(PestanaExtras.GALERIA) }
    var abierta by rememberSaveable { mutableStateOf<Int?>(null) }
    var guiaAbierta by rememberSaveable { mutableStateOf<String?>(null) }
    var carpetaAbierta by rememberSaveable { mutableStateOf("historia") }
    var logrosAbierto by rememberSaveable { mutableStateOf(false) }
    val visibles = remember(juego) { todas.filter { it.juego == juego } }
    // Pantallazos de logros (se guardan en la memoria de la app al saltar cada logro)
    var pantallazos by remember { mutableStateOf(Pantallazos.listar(context)) }
    var pantallazoAbierto by rememberSaveable { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxSize()) {
    PantallaSeccion(stringResource(R.string.arte_titulo), onVolver, desplazable = false) {
        // Logros de RetroAchievements (botón con color propio)
        key(logrosAbierto) { BotonRetroAchievements { logrosAbierto = true } }

        // Pestañas centradas, con aire arriba (botón de logros) y abajo (miniaturas)
        Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PestanaExtras.entries.forEach { p ->
                BotonMystara(
                    texto = stringResource(p.nombre),
                    estilo = if (pestana == p) EstiloBoton.PRINCIPAL else EstiloBoton.DISCRETO,
                    icono = when (p) {
                        PestanaExtras.GALERIA -> Icons.Filled.PhotoLibrary
                        PestanaExtras.HISTORIA -> Icons.Filled.HistoryEdu
                        PestanaExtras.PANTALLAZOS -> Icons.Filled.PhotoCamera
                    },
                    onClick = { pestana = p },
                )
            }
        }
        }
        when {
            pestana == PestanaExtras.HISTORIA -> {
                if (historias.isEmpty()) {
                    Text(stringResource(R.string.extras_historia_vacio),
                        style = MaterialTheme.typography.bodyLarge, color = Colores.TextoSuave)
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    historias.forEach { g ->
                        BotonMystara(g.titulo, { carpetaAbierta = g.carpeta; guiaAbierta = g.archivo }, Modifier.fillMaxWidth(),
                            icono = Icons.Filled.HistoryEdu)
                    }
                }
            }
            pestana == PestanaExtras.PANTALLAZOS -> {
                if (pantallazos.isEmpty()) {
                    Text(stringResource(R.string.extras_pantallazos_vacio), style = MaterialTheme.typography.bodyLarge,
                        color = Colores.TextoSuave)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(210.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        itemsIndexed(pantallazos, key = { _, p -> p.archivo.path }) { i, p ->
                            TarjetaPantallazo(p) { pantallazoAbierto = i }
                        }
                    }
                }
            }
            conImagenes.isEmpty() -> Text(stringResource(R.string.arte_vacio), style = MaterialTheme.typography.bodyLarge,
                color = Colores.TextoSuave)
            else -> {
                // Juego de la galería: una fila que se desliza (con cruceta o dedo)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    conImagenes.forEach { j ->
                        BotonMystara(
                            texto = j.nombre,
                            onClick = { juegoId = j.id },
                            estilo = if (j == juego) EstiloBoton.PRINCIPAL else EstiloBoton.DISCRETO,
                        )
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(170.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    itemsIndexed(visibles, key = { _, img -> img.uri }) { i, img ->
                        Miniatura(img) { abierta = i }
                    }
                }
            }
        }
    }

    abierta?.let { inicio -> Visor(visibles.map { ImagenVisor(it.uri, it.archivo) }, inicio, onCerrar = { abierta = null }) }
    pantallazoAbierto?.let { inicio ->
        Visor(
            pantallazos.map { ImagenVisor(it.uri, it.logro, pieDe(it)) },
            inicio,
            onCerrar = { pantallazoAbierto = null },
            onBorrar = { i ->
                pantallazos.getOrNull(i)?.let(Pantallazos::borrar)
                pantallazos = Pantallazos.listar(context)
                if (pantallazos.isEmpty()) pantallazoAbierto = null
                else pantallazoAbierto = i.coerceAtMost(pantallazos.lastIndex)
            },
        )
    }
    guiaAbierta?.let { archivo ->
        val g = Guia(archivo, carpetaAbierta)
        if (g.esPdf) LectorPdf(g) { guiaAbierta = null } else LectorGuia(g) { guiaAbierta = null }
    }
    if (logrosAbierto) PanelRetroAchievements { logrosAbierto = false }
    }
}

/**
 * Lector de textos: letra de ancho fijo (para los dibujos hechos con letras), se mueve
 * con el dedo o con la cruceta (L1/R1 pasan página, izquierda/derecha cambian el tamaño).
 */
@Composable
private fun LectorGuia(guia: Guia, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val lineas = remember(guia) { guia.leer(context).lines() }
    val maxCaracteres = remember(lineas) { lineas.maxOfOrNull { it.length } ?: 80 }
    var tamano by rememberSaveable { mutableStateOf(13f) }
    val estado = rememberLazyListState()
    val horizontal = rememberScrollState()
    val alcance = rememberCoroutineScope()
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    BackHandler(onBack = onCerrar)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF15100C))
            .focusRequester(foco)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionDown -> alcance.launch { estado.animateScrollBy(120f) }
                    Key.DirectionUp -> alcance.launch { estado.animateScrollBy(-120f) }
                    Key.ButtonR1, Key.PageDown -> alcance.launch { estado.animateScrollBy(900f) }
                    Key.ButtonL1, Key.PageUp -> alcance.launch { estado.animateScrollBy(-900f) }
                    Key.DirectionRight -> tamano = (tamano + 1f).coerceAtMost(20f)
                    Key.DirectionLeft -> tamano = (tamano - 1f).coerceAtLeast(9f)
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(guia.titulo, style = MaterialTheme.typography.titleLarge, color = Colores.Oro, modifier = Modifier.weight(1f))
            BotonMystara("A−", { tamano = (tamano - 1f).coerceAtLeast(9f) }, estilo = EstiloBoton.DISCRETO)
            BotonMystara("A+", { tamano = (tamano + 1f).coerceAtMost(20f) }, estilo = EstiloBoton.DISCRETO)
            BotonMystara(stringResource(R.string.volver), onCerrar, estilo = EstiloBoton.DISCRETO)
        }
        Spacer(Modifier.height(6.dp))
        // Ancho del texto según su línea más larga: las guías anchas se mueven también de lado
        val anchoTexto = (maxCaracteres * tamano * 0.62f + 24f).dp
        Box(Modifier.fillMaxSize().horizontalScroll(horizontal)) {
            LazyColumn(state = estado, modifier = Modifier.width(anchoTexto).fillMaxHeight()) {
                items(lineas.size) { i ->
                    Text(
                        lineas[i].ifEmpty { " " },
                        fontFamily = FontFamily.Monospace,
                        fontSize = tamano.sp,
                        lineHeight = (tamano * 1.25f).sp,
                        color = Colores.Pergamino,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** Lector de guías en PDF: páginas una debajo de otra, con zoom y cruceta/L1/R1. */
@Composable
private fun LectorPdf(guia: Guia, onCerrar: () -> Unit) {
    val context = LocalContext.current
    val documento = remember(guia) { DocumentoPdf.abrir(context, guia) }
    DisposableEffect(documento) { onDispose { documento?.cerrar() } }
    var zoom by rememberSaveable { mutableStateOf(1f) }
    val estado = rememberLazyListState()
    val horizontal = rememberScrollState()
    val alcance = rememberCoroutineScope()
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    BackHandler(onBack = onCerrar)
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF15100C))
            .focusRequester(foco)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionDown -> alcance.launch { estado.animateScrollBy(200f) }
                    Key.DirectionUp -> alcance.launch { estado.animateScrollBy(-200f) }
                    Key.ButtonR1, Key.PageDown -> alcance.launch { estado.animateScrollToItem((estado.firstVisibleItemIndex + 1).coerceAtMost((documento?.paginas ?: 1) - 1)) }
                    Key.ButtonL1, Key.PageUp -> alcance.launch { estado.animateScrollToItem((estado.firstVisibleItemIndex - 1).coerceAtLeast(0)) }
                    Key.DirectionRight -> zoom = (zoom + 0.25f).coerceAtMost(3f)
                    Key.DirectionLeft -> zoom = (zoom - 0.25f).coerceAtLeast(0.5f)
                    else -> return@onPreviewKeyEvent false
                }
                true
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(guia.titulo, style = MaterialTheme.typography.titleLarge, color = Colores.Oro, modifier = Modifier.weight(1f))
            if (documento != null) {
                Text("${estado.firstVisibleItemIndex + 1} / ${documento.paginas}", style = MaterialTheme.typography.labelLarge,
                    color = Colores.TextoSuave)
            }
            BotonMystara("−", { zoom = (zoom - 0.25f).coerceAtLeast(0.5f) }, estilo = EstiloBoton.DISCRETO)
            BotonMystara("+", { zoom = (zoom + 0.25f).coerceAtMost(3f) }, estilo = EstiloBoton.DISCRETO)
            BotonMystara(stringResource(R.string.volver), onCerrar, estilo = EstiloBoton.DISCRETO)
        }
        Spacer(Modifier.height(6.dp))
        if (documento == null) {
            Text(stringResource(R.string.extras_pdf_error), color = Colores.OroClaro)
            return@Column
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val anchoPagina = maxWidth * zoom
            val anchoPx = with(LocalDensity.current) { anchoPagina.roundToPx() }
            Box(Modifier.fillMaxSize().horizontalScroll(horizontal)) {
                LazyColumn(
                    state = estado,
                    modifier = Modifier.width(anchoPagina).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(documento.paginas) { i -> PaginaPdf(documento, i, anchoPx) }
                }
            }
        }
    }
}

@Composable
private fun PaginaPdf(documento: DocumentoPdf, i: Int, anchoPx: Int) {
    val proporcion = remember(documento, i) { documento.proporcion(i) }
    val imagen by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, documento, i, anchoPx) {
        value = withContext(Dispatchers.IO) { documento.pagina(i, anchoPx)?.asImageBitmap() }
    }
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f / proporcion).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        val img = imagen
        if (img != null) {
            Image(img, contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxSize())
        } else {
            CircularProgressIndicator(color = Colores.OroOscuro)
        }
    }
}

@Composable
private fun Miniatura(img: ImagenArte, onClick: () -> Unit) {
    AsyncImage(
        model = img.uri,
        contentDescription = img.archivo,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .aspectRatio(4f / 3f)
            .border(1.5.dp, Colores.OroOscuro, FormaPlaca)
            .background(Colores.Piedra, FormaPlaca)
            .clickable(onClick = onClick),
    )
}

/**
 * Visor a pantalla completa: se pasa de foto deslizando o con la cruceta (también L1/R1).
 * Con [onBorrar] (pantallazos) aparece «Borrar», que también se pulsa con Y.
 */
@Composable
private fun Visor(
    imagenes: List<ImagenVisor>,
    inicio: Int,
    onCerrar: () -> Unit,
    onBorrar: ((Int) -> Unit)? = null,
) {
    BackHandler(onBack = onCerrar)
    val estado = rememberPagerState(initialPage = inicio.coerceIn(0, imagenes.lastIndex)) { imagenes.size }
    val alcance = rememberCoroutineScope()
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }

    // Zoom de la foto que se está viendo: R2 / L2 con el mando (o + y − del teclado) y
    // pellizcando con dos dedos. Con zoom, la cruceta o un dedo mueven la foto.
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }
    var tamano by remember { mutableStateOf(IntSize.Zero) }
    val conZoom = escala > 1.01f
    fun limitar(o: Offset, e: Float = escala): Offset {
        // Lo justo para llegar al borde de la foto ampliada, sin dejar hueco negro
        val maxX = tamano.width * (e - 1f) / 2f
        val maxY = tamano.height * (e - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }
    fun ampliar(factor: Float) {
        val nueva = (escala * factor).coerceIn(1f, ZOOM_MAXIMO)
        desplazamiento = limitar(desplazamiento * (nueva / escala), nueva)
        escala = nueva
    }
    fun quitarZoom() { escala = 1f; desplazamiento = Offset.Zero }
    fun mover(dx: Float, dy: Float) {
        desplazamiento = limitar(desplazamiento + Offset(dx * tamano.width * 0.15f, dy * tamano.height * 0.15f))
    }
    // Al cambiar de foto se empieza sin zoom
    LaunchedEffect(estado.currentPage) { quitarZoom() }

    BackHandler(enabled = conZoom) { quitarZoom() } // B con zoom: primero se quita el zoom

    fun ir(pagina: Int) {
        val destino = pagina.coerceIn(0, imagenes.lastIndex)
        if (destino != estado.currentPage) alcance.launch { estado.animateScrollToPage(destino) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { tamano = it }
            .focusRequester(foco)
            .focusable()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.ButtonR2, Key.Plus, Key.Equals, Key.NumPadAdd, Key.ZoomIn -> { ampliar(1.25f); true }
                    Key.ButtonL2, Key.Minus, Key.NumPadSubtract, Key.ZoomOut -> { ampliar(1f / 1.25f); true }
                    Key.DirectionRight -> { if (conZoom) mover(-1f, 0f) else ir(estado.currentPage + 1); true }
                    Key.DirectionLeft -> { if (conZoom) mover(1f, 0f) else ir(estado.currentPage - 1); true }
                    Key.DirectionUp -> { if (conZoom) mover(0f, 1f); conZoom }
                    Key.DirectionDown -> { if (conZoom) mover(0f, -1f); conZoom }
                    Key.ButtonR1 -> { ir(estado.currentPage + 1); true }
                    Key.ButtonL1 -> { ir(estado.currentPage - 1); true }
                    Key.DirectionCenter, Key.Enter, Key.ButtonA -> { onCerrar(); true }
                    Key.ButtonY -> { onBorrar?.invoke(estado.currentPage); onBorrar != null }
                    else -> false
                }
            }
            // Tocar: con zoom lo quita; sin zoom cierra la foto, como antes
            .clickable { if (conZoom) quitarZoom() else onCerrar() },
    ) {
        // Con zoom no se pasa de foto deslizando: el dedo mueve la foto ampliada
        HorizontalPager(state = estado, userScrollEnabled = !conZoom, modifier = Modifier.fillMaxSize()) { pagina ->
            val actual = pagina == estado.currentPage
            AsyncImage(
                model = imagenes[pagina].uri,
                contentDescription = imagenes[pagina].descripcion,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (actual) {
                            scaleX = escala; scaleY = escala
                            translationX = desplazamiento.x; translationY = desplazamiento.y
                        }
                    }
                    .pointerInput(actual) {
                        if (!actual) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val ev = awaitPointerEvent()
                                val dedos = ev.changes.count { it.pressed }
                                // Dos dedos: zoom. Un dedo solo cuenta si ya hay zoom (si no, pasa de foto)
                                if (dedos >= 2 || escala > 1.01f) {
                                    val nueva = (escala * ev.calculateZoom()).coerceIn(1f, ZOOM_MAXIMO)
                                    desplazamiento = limitar(desplazamiento * (nueva / escala) + ev.calculatePan(), nueva)
                                    escala = nueva
                                    ev.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } while (ev.changes.any { it.pressed })
                        }
                    },
            )
        }
        // Flechas: indican que hay más fotos a cada lado (y se pueden tocar)
        if (estado.currentPage > 0) {
            Text("‹", color = Colores.OroClaro.copy(alpha = 0.8f), fontSize = 54.sp,
                modifier = Modifier.align(Alignment.CenterStart).padding(8.dp).clickable { ir(estado.currentPage - 1) }.padding(8.dp))
        }
        if (estado.currentPage < imagenes.lastIndex) {
            Text("›", color = Colores.OroClaro.copy(alpha = 0.8f), fontSize = 54.sp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp).clickable { ir(estado.currentPage + 1) }.padding(8.dp))
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            imagenes.getOrNull(estado.currentPage)?.pie?.let { pie ->
                Text(pie, style = MaterialTheme.typography.labelMedium, color = Colores.Pergamino.copy(alpha = 0.8f))
            }
            Text(
                "${estado.currentPage + 1} / ${imagenes.size}" +
                    (if (conZoom) " · ×" + String.format(Locale.getDefault(), "%.1f", escala) else ""),
                style = MaterialTheme.typography.labelLarge,
                color = Colores.OroClaro,
            )
            Text(
                stringResource(R.string.extras_zoom_ayuda),
                style = MaterialTheme.typography.labelSmall,
                color = Colores.Pergamino.copy(alpha = 0.55f),
            )
        }
        if (onBorrar != null) {
            Text(
                stringResource(R.string.extras_pantallazo_borrar),
                style = MaterialTheme.typography.labelLarge,
                color = Colores.Pergamino,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Colores.CarmesiOscuro.copy(alpha = 0.85f), FormaPlaca)
                    .border(1.dp, Colores.Carmesi, FormaPlaca)
                    .clickable { onBorrar(estado.currentPage) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** Hasta cuánto se puede ampliar una foto de la galería. */
private const val ZOOM_MAXIMO = 5f

/** «Final Fight · 27/10/2026 22:41 · 10 puntos»: el pie de cada pantallazo. */
@Composable
private fun pieDe(p: Pantallazo): String {
    val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(p.fecha))
    return listOfNotNull(p.juego?.nombre, fecha, stringResource(R.string.ra_puntos, p.puntos)).joinToString(" · ")
}

/** Miniatura de un pantallazo con el nombre del logro y el juego debajo. */
@Composable
private fun TarjetaPantallazo(p: Pantallazo, onClick: () -> Unit) {
    Column(
        Modifier
            .background(Colores.Piedra, FormaPlaca)
            .border(1.5.dp, Colores.OroOscuro, FormaPlaca)
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        AsyncImage(
            model = p.uri,
            contentDescription = p.logro,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
        )
        Text(p.logro, style = MaterialTheme.typography.labelLarge, color = Colores.OroClaro, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Text(pieDe(p), style = MaterialTheme.typography.labelSmall, color = Colores.TextoSuave, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}
