package com.diasdefuria.datos

import android.content.Context
import com.swordfish.libretrodroid.Variable

/**
 * «Reglas de la recreativa»: los interruptores DIP de la placa, que FBNeo expone
 * como opciones «fbneo-dipswitch-<romset>-<nombre>». Los nombres exactos salen del
 * código de FBNeo (lista DIP de cada juego).
 */
data class Regla(
    val nombre: String,
    val titulo: String,
    /** Valor exacto de FBNeo → texto en español. */
    val valores: List<Pair<String, String>>,
    val porDefecto: String,
) {
    /** Como la forma FBNeo (libretro.cpp): espacios, «=», «:» y «#» pasan a «_». */
    fun clave(romset: RomsetConocido): String {
        val limpio = nombre.replace(' ', '_').replace('=', '_').replace(':', '_').replace('#', '_')
        return "fbneo-dipswitch-${romset.nombre}-$limpio"
    }
}

object Reglas {
    // Ajustes rápidos de dificultad y vidas (interruptores DIP) por id de juego. Sacados de las
    // listas DIP de FBNeo con herramientas/reglas.py; todas las versiones de cada juego usan la
    // misma lista. Los juegos que no aparecen (CPS-2, CPS-1 QSound, Konami GX con EEPROM,
    // Sega System 32, Neo Geo) guardan esto en el menú de servicio de la placa, no en DIP.
    private val porJuego: Map<String, List<Regla>> by lazy {
        mapOf(
            "altbeast" to listOf(
                dificultad("Difficulty", NIVELES_4),
                Regla("Lives", "Vidas", listOf("2" to "2 vidas", "3" to "3 vidas", "4" to "4 vidas", "240" to "Infinitas (240)"), "3"),
                Regla("Energy Meter", "Barras de energía", listOf("2" to "2 barras", "3" to "3 barras", "4" to "4 barras",
                    "5" to "5 barras"), "3"),
            ),
            "baddudes" to listOf(
                dificultad("Difficulty", NIVELES_4),
                Regla("Lives", "Vidas", listOf("1" to "1 vida", "3" to "3 vidas", "5" to "5 vidas", "Infinite" to "Infinitas"), "3"),
            ),
            "captaven" to listOf(dificultad("Difficulty", NIVELES_4), vidas("Lives", "1", "2", "3", "4", defecto = "3")),
            "captcomm" to listOf(dificultad("Difficulty 2", NIVELES_4), vidas("Lives", "1", "2", "3", "4", defecto = "2")),
            // Gang Wars: la versión de EE. UU. admite también 2 y 5 vidas; 3 y 4 valen en todas
            "gangwars" to listOf(
                dificultad("Difficulty", NIVELES_4),
                vidas("Lives", "3", "4", defecto = "3"),
                Regla("Timer Speed", "Reloj", listOf("Slow" to "Lento", "Normal" to "Normal"), "Slow"),
            ),
            "ffight" to listOf(dificultad("Difficulty 2", NIVELES_4), vidas("Lives", "1", "2", "3", "4", defecto = "2")),
            "knights" to listOf(
                Regla("Enemys Attack Frequency", "Frecuencia de ataque enemiga", NIVELES_8, "4 (Normal)"),
                Regla("Enemys Attack Power", "Fuerza de ataque enemiga", NIVELES_8, "4 (Normal)"),
                vidas("Lives", "1", "2", "3", "4", defecto = "2"),
            ),
            "ddragon" to listOf(
                Regla("Difficulty", "Dificultad", listOf("Easy" to "Fácil", "Medium" to "Normal", "Hard" to "Difícil",
                    "Hardest" to "Muy difícil"), "Medium"),
                Regla("Lives", "Vidas", listOf("2" to "2 vidas", "3" to "3 vidas", "4" to "4 vidas",
                    "Infinite (Cheat)" to "Infinitas (truco)"), "2"),
            ),
            "ddragon2" to listOf(
                Regla("Difficulty", "Dificultad", listOf("Easy" to "Fácil", "Medium" to "Normal", "Hard" to "Difícil",
                    "Hardest" to "Muy difícil"), "Hard"),
                vidas("Lives", "1", "2", "3", "4", defecto = "2"),
            ),
            "goldnaxe" to listOf(
                Regla("Difficulty", "Dificultad", listOf("Special" to "Especial", "Easiest" to "La más fácil",
                    "Easier" to "Muy fácil", "Easy" to "Fácil", "Normal" to "Normal", "Hard" to "Difícil",
                    "Harder" to "Muy difícil", "Hardest" to "La más difícil"), "Normal"),
                vidas("Lives", "1", "2", "3", "5", defecto = "2"),
                Regla("Energy Meter", "Barras de energía", listOf("2" to "2 barras", "3" to "3 barras", "4" to "4 barras",
                    "5" to "5 barras"), "3"),
            ),
            "hook" to listOf(
                Regla("Difficulty", "Dificultad", listOf("Very Easy" to "Muy fácil", "Easy" to "Fácil", "Normal" to "Normal",
                    "Hard" to "Difícil"), "Normal"),
                vidas("Lives", "1", "2", "3", "4", defecto = "2"),
            ),
            "mwalk" to listOf(
                dificultad("Difficulty", NIVELES_4),
                vidas("Lives", "2", "3", defecto = "2"),
                Regla("Player Vitality", "Vitalidad del jugador", listOf("Low" to "Baja", "High" to "Alta"), "Low"),
            ),
        )
    }

    private val NIVELES_4 = listOf("Easy" to "Fácil", "Normal" to "Normal", "Hard" to "Difícil", "Hardest" to "Muy difícil")
    private val NIVELES_8 = listOf("1 (Easiest)" to "1 · La más baja", "2" to "2", "3" to "3", "4 (Normal)" to "4 · Normal",
        "5" to "5", "6" to "6", "7" to "7", "8 (Hardest)" to "8 · La más alta")

    private fun dificultad(nombre: String, niveles: List<Pair<String, String>>) =
        Regla(nombre, "Dificultad", niveles, "Normal")

    private fun vidas(nombre: String, vararg n: String, defecto: String) =
        Regla(nombre, "Vidas", n.map { it to if (it == "1") "1 vida" else "$it vidas" }, defecto)

    fun de(juego: Juego): List<Regla> = porJuego[juego.id].orEmpty()

    fun variables(context: Context, romset: RomsetConocido): List<Variable> {
        val p = prefs(context)
        return de(romset.juego).mapNotNull { r ->
            val v = p.getString("${romset.juego.id}.${r.nombre}", null) ?: return@mapNotNull null
            if (r.valores.none { it.first == v }) null else Variable(r.clave(romset), v)
        }
    }

    fun valor(context: Context, juego: Juego, r: Regla): String =
        prefs(context).getString("${juego.id}.${r.nombre}", null) ?: r.porDefecto

    fun fijar(context: Context, juego: Juego, r: Regla, v: String) {
        prefs(context).edit().putString("${juego.id}.${r.nombre}", v).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("reglas", Context.MODE_PRIVATE)
}
