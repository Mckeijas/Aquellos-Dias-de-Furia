package com.diasdefuria

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.diasdefuria.datos.RepositorioPartidas
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.tema.Colores
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import com.diasdefuria.ui.componentes.AjustePantalla
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.diasdefuria.datos.EstadoJuego
import com.diasdefuria.datos.ClaveAjuste
import com.diasdefuria.datos.LectorAjustes
import com.diasdefuria.audio.Efectos
import com.diasdefuria.audio.MusicaMenu
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.currentBackStackEntryAsState
import com.diasdefuria.juego.CapturaMando
import com.diasdefuria.juego.JuegoActivity
import com.diasdefuria.red.InvitadoActivity
import com.diasdefuria.juego.Mando
import android.view.KeyEvent
import com.diasdefuria.datos.Juego
import com.diasdefuria.datos.CatalogoRomsets
import com.diasdefuria.datos.CarpetaRoms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.diasdefuria.datos.RepositorioRoms
import com.diasdefuria.datos.ResultadoArchivo
import com.diasdefuria.datos.RomsetConocido
import com.diasdefuria.ui.pantallas.PantallaArte
import com.diasdefuria.ui.pantallas.PantallaLogo
import com.diasdefuria.ui.pantallas.PantallaControles
import com.diasdefuria.ui.pantallas.PantallaFiltros
import com.diasdefuria.ui.pantallas.PantallaIntro
import com.diasdefuria.ui.pantallas.PantallaMenu
import com.diasdefuria.ui.pantallas.PantallaJuegos
import com.diasdefuria.ui.pantallas.PantallaMultijugador
import com.diasdefuria.ui.pantallas.PantallaMods
import com.diasdefuria.ui.pantallas.PantallaPartidas
import com.diasdefuria.ui.pantallas.Seccion
import com.diasdefuria.ui.tema.TemaMystara
import kotlinx.coroutines.launch

/** Pon a false para entrar directamente al menú mientras no haya intro definitiva. */
private const val MOSTRAR_INTRO = true

private object Ruta {
    const val LOGO = "logo"
    const val INTRO = "intro"
    const val MENU = "menu"
    fun seccion(s: Seccion) = "seccion/${s.name.lowercase()}"
}

