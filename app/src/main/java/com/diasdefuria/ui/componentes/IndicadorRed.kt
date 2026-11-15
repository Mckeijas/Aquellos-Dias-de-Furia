package com.diasdefuria.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diasdefuria.R
import com.diasdefuria.ui.tema.Colores

/**
 * Aviso discreto de partida en red, en una esquina durante el juego: un icono con
 * cuántos jugadores hay (contando al anfitrión). Sin nadie conectado todavía se ve
 * el icono de «sala abierta». [ping] (solo el invitado): punto verde, amarillo o rojo
 * según el retraso. El detalle (quién es quién) está en el menú de pausa.
 */
@Composable
fun IndicadorRed(jugadores: Int, esperando: Boolean, ping: Int?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Colores.Piedra.copy(alpha = 0.55f), FormaPlaca)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            if (esperando) Icons.Filled.WifiTethering else Icons.Filled.Groups,
            contentDescription = stringResource(R.string.multi_conectados),
            tint = Colores.OroClaro.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp),
        )
        if (!esperando) {
            Text("$jugadores", style = MaterialTheme.typography.labelMedium, color = Colores.OroClaro.copy(alpha = 0.85f))
        }
        if (ping != null) {
            Box(Modifier.size(8.dp).background(colorPing(ping), CircleShape))
        }
    }
}

/** Verde hasta 40 ms, amarillo hasta 90 ms y rojo a partir de ahí. */
fun colorPing(ms: Int): Color = when {
    ms < 40 -> Color(0xFF4CD964)
    ms < 90 -> Color(0xFFFFC21F)
    else -> Color(0xFFE3262E)
}
