package com.diasdefuria.datos

import android.content.Context
import androidx.annotation.StringRes

/**
 * Los juegos que aparecen en la ruleta de selección, en este orden (la ruleta puede
 * ordenarlos también por nombre o por año). Las versiones concretas (romsets) que se
 * aceptan para cada uno están en [CatalogoRomsets].
 *
 * Para añadir un juego basta una línea aquí y sus romsets en el catálogo: el nombre va
 * como texto (no hace falta strings.xml) y la imagen es opcional
 * (`assets/previews/<id>.jpg|png|webp`; si falta se dibuja un recuadro con el nombre).
 */
enum class Juego(
    val id: String,
    /** Título tal como aparece en la recreativa (no se traduce). */
    val nombre: String,
    val fabricante: String,
    val anio: Int,
    /** Placa o sistema: «CPS-1», «CPS-2», «Neo Geo MVS»… */
    val placa: String,
    /** Género en español: «Beat 'em up», «Lucha», «Shoot 'em up»… */
    val genero: String,
    /** Jugadores a la vez en la recreativa. */
    val jugadores: Int,
    /** Existe versión Hispanic (en español): se prefiere y el padre queda como alternativa. */
    val tieneHispanic: Boolean = false,
    /** Botones de la recreativa (sin contar Start ni Moneda). */
    val botones: Int = 4,
    /** Aviso opcional en la sección Multijugador cuando este juego se puede jugar. */
    @StringRes val avisoMultijugador: Int? = null,
) {
    // id = romset padre de FBNeo (el mismo que en herramientas/catalogo.py y en assets/previews/<id>.jpg)
    ALIEN_VS_PREDATOR("avsp", "Alien vs. Predator", "Capcom", 1994, "CPS-2", "Beat 'em up", 3, tieneHispanic = true, botones = 3),
    ALTERED_BEAST("altbeast", "Altered Beast", "Sega", 1988, "System 16B", "Beat 'em up", 2, botones = 3),
    ARMORED_WARRIORS("armwar", "Armored Warriors", "Capcom", 1994, "CPS-2", "Beat 'em up", 3, botones = 3),
    ASTERIX("asterix", "Asterix", "Konami", 1992, "Konami GX068", "Beat 'em up", 2, botones = 2),
    BAD_DUDES("baddudes", "Bad Dudes vs. Dragonninja", "Data East", 1988, "Data East DEC0", "Beat 'em up", 2, botones = 2),
    BURNING_FIGHT("burningf", "Burning Fight", "SNK", 1991, "Neo Geo MVS", "Beat 'em up", 2, botones = 3),
    CADILLACS_AND_DINOSAURS("dino", "Cadillacs and Dinosaurs", "Capcom", 1993, "CPS-1 QSound", "Beat 'em up", 3, botones = 2),
    CAPTAIN_AMERICA("captaven", "Captain America and the Avengers", "Data East", 1991, "Deco 32", "Beat 'em up", 4, botones = 2),
    CAPTAIN_COMMANDO("captcomm", "Captain Commando", "Capcom", 1991, "CPS-1", "Beat 'em up", 4, botones = 2),
    DOUBLE_DRAGON("ddragon", "Double Dragon", "Technos Japan", 1987, "Technos", "Beat 'em up", 2, botones = 3),
    DOUBLE_DRAGON_2("ddragon2", "Double Dragon II: The Revenge", "Technos Japan", 1988, "Technos", "Beat 'em up", 2, botones = 3),
    FINAL_FIGHT("ffight", "Final Fight", "Capcom", 1989, "CPS-1", "Beat 'em up", 2, botones = 2),
    GANG_WARS("gangwars", "Gang Wars", "Alpha Denshi / SNK", 1989, "Alpha 68K", "Beat 'em up", 2, botones = 3),
    GOLDEN_AXE("goldnaxe", "Golden Axe", "Sega", 1989, "System 16B", "Beat 'em up", 2, botones = 3),
    GOLDEN_AXE_2("ga2", "Golden Axe: The Revenge of Death Adder", "Sega", 1992, "System 32", "Beat 'em up", 4, botones = 3),
    HOOK("hook", "Hook", "Irem", 1992, "Irem M92", "Beat 'em up", 4, botones = 2),
    KNIGHTS_OF_THE_ROUND("knights", "Knights of the Round", "Capcom", 1991, "CPS-1", "Beat 'em up", 3, botones = 2),
    METAMORPHIC_FORCE("metamrph", "Metamorphic Force", "Konami", 1993, "Konami GX224", "Beat 'em up", 4, botones = 3),
    MOONWALKER("mwalk", "Michael Jackson's Moonwalker", "Sega", 1990, "System 18", "Beat 'em up", 3, botones = 2),
    MUTATION_NATION("mutnat", "Mutation Nation", "SNK", 1992, "Neo Geo MVS", "Beat 'em up", 2, botones = 4),
    NIGHT_SLASHERS("nslasher", "Night Slashers", "Data East", 1994, "Deco 32", "Beat 'em up", 3, botones = 3),
    SENGOKU_2("sengoku2", "Sengoku 2", "SNK", 1993, "Neo Geo MVS", "Beat 'em up", 2, botones = 3),
    TMNT_TURTLES_IN_TIME("tmnt2", "Teenage Mutant Ninja Turtles: Turtles in Time", "Konami", 1991, "Konami GX063", "Beat 'em up", 4, botones = 2),
    THE_PUNISHER("punisher", "The Punisher", "Capcom", 1993, "CPS-1 QSound", "Beat 'em up", 2, tieneHispanic = true, botones = 2),
    THE_SIMPSONS("simpsons", "The Simpsons", "Konami", 1991, "Konami GX072", "Beat 'em up", 4, botones = 2),
    VENDETTA("vendetta", "Vendetta", "Konami", 1991, "Konami GX081", "Beat 'em up", 4, botones = 2),
    VIOLENT_STORM("viostorm", "Violent Storm", "Konami", 1993, "Konami GX224", "Beat 'em up", 3, botones = 3),
    WARRIORS_OF_FATE("wof", "Warriors of Fate", "Capcom", 1992, "CPS-1 QSound", "Beat 'em up", 3, botones = 2),
    X_MEN("xmen", "X-Men", "Konami", 1992, "Konami GX065", "Beat 'em up", 4, botones = 3);

    /** «CPS-2 · 1993», para las líneas cortas. */
    val ficha: String get() = "$placa · $anio"

    companion object {
        fun porId(id: String?): Juego? = entries.firstOrNull { it.id == id }

        /** Juego que se usa cuando hace falta uno «por defecto» (el primero de la lista). */
        val primero: Juego get() = entries.first()
    }
}

