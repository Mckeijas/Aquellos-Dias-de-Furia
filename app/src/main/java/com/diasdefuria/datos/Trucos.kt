package com.diasdefuria.datos

import android.content.Context
import com.swordfish.libretrodroid.Variable
import java.io.File

/**
 * Un truco del archivo .ini de FBNeo. [indice] es su posición en el archivo
 * (contando también las líneas de nota), que FBNeo usa en el nombre de la opción.
 */
data class Truco(
    val indice: Int,
    val nombre: String,
    val nombreEs: String,
    /** Jugador al que afecta (1-4) o null si es general. */
    val jugador: Int?,
    val opciones: List<String>,
    val porDefecto: Int,
) {
    val esInterruptor: Boolean get() = opciones.size == 2
    fun opcionEs(i: Int): String = Trucos.traducirOpcion(opciones.getOrElse(i) { "" })
}

/**
 * Trucos de cada juego (assets/trucos/<id>.ini, del repositorio FBNeo-cheats).
 *
 * FBNeo los convierte en opciones del núcleo: «fbneo-cheat-<índice>-<romset>-<nombre>»
 * con valores «<n> - <opción>». La app genera esas mismas claves para activarlos
 * al arrancar y también en mitad de la partida, desde el menú de pausa.
 */
object Trucos {

    fun cargar(context: Context, juego: Juego): List<Truco> {
        val texto = runCatching {
            context.assets.open("trucos/${juego.id}.ini").bufferedReader(Charsets.ISO_8859_1).readText()
        }.getOrNull() ?: return emptyList()
        return analizar(texto)
    }

