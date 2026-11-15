package com.diasdefuria.datos

import android.content.Context

/** Grupos de controles táctiles que se pueden mover en «Controles → Colocar controles táctiles». */
enum class GrupoTactil { CRUCETA, ACCIONES, CENTRO }

/**
 * Dónde está cada grupo de controles táctiles (centro, en fracción de la pantalla; null =
 * posición de fábrica) y su tamaño ([escala], 1 = normal).
 */
data class DisposicionTactil(
    val centros: Map<GrupoTactil, Pair<Float, Float>> = emptyMap(),
    val escala: Float = 1f,
) {
    fun con(grupo: GrupoTactil, fx: Float, fy: Float) = copy(centros = centros + (grupo to (fx to fy)))

    fun guardar(context: Context) {
        val e = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE).edit()
        GrupoTactil.entries.forEach { g ->
            val c = centros[g]
            if (c == null) { e.remove(clave(g, "x")); e.remove(clave(g, "y")) }
            else { e.putFloat(clave(g, "x"), c.first); e.putFloat(clave(g, "y"), c.second) }
        }
        e.putFloat(ClaveAjuste.ESCALA_TACTIL, escala).apply()
    }

    companion object {
        private fun clave(g: GrupoTactil, eje: String) = "tactil_${g.name.lowercase()}_$eje"

        fun cargar(context: Context): DisposicionTactil {
            val p = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)
            val centros = GrupoTactil.entries.mapNotNull { g ->
                val x = p.getFloat(clave(g, "x"), -1f)
                val y = p.getFloat(clave(g, "y"), -1f)
                if (x < 0f || y < 0f) null else g to (x to y)
            }.toMap()
            return DisposicionTactil(centros, p.getFloat(ClaveAjuste.ESCALA_TACTIL, 1f))
        }
    }
}
