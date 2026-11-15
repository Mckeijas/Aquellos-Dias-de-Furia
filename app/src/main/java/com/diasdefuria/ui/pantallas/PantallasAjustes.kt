package com.diasdefuria.ui.pantallas

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.diasdefuria.datos.AccionMando
import com.diasdefuria.datos.BotonesFisicos
import com.diasdefuria.datos.DisposicionTactil
import com.diasdefuria.datos.MapaBotones
import com.diasdefuria.juego.CapturaMando
import com.diasdefuria.juego.ControlesTactiles
import com.diasdefuria.juego.EstiloTactil
import com.diasdefuria.juego.leerEstiloTactil
import com.diasdefuria.ui.componentes.FormaPlaca
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.diasdefuria.R
import com.diasdefuria.datos.ClaveAjuste
import com.diasdefuria.datos.Juego
import com.diasdefuria.datos.rememberAjuste
import com.diasdefuria.ui.componentes.FilaDeslizador
import com.diasdefuria.ui.componentes.FilaInterruptor
import com.diasdefuria.ui.componentes.PantallaSeccion
import com.diasdefuria.ui.componentes.Subtitulo
import com.diasdefuria.ui.tema.Colores
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.mutableIntStateOf
import com.diasdefuria.datos.Partida
import com.diasdefuria.datos.RepositorioPartidas
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PantallaControles(onVolver: () -> Unit) {
    val context = LocalContext.current
    val mapa = remember { MapaBotones(context) }
    var version by remember { mutableIntStateOf(0) } // fuerza a releer la asignación
    var capturando by remember { mutableStateOf<AccionMando?>(null) }
    var editandoTactiles by remember { mutableStateOf(false) }

    var tactiles by rememberAjuste(ClaveAjuste.CONTROLES_TACTILES, true)
    var opacidad by rememberAjuste(ClaveAjuste.OPACIDAD_TACTIL, 0.6f)
    var escala by rememberAjuste(ClaveAjuste.ESCALA_TACTIL, 1f)
    var vibracion by rememberAjuste(ClaveAjuste.VIBRACION, true)
    var ocultarConMando by rememberAjuste(ClaveAjuste.OCULTAR_CON_MANDO, true)
    var monedaConStart by rememberAjuste(ClaveAjuste.MONEDA_CON_START, false)
    val prefs = remember { context.getSharedPreferences("ajustes", android.content.Context.MODE_PRIVATE) }
    var estiloTactil by remember { mutableStateOf(leerEstiloTactil(context)) }

    Box(Modifier.fillMaxSize()) {
        PantallaSeccion(stringResource(R.string.controles_titulo), onVolver) {
            // ── Mando físico ──
            Subtitulo(stringResource(R.string.controles_mando_fisico))
            Text(stringResource(R.string.controles_mando_ayuda), style = MaterialTheme.typography.bodyMedium,
                color = Colores.TextoSuave)
            key(version) {
                AccionMando.entries.forEach { a ->
                    FilaBoton(a, BotonesFisicos.nombre(mapa.tecla(a)),
                        onCambiar = { capturando = a },
                        onQuitar = { mapa.quitar(a); version++ })
                }
            }
            FilaInterruptor(stringResource(R.string.controles_moneda_start), monedaConStart) { monedaConStart = it }
            BotonMystara(stringResource(R.string.controles_restablecer_botones),
                { mapa.restablecer(); monedaConStart = false; version++ },
                Modifier.padding(vertical = 6.dp), icono = Icons.Filled.RestartAlt, estilo = EstiloBoton.DISCRETO)

            // ── Controles táctiles ──
            Subtitulo(stringResource(R.string.controles_tactiles_titulo))
            Text(stringResource(R.string.controles_tactiles_ayuda), style = MaterialTheme.typography.bodyMedium,
                color = Colores.TextoSuave)
            FilaInterruptor(stringResource(R.string.controles_tactiles), tactiles) { tactiles = it }
            FilaInterruptor(stringResource(R.string.controles_mando), ocultarConMando) { ocultarConMando = it }
            Text(stringResource(R.string.controles_estilo), style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                EstiloTactil.entries.forEach { e ->
                    BotonMystara(
                        stringResource(if (e == EstiloTactil.TRANSPARENTE) R.string.estilo_transparente else R.string.estilo_clasico),
                        { estiloTactil = e; prefs.edit().putString(ClaveAjuste.ESTILO_TACTIL, e.name).apply() },
                        estilo = if (estiloTactil == e) EstiloBoton.PRINCIPAL else EstiloBoton.DISCRETO,
                    )
                }
            }
            FilaDeslizador(stringResource(R.string.controles_opacidad), opacidad, { opacidad = it }, habilitado = tactiles)
            FilaDeslizador(stringResource(R.string.controles_tamano), escala, { escala = it }, habilitado = tactiles,
                rango = 0.6f..1.5f)
            FilaInterruptor(stringResource(R.string.controles_vibracion), vibracion) { vibracion = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                BotonMystara(stringResource(R.string.controles_colocar), { editandoTactiles = true },
                    icono = Icons.Filled.OpenWith, estilo = EstiloBoton.PRINCIPAL, habilitado = tactiles)
                BotonMystara(stringResource(R.string.controles_restablecer_tactiles), {
                    DisposicionTactil(escala = 1f).guardar(context); escala = 1f
                }, icono = Icons.Filled.RestartAlt, estilo = EstiloBoton.DISCRETO)
            }
        }

        capturando?.let { a ->
            CapturaBoton(a, onTecla = { k -> mapa.asignar(a, k); version++; capturando = null },
                onCancelar = { capturando = null })
        }
        if (editandoTactiles) {
            EditorTactiles(estiloTactil, onCerrar = { editandoTactiles = false })
        }
    }
}