    fun analizar(texto: String): List<Truco> {
        data class EnCurso(val indice: Int, val nombre: String, var defecto: Int = 0, val opciones: MutableMap<Int, String> = sortedMapOf())
        val todos = mutableListOf<EnCurso>()
        var actual: EnCurso? = null
        val reCheat = Regex("""^\s*cheat\s+"([^"]*)"""")
        val reDefault = Regex("""^\s*default\s+(\d+)""")
        val reOpcion = Regex("""^\s*(\d+)\s+"([^"]*)"""")
        texto.lineSequence().forEach { linea ->
            if (linea.trimStart().startsWith("//")) return@forEach
            reCheat.find(linea)?.let { m ->
                actual = EnCurso(todos.size, m.groupValues[1]).also { todos += it }
                return@forEach
            }
            val c = actual ?: return@forEach
            reDefault.find(linea)?.let { c.defecto = it.groupValues[1].toInt(); return@forEach }
            reOpcion.find(linea)?.let { c.opciones[it.groupValues[1].toInt()] = it.groupValues[2] }
        }
        // FBNeo solo tiene en cuenta opciones consecutivas desde la 0
        return todos.mapNotNull { c ->
            val opciones = generateSequence(0) { it + 1 }.map { c.opciones[it] }.takeWhile { !it.isNullOrEmpty() }
                .filterNotNull().toList()
            // «Select Region» se oculta: cambiaría el juego a otra región y perdería el español
            // Más de 127 opciones (los «Sound Test» de 256): FBNeo en libretro no los convierte en
            // opción del núcleo, así que no funcionarían; se ocultan
            if (opciones.isEmpty() || opciones.size >= 128 || c.nombre.isBlank() || c.nombre.startsWith(" ") ||
                c.nombre.contains("Region")) null
            else Truco(c.indice, c.nombre, traducirNombre(c.nombre), jugadorDe(c.nombre), opciones,
                c.defecto.coerceIn(0, opciones.lastIndex))
        }
    }

    fun clave(t: Truco, romset: RomsetConocido): String {
        val limpio = t.nombre.replace(' ', '_').replace('=', '_').replace(':', '_').replace('#', '_')
        return "fbneo-cheat-${t.indice}-${romset.nombre}-$limpio"
    }

    fun valor(t: Truco, opcion: Int): String = "$opcion - ${t.opciones[opcion]}"

    /** Variables del núcleo para los trucos elegidos (solo los que no están en su valor por defecto). */
    fun variables(context: Context, romset: RomsetConocido): List<Variable> {
        val elegidos = AjustesTrucos(context).elegidos(romset.juego)
        return cargar(context, romset.juego).mapNotNull { t ->
            val v = elegidos[t.indice] ?: return@mapNotNull null
            if (v == t.porDefecto || v !in t.opciones.indices) null else Variable(clave(t, romset), valor(t, v))
        }
    }

    /** Copia el .ini del juego donde FBNeo lo busca: <sistema>/fbneo/cheats/<romset>.ini */
    fun instalar(context: Context, romset: RomsetConocido, sistema: File) {
        val destino = File(sistema, "fbneo/cheats").apply { mkdirs() }
        runCatching {
            context.assets.open("trucos/${romset.juego.id}.ini").use { entrada ->
                File(destino, "${romset.nombre}.ini").outputStream().use { entrada.copyTo(it) }
            }
        }
    }

    private fun jugadorDe(nombre: String): Int? =
        Regex("""PL([1-4])\s*$""").find(nombre)?.groupValues?.get(1)?.toInt()

    // ── Traducción ───────────────────────────────────────────────────────────

    private val frases = listOf(
        // Beat 'em up en general (los más largos primero: se busca el primero que encaje)
        "Infinite Ammo on Pickup" to "Munición infinita al recogerla",
        "Infinite Mutant Power" to "Poder mutante infinito",
        "Infinite Health" to "Salud infinita",
        "Infinite Credits" to "Créditos infinitos",
        "Infinite Bullets" to "Balas infinitas",
        "Infinite Super" to "Súper infinito",
        "Infinite Special" to "Especial infinito",
        "Select Character" to "Elegir personaje",
        "Select Sub Weapon" to "Arma secundaria",
        "Select Main Weapon" to "Arma principal",
        "Select Weapon" to "Arma",
        "Action Speed Up" to "Acción más rápida",
        "Rapid Fire" to "Disparo rápido",
        "Endless Grab Attack" to "Agarre sin fin",
        "Easy Special Attack" to "Ataque especial fácil",
        "All Hits Combo" to "Todos los golpes encadenan",
        "Always have Horse with Invincibility" to "Siempre con montura e invencible",
        "Always have Horse" to "Siempre con montura",
        "Infinite Upgrade Points" to "Puntos de mejora infinitos",
        "Infinite Energy" to "Energía infinita",
        "Infinite Gold" to "Oro infinito",
        "Infinite Dagger" to "Dagas infinitas",
        "Infinite Hammer" to "Martillos infinitos",
        "Infinite Arrows" to "Flechas infinitas",
        "Infinite Oil" to "Aceite infinito",
        "Infinite Ring MM" to "Anillo de Proyectil mágico infinito",
        "Infinite Ring FB" to "Anillo de Bola de fuego infinito",
        "Infinite Ring LB" to "Anillo de Relámpago infinito",
        "Infinite Ring CL" to "Anillo de Cono de frío infinito",
        "Infinite Magic Missile" to "Proyectil mágico infinito",
        "Infinite Invisibility" to "Invisibilidad infinita",
        "Infinite Fireball" to "Bola de fuego infinita",
        "Infinite Hold Person" to "Inmovilizar persona infinito",
        "Infinite Cont. Light" to "Luz continua infinita",
        "Infinite Striking" to "Golpe mágico infinito",
        "Infinite Time" to "Tiempo infinito",
        "1 Hit to Kill Enemy" to "Enemigos caen de un golpe",
        "One Hit to Kill Goons" to "Matones caen de un golpe",
        "Kill Boss Faster Now!" to "El jefe cae antes",
        "Always Clear Time 0" to "Tiempo de fase siempre a 0",
        "Infinite Keys" to "Llaves infinitas",
        "Infinite Key" to "Llaves infinitas",
        "Invincibility" to "Invencible",
        "Maximum Stage Clear Bonus (Rest Time)" to "Bonus máximo al terminar la fase",
        "Select Starting Stage" to "Fase inicial",
        "Maximum SP" to "Puntos especiales al máximo",
        "Infinite Item" to "Objetos infinitos",
        "Infinite Magic" to "Magia infinita",
        "Infinite Wands" to "Varitas infinitas",
        "Select Right Hand Weapon" to "Arma de la mano derecha",
        "Select Left Hand Weapon" to "Arma de la mano izquierda",
        "Select Head Equipment" to "Equipo de cabeza",
        "Select Body Equipment" to "Armadura",
        "Select Leg Equipment" to "Equipo de piernas",
        "Select Arm Equipment" to "Equipo de brazos",
        "Select Shield Equipment" to "Escudo",
        "Select Hand Equipment" to "Guantes",
        "Select Accessory" to "Accesorio",
        "Select Artifact" to "Artefacto",
        "Infinite Final Strike!" to "Golpe final infinito",
        "Select Region" to "Región",
        "Kill Stage Guardian with 1 hit Now!" to "El jefe de la fase cae de un golpe",
        "Kill Last Guardian Now!" to "Derrotar ya al último jefe",
        "Infinite Lives" to "Vidas infinitas",
        "Always Maximum Attack Power" to "Ataque siempre al máximo",
        "Always Maximum Defence Power" to "Defensa siempre al máximo",
        "Always Maximum Character" to "Nivel de personaje al máximo",
        "Always Maximum Weapon" to "Arma siempre al máximo",
        "Always Maximum Protection" to "Protección siempre al máximo",
        "Always Maximum Experience Points" to "Experiencia siempre al máximo",
        "Sound Test" to "Prueba de sonido",
    )

    private val clases = mapOf(
        "'ALL'" to "(todos)", "'ELF'" to "(Elfa)", "'CLERIC'" to "(Clérigo)",
    )

    fun traducirNombre(nombre: String): String {
        var s = nombre.replace(Regex("""\s*PL[1-4]\s*$"""), "")
        frases.firstOrNull { s.startsWith(it.first) }?.let { (en, es) -> s = es + s.removePrefix(en) }
        clases.forEach { (en, es) -> s = s.replace(en, es) }
        return s.trim()
    }

    fun traducirOpcion(o: String): String = when (o) {
        "Disabled" -> "Desactivado"
        "Enabled" -> "Activado"
        "None" -> "Ninguno"
        "On" -> "Sí"
        "Off" -> "No"
        else -> o
    }
}

/** Trucos elegidos por el jugador, guardados por juego. */
class AjustesTrucos(context: Context) {
    private val p = context.getSharedPreferences("trucos", Context.MODE_PRIVATE)
    private fun prefijo(j: Juego) = "${j.id}."

    fun elegidos(juego: Juego): Map<Int, Int> =
        p.all.filterKeys { it.startsWith(prefijo(juego)) }
            .mapNotNull { (k, v) -> k.removePrefix(prefijo(juego)).toIntOrNull()?.let { it to (v as? Int ?: 0) } }
            .toMap()

    fun fijar(juego: Juego, indice: Int, opcion: Int) {
        p.edit().putInt(prefijo(juego) + indice, opcion).apply()
    }

    /** Hay algún truco distinto de su valor normal (los logros se pausan). */
    fun hayActivos(context: Context, juego: Juego): Boolean {
        val elegidos = elegidos(juego)
        if (elegidos.isEmpty()) return false
        return Trucos.cargar(context, juego).any { t -> elegidos[t.indice]?.let { it != t.porDefecto } == true }
    }

    fun quitarTodos(juego: Juego) {
        p.edit().apply { p.all.keys.filter { it.startsWith(prefijo(juego)) }.forEach { remove(it) } }.apply()
    }
}