class MainActivity : ComponentActivity() {
    /** Se ha elegido «Salir»: al terminar de cerrarse, se acaba también el proceso. */
    private var salirDelTodo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaCompleta()
        setContent { TemaMystara { AjustePantalla { App(onSalir = ::salirDeLaApp) } } }
    }

    /**
     * Salir de verdad: se cierran todas las pantallas, la app desaparece de las recientes y,
     * al terminar, se acaba el proceso (antes Android lo dejaba en segundo plano).
     */
    private fun salirDeLaApp() {
        salirDelTodo = true
        finishAndRemoveTask()
    }

    /** Mando en el menú: A confirma y B vuelve atrás. */
    // Aviso falso de lint: super.dispatchKeyEvent es el método público de Activity
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        CapturaMando.oyente?.let { if (it(event)) return true }
        val equivalente = Mando.teclaDeMenu(event.keyCode) ?: return super.dispatchKeyEvent(event)
        return super.dispatchKeyEvent(
            KeyEvent(event.downTime, event.eventTime, event.action, equivalente, event.repeatCount, event.metaState)
        )
    }

    override fun onDestroy() {
        if (isFinishing) MusicaMenu.liberar()
        super.onDestroy()
        if (salirDelTodo) android.os.Process.killProcess(android.os.Process.myPid())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) pantallaCompleta()
    }

    /** Modo inmersivo: oculta barras de estado y navegación (se muestran deslizando). */
    private fun pantallaCompleta() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun App(onSalir: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { RepositorioRoms(context.applicationContext) }
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()

    var estados by remember { mutableStateOf<Map<Juego, EstadoJuego>>(emptyMap()) }
    var importando by remember { mutableStateOf<Juego?>(null) }
    var pendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var resultados by remember { mutableStateOf<List<String>>(emptyList()) }
    var detalle by remember { mutableStateOf<Juego?>(null) }
    var preguntarContinuar by remember { mutableStateOf<RomsetConocido?>(null) }
    val partidas = remember { RepositorioPartidas(context.applicationContext) }

    /** Si quedó guardado dónde se dejó el juego, se pregunta si continuar. */
    fun jugar(rs: RomsetConocido) {
        if (partidas.automatica(rs) != null) preguntarContinuar = rs
        else context.startActivity(JuegoActivity.intent(context, rs))
    }

    suspend fun refrescar() {
        estados = Juego.entries.associateWith { repo.estado(it) }
    }
    LaunchedEffect(Unit) { refrescar() }

    /** Primero el veredicto de cada juego afectado (¿se puede jugar?), luego el detalle por archivo. */
    fun mostrarResultados(res: List<ResultadoArchivo>, cabecera: String? = null) {
        val biosNuevas = res.mapNotNull { (it as? ResultadoArchivo.BiosAnadida)?.bios }.toSet()
        val afectados = (res.mapNotNull { (it as? ResultadoArchivo.ZipReconocido)?.romset?.juego } +
            CatalogoRomsets.todos.filter { it.bios in biosNuevas }.map { it.juego }).toSet()
            .ifEmpty { if (res.any { it is ResultadoArchivo.ClaveAnadida }) Juego.entries.toSet() else emptySet() }
        val veredictos = Juego.entries.filter { it in afectados }.mapNotNull { j -> estados[j]?.let { veredicto(context, it) } }
        resultados = listOfNotNull(cabecera) + veredictos + res.map { describir(context, it) }
    }

    // Se pueden elegir varios archivos a la vez: el ZIP padre, el Hispanic y una clave .key suelta
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val juego = Juego.porId(pendiente)
        pendiente = null
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        importando = juego ?: Juego.primero
        scope.launch {
            val res = repo.importar(uris, juego)
            refrescar()
            importando = null
            mostrarResultados(res)
        }
    }

    // «Ruta» y «Refresca»: importar de golpe todas las ROMs de una carpeta del móvil
    val carpeta = remember { CarpetaRoms(context.applicationContext, repo) }
    var nombreCarpeta by remember { mutableStateOf<String?>(null) }
    var refrescando by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { nombreCarpeta = withContext(Dispatchers.IO) { carpeta.nombre() } }

    fun refrescarCarpeta() {
        if (refrescando || importando != null) return
        refrescando = true
        scope.launch {
            val r = carpeta.refrescar()
            refrescar()
            refrescando = false
            val cabecera = when {
                r.sinAcceso -> context.getString(R.string.carpeta_sin_acceso)
                r.encontrados == 0 -> context.getString(R.string.carpeta_vacia, r.carpeta)
                r.resultados.isEmpty() -> context.getString(R.string.carpeta_nada_nuevo, r.carpeta, r.encontrados)
                else -> context.getString(R.string.carpeta_resumen, r.carpeta, r.encontrados, r.resultados.size, r.yaImportados)
            }
            mostrarResultados(r.resultados, cabecera)
        }
    }
    val elegirCarpeta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { arbol ->
        if (arbol == null) return@rememberLauncherForActivityResult
        val ok = runCatching { carpeta.elegir(arbol) }.isSuccess
        if (!ok) { resultados = listOf(context.getString(R.string.carpeta_sin_acceso)); return@rememberLauncherForActivityResult }
        nombreCarpeta = carpeta.nombre(arbol)
        refrescarCarpeta() // recién elegida: se importa ya lo que haya
    }
    fun elegirArchivos(juego: Juego) {
        pendiente = juego.id
        selector.launch(arrayOf("*/*"))
    }

    // Música: suena en todas las pantallas del menú; el logo y la intro van sin música (no suena mientras se juega)
    val ajustes = remember { LectorAjustes(context) }
    var retrasoMusica by remember { mutableStateOf(0L) }
    val rutaActual = nav.currentBackStackEntryAsState().value?.destination?.route
    val enMenu = rutaActual != null && rutaActual != Ruta.INTRO && rutaActual != Ruta.LOGO
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(enMenu, ciclo) {
        // Al volver del menú a la portada (B), la canción se para: allí solo suena la ciudad
        if (!enMenu) MusicaMenu.pausar()
        fun sonar() {
            if (enMenu && ajustes.booleano(ClaveAjuste.MUSICA, true)) {
                MusicaMenu.reproducir(context, ajustes.decimal(ClaveAjuste.VOLUMEN_MUSICA, 0.7f), retrasoMusica)
                retrasoMusica = 0
            }
        }
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_START -> sonar()
                Lifecycle.Event.ON_STOP -> MusicaMenu.pausar()
                else -> Unit
            }
        }
        if (ciclo.currentState.isAtLeast(Lifecycle.State.STARTED)) sonar()
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    NavHost(navController = nav, startDestination = Ruta.LOGO) {
        composable(Ruta.LOGO) {
            // Logo de Patrulla Retro (fundido de negro al logo y del logo a negro) y luego la intro
            PantallaLogo(onTerminar = {
                if (nav.currentDestination?.route == Ruta.LOGO) {
                    nav.navigate(if (MOSTRAR_INTRO) Ruta.INTRO else Ruta.MENU) { popUpTo(Ruta.LOGO) { inclusive = true } }
                }
            })
        }
        composable(Ruta.INTRO) {
            // Sin música: al pulsar START suena el puñetazo y se pasa al menú, donde empieza la canción
            PantallaIntro(
                onTerminar = { _ -> if (nav.currentDestination?.route == Ruta.INTRO) irAlMenu(nav) },
                // B / Atrás en la intro → «¿Realmente quieres salir…?» → Sí cierra la app
                onSalir = onSalir,
            )
        }
        composable(Ruta.MENU) {
            // B / Atrás en el menú principal: no cierra la app, vuelve a la portada (la intro, sin
            // menú, con el ruido de la ciudad); desde allí, otra vez B pregunta si se quiere salir
            BackHandler {
                nav.navigate(Ruta.INTRO) { popUpTo(Ruta.MENU) { inclusive = true } }
            }
            PantallaMenu(
                version = textoVersion(),
                onSeccion = { s -> nav.navigate(Ruta.seccion(s)) },
                onSalir = onSalir,
            )
        }
        composable(Ruta.seccion(Seccion.LEYENDAS)) {
            PantallaJuegos(
                estados = estados,
                importando = importando,
                carpeta = nombreCarpeta,
                refrescando = refrescando,
                onRuta = { if (!refrescando) elegirCarpeta.launch(carpeta.ruta) },
                onRefresca = { if (carpeta.ruta == null) elegirCarpeta.launch(null) else refrescarCarpeta() },
                onVolver = { nav.popBackStack() },
                onElegirZip = ::elegirArchivos,
                onJugar = ::jugar,
                onVerEstado = { juego -> detalle = juego },
                onElegirVersion = { rs -> repo.elegirVersion(rs); scope.launch { refrescar() } },
            )
        }
        composable(Ruta.seccion(Seccion.MULTIJUGADOR)) {
            PantallaMultijugador(
                estados = estados,
                onVolver = { nav.popBackStack() },
                onCrear = { rs, internet ->
                    context.startActivity(JuegoActivity.intent(context, rs, anfitrion = true, internet = internet))
                },
                onUnirse = { p -> context.startActivity(InvitadoActivity.intent(context, p)) },
                onUnirseCodigo = { host, puerto -> context.startActivity(InvitadoActivity.intent(context, host, puerto)) },
            )
        }
        composable(Ruta.seccion(Seccion.FILTROS)) { PantallaFiltros { nav.popBackStack() } }
        composable(Ruta.seccion(Seccion.MODS)) { PantallaMods { nav.popBackStack() } }
        composable(Ruta.seccion(Seccion.CONTROLES)) { PantallaControles { nav.popBackStack() } }
        composable(Ruta.seccion(Seccion.PARTIDAS)) {
            PantallaPartidas(
                onVolver = { nav.popBackStack() },
                onCargar = { p -> context.startActivity(JuegoActivity.intent(context, p.romset, p.archivo)) },
            )
        }
        composable(Ruta.seccion(Seccion.ARTE)) { PantallaArte { nav.popBackStack() } }
    }

    if (resultados.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { resultados = emptyList() },
            title = { Text(stringResource(R.string.importacion_titulo)) },
            text = { Text(resultados.joinToString("\n\n")) },
            confirmButton = {
                TextButton(onClick = { resultados = emptyList() }) { Text(stringResource(R.string.aceptar)) }
            },
        )
    }

    val estadoDetalle = detalle?.let { estados[it] }
    if (estadoDetalle != null) {
        DialogoIncompleto(
            estado = estadoDetalle,
            onElegir = { detalle = null; elegirArchivos(estadoDetalle.juego) },
            onJugarAlternativa = { rs -> detalle = null; jugar(rs) },
            onCerrar = { detalle = null },
        )
    }

    preguntarContinuar?.let { rs ->
        DialogoContinuar(
            romset = rs,
            fecha = partidas.automatica(rs)?.lastModified() ?: 0L,
            onContinuar = {
                preguntarContinuar = null
                context.startActivity(JuegoActivity.intent(context, rs, partidas.automatica(rs)))
            },
            onNueva = { preguntarContinuar = null; context.startActivity(JuegoActivity.intent(context, rs)) },
            onCerrar = { preguntarContinuar = null },
        )
    }
}