@Composable
private fun FilaBoton(a: AccionMando, tecla: String, onCambiar: () -> Unit, onQuitar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(a.nombre), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(a.descripcion), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
        }
        Text(
            tecla,
            style = MaterialTheme.typography.titleMedium,
            color = Colores.OroClaro,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 72.dp)
                .border(1.5.dp, Colores.Oro, FormaPlaca)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        BotonMystara(stringResource(R.string.controles_cambiar), onCambiar)
        BotonMystara(stringResource(R.string.controles_quitar), onQuitar, estilo = EstiloBoton.DISCRETO)
    }
}

/** Espera a que se pulse un botón del mando o del teclado. Atrás cancela. */
@Composable
private fun CapturaBoton(a: AccionMando, onTecla: (Int) -> Unit, onCancelar: () -> Unit) {
    val alTecla by rememberUpdatedState(onTecla)
    val alCancelar by rememberUpdatedState(onCancelar)
    DisposableEffect(Unit) {
        var pulsada = 0
        CapturaMando.oyente = { e ->
            when {
                e.keyCode == KeyEvent.KEYCODE_BACK || e.keyCode == KeyEvent.KEYCODE_ESCAPE ||
                    e.keyCode == KeyEvent.KEYCODE_BUTTON_MODE -> { if (e.action == KeyEvent.ACTION_UP) alCancelar() }
                e.keyCode in BotonesFisicos.ASIGNABLES -> {
                    // Se asigna al soltar, para que esa misma pulsación no active nada más
                    if (e.action == KeyEvent.ACTION_DOWN) pulsada = e.keyCode
                    else if (e.action == KeyEvent.ACTION_UP && pulsada == e.keyCode) alTecla(e.keyCode)
                }
            }
            true
        }
        onDispose { CapturaMando.oyente = null }
    }
    BackHandler(onBack = onCancelar)
    Box(
        Modifier.fillMaxSize().background(Colores.Piedra.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(440.dp)
                .background(Colores.Piedra, FormaPlaca)
                .border(2.dp, Colores.Oro, FormaPlaca)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(a.nombre), style = MaterialTheme.typography.headlineMedium, color = Colores.Oro)
            Text(stringResource(R.string.controles_pulsa), style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center)
            Text(stringResource(R.string.controles_pulsa_ayuda), style = MaterialTheme.typography.labelMedium,
                color = Colores.TextoSuave, textAlign = TextAlign.Center)
            BotonMystara(stringResource(R.string.cancelar), onCancelar, estilo = EstiloBoton.DISCRETO)
        }
    }
}