/**
 * Imagen de la ruleta: `assets/previews/<id>.(jpg|png|webp)` (o, si falta, la primera de su
 * galería). Devuelve la URI para Coil o null si no hay ninguna imagen para ese juego. La lista de la carpeta se lee una sola vez.
 */
object Previews {
    private var archivos: Set<String>? = null

    fun uri(context: Context, juego: Juego): String? {
        val lista = archivos ?: (runCatching { context.assets.list("previews")?.toSet() }.getOrNull() ?: emptySet())
            .also { archivos = it }
        val archivo = listOf("jpg", "png", "webp", "jpeg").map { "${juego.id}.$it" }.firstOrNull { it in lista }
        if (archivo != null) return "file:///android_asset/previews/$archivo"
        // Sin preview propia: la primera imagen de su galería de Extras, si tiene
        return cargarArte(context, juego).firstOrNull()?.uri
    }
}

/** Un archivo dentro de un romset, tal como lo espera FBNeo. */
data class ArchivoRom(
    val nombre: String,
    val tamano: Long,
    val crc: Long,
    /** Clave de descifrado de la CPS-2 (archivo .key de 20 bytes). */
    val clave: Boolean,
    /** Programa del 68000: identifica de forma única cada versión. */
    val programa: Boolean,
    /** Archivos que FBNeo no necesita para arrancar (chips PLD de la placa). */
    val opcional: Boolean = false,
)

/** Una versión del juego que FBNeo reconoce, con su lista de archivos. */
data class RomsetConocido(
    val nombre: String,
    val padre: String?,
    val juego: Juego,
    val region: String,
    val hispanic: Boolean,
    /** BIOS de placa que necesita además de su ZIP (p. ej. «neogeo» → neogeo.zip). */
    val bios: String? = null,
    val archivos: List<ArchivoRom>,
) {
    val archivoZip get() = "$nombre.zip"

    /** Tamaño total de la versión (sin los chips opcionales), para la ficha. */
    val tamanoTotal: Long get() = archivos.filterNot { it.opcional }.sumOf { it.tamano }
}