/** Explica qué le falta a un juego y ofrece completar o jugar la versión en inglés. */
@Composable
private fun DialogoIncompleto(
    estado: EstadoJuego,
    onElegir: () -> Unit,
    onJugarAlternativa: (RomsetConocido) -> Unit,
    onCerrar: () -> Unit,
) {
    val nombre = estado.juego.nombre
    val incompleto = estado.incompleto
    val texto = buildString {
        if (incompleto == null) {
            val alt = estado.alternativa
            append(stringResource(R.string.estado_sin_hispanic, alt?.region ?: "Europa"))
        } else {
            append(stringResource(R.string.estado_version, "${incompleto.nombre} · ${incompleto.region}"))
            val deBios = incompleto.bios?.let { CatalogoRomsets.bios[it] }.orEmpty().toSet()
            val clave = estado.faltan.filter { it.clave }
            val bios = estado.faltan.filter { it in deBios }
            val otros = estado.faltan.filterNot { it.clave || it in deBios }
            clave.forEach { append("\n\n").append(stringResource(R.string.estado_falta_clave, it.nombre)) }
            if (bios.isNotEmpty()) {
                append("\n\n").append(stringResource(R.string.estado_falta_bios, "${incompleto.bios}.zip",
                    bios.joinToString(", ") { it.nombre }))
            }
            if (otros.isNotEmpty()) {
                append("\n\n").append(
                    stringResource(R.string.estado_faltan_archivos,
                        otros.joinToString(", ") { it.nombre }, "${incompleto.padre ?: incompleto.nombre}.zip")
                )
            }
        }
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.estado_titulo, nombre)) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onElegir) { Text(stringResource(R.string.elegir_archivos)) } },
        dismissButton = {
            Row {
                estado.alternativa?.let { alt ->
                    TextButton(onClick = { onJugarAlternativa(alt) }) {
                        Text(stringResource(R.string.jugar_alternativa, alt.region))
                    }
                }
                TextButton(onClick = onCerrar) { Text(stringResource(R.string.cerrar)) }
            }
        },
    )
}