/** Pantalla para colocar los controles táctiles arrastrándolos sobre una imagen del juego. */
@Composable
private fun EditorTactiles(estilo: EstiloTactil, onCerrar: () -> Unit) {
    val context = LocalContext.current
    var disposicion by remember { mutableStateOf(DisposicionTactil.cargar(context)) }
    BackHandler(onBack = onCerrar)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = "file:///android_asset/vistas/ninguno.webp",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize().alpha(0.55f),
        )
        ControlesTactiles(
            opacidad = 1f,
            vibracion = false,
            botones = 4,
            onBoton = { _, _ -> },
            onDireccion = { _, _ -> },
            onMenu = {},
            disposicion = disposicion,
            onMover = { g, fx, fy -> disposicion = disposicion.con(g, fx, fy) },
            estilo = estilo,
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .background(Colores.Piedra.copy(alpha = 0.85f), FormaPlaca)
                .border(1.5.dp, Colores.Oro, FormaPlaca)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.controles_colocar_ayuda), style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonMystara(stringResource(R.string.guardar), {
                    disposicion.guardar(context); onCerrar()
                }, icono = Icons.Filled.Save, estilo = EstiloBoton.PRINCIPAL)
                BotonMystara(stringResource(R.string.controles_restablecer_posicion), {
                    disposicion = DisposicionTactil(escala = disposicion.escala)
                }, icono = Icons.Filled.RestartAlt)
                BotonMystara(stringResource(R.string.cancelar), onCerrar, estilo = EstiloBoton.DISCRETO)
            }
        }
    }
}

/**
 * Partidas guardadas de cada juego. «Cargar» abre el juego directamente en ese punto,
 * con la misma versión (romset) con la que se guardó.
 */
@Composable
fun PantallaPartidas(onVolver: () -> Unit, onCargar: (Partida) -> Unit) {
    val context = LocalContext.current
    val repo = remember { RepositorioPartidas(context) }
    var autoguardado by rememberAjuste(ClaveAjuste.AUTOGUARDADO, true)
    var version by remember { mutableIntStateOf(0) }
    val formato = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    PantallaSeccion(stringResource(R.string.partidas_titulo), onVolver) {
        FilaInterruptor(stringResource(R.string.partidas_autoguardado), autoguardado) { autoguardado = it }
        // Con muchos juegos solo se listan los que tienen alguna partida guardada
        val porJuego = remember(version) { Juego.entries.map { it to repo.listar(it) }.filter { it.second.isNotEmpty() } }
        if (porJuego.isEmpty()) {
            Text(stringResource(R.string.partidas_ninguna),
                style = MaterialTheme.typography.bodyMedium, color = Colores.TextoSuave)
        }
        porJuego.forEach { (juego, partidas) ->
            Subtitulo(juego.nombre)
            partidas.forEach { partida ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                partida.automatica -> stringResource(R.string.partida_auto)
                                partida.ranura != null -> stringResource(R.string.ranura_n, partida.ranura!!)
                                else -> stringResource(R.string.partida_manual, formato.format(Date(partida.fecha)))
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "${formato.format(Date(partida.fecha))} · ${partida.romset.nombre} (${partida.romset.region})",
                            style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave,
                        )
                    }
                    BotonMystara(stringResource(R.string.partida_cargar), { onCargar(partida) },
                        icono = Icons.Filled.PlayArrow, estilo = EstiloBoton.PRINCIPAL)
                    BotonMystara(stringResource(R.string.partida_borrar), { repo.borrar(partida); version++ },
                        icono = Icons.Filled.Delete, estilo = EstiloBoton.DISCRETO)
                }
            }
        }
    }
}
