package com.diasdefuria.red

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import com.diasdefuria.ui.componentes.AjustePantalla
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.diasdefuria.R
import com.diasdefuria.datos.ClaveAjuste
import com.diasdefuria.datos.Juego
import com.diasdefuria.datos.LectorAjustes
import com.diasdefuria.juego.ControlesTactiles
import com.diasdefuria.juego.Mando
import com.diasdefuria.juego.TraductorMando
import com.diasdefuria.datos.AccionMando
import com.diasdefuria.datos.MapaBotones
import android.os.Handler
import android.os.Looper
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.pantallas.FondoBezel
import com.diasdefuria.ui.tema.Colores
import com.diasdefuria.ui.tema.TemaMystara
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pantalla del invitado: ve la partida que retransmite el anfitrión y le envía
 * sus controles (mando físico o táctiles). No emula nada ni necesita las ROMs.
 */
class InvitadoActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_DIRECCION = "direccion"
        private const val EXTRA_PUERTO = "puerto"
        private const val EXTRA_JUEGO = "juego"
        private const val EXTRA_ANFITRION = "anfitrion"

        fun intent(context: Context, p: PartidaEnRed) =
            intent(context, p.direccion.hostAddress ?: "", p.puerto, p.juego, p.anfitrion)

        /** Unirse escribiendo un código, una IP o un nombre de equipo. El juego llega al conectar. */
        fun intent(context: Context, host: String, puerto: Int, juego: String? = null, anfitrion: String = host) =
            Intent(context, InvitadoActivity::class.java)
                .putExtra(EXTRA_DIRECCION, host)
                .putExtra(EXTRA_PUERTO, puerto)
                .putExtra(EXTRA_JUEGO, juego)
                .putExtra(EXTRA_ANFITRION, anfitrion)
    }

    private var conexion: ConexionInvitado? = null
    private var estado by mutableStateOf<EstadoInvitado>(EstadoInvitado.Conectando)
    private var jugadores by mutableStateOf<List<String>>(emptyList())
    private var menuAbierto by mutableStateOf(false)
    private var mostrarTactiles by mutableStateOf(true)
    private var juego by mutableStateOf<Juego?>(null)
    private var ping by mutableStateOf<Int?>(null)
    /** Retraso medio de la imagen (ms), de la copia en el anfitrión a la pantalla de aquí. */
    private var retrasoImagen by mutableStateOf<Int?>(null)
    /** Diagnóstico: imágenes recibidas del anfitrión y mostradas aquí. */
    private var videoDiag by mutableStateOf<EstadoInvitado.Video?>(null)
    /** El anfitrión tiene la partida en pausa. */
    private var anfitrionEnPausa by mutableStateOf(false)
    private val bloqueoWifi by lazy { BloqueoWifi(this) }
    private val ajustes by lazy { LectorAjustes(this) }
    private val mapa by lazy { MapaBotones(this) }
    private val principal = Handler(Looper.getMainLooper())
    /** Los botones se traducen igual que en el anfitrión, con la asignación de «Controles». */
    private val traductor: TraductorMando by lazy {
        TraductorMando(
            mapa = mapa,
            enviar = { k, p -> conexion?.boton(k, p) },
            cruceta = ::crucetaTeclas,
            abrirPausa = { abrirPausa() },
            programar = { ms, f -> principal.postDelayed(f, ms) },
        )
    }

    private fun abrirPausa() {
        traductor.soltarTodo()
        menuAbierto = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaCompleta()
        val direccion = intent.getStringExtra(EXTRA_DIRECCION)?.trim()?.takeIf { it.isNotEmpty() }
        val puerto = intent.getIntExtra(EXTRA_PUERTO, 0)
        juego = Juego.porId(intent.getStringExtra(EXTRA_JUEGO))
        val anfitrion = intent.getStringExtra(EXTRA_ANFITRION).orEmpty()
        if (direccion == null || puerto == 0) estado = EstadoInvitado.Error("dirección no válida")
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        bloqueoWifi.activar()
        actualizarTactiles()

        onBackPressedDispatcher.addCallback(this) { menuAbierto = !menuAbierto }

        // La conexión empieza cuando la superficie de vídeo está lista
        val superficie = SurfaceView(this).apply {
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(h: SurfaceHolder) {
                    if (conexion == null && direccion != null && puerto != 0) {
                        conexion = ConexionInvitado(direccion, puerto, h.surface) { e ->
                            runOnUiThread {
                                when (e) {
                                    is EstadoInvitado.Jugadores -> jugadores = e.nombres
                                    is EstadoInvitado.Ping -> ping = e.ms
                                    is EstadoInvitado.Imagen -> retrasoImagen = e.ms
                                    is EstadoInvitado.Video -> videoDiag = e
                                    is EstadoInvitado.Pausa -> anfitrionEnPausa = e.pausado
                                    else -> {
                                        if (e is EstadoInvitado.Dentro) Juego.porId(e.juego)?.let { juego = it }
                                        estado = e
                                    }
                                }
                            }
                        }
                    }
                }
                override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {}
                override fun surfaceDestroyed(h: SurfaceHolder) { conexion?.cerrar(); conexion = null }
            })
        }

        setContent {
            TemaMystara { AjustePantalla {
                PantallaInvitado(
                    superficie = superficie,
                    nombreJuego = juego?.nombre ?: "",
                    ping = ping,
                    retrasoImagen = retrasoImagen,
                    videoDiag = videoDiag,
                    anfitrion = anfitrion,
                    estado = estado,
                    jugadores = jugadores,
                    proporcion = ajustes.aspecto.proporcion,
                    bezel = ajustes.bezel,
                    juego = juego,
                    mostrarTactiles = mostrarTactiles && estado is EstadoInvitado.Dentro && !menuAbierto,
                    botones = juego?.botones ?: 4,
                    opacidad = ajustes.decimal(ClaveAjuste.OPACIDAD_TACTIL, 0.6f),
                    vibracion = ajustes.booleano(ClaveAjuste.VIBRACION, true),
                    menuAbierto = menuAbierto,
                    anfitrionEnPausa = anfitrionEnPausa,
                    onBoton = { k, p ->
                        conexion?.boton(k, p)
                        // Moneda también pulsa Start (si está activado en «Controles»)
                        if (k == KeyEvent.KEYCODE_BUTTON_SELECT && !p && mapa.monedaConStart) {
                            principal.postDelayed({
                                conexion?.boton(KeyEvent.KEYCODE_BUTTON_START, true)
                                principal.postDelayed({ conexion?.boton(KeyEvent.KEYCODE_BUTTON_START, false) }, 120)
                            }, 150)
                        }
                    },
                    onDireccion = { x, y -> conexion?.direccion(x, y) },
                    onMenu = { menuAbierto = true },
                    tactilesVisibles = mostrarTactiles,
                    onAlternarTactiles = {
                        val p = getSharedPreferences("ajustes", Context.MODE_PRIVATE).edit()
                        if (mostrarTactiles) p.putBoolean(ClaveAjuste.CONTROLES_TACTILES, false)
                        else p.putBoolean(ClaveAjuste.CONTROLES_TACTILES, true).putBoolean(ClaveAjuste.OCULTAR_CON_MANDO, false)
                        p.commit()
                        actualizarTactiles()
                    },
                    onContinuar = { menuAbierto = false },
                    onSalir = { finish() },
                )
            }
}
        }
    }

    override fun onDestroy() {
        bloqueoWifi.soltar()
        conexion?.cerrar(); conexion = null
        super.onDestroy()
    }

    // ── Mando físico: se envía al anfitrión ──────────────────────────────────

    // Aviso falso de lint: super.dispatchKeyEvent es el método público de Activity
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (menuAbierto || estado !is EstadoInvitado.Dentro) {
            val esPausa = menuAbierto && mapa.accion(event.keyCode) == AccionMando.PAUSA
            if (event.keyCode in Mando.TECLAS_MENU || esPausa || (menuAbierto && event.keyCode == KeyEvent.KEYCODE_BUTTON_B)) {
                if (event.action == KeyEvent.ACTION_UP) {
                    if (estado !is EstadoInvitado.Dentro && estado !is EstadoInvitado.Conectando) finish()
                    else menuAbierto = !menuAbierto
                }
                return true
            }
            val eq = Mando.teclaDeMenu(event.keyCode) ?: return super.dispatchKeyEvent(event)
            return super.dispatchKeyEvent(KeyEvent(event.downTime, event.eventTime, event.action, eq, event.repeatCount, event.metaState))
        }
        if (traductor.tecla(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private var cx = 0f
    private var cy = 0f
    private fun crucetaTeclas(k: Int, p: Boolean) {
        when (k) {
            KeyEvent.KEYCODE_DPAD_UP -> cy = if (p) -1f else if (cy < 0) 0f else cy
            KeyEvent.KEYCODE_DPAD_DOWN -> cy = if (p) 1f else if (cy > 0) 0f else cy
            KeyEvent.KEYCODE_DPAD_LEFT -> cx = if (p) -1f else if (cx < 0) 0f else cx
            KeyEvent.KEYCODE_DPAD_RIGHT -> cx = if (p) 1f else if (cx > 0) 0f else cx
        }
        conexion?.direccion(cx, cy)
    }

    private var ultimaX = 0f
    private var ultimaY = 0f
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (menuAbierto || estado !is EstadoInvitado.Dentro) return super.dispatchGenericMotionEvent(event)
        if (event.isFromSource(InputDevice.SOURCE_JOYSTICK) && event.action == MotionEvent.ACTION_MOVE) {
            var x = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            var y = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            if (x == 0f && y == 0f) {
                x = umbral(event.getAxisValue(MotionEvent.AXIS_X))
                y = umbral(event.getAxisValue(MotionEvent.AXIS_Y))
            }
            // Solo se envía cuando cambia, para no saturar la wifi
            if (x != ultimaX || y != ultimaY) {
                ultimaX = x; ultimaY = y
                conexion?.direccion(x, y)
            }
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun umbral(v: Float) = if (abs(v) < 0.5f) 0f else if (v > 0) 1f else -1f

    override fun onResume() {
        super.onResume()
        pantallaCompleta()
        actualizarTactiles()
    }

    private fun actualizarTactiles() {
        val activos = ajustes.booleano(ClaveAjuste.CONTROLES_TACTILES, true)
        val ocultar = ajustes.booleano(ClaveAjuste.OCULTAR_CON_MANDO, true)
        mostrarTactiles = activos && !(ocultar && Mando.hayMandoFisico())
    }

    private fun pantallaCompleta() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun PantallaInvitado(
    superficie: SurfaceView,
    nombreJuego: String,
    ping: Int?,
    retrasoImagen: Int?,
    videoDiag: EstadoInvitado.Video?,
    anfitrion: String,
    estado: EstadoInvitado,
    jugadores: List<String>,
    proporcion: Float,
    bezel: String,
    juego: Juego?,
    mostrarTactiles: Boolean,
    botones: Int,
    opacidad: Float,
    vibracion: Boolean,
    menuAbierto: Boolean,
    anfitrionEnPausa: Boolean,
    onBoton: (Int, Boolean) -> Unit,
    onDireccion: (Float, Float) -> Unit,
    onMenu: () -> Unit,
    tactilesVisibles: Boolean,
    onAlternarTactiles: () -> Unit,
    onContinuar: () -> Unit,
    onSalir: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        FondoBezel(bezel, Modifier.fillMaxSize(), juego = juego)
        val d = LocalDensity.current
        var alto = constraints.maxHeight
        var ancho = (alto * proporcion).roundToInt()
        if (ancho > constraints.maxWidth) { ancho = constraints.maxWidth; alto = (ancho / proporcion).roundToInt() }
        AndroidView(
            factory = { superficie },
            modifier = Modifier.align(Alignment.Center).size(with(d) { ancho.toDp() }, with(d) { alto.toDp() }),
        )

        if (mostrarTactiles) {
            ControlesTactiles(opacidad, vibracion, botones, onBoton, onDireccion, onMenu)
        }

        // Estado de la conexión
        val texto = when (estado) {
            EstadoInvitado.Conectando -> stringResource(R.string.multi_conectando, anfitrion)
            is EstadoInvitado.Dentro -> null
            EstadoInvitado.Lleno -> stringResource(R.string.multi_lleno)
            EstadoInvitado.VersionDistinta -> stringResource(R.string.multi_version)
            EstadoInvitado.Terminada -> stringResource(R.string.multi_terminada)
            is EstadoInvitado.Error -> stringResource(R.string.multi_error_conexion, estado.motivo)
            is EstadoInvitado.Jugadores, is EstadoInvitado.Ping, is EstadoInvitado.Pausa, is EstadoInvitado.Imagen,
            is EstadoInvitado.Video -> null
        }
        // El anfitrión está en su menú de pausa: la imagen se queda quieta hasta que vuelva
        if (anfitrionEnPausa && estado is EstadoInvitado.Dentro && !menuAbierto) {
            Text(
                stringResource(R.string.multi_esperando_anfitrion),
                style = MaterialTheme.typography.headlineSmall, color = Colores.OroClaro, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
                    .background(Colores.Piedra.copy(alpha = 0.85f), FormaPlaca)
                    .border(2.dp, Colores.Oro, FormaPlaca)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            )
        }
        // Solo un icono con cuántos jugadores hay y un punto de color con el retraso;
        // el detalle (quién es quién) está en la pausa
        if (estado is EstadoInvitado.Dentro && !menuAbierto) {
            com.diasdefuria.ui.componentes.IndicadorRed(
                jugadores = jugadores.size + 1,
                esperando = false,
                ping = ping,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
            )
        }
        if (texto != null || menuAbierto) {
            val foco = remember { FocusRequester() }
            LaunchedEffect(texto, menuAbierto) { runCatching { foco.requestFocus() } }
            Box(Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.width(380.dp).background(Colores.Piedra, FormaPlaca).border(2.dp, Colores.Oro, FormaPlaca)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.menu_multijugador), style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
                    if (estado == EstadoInvitado.Conectando) CircularProgressIndicator(color = Colores.Oro)
                    Text(texto ?: stringResource(R.string.multi_en_partida, anfitrion), textAlign = TextAlign.Center)
                    if (estado is EstadoInvitado.Dentro) {
                        // Quién está conectado
                        Text(stringResource(R.string.multi_conectados), style = MaterialTheme.typography.titleLarge, color = Colores.Oro)
                        Text(stringResource(R.string.multi_anfitrion, anfitrion), style = MaterialTheme.typography.titleMedium,
                            color = Colores.OroClaro, textAlign = TextAlign.Center)
                        jugadores.forEach {
                            Text(it, style = MaterialTheme.typography.titleMedium, color = Colores.OroClaro, textAlign = TextAlign.Center)
                        }
                        Text(stringResource(R.string.multi_tu, estado.jugador) + (if (nombreJuego.isNotEmpty()) " · $nombreJuego" else ""),
                            style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave, textAlign = TextAlign.Center)
                        ping?.let {
                            Text(stringResource(R.string.multi_conexion, it), style = MaterialTheme.typography.labelMedium,
                                color = com.diasdefuria.ui.componentes.colorPing(it), textAlign = TextAlign.Center)
                        }
                        retrasoImagen?.let {
                            Text(stringResource(R.string.multi_retraso_imagen, it), style = MaterialTheme.typography.labelMedium,
                                color = com.diasdefuria.ui.componentes.colorPing(it / 2), textAlign = TextAlign.Center)
                        }
                        // Diagnóstico: si llegan imágenes pero no se muestran, el problema está en
                        // este dispositivo; si no llega ninguna, en el anfitrión o la red
                        videoDiag?.let {
                            Text(stringResource(R.string.multi_video_diag, it.recibidas, it.mostradas),
                                style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave, textAlign = TextAlign.Center)
                        }
                    } else if (jugadores.isNotEmpty()) {
                        Text(jugadores.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave,
                            textAlign = TextAlign.Center)
                    }
                    if (menuAbierto && estado is EstadoInvitado.Dentro) {
                        BotonMystara(stringResource(R.string.pausa_continuar), onContinuar, Modifier.fillMaxWidth().focusRequester(foco),
                            icono = Icons.Filled.PlayArrow, estilo = EstiloBoton.PRINCIPAL)
                        BotonMystara(
                            stringResource(if (tactilesVisibles) R.string.pausa_ocultar_tactiles else R.string.pausa_mostrar_tactiles),
                            onAlternarTactiles, Modifier.fillMaxWidth(), icono = Icons.Filled.TouchApp,
                        )
                    }
                    BotonMystara(stringResource(R.string.multi_salir), onSalir,
                        if (menuAbierto && estado is EstadoInvitado.Dentro) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().focusRequester(foco),
                        icono = Icons.AutoMirrored.Filled.ExitToApp, estilo = EstiloBoton.DISCRETO)
                }
            }
        }
    }
}
