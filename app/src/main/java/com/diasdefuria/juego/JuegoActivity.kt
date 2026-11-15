package com.diasdefuria.juego

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import com.diasdefuria.ui.componentes.AjustePantalla
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.diasdefuria.R
import com.diasdefuria.datos.CatalogoRomsets
import com.diasdefuria.datos.ClaveAjuste
import com.diasdefuria.datos.Filtro
import com.diasdefuria.datos.LectorAjustes
import com.diasdefuria.datos.MapaBotones
import com.diasdefuria.datos.Partida
import com.diasdefuria.datos.RepositorioPartidas
import com.diasdefuria.datos.Reglas
import com.diasdefuria.datos.Trucos
import com.diasdefuria.red.Anfitrion
import com.diasdefuria.red.Descubrimiento
import com.diasdefuria.red.Retransmision
import com.diasdefuria.red.nombreDispositivo
import com.diasdefuria.datos.RepositorioRoms
import com.diasdefuria.datos.RomsetConocido
import com.diasdefuria.ui.tema.TemaMystara
import com.diasdefuria.red.AperturaPuerto
import com.diasdefuria.red.AudioRed
import com.diasdefuria.datos.AjustesTrucos
import com.diasdefuria.datos.Puntuaciones
import com.diasdefuria.logros.Logro
import com.diasdefuria.logros.LogrosPartida
import com.diasdefuria.red.BloqueoWifi
import com.diasdefuria.red.CalidadRed
import com.diasdefuria.red.CodigoInvitacion
import com.diasdefuria.red.Invitacion
import com.swordfish.libretrodroid.GLRetroView
import com.diasdefuria.datos.Estadisticas
import android.os.SystemClock
import com.diasdefuria.datos.Pantallazos
import androidx.core.graphics.drawable.toBitmap
import coil.request.SuccessResult
import coil.request.ImageRequest
import coil.imageLoader
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.LibretroDroid
import com.swordfish.libretrodroid.ShaderConfig
import com.swordfish.libretrodroid.Variable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pantalla de juego: ejecuta FBNeo mediante LibretroDroid.
 *
 * Va en su propia Activity porque LibretroDroid es un único emulador nativo por
 * proceso: al cerrar la Activity se destruye limpiamente y el siguiente juego
 * empieza de cero.
 */
class JuegoActivity : ComponentActivity() {

    companion object {
        const val NUCLEO = "libfbneo_libretro_android.so"
        private const val EXTRA_ROMSET = "romset"
        private const val EXTRA_ESTADO = "estado"
        private const val EXTRA_ANFITRION = "anfitrion"
        private const val EXTRA_INTERNET = "internet"
        private const val OPCION_SERVICIO = "fbneo-diagnostic-input"
        private const val COMBO_SERVICIO = "Select + L + R"
        /** Lo que hay que mantener para que FBNeo pulse TEST (la combinación de COMBO_SERVICIO). */
        private val TECLAS_TEST = listOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1)
        /** SERVICE: FBNeo lo conecta a R3 en las placas que lo necesitan en su menú. */
        private val TECLAS_SERVICE = listOf(KeyEvent.KEYCODE_BUTTON_THUMBR)

        /** [anfitrion] = abre la partida en la wifi para que se unan hasta 3 invitados. */
        fun intent(
            context: Context, romset: RomsetConocido, estado: File? = null,
            anfitrion: Boolean = false, internet: Boolean = false,
        ) =
            Intent(context, JuegoActivity::class.java)
                .putExtra(EXTRA_ROMSET, romset.nombre)
                .putExtra(EXTRA_ESTADO, estado?.absolutePath)
                .putExtra(EXTRA_ANFITRION, anfitrion || internet)
                .putExtra(EXTRA_INTERNET, internet)