/** Mensaje claro de si el juego se puede jugar tras la importación. */
private fun veredicto(context: Context, e: EstadoJuego): String {
    val nombre = e.juego.nombre
    val jugable = e.jugable
    val incompleto = e.incompleto
    return when {
        jugable != null -> context.getString(
            if (jugable.hispanic) R.string.veredicto_listo else R.string.veredicto_listo_version,
            nombre, "${jugable.nombre} · ${jugable.region}",
        )
        incompleto != null -> {
            val base = context.getString(
                if (e.juego.tieneHispanic) R.string.veredicto_incompleto else R.string.veredicto_incompleto_general,
                nombre, e.faltan.joinToString(", ") { it.nombre },
            )
            e.alternativa?.let { base + " " + context.getString(R.string.veredicto_alternativa, it.region) } ?: base
        }
        e.alternativa != null -> context.getString(R.string.veredicto_solo_ingles, nombre, e.alternativa.region)
        else -> context.getString(R.string.veredicto_no_jugable, nombre)
    }
}

private fun describir(context: Context, r: ResultadoArchivo): String = when (r) {
    is ResultadoArchivo.ZipReconocido -> {
        val juego = r.romset.juego.nombre
        val version = "${r.romset.nombre} · ${r.romset.region}"
        buildString {
            append(context.getString(R.string.rom_reconocido, r.archivo, juego, version))
            if (r.reparados.isNotEmpty()) append(" ").append(context.getString(R.string.rom_reparados, r.reparados.joinToString(", ")))
            if (r.bios != null) append(" ").append(context.getString(R.string.rom_bios_dentro, "${r.bios}.zip"))
        }
    }
    is ResultadoArchivo.BiosAnadida -> context.getString(R.string.bios_anadida, r.archivo, "${r.bios}.zip")
    is ResultadoArchivo.ClaveAnadida ->
        if (r.romsets.isEmpty()) context.getString(R.string.clave_ya_estaba, r.archivo, r.clave)
        else context.getString(R.string.clave_anadida, r.archivo, r.clave, r.romsets.joinToString(", ") { "$it.zip" })
    is ResultadoArchivo.ClaveSinDestino -> context.getString(R.string.clave_sin_destino, r.archivo, r.clave)
    is ResultadoArchivo.NoReconocido ->
        if (r.esperado == null) context.getString(R.string.rom_no_reconocido, r.archivo)
        else context.getString(R.string.rom_no_es_del_juego, r.archivo, r.esperado.nombre)
    is ResultadoArchivo.OtroJuego -> context.getString(
        R.string.rom_otro_juego, r.archivo, r.encontrado.nombre, r.esperado.nombre)
    is ResultadoArchivo.Fallo -> context.getString(R.string.rom_error, r.archivo, r.motivo)
}

