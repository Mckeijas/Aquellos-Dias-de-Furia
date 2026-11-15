package com.diasdefuria.logros

import android.content.Context
import com.diasdefuria.datos.RomsetConocido
import java.security.MessageDigest

/**
 * Cuenta de RetroAchievements guardada en el dispositivo. Solo se guarda el
 * nombre de usuario y el token de sesión que da el servidor, nunca la contraseña.
 */
class CuentaLogros(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("retroachievements", Context.MODE_PRIVATE)

    val usuario: String? get() = p.getString("usuario", null)
    val nombre: String? get() = p.getString("nombre", null)
    val token: String? get() = p.getString("token", null)
    val puntos: Int get() = p.getInt("puntos", 0)
    val conectada: Boolean get() = !usuario.isNullOrEmpty() && !token.isNullOrEmpty()

    /** Logros activos en las partidas. */
    var activados: Boolean
        get() = p.getBoolean("activados", true)
        set(v) { p.edit().putBoolean("activados", v).apply() }

    /** Sonido al conseguir un logro. */
    var sonido: Boolean
        get() = p.getBoolean("sonido", true)
        set(v) { p.edit().putBoolean("sonido", v).apply() }

    fun guardar(usuario: String, nombre: String, token: String, puntos: Int) {
        p.edit().putString("usuario", usuario).putString("nombre", nombre.ifEmpty { usuario })
            .putString("token", token).putInt("puntos", puntos).apply()
    }

    fun actualizarPuntos(puntos: Int) = p.edit().putInt("puntos", puntos).apply()

    fun cerrarSesion() {
        p.edit().remove("usuario").remove("nombre").remove("token").remove("puntos").apply()
    }

    companion object {
        /**
         * Identificador del juego para RetroAchievements: en recreativa es el MD5 del
         * nombre del romset (por ejemplo «ddtod»). Cada versión tiene el suyo.
         */
        fun hash(romset: RomsetConocido): String =
            MessageDigest.getInstance("MD5").digest(romset.nombre.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
