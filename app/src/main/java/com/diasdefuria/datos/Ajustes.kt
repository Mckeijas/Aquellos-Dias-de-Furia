package com.diasdefuria.datos

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Claves de ajustes. Las usará también la pantalla de emulación. */
object ClaveAjuste {
    const val FILTRO = "filtro"
    const val ESCALADO_ENTERO = "escalado_entero"
    const val ASPECTO = "aspecto"
    const val BEZEL = "bezel"
    const val CONTROLES_TACTILES = "controles_tactiles"
    const val OPACIDAD_TACTIL = "opacidad_tactil"
    const val VIBRACION = "vibracion"
    const val OCULTAR_CON_MANDO = "ocultar_con_mando"
    const val AUTOGUARDADO = "autoguardado"
    const val MUSICA = "musica_menu"
    const val VOLUMEN_MUSICA = "volumen_musica"
    const val EFECTOS = "efectos"
    const val MONEDA_CON_START = "moneda_con_start"
    const val ESCALA_TACTIL = "escala_tactil"
    const val ESTILO_TACTIL = "estilo_tactil"
}

/** Filtros de imagen disponibles en LibretroDroid. */
enum class Filtro { NINGUNO, BILINEAL, MEJORADO, CRT, LCD }

/**
 * Relación de aspecto de la imagen del juego. La CPS-2 genera 384×224 píxeles:
 * en la recreativa se veían en un monitor 4:3 (píxeles algo anchos); con píxeles
 * cuadrados la imagen queda en 12:7, prácticamente 16:9. Ninguna opción deforma:
 * lo que sobra a los lados se rellena con el bezel elegido.
 */
enum class Aspecto(val proporcion: Float) {
    ARCADE_4_3(4f / 3f),
    PANORAMICO(384f / 224f),
}

/**
 * Bezel: «negro», «juego» (el del juego que se está jugando, assets/bezels_juegos/<id>.jpg,
 * hecho con su propia ilustración por herramientas/bezels/bezels_juegos.py) o
 * «asset:archivo» de assets/bezels/ (se añaden copiando imágenes ahí).
 */
object Bezels {
    const val NEGRO = "negro"
    const val DEL_JUEGO = "juego"
    const val POR_DEFECTO = "asset:01_arte_difuminado.jpg"
    const val PREFIJO_ASSET = "asset:"
    /** Una imagen de la galería de Extras: «arte:ilustraciones/37_dragon_rojo.jpg». */
    const val PREFIJO_ARTE = "arte:"
    private val EXTENSIONES = setOf("jpg", "jpeg", "png", "webp")

    fun disponibles(context: Context): List<String> =
        listOf(NEGRO, DEL_JUEGO) + (context.assets.list("bezels") ?: emptyArray())
            .filter { it.substringAfterLast('.').lowercase() in EXTENSIONES }
            .sorted()
            .map { PREFIJO_ASSET + it }

    /** [juego]: el que se está jugando (o el de muestra en Ajustes); solo cuenta con «Bezel del juego». */
    fun uriAsset(bezel: String, juego: Juego? = null): String? = when {
        bezel == DEL_JUEGO -> juego?.let { "file:///android_asset/bezels_juegos/${it.id}.jpg" }
            ?: uriAsset(POR_DEFECTO)
        bezel.startsWith(PREFIJO_ASSET) -> "file:///android_asset/bezels/" + bezel.removePrefix(PREFIJO_ASSET)
        bezel.startsWith(PREFIJO_ARTE) -> "file:///android_asset/arte/" + bezel.removePrefix(PREFIJO_ARTE)
        else -> null
    }

    fun esDeGaleria(bezel: String) = bezel.startsWith(PREFIJO_ARTE)

    /** Juego con el que se enseña «Bezel del juego» fuera de la partida: el último elegido en la ruleta. */
    fun juegoDeMuestra(context: Context): Juego =
        Juego.porId(context.getSharedPreferences("ruleta", Context.MODE_PRIVATE).getString("ultimo", null)) ?: Juego.primero
}