/** La intro solo navega una vez aunque se toque y además venza el tiempo. */
private fun irAlMenu(nav: NavHostController) {
    if (nav.currentDestination?.route != Ruta.INTRO) return
    nav.navigate(Ruta.MENU) { popUpTo(Ruta.INTRO) { inclusive = true } }
}

/** Solo el número de versión de la compilación («0.7.0»); la fecha ya no se muestra. */
private fun textoVersion(): String = BuildConfig.VERSION_NAME

/**
 * «Continuar donde lo dejaste»: aparece al entrar en un juego que tiene guardado automático.
 * Va en la misma ventana (no un diálogo de Android) para que el mando funcione igual que en el menú.
 */
@Composable
private fun DialogoContinuar(
    romset: RomsetConocido,
    fecha: Long,
    onContinuar: () -> Unit,
    onNueva: () -> Unit,
    onCerrar: () -> Unit,
) {
    val foco = remember { FocusRequester() }
    val focoNueva = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    BackHandler(onBack = onCerrar)
    MandoDialogo(listOf(foco, focoNueva), listOf(onContinuar, onNueva), onCerrar)
    val cuando = remember(fecha) { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fecha)) }
    Box(
        Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(460.dp)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.continuar_titulo), style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
            Text(
                stringResource(R.string.continuar_texto, romset.juego.nombre, cuando),
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            )
            BotonMystara(stringResource(R.string.continuar_si), onContinuar, Modifier.fillMaxWidth().focusRequester(foco),
                icono = Icons.Filled.PlayArrow, estilo = EstiloBoton.PRINCIPAL)
            BotonMystara(stringResource(R.string.continuar_nueva), onNueva, Modifier.fillMaxWidth().focusRequester(focoNueva),
                icono = Icons.Filled.RestartAlt)
        }
    }
}

/**
 * El mando en un aviso que se abre encima de otra pantalla (p. ej. «¿Continuar?» sobre la ruleta).
 * Mientras está abierto, la app recibe los botones antes que nadie:
 * - la cruceta solo se mueve entre sus botones (antes el foco podía quedarse en la lista de
 *   detrás y la cruceta movía los juegos del fondo);
 * - A solo vale si se pulsa con el aviso ya abierto. En la ruleta el juego se elige al pulsar A,
 *   y al soltarlo ese mismo A pulsaba «Continuar»: el juego entraba sin dejar elegir.
 * - B cierra el aviso.
 */
@Composable
private fun MandoDialogo(botones: List<FocusRequester>, acciones: List<() -> Unit>, onCerrar: () -> Unit) {
    val accionesActuales by androidx.compose.runtime.rememberUpdatedState(acciones)
    val cerrarActual by androidx.compose.runtime.rememberUpdatedState(onCerrar)
    DisposableEffect(Unit) {
        val anterior = CapturaMando.oyente
        var elegido = 0
        var aceptarArmado = false
        var cerrarArmado = false
        fun enfocar(i: Int) {
            elegido = i.coerceIn(0, botones.lastIndex)
            runCatching { botones[elegido].requestFocus() }
        }
        CapturaMando.oyente = { e ->
            val abajo = e.action == KeyEvent.ACTION_DOWN
            val arriba = e.action == KeyEvent.ACTION_UP
            when (e.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> { if (abajo) enfocar(elegido - 1); true }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> { if (abajo) enfocar(elegido + 1); true }
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BUTTON_START -> {
                    if (abajo && e.repeatCount == 0) aceptarArmado = true
                    else if (arriba && aceptarArmado) { aceptarArmado = false; accionesActuales.getOrNull(elegido)?.invoke() }
                    true
                }
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    if (abajo && e.repeatCount == 0) cerrarArmado = true
                    else if (arriba && cerrarArmado) { cerrarArmado = false; cerrarActual() }
                    true
                }
                else -> false
            }
        }
        onDispose { CapturaMando.oyente = anterior }
    }
}
