package com.diasdefuria.ui.pantallas

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.WifiTethering
import com.diasdefuria.red.Descubrimiento
import com.diasdefuria.red.PartidaEnRed
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.rememberScrollState
import com.diasdefuria.red.CodigoInvitacion
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.diasdefuria.ui.componentes.Subtitulo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.diasdefuria.R
import com.diasdefuria.datos.EstadoJuego
import com.diasdefuria.datos.Juego
import com.diasdefuria.datos.RomsetConocido
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.FormaPlaca
import com.diasdefuria.ui.componentes.PantallaSeccion
import com.diasdefuria.ui.tema.Pinceles
import com.diasdefuria.ui.tema.Colores

/**
 * Sección «Multijugador»: crear una partida en la wifi (este dispositivo emula y
 * retransmite) o unirse a una que haya abierto otro dispositivo.
 */
@Composable
fun PantallaMultijugador(
    estados: Map<Juego, EstadoJuego>,
    onVolver: () -> Unit,
    onCrear: (RomsetConocido, internet: Boolean) -> Unit,
    onUnirse: (PartidaEnRed) -> Unit,
    onUnirseCodigo: (host: String, puerto: Int) -> Unit,
) {
    val context = LocalContext.current
    var partidas by remember { mutableStateOf<List<PartidaEnRed>>(emptyList()) }
    DisposableEffect(Unit) {
        val d = Descubrimiento(context)
        d.buscar { partidas = it }
        onDispose { d.cerrar() }
    }

    PantallaSeccion(stringResource(R.string.menu_multijugador), onVolver) {
        Text(stringResource(R.string.multi_descripcion), style = MaterialTheme.typography.bodyMedium, color = Colores.TextoSuave)

        Subtitulo(stringResource(R.string.multi_crear))
        val jugables = Juego.entries.mapNotNull { j -> estados[j]?.jugable }
        if (jugables.isEmpty()) {
            Text(stringResource(R.string.multi_sin_juegos), style = MaterialTheme.typography.bodyMedium, color = Colores.TextoSuave)
        }
        jugables.forEach { rs ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(rs.juego.nombre, style = MaterialTheme.typography.titleMedium, color = Colores.Oro,
                    modifier = Modifier.weight(1f))
                BotonMystara(stringResource(R.string.inet_crear_casa), { onCrear(rs, false) },
                    icono = Icons.Filled.WifiTethering, estilo = EstiloBoton.PRINCIPAL)
                BotonMystara(stringResource(R.string.inet_crear_internet), { onCrear(rs, true) },
                    icono = Icons.Filled.Public, estilo = EstiloBoton.PRINCIPAL)
            }
        }
        // Avisos propios de algunos juegos (campo avisoMultijugador de Juego.kt)
        jugables.mapNotNull { it.juego.avisoMultijugador }.distinct().forEach { aviso ->
            Text(stringResource(aviso), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave,
                modifier = Modifier.padding(top = 4.dp))
        }

        Subtitulo(stringResource(R.string.multi_unirse))
        if (partidas.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Colores.Oro, strokeWidth = 2.dp)
                Text("  " + stringResource(R.string.multi_buscando), style = MaterialTheme.typography.bodyMedium, color = Colores.TextoSuave)
            }
        }
        partidas.forEach { p ->
            val nombreJuego = Juego.porId(p.juego)?.nombre ?: p.juego
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(nombreJuego, style = MaterialTheme.typography.titleMedium, color = Colores.Oro)
                    Text(stringResource(R.string.multi_de, p.anfitrion), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
                }
                BotonMystara(stringResource(R.string.multi_unirse_boton), { onUnirse(p) }, icono = Icons.AutoMirrored.Filled.Login)
            }
        }
        Subtitulo(stringResource(R.string.inet_unirse_codigo))
        Text(stringResource(R.string.inet_unirse_ayuda), style = MaterialTheme.typography.bodyMedium, color = Colores.TextoSuave)
        var codigo by rememberSaveable { mutableStateOf("") }
        var errorCodigo by remember { mutableStateOf(false) }
        fun unirse() {
            val destino = CodigoInvitacion.leer(codigo)
            if (destino == null) errorCodigo = true else onUnirseCodigo(destino.first, destino.second)
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it.take(64); errorCodigo = false },
                singleLine = true,
                isError = errorCodigo,
                placeholder = { Text(stringResource(R.string.inet_codigo_ejemplo)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { unirse() }),
                textStyle = MaterialTheme.typography.titleMedium.copy(color = Colores.OroClaro),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Colores.OroClaro, unfocusedBorderColor = Colores.Oro,
                    cursorColor = Colores.OroClaro, errorBorderColor = Colores.Carmesi,
                ),
                modifier = Modifier.weight(1f),
            )
            BotonMystara(stringResource(R.string.multi_unirse_boton), ::unirse, icono = Icons.AutoMirrored.Filled.Login,
                estilo = EstiloBoton.PRINCIPAL, habilitado = codigo.isNotBlank())
        }
        if (errorCodigo) {
            Text(stringResource(R.string.inet_codigo_malo), style = MaterialTheme.typography.labelMedium, color = Colores.OroClaro)
        }

        Text(stringResource(R.string.multi_notas), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave,
            modifier = Modifier.padding(top = 12.dp))
    }
}
