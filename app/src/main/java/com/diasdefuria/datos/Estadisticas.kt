package com.diasdefuria.datos

import android.content.Context

/** Lo que se lleva jugado a un juego (todas sus versiones juntas). */
data class EstadisticasJuego(
    val partidas: Int,
    /** Tiempo jugando de verdad, en milisegundos (sin contar la pausa ni la app en segundo plano). */
    val tiempoMs: Long,
    /** Cuándo se jugó por última vez (0 = nunca). */
    val ultima: Long,
)

/**
 * Estadísticas por juego, guardadas en la memoria de la app: cuántas veces se ha arrancado,
 * cuánto tiempo se ha jugado y la última vez. Las anota JuegoActivity y se ven en la ficha.
 */
object Estadisticas {
    private fun prefs(context: Context) = context.getSharedPreferences("estadisticas", Context.MODE_PRIVATE)

    fun de(context: Context, juego: Juego): EstadisticasJuego {
        val p = prefs(context)
        return EstadisticasJuego(
            partidas = p.getInt("${juego.id}.partidas", 0),
            tiempoMs = p.getLong("${juego.id}.tiempo", 0L),
            ultima = p.getLong("${juego.id}.ultima", 0L),
        )
    }

    /** Una partida más (al arrancar el juego). */
    fun partidaNueva(context: Context, juego: Juego) {
        val p = prefs(context)
        p.edit()
            .putInt("${juego.id}.partidas", p.getInt("${juego.id}.partidas", 0) + 1)
            .putLong("${juego.id}.ultima", System.currentTimeMillis())
            .apply()
    }

    fun sumarTiempo(context: Context, juego: Juego, ms: Long) {
        if (ms <= 0) return
        val p = prefs(context)
        p.edit().putLong("${juego.id}.tiempo", p.getLong("${juego.id}.tiempo", 0L) + ms).apply()
    }

    /** «3 h 25 min», «12 min», «menos de 1 min». */
    fun textoTiempo(ms: Long): String {
        val min = ms / 60_000
        return when {
            min < 1 -> "< 1 min"
            min < 60 -> "$min min"
            else -> "${min / 60} h ${min % 60} min"
        }
    }
}