private const val ARCHIVO = "ajustes"

private fun prefs(context: Context) = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

/** Estado de Compose enlazado a un ajuste booleano: se guarda en cuanto cambia. */
@Composable
fun rememberAjuste(clave: String, porDefecto: Boolean): MutableState<Boolean> {
    val p = prefs(LocalContext.current)
    return remember(clave) {
        object : MutableState<Boolean> {
            private var interno by mutableStateOf(p.getBoolean(clave, porDefecto))
            override var value: Boolean
                get() = interno
                set(v) { interno = v; p.edit().putBoolean(clave, v).apply() }
            override operator fun component1() = value
            override operator fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}

@Composable
fun rememberAjuste(clave: String, porDefecto: Float): MutableState<Float> {
    val p = prefs(LocalContext.current)
    return remember(clave) {
        object : MutableState<Float> {
            private var interno by mutableStateOf(p.getFloat(clave, porDefecto))
            override var value: Float
                get() = interno
                set(v) { interno = v; p.edit().putFloat(clave, v).apply() }
            override operator fun component1() = value
            override operator fun component2(): (Float) -> Unit = { value = it }
        }
    }
}

@Composable
fun rememberAjusteFiltro(): MutableState<Filtro> {
    val p = prefs(LocalContext.current)
    return remember {
        object : MutableState<Filtro> {
            private var interno by mutableStateOf(
                runCatching { Filtro.valueOf(p.getString(ClaveAjuste.FILTRO, null) ?: "") }
                    .getOrDefault(Filtro.NINGUNO)
            )
            override var value: Filtro
                get() = interno
                set(v) { interno = v; p.edit().putString(ClaveAjuste.FILTRO, v.name).apply() }
            override operator fun component1() = value
            override operator fun component2(): (Filtro) -> Unit = { value = it }
        }
    }
}

@Composable
fun rememberAjusteAspecto(): MutableState<Aspecto> {
    val p = prefs(LocalContext.current)
    return remember {
        object : MutableState<Aspecto> {
            private var interno by mutableStateOf(
                runCatching { Aspecto.valueOf(p.getString(ClaveAjuste.ASPECTO, null) ?: "") }
                    .getOrDefault(Aspecto.ARCADE_4_3)
            )
            override var value: Aspecto
                get() = interno
                set(v) { interno = v; p.edit().putString(ClaveAjuste.ASPECTO, v.name).apply() }
            override operator fun component1() = value
            override operator fun component2(): (Aspecto) -> Unit = { value = it }
        }
    }
}

@Composable
fun rememberAjusteBezel(): MutableState<String> {
    val p = prefs(LocalContext.current)
    return remember {
        object : MutableState<String> {
            private var interno by mutableStateOf(p.getString(ClaveAjuste.BEZEL, null) ?: Bezels.POR_DEFECTO)
            override var value: String
                get() = interno
                set(v) { interno = v; p.edit().putString(ClaveAjuste.BEZEL, v).apply() }
            override operator fun component1() = value
            override operator fun component2(): (String) -> Unit = { value = it }
        }
    }
}

/** Lectura de ajustes fuera de Compose (pantalla de juego). */
class LectorAjustes(context: Context) {
    private val p = prefs(context)
    fun booleano(clave: String, porDefecto: Boolean) = p.getBoolean(clave, porDefecto)
    fun decimal(clave: String, porDefecto: Float) = p.getFloat(clave, porDefecto)
    val filtro: Filtro get() = runCatching { Filtro.valueOf(p.getString(ClaveAjuste.FILTRO, null) ?: "") }.getOrDefault(Filtro.NINGUNO)
    val aspecto: Aspecto get() = runCatching { Aspecto.valueOf(p.getString(ClaveAjuste.ASPECTO, null) ?: "") }.getOrDefault(Aspecto.ARCADE_4_3)
    val bezel: String get() = p.getString(ClaveAjuste.BEZEL, null) ?: Bezels.POR_DEFECTO
}
