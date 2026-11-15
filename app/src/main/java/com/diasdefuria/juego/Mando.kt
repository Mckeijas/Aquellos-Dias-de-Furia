package com.diasdefuria.juego

import android.view.InputDevice
import android.view.KeyEvent

/**
 * Traducción del mando físico (Odin 2 Portal, mandos Bluetooth/USB) a RetroPad.
 *
 * Android y RetroPad tienen A/B y X/Y en posiciones cruzadas; se intercambian para
 * que la disposición física coincida con la recreativa:
 *   abajo  (A Android) → Botón 1 · Atacar
 *   derecha(B Android) → Botón 2 · Saltar
 *   izq.   (X Android) → Botón 3 · Seleccionar objeto
 *   arriba (Y Android) → Botón 4 · Usar objeto
 *   Select → Moneda · Start → Start
 */
object Mando {
    val TECLAS_JUEGO = setOf(
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
        KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_BUTTON_START,
    )

    /** Teclas que abren el menú de pausa durante la partida. */
    val TECLAS_MENU = setOf(KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_ESCAPE)

    fun aRetroPad(keyCode: Int): Int = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_BUTTON_B
        KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BUTTON_A
        KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_BUTTON_Y
        KeyEvent.KEYCODE_BUTTON_Y -> KeyEvent.KEYCODE_BUTTON_X
        else -> keyCode
    }

    /** Hay algún mando físico conectado (incluye los integrados, como el de la Odin). */
    fun hayMandoFisico(): Boolean = InputDevice.getDeviceIds().any { id ->
        val d = InputDevice.getDevice(id) ?: return@any false
        !d.isVirtual && (d.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            d.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
    }

    /**
     * En los menús, A confirma y B vuelve atrás, como en cualquier consola.
     * Devuelve la tecla equivalente o null si no hay que traducir.
     */
    fun teclaDeMenu(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_DPAD_CENTER
        KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BACK
        else -> null
    }
}

/**
 * Traduce el mando físico durante la partida según la asignación de «Controles».
 * La cruceta (teclas DPAD) va siempre al movimiento; Atrás/Home abren siempre la pausa.
 *
 * [enviar] recibe botones RetroPad; [programar] ejecuta algo tras un retraso (hilo principal).
 */
class TraductorMando(
    private val mapa: com.diasdefuria.datos.MapaBotones,
    private val enviar: (retro: Int, pulsado: Boolean) -> Unit,
    private val cruceta: (keyCode: Int, pulsado: Boolean) -> Unit,
    private val abrirPausa: () -> Unit,
    private val programar: (Long, () -> Unit) -> Unit,
) {
    /** Cuántas teclas físicas mantienen pulsado cada botón RetroPad (varias acciones pueden compartirlo). */
    private val pulsados = HashMap<Int, Int>()
    private val acciones = HashMap<Int, com.diasdefuria.datos.AccionMando?>()

    /** Se llama al volver de «Controles» por si cambió la asignación. */
    fun recargar() = acciones.clear()

    private fun accion(k: Int) = acciones.getOrPut(k) { mapa.accion(k) }

    /** Devuelve true si la tecla pertenece al juego (y se ha consumido). */
    fun tecla(event: KeyEvent): Boolean {
        val k = event.keyCode
        val abajo = event.action == KeyEvent.ACTION_DOWN
        if (k in Mando.TECLAS_MENU) {
            if (!abajo) abrirPausa()
            return true
        }
        if (k == KeyEvent.KEYCODE_DPAD_UP || k == KeyEvent.KEYCODE_DPAD_DOWN ||
            k == KeyEvent.KEYCODE_DPAD_LEFT || k == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (event.repeatCount == 0) cruceta(k, abajo)
            return true
        }
        val a = accion(k) ?: return k in com.diasdefuria.datos.BotonesFisicos.ASIGNABLES
        if (event.repeatCount > 0) return true
        when {
            a == com.diasdefuria.datos.AccionMando.PAUSA -> if (!abajo) abrirPausa()
            a == com.diasdefuria.datos.AccionMando.MONEDA && mapa.monedaConStart -> {
                boton(KeyEvent.KEYCODE_BUTTON_SELECT, abajo)
                // Al soltar la moneda se pulsa Start un instante: crédito y a jugar
                if (!abajo) programar(150) {
                    boton(KeyEvent.KEYCODE_BUTTON_START, true)
                    programar(120) { boton(KeyEvent.KEYCODE_BUTTON_START, false) }
                }
            }
            else -> a.retro.forEach { boton(it, abajo) }
        }
        return true
    }

    private fun boton(retro: Int, abajo: Boolean) {
        val n = pulsados[retro] ?: 0
        if (abajo) {
            pulsados[retro] = n + 1
            if (n == 0) enviar(retro, true)
        } else if (n > 0) {
            pulsados[retro] = n - 1
            if (n == 1) enviar(retro, false)
        }
    }

    /** Suelta todo (al abrir la pausa, para que nada se quede pulsado). */
    fun soltarTodo() {
        pulsados.filterValues { it > 0 }.keys.forEach { enviar(it, false) }
        pulsados.clear()
    }
}

/** Mientras «Controles» espera a que se pulse un botón, recibe las teclas antes que nadie. */
object CapturaMando {
    var oyente: ((KeyEvent) -> Boolean)? = null
}
