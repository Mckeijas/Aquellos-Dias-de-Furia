package com.diasdefuria.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.diasdefuria.R
import com.diasdefuria.datos.AjustesTrucos
import com.diasdefuria.datos.Juego
import com.diasdefuria.datos.Regla
import com.diasdefuria.datos.Reglas
import com.diasdefuria.datos.Truco
import com.diasdefuria.datos.Trucos
import com.diasdefuria.ui.componentes.BotonMystara
import com.diasdefuria.ui.componentes.EstiloBoton
import com.diasdefuria.ui.componentes.PantallaSeccion
import com.diasdefuria.ui.componentes.Subtitulo
import com.diasdefuria.ui.tema.Colores

/** Sección «Trucos»: trucos y reglas de la recreativa de cada juego. */
@Composable
fun PantallaMods(onVolver: () -> Unit) {
    var juego by rememberSaveable { mutableStateOf(Juego.primero) }

    Box(Modifier.fillMaxSize()) {
    PantallaSeccion(stringResource(R.string.mods_titulo), onVolver) {

        // Elección de juego
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Juego.entries.forEach { j ->
                BotonMystara(
                    texto = j.nombre,
                    onClick = { juego = j },
                    estilo = if (j == juego) EstiloBoton.PRINCIPAL else EstiloBoton.DISCRETO,
                )
            }
        }
        Text(stringResource(R.string.mods_aviso_romset), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)

        SeccionTrucos(juego, onCambio = null)
        SeccionReglas(juego)
    }
    }
}

// ── Trucos ───────────────────────────────────────────────────────────────────

/**
 * Lista de trucos de un juego, filtrada por jugador. Se usa en «Mods y trucos» y en
 * el menú de pausa; allí [onCambio] aplica el truco al momento.
 */
@Composable
fun SeccionTrucos(juego: Juego, onCambio: ((Truco, Int) -> Unit)?) {
    val context = LocalContext.current
    val ajustes = remember { AjustesTrucos(context) }
    val trucos = remember(juego) { Trucos.cargar(context, juego) }
    var version by remember { mutableIntStateOf(0) }
    val elegidos = remember(juego, version) { ajustes.elegidos(juego) }
    var grupo by rememberSaveable(juego) { mutableStateOf<Int?>(null) } // null = generales
    var dialogo by remember { mutableStateOf<Truco?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Subtitulo(stringResource(R.string.trucos_titulo))
        Text(
            "  " + stringResource(R.string.trucos_activos, elegidos.count { (i, v) -> trucos.any { it.indice == i && it.porDefecto != v } }),
            style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave,
            modifier = Modifier.weight(1f),
        )
        BotonMystara(stringResource(R.string.trucos_quitar), {
            trucos.forEach { t -> if ((elegidos[t.indice] ?: t.porDefecto) != t.porDefecto) onCambio?.invoke(t, t.porDefecto) }
            ajustes.quitarTodos(juego); version++
        }, icono = Icons.Filled.RestartAlt, estilo = EstiloBoton.DISCRETO)
    }
    if (trucos.isEmpty()) {
        Text(stringResource(R.string.trucos_vacio), style = MaterialTheme.typography.bodyMedium, color = Colores.TextoSuave)
        return
    }

    val grupos = listOf<Int?>(null) + (1..4).filter { j -> trucos.any { it.jugador == j } }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        grupos.forEach { g ->
            BotonMystara(
                texto = if (g == null) stringResource(R.string.trucos_generales) else stringResource(R.string.trucos_jugador, g),
                onClick = { grupo = g },
                estilo = if (g == grupo) EstiloBoton.PRINCIPAL else EstiloBoton.DISCRETO,
            )
        }
    }

    trucos.filter { it.jugador == grupo }.forEach { t ->
        val actual = elegidos[t.indice] ?: t.porDefecto
        val fijar = { v: Int ->
            ajustes.fijar(juego, t.indice, v); version++
            onCambio?.invoke(t, v)
        }
        if (t.esInterruptor) {
            FilaConInterruptor(t.nombreEs, actual != t.porDefecto) { si -> fijar(if (si) 1 - t.porDefecto else t.porDefecto) }
        } else {
            FilaConValor(t.nombreEs, t.opcionEs(actual)) { dialogo = t }
        }
    }

    dialogo?.let { t ->
        DialogoOpciones(
            titulo = t.nombreEs,
            opciones = t.opciones.indices.map { t.opcionEs(it) },
            elegida = elegidos[t.indice] ?: t.porDefecto,
            onElegir = { v -> ajustes.fijar(juego, t.indice, v); version++; onCambio?.invoke(t, v); dialogo = null },
            onCerrar = { dialogo = null },
        )
    }
}

// ── Reglas de la recreativa (DIP) ────────────────────────────────────────────

@Composable
private fun SeccionReglas(juego: Juego) {
    val context = LocalContext.current
    val reglas = Reglas.de(juego)
    Subtitulo(stringResource(R.string.reglas_titulo))
    if (reglas.isEmpty()) {
        Text(stringResource(R.string.reglas_cps2), style = MaterialTheme.typography.bodyMedium, color = Colores.TextoSuave)
        return
    }
    var version by remember { mutableIntStateOf(0) }
    var dialogo by remember { mutableStateOf<Regla?>(null) }
    reglas.forEach { r ->
        val v = remember(juego, version) { Reglas.valor(context, juego, r) }
        FilaConValor(r.titulo, r.valores.firstOrNull { it.first == v }?.second ?: v) { dialogo = r }
    }
    Text(stringResource(R.string.reglas_aviso), style = MaterialTheme.typography.labelMedium, color = Colores.TextoSuave)
    dialogo?.let { r ->
        val actual = Reglas.valor(context, juego, r)
        DialogoOpciones(
            titulo = r.titulo,
            opciones = r.valores.map { it.second },
            elegida = r.valores.indexOfFirst { it.first == actual }.coerceAtLeast(0),
            onElegir = { i -> Reglas.fijar(context, juego, r, r.valores[i].first); version++; dialogo = null },
            onCerrar = { dialogo = null },
        )
    }
}

// ── Piezas comunes ───────────────────────────────────────────────────────────

@Composable
private fun FilaConInterruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch) { onCambio(!valor) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = valor, onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedThumbColor = Colores.OroClaro, checkedTrackColor = Colores.Carmesi,
                checkedBorderColor = Colores.Oro),
        )
    }
}

@Composable
private fun FilaConValor(texto: String, valor: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = Colores.OroClaro)
    }
}

@Composable
private fun DialogoOpciones(
    titulo: String,
    opciones: List<String>,
    elegida: Int,
    onElegir: (Int) -> Unit,
    onCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                opciones.forEachIndexed { i, o ->
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { onElegir(i) }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = i == elegida, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = Colores.Oro))
                        Text(o, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.cerrar)) } },
    )
}
