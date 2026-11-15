package com.diasdefuria.datos

import android.content.Context
import android.view.KeyEvent
import androidx.annotation.StringRes
import com.diasdefuria.R

/**
 * Acciones que se pueden asignar a los botones del mando físico.
 *
 * [retro] son los botones RetroPad que se envían a FBNeo (vacío = acción de la app).
 * FBNeo asigna en casi todas las placas: B = Botón 1, A = Botón 2, Y = Botón 3, X = Botón 4.
 */
enum class AccionMando(
    @StringRes val nombre: Int,
    @StringRes val descripcion: Int,
    val retro: IntArray,
    val porDefecto: Int,
    /** Solo aparece en los juegos que tienen al menos estos botones. */
    val minimoBotones: Int = 0,
) {
    ATACAR(R.string.accion_atacar, R.string.accion_atacar_desc, intArrayOf(KeyEvent.KEYCODE_BUTTON_B), KeyEvent.KEYCODE_BUTTON_A),
    SALTAR(R.string.accion_saltar, R.string.accion_saltar_desc, intArrayOf(KeyEvent.KEYCODE_BUTTON_A), KeyEvent.KEYCODE_BUTTON_B),
    ELEGIR(R.string.accion_elegir, R.string.accion_elegir_desc, intArrayOf(KeyEvent.KEYCODE_BUTTON_Y), KeyEvent.KEYCODE_BUTTON_X, minimoBotones = 3),
    USAR(R.string.accion_usar, R.string.accion_usar_desc, intArrayOf(KeyEvent.KEYCODE_BUTTON_X), KeyEvent.KEYCODE_BUTTON_Y, minimoBotones = 4),
    /** Botón 1 + Botón 2 a la vez: el ataque especial de casi todos los beat 'em up. */
    ESPECIAL(R.string.accion_especial, R.string.accion_especial_desc,
        intArrayOf(KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_A), KeyEvent.KEYCODE_BUTTON_L1),
    MONEDA(R.string.accion_moneda, R.string.accion_moneda_desc, intArrayOf(KeyEvent.KEYCODE_BUTTON_SELECT), KeyEvent.KEYCODE_BUTTON_SELECT),
    START(R.string.accion_start, R.string.accion_start_desc, intArrayOf(KeyEvent.KEYCODE_BUTTON_START), KeyEvent.KEYCODE_BUTTON_START),
    PAUSA(R.string.accion_pausa, R.string.accion_pausa_desc, intArrayOf(), KeyEvent.KEYCODE_BUTTON_R1);

    val clave: String get() = "boton_" + name.lowercase()
    val esDelJuego: Boolean get() = retro.isNotEmpty()

    companion object {
        fun para(juego: Juego?): List<AccionMando> =
            entries.filter { (juego?.botones ?: 4) >= it.minimoBotones }
    }
}

/** Botones del mando que se pueden asignar (la cruceta y los sticks mueven siempre al personaje). */
object BotonesFisicos {
    val ASIGNABLES = setOf(
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_C, KeyEvent.KEYCODE_BUTTON_Z,
        KeyEvent.KEYCODE_BUTTON_1, KeyEvent.KEYCODE_BUTTON_2, KeyEvent.KEYCODE_BUTTON_3, KeyEvent.KEYCODE_BUTTON_4,
        KeyEvent.KEYCODE_BUTTON_5, KeyEvent.KEYCODE_BUTTON_6, KeyEvent.KEYCODE_BUTTON_7, KeyEvent.KEYCODE_BUTTON_8,
        // Teclado físico (Bluetooth / USB)
        KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_V,
        KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_F,
        KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_R,
        KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_TAB,
    )

    fun nombre(keyCode: Int): String = when (keyCode) {
        0 -> "—"
        KeyEvent.KEYCODE_BUTTON_A -> "A"
        KeyEvent.KEYCODE_BUTTON_B -> "B"
        KeyEvent.KEYCODE_BUTTON_X -> "X"
        KeyEvent.KEYCODE_BUTTON_Y -> "Y"
        KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
        KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
        KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
        KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
        KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
        KeyEvent.KEYCODE_BUTTON_START -> "Start"
        KeyEvent.KEYCODE_SPACE -> "Espacio"
        KeyEvent.KEYCODE_ENTER -> "Intro"
        KeyEvent.KEYCODE_SHIFT_LEFT -> "Mayús"
        KeyEvent.KEYCODE_CTRL_LEFT -> "Ctrl"
        KeyEvent.KEYCODE_TAB -> "Tab"
        else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").removePrefix("BUTTON_")
            .lowercase().replaceFirstChar { it.uppercase() }
    }
}

/** Asignación actual de botones del mando físico, guardada en los ajustes. */
class MapaBotones(context: Context) {
    private val p = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    fun tecla(a: AccionMando): Int = p.getInt(a.clave, a.porDefecto)

    /** Acción asignada a una tecla, o null si no tiene. */
    fun accion(keyCode: Int): AccionMando? = AccionMando.entries.firstOrNull { tecla(it) == keyCode && keyCode != 0 }

    /** Asigna [keyCode] a [a]; si otra acción la tenía, se intercambian. */
    fun asignar(a: AccionMando, keyCode: Int) {
        val anterior = tecla(a)
        val editor = p.edit()
        AccionMando.entries.filter { it != a && tecla(it) == keyCode }.forEach { editor.putInt(it.clave, anterior) }
        editor.putInt(a.clave, keyCode).apply()
    }

    fun quitar(a: AccionMando) = p.edit().putInt(a.clave, 0).apply()

    fun restablecer() {
        val e = p.edit()
        AccionMando.entries.forEach { e.remove(it.clave) }
        e.remove(ClaveAjuste.MONEDA_CON_START).apply()
    }

    /** Moneda pulsa también Start: con un solo botón se mete crédito y se entra a jugar. */
    val monedaConStart: Boolean get() = p.getBoolean(ClaveAjuste.MONEDA_CON_START, false)
}