        fun nucleoInstalado(context: Context) =
            File(context.applicationInfo.nativeLibraryDir, NUCLEO).isFile
    }

    private var vista: GLRetroView? = null
    private lateinit var romset: RomsetConocido
    private val partidas by lazy { RepositorioPartidas(this) }
    private val ajustes by lazy { LectorAjustes(this) }
    private val mapa by lazy { MapaBotones(this) }
    private val principal = Handler(Looper.getMainLooper())
    private val traductor: TraductorMando by lazy {
        TraductorMando(
            mapa = mapa,
            enviar = { k, p -> vista?.sendKeyEvent(if (p) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, k, 0) },
            cruceta = { k, p -> vista?.sendKeyEvent(if (p) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, k, 0) },
            abrirPausa = ::abrirMenu,
            programar = { ms, f -> principal.postDelayed(f, ms) },
        )
    }

    // Estado que observa la interfaz
    private var menuAbierto by mutableStateOf(false)
    private var error by mutableStateOf<String?>(null)
    private var aviso by mutableStateOf<String?>(null)
    /**
     * Dentro del menú de servicio de la recreativa: X (botón 3) hace de botón TEST y
     * Y (botón 4) de botón SERVICE. Se activa al abrir «Configuración de la recreativa»
     * y se apaga al abrir la pausa (así X e Y vuelven a ser los botones 3 y 4 del juego).
     */
    private var modoServicio by mutableStateOf(false)
    private var hayUltima by mutableStateOf(false)
    private var panel by mutableStateOf(PanelPausa.MENU)
    private var ranuras by mutableStateOf<List<Partida?>>(emptyList())
    private var cargables by mutableStateOf<List<Partida>>(emptyList())
    private var mostrarTactiles by mutableStateOf(true)
    /** Marco de los laterales; se puede cambiar desde la pausa. */
    private var bezelActual by mutableStateOf(com.diasdefuria.datos.Bezels.POR_DEFECTO)

    // Multijugador: este dispositivo es el anfitrión
    private var anfitrion: Anfitrion? = null
    private var retransmision: Retransmision? = null
    /** Avisa a [retransmision] de cada imagen que dibuja el juego. */
    private var avisoFotogramas: Job? = null
    private var descubrimiento: Descubrimiento? = null
    /** null = partida de un solo dispositivo; lista vacía = sala abierta sin invitados. */
    private var jugadoresRed by mutableStateOf<List<String>?>(null)
    private var invitacion by mutableStateOf<Invitacion?>(null)

    // RetroAchievements
    private var logros: LogrosPartida? = null
    private var logroVisible by mutableStateOf<Logro?>(null)
    private val colaLogros = ArrayDeque<Logro>()
    private var apertura: AperturaPuerto? = null
    private var audioRed: AudioRed? = null
    private val bloqueoWifi by lazy { BloqueoWifi(this) }

    /** Estado del juego capturado al abrir el menú (el emulador está parado mientras). */
    private var instantanea: ByteArray? = null
    /** Imagen del juego al abrir el menú (miniatura de la partida guardada). */
    private var miniatura: Bitmap? = null
    private var cerrando = false
    /** El juego ya ha dibujado su primer fotograma (antes no hay nada que guardar). */
    private var arrancado = false
    private var autoguardadoPeriodico: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pantallaCompleta()

        val rs = CatalogoRomsets.porNombre(intent.getStringExtra(EXTRA_ROMSET).orEmpty())
        if (rs == null) { finish(); return }
        romset = rs
        val zip = RepositorioRoms(this).archivo(rs)
        bezelActual = ajustes.bezel

        when {
            !nucleoInstalado(this) -> error = getString(R.string.juego_falta_nucleo, NUCLEO)
            !zip.isFile -> error = getString(R.string.juego_falta_zip, rs.archivoZip)
            else -> crearEmulador(zip)
        }
        actualizarTactiles()

        // Gesto «atrás» de Android: abre o cierra el menú de pausa
        onBackPressedDispatcher.addCallback(this) {
            when {
                error != null || vista == null -> finish()
                panel != PanelPausa.MENU -> { panel = PanelPausa.MENU }
                menuAbierto -> reanudar()
                else -> abrirMenu()
            }
        }

        setContent {
            TemaMystara { AjustePantalla {
                PantallaEmulacion(
                    vista = vista,
                    romset = romset,
                    aspecto = ajustes.aspecto,
                    bezel = bezelActual,
                    escaladoEntero = ajustes.booleano(ClaveAjuste.ESCALADO_ENTERO, false),
                    mostrarTactiles = mostrarTactiles && !menuAbierto && error == null,
                    opacidadTactiles = ajustes.decimal(ClaveAjuste.OPACIDAD_TACTIL, 0.6f),
                    vibracion = ajustes.booleano(ClaveAjuste.VIBRACION, true),
                    menuAbierto = menuAbierto,
                    hayUltima = hayUltima,
                    error = error,
                    aviso = aviso,
                    modoServicio = modoServicio && !menuAbierto,
                    onBoton = ::botonTactil,
                    onDireccion = { x, y -> vista?.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, x, y, 0) },
                    onAbrirMenu = ::abrirMenu,
                    onContinuar = ::reanudar,
                    onReiniciar = ::reiniciar,
                    onMenuServicio = ::menuServicio,
                    onElegirBezel = { b ->
                        getSharedPreferences("ajustes", Context.MODE_PRIVATE).edit().putString(ClaveAjuste.BEZEL, b).apply()
                        bezelActual = b
                    },
                    panel = panel,
                    onPanel = { panel = it },
                    ranuras = ranuras,
                    cargables = cargables,
                    onGuardarEn = ::guardarEn,
                    onCargar = ::cargar,
                    onCambiarTruco = { t, opcion ->
                        // Se aplica al momento: FBNeo relee sus opciones al volver a la partida
                        vista?.updateVariables(Variable(Trucos.clave(t, romset), Trucos.valor(t, opcion)))
                        // Con trucos, los logros se pausan (se comprueba tras guardarse la elección)
                        principal.post { logros?.trucosActivos = AjustesTrucos(this).hayActivos(this, romset.juego) }
                    },
                    onSalir = ::salir,
                    onAvisoVisto = { aviso = null },
                    jugadoresRed = jugadoresRed,
                    invitacion = invitacion,
                    onCompartir = ::compartirInvitacion,
                    onCopiar = ::copiarInvitacion,
                    tactilesVisibles = mostrarTactiles,
                    onAlternarTactiles = ::alternarTactiles,
                    logro = logroVisible,
                    onAbrirSala = ::abrirSalaDesdePausa,
                    onUnirse = ::unirseDesdePausa,
                )
            }
}
        }
    }

    private fun crearEmulador(zip: File) {
        val sistema = File(filesDir, "sistema").apply { mkdirs() }
        // Trucos y reglas (DIP): FBNeo los lee como opciones del núcleo al arrancar
        Trucos.instalar(this, romset, sistema)
        // Tablas de récords: se guardan al salir y se comparten entre versiones del mismo juego
        val guardados = File(filesDir, "saves").apply { mkdirs() }
        Puntuaciones.preparar(this, sistema, guardados, romset)
        val opciones = Trucos.variables(this, romset) + Reglas.variables(this, romset) +
            // Menú de servicio de la recreativa: se abre con Select + L + R a la vez (lo pulsa la app
            // desde la pausa). Así mantener Start no lo abre por accidente, como pasa por defecto.
            Variable(OPCION_SERVICIO, COMBO_SERVICIO) +
            Variable("fbneo-hiscores", "enabled")
        val datos = GLRetroViewData(this).apply {
            coreFilePath = NUCLEO
            gameFilePath = zip.absolutePath
            systemDirectory = sistema.absolutePath
            variables = opciones.toTypedArray()
            savesDirectory = guardados.absolutePath
            shader = shaderDe(ajustes.filtro)
            preferLowLatencyAudio = true
            rumbleEventsEnabled = false
        }
        val v = GLRetroView(this, datos)
        lifecycle.addObserver(v)
        vista = v

        lifecycleScope.launch {
            v.getGLRetroErrors().collect { codigo -> error = textoError(codigo) }
        }

        // Partida elegida en «Partidas guardadas»: se carga en cuanto el juego arranca
        intent.getStringExtra(EXTRA_ESTADO)?.let { ruta ->
            lifecycleScope.launch {
                v.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().first()
                delay(300)
                val ok = withContext(Dispatchers.IO) {
                    runCatching { v.unserializeState(File(ruta).readBytes()) }.getOrDefault(false)
                }
                aviso = getString(if (ok) R.string.pausa_cargada else R.string.pausa_error_cargar)
            }
        }
        hayUltima = partidas.ultima(romset) != null
        if (intent.getBooleanExtra(EXTRA_ANFITRION, false)) abrirSala(v, intent.getBooleanExtra(EXTRA_INTERNET, false))

        // Guardado automático cada minuto mientras se juega: si Android cierra la app
        // o se apaga la pantalla, se continúa casi donde se estaba
        lifecycleScope.launch {
            v.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().first()
            delay(1500)
            arrancado = true
            // Estadísticas: una partida más y empieza a contar el tiempo jugado
            Estadisticas.partidaNueva(this@JuegoActivity, romset.juego)
            if (!menuAbierto) empezarCrono()
            iniciarLogros()
        }
        autoguardadoPeriodico = lifecycleScope.launch {
            while (isActive) {
                delay(60_000)
                // Solo con el juego en marcha y en pantalla (pausado, serializar bloquearía el emulador)
                if (!arrancado || menuAbierto || cerrando || error != null ||
                    !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) ||
                    !ajustes.booleano(ClaveAjuste.AUTOGUARDADO, true)) continue
                val imagen = capturarImagen()
                val datos = withContext(Dispatchers.IO) { runCatching { v.serializeState() }.getOrNull() } ?: continue
                if (imagen != null) miniatura = imagen
                withContext(Dispatchers.IO) { runCatching { partidas.guardar(romset, datos, null, miniatura) } }
            }
        }
    }

    /**
     * Abre la partida en la red: servidor, anuncio en la wifi y retransmisión del vídeo.
     * Por internet además abre el puerto en el router y prepara el código de invitación.
     */
    private fun abrirSala(v: GLRetroView, internet: Boolean) {
        if (anfitrion != null || jugadoresRed != null) return
        val calidad = if (internet) CalidadRed.INTERNET else CalidadRed.CASA
        jugadoresRed = emptyList()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        bloqueoWifi.activar()
        lifecycleScope.launch {
            val a = withContext(Dispatchers.IO) {
                runCatching {
                    Anfitrion(
                        juego = romset.juego.id,
                        calidad = calidad,
                        alPulsar = { puerto, tecla, pulsado ->
                            vista?.sendKeyEvent(if (pulsado) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, tecla, puerto)
                        },
                        alMover = { puerto, x, y -> vista?.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, x, y, puerto) },
                        alCambiarJugadores = { nombres -> runOnUiThread { jugadoresRed = nombres } },
                        pedirFotogramaClave = { retransmision?.pedirFotogramaClave() },
                    )
                }.getOrNull()
            }
            if (a == null || isFinishing) { aviso = getString(R.string.multi_error_sala); jugadoresRed = null; return@launch }
            anfitrion = a
            descubrimiento = Descubrimiento(this@JuegoActivity).also { it.anunciar(a.puerto, romset.juego.id, nombreDispositivo()) }
            val ipLocal = withContext(Dispatchers.IO) { AperturaPuerto.ipLocalWifi() }
            invitacion = Invitacion(
                internet = internet, puerto = a.puerto, ipLocal = ipLocal,
                codigoCasa = ipLocal?.let { CodigoInvitacion.crear(it, a.puerto) },
            )
            if (internet) prepararInternet(a.puerto)
            // La retransmisión empieza cuando el juego ya está dibujando
            if (!arrancado) v.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().first()
            // Cada imagen que dibuja el juego se manda en cuanto sale (ver Retransmision.nuevoFotograma)
            avisoFotogramas?.cancel()
            avisoFotogramas = lifecycleScope.launch {
                v.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>()
                    .collect { retransmision?.nuevoFotograma() }
            }
            retransmision = runCatching {
                Retransmision(v, calidad) { anfitrion }.also {
                    it.iniciar()
                    // Abierta desde la pausa: la imagen sale en cuanto se vuelve a la partida
                    if (menuAbierto) { it.pausar(true); Thread { a.avisarPausa(true) }.start() }
                }
            }.onFailure { aviso = getString(R.string.multi_error_video) }.getOrNull()
            // El sonido también viaja a los invitados (si el dispositivo lo permite)
            val audio = AudioRed(NUCLEO, calidad) { anfitrion }
            if (withContext(Dispatchers.IO) { audio.iniciar() }) audioRed = audio
            else aviso = getString(R.string.multi_sin_audio)
        }
    }

    private fun prepararInternet(puerto: Int) {
        lifecycleScope.launch {
            val ap = AperturaPuerto()
            val r = withContext(Dispatchers.IO) { ap.abrir(puerto, "Días de Furia") }
            if (isFinishing || anfitrion == null) { withContext(Dispatchers.IO) { ap.cerrar() }; return@launch }
            apertura = ap
            val ip = r.ipInvitacion
            val estado = when {
                ip == null -> Invitacion.Apertura.SIN_INTERNET
                r.cgnat -> Invitacion.Apertura.CGNAT
                r.abierto -> Invitacion.Apertura.ABIERTO
                else -> Invitacion.Apertura.MANUAL
            }
            invitacion = invitacion?.copy(
                preparando = false,
                ipPublica = ip,
                ipLocal = r.ipLocal ?: invitacion?.ipLocal,
                codigoInternet = ip?.let { CodigoInvitacion.crear(it, puerto) },
                apertura = estado,
            )
            aviso = getString(if (estado == Invitacion.Apertura.ABIERTO) R.string.inet_listo else R.string.inet_revisar)
        }
    }

    /** Desde el menú de pausa: la partida en solitario pasa a ser multijugador sin reiniciar. */
    private fun abrirSalaDesdePausa(internet: Boolean) {
        val v = vista ?: return
        abrirSala(v, internet)
        panel = PanelPausa.INVITAR
    }

    /**
     * Desde el menú de pausa: deja esta partida (guardándola, como al salir) y entra como
     * invitado en la de otro dispositivo.
     */
    private fun unirseDesdePausa(host: String, puerto: Int, juego: String?) {
        if (cerrando) return
        val destino = com.diasdefuria.red.InvitadoActivity.intent(this, host, puerto, juego)
        salir()
        startActivity(destino)
    }

    /**
     * Cierra la partida en red. Tiene que hacerse con el núcleo aún cargado: al cerrar la
     * Activity, LibretroDroid lo descarga antes de llegar a onDestroy, y devolver entonces
     * el sonido al núcleo (AudioRed) cerraba toda la app.
     */
    private fun cerrarSala() {
        avisoFotogramas?.cancel(); avisoFotogramas = null
        retransmision?.detener(); retransmision = null
        audioRed?.detener(); audioRed = null
        descubrimiento?.cerrar(); descubrimiento = null
        val a = anfitrion; anfitrion = null
        val ap = apertura; apertura = null
        if (a != null || ap != null) Thread { a?.cerrar(); ap?.cerrar() }.start()
        bloqueoWifi.soltar()
        jugadoresRed = null
        invitacion = null
    }

    /** Comparte el código por WhatsApp, Telegram, correo… */
    private fun compartirInvitacion() {
        val inv = invitacion ?: return
        val codigo = inv.codigoInternet ?: inv.codigoCasa ?: return
        val texto = getString(R.string.inet_mensaje, romset.juego.nombre, codigo)
        runCatching {
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto),
                getString(R.string.inet_compartir),
            ))
        }
    }

    private fun copiarInvitacion() {
        val inv = invitacion ?: return
        val codigo = inv.codigoInternet ?: inv.codigoCasa ?: return
        val portapapeles = getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        portapapeles?.setPrimaryClip(android.content.ClipData.newPlainText("Días de Furia", codigo))
        aviso = getString(R.string.inet_copiado)
    }

    // ── RetroAchievements ────────────────────────────────────────────────────

    private fun iniciarLogros() {
        if (logros != null || cerrando) return
        val l = LogrosPartida(
            context = this,
            romset = romset,
            nucleo = NUCLEO,
            alMensaje = { aviso = it },
            alLogro = {
                pantallazoDeLogro(it)
                colaLogros.addLast(it); if (logroVisible == null) siguienteLogro()
            },
        )
        if (!l.habilitado) return
        logros = l
        l.trucosActivos = AjustesTrucos(this).hayActivos(this, romset.juego)
        l.iniciar()
        l.jugando = !menuAbierto && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }

    /**
     * Pantallazo del logro (Extras › Pantallazos): la imagen del juego en el momento en que
     * salta, con el aviso del logro y su icono pintados encima. Se captura a 720 px de alto
     * y con la proporción con que se ve el juego en pantalla.
     */
    private fun pantallazoDeLogro(l: Logro) {
        val v = vista ?: return
        lifecycleScope.launch {
            val alto = 720
            val ancho = if (v.width > 0 && v.height > 0) (alto * v.width.toFloat() / v.height).roundToInt() else 960
            val captura = capturarImagen(ancho, alto) ?: return@launch
            withContext(Dispatchers.IO) {
                val icono = runCatching {
                    val r = imageLoader.execute(
                        ImageRequest.Builder(this@JuegoActivity).data(l.imagen).allowHardware(false).size(256).build()
                    )
                    (r as? SuccessResult)?.drawable?.toBitmap()
                }.getOrNull()
                Pantallazos.guardar(this@JuegoActivity, captura, romset, l.titulo, l.descripcion, l.puntos, icono)
                captura.recycle()
            }
        }
    }

    /** Los logros se enseñan de uno en uno, unos segundos cada uno. */
    private fun siguienteLogro() {
        val l = colaLogros.removeFirstOrNull()
        logroVisible = l
        if (l != null) lifecycleScope.launch {
            delay(5000)
            logroVisible = null
            delay(400)
            siguienteLogro()
        }
    }

    private fun detenerLogros() {
        logros?.detener()
        logros = null
    }

    override fun onDestroy() {
        detenerLogros()
        cerrarSala()
        super.onDestroy()
    }

    private fun shaderDe(f: Filtro): ShaderConfig = when (f) {
        Filtro.NINGUNO -> ShaderConfig.Sharp
        Filtro.BILINEAL -> ShaderConfig.Default
        Filtro.MEJORADO -> ShaderConfig.CUT2()
        Filtro.CRT -> ShaderConfig.CRT
        Filtro.LCD -> ShaderConfig.LCD
    }

    private fun textoError(codigo: Int): String = getString(
        when (codigo) {
            GLRetroView.ERROR_LOAD_LIBRARY -> R.string.error_nucleo
            GLRetroView.ERROR_LOAD_GAME -> R.string.error_juego
            GLRetroView.ERROR_GL_NOT_COMPATIBLE -> R.string.error_gl
            else -> R.string.error_generico
        }
    )

    // ── Menú de pausa ────────────────────────────────────────────────────────

    private fun abrirMenu() {
        val v = vista ?: return
        if (menuAbierto || cerrando) return
        traductor.soltarTodo()
        if (modoServicio) { soltarServicio(); modoServicio = false }
        lifecycleScope.launch {
            capturarImagen()?.let { miniatura = it }
            instantanea = withContext(Dispatchers.IO) { runCatching { v.serializeState() }.getOrNull() }
            pausarEmulacion()
            refrescarPartidas()
            panel = PanelPausa.MENU
            menuAbierto = true
        }
    }

    /** Copia la imagen que se ve en pantalla (para la miniatura de las partidas). */
    private suspend fun capturarImagen(ancho: Int = 320, alto: Int = 240): Bitmap? {
        val v = vista ?: return null
        if (!arrancado || !v.holder.surface.isValid) return null
        val bmp = runCatching { Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888) }.getOrNull() ?: return null
        return withTimeoutOrNull(600) {
            suspendCancellableCoroutine { c ->
                runCatching {
                    PixelCopy.request(v, bmp, { r -> if (c.isActive) c.resume(if (r == PixelCopy.SUCCESS) bmp else null) }, principal)
                }.onFailure { if (c.isActive) c.resume(null) }
            }
        }
    }

    private suspend fun refrescarPartidas() {
        val (r, c) = withContext(Dispatchers.IO) { partidas.ranuras(romset) to partidas.listar(romset) }
        ranuras = r
        cargables = c
        hayUltima = c.isNotEmpty()
    }

    // ── Tiempo jugado (Estadísticas): solo cuenta con el juego en marcha y en pantalla ──
    private var jugandoDesde = 0L

    private fun empezarCrono() {
        if (arrancado && jugandoDesde == 0L) jugandoDesde = SystemClock.elapsedRealtime()
    }

    private fun pararCrono() {
        if (jugandoDesde == 0L) return
        Estadisticas.sumarTiempo(this, romset.juego, SystemClock.elapsedRealtime() - jugandoDesde)
        jugandoDesde = 0L
    }

    private fun pausarEmulacion() {
        pararCrono()
        logros?.jugando = false
        retransmision?.pausar(true)
        anfitrion?.let { a -> Thread { a.avisarPausa(true) }.start() }
        val v = vista ?: return
        v.onPause()
        LibretroDroid.pause()
    }

    private fun reanudarEmulacion() {
        empezarCrono()
        logros?.jugando = true
        retransmision?.pausar(false)
        anfitrion?.let { a -> Thread { a.avisarPausa(false) }.start() }
        val v = vista ?: return
        LibretroDroid.resume()
        v.onResume()
        audioRed?.reenganchar()
    }

    private fun reanudar() {
        if (!menuAbierto) return
        panel = PanelPausa.MENU
        menuAbierto = false
        traductor.recargar()
        reanudarEmulacion()
    }

    /** Guarda la partida tal como estaba al abrir el menú en la ranura [n] (1..6). */
    private fun guardarEn(n: Int) {
        val datos = instantanea
        if (datos == null) { aviso = getString(R.string.pausa_error_guardar); return }
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { partidas.guardar(romset, datos, n, miniatura) }.isSuccess }
            refrescarPartidas()
            aviso = if (ok) getString(R.string.pausa_guardada_en, n) else getString(R.string.pausa_error_guardar)
        }
    }

    private fun cargar(partida: Partida) {
        val v = vista ?: return
        reanudar()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { v.unserializeState(partida.archivo.readBytes()) }.getOrDefault(false)
            }
            aviso = getString(if (ok) R.string.pausa_cargada else R.string.pausa_error_cargar)
        }
    }

    private fun reiniciar() {
        val v = vista ?: return
        reanudar()
        lifecycleScope.launch { withContext(Dispatchers.IO) { v.reset() } }
    }

    /**
     * Menú de servicio (test) de la placa original: dificultad, vidas, continuaciones,
     * idioma… Se ajusta como en la recreativa y se sale eligiendo EXIT; la placa
     * guarda la configuración y vuelve al juego.
     */
    private fun menuServicio() {
        val v = vista ?: return
        reanudar()
        modoServicio = true
        val teclas = listOf(KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1)
        principal.postDelayed({
            teclas.forEach { v.sendKeyEvent(KeyEvent.ACTION_DOWN, it, 0) }
            principal.postDelayed({ teclas.forEach { v.sendKeyEvent(KeyEvent.ACTION_UP, it, 0) } }, 250)
        }, 150)
        aviso = getString(R.string.servicio_aviso)
    }

    /**
     * Botones del panel de la placa durante el menú de servicio:
     * - TEST (X / botón 3): FBNeo lo pulsa con la combinación Select + L + R
     *   (opción fbneo-diagnostic-input); se mantiene mientras se mantenga X.
     * - SERVICE (Y / botón 4): FBNeo lo conecta a R3 en las placas que lo usan para moverse
     *   por el menú (Sega System 24/32, como Golden Axe II). En las demás placas el núcleo
     *   no conecta SERVICE a ningún botón y no hace nada.
     * Devuelve true si la tecla era X o Y (ya enviada al núcleo).
     */
    private fun teclaDeServicio(tecla: Int, accion: Int, repeticion: Int): Boolean {
        val teclas = when (tecla) {
            KeyEvent.KEYCODE_BUTTON_X -> TECLAS_TEST
            KeyEvent.KEYCODE_BUTTON_Y -> TECLAS_SERVICE
            else -> return false
        }
        val v = vista ?: return true
        when {
            accion == KeyEvent.ACTION_DOWN && repeticion == 0 -> teclas.forEach { v.sendKeyEvent(KeyEvent.ACTION_DOWN, it, 0) }
            accion == KeyEvent.ACTION_UP -> teclas.forEach { v.sendKeyEvent(KeyEvent.ACTION_UP, it, 0) }
        }
        return true
    }

    private fun soltarServicio() {
        val v = vista ?: return
        (TECLAS_TEST + TECLAS_SERVICE).forEach { v.sendKeyEvent(KeyEvent.ACTION_UP, it, 0) }
    }

    /** Salir al menú principal del launcher, guardando dónde se estaba. */
    private fun salir() {
        if (cerrando) return
        cerrando = true
        pararCrono()
        autoguardadoPeriodico?.cancel()
        val datos = instantanea
        if (datos != null && arrancado && ajustes.booleano(ClaveAjuste.AUTOGUARDADO, true)) {
            runCatching { partidas.guardar(romset, datos, null, miniatura) }
        }
        detenerLogros()
        // Antes de finish(): con la partida en red abierta, cerrarla después tumbaba la app
        cerrarSala()
        finish()
    }

    /**
     * Botón Inicio / Recientes de Android: se guarda antes de que la app pase a segundo
     * plano (y Android pueda cerrarla). El emulador aún está en marcha en este momento.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (cerrando || !arrancado || error != null || !ajustes.booleano(ClaveAjuste.AUTOGUARDADO, true)) return
        val v = vista ?: return
        val datos = if (menuAbierto) instantanea else {
            val tarea = FutureTask { runCatching { v.serializeState() }.getOrNull() }
            Thread(tarea, "autoguardado").apply { isDaemon = true }.start()
            runCatching { tarea.get(1500, TimeUnit.MILLISECONDS) }.getOrNull()
        }
        if (datos != null) runCatching { partidas.guardar(romset, datos, null, miniatura) }
    }

    /** Controles táctiles: Moneda también pulsa Start si está activado en «Controles». */
    private fun botonTactil(k: Int, pulsado: Boolean) {
        val v = vista ?: return
        // En el menú de servicio, los botones táctiles 3 y 4 también son TEST y SERVICE
        if (modoServicio && teclaDeServicio(k, if (pulsado) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, 0)) return
        v.sendKeyEvent(if (pulsado) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, k, 0)
        if (k == KeyEvent.KEYCODE_BUTTON_SELECT && !pulsado && mapa.monedaConStart) {
            principal.postDelayed({
                vista?.sendKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START, 0)
                principal.postDelayed({ vista?.sendKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_START, 0) }, 120)
            }, 150)
        }
    }

    // ── Entrada del mando físico ─────────────────────────────────────────────

    // Aviso falso de lint: super.dispatchKeyEvent es el método público de Activity
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val v = vista
        if (error != null || v == null) {
            if (event.keyCode in Mando.TECLAS_MENU || event.keyCode == KeyEvent.KEYCODE_BUTTON_B) {
                if (event.action == KeyEvent.ACTION_UP) finish()
                return true
            }
            return traducirParaMenus(event)
        }
        if (menuAbierto) {
            val esPausa = mapa.accion(event.keyCode) == com.diasdefuria.datos.AccionMando.PAUSA
            if (event.keyCode in Mando.TECLAS_MENU || event.keyCode == KeyEvent.KEYCODE_BUTTON_B || esPausa) {
                if (event.action == KeyEvent.ACTION_UP) {
                    // Start cierra la pausa desde cualquier panel; Atrás/B vuelve un paso
                    if (panel != PanelPausa.MENU && !esPausa) panel = PanelPausa.MENU else reanudar()
                }
                return true
            }
            return traducirParaMenus(event)
        }
        if (modoServicio && teclaDeServicio(event.keyCode, event.action, event.repeatCount)) return true
        if (traductor.tecla(event)) return true
        return super.dispatchKeyEvent(event)
    }

    /** En los menús, A confirma (como pulsar el centro de la cruceta). */
    @SuppressLint("RestrictedApi")
    private fun traducirParaMenus(event: KeyEvent): Boolean {
        val equivalente = Mando.teclaDeMenu(event.keyCode) ?: return super.dispatchKeyEvent(event)
        return super.dispatchKeyEvent(
            KeyEvent(event.downTime, event.eventTime, event.action, equivalente, event.repeatCount, event.metaState)
        )
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val v = vista
        if (v == null || menuAbierto || error != null) return super.dispatchGenericMotionEvent(event)
        if (event.isFromSource(InputDevice.SOURCE_JOYSTICK) && event.action == MotionEvent.ACTION_MOVE) {
            var x = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            var y = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            if (x == 0f && y == 0f) {
                // Sin cruceta: el stick izquierdo también mueve al personaje
                x = umbral(event.getAxisValue(MotionEvent.AXIS_X))
                y = umbral(event.getAxisValue(MotionEvent.AXIS_Y))
            }
            v.sendMotionEvent(GLRetroView.MOTION_SOURCE_DPAD, x, y, 0)
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun umbral(valor: Float) = if (abs(valor) < 0.5f) 0f else if (valor > 0) 1f else -1f

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        pantallaCompleta()
        actualizarTactiles()
        traductor.recargar()
        // Al volver a la app, LibretroDroid reanuda solo; si el menú estaba abierto, se vuelve a parar
        if (menuAbierto) window.decorView.post { if (menuAbierto) pausarEmulacion() }
        else {
            logros?.jugando = true; empezarCrono(); window.decorView.postDelayed({ audioRed?.reenganchar() }, 500)
            anfitrion?.let { a -> Thread { a.avisarPausa(false) }.start() }
        }
    }

    override fun onPause() {
        pararCrono()
        logros?.jugando = false
        if (isFinishing) { detenerLogros(); cerrarSala() }
        else anfitrion?.let { a -> Thread { a.avisarPausa(true) }.start() }
        traductor.soltarTodo()
        super.onPause()
    }

    /** Desde la pausa: enseña u oculta los controles táctiles aunque haya mando conectado. */
    private fun alternarTactiles() {
        val p = getSharedPreferences("ajustes", Context.MODE_PRIVATE).edit()
        if (mostrarTactiles) p.putBoolean(ClaveAjuste.CONTROLES_TACTILES, false)
        else p.putBoolean(ClaveAjuste.CONTROLES_TACTILES, true).putBoolean(ClaveAjuste.OCULTAR_CON_MANDO, false)
        p.commit()
        actualizarTactiles()
    }

    private fun actualizarTactiles() {
        val activos = ajustes.booleano(ClaveAjuste.CONTROLES_TACTILES, true)
        val ocultarConMando = ajustes.booleano(ClaveAjuste.OCULTAR_CON_MANDO, true)
        mostrarTactiles = activos && !(ocultarConMando && Mando.hayMandoFisico())
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) pantallaCompleta()
    }

    private fun pantallaCompleta() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
